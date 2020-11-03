package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.language.TransrepresentationExecutor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {
    SemanticKnowledgeAssetRepository.class,
    JenaQuery.class,
    TransrepresentationExecutor.class})
@EnableAutoConfiguration
public class SemanticRepoIntegrationTestConfig {

  @Bean
  public KnowledgeAssetRepositoryServerConfig cfg() {
    return new KnowledgeAssetRepositoryServerConfig();
  }

}