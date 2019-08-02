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
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole._20190801.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    this.db = DBMaker.memoryDB().make();
  }

  public MapDbIndex(File storageDir) {
    this.db = DBMaker.fileDB(storageDir).make();
  }

  @Override
  public void destroy() throws Exception {
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
  public void registerAsset(IndexPointer asset, IndexPointer surrogate,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      String name,
      String description) {
    types.stream().forEach(type -> {
      HTreeMap<String, Set<IndexPointer>> map = (HTreeMap<String, Set<IndexPointer>>) this.db
          .hashMap(BY_TYPE_MAP).createOrOpen();

      map.computeIfAbsent(type.getTag(), (x) -> map.put(x, new HashSet()));
      Set<IndexPointer> entryList = map.get(type.getTag());
      entryList.add(asset);

      map.put(type.getTag(), entryList);
    });

    roles.stream().forEach(type -> {
      HTreeMap<String, Set<IndexPointer>> map = (HTreeMap<String, Set<IndexPointer>>) this.db
          .hashMap(BY_TYPE_MAP).createOrOpen();

      map.computeIfAbsent(type.getTag(), (x) -> map.put(x, new HashSet()));
      Set<IndexPointer> entryList = map.get(type.getTag());
      entryList.add(asset);

      map.put(type.getTag(), entryList);
    });

    Set<IndexPointer> set = (Set<IndexPointer>) this.db.hashSet(ASSETS_SET).createOrOpen();

    set.add(asset);
    this.registerLatestAsset(asset);
    this.registerSurrogateToAsset(asset, surrogate);
    this.registerAnnotations(asset,
        annotations.stream().map(annotation -> (SimpleAnnotation) annotation)
            .collect(Collectors.toSet()));
    this.registerDescriptiveMetadata(asset, name, description,
        types.stream().map((type) -> type.getRef()).collect(Collectors.toSet()));
  }

  private void registerLatestAsset(IndexPointer assetPointer) {
    HTreeMap<String, IndexPointer> map = (HTreeMap<String, IndexPointer>) this.db
        .hashMap(LATEST_ASSET_MAP).createOrOpen();

    map.put(assetPointer.getId(), assetPointer);
  }

  @Override
  public void registerArtifactToAsset(IndexPointer assetPointer, IndexPointer artifact) {
    HTreeMap<IndexPointer, Set<IndexPointer>> map = (HTreeMap<IndexPointer, Set<IndexPointer>>) this.db
        .hashMap(ASSET_TO_ARTIFACT_MAP).createOrOpen();

    map.computeIfAbsent(assetPointer, (x) -> map.put(x, new HashSet()));
    Set<IndexPointer> set = map.get(assetPointer);
    set.add(artifact);

    map.put(assetPointer, set);
  }

  @Override
  public void registerSurrogateToAsset(IndexPointer assetPointer, IndexPointer surrogate) {
    HTreeMap<IndexPointer, IndexPointer> map = (HTreeMap<IndexPointer, IndexPointer>) this.db
        .hashMap(ASSET_TO_SURROGATE_MAP).createOrOpen();

    map.put(assetPointer, surrogate);
  }

  @Override
  public IndexPointer getSurrogateForAsset(IndexPointer assetPointer) {
    HTreeMap<IndexPointer, IndexPointer> map = (HTreeMap<IndexPointer, IndexPointer>) this.db
        .hashMap(ASSET_TO_SURROGATE_MAP).createOrOpen();

    return map.get(assetPointer);
  }

  @Override
  public void registerLocation(IndexPointer pointer, String href) {
    HTreeMap<IndexPointer, String> map = (HTreeMap<IndexPointer, String>) this.db
        .hashMap(ID_TO_LOCATION_MAP).createOrOpen();

    map.put(pointer, href);
  }

  @Override
  public IndexPointer getLatestAssetForId(String assetId) {
    HTreeMap<String, IndexPointer> map = (HTreeMap<String, IndexPointer>) this.db
        .hashMap(LATEST_ASSET_MAP).createOrOpen();

    return map.get(assetId);
  }

  @Override
  public String getLocation(IndexPointer pointer) {
    HTreeMap<IndexPointer, String> map = (HTreeMap<IndexPointer, String>) this.db
        .hashMap(ID_TO_LOCATION_MAP).createOrOpen();

    if (map.containsKey(pointer)) {
      return map.get(pointer);
    } else {
      throw new RuntimeException(
          "Location of: " + pointer.getId() + ", " + pointer.getVersion() + " not found.");
    }
  }

  @Override
  public Set<IndexPointer> getAssetIdsByType(String assetType) {
    HTreeMap<String, Set<IndexPointer>> map = (HTreeMap<String, Set<IndexPointer>>) this.db
        .hashMap(BY_TYPE_MAP).createOrOpen();

    if (StringUtils.isNotBlank(assetType)) {
      if (map.containsKey(assetType)) {
        return map.get(assetType).stream().collect(Collectors.toSet());
      } else {
        return Sets.newHashSet();
      }
    } else {
      return this.db.hashSet(ASSETS_SET).createOrOpen().stream().map(p -> (IndexPointer) p)
          .collect(Collectors.toSet());
    }
  }

  @Override
  public void registerAnnotations(IndexPointer pointer, Set<SimpleAnnotation> annotations) {
    HTreeMap<String, Set<IndexPointer>> map = (HTreeMap<String, Set<IndexPointer>>) this.db
        .hashMap(BY_ANNOTATION_MAP).createOrOpen();

    HTreeMap<String, Set<String>> typeMap = (HTreeMap<String, Set<String>>) this.db
        .hashMap(BY_ANNOTATION_TYPE_MAP).createOrOpen();

    if (!CollectionUtils.isEmpty(annotations)) {
      annotations.stream().forEach(annotation -> {
        String value = annotation.getExpr().getConceptId().toString();

        map.computeIfAbsent(value, (x) -> map.put(x, new HashSet<>()));
        Set<IndexPointer> pointers = map.get(value);
        pointers.add(pointer);
        map.put(value, pointers);

        if (annotation.getRel() != null) {
          String rel = annotation.getRel().getRef().toString();

          typeMap.computeIfAbsent(rel, (x) -> typeMap.put(x, new HashSet<>()));
          Set<String> values = typeMap.get(rel);
          values.add(value);

          typeMap.put(rel, values);

          value = rel + ":" + value;
          map.computeIfAbsent(value, (x) -> map.put(x, new HashSet<>()));
          pointers = map.get(value);
          pointers.add(pointer);
          map.put(value, pointers);
        }
      });
    }
  }

  @Override
  public void registerDescriptiveMetadata(IndexPointer pointer, String name, String description,
      Set<URI> types) {
    HTreeMap<IndexPointer, DescriptiveMetadata> map = (HTreeMap<IndexPointer, DescriptiveMetadata>) this.db
        .hashMap(DESCRIPTIVE_METADATA_MAP).createOrOpen();

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
  public DescriptiveMetadata getDescriptiveMetadataForAsset(IndexPointer pointer) {
    HTreeMap<IndexPointer, DescriptiveMetadata> map = (HTreeMap<IndexPointer, DescriptiveMetadata>) this.db
        .hashMap(DESCRIPTIVE_METADATA_MAP).createOrOpen();

    return map.get(pointer);
  }

  @Override
  public Set<IndexPointer> getAssetIdsByAnnotation(String annotation) {
    HTreeMap<String, Set<IndexPointer>> map = (HTreeMap<String, Set<IndexPointer>>) this.db
        .hashMap(BY_ANNOTATION_MAP).createOrOpen();

    if (StringUtils.isNotBlank(annotation)) {
      if (map.containsKey(annotation)) {
        return map.get(annotation).stream().collect(Collectors.toSet());
      } else {
        return Sets.newHashSet();
      }
    } else {
      return this.db.hashSet(ASSETS_SET).createOrOpen().stream().map(p -> (IndexPointer) p)
          .collect(Collectors.toSet());
    }
  }

  @Override
  public Set<String> getAnnotationsOfType(String annotationPredicate) {
    HTreeMap<String, Set<String>> typeMap = (HTreeMap<String, Set<String>>) this.db
        .hashMap(BY_ANNOTATION_TYPE_MAP).createOrOpen();

    return new HashSet<>(typeMap.getOrDefault(annotationPredicate, new HashSet<>()));
  }

  @Override
  public Set<IndexPointer> getArtifactsForAsset(IndexPointer artifact) {
    HTreeMap<IndexPointer, Set<IndexPointer>> map = (HTreeMap<IndexPointer, Set<IndexPointer>>) this.db
        .hashMap(ASSET_TO_ARTIFACT_MAP).createOrOpen();

    return map.getOrDefault(artifact, new HashSet<>());
  }


}
