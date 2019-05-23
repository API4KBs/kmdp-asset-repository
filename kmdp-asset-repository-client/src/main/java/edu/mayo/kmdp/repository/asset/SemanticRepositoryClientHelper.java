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

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.utils.JsonRestWSUtils;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public class SemanticRepositoryClientHelper {

  private KnowledgeAssetCatalogApi knowledgeAssetCatalogApi;

  private KnowledgeAssetRepositoryApi assetRepositoryApi;

  public SemanticRepositoryClientHelper(KnowledgeAssetCatalogApi knowledgeAssetCatalogApi,
      KnowledgeAssetRepositoryApi semanticKnowledgeRepositoryApi) {
    this.knowledgeAssetCatalogApi = knowledgeAssetCatalogApi;
    this.assetRepositoryApi = semanticKnowledgeRepositoryApi;
  }


  public SemanticRepositoryClientHelper(String repoUrl) {
    ApiClient webClient = JsonRestWSUtils
        .configuredClient(ApiClient.class, JsonRestWSUtils.WithFHIR.DSTU2).setBasePath(repoUrl);
    this.knowledgeAssetCatalogApi = KnowledgeAssetCatalogApi.newInstance(webClient);
    this.assetRepositoryApi = KnowledgeAssetRepositoryApi.newInstance(webClient);
  }

  public void saveToRepo(KnowledgeAsset surrogate, KnowledgeCarrier artifact,
      URIIdentifier associateArtifactTo) {

    URIIdentifier surrogateId = surrogate.getAssetId();

    assetRepositoryApi.initKnowledgeAsset(surrogate);

    if (artifact != null) {
      URIIdentifier artifactId = artifact.getAssetId();

      String assetId;
      String assetVersion;

      if (associateArtifactTo != null) {
        assetId = associateArtifactTo.getTag();
        assetVersion = associateArtifactTo.getVersion();
      } else {
        assetId = surrogateId.getTag();
        assetVersion = surrogateId.getVersion();
      }

      assetRepositoryApi.setKnowledgeAssetCarrierVersion(
          assetId,
          assetVersion,
          artifactId.getTag(),
          artifactId.getVersion(),
          ((BinaryCarrier) artifact).getEncodedExpression());
    }
  }


}
