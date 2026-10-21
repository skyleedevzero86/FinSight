package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoardJpaMapperTest {

    private final BoardJpaMapper mapper = new BoardJpaMapper();

    @Test
    @DisplayName("도메인으로 바꿀 때 해시태그를 영속성 컬렉션 밖으로 복사한다")
    void toDomainCopiesHashtagsOutOfThePersistenceCollection() {
        // given
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

        // when
        Board board = mapper.toDomain(entity);
        persistenceHashtags.add("late-change");

        // then
        assertThat(board.getHashtags()).containsExactly("finance", "notice");
        assertThat(board.getHashtags()).isNotSameAs(persistenceHashtags);
    }
}
