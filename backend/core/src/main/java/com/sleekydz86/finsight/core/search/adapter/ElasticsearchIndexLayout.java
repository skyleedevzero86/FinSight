package com.sleekydz86.finsight.core.search.adapter;

import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class ElasticsearchIndexLayout {

    private static final String TEMPLATE = "/search/finsight-search-index.json";

    private ElasticsearchIndexLayout() {
    }

    static String firstUri(String uris) {
        String raw = uris == null ? "" : uris.split(",")[0].trim();
        if (raw.isEmpty()) {
            throw new RestClientException("Elasticsearch 주소가 비어 있습니다.");
        }
        return raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    }

    static String writeAlias(String readAlias) {
        return requireIndexName(readAlias) + "-write";
    }

    static String initialConcrete(String readAlias) {
        return requireIndexName(readAlias) + "-v1";
    }

    static String nextConcrete(String currentConcrete) {
        String current = requireIndexName(currentConcrete);
        int mark = current.lastIndexOf("-v");
        if (mark < 1 || !current.substring(mark + 2).matches("\\d+")) {
            throw new IllegalArgumentException("물리 인덱스 이름은 이름-v숫자 형식이어야 합니다. 값: " + currentConcrete);
        }
        int version = Integer.parseInt(current.substring(mark + 2));
        return current.substring(0, mark) + "-v" + (version + 1);
    }

    static String indexBody(int dims) {
        return readTemplate().replace("__VECTOR_DIMS__", Integer.toString(clampDims(dims)));
    }

    static String aliasBody(String readAlias, String writeAlias, String concrete) {
        String index = requireIndexName(concrete);
        return "{\"actions\":["
                + addAction(index, requireIndexName(readAlias), false) + ","
                + addAction(index, requireIndexName(writeAlias), true)
                + "]}";
    }

    static String swapAliasBody(String readAlias, String writeAlias, String fromIndex, String toIndex) {
        String read = requireIndexName(readAlias);
        String write = requireIndexName(writeAlias);
        String from = requireIndexName(fromIndex);
        String to = requireIndexName(toIndex);
        return "{\"actions\":["
                + removeAction(from, read) + ","
                + removeAction(from, write) + ","
                + addAction(to, read, false) + ","
                + addAction(to, write, true)
                + "]}";
    }

    private static int clampDims(int dims) {
        return Math.min(4096, Math.max(8, dims));
    }

    private static String requireIndexName(String name) {
        if (name == null || !name.matches("[a-z][a-z0-9_-]{0,80}")) {
            throw new IllegalArgumentException("Elasticsearch 인덱스 이름은 소문자로 시작해야 합니다. 값: " + name);
        }
        return name;
    }

    private static String addAction(String index, String alias, boolean write) {
        if (!write) {
            return "{\"add\":{\"index\":\"" + index + "\",\"alias\":\"" + alias + "\"}}";
        }
        return "{\"add\":{\"index\":\"" + index + "\",\"alias\":\"" + alias + "\",\"is_write_index\":true}}";
    }

    private static String removeAction(String index, String alias) {
        return "{\"remove\":{\"index\":\"" + index + "\",\"alias\":\"" + alias + "\"}}";
    }

    private static String readTemplate() {
        InputStream input = ElasticsearchIndexLayout.class.getResourceAsStream(TEMPLATE);
        if (input == null) {
            throw new IllegalStateException("검색 인덱스 템플릿을 찾을 수 없습니다. 경로: " + TEMPLATE);
        }
        try (input) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("검색 인덱스 템플릿을 읽지 못했습니다. 경로: " + TEMPLATE, exception);
        }
    }
}
