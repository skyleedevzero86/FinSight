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
import com.sleekydz86.finsight.core.global.exception.BaseException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
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
            throw new BaseException("권한이 없습니다", "UNAUTHORIZED", "AUTHORIZATION_ERROR", 403);
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
            throw new BaseException("권한이 없습니다", "UNAUTHORIZED", "AUTHORIZATION_ERROR", 403);
        }

        boardPersistencePort.deleteById(boardId);
        log.info("Board {} deleted successfully", boardId);
    }

    @Override
    public Board likeBoard(String userEmail, Long boardId) {
        log.info("User {} liking board {}", userEmail, boardId);

        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new UserNotFoundException("게시글을 찾을 수 없습니다"));

        // Check if user already liked
        Optional<BoardReaction> existingReaction = boardReactionPersistencePort
                .findByBoardIdAndUserEmail(boardId, userEmail);

        if (existingReaction.isPresent()) {
            boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
            Board updatedBoard = board.incrementDislike();
            return boardPersistencePort.save(updatedBoard);
        } else {
            BoardReaction reaction = BoardReaction.builder()
                    .boardId(boardId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.LIKE)
                    .build();
            boardReactionPersistencePort.save(reaction);

            Board updatedBoard = board.incrementLike();
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
            boardReactionPersistencePort.deleteByBoardIdAndUserEmail(boardId, userEmail);
            Board updatedBoard = board.incrementLike();
            return boardPersistencePort.save(updatedBoard);
        } else {
            BoardReaction reaction = BoardReaction.builder()
                    .boardId(boardId)
                    .userEmail(userEmail)
                    .reactionType(ReactionType.DISLIKE)
                    .build();
            boardReactionPersistencePort.save(reaction);

            Board updatedBoard = board.incrementDislike();
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
            throw new BaseException("이미 신고한 게시글입니다", "ALREADY_REPORTED", "VALIDATION_ERROR", 400);
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
            throw new BaseException("이미 스크랩한 게시글입니다", "ALREADY_SCRAPPED", "VALIDATION_ERROR", 400);
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
    public BoardFile uploadFile(Long boardId, String originalFileName, String storedFileName,
                                String filePath, String contentType, Long fileSize) {
        log.info("Uploading file for board {}", boardId);

        BoardFile file = BoardFile.builder()
                .boardId(boardId)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .contentType(contentType)
                .fileSize(fileSize)
                .uploadedAt(LocalDateTime.now())
                .build();

        BoardFile savedFile = boardFilePersistencePort.save(file);
        log.info("File uploaded successfully with ID: {}", savedFile.getId());

        return savedFile;
    }

    @Override
    public void deleteFile(Long fileId) {
        log.info("Deleting file {}", fileId);

        boardFilePersistencePort.deleteById(fileId);
        log.info("File {} deleted successfully", fileId);
    }
}