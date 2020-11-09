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

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;

import ch.qos.logback.classic.pattern.MessageConverter;
import com.google.common.util.concurrent.MoreExecutors;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.parsers.html.HtmlDeserializer;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.server.ServerContextAwareHrefBuilder;
import edu.mayo.kmdp.repository.asset.server.configuration.HTMLAdapter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Repository;
import javax.sql.DataSource;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentNodeStoreBuilder;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBOptions;
import org.jsoup.nodes.Document;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

@Configuration
@ComponentScan(basePackageClasses = {
    SemanticKnowledgeAssetRepository.class,
    TransrepresentationExecutor.class})
@PropertySource(value = {"classpath:application.properties"})
public class KnowledgeAssetRepositoryComponentConfig {

  @Value("${artifactTablePrefix:artifacts}")
  private String artifactTablePrefix;

  private static final Logger logger = LoggerFactory
          .getLogger(KnowledgeAssetRepositoryComponentConfig.class);

  @Bean
  public KnowledgeAssetRepositoryServerConfig defaultConfiguration() {
    return new KnowledgeAssetRepositoryServerConfig();
  }

  @Bean
  @Primary
  public HrefBuilder contextAwareHrefBuilder(
      @Autowired KnowledgeAssetRepositoryServerConfig cfg) {
    return new ServerContextAwareHrefBuilder(cfg);
  }

  /**
   * Configures a default instance of {@link KnowledgeArtifactRepositoryService} to be used by the Assert
   * Repoository. This will look for Spring JDBC connection info (such as 'spring.datasource.url'), set as
   * either environment variables or in the application-*.properties files. If ot found will set up a default
   * in-memory datasource that will clear on server restarts.
   *
   * @param dataSource
   * @return
   * @throws SQLException
   */
  @Bean
  @KPServer
  public KnowledgeArtifactRepositoryService jdbcRepository(DataSource dataSource)  {

    try {
      String url = (String) JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> databaseMetaData.getURL());

      logger.info("Knowledge Asset Repository using DB connection: {}", url);
    } catch (MetaDataAccessException e) {
      logger.warn("Unable to access DB Connection info:", e);
    }

    RDBOptions options = new RDBOptions().tablePrefix(this.artifactTablePrefix);

    RDBDocumentNodeStoreBuilder builder = RDBDocumentNodeStoreBuilder.newRDBDocumentNodeStoreBuilder();
    builder.setExecutor(MoreExecutors.directExecutor());

    builder.setRDBConnection(dataSource, options);

    DocumentNodeStore dns = builder.build();

    Repository jcr = new Jcr(new Oak(dns)).createRepository();

    return new JcrKnowledgeArtifactRepository(jcr, new KnowledgeArtifactRepositoryServerConfig());
  }

  @Bean
  HttpMessageConverter<KnowledgeCarrier> knowledgeCarrierToHTMLAdapter() {
    return new HTMLAdapter();
  }
}
