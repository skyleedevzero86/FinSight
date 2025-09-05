package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_scraps", uniqueConstraints = @UniqueConstraint(columnNames = { "board_id", "user_email" }))
public class BoardScrapJpaEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;

    public BoardScrapJpaEntity() {
    }

    public BoardScrapJpaEntity(Long id, Long boardId, String userEmail, LocalDateTime scrapedAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.boardId = boardId;
        this.userEmail = userEmail;
        this.scrapedAt = scrapedAt;
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

    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(LocalDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BoardScrapJpaEntity that = (BoardScrapJpaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardScrapJpaEntity{" +
                "id=" + id +
                ", boardId=" + boardId +
                ", userEmail='" + userEmail + '\'' +
                ", scrapedAt=" + scrapedAt +
                '}';
    }
}