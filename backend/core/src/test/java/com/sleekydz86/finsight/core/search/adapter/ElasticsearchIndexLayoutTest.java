package com.sleekydz86.finsight.core.search.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElasticsearchIndexLayoutTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void indexBodyUsesNoriAndRequestedDims() throws Exception {
        JsonNode root = mapper.readTree(ElasticsearchIndexLayout.indexBody(768));

        assertThat(root.at("/settings/analysis/tokenizer/finsight_nori/type").asText()).isEqualTo("nori_tokenizer");
        assertThat(root.at("/settings/analysis/tokenizer/finsight_nori/decompound_mode").asText()).isEqualTo("mixed");
        assertThat(root.at("/mappings/properties/title/analyzer").asText()).isEqualTo("finsight_korean");
        assertThat(root.at("/mappings/properties/embedding/dims").asInt()).isEqualTo(768);
        assertThat(root.at("/mappings/properties/embedding/similarity").asText()).isEqualTo("cosine");
        assertThat(root.at("/mappings/properties/embedding/index_options/type").asText()).isEqualTo("int8_hnsw");
        assertThat(root.at("/settings/index.search.slowlog.threshold.query.warn").asText()).isEqualTo("200ms");
    }

    @Test
    void indexBodyClampsVectorDims() throws Exception {
        assertThat(mapper.readTree(ElasticsearchIndexLayout.indexBody(1))
                .at("/mappings/properties/embedding/dims").asInt()).isEqualTo(8);
        assertThat(mapper.readTree(ElasticsearchIndexLayout.indexBody(9000))
                .at("/mappings/properties/embedding/dims").asInt()).isEqualTo(4096);
    }

    @Test
    void aliasBodyMarksWriteAlias() throws Exception {
        JsonNode actions = mapper.readTree(ElasticsearchIndexLayout.aliasBody(
                "finsight-search", "finsight-search-write", "finsight-search-v1")).path("actions");

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).at("/add/alias").asText()).isEqualTo("finsight-search");
        assertThat(actions.get(1).at("/add/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(1).at("/add/is_write_index").asBoolean()).isTrue();
    }

    @Test
    void swapAliasBodyMovesReadAndWriteTogether() throws Exception {
        JsonNode actions = mapper.readTree(ElasticsearchIndexLayout.swapAliasBody(
                "finsight-search", "finsight-search-write", "finsight-search-v1", "finsight-search-v2"))
                .path("actions");

        assertThat(actions.get(0).at("/remove/index").asText()).isEqualTo("finsight-search-v1");
        assertThat(actions.get(1).at("/remove/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(2).at("/add/index").asText()).isEqualTo("finsight-search-v2");
        assertThat(actions.get(3).at("/add/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(3).at("/add/is_write_index").asBoolean()).isTrue();
    }

    @Test
    void nextConcreteIncrementsVersion() {
        assertThat(ElasticsearchIndexLayout.initialConcrete("finsight-search")).isEqualTo("finsight-search-v1");
        assertThat(ElasticsearchIndexLayout.writeAlias("finsight-search")).isEqualTo("finsight-search-write");
        assertThat(ElasticsearchIndexLayout.nextConcrete("finsight-search-v1")).isEqualTo("finsight-search-v2");
        assertThat(ElasticsearchIndexLayout.nextConcrete("finsight-search-v9")).isEqualTo("finsight-search-v10");
    }

    @Test
    void rejectsNamesThatCannotBeAliases() {
        assertThatThrownBy(() -> ElasticsearchIndexLayout.nextConcrete("Finsight"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소문자");
        assertThatThrownBy(() -> ElasticsearchIndexLayout.nextConcrete("finsight-search"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이름-v숫자");
    }

    @Test
    void firstUriUsesTheFirstAddress() {
        assertThat(ElasticsearchIndexLayout.firstUri("http://127.0.0.1:9200/, http://es:9200"))
                .isEqualTo("http://127.0.0.1:9200");
        assertThat(ElasticsearchIndexLayout.firstUri("http://127.0.0.1:9200/"))
                .isEqualTo("http://127.0.0.1:9200");
        assertThatThrownBy(() -> ElasticsearchIndexLayout.firstUri(" "))
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("비어 있습니다");
    }
}
