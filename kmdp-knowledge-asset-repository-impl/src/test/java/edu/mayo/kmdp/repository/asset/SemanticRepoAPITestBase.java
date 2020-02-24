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

import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import org.junit.jupiter.api.AfterEach;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = edu.mayo.kmdp.repository.asset.v3.server.Swagger2SpringBoot.class)
@ActiveProfiles("test")
@Configuration
@ComponentScan(basePackageClasses = {
        SemanticKnowledgeAssetRepository.class,
        TransrepresentationExecutor.class})
@PropertySource(value = {"classpath:application.test.properties"})
public abstract class SemanticRepoAPITestBase {

  @LocalServerPort
  protected int port;

  @AfterEach
  void init() {

  }

  @Bean
  public KnowledgeAssetRepositoryServerConfig cfg() {
    return new KnowledgeAssetRepositoryServerConfig()
            .with(KnowledgeAssetRepositoryOptions.SERVER_HOST, "http://localhost:" + this.port);
  }

  @Bean
  @KPComponent
  public KnowledgeArtifactRepositoryService artifactRepository() {
    return KnowledgeArtifactRepositoryService.inMemoryArtifactRepository();
  }


}
