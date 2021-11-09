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

import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.test.MockTranslator;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toLibraryTranslator;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.SemanticRepoAPITestBase.AssetRepositoryTestConfig;
import java.net.URI;
import java.util.Arrays;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.Swagger2SpringBoot;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = {
    KnowledgeAssetRepositoryComponentConfig.class,
    AssetRepositoryTestConfig.class
})
@ActiveProfiles("local")
@TestPropertySource("classpath:application-local.properties")
@TestPropertySource("classpath:application.properties")
public abstract class SemanticRepoAPITestBase {

  @Autowired
  KnowledgeAssetRepositoryServerProperties cfg;

  @LocalServerPort
  protected int port;


  protected URI testAssetNS() {
    return cfg.getTyped(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE, URI.class);
  }

  protected URI testArtifactNS() {
    return cfg.getTyped(KnowledgeAssetRepositoryOptions.ARTIFACT_NAMESPACE, URI.class);
  }

  protected void init() {
    // nothing to do for now
  }

  public static class AssetRepositoryTestConfig {

    @Bean
    @Primary
    @KPServer
    public TransxionApiInternal testTranslator() {
      return new TransrepresentationExecutor(Arrays.asList(
          new SurrogateV2toHTMLTranslator(),
          new SurrogateV2toLibraryTranslator(),
          new MockTranslator()
      ));
    }
  }
}
