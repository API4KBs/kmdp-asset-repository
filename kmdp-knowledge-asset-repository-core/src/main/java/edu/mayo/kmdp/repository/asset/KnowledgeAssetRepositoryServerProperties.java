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
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryServerProperties.KnowledgeArtifactRepositoryOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@SuppressWarnings("unchecked")
public class KnowledgeAssetRepositoryServerProperties extends
    ConfigProperties<KnowledgeAssetRepositoryServerProperties,
        KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions> {

  private static final Properties DEFAULTS = defaulted(KnowledgeAssetRepositoryOptions.class);

  private KnowledgeAssetRepositoryServerProperties() {
    super(DEFAULTS);
  }

  public KnowledgeAssetRepositoryServerProperties(Properties properties) {
    super(properties);
  }

  public KnowledgeAssetRepositoryServerProperties(InputStream propertiesStream) {
    super(defaulted(KnowledgeArtifactRepositoryOptions.class));
    if (propertiesStream != null) {
      try {
        this.load(propertiesStream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static KnowledgeAssetRepositoryServerProperties emptyProperties() {
    return new KnowledgeAssetRepositoryServerProperties(new Properties());
  }

  @Override
  public KnowledgeAssetRepositoryOptions[] properties() {
    return KnowledgeAssetRepositoryOptions.values();
  }

  public enum KnowledgeAssetRepositoryOptions implements
      Option<KnowledgeAssetRepositoryOptions> {

    CLEARABLE(
        Opt.of("allowClearAll",
            Boolean.FALSE.toString(),
            "Flag that, when true, allows clients to perform certain DELETE operations",
            Boolean.class,
            false)),

    ASSET_NAMESPACE(
        Opt.of("edu.mayo.kmdp.repository.asset.namespace",
            Registry.MAYO_ASSETS_BASE_URI,
            "Base namespace used for Assets",
            String.class,
            false)),

    ARTIFACT_NAMESPACE(
        Opt.of("edu.mayo.kmdp.repository.artifact.namespace",
            Registry.MAYO_ARTIFACTS_BASE_URI,
            "Base namespace used for Assets",
            String.class,
            false));

    private Opt<KnowledgeAssetRepositoryOptions> opt;

    KnowledgeAssetRepositoryOptions(Opt<KnowledgeAssetRepositoryOptions> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<KnowledgeAssetRepositoryOptions> getOption() {
      return opt;
    }


  }
}
