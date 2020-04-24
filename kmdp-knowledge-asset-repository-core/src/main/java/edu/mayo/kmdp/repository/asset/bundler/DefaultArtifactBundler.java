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
import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultArtifactBundler {

  private static final Logger logger = LoggerFactory
          .getLogger(DefaultArtifactBundler.class);

  private SemanticKnowledgeAssetRepository assetRepository;
  private Index index;

  public DefaultArtifactBundler(SemanticKnowledgeAssetRepository assetRepository, Index index) {
    super();
    this.assetRepository = assetRepository;
    this.index = index;
  }

  public Answer<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID assetId, String versionTag,
      String assetRelationshipTag, Integer depth) {

    ResourceIdentifier uriId = SurrogateBuilder.assetId(assetId,versionTag);

    Set<KnowledgeAsset> dependencies =
        this.index.getRelatedAssets(uriId).stream()
            .map(pointer ->
                assetRepository.getKnowledgeAssetVersion(
                    pointer.getUuid(), pointer.getVersionTag()))
            .filter(Answer::isSuccess)
            .map(Answer::get)
            .collect(Collectors.toSet());

    return Answer.of(new ArrayList<>(dependencies));
  }


  public Answer<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId, String versionTag,
      String assetRelationship, Integer depth, String xAccept) {

    KnowledgeAsset asset = this.assetRepository.getKnowledgeAssetVersion(assetId, versionTag)
        .orElseThrow(IllegalStateException::new);

    ResourceIdentifier uriId = asset.getAssetId();

    Set<KnowledgeAsset> dependencies =
            this.index.getRelatedAssets(uriId).stream()
            .map(pointer -> {
              Answer<KnowledgeAsset> foundRelation
                  = assetRepository.getKnowledgeAssetVersion(pointer.getUuid(), pointer.getVersionTag());

              // TODO: We want to be smarter about this and fail if important dependencies are missing.
              if (! foundRelation.isSuccess()) {
                logger.warn("Related asset not found, FROM: {}, TO: {}", asset.getAssetId().getVersionId(), pointer.getVersionId());
              }

              return foundRelation;
            })
            .filter(Answer::isSuccess)
            .map(Answer::get)
            .collect(Collectors.toSet());

    List<KnowledgeCarrier> returnList = Lists.newArrayList();

    dependencies.forEach(x -> retrieveCarriers(x, returnList));

    return Answer.of(returnList);
  }

  private void retrieveCarriers(KnowledgeAsset x, List<KnowledgeCarrier> returnList) {
    if (x.getAssetId() != null) {
      ResourceIdentifier id = x.getAssetId();
      if (id.getTag() == null || id.getVersionTag() == null) {
        // TODO can version be optional?
        return;
      }
      this.assetRepository.getCanonicalKnowledgeAssetCarrier(
          id.getUuid(),
          id.getVersionTag())
          .ifPresent(returnList::add);
    } else {
      returnList.addAll(this.getAnonymousArtifacts(x));
    }
  }

  private List<KnowledgeCarrier> getAnonymousArtifacts(KnowledgeAsset assetSurrogate) {
    List<KnowledgeCarrier> carriers = Lists.newArrayList();

    assetSurrogate.getCarriers().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .forEach(carrier -> {
          URI masterLocation = carrier.getLocator();
          if (masterLocation != null) {

            KnowledgeCarrier newCarrier = AbstractCarrier.of(
                FileUtil.readBytes(masterLocation).orElse(new byte[0])
            );

            if (carrier.getRepresentation() != null
                && carrier.getRepresentation().getLanguage() != null) {
              KnowledgeRepresentationLanguage language = carrier.getRepresentation()
                  .getLanguage();
              newCarrier.setRepresentation(rep(language));
            }

            newCarrier.withAssetId(assetSurrogate.getAssetId());

            carriers.add(newCarrier);
          }
        });

    return carriers;
  }

}
