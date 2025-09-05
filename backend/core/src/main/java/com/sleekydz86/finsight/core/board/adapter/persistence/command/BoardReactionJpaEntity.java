package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.global.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_reactions", uniqueConstraints = @UniqueConstraint(columnNames = { "board_id", "user_email" }))
public class BoardReactionJpaEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType;

    public BoardReactionJpaEntity() {
    }

    public BoardReactionJpaEntity(Long id, Long boardId, String userEmail, ReactionType reactionType,
            LocalDateTime createdAt) {
        this.id = id;
        this.boardId = boardId;
        this.userEmail = userEmail;
        this.reactionType = reactionType;
        this.setCreatedAt(createdAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public ReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BoardReactionJpaEntity that = (BoardReactionJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardReactionJpaEntity{" +
                "id=" + id +
                ", boardId=" + boardId +
                ", userEmail='" + userEmail + '\'' +
                ", reactionType=" + reactionType +
                '}';
    }
}