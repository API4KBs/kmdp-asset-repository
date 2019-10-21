package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.parsers.SurrogateParser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import edu.mayo.kmdp.tranx.DeserializeApi;
import java.util.Collections;
import javax.inject.Inject;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class RepositoryTestBase {

  @Inject
  protected SemanticKnowledgeAssetRepository semanticRepository;

  private JcrKnowledgeArtifactRepository repos;

  @BeforeEach
  void setUpRepos() {
    repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(), new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi =
        KnowledgeArtifactRepositoryApi.newInstance(repos);
    KnowledgeArtifactApi knowledgeArtifactApi =
        KnowledgeArtifactApi.newInstance(repos);
    KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi =
        KnowledgeArtifactSeriesApi.newInstance(repos);
    DeserializeApi parserApi = DeserializeApi.newInstance(new LanguageDeSerializer(
        Collections.singletonList(new SurrogateParser())));

    semanticRepository = new SemanticKnowledgeAssetRepository(knowledgeArtifactRepositoryApi,
        knowledgeArtifactApi,
        knowledgeArtifactSeriesApi,
        parserApi,
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
