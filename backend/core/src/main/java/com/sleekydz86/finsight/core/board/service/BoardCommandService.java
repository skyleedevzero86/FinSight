package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.*;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardCommandUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardReportRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardUpdateRequest;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardFilePersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReportPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.global.exception.CommentAlreadyReportedException;
import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BoardCommandService implements BoardCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(BoardCommandService.class);

    private final BoardPersistencePort boardPersistencePort;
    private final BoardReactionPersistencePort boardReactionPersistencePort;
    private final BoardReportPersistencePort boardReportPersistencePort;
    private final BoardScrapPersistencePort boardScrapPersistencePort;
    private final BoardFilePersistencePort boardFilePersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final InboxService inboxService;

    public BoardCommandService(BoardPersistencePort boardPersistencePort,
                               BoardReactionPersistencePort boardReactionPersistencePort,
                               BoardReportPersistencePort boardReportPersistencePort,
                               BoardScrapPersistencePort boardScrapPersistencePort,
                               BoardFilePersistencePort boardFilePersistencePort,
                               UserPersistencePort userPersistencePort,
                               InboxService inboxService) {
        this.boardPersistencePort = boardPersistencePort;
        this.boardReactionPersistencePort = boardReactionPersistencePort;
        this.boardReportPersistencePort = boardReportPersistencePort;
        this.boardScrapPersistencePort = boardScrapPersistencePort;
        this.boardFilePersistencePort = boardFilePersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.inboxService = inboxService;
    }

    @Override
    public Board createBoard(String userEmail, BoardCreateRequest request) {
        log.info("게시글 생성 요청 - 사용자: {}", userEmail);

        BoardStatus initialStatus = request.getStatus() != null ? request.getStatus() : BoardStatus.ACTIVE;
        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorEmail(userEmail)
                .boardType(request.getBoardType())
                .status(initialStatus)
                .hashtags(request.getHashtags())
                .build();

        Board savedBoard = boardPersistencePort.save(board);
        log.info("게시글 생성 완료 - 게시글 ID: {}", savedBoard.getId());
        notifyOnBoardCreated(savedBoard, userEmail);
        return savedBoard;
    }

    private void notifyOnBoardCreated(Board board, String authorEmail) {
        try {
            Optional<User> actorOpt = userPersistencePort.findByEmail(authorEmail);
            User actor = actorOpt.orElse(null);
            String actorName = actor != null && actor.getNickname() != null && !actor.getNickname().isBlank()
                    ? actor.getNickname()
                    : authorEmail;

            if (board.getBoardType() == BoardType.MEDIA) {
                List<User> recipients = userPersistencePort.findAllActiveUsers();
                inboxService.createForUsers(
                        recipients,
                        InboxCategory.YOUTUBE,
                        actor != null ? actor.getId() : null,
                        actorName,
                        actor != null ? actor.getProfileImageUrl() : null,
                        actorName + " 님이 새 유튜브/미디어 콘텐츠를 등록했습니다: [" + board.getTitle() + "]",
                        null,
                        "/live-vod",
                        "BOARD",
                        board.getId());
                return;
            }

            if (board.getBoardType() == BoardType.NOTICE) {
                List<User> admins = userPersistencePort.findAllActiveUsers().stream()
                        .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.MANAGER)
                        .toList();
                inboxService.createForUsers(
                        admins,
                        InboxCategory.ADMIN,
                        actor != null ? actor.getId() : null,
                        actorName,
                        actor != null ? actor.getProfileImageUrl() : null,
                        "관리 메뉴·공지 갱신: [" + board.getTitle() + "]",
                        null,
                        "/community/notice/" + board.getId(),
                        "BOARD",
                        board.getId());
            }
        } catch (Exception e) {
            log.warn("게시글 인앱 알림 생성 실패 - boardId={}, error={}", board.getId(), e.getMessage());
        }
    }

    @Override
    public Board updateBoard(String userEmail, String userRole, Long boardId, BoardUpdateRequest request) {
        log.info("게시글 수정 요청 - 게시글 ID: {}, 사용자: {}, 권한: {}", boardId, userEmail, userRole);

        Board existingBoard = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        assertCanModifyBoard(existingBoard, userEmail, userRole);

        Board updatedBoard = existingBoard.updateContent(
                request.getTitle(),
                request.getContent(),
                request.getHashtags());
        if (request.getStatus() != null) {
            updatedBoard = updatedBoard.updateStatus(request.getStatus());
        }

        Board savedBoard = boardPersistencePort.save(updatedBoard);
        log.info("게시글 수정 완료 - 게시글 ID: {}", boardId);

        return savedBoard;
    }

    @Override
    public void deleteBoard(String userEmail, String userRole, Long boardId) {
        log.info("게시글 삭제 요청 - 게시글 ID: {}, 사용자: {}, 권한: {}", boardId, userEmail, userRole);

        Board existingBoard = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        assertCanModifyBoard(existingBoard, userEmail, userRole);

        boardPersistencePort.deleteById(boardId);
        log.info("게시글 삭제 완료 - 게시글 ID: {}", boardId);
    }

    @Override
    public Board likeBoard(String userEmail, Long boardId) {
        log.info("게시글 좋아요 요청 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Optional<BoardReaction> existingReaction = boardReactionPersistencePort
                .findByBoardIdAndUserEmail(boardId, userEmail);

        if (existingReaction.isPresent()) {
            BoardReaction reaction = existingReaction.get();
            if (reaction.getReactionType() == ReactionType.LIKE) {
                boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
                Board updatedBoard = board.decrementLike();
                return boardPersistencePort.save(updatedBoard);
            } else {
                boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
                Board updatedBoard = board.decrementDislike().incrementLike();
                boardReactionPersistencePort.save(BoardReaction.builder()
                        .boardId(boardId)
                        .userEmail(userEmail)
                        .reactionType(ReactionType.LIKE)
                        .build());
                return boardPersistencePort.save(updatedBoard);
            }
        } else {
            Board updatedBoard = board.incrementLike();
            boardReactionPersistencePort.save(BoardReaction.builder()
                    .boardId(boardId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.LIKE)
                    .build());

            return boardPersistencePort.save(updatedBoard);
        }
    }

    @Override
    public Board dislikeBoard(String userEmail, Long boardId) {
        log.info("게시글 싫어요 요청 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Optional<BoardReaction> existingReaction = boardReactionPersistencePort
                .findByBoardIdAndUserEmail(boardId, userEmail);

        if (existingReaction.isPresent()) {
            BoardReaction reaction = existingReaction.get();
            if (reaction.getReactionType() == ReactionType.DISLIKE) {
                boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
                Board updatedBoard = board.decrementDislike();
                return boardPersistencePort.save(updatedBoard);
            } else {
                boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
                Board updatedBoard = board.decrementLike().incrementDislike();
                boardReactionPersistencePort.save(BoardReaction.builder()
                        .boardId(boardId)
                        .userEmail(userEmail)
                        .reactionType(ReactionType.DISLIKE)
                        .build());
                return boardPersistencePort.save(updatedBoard);
            }
        } else {
            Board updatedBoard = board.incrementDislike();
            boardReactionPersistencePort.save(BoardReaction.builder()
                    .boardId(boardId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.DISLIKE)
                    .build());

            return boardPersistencePort.save(updatedBoard);
        }
    }

    @Override
    public void reportBoard(String userEmail, Long boardId, BoardReportRequest request) {
        log.info("게시글 신고 요청 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Optional<BoardReport> existingReport = boardReportPersistencePort
                .findByBoardIdAndReporterEmail(boardId, userEmail);

        if (existingReport.isPresent()) {
            throw new CommentAlreadyReportedException(boardId, userEmail);
        }

        BoardReport report = BoardReport.builder()
                .boardId(boardId)
                .reporterEmail(userEmail)
                .reason(request.getReason())
                .description(request.getDescription())
                .reportedAt(LocalDateTime.now())
                .isProcessed(false)
                .build();

        boardReportPersistencePort.save(report);

        Board updatedBoard = board.incrementReport();
        boardPersistencePort.save(updatedBoard);

        log.info("게시글 신고 완료 - 게시글 ID: {}", boardId);
    }

    @Override
    public void blockBoard(Long boardId) {
        log.info("게시글 차단 요청 - 게시글 ID: {}", boardId);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Board blockedBoard = board.updateStatus(BoardStatus.BLOCKED);
        boardPersistencePort.save(blockedBoard);

        log.info("게시글 차단 완료 - 게시글 ID: {}", boardId);
    }

    @Override
    public BoardScrap scrapBoard(String userEmail, Long boardId) {
        log.info("게시글 스크랩 요청 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Optional<BoardScrap> existingScrap = boardScrapPersistencePort
                .findByBoardIdAndUserEmail(boardId, userEmail);

        if (existingScrap.isPresent()) {
            throw new CommentAlreadyReportedException(boardId, userEmail);
        }

        BoardScrap scrap = BoardScrap.builder()
                .boardId(boardId)
                .userEmail(userEmail)
                .scrapedAt(LocalDateTime.now())
                .build();

        BoardScrap savedScrap = boardScrapPersistencePort.save(scrap);
        log.info("게시글 스크랩 완료 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        return savedScrap;
    }

    @Override
    public void unscrapBoard(String userEmail, Long boardId) {
        log.info("게시글 스크랩 해제 요청 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);

        boardScrapPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
        log.info("게시글 스크랩 해제 완료 - 게시글 ID: {}, 사용자: {}", boardId, userEmail);
    }

    @Override
    public BoardFile uploadFile(String userEmail, Long boardId, String fileName, String filePath, long fileSize) {
        log.info("게시글 파일 업로드 요청 - 게시글 ID: {}, 사용자: {}, 파일명: {}", boardId, userEmail, fileName);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!board.getAuthorEmail().equalsIgnoreCase(userEmail)) {
            throw new InsufficientPermissionException("FILE_UPLOAD", "파일 업로드 권한이 없습니다");
        }

        BoardFile file = BoardFile.builder()
                .boardId(boardId)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .uploadedAt(LocalDateTime.now())
                .build();

        BoardFile savedFile = boardFilePersistencePort.save(file);
        log.info("게시글 파일 업로드 완료 - 게시글 ID: {}, 파일명: {}", boardId, fileName);

        return savedFile;
    }

    @Override
    public void deleteFile(String userEmail, Long fileId) {
        log.info("게시글 파일 삭제 요청 - 파일 ID: {}, 사용자: {}", fileId, userEmail);

        BoardFile file = boardFilePersistencePort.findById(fileId)
                .orElseThrow(() -> new UserNotFoundException("파일을 찾을 수 없습니다"));

        Board board = boardPersistencePort.findById(file.getBoardId())
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!board.getAuthorEmail().equalsIgnoreCase(userEmail)) {
            throw new InsufficientPermissionException("FILE_DELETE", "파일 삭제 권한이 없습니다");
        }

        boardFilePersistencePort.deleteById(fileId);
        log.info("게시글 파일 삭제 완료 - 파일 ID: {}", fileId);
    }

    private void assertCanModifyBoard(Board board, String userEmail, String userRole) {
        boolean isAuthor = board.getAuthorEmail() != null
                && userEmail != null
                && board.getAuthorEmail().equalsIgnoreCase(userEmail.trim());
        boolean isStaff = isStaffRole(userRole);
        if (!isAuthor && !isStaff) {
            log.warn("게시글 수정/삭제 권한 거부 - 요청자: {}, 역할: {}, 글작성자: {}",
                    userEmail, userRole, board.getAuthorEmail());
            throw new InsufficientPermissionException(
                    "BOARD_MODIFY",
                    "게시글은 작성자와 관리자만 수정·삭제할 수 있습니다");
        }
    }

    private boolean isStaffRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return false;
        }
        String normalized = userRole.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return "ADMIN".equals(normalized) || "MANAGER".equals(normalized);
    }
}