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

import static edu.mayo.kmdp.util.Util.uuid;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;

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
import java.util.logging.Level;
import javax.jcr.Repository;
import javax.sql.DataSource;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;


class RemoteRepoTest {

  static String artifactTablePrefix = "artifacts";

  static DB_TYPE dbType = DB_TYPE.LOCAL;

  static Logger logger = LoggerFactory.getLogger(RemoteRepoTest.class);

  static final String USER = "USEME";
  static final String PASS = "CHANGEME";


  @Test
  void initAndGetResetAssetByType() {

    java.util.logging.Logger logger = java.util.logging.Logger
        .getLogger("com.microsoft.sqlserver.jdbc");
    logger.setLevel(Level.FINEST);

    ResourceIdentifier id = newId(uuid("foo"), "1");
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id.getUuid(), id.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id)
                .withFormalType(Care_Process_Model)));

    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id.getUuid(), id.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id)
                .withFormalType(Care_Process_Model)));

    ResourceIdentifier id2 = newId(uuid("foo"), "2");
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id2.getUuid(), id2.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id2)
                .withFormalType(Care_Process_Model)));

    long t0 = System.currentTimeMillis();
    System.out.println("READ");
    semanticRepository.getKnowledgeAsset(id.getUuid(), id.getVersionTag());
    semanticRepository.getKnowledgeAsset(id2.getUuid(), id2.getVersionTag());
    System.out.println("READ DONE in " + (System.currentTimeMillis() - t0));

  }


  static DocumentNodeStore dns;

  static SemanticKnowledgeAssetRepository semanticRepository;

  static private JcrKnowledgeArtifactRepository repos;

  static Index index;

  static private JenaSparqlDao jenaSparqlDao;

  static private DataSource ds;

  static protected KnowledgeAssetRepositoryServerConfig cfg = new KnowledgeAssetRepositoryServerConfig();


  @BeforeAll
  static void setUpRepos() {

    ds = getDataSource(dbType);

    repos = jdbcRepository(ds);

//    jenaSparqlDao = new JenaSparqlDao(ds, dbType != DB_TYPE.REMOTE);
    jenaSparqlDao = new JenaSparqlDao(ds, true);

    index = new SparqlIndex(jenaSparqlDao);

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

  @AfterEach
  void shutdown() {
    if (repos != null) {
      repos.shutdown();
    }
    dns.dispose();
  }

  public static JcrKnowledgeArtifactRepository jdbcRepository(DataSource dataSource) {

    RDBOptions options = new RDBOptions().tablePrefix(artifactTablePrefix);

    RDBDocumentNodeStoreBuilder builder = RDBDocumentNodeStoreBuilder
        .newRDBDocumentNodeStoreBuilder();
    builder.setExecutor(MoreExecutors.directExecutor());

    builder.setRDBConnection(dataSource, options);

    dns = builder.build();

    Repository jcr = new Jcr(new Oak(dns)).createRepository();

    return new JcrKnowledgeArtifactRepository(jcr, new KnowledgeArtifactRepositoryServerConfig());
  }

  enum DB_TYPE {
    IN_MEM("mem"),
    LOCAL("localhost\\SQLEXPRESS:52188"),
    REMOTE_DEV("ROEFDN807Q.mayo.edu:1433"),
    REMOTE_INT("ROEFDN906Q.mayo.edu:1433"),
    REMOTE_PROD("ROEFDN025Q.mayo.edu:1433");

    public String server;

    DB_TYPE(String srv) {
      this.server = srv;
    }
  }

  public static DataSource getDataSource(DB_TYPE type) {
    switch (type) {
      case IN_MEM:
        return getH2DataSource();
      case LOCAL:
        return getLocalDataSource();
      case REMOTE_DEV:
        return getRemoteDataSource(DB_TYPE.REMOTE_DEV);
      case REMOTE_INT:
        return getRemoteDataSource(DB_TYPE.REMOTE_INT);
      case REMOTE_PROD:
        return getRemoteDataSource(DB_TYPE.REMOTE_PROD);
      default:
        throw new IllegalArgumentException();
    }
  }

  public static DataSource getRemoteDataSource(DB_TYPE type) {
    String dbName = UUID.randomUUID().toString();

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    dataSourceBuilder.url("jdbc:sqlserver://"
        + type.server
            + ";database=KmdKnowledgeArtifactRepo;"
    );

    dataSourceBuilder.username(USER);
    dataSourceBuilder.password(PASS);

    return dataSourceBuilder.build();
  }

  public static DataSource getH2DataSource() {
    String dbName = UUID.randomUUID().toString();

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("org.h2.Driver");
    dataSourceBuilder.url("jdbc:h2:"+ DB_TYPE.IN_MEM.server + ":" + dbName);
    dataSourceBuilder.username("SA");
    dataSourceBuilder.password("");

    return dataSourceBuilder.build();
  }

  public static DataSource getLocalDataSource() {
    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    dataSourceBuilder.url("jdbc:sqlserver://" +
        DB_TYPE.LOCAL.server
        + ";database=artifacts");
    dataSourceBuilder.username("dsotty");
    dataSourceBuilder.password(PASS);

    return dataSourceBuilder.build();
  }

}
