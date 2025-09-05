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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public BoardCommandService(BoardPersistencePort boardPersistencePort,
                               BoardReactionPersistencePort boardReactionPersistencePort,
                               BoardReportPersistencePort boardReportPersistencePort,
                               BoardScrapPersistencePort boardScrapPersistencePort,
                               BoardFilePersistencePort boardFilePersistencePort) {
        this.boardPersistencePort = boardPersistencePort;
        this.boardReactionPersistencePort = boardReactionPersistencePort;
        this.boardReportPersistencePort = boardReportPersistencePort;
        this.boardScrapPersistencePort = boardScrapPersistencePort;
        this.boardFilePersistencePort = boardFilePersistencePort;
    }

    @Override
    public Board createBoard(String userEmail, BoardCreateRequest request) {
        log.info("Creating board for user: {}", userEmail);

        Board board = Board.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorEmail(userEmail)
                .boardType(request.getBoardType())
                .status(BoardStatus.ACTIVE)
                .hashtags(request.getHashtags())
                .build();

        Board savedBoard = boardPersistencePort.save(board);
        log.info("Board created successfully with ID: {}", savedBoard.getId());

        return savedBoard;
    }

    @Override
    public Board updateBoard(String userEmail, Long boardId, BoardUpdateRequest request) {
        log.info("Updating board {} by user: {}", boardId, userEmail);

        Board existingBoard = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!existingBoard.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("게시글 수정 권한이 없습니다");
        }

        Board updatedBoard = existingBoard.updateContent(
                request.getTitle(),
                request.getContent(),
                request.getHashtags()
        );

        Board savedBoard = boardPersistencePort.save(updatedBoard);
        log.info("Board {} updated successfully", boardId);

        return savedBoard;
    }

    @Override
    public void deleteBoard(String userEmail, Long boardId) {
        log.info("Deleting board {} by user: {}", boardId, userEmail);

        Board existingBoard = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!existingBoard.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("게시글 삭제 권한이 없습니다");
        }

        boardPersistencePort.deleteById(boardId);
        log.info("Board {} deleted successfully", boardId);
    }

    @Override
    public Board likeBoard(String userEmail, Long boardId) {
        log.info("User {} liking board {}", userEmail, boardId);

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
            // User hasn't reacted, add like
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
        log.info("User {} disliking board {}", userEmail, boardId);

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
        log.info("User {} reporting board {}", userEmail, boardId);

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

        log.info("Board {} reported successfully", boardId);
    }

    @Override
    public void blockBoard(Long boardId) {
        log.info("Blocking board {}", boardId);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        Board blockedBoard = board.updateStatus(BoardStatus.BLOCKED);
        boardPersistencePort.save(blockedBoard);

        log.info("Board {} blocked successfully", boardId);
    }

    @Override
    public BoardScrap scrapBoard(String userEmail, Long boardId) {
        log.info("User {} scraping board {}", userEmail, boardId);

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
        log.info("Board {} scrapped successfully by user {}", boardId, userEmail);

        return savedScrap;
    }

    @Override
    public void unscrapBoard(String userEmail, Long boardId) {
        log.info("User {} unscraping board {}", userEmail, boardId);

        boardScrapPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
        log.info("Board {} unscrapped successfully by user {}", boardId, userEmail);
    }

    @Override
    public BoardFile uploadFile(String userEmail, Long boardId, String fileName, String filePath, long fileSize) {
        log.info("User {} uploading file {} to board {}", userEmail, fileName, boardId);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!board.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("파일 업로드 권한이 없습니다");
        }

        BoardFile file = BoardFile.builder()
                .boardId(boardId)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .uploadedAt(LocalDateTime.now())
                .build();

        BoardFile savedFile = boardFilePersistencePort.save(file);
        log.info("File {} uploaded successfully to board {}", fileName, boardId);

        return savedFile;
    }

    @Override
    public void deleteFile(String userEmail, Long fileId) {
        log.info("User {} deleting file {}", userEmail, fileId);

        BoardFile file = boardFilePersistencePort.findById(fileId)
                .orElseThrow(() -> new UserNotFoundException("파일을 찾을 수 없습니다"));

        Board board = boardPersistencePort.findById(file.getBoardId())
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        if (!board.getAuthorEmail().equals(userEmail)) {
            throw new InsufficientPermissionException("파일 삭제 권한이 없습니다");
        }

        boardFilePersistencePort.deleteById(fileId);
        log.info("File {} deleted successfully", fileId);
    }
}