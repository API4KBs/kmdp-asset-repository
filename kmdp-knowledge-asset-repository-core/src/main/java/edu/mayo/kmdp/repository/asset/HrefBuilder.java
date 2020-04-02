/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.repository.asset;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.services.URIPathHelper;

public class HrefBuilder {

  private String host;

  public HrefBuilder(KnowledgeAssetRepositoryServerConfig cfg) {
    this.host = StringUtils
        .removeEnd(cfg.getTyped(
            KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions.SERVER_HOST)
            .toString(), "/");
  }

  public URI getAssetHref(String id) {
    return URI.create(String.format("%s/cat/assets/%s", this.host, id));
  }

  public URI getAssetVersionHref(String id, String version) {
    return URI.create(String.format("%s/cat/assets/%s/versions/%s", this.host, id, version));
  }

  public URI getAssetCarrierVersionHref(String assetId, String assetVersion, String carrierId,
      String carrierVersion) {
    return URI.create(String
        .format("%s/cat/assets/%s/versions/%s/carriers/%s/versions/%s", this.host, assetId,
            assetVersion, carrierId, carrierVersion));
  }

  public URI getArtifactRef(String repositoryId, String artifactId, String version) {
    return URI
        .create(URIPathHelper.knowledgeArtifactLocation(host, repositoryId, artifactId, version));
  }

  public URL getSurrogateRef(UUID assetId, String versionTag, UUID surrogateId, String surrogateVersionTag) {
    try {
      return URI.create(String
          .format("%s/cat/assets/%s/versions/%s/surrogate/%s/versions/%s", this.host, assetId,
              versionTag, surrogateId, surrogateVersionTag)).toURL();
    } catch (MalformedURLException e) {
      return null;
    }
  }
}
