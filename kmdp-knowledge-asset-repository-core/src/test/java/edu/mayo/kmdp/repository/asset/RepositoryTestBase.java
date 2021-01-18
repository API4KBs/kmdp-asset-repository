package edu.mayo.kmdp.repository.asset;

import static java.util.Collections.singletonList;

import com.google.common.util.concurrent.MoreExecutors;
import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2Transcriptor;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.sparql.JenaSparqlDao;
import edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.jcr.Repository;
import javax.sql.DataSource;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBOptions;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.jdbc.DataSourceBuilder;

abstract class RepositoryTestBase {

  static SemanticKnowledgeAssetRepository semanticRepository;

  static private JcrKnowledgeArtifactRepository repos;

  static Index index;

  static private JenaSparqlDao jenaSparqlDao;

  static private DataSource ds;

  @BeforeEach
  void reset() {
    index.reset();
  }

  @AfterAll
  static void tearDownRepos() {
    jenaSparqlDao.shutdown();
  }

  @BeforeAll
  static void setUpRepos() {
    ds = getDataSource();

    RDBOptions options = new RDBOptions().tablePrefix("pre");

    RDBDocumentNodeStoreBuilder b = new RDBDocumentNodeStoreBuilder().newRDBDocumentNodeStoreBuilder();
    b.setExecutor(MoreExecutors.directExecutor());

    b.setRDBConnection(ds, options);

    DocumentNodeStore dns = b.build();

    Repository jcr = new Jcr(new Oak(dns)).with(new OpenSecurityProvider()).createRepository();

    repos = new JcrKnowledgeArtifactRepository(
        jcr,
        new KnowledgeArtifactRepositoryServerConfig());

    jenaSparqlDao = new JenaSparqlDao(ds, true);

    index = new SparqlIndex(jenaSparqlDao);

    KnowledgeAssetRepositoryServerConfig cfg = new KnowledgeAssetRepositoryServerConfig();
    semanticRepository = new SemanticKnowledgeAssetRepository(
        repos,
        new LanguageDeSerializer(
            Arrays.asList(new Surrogate2Parser(), new JenaOwlParser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(
            Arrays.asList(new SurrogateV2toHTMLTranslator(), new SurrogateV2Transcriptor())),
        new JenaQuery(jenaSparqlDao),
        index,
        new HrefBuilder(cfg),
        cfg);
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

  @AfterEach
  void shutdown() {
    if (repos != null) {
      repos.shutdown();
    }
  }

}
