package edu.mayo.kmdp.repository.asset.index;


import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.terms.kao.knowledgeassettype._1_0.KnowledgeAssetType;

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
