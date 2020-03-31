package edu.mayo.kmdp.repository.asset;

import com.google.common.util.concurrent.MoreExecutors;
import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageDetector;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.surrogate.v1.SurrogateParser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.sparql.JenaSparqlDao;
import edu.mayo.kmdp.repository.asset.index.sparql.SparqlIndex;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBOptions;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.jcr.Repository;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.UUID;

abstract class RepositoryTestBase {

  SemanticKnowledgeAssetRepository semanticRepository;

  private JcrKnowledgeArtifactRepository repos;

  private JenaSparqlDao jenaSparqlDao;

  private DataSource ds;

  @AfterEach
  void tearDownRepos() {
    jenaSparqlDao.shutdown();
  }

  @BeforeEach
  void setUpRepos() {
    ds = this.getDataSource();

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

    Index index = new SparqlIndex(jenaSparqlDao);

    semanticRepository = new SemanticKnowledgeAssetRepository(
        repos,
        new LanguageDeSerializer(
            Collections.singletonList(new SurrogateParser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(Collections.emptyList()),
        new JenaQuery(jenaSparqlDao),
        index,
        new KnowledgeAssetRepositoryServerConfig());
  }

  public DataSource getDataSource() {
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
