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

import static edu.mayo.kmdp.util.Util.toUUID;

import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRetrievalApiDelegate;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public interface KnowledgeAssetRepository extends KnowledgeAssetCatalogApiDelegate,
    KnowledgeAssetRepositoryApiDelegate, KnowledgeAssetRetrievalApiDelegate {

  static KnowledgeAssetRepository newRepository() {
    return new KnowledgeAssetRepositoryComponentConfig(null, null, null)
        .selfContainedRepository();
  }

  default void publish(
      KnowledgeAsset surrogate,
      KnowledgeCarrier artifact) {

    VersionedIdentifier surrogateId = surrogate.getAssetId();

    this.setVersionedKnowledgeAsset(
        toUUID(surrogateId.getTag()),
        surrogateId.getVersion(),
        surrogate);

    if (artifact != null) {
      VersionedIdentifier artifactId = artifact.getArtifactId();
      // the main Surrogate may have sub-surrogates, and this artifact may be associated
      // to one of them, rather than the main one
      if (artifact.getAssetId() != null) {
        surrogateId = artifact.getAssetId();
      }
      this.setKnowledgeAssetCarrierVersion(
          toUUID(surrogateId.getTag()),
          surrogateId.getVersion(),
          toUUID(artifactId.getTag()),
          artifactId.getVersion(),
          ((BinaryCarrier) artifact).getEncodedExpression());
    }
  }
}
