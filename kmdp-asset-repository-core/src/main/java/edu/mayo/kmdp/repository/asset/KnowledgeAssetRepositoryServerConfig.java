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

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerConfig.KnowledgeArtifactRepositoryOptions;
import edu.mayo.kmdp.util.Util;
import java.io.File;
import java.net.URL;
import java.util.Properties;


public class KnowledgeAssetRepositoryServerConfig extends
    ConfigProperties<KnowledgeAssetRepositoryServerConfig, KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions> {

  private static final Properties defaults = defaulted(KnowledgeAssetRepositoryOptions.class);

  public KnowledgeAssetRepositoryServerConfig() {
    super(defaults);
  }

  @Override
  public KnowledgeAssetRepositoryOptions[] properties() {
    return KnowledgeAssetRepositoryOptions.values();
  }

  public enum KnowledgeAssetRepositoryOptions implements
      Option<KnowledgeAssetRepositoryOptions> {

    DEFAULT_REPOSITORY_ID(
        Opt.of("http://edu.mayo.kmdp/assetRepository/artifactRepositoryIdentifier",
            getDefaultRepositoryId(),
            "ID of the default Artifact Repository that the Asset Repository will link to",
            String.class,
            false)),

    SERVER_HOST(
        Opt.of("http://edu.mayo.kmdp/assetRepository/host",
            getHost(),
            "Host",
            URL.class,
            false)),

    BASE_DIR(
        Opt.of("http://edu.mayo.kmdp/assetRepository/filesystem/directory",
            getConfigDir().getAbsolutePath(),
            "Root directory for filesystem-based asset/artifct indexes",
            File.class,
            false));

    private Opt<KnowledgeAssetRepositoryOptions> opt;

    KnowledgeAssetRepositoryOptions(Opt<KnowledgeAssetRepositoryOptions> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<KnowledgeAssetRepositoryOptions> getOption() {
      return opt;
    }


    private static String getHost() {
      String envHost = System.getProperty("http://edu.mayo.kmdp/assetRepository/host");
      return !Util.isEmpty(envHost) ? envHost : "http://localhost:8080";
    }

    private static String getDefaultRepositoryId() {
      return new KnowledgeArtifactRepositoryServerConfig().getTyped(
          KnowledgeArtifactRepositoryOptions.DEFAULT_REPOSITORY_ID);
    }

    private static File getConfigDir() {
      File home = null;

      String repoHome = System
          .getProperty("http://edu.mayo.kmdp/assetRepository/repositoryHomeDir");

      if (Util.isEmpty(repoHome)) {
        repoHome = System.getProperty("user.home");
      }

      if (!Util.isEmpty(repoHome)) {
        home = new File(repoHome, ".assetIndex");
        if (!home.exists()) {
          home = home.mkdirs() ? home : null;
        }
      }

      return home;
    }

  }
}
