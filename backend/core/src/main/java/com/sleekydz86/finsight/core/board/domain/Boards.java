package com.sleekydz86.finsight.core.board.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Boards {
    private final List<Board> boards;

    public Boards() {
        this.boards = new ArrayList<>();
    }

    public Boards(List<Board> boards) {
        this.boards = boards != null ? boards : new ArrayList<>();
    }

    public Boards addBoard(Board board) {
        List<Board> newBoards = new ArrayList<>(this.boards);
        newBoards.add(board);
        return new Boards(newBoards);
    }

    public Boards removeBoard(Long boardId) {
        List<Board> newBoards = this.boards.stream()
                .filter(board -> !board.getId().equals(boardId))
                .collect(Collectors.toList());
        return new Boards(newBoards);
    }

    public Boards updateBoard(Long boardId, Board updatedBoard) {
        List<Board> newBoards = this.boards.stream()
                .map(board -> board.getId().equals(boardId) ? updatedBoard : board)
                .collect(Collectors.toList());
        return new Boards(newBoards);
    }

    public List<Board> getActiveBoards() {
        return boards.stream()
                .filter(Board::isActive)
                .collect(Collectors.toList());
    }

    public List<Board> getBoardsByType(BoardType boardType) {
        return boards.stream()
                .filter(board -> board.getBoardType() == boardType)
                .collect(Collectors.toList());
    }

    public List<Board> getBoardsByAuthor(String authorEmail) {
        return boards.stream()
                .filter(board -> board.getAuthorEmail().equals(authorEmail))
                .collect(Collectors.toList());
    }

    public List<Board> getReportedBoards() {
        return boards.stream()
                .filter(Board::isReported)
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return boards.size();
    }

    public int getActiveCount() {
        return (int) boards.stream().filter(Board::isActive).count();
    }

    public boolean isEmpty() {
        return boards.isEmpty();
    }

    public List<Board> getBoards() {
        return boards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Boards boards1 = (Boards) o;
        return boards.equals(boards1.boards);
    }

    @Override
    public int hashCode() {
        return boards.hashCode();
    }

    @Override
    public String toString() {
        return "Boards{" +
                "boards=" + boards +
                '}';
    }
}