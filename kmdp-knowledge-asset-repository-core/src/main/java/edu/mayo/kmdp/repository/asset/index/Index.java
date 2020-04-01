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


import edu.mayo.kmdp.metadata.v2.surrogate.Link;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Annotation;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;

/**
 * An interface to index Assets and their relationships.
 */
public interface Index {

  /**
   * Register an Asset and its initial metadata.
   *  @param asset
   * @param surrogate
   * @param types
   * @param roles
   * @param annotations
   * @param related
   */
  void registerAsset(ResourceIdentifier asset, ResourceIdentifier surrogate, List<KnowledgeAssetType> types,
                     List<KnowledgeAssetRole> roles, List<Annotation> annotations, List<Link> related);

  /**
   * Link an Artifact to an Asset.
   *
   * @param assetPointer
   * @param artifact
   */
  void registerArtifactToAsset(ResourceIdentifier assetPointer, ResourceIdentifier artifact);

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
  void registerSurrogateToAsset(ResourceIdentifier assetPointer, ResourceIdentifier surrogate);

  /**
   * Retrieve a pointer to the Surrogate given an Asset.
   *
   * @param assetPointer
   * @return
   */
  ResourceIdentifier getSurrogateForAsset(ResourceIdentifier assetPointer);

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
   * @param artifact
   * @return
   */
  Set<ResourceIdentifier> getArtifactsForAsset(ResourceIdentifier artifact);

  /**
   * Reset and clear the store.
   *
   * NOTE: Implementations are encouraged to throw an {@link UnsupportedOperationException} unless
   * there is a very specific need to reset the store.
   */
  void reset();

}
