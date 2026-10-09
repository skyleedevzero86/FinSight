package com.sleekydz86.finsight.core.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finsight.elasticsearch")
public class ElasticsearchProperties {

    private boolean enabled = false;
    private String uris = "http://127.0.0.1:9200";
    private String index = "finsight-search";
    private int vectorDims = 768;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUris() {
        return uris;
    }

    public void setUris(String uris) {
        this.uris = uris;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public int getVectorDims() {
        return vectorDims;
    }

    public void setVectorDims(int vectorDims) {
        this.vectorDims = vectorDims;
    }
}
