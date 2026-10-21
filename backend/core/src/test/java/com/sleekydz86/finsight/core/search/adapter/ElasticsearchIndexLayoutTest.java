package com.sleekydz86.finsight.core.search.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElasticsearchIndexLayoutTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("색인 본문은 노리와 요청한 차원을 쓴다")
    void indexBodyUsesNoriAndRequestedDims() throws Exception {
        // given

        // when
        JsonNode root = mapper.readTree(ElasticsearchIndexLayout.indexBody(768));

        // then
        assertThat(root.at("/settings/analysis/tokenizer/finsight_nori/type").asText()).isEqualTo("nori_tokenizer");
        assertThat(root.at("/settings/analysis/tokenizer/finsight_nori/decompound_mode").asText()).isEqualTo("mixed");
        assertThat(root.at("/mappings/properties/title/analyzer").asText()).isEqualTo("finsight_korean");
        assertThat(root.at("/mappings/properties/embedding/dims").asInt()).isEqualTo(768);
        assertThat(root.at("/mappings/properties/embedding/similarity").asText()).isEqualTo("cosine");
        assertThat(root.at("/mappings/properties/embedding/index_options/type").asText()).isEqualTo("int8_hnsw");
        assertThat(root.at("/settings/index.search.slowlog.threshold.query.warn").asText()).isEqualTo("200ms");
    }

    @Test
    @DisplayName("벡터 차원은 허용 범위 안으로 맞춘다")
    void indexBodyClampsVectorDims() throws Exception {
        // given

        // when

        // then
        assertThat(mapper.readTree(ElasticsearchIndexLayout.indexBody(1))
                .at("/mappings/properties/embedding/dims").asInt()).isEqualTo(8);
        assertThat(mapper.readTree(ElasticsearchIndexLayout.indexBody(9000))
                .at("/mappings/properties/embedding/dims").asInt()).isEqualTo(4096);
    }

    @Test
    @DisplayName("별칭 본문은 쓰기 별칭을 표시한다")
    void aliasBodyMarksWriteAlias() throws Exception {
        // given

        // when
        JsonNode actions = mapper.readTree(ElasticsearchIndexLayout.aliasBody(
                "finsight-search", "finsight-search-write", "finsight-search-v1")).path("actions");

        // then
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).at("/add/alias").asText()).isEqualTo("finsight-search");
        assertThat(actions.get(1).at("/add/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(1).at("/add/is_write_index").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("별칭 교체는 읽기와 쓰기를 함께 옮긴다")
    void swapAliasBodyMovesReadAndWriteTogether() throws Exception {
        // given

        // when
        JsonNode actions = mapper.readTree(ElasticsearchIndexLayout.swapAliasBody(
                "finsight-search", "finsight-search-write", "finsight-search-v1", "finsight-search-v2"))
                .path("actions");

        // then
        assertThat(actions.get(0).at("/remove/index").asText()).isEqualTo("finsight-search-v1");
        assertThat(actions.get(1).at("/remove/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(2).at("/add/index").asText()).isEqualTo("finsight-search-v2");
        assertThat(actions.get(3).at("/add/alias").asText()).isEqualTo("finsight-search-write");
        assertThat(actions.get(3).at("/add/is_write_index").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("다음 실제 색인 이름은 버전을 올린다")
    void nextConcreteIncrementsVersion() {
        // given

        // when

        // then
        assertThat(ElasticsearchIndexLayout.initialConcrete("finsight-search")).isEqualTo("finsight-search-v1");
        assertThat(ElasticsearchIndexLayout.writeAlias("finsight-search")).isEqualTo("finsight-search-write");
        assertThat(ElasticsearchIndexLayout.nextConcrete("finsight-search-v1")).isEqualTo("finsight-search-v2");
        assertThat(ElasticsearchIndexLayout.nextConcrete("finsight-search-v9")).isEqualTo("finsight-search-v10");
    }

    @Test
    @DisplayName("별칭이 될 수 없는 이름은 거절한다")
    void rejectsNamesThatCannotBeAliases() {
        // given

        // when

        // then
        assertThatThrownBy(() -> ElasticsearchIndexLayout.nextConcrete("Finsight"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소문자");
        assertThatThrownBy(() -> ElasticsearchIndexLayout.nextConcrete("finsight-search"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이름-v숫자");
    }

    @Test
    @DisplayName("첫 주소만 연결 주소로 쓴다")
    void firstUriUsesTheFirstAddress() {
        // given

        // when

        // then
        assertThat(ElasticsearchIndexLayout.firstUri("http://127.0.0.1:9200/, http://es:9200"))
                .isEqualTo("http://127.0.0.1:9200");
        assertThat(ElasticsearchIndexLayout.firstUri("http://127.0.0.1:9200/"))
                .isEqualTo("http://127.0.0.1:9200");
        assertThatThrownBy(() -> ElasticsearchIndexLayout.firstUri(" "))
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("비어 있습니다");
    }
}
