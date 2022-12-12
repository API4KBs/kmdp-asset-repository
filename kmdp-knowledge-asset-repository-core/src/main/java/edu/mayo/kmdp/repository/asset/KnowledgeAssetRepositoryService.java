/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.asset;

import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphHolder.newKnowledgeGraphHolder;
import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.newKnowledgeGraphInfo;
import static edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex.newSparqlIndex;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.DeserializeApiOperator;
import edu.mayo.kmdp.language.DetectApiOperator;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransionApiOperator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.ValidateApiOperator;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.index.sparql.JenaSparqlDAO;
import java.util.Collections;
import java.util.List;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * Combination interface that joins KnowledgeAssetCatalogApiInternal
 * and KnowledgeAssetRepositoryApiInternal
 *
 * Provides additional helper methods
 */
public interface KnowledgeAssetRepositoryService extends KnowledgeAssetCatalogApiInternal,
    KnowledgeAssetRepositoryApiInternal {

  /**
   * Factory method that constructs a Knowledge Asset Repository instance with
   * default configuration, components, and in-memory volatile artifact repository database
   *
   * @return a self-contained KnowledgeAssetRepositoryService
   * @see KnowledgeAssetRepositoryService#selfContainedRepository(List, List, List, List)
   */
  static KnowledgeAssetRepositoryService selfContainedRepository() {
    var cfg = new KnowledgeAssetRepositoryServerProperties(
        KnowledgeAssetRepositoryService.class.getResourceAsStream("/application.properties"));
    return selfContainedRepository(cfg);
  }

  /**
   * Self-contained Asset Repository instance used for testing
   *
   * @return
   */
  static KnowledgeAssetRepositoryService mockTestRepository() {
    KnowledgeArtifactRepositoryServerProperties subCfg =
        KnowledgeArtifactRepositoryServerProperties.emptyConfig()
            .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, "test")
            .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME, "Test Artifact Repo");
    KnowledgeAssetRepositoryServerProperties cfg =
        KnowledgeAssetRepositoryServerProperties.emptyProperties()
            .with(KnowledgeAssetRepositoryOptions.CLEARABLE, true)
            .with(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE, "urn:uuid:")
            .with(KnowledgeAssetRepositoryOptions.ARTIFACT_NAMESPACE, "urn:uuid:");
    cfg.putAll(subCfg);
    cfg.put("spring.profiles.active", "jpa");
    cfg.put("spring.jpa.hibernate.ddl-auto", "create");
    return selfContainedRepository(cfg);
  }

  /**
   * Factory method that constructs a Knowledge Asset Repository instance with
   * given configuration, default components, and in-memory volatile artifact repository database
   *
   * @return a self-contained KnowledgeAssetRepositoryService
   * @see KnowledgeAssetRepositoryService#selfContainedRepository(List, List, List, List)
   */
  static KnowledgeAssetRepositoryService selfContainedRepository(
      KnowledgeAssetRepositoryServerProperties cfg) {
    var kgi = newKnowledgeGraphInfo();
    var artifactRepo =
        JPAKnowledgeArtifactRepositoryService.inMemoryArtifactRepository(cfg);
    var kGraphHolder
        = newKnowledgeGraphHolder(artifactRepo, kgi);
    var dao =
        new JenaSparqlDAO(kGraphHolder);
    var index = newSparqlIndex(dao, kgi);
    return new SemanticKnowledgeAssetRepository(
        artifactRepo,
        new LanguageDeSerializer(Collections.singletonList(new Surrogate2Parser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(Collections.emptyList()),
        new JenaQuery(kGraphHolder),
        index,
        kGraphHolder,
        new KARSHrefBuilder(cfg),
        cfg
    );
  }

  /**
   * Factory method that constructs a Knowledge Asset Repository instance with
   * given configuration, components, and in-memory volatile artifact repository database
   *
   * @param parsers the {@link DeserializeApiOperator} used for reading/writing Artifacts.
   *                Must include an operator that supports {@link KnowledgeAsset}
   * @param detectors the {@link DetectApiOperator} used to detect Artifacts's representations
   * @param validators the {@link ValidateApiOperator} used to validate Artifacts
   * @param translators the {@link TransionApiOperator} used to trans* Artifacts as required
   *                    by content negotiation
   * @return a self-contained KnowledgeAssetRepositoryService
   */
  static KnowledgeAssetRepositoryService selfContainedRepository(
      List<DeserializeApiOperator> parsers,
      List<DetectApiOperator> detectors,
      List<ValidateApiOperator> validators,
      List<TransionApiOperator> translators) {
    var kgi = newKnowledgeGraphInfo();
    var cfg = new KnowledgeAssetRepositoryServerProperties(
        KnowledgeAssetRepositoryService.class.getResourceAsStream("/application.properties"));
    var artifactRepo =
        JPAKnowledgeArtifactRepositoryService.inMemoryArtifactRepository(cfg);

    var kGraphHolder
        = newKnowledgeGraphHolder(artifactRepo, kgi);
    var dao =
        new JenaSparqlDAO(kGraphHolder);

    return new SemanticKnowledgeAssetRepository(
        artifactRepo,
        new LanguageDeSerializer(parsers),
        new LanguageDetector(detectors),
        new LanguageValidator(validators),
        new TransrepresentationExecutor(translators),
        new JenaQuery(kGraphHolder),
        newSparqlIndex(dao, kgi),
        kGraphHolder,
        new KARSHrefBuilder(cfg),
        cfg
    );
  }


  /**
   * Helper method that combines the publication of an Asset's Surrogate + Carrier Artifact
   * @param surrogate the Asset metadata
   * @param artifact the Asset manifestation
   */
  default void publish(
      KnowledgeAsset surrogate,
      KnowledgeCarrier artifact) {

    ResourceIdentifier surrogateId = surrogate.getAssetId();

    this.setKnowledgeAssetVersion(
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
