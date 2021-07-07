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
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.dao.ArtifactDAO;
import edu.mayo.kmdp.repository.artifact.jpa.JPAArtifactDAO;
import edu.mayo.kmdp.repository.artifact.jpa.JPAKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.artifact.jpa.entities.ArtifactVersionEntity;
import edu.mayo.kmdp.repository.artifact.jpa.stores.ArtifactVersionRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.server.ServerContextAwareHrefBuilder;
import edu.mayo.kmdp.repository.asset.server.configuration.HTMLAdapter;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan(basePackageClasses = {
    SemanticKnowledgeAssetRepository.class,
    JPAArtifactDAO.class,
    TransrepresentationExecutor.class})
@PropertySource(value = {"classpath:application.properties"})
@EnableJpaRepositories(basePackageClasses = ArtifactVersionRepository.class)
@EntityScan(basePackageClasses = ArtifactVersionEntity.class)
@EnableTransactionManagement
public class KnowledgeAssetRepositoryComponentConfig {

  @Value("${edu.mayo.kmdp.repository.asset.namespace}")
  private String assetNamespace;

  @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
  private String artifactRepoId;
  @Value("${edu.mayo.kmdp.repository.artifact.name:Default Artifact Repository}")
  private String artifactRepoName;
  @Value("${edu.mayo.kmdp.repository.artifact.namespace}")
  private String artifactNamespace;


  private static final Logger logger = LoggerFactory
      .getLogger(KnowledgeAssetRepositoryComponentConfig.class);

  @Bean
  public KnowledgeAssetRepositoryServerProperties defaultConfiguration() {
    return KnowledgeAssetRepositoryServerProperties.emptyProperties()
        .with(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE, assetNamespace);
  }

  @Bean
  @Primary
  public HrefBuilder contextAwareHrefBuilder(
      @Autowired KnowledgeAssetRepositoryServerProperties cfg) {
    return new ServerContextAwareHrefBuilder(cfg);
  }


  @Bean
  KnowledgeArtifactRepositoryServerProperties artifactConfig(
      @Autowired ConfigurableEnvironment env) {
    KnowledgeArtifactRepositoryServerProperties artifactConfig = KnowledgeArtifactRepositoryServerProperties
        .emptyConfig()
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID, artifactRepoId)
        .with(KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_NAME, artifactRepoName)
        .with(KnowledgeArtifactRepositoryOptions.BASE_NAMESPACE, artifactNamespace);

    MutablePropertySources propSrcs = env.getPropertySources();
    StreamSupport.stream(propSrcs.spliterator(), false)
        .filter(ps -> ps instanceof EnumerablePropertySource)
        .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
        .flatMap(Arrays::stream)
        .forEach(propName -> artifactConfig.setProperty(propName, env.getProperty(propName)));

    return artifactConfig;
  }

  /**
   * Configures a default instance of {@link KnowledgeArtifactRepositoryService} to be used by the Assert
   * Repoository. This will look for Spring JDBC connection info (such as 'spring.datasource.url'), set as
   * either environment variables or in the application-*.properties files. If not found will set up a default
   * in-memory datasource that will clear on server restarts.
   *
   * @param dataSource
   * @return
   * @throws SQLException
   */
  @Bean
  @KPServer
  public KnowledgeArtifactRepositoryService jdbcRepository(
      @Autowired ArtifactDAO dao,
      @Autowired DataSource dataSource,
      @Autowired KnowledgeArtifactRepositoryServerProperties artifactConfig) {

    try {
      String url = (String) extractDatabaseMetaData(dataSource, DatabaseMetaData::getURL);
      logger.info("Knowledge Asset Repository using DB connection: {}", url);
    } catch (MetaDataAccessException e) {
      logger.warn("Unable to access DB Connection info:", e);
    }

    return new JPAKnowledgeArtifactRepository(dao, artifactConfig);
  }

  @Bean
  HttpMessageConverter<KnowledgeCarrier> knowledgeCarrierToHTMLAdapter() {
    return new HTMLAdapter();
  }
}
