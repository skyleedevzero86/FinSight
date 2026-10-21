package com.sleekydz86.finsight.core.board.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BoardViewCountSqlConcurrencyTest {

    private static final String URL = "jdbc:h2:mem:viewcount;DB_CLOSE_DELAY=-1;MODE=MySQL";

    @Test
    @DisplayName("같은 글을 100명이 동시에 증가하면 조회수는 100이다")
    void oneHundredConcurrentIncrementsMakeViewCountOneHundred() throws Exception {
        // given
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS boards");
            statement.execute("CREATE TABLE boards (id BIGINT PRIMARY KEY, view_count INT NOT NULL)");
            statement.execute("INSERT INTO boards (id, view_count) VALUES (1, 7)");
        }
        ExecutorService pool = Executors.newFixedThreadPool(100);
        List<Future<Integer>> updates = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            updates.add(pool.submit(this::incrementOnce));
        }

        // then
        for (Future<Integer> update : updates) {
            assertThat(update.get(30, TimeUnit.SECONDS)).isEqualTo(1);
        }

        // when
        pool.shutdown();

        // then
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT view_count FROM boards WHERE id = 1")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(107);
        }
    }

    private int incrementOnce() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE boards SET view_count = view_count + 1 WHERE id = ?")) {
            statement.setLong(1, 1L);
            return statement.executeUpdate();
        }
    }
}
