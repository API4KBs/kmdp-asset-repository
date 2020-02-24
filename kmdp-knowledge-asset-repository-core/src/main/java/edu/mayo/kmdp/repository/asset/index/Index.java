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


import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * An interface to index Assets and their relationships.
 */
public interface Index {

  /**
   * Register an Asset and its initial metadata.
   *
   * @param asset
   * @param surrogate
   * @param types
   * @param roles
   * @param annotations
   * @param name
   * @param description
   */
  void registerAsset(IndexPointer asset, IndexPointer surrogate, List<KnowledgeAssetType> types,
      List<KnowledgeAssetRole> roles, List<Annotation> annotations, String name,
      String description);

  /**
   * Link an Artifact to an Asset.
   *
   * @param assetPointer
   * @param artifact
   */
  void registerArtifactToAsset(IndexPointer assetPointer, IndexPointer artifact);

  /**
   * Linke a Surrogate to an Asset.
   *
   * @param assetPointer
   * @param surrogate
   */
  void registerSurrogateToAsset(IndexPointer assetPointer, IndexPointer surrogate);

  /**
   * Retrieve a pointer to the Surrogate given an Asset.
   *
   * @param assetPointer
   * @return
   */
  IndexPointer getSurrogateForAsset(IndexPointer assetPointer);

  /**
   * Get the storage location of an Asset/Artifact.
   *
   * @param pointer
   * @return
   */
  String getLocation(IndexPointer pointer);

  /**
   * Retrieve a list of Assets of a given type.
   *
   * @param assetType
   * @return
   */
  Set<IndexPointer> getAssetIdsByType(URI assetType);

  /**
   * Retrieve a list of Assets with a given annotation.
   *
   * @param annotation
   * @return
   */
  Set<IndexPointer> getAssetIdsByAnnotation(URI annotation);

  /**
   * Retrieve a list of all Assets.
   *
   * @return
   */
  Set<IndexPointer> getAllAssetIds();

  /**
   * Get the list of all Artifacts (carriers) for an Asset.
   * @param artifact
   * @return
   */
  Set<IndexPointer> getArtifactsForAsset(IndexPointer artifact);

  /**
   * Reset and clear the store.
   *
   * NOTE: Implementations are encouraged to throw an {@link UnsupportedOperationException} unless
   * there is a very specific need to reset the store.
   */
  void reset();

}
