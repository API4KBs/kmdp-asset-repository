package edu.mayo.kmdp.repository.graph;

import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.DEFAULT_GRAPH_UUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.repository.artifact.jpa.stores.ArtifactVersionRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryComponentConfig;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties;
import edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphHolder;
import edu.mayo.kmdp.repository.graph.GraphPersistenceTest.TestConfig;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.Swagger2SpringBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = {KnowledgeAssetRepositoryComponentConfig.class, TestConfig.class})
@ActiveProfiles("local")
@TestPropertySource("classpath:application-local.properties")
@TestPropertySource("classpath:application.properties")
class GraphPersistenceTest {

  @Autowired
  KnowledgeAssetRepositoryServerProperties cfg;

  @Autowired
  TestDBProxy db;

  @LocalServerPort
  protected int port;

  private KnowledgeAssetCatalogApi ckac;

  static final UUID GRAPH_ARTIFACT_ID =
      defaultArtifactId(newId(DEFAULT_GRAPH_UUID, VERSION_ZERO), OWL_2).getUuid();
  static final String REPO = "default";


  @BeforeEach
  void init() {
    ApiClientFactory apiClientFactory = new ApiClientFactory("http://localhost:" + port,
        WithFHIR.NONE);

    ckac = KnowledgeAssetCatalogApi.newInstance(apiClientFactory);

    assertNotNull(db);
  }


  @Test
  void testLazyGraphInitialization() {
    db.deleteAll();
    assertFalse(db.graphExists());
    Answer<Void> ans = ckac.clearKnowledgeAssetCatalog();
    assertTrue(ans.isSuccess());
    assertTrue(db.graphExists());
  }

  @Test
  void testGraphInitialization() {
    assertTrue(db.graphExists());
  }

  @Configuration
  public static class TestConfig {

    @Bean
    TestDBProxy dbProxy() {
      return new TestDBProxy();
    }
  }


  public static class TestDBProxy {

    @Autowired
    ArtifactVersionRepository database;


    @Transactional(readOnly = true)
    public boolean graphExists() {
      return database
          .existsByKey_RepositoryIdAndKey_ArtifactId(REPO, GRAPH_ARTIFACT_ID);
    }

    @Transactional
    public void deleteAll() {
      database.deleteAll();
    }

  }

}
