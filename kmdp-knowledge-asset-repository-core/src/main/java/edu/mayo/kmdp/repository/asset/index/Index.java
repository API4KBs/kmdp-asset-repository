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
package edu.mayo.kmdp.repository.asset.index;


import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;

/**
 * An interface to index Assets and their relationships.
 */
public interface Index {

  /**
   * Registers an Asset, using the information contained in the canonical Surrogate
   * @see Index#registerAsset(ResourceIdentifier, String, ResourceIdentifier, String, List, List, List, List)
   *@param assetSurrogate
   * @param surrogateId
   * @param surrogateMimeType
   * @param index
   */
  static void registerAssetByCanonicalSurrogate(KnowledgeAsset assetSurrogate,
      ResourceIdentifier surrogateId, String surrogateMimeType, Index index) {
    index.registerAsset(
        assetSurrogate.getAssetId(),
        assetSurrogate.getName(),
        surrogateId,
        surrogateMimeType,
        assetSurrogate.getFormalType(),
        assetSurrogate.getRole(),
        assetSurrogate.getAnnotation(),
        assetSurrogate.getLinks());
    assetSurrogate.getCarriers()
        .forEach(ka -> index.registerArtifactToAsset(
            assetSurrogate.getAssetId(),
            ka.getArtifactId(),
            Util.coalesce(ModelMIMECoder.encode(ka.getRepresentation()), ka.getMimeType())));
    // exclude the canonical surrogate, which is processed by 'registerAsset'
    assetSurrogate.getSurrogate().stream()
        .filter(surr -> ! surr.getArtifactId().sameAs(surrogateId))
        .forEach(surr -> index.registerSurrogateToAsset(
            assetSurrogate.getAssetId(),
            surr.getArtifactId(),
            ModelMIMECoder.encode(surr.getRepresentation())));
  }

  /**
   * Register an Asset and its initial metadata.
   * @param assetId
   * @param surrogateId
   * @param types
   * @param roles
   * @param annotations
   * @param related
   */
  void registerAsset(ResourceIdentifier assetId, String assetName, ResourceIdentifier surrogateId,
      String surrogateMimeType, List<KnowledgeAssetType> types,
      List<KnowledgeAssetRole> roles, List<Annotation> annotations, List<Link> related);

  /**
   * Link an Artifact to an Asset.
   *
   * @param assetPointer
   * @param artifact
   * @parm
   */
  void registerArtifactToAsset(ResourceIdentifier assetPointer, ResourceIdentifier artifact, String mimeTypes);


  /**
   * Returns true if the given UUID is associated to an Asset
   * @param assetId
   * @return
   */
  boolean isKnownAsset(ResourceIdentifier assetId);

  /**
   * Get all related Assets, regardless our how they are related.
   * This will transitively search for related Assets.
   *
   * @param assetPointer
   * @return
   */
  Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer);

  /**
   * Get related Assets, restricted to a relation type.
   * This will transitively search for related Assets.
   *
   * @param assetPointer
   * @param relation
   * @return
   */
  Set<ResourceIdentifier> getRelatedAssets(ResourceIdentifier assetPointer, URI relation);

  /**
   * Linke a Surrogate to an Asset.
   *
   * @param assetPointer
   * @param surrogate
   */
  void registerSurrogateToAsset(ResourceIdentifier assetPointer,
      ResourceIdentifier surrogate, String mimeType);

  /**
   * Retrieve a pointer to the Surrogate given an Asset.
   *
   * @param assetPointer
   * @return
   */
  Optional<ResourceIdentifier> getCanonicalSurrogateForAsset(ResourceIdentifier assetPointer);

  /**
   * Get the storage location of an Asset/Artifact.
   *
   * @param pointer
   * @return
   */
  URI getLocation(ResourceIdentifier pointer);

  /**
   * Retrieve a list of Assets of a given type.
   *
   * @param assetType
   * @return
   */
  Set<ResourceIdentifier> getAssetIdsByType(URI assetType);

  /**
   * Retrieve a list of Assets with a given annotation (value).
   *
   * @param annotationValue the URI of a concept using to annotate the Assets
   * @return the IDs of the Assets such that (?s * annotationValue)
   */
  Set<ResourceIdentifier> getAssetIdsByAnnotationValue(URI annotationValue);

  /**
   * Retrieve a list of Assets with a given annotation (property).
   *
   * @param annotation the URI of a relationship used to annotate the Assets
   * @return the IDs of the Assets such that (?s annotation *)
   */
  Set<ResourceIdentifier> getAssetIdsByAnnotation(URI annotation);

  /**
   * Retrieve a list of Assets with a given annotation (property + value)
   *
   * @param annotation the URI of a relationship used to annotate the Assets
   * @param value the URI of a concept using to annotate the Assets
   * @return the IDs of the Assets such that (?s annotation annotationValue)
   */
  Set<ResourceIdentifier> getAssetIdsByAnnotation(URI annotation, URI value);

  /**
   * Retrieve a list of all Assets.
   *
   * @return
   */
  Set<ResourceIdentifier> getAllAssetIds();

  /**
   * Get the list of all Artifacts (carriers) for an Asset.
   * @param assetId
   * @return
   */
  Set<ResourceIdentifier> getArtifactsForAsset(ResourceIdentifier assetId);

  /**
   * Get the list of all Surrogates (carriers) for an Asset.
   * @param assetId
   * @return
   */
  Set<ResourceIdentifier> getSurrogatesForAsset(ResourceIdentifier assetId);

  /**
   * Retrieves the name of an Asset
   * @param assetId
   * @return
   */
  Optional<String> getAssetName(ResourceIdentifier assetId);

  /**
   * Retrieves the types and roles of an Asset
   * @param assetId
   * @return
   */
  List<ConceptIdentifier> getAssetTypes(ResourceIdentifier assetId);

  /**
   * Returns the known Versions of a given KnowledgeAsset,
   * sorted by timestamp
   *
   * @param assetSeriesId the UUID of the Asset series, common to all versions
   */
  List<ResourceIdentifier> getAssetVersions(UUID assetSeriesId);

  /**
   * Returns the known Versions of a Canonical Surrogate
   * sorted by timestamp
   *
   * @param surrogateSeriesId the identifier of a Surrogate series
   */
  List<Pointer> getSurrogateVersions(UUID surrogateSeriesId);

  /**
   * Returns the known Versions of a Knowledge Artifact
   * sorted by timestamp
   *
   * @param carrierSeriesId the identifier of a Surrogate series
   */
  List<Pointer> getCarrierVersions(UUID carrierSeriesId);

  /**
   * Reset and clear the store.
   *
   * NOTE: Implementations are encouraged to throw an {@link UnsupportedOperationException} unless
   * there is a very specific need to reset the store.
   */
  void reset();

  /**
   * Returns the KnowledgeGraph underlying this index
   *
   * @return
   */
  KnowledgeBase asKnowledgeBase();

  /**
   * Returns the ID of the KnowledgeGraph underlying this index
   * @return
   */
  ResourceIdentifier getKnowledgeBaseId();


  Optional<ResourceIdentifier> resolve(UUID assetId, String versionTag);

  Optional<ResourceIdentifier> resolve(UUID assetId);
}
