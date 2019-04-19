package edu.mayo.kmdp.repository.asset;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

public class HrefBuilder {

    private String DEFAULT_HOST = "http://localhost:8080";
    private String host;

    public HrefBuilder() {
        String envHost = System.getProperty("SEMANTIC_REPOSITORY_URL");
        if(StringUtils.isNotBlank(envHost)) {
            this.host = envHost;
        } else {
            this.host = DEFAULT_HOST;
        }

        this.host = StringUtils.removeEnd(this.host, "/");
    }

    public URI getAssetHref(String id) {
        // /cat/assets/{assetId}
        return URI.create(String.format("%s/cat/assets/%s", this.host, id));
    }

    public URI getAssetVersionHref(String id, String version) {
        // /cat/assets/{assetId}/versions/{versionTag}
        return URI.create(String.format("%s/cat/assets/%s/versions/%s", this.host, id, version));
    }

    public URI getAssetCarrierVersionHref(String assetId, String assetVersion, String carrierId, String carrierVersion) {
        return URI.create(String.format("%s/cat/assets/%s/versions/%s/carriers/%s/versions/%s", this.host, assetId, assetVersion, carrierId, carrierVersion));
    }

}
