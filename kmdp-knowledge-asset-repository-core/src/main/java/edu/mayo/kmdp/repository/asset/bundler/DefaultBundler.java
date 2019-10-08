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
package edu.mayo.kmdp.repository.asset.bundler;

import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.Bundler;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

public class DefaultBundler implements Bundler {

  private SemanticKnowledgeAssetRepository coreApi;

  public DefaultBundler(SemanticKnowledgeAssetRepository coreApi) {
    super();
    this.coreApi = coreApi;
  }

  @Override
  public List<KnowledgeCarrier> bundle(UUID assetId, String version) {
    KnowledgeAsset asset = this.coreApi.getVersionedKnowledgeAsset(assetId, version).getBody();

    KnowledgeCarrier carrier = this.coreApi
        .getCanonicalKnowledgeAssetCarrier(assetId, version, null).getBody();

    List<KnowledgeCarrier> returnList = Lists.newArrayList();

    if (carrier != null) {
      returnList.add(carrier);
    }

    Set<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> dependencies = SurrogateHelper
        .closure(asset, false);

    dependencies.forEach(x -> retrieveCarriers(x, returnList));

    return returnList;
  }

  private void retrieveCarriers(KnowledgeAsset x, List<KnowledgeCarrier> returnList) {
    URIIdentifier uriIdentifier = x.getAssetId();

    if (uriIdentifier != null) {
      VersionIdentifier id = DatatypeHelper.toVersionIdentifier(uriIdentifier);
      returnList.add(
          this.coreApi.getCanonicalKnowledgeAssetCarrier(
              Util.ensureUUID(id.getTag())
                  .orElseThrow(IllegalStateException::new),
              id.getVersion(),
              null)
              .getBody());
    } else {
      returnList.addAll(this.getAnonymousArtifacts(x));
    }
  }

  private List<KnowledgeCarrier> getAnonymousArtifacts(KnowledgeAsset assetSurrogate) {
    List<KnowledgeCarrier> carriers = Lists.newArrayList();

    if (assetSurrogate.getCarriers() != null) {
      assetSurrogate.getCarriers().stream()
          .filter(ComputableKnowledgeArtifact.class::isInstance)
          .map(ComputableKnowledgeArtifact.class::cast)
          .forEach(carrier -> {
            URI masterLocation = carrier.getLocator();
            if (masterLocation != null) {
              BinaryCarrier newCarrier = new BinaryCarrier();
              if (carrier.getRepresentation() != null
                  && carrier.getRepresentation().getLanguage() != null) {
                KnowledgeRepresentationLanguage language = carrier.getRepresentation()
                    .getLanguage();
                newCarrier.setRepresentation(rep(language));
              }
              newCarrier
                  .withAssetId(assetSurrogate.getAssetId())
                  .withEncodedExpression(FileUtil.readBytes(masterLocation).orElse(new byte[0]));
              carriers.add(newCarrier);
            }
          });
    }

    return carriers;
  }

}
