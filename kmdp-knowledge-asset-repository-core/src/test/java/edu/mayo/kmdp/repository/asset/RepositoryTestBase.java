package edu.mayo.kmdp.repository.asset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2Transcriptor;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.sparql.JenaSparqlDAO;
import edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphHolder;
import edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.boot.jdbc.DataSourceBuilder;

abstract class RepositoryTestBase {

  protected static SemanticKnowledgeAssetRepository semanticRepository;

  protected static KnowledgeArtifactRepositoryService repos;

  protected static Index index;

  protected static JenaSparqlDAO jenaSparqlDao;

  protected static KnowledgeGraphHolder kgHolder;

  protected static DataSource ds;

  protected static KnowledgeArtifactRepositoryServerProperties artifactCfg;

  protected static KnowledgeAssetRepositoryServerProperties assetCfg;

  @BeforeEach
  void reset() {
    semanticRepository.clearKnowledgeAssetCatalog();
  }

  @AfterAll
  static void tearDownRepos() {
    jenaSparqlDao.shutdown();
    if (repos != null) {
      repos.shutdown();
    }
  }

  @BeforeAll
  static void setUpRepos() {

    assetCfg = new KnowledgeAssetRepositoryServerProperties(
        RepositoryTestBase.class.getResourceAsStream("/application.test.properties"));
    artifactCfg = new KnowledgeArtifactRepositoryServerProperties(assetCfg);

    ds = getDataSource();

    repos = new JPAKnowledgeArtifactRepository(ds, artifactCfg);

    kgHolder = new KnowledgeGraphHolder(repos);

    jenaSparqlDao = new JenaSparqlDAO(kgHolder);

    index = new SparqlIndex(jenaSparqlDao);

    semanticRepository = new SemanticKnowledgeAssetRepository(
        repos,
        new LanguageDeSerializer(
            Arrays.asList(new Surrogate2Parser(), new JenaOwlParser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(
            Arrays.asList(new SurrogateV2toHTMLTranslator(), new SurrogateV2Transcriptor())),
        new JenaQuery(kgHolder),
        index,
        kgHolder,
        new HrefBuilder(assetCfg),
        assetCfg);

    ensureInitialized();
  }

  private static void ensureInitialized() {
    KnowledgeCarrier kgraph = semanticRepository.getKnowledgeGraph()
        .orElseGet(Assertions::fail);
    assertTrue(kgraph.asString().orElse("")
        .contains("https://www.omg.org/spec/API4KP/api4kp/KnowledgeAsset"));
  }

  public static DataSource getDataSource() {
    String dbName = UUID.randomUUID().toString();

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("org.h2.Driver");
    dataSourceBuilder.url("jdbc:h2:mem:" + dbName);
    dataSourceBuilder.username("SA");
    dataSourceBuilder.password("");

    return dataSourceBuilder.build();
  }

  static URI testAssetNS() {
    return assetCfg.getTyped(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE, URI.class);
  }

  static URI testArtifactNS() {
    return assetCfg.getTyped(KnowledgeAssetRepositoryOptions.ARTIFACT_NAMESPACE, URI.class);
  }

}
