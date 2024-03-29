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
package edu.mayo.kmdp.repository.asset.performance;

import static edu.mayo.kmdp.repository.asset.index.sparql.DefaultKnowledgeGraphHolder.newKnowledgeGraphHolder;
import static edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo.newKnowledgeGraphInfo;
import static edu.mayo.kmdp.repository.asset.index.sparql.impl.SparqlIndex.newSparqlIndex;
import static edu.mayo.kmdp.util.Util.uuid;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;

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
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.sparql.DefaultKnowledgeGraphHolder;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.sparql.impl.JenaSparqlDAO;
import edu.mayo.kmdp.repository.asset.index.sparql.KnowledgeGraphInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.boot.jdbc.DataSourceBuilder;

class RemoteRepoTest {

  static DB_TYPE dbType = DB_TYPE.IN_MEM;

  static final String USER = "USEME";

  static final String PASS = "CHANGEME";

  @Test
  void initAndGetResetAssetByType() {

    System.out.println("INIT A1");
    long t0 = System.currentTimeMillis();
    ResourceIdentifier id = newId(uuid("foo"), "1");
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id.getUuid(), id.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id)
                .withFormalType(Care_Process_Model)));
    System.out.println("INIT A1 DONE in " + (System.currentTimeMillis() - t0));

    System.out.println("UPDATE A1");
    long t1 = System.currentTimeMillis();
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id.getUuid(), id.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id)
                .withFormalType(Care_Process_Model)));
    System.out.println("UPDATE A1 DONE in " + (System.currentTimeMillis() - t1));

    System.out.println("INIT A2");
    long t2 = System.currentTimeMillis();
    ResourceIdentifier id2 = newId(uuid("foo"), "2");
    assertNotNull(semanticRepository
        .setKnowledgeAssetVersion(id2.getUuid(), id2.getVersionTag(),
            new KnowledgeAsset()
                .withAssetId(id2)
                .withFormalType(Care_Process_Model)));
    System.out.println("INIT A2 DONE in " + (System.currentTimeMillis() - t2));

    System.out.println("READ");
    long t3 = System.currentTimeMillis();
    semanticRepository.getKnowledgeAsset(id.getUuid(), id.getVersionTag());
    semanticRepository.getKnowledgeAsset(id2.getUuid(), id2.getVersionTag());
    System.out.println("READ DONE in " + (System.currentTimeMillis() - t3));

  }


  static SemanticKnowledgeAssetRepository semanticRepository;

  static Index index;

  static protected KnowledgeAssetRepositoryServerProperties cfg
      = new KnowledgeAssetRepositoryServerProperties(
      RemoteRepoTest.class.getResourceAsStream("/application.test.properties"));


  @BeforeAll
  static void setUpRepos() {

    DataSource ds = getDataSource(dbType);

    JPAKnowledgeArtifactRepository artifactRepo = jpaRepository(ds);

    KnowledgeGraphInfo kgi = newKnowledgeGraphInfo();

    DefaultKnowledgeGraphHolder knowledgeGraphHelper = newKnowledgeGraphHolder(artifactRepo, kgi);

    JenaSparqlDAO jenaSparqlDao = new JenaSparqlDAO(knowledgeGraphHelper);

    index = newSparqlIndex(jenaSparqlDao, kgi);

    semanticRepository = new SemanticKnowledgeAssetRepository(
        artifactRepo,
        new LanguageDeSerializer(
            Arrays.asList(new Surrogate2Parser(), new JenaOwlParser())),
        new LanguageDetector(Collections.emptyList()),
        new LanguageValidator(Collections.emptyList()),
        new TransrepresentationExecutor(
            Arrays.asList(new SurrogateV2toHTMLTranslator(), new SurrogateV2Transcriptor())),
        new JenaQuery(knowledgeGraphHelper),
        index,
        knowledgeGraphHelper,
        new KARSHrefBuilder(cfg),
        cfg);
  }

  public static JPAKnowledgeArtifactRepository jpaRepository(DataSource dataSource) {
    return new JPAKnowledgeArtifactRepository(
        dataSource,
        new KnowledgeArtifactRepositoryServerProperties(cfg));
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
    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    dataSourceBuilder.url("jdbc:sqlserver://"
        + type.server
        + ";database=KmdKnowledgeArtifactRepo;sendStringParametersAsUnicode=false;"
    );

    dataSourceBuilder.username(USER);
    dataSourceBuilder.password(PASS);

    return dataSourceBuilder.build();
  }

  public static DataSource getH2DataSource() {
    String dbName = UUID.randomUUID().toString();

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("org.h2.Driver");
    dataSourceBuilder.url("jdbc:h2:" + DB_TYPE.IN_MEM.server + ":" + dbName);
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
    dataSourceBuilder.username(USER);
    dataSourceBuilder.password(PASS);

    return dataSourceBuilder.build();
  }

}
