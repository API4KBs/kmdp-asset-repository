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
package edu.mayo.kmdp.repository.asset.config;

import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.parsers.SurrogateParser;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import edu.mayo.kmdp.tranx.DeserializeApi;
import java.util.Collections;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan(basePackages = {"edu.mayo.kmdp.repository.asset"})
@PropertySource(value = {"classpath:application.test.properties"})
public class IntegrationTestConfig {

  private KnowledgeAssetRepositoryServerConfig cfg = new KnowledgeAssetRepositoryServerConfig()
      .with(KnowledgeAssetRepositoryOptions.SERVER_HOST, "http://localhost:11111");

  @Bean
  public SemanticKnowledgeAssetRepository testRepository() {
    JcrKnowledgeArtifactRepository repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi = KnowledgeArtifactRepositoryApi
        .newInstance(repos);
    KnowledgeArtifactApi knowledgeArtifactApi = KnowledgeArtifactApi.newInstance(repos);
    KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi = KnowledgeArtifactSeriesApi
        .newInstance(repos);

    DeserializeApi parser = DeserializeApi
        .newInstance(new LanguageDeSerializer(Collections.singletonList(new SurrogateParser())));

    return new SemanticKnowledgeAssetRepository(
        knowledgeArtifactRepositoryApi,
        knowledgeArtifactApi,
        knowledgeArtifactSeriesApi,
        parser,
        index,
        cfg);
  }
}
