package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoardJpaMapperTest {

    private final BoardJpaMapper mapper = new BoardJpaMapper();

    @Test
    void toDomainCopiesHashtagsOutOfThePersistenceCollection() {
        List<String> persistenceHashtags = new ArrayList<>(List.of("finance", "notice"));
        BoardJpaEntity entity = new BoardJpaEntity(
                1L,
                "title",
                "content",
                "author@example.com",
                BoardType.NOTICE,
                BoardStatus.ACTIVE,
                0,
                0,
                0,
                0,
                0,
                persistenceHashtags,
                LocalDateTime.now(),
                LocalDateTime.now());

        Board board = mapper.toDomain(entity);
        persistenceHashtags.add("late-change");

        assertThat(board.getHashtags()).containsExactly("finance", "notice");
        assertThat(board.getHashtags()).isNotSameAs(persistenceHashtags);
    }
}
