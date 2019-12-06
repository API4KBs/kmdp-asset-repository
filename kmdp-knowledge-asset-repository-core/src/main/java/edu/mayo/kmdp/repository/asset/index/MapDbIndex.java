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

import com.google.common.collect.Sets;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.CollectionUtils;

public class MapDbIndex implements DisposableBean, Index {

  private static final String ASSETS_SET = "assets";
  private static final String BY_TYPE_MAP = "byType";
  private static final String BY_ANNOTATION_MAP = "byAannotation";
  private static final String BY_ANNOTATION_TYPE_MAP = "byAannotationType";
  private static final String ASSET_TO_ARTIFACT_MAP = "assetToArtifact";
  private static final String ASSET_TO_SURROGATE_MAP = "assetToSurrogate";
  private static final String ID_TO_LOCATION_MAP = "idToLocation";
  private static final String DESCRIPTIVE_METADATA_MAP = "metadata";
  private static final String LATEST_ASSET_MAP = "latestAsset";


  private DB db;

  public MapDbIndex() {
    this.db = DBMaker
        .memoryDB()
        .transactionEnable()
        .closeOnJvmShutdown()
        .make();
  }

  public MapDbIndex(File storageDir) {
    this.db = DBMaker
        .fileDB(storageDir)
        .transactionEnable()
        .closeOnJvmShutdown()
        .make();
  }

  @Override
  public void destroy() {
    this.db.close();
  }

  @Override
  public void reset() {
    this.db.getAll().forEach((name, obj) -> {
      if (obj instanceof Collection) {
        ((Collection) obj).clear();
      } else if (obj instanceof Map) {
        ((Map) obj).clear();
      } else {
        throw new IllegalStateException("Unknown map type: " + obj.getClass());
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerAsset(IndexPointer asset, IndexPointer surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      String name,
      String description) {

    HTreeMap<String, Set<IndexPointer>> map =
        (HTreeMap<String, Set<IndexPointer>>) getMap(BY_TYPE_MAP);

    types.forEach(type -> registerAssetType(map, asset, type.getTag()));

    roles.forEach(role -> registerAssetType(map, asset, role.getTag()));

    Set<IndexPointer> set = (Set<IndexPointer>) this.db.hashSet(ASSETS_SET).createOrOpen();

    set.add(asset);
    this.registerLatestAsset(asset);
    this.registerSurrogateToAsset(asset, surrogate);

    this.registerAnnotations(asset,
        annotations.stream().map(annotation -> (SimpleAnnotation) annotation)
            .collect(Collectors.toSet()));
    this.registerDescriptiveMetadata(asset, name, description,
        types.stream().map(KnowledgeAssetType::getRef)
            .collect(Collectors.toSet()));
  }

  private void registerAssetType(HTreeMap<String, Set<IndexPointer>> map, IndexPointer asset,
      String typeTag) {
    Set<IndexPointer> entryList = map.computeIfAbsent(typeTag, x -> new HashSet<>());
    entryList.add(asset);
    map.put(typeTag, entryList);
  }

  @SuppressWarnings("unchecked")
  private void registerLatestAsset(IndexPointer assetPointer) {
    HTreeMap<String, IndexPointer> map =
        (HTreeMap<String, IndexPointer>) getMap(LATEST_ASSET_MAP);

    map.put(assetPointer.getId(), assetPointer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerArtifactToAsset(IndexPointer assetPointer, IndexPointer artifact) {
    HTreeMap<IndexPointer, Set<IndexPointer>> map =
        (HTreeMap<IndexPointer, Set<IndexPointer>>) getMap(ASSET_TO_ARTIFACT_MAP);

    Set<IndexPointer> set = map.computeIfAbsent(assetPointer, x -> new HashSet<>());
    set.add(artifact);

    map.put(assetPointer, set);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerSurrogateToAsset(IndexPointer assetPointer, IndexPointer surrogate) {
    HTreeMap<IndexPointer, IndexPointer> map =
        (HTreeMap<IndexPointer, IndexPointer>) getMap(ASSET_TO_SURROGATE_MAP);

    map.put(assetPointer, surrogate);
  }

  @Override
  @SuppressWarnings("unchecked")
  public IndexPointer getSurrogateForAsset(IndexPointer assetPointer) {
    HTreeMap<IndexPointer, IndexPointer> map =
        (HTreeMap<IndexPointer, IndexPointer>) getMap(ASSET_TO_SURROGATE_MAP);

    return map.get(assetPointer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerLocation(IndexPointer pointer, String href) {
    HTreeMap<IndexPointer, String> map =
        (HTreeMap<IndexPointer, String>) getMap(ID_TO_LOCATION_MAP);

    map.put(pointer, href);
  }

  @Override
  @SuppressWarnings("unchecked")
  public IndexPointer getLatestAssetForId(String assetId) {
    HTreeMap<String, IndexPointer> map =
        (HTreeMap<String, IndexPointer>) getMap(LATEST_ASSET_MAP);

    return map.get(assetId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getLocation(IndexPointer pointer) {
    HTreeMap<IndexPointer, String> map =
        (HTreeMap<IndexPointer, String>) getMap(ID_TO_LOCATION_MAP);

    if (map.containsKey(pointer)) {
      return map.get(pointer);
    } else {
      throw new IllegalStateException(
          "Location of: " + pointer.getId() + ", " + pointer.getVersion() + " not found.");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<IndexPointer> getAssetIdsByType(String assetType) {

    if (StringUtils.isNotBlank(assetType)) {
      HTreeMap<String, Set<IndexPointer>> map =
          (HTreeMap<String, Set<IndexPointer>>) getMap(BY_TYPE_MAP);

      if (map.containsKey(assetType)) {
        return new HashSet<>(Objects.requireNonNull(map.get(assetType)));
      } else {
        return Collections.emptySet();
      }
    } else {
      return this.db.hashSet(ASSETS_SET).createOrOpen().stream()
          .map(p -> (IndexPointer) p)
          .collect(Collectors.toSet());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerAnnotations(IndexPointer pointer, Set<SimpleAnnotation> annotations) {
    HTreeMap<String, Set<IndexPointer>> map =
        (HTreeMap<String, Set<IndexPointer>>) getMap(BY_ANNOTATION_MAP);

    HTreeMap<String, Set<String>> typeMap =
        (HTreeMap<String, Set<String>>) getMap(BY_ANNOTATION_TYPE_MAP);

    if (!CollectionUtils.isEmpty(annotations)) {
      annotations.forEach(annotation -> {
        String value = annotation.getExpr().getConceptId().toString();

        Set<IndexPointer> pointers = map.computeIfAbsent(value, x -> new HashSet<>());
        pointers.add(pointer);
        map.put(value, pointers);

        if (annotation.getRel() != null) {
          String rel = annotation.getRel().getRef().toString();


          Set<String> values = typeMap.computeIfAbsent(rel, x -> new HashSet<>());
          values.add(value);

          typeMap.put(rel, values);

          value = rel + ":" + value;
          pointers = map.computeIfAbsent(value, x -> new HashSet<>());
          pointers.add(pointer);

          map.put(value, pointers);
        }
      });
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerDescriptiveMetadata(IndexPointer pointer, String name, String description,
      Set<URI> types) {
    HTreeMap<IndexPointer, DescriptiveMetadata> map =
        (HTreeMap<IndexPointer, DescriptiveMetadata>) getMap(DESCRIPTIVE_METADATA_MAP);

    String type = null;
    if (types != null) {
      Optional<URI> foundType = types.stream().findAny();
      if (foundType.isPresent()) {
        type = foundType.get().toString();
      }
    }
    map.put(pointer, new DescriptiveMetadata(name, description, type));
  }

  @Override
  @SuppressWarnings("unchecked")
  public DescriptiveMetadata getDescriptiveMetadataForAsset(IndexPointer pointer) {
    HTreeMap<IndexPointer, DescriptiveMetadata> map =
        (HTreeMap<IndexPointer, DescriptiveMetadata>) getMap(DESCRIPTIVE_METADATA_MAP);

    return map.get(pointer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<IndexPointer> getAssetIdsByAnnotation(String annotation) {

    if (StringUtils.isNotBlank(annotation)) {
      HTreeMap<String, Set<IndexPointer>> map =
          (HTreeMap<String, Set<IndexPointer>>) getMap(BY_ANNOTATION_MAP);
      if (map.containsKey(annotation)) {
        return new HashSet<>(Objects.requireNonNull(map.get(annotation)));
      } else {
        return Sets.newHashSet();
      }
    } else {
      return this.db.hashSet(ASSETS_SET).createOrOpen().stream().map(p -> (IndexPointer) p)
          .collect(Collectors.toSet());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<String> getAnnotationsOfType(String annotationPredicate) {
    HTreeMap<String, Set<String>> typeMap =
        (HTreeMap<String, Set<String>>) getMap(BY_ANNOTATION_TYPE_MAP);

    return new HashSet<>(typeMap.getOrDefault(annotationPredicate, new HashSet<>()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<IndexPointer> getArtifactsForAsset(IndexPointer artifact) {
    HTreeMap<IndexPointer, Set<IndexPointer>> map =
        (HTreeMap<IndexPointer, Set<IndexPointer>>) getMap(ASSET_TO_ARTIFACT_MAP);

    return map.getOrDefault(artifact, new HashSet<>());
  }

  private HTreeMap<?,?> getMap(String key) {
    return this.db.hashMap(key).createOrOpen();
  }


}
