package com.sleekydz86.finsight.core.search.service;

import com.sleekydz86.finsight.core.board.adapter.persistence.command.BoardJpaEntity;
import com.sleekydz86.finsight.core.board.adapter.persistence.command.BoardJpaRepository;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.comment.adapter.persistence.command.CommentJpaEntity;
import com.sleekydz86.finsight.core.comment.adapter.persistence.command.CommentJpaRepository;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaEntity;
import com.sleekydz86.finsight.core.inbox.adapter.persistence.InboxNotificationJpaRepository;
import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaEntity;
import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaRepository;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult.SearchHit;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult.SearchSection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MysqlCatalogSearch {

    private static final int HITS = 5;
    private static final int MINE = 40;
    private static final List<BoardType> COMMUNITY = List.of(
            BoardType.NOTICE, BoardType.QNA, BoardType.FREE, BoardType.COMMUNITY);

    private final NewsJpaRepository newsRepository;
    private final YoutubeVideoMetaJpaRepository videoRepository;
    private final BoardJpaRepository boardRepository;
    private final CommentJpaRepository commentRepository;
    private final InboxNotificationJpaRepository inboxRepository;

    public MysqlCatalogSearch(
            NewsJpaRepository newsRepository,
            YoutubeVideoMetaJpaRepository videoRepository,
            BoardJpaRepository boardRepository,
            CommentJpaRepository commentRepository,
            InboxNotificationJpaRepository inboxRepository) {
        this.newsRepository = newsRepository;
        this.videoRepository = videoRepository;
        this.boardRepository = boardRepository;
        this.commentRepository = commentRepository;
        this.inboxRepository = inboxRepository;
    }

    @Transactional(readOnly = true)
    public SearchSection news(String keyword) {
        Page<NewsJpaEntity> page = newsRepository.searchCatalog(keyword, PageRequest.of(0, HITS));
        List<SearchHit> hits = page.getContent().stream().map(row -> hit(
                SearchSnippets.title(row.getTranslatedTitle(), row.getOriginalTitle()),
                SearchSnippets.snippet(row.getTranslatedContent(), row.getOriginalContent(), keyword),
                "/news/" + row.getId())).toList();
        return section("news", "뉴스", page.getTotalElements(), "", hits);
    }

    @Transactional(readOnly = true)
    public SearchSection videos(String keyword) {
        Page<YoutubeVideoMetaJpaEntity> page = videoRepository.searchPublishedCatalog(keyword, PageRequest.of(0, HITS));
        List<SearchHit> hits = page.getContent().stream().map(row -> hit(
                SearchSnippets.title(row.getYoutubeTitle(), row.getChannelTitle()),
                SearchSnippets.snippet(row.getSummary(), row.getYoutubeTitle(), keyword),
                "/live-vod/watch/" + row.getVideoId())).toList();
        return section("vod", "실시간 VOD", page.getTotalElements(), "", hits);
    }

    @Transactional(readOnly = true)
    public SearchSection community(String keyword) {
        List<SearchHit> hits = new ArrayList<>();
        long total = 0;
        for (BoardType type : COMMUNITY) {
            Page<BoardJpaEntity> page = boardRepository.findVisibleByBoardTypeAndKeyword(
                    type, keyword, null, false, PageRequest.of(0, HITS));
            total += page.getTotalElements();
            page.getContent().stream().limit(HITS - hits.size()).map(row -> boardHit(row, keyword)).forEach(hits::add);
        }
        return section("community", "커뮤니티", total, "", hits);
    }

    @Transactional(readOnly = true)
    public SearchSection mine(String keyword, Long userId, String email) {
        if (userId == null || email == null || email.isBlank()) {
            return section("activity", "내 활동", 0, "로그인해야지 볼 수 있습니다.", List.of());
        }
        List<SearchHit> hits = new ArrayList<>();
        mineBoards(email, keyword, hits);
        mineComments(email, keyword, hits);
        mineInbox(userId, keyword, hits);
        return section("activity", "내 활동", hits.size(), "", hits.stream().limit(HITS).toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogDocument> indexPage(String sourceType, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        if ("NEWS".equals(sourceType)) {
            return newsRepository.findLatestNews(request).getContent().stream().map(this::newsDoc).toList();
        }
        if ("VOD".equals(sourceType)) {
            return videoRepository.findByImportStatus(YoutubeImportStatus.PUBLISHED, request).getContent().stream()
                    .filter(row -> row.getVideoId() != null && row.getVideoId().length() == 11)
                    .map(this::videoDoc)
                    .toList();
        }
        List<CatalogDocument> docs = new ArrayList<>();
        for (BoardType type : COMMUNITY) {
            boardRepository.findByBoardTypeAndStatusOrderByCreatedAtDesc(type, BoardStatus.ACTIVE, request)
                    .getContent().stream().map(this::boardDoc).forEach(docs::add);
        }
        return docs;
    }

    private void mineBoards(String email, String keyword, List<SearchHit> hits) {
        boardRepository.findByAuthorEmailAndStatusOrderByCreatedAtDesc(email, BoardStatus.ACTIVE, PageRequest.of(0, MINE))
                .getContent().stream()
                .filter(row -> contains(row.getTitle(), keyword) || contains(row.getContent(), keyword))
                .map(row -> boardHit(row, keyword))
                .forEach(hits::add);
    }

    private void mineComments(String email, String keyword, List<SearchHit> hits) {
        commentRepository.findByAuthorEmailAndStatusOrderByCreatedAtDesc(email, CommentStatus.ACTIVE, PageRequest.of(0, MINE))
                .getContent().stream()
                .filter(row -> contains(row.getContent(), keyword))
                .map(row -> commentHit(row, keyword))
                .forEach(hits::add);
    }

    private void mineInbox(Long userId, String keyword, List<SearchHit> hits) {
        inboxRepository.findByRecipientUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, MINE))
                .getContent().stream()
                .filter(row -> contains(row.getTitle(), keyword) || contains(row.getBody(), keyword))
                .map(row -> inboxHit(row, keyword))
                .forEach(hits::add);
    }

    private CatalogDocument newsDoc(NewsJpaEntity row) {
        return new CatalogDocument("NEWS", String.valueOf(row.getId()),
                SearchSnippets.title(row.getTranslatedTitle(), row.getOriginalTitle()),
                first(row.getTranslatedContent(), row.getOriginalContent()),
                "/news/" + row.getId());
    }

    private CatalogDocument videoDoc(YoutubeVideoMetaJpaEntity row) {
        return new CatalogDocument("VOD", row.getVideoId(),
                SearchSnippets.title(row.getYoutubeTitle(), row.getChannelTitle()),
                first(row.getSummary(), ""),
                "/live-vod/watch/" + row.getVideoId());
    }

    private CatalogDocument boardDoc(BoardJpaEntity row) {
        return new CatalogDocument("COMMUNITY", String.valueOf(row.getId()),
                SearchSnippets.title(row.getTitle(), ""),
                first(row.getContent(), ""),
                boardHref(row));
    }

    private SearchHit boardHit(BoardJpaEntity row, String keyword) {
        return hit(SearchSnippets.title(row.getTitle(), ""),
                SearchSnippets.snippet(row.getContent(), row.getTitle(), keyword),
                boardHref(row));
    }

    private SearchHit commentHit(CommentJpaEntity row, String keyword) {
        return hit("내 댓글", SearchSnippets.snippet(row.getContent(), "", keyword), commentHref(row));
    }

    private SearchHit inboxHit(InboxNotificationJpaEntity row, String keyword) {
        String href = row.getLinkUrl() != null && row.getLinkUrl().startsWith("/") ? row.getLinkUrl() : "/myinfo/activity";
        return hit(SearchSnippets.title(row.getTitle(), "알림"),
                SearchSnippets.snippet(row.getBody(), row.getTitle(), keyword),
                href);
    }

    private String commentHref(CommentJpaEntity row) {
        if (row.getCommentType() == CommentType.NEWS) {
            return "/news/" + row.getTargetId();
        }
        return "/myinfo/activity";
    }

    private String boardHref(BoardJpaEntity row) {
        BoardType type = row.getBoardType();
        if (type == BoardType.NOTICE) {
            return "/community/notice/" + row.getId();
        }
        if (type == BoardType.QNA) {
            return "/community/qna/" + row.getId();
        }
        return "/community/free/" + row.getId();
    }

    private SearchSection section(String key, String label, long total, String note, List<SearchHit> hits) {
        return new SearchSection(key, label, total, note, hits);
    }

    private SearchHit hit(String title, String snippet, String href) {
        return new SearchHit(title, snippet, href);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private String first(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }
}
