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

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import java.net.URI;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.Swagger2SpringBoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = SemanticRepoIntegrationTestConfig.class)
public abstract class SemanticRepoAPITestBase {

  @Autowired
  KnowledgeAssetRepositoryServerConfig cfg;

  @LocalServerPort
  protected int port;


  protected URI testAssetNS() {
    return cfg.getTyped(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE, URI.class);
  }

  protected URI testArtifactNS() {
    return cfg.getTyped(KnowledgeAssetRepositoryOptions.ARTIFACT_NAMESPACE, URI.class);
  }
}
