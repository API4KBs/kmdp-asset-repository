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

import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.client.ApiClientFactory;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.kmdp.tranx.DetectApi;
import edu.mayo.kmdp.tranx.TransxionApi;
import edu.mayo.kmdp.tranx.server.DeserializeApiDelegate;
import edu.mayo.kmdp.tranx.server.DetectApiDelegate;
import edu.mayo.kmdp.tranx.server.TransxionApiDelegate;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.io.File;
import javax.inject.Inject;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
public class KnowledgeAssetRepositoryComponentConfig {

  private KnowledgeAssetRepositoryServerConfig cfg = new KnowledgeAssetRepositoryServerConfig();

  @Bean
  @Profile({"integration"})
  public SemanticKnowledgeAssetRepository selfContainedRepository() {
    JcrKnowledgeArtifactRepository repos = new JcrKnowledgeArtifactRepository(
        new Jcr(new Oak()).createRepository(),
        new KnowledgeArtifactRepositoryServerConfig());

    MapDbIndex index = new MapDbIndex();

    KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi = KnowledgeArtifactRepositoryApi
        .newInstance(repos);
    KnowledgeArtifactApi knowledgeArtifactApi = KnowledgeArtifactApi.newInstance(repos);
    KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi = KnowledgeArtifactSeriesApi
        .newInstance(repos);

    return new SemanticKnowledgeAssetRepository(
        knowledgeArtifactRepositoryApi,
        knowledgeArtifactApi,
        knowledgeArtifactSeriesApi,
        index,
        cfg);
  }

  @Bean
  @Profile({"default", "inmemory"})
  public SemanticKnowledgeAssetRepository semanticRepository(Index index) throws Exception {
    return new SemanticKnowledgeAssetRepository(
        knowledgeArtifactRepositoryApi(),
        knowledgeArtifactApi(),
        knowledgeArtifactSeriesApi(),
        index,
        cfg);
  }

  @Bean
  @Profile({"default"})
  public Index fileSystemIndex() {
    File dataDir = cfg.getTyped(KnowledgeAssetRepositoryOptions.BASE_DIR);
    if (dataDir != null && !dataDir.exists()) {
      if (!dataDir.mkdirs()) {
        return null;
      }
    }

    File indexFile = new File(dataDir, "index");
    return new MapDbIndex(indexFile);
  }

  @Bean
  @Profile({"inmemory"})
  public Index inMemoryIndex() {
    return new MapDbIndex();
  }

  @Bean
  @Profile({"default", "inmemory"})
  public KnowledgeArtifactApi knowledgeArtifactApi() {
    return KnowledgeArtifactApi.newInstance(apiClient());
  }

  @Bean
  @Profile({"default", "inmemory"})
  public KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi() {
    return KnowledgeArtifactRepositoryApi.newInstance(apiClient());
  }

  @Bean
  @Profile({"default", "inmemory"})
  public KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi() {
    return KnowledgeArtifactSeriesApi.newInstance(apiClient());
  }

  @Bean
  @Profile({"default", "inmemory"})
  public ApiClientFactory apiClient() {
    return new ApiClientFactory(
        cfg.getTyped(KnowledgeAssetRepositoryOptions.SERVER_HOST).toString(), WithFHIR.NONE);
  }


  @Inject
  @KPServer
  DetectApiDelegate detector;

  @Bean
  @KPComponent
  public DetectApi detectApi() {
    return DetectApi.newInstance(detector);
  }


  @Inject
  @KPServer
  TransxionApiDelegate txor;

  @Bean
  @KPComponent
  public TransxionApi executionApi() {
    return TransxionApi.newInstance(txor);
  }


  @Inject
  @KPServer
  DeserializeApiDelegate deser;

  @Bean
  @KPComponent
  public DeserializeApi deserializeApi() {
    return DeserializeApi.newInstance(deser);
  }


}
