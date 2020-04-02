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

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.index.sparql.JenaSparqlDao;
import edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex;
import edu.mayo.kmdp.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import edu.mayo.kmdp.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import edu.mayo.kmdp.repository.asset.v4.server.KnowledgeAssetRetrievalApiInternal;
import edu.mayo.kmdp.tranx.v4.server.DeserializeApiInternal;
import edu.mayo.kmdp.tranx.v4.server.DetectApiInternal;
import edu.mayo.kmdp.tranx.v4.server.TransxionApiInternal;
import edu.mayo.kmdp.tranx.v4.server.ValidateApiInternal;
import java.util.Collections;
import java.util.List;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public interface KnowledgeAssetRepositoryService extends KnowledgeAssetCatalogApiInternal,
    KnowledgeAssetRepositoryApiInternal, KnowledgeAssetRetrievalApiInternal {

  static KnowledgeAssetRepositoryService selfContainedRepository() {
    JenaSparqlDao dao = JenaSparqlDao.inMemoryDao();
    return new SemanticKnowledgeAssetRepository(
        KnowledgeArtifactRepositoryService.inMemoryArtifactRepository(),
        new LanguageDeSerializer(Collections.singletonList(new Surrogate2Parser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(Collections.emptyList()),
        new JenaQuery(dao),
        new SparqlIndex(dao),
        new KnowledgeAssetRepositoryServerConfig()
    );
  }

  static KnowledgeAssetRepositoryService selfContainedRepository(
      List<DeserializeApiInternal> parsers,
      List<DetectApiInternal> detectors,
      List<ValidateApiInternal> validators,
      List<TransxionApiInternal> translators
  ) {
    JenaSparqlDao dao = JenaSparqlDao.inMemoryDao();
    return new SemanticKnowledgeAssetRepository(
        KnowledgeArtifactRepositoryService.inMemoryArtifactRepository(),
        new LanguageDeSerializer(parsers),
        new LanguageDetector(detectors),
        new LanguageValidator(validators),
        new TransrepresentationExecutor(translators),
        new JenaQuery(dao),
        new SparqlIndex(dao),
        new KnowledgeAssetRepositoryServerConfig()
    );
  }

  default void publish(
      KnowledgeAsset surrogate,
      KnowledgeCarrier artifact) {

    ResourceIdentifier surrogateId = surrogate.getAssetId();

    this.setVersionedKnowledgeAsset(
        surrogateId.getUuid(),
        surrogateId.getVersionTag(),
        surrogate);

    if (artifact != null) {
      ResourceIdentifier artifactId = artifact.getArtifactId();
      // the main Surrogate may have sub-surrogates, and this artifact may be associated
      // to one of them, rather than the main one
      if (artifact.getAssetId() != null) {
        surrogateId = artifact.getAssetId();
      }
      this.setKnowledgeAssetCarrierVersion(
          surrogateId.getUuid(),
          surrogateId.getVersionTag(),
          artifactId.getUuid(),
          artifactId.getVersionTag(),
          artifact.asBinary()
              .orElseThrow(UnsupportedOperationException::new));
    }
  }
}
