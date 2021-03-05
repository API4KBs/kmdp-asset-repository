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

import static org.springframework.jdbc.support.JdbcUtils.extractDatabaseMetaData;

import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.server.ServerContextAwareHrefBuilder;
import edu.mayo.kmdp.repository.asset.server.configuration.HTMLAdapter;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.HttpMessageConverter;
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
      String url = (String) extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
      logger.info("Knowledge Asset Repository using DB connection: {}", url);
    } catch (MetaDataAccessException e) {
      logger.warn("Unable to access DB Connection info:", e);
    }

    return new JPAKnowledgeArtifactRepository(dataSource, new KnowledgeArtifactRepositoryServerConfig());
  }

  @Bean
  HttpMessageConverter<KnowledgeCarrier> knowledgeCarrierToHTMLAdapter() {
    return new HTMLAdapter();
  }
}
