package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.surrogate.v1.SurrogateParser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import java.util.Collections;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class RepositoryTestBase {

  SemanticKnowledgeAssetRepository semanticRepository;

  private JcrKnowledgeArtifactRepository repos;

  @BeforeEach
  void setUpRepos() {
    repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    semanticRepository = new SemanticKnowledgeAssetRepository(
        repos,
        new LanguageDeSerializer(
            Collections.singletonList(new SurrogateParser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(Collections.emptyList()),
        index,
        new KnowledgeAssetRepositoryServerConfig());
  }

  @AfterEach
  void shutdown() {
    if (repos != null) {
      repos.shutdown();
    }
  }

}
