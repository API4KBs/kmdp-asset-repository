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
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Set;

public interface Index {

    void registerAsset(IndexPointer asset, IndexPointer surrogate, List<KnowledgeAssetType> types, List<Annotation> annotations, String name, String description);

    void registerArtifactToAsset(IndexPointer assetPointer, IndexPointer artifact);

    void registerSurrogateToAsset(IndexPointer assetPointer, IndexPointer surrogate);

    IndexPointer getSurrogateForAsset(IndexPointer assetPointer);

    void registerLocation(IndexPointer pointer, String href);

    void registerAnnotations(IndexPointer pointer, Set<SimpleAnnotation> annotations);

    void registerDescriptiveMetadata(IndexPointer pointer, String name, String description, Set<URI> types);

    String getLocation(IndexPointer pointer);

    Set<IndexPointer> getAssetIdsByType(String assetType);

    Set<IndexPointer> getAssetIdsByAnnotation(String annotation);

    Set<IndexPointer> getArtifactsForAsset(IndexPointer artifact);

    DescriptiveMetadata getDescriptiveMetadataForAsset(IndexPointer artifact);

    Set<String> getAnnotationsOfType(String operationallyDefinesPredicate);

    IndexPointer getLatestAssetForId(String assetId);

    void reset();

    class DescriptiveMetadata implements Serializable {
        public String name;
        public String description;
        public String type;

        public DescriptiveMetadata(String name, String description, String type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }
    }
}
