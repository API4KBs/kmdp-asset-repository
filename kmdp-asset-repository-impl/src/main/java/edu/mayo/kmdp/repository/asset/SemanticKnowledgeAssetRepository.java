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

import static edu.mayo.kmdp.SurrogateBuilder.id;
import static edu.mayo.kmdp.util.Util.ensureUUID;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.Association;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.bundler.DefaultBundler;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.kmdp.tranx.DeserializeApi;
import edu.mayo.kmdp.tranx.DetectApi;
import edu.mayo.kmdp.tranx.TransxionApi;
import edu.mayo.kmdp.tranx.ValidateApi;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole._1_0.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.ASTCarrier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._1_0.services.resources.SyntacticRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;


public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepository {

  private static final String URI_BASE = Registry.MAYO_ASSETS_BASE_URI;

  private final String REPOSITORY_ID;

  /* Knowledge Artifact Repository Service Client*/
  private KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi;

  private KnowledgeArtifactApi knowledgeArtifactApi;

  private KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi;

  /* Language Service Client*/
  @Autowired(required = false)
  private DeserializeApi parser;

  @Autowired(required = false)
  private DetectApi detector;

  @Autowired(required = false)
  private ValidateApi validator;

  @Autowired(required = false)
  private TransxionApi translator;

  /* Internal helpers */
  private Index index;

  private HrefBuilder hrefBuilder;

  private Bundler bundler;


  private static final String CCG_MIME = "ccg+json";  // TOOD Remove!!

  public SemanticKnowledgeAssetRepository() {
    this.REPOSITORY_ID = new KnowledgeAssetRepositoryServerConfig()
        .getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);
  }

  public SemanticKnowledgeAssetRepository(
      KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi,
      KnowledgeArtifactApi knowledgeArtifactApi,
      KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi,
      Index index,
      KnowledgeAssetRepositoryServerConfig cfg) {
    super();

    this.knowledgeArtifactRepositoryApi = knowledgeArtifactRepositoryApi;
    this.knowledgeArtifactApi = knowledgeArtifactApi;
    this.knowledgeArtifactSeriesApi = knowledgeArtifactSeriesApi;

    this.index = index;
    this.hrefBuilder = new HrefBuilder(cfg);
    this.bundler = new DefaultBundler(this);

    this.REPOSITORY_ID = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth, String xAccept) {
    return this.wrap(this.bundler.bundle(assetId, versionTag));
  }

  @Override
  public ResponseEntity<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth) {
    return null;
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    UUID assetId;
    UUID assetVersion;

    KnowledgeAsset surrogate = new KnowledgeAsset();
    assetId = UUID.randomUUID();
    assetVersion = UUID.randomUUID();
    surrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), assetVersion.toString()));

    this.setVersionedKnowledgeAsset(assetId, assetVersion.toString(), surrogate);

    return wrap(assetId);
  }

  @Override
  public ResponseEntity<Void> addKnowledgeAssetCarrier(UUID assetId, String versionTag,
      byte[] exemplar) {
    UUID artifactId = this.getNewArtifactId();
    String artifactVersion = UUID.randomUUID().toString();

    return setKnowledgeAssetCarrierVersion(assetId, versionTag, artifactId, artifactVersion,
        exemplar);
  }


  @Override
  public ResponseEntity<KnowledgeAssetCatalog> getAssetCatalog() {
    return this.wrap(new KnowledgeAssetCatalog()
        .withId(id(UUID.randomUUID().toString(), null))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetType.values()
        ));
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String xAccept) {
    KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElseGet(KnowledgeAsset::new);

    if (isCarrierNativelyAvailable(surrogate, xAccept)) {
      BinaryCarrier carrier = new BinaryCarrier();
      Optional<IndexPointer> artifactPtr = lookupDefaultCarriers(assetId, versionTag);

      artifactPtr
          .flatMap((ptr) -> getRepresentationLanguage(surrogate, ptr))
          .ifPresent((lang) -> carrier.withRepresentation(rep(lang)));

      carrier.withAssetId(surrogate.getAssetId());

      if (artifactPtr.isPresent()) {

        resolve(artifactPtr.get()).ifPresent(carrier::withEncodedExpression);
        return this.wrap(carrier);

      } else {
        Optional<KnowledgeArtifact> inlinedArtifact = surrogate.getCarriers().stream()
            .filter((c) -> c.getInlined() != null && !Util.isEmpty(c.getInlined().getExpr()))
            .findFirst();
        if (inlinedArtifact.isPresent()) {
          carrier.withEncodedExpression(inlinedArtifact.get().getInlined().getExpr().getBytes());
          return wrap(carrier);
        } else {
          System.err.println(" ASSET NOT FOUND");
          return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
      }
    } else {

      System.err.println("TODO: Lookup the appropriate translator... with  ");

      KnowledgeCarrier input = new ASTCarrier().withParsedExpression(surrogate)
          .withRepresentation(
              new SyntacticRepresentation()
                  .withLanguage(KnowledgeRepresentationLanguage.Knowledge_Asset_Surrogate));
      List<KnowledgeCarrier> assets = new LinkedList<>();
      assets.add(input);

      // TODO MOVE OUT: THIS IS THE SEED of a new Helper class : Struct-urer (the counterpart of Bundler)
      List<KnowledgeCarrier> deps = SurrogateHelper.closure(surrogate, false).stream()
          .map((dep) -> retrieveAssetSurrogate(dep.getAssetId()))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map((dep) -> new ASTCarrier().withParsedExpression(dep)
              .withRepresentation(
                  new SyntacticRepresentation()
                      .withLanguage(KnowledgeRepresentationLanguage.Knowledge_Asset_Surrogate)))
          .collect(Collectors.toList());
      assets.addAll(deps);

      //TODO Rewire the lang service
      KnowledgeCarrier kcarrier = this.translator
          .applyTransrepresentation("SurrToCCG", assets.get(0), new Properties()).getOptionalValue()
          .get();

      // TODO Need proper parse/serializtion APIs
      if (kcarrier instanceof ASTCarrier) {
        BinaryCarrier bCarrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
            .withEncodedExpression(JSonUtil.writeJson(((ASTCarrier) kcarrier).getParsedExpression())
                .map(ByteArrayOutputStream::toByteArray)
                .orElseThrow(IllegalStateException::new))
            .withRepresentation(kcarrier.getRepresentation().withFormat(SerializationFormat.JSON));
        return this.wrap(bCarrier);
      }
      return this.wrap(kcarrier);
    }
  }


  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(
      UUID assetId,
      String versionTag,
      UUID artifactId,
      String artifactVersionTag) {
    IndexPointer artifactPointer = new IndexPointer(artifactId.toString(), artifactVersionTag);

    byte[] data = this.resolve(artifactPointer).orElse(new byte[]{});
    BinaryCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
        .withEncodedExpression(data);
    return this.wrap(carrier);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID assetId,
      String versionTag) {
    IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
    return this.wrap(this.index.getArtifactsForAsset(assetPointer).stream().map(pointer -> {
      Pointer p = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      p.setHref(this.hrefBuilder
          .getAssetCarrierVersionHref(assetId.toString(), versionTag, pointer.getId(),
              pointer.getVersion()));

      return p;
    }).collect(Collectors.toList()));
  }


  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    IndexPointer pointer = this.index.getLatestAssetForId(assetId.toString());

    return this.getVersionedKnowledgeAsset(ensureUUID(pointer.getId()).get(), pointer.getVersion());
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId,
      String versionTag) {
    return this.wrap(retrieveAssetSurrogate(assetId, versionTag).orElseGet(KnowledgeAsset::new));
  }

  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID assetId, String versionTag,
      KnowledgeAsset assetSurrogate) {
    System.err.println("INITIALIZING ASSET " + assetId + ":" + versionTag);

    if (assetSurrogate.getAssetId() == null) {
      assetSurrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), versionTag));
    } else {
      if (!assetSurrogate.getAssetId().getTag().equals(assetId.toString()) ||
          !assetSurrogate.getAssetId().getVersion().equals(versionTag)) {
        throw new RuntimeException("Surrogate ID/version must match asset ID/version. \n"
            + "> assetId = " + assetId + " vs " + assetSurrogate.getAssetId().getTag() + "\n "
            + "> versionTag = " + versionTag + " vs " + assetSurrogate.getAssetId().getVersion());
      }
    }

    String surrogateId = assetSurrogate.getAssetId().getTag();
    String surrogateVersion = assetSurrogate.getAssetId().getVersion();

    this.knowledgeArtifactApi.setKnowledgeArtifactVersion(REPOSITORY_ID,
        ensureUUID(surrogateId).get(),
        surrogateVersion,
        JSonUtil.writeJson(assetSurrogate).map(ByteArrayOutputStream::toByteArray)
            .orElseThrow(RuntimeException::new));

    IndexPointer surrogatePointer = new IndexPointer(surrogateId, surrogateVersion);

    this.index.registerAsset(
        new IndexPointer(assetId, versionTag),
        surrogatePointer,
        assetSurrogate.getFormalType(),
        assetSurrogate.getRole(),
        assetSurrogate.getSubject(),
        assetSurrogate.getName(),
        assetSurrogate.getDescription());

    this.index.registerLocation(surrogatePointer,
        hrefBuilder.getArtifactRef(REPOSITORY_ID, surrogateId, surrogateVersion).toString());

    assetSurrogate.getCarriers().stream().map(c -> (KnowledgeArtifact) c)
        .forEach(carrier -> {
          URI masterLocation = carrier.getLocator();
          if (masterLocation != null) {
            // TODO FIXME 'masterLocation' can be set with or without actually embedding the artifact.
            // Reserving 'EMBEDDED' also seems brittle
            IndexPointer carrierPointer = new IndexPointer(masterLocation.toString(), "EMBEDDED");
            this.index
                .registerArtifactToAsset(new IndexPointer(assetId, versionTag), carrierPointer);
            this.index.registerLocation(carrierPointer, masterLocation.toString());
          }
        });

    // recurse to register dependencies
    assetSurrogate.getRelated().stream().
        map(Association::getTgt).
        filter(knowledgeResource -> knowledgeResource instanceof KnowledgeAsset).
        map(knowledgeResource -> (KnowledgeAsset) knowledgeResource).
        forEach(dependency -> {
          // if the resource id is null must be anonymous
          // only do this if it has a 'type'. This is to distinguish 'stubs' vs full resources.
          if (dependency.getAssetId() != null && !CollectionUtils
              .isEmpty(dependency.getFormalType())) {
            String id = dependency.getAssetId().getTag();
            String version = dependency.getAssetId().getVersion();

            setVersionedKnowledgeAsset(ensureUUID(id).get(), version, dependency);
          }
        });

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(UUID assetId, Integer offset,
      Integer limit, String beforeTag, String afterTag, String sort) {

    List<Pointer> pointers = this.knowledgeArtifactSeriesApi
        .getKnowledgeArtifactSeries(REPOSITORY_ID,
            assetId,
            false, -1, -1,
            null, null, null).getOptionalValue().get();

    List<Pointer> versionPointers = pointers.stream()
        .map(pointer -> this.toPointer(pointer.getEntityRef(), HrefType.ASSET_VERSION))
        .collect(Collectors.toList());

    return this.wrap(versionPointers);
  }


  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(
      String assetType,
      final String annotation,
      Integer offset,
      Integer limit) {
    List<Pointer> pointers;

    boolean ignored = validateFilters(assetType, annotation);

    Set<IndexPointer> list = this.index.getAssetIdsByType(assetType);
    Set<IndexPointer> annos = this.index.getAssetIdsByAnnotation(annotation);
    list.retainAll(annos);

    pointers = list.stream().map(asset -> this.toPointer(asset, HrefType.ASSET))
        .collect(Collectors.toList());

    return this.wrap(this.aggregateVersions(pointers));
  }


  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersion, byte[] exemplar) {

    System.err.println(
        "ADDING CARRIER TO ASSET " + assetId + ":" + versionTag + " >>> " + artifactId + ":"
            + artifactVersion);
    this.knowledgeArtifactApi
        .setKnowledgeArtifactVersion(REPOSITORY_ID,
            artifactId,
            artifactVersion,
            exemplar);

    // TODO FIXME href from artifact in repository
    this.index.registerLocation(new IndexPointer(artifactId.toString(), artifactVersion),
        hrefBuilder.getArtifactRef(REPOSITORY_ID, artifactId.toString(), artifactVersion)
            .toString());

    this.index.registerArtifactToAsset(new IndexPointer(assetId, versionTag),
        new IndexPointer(artifactId.toString(), artifactVersion));

    KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElseGet(KnowledgeAsset::new);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID assetId,
      String versionTag, Boolean flat, String xAccept) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID assetId,
      String versionTag) {
    return null;
  }


  private Optional<KnowledgeAsset> retrieveAssetSurrogate(URIIdentifier resourceId) {
    VersionedIdentifier id = DatatypeHelper.toVersionIdentifier(resourceId);
    return retrieveAssetSurrogate(ensureUUID(id.getTag()).get(),
        id.getVersion());
  }


  private Optional<KnowledgeAsset> retrieveAssetSurrogate(UUID assetId, String versionTag) {
    IndexPointer surrogatePointer = this.index
        .getSurrogateForAsset(new IndexPointer(assetId, versionTag));
    return this.resolve(surrogatePointer)
        .flatMap((sr) -> JSonUtil.readJson(sr, KnowledgeAsset.class));
  }


  private Optional<KnowledgeRepresentationLanguage> getRepresentationLanguage(
      KnowledgeAsset surrogate, IndexPointer artifactId) {
    return surrogate.getCarriers().stream()
        .filter((c) -> same(c.getArtifactId(), artifactId))
        .filter(ComputableKnowledgeArtifact.class::isInstance)
        .map(ComputableKnowledgeArtifact.class::cast)
        .filter((c) -> c.getRepresentation() != null)
        .map((c) -> c.getRepresentation().getLanguage())
        .findFirst();
  }

  private boolean same(URIIdentifier resourceId, IndexPointer artifactId) {
    //TODO Just use VID?
    if (resourceId == null) {
      return false;
    }
    VersionedIdentifier vid = DatatypeHelper.toVersionIdentifier(resourceId);
    return vid.getTag().equals(artifactId.getId()) && vid.getVersion()
        .equals(artifactId.getVersion());
  }

  private Optional<IndexPointer> lookupDefaultCarriers(UUID assetId, String versionTag) {
    IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
    return lookupDefaultCarriers(assetPointer);
  }

  private Optional<IndexPointer> lookupDefaultCarriers(IndexPointer assetPointer) {
    Set<IndexPointer> artifacts = this.index.getArtifactsForAsset(assetPointer);
    IndexPointer artifact;
    if (artifacts.size() == 0) {
      return Optional.empty();
    } else if (artifacts.size() > 1) {
      //TOOD FIXME
      artifact = artifacts.stream().filter((a) -> !"EMBEDDED".equals(a.getVersion())).findFirst()
          .orElse(null);
      if (artifact == null) {
        return Optional.empty();
      }
    } else {
      artifact = artifacts.iterator().next();
    }
    return Optional.of(artifact);
  }

  private boolean isCarrierNativelyAvailable(KnowledgeAsset surrogate, String xAccept) {
    //TODO This is obviously much more complex...
    return !StringUtils.equals(xAccept, CCG_MIME);
  }


  private boolean validateFilters(String assetType, String annotation) {
    // Defensive programming: ensure assetType is a known type, and that annotation is a fully qualified URI
    try {
      if (assetType != null) {
        Optional<KnowledgeAssetType> type = KnowledgeAssetType.resolve(assetType);
        if (!type.isPresent()) {
          Optional<KnowledgeAssetRole> role = KnowledgeAssetRole.resolve(assetType);
          if (!role.isPresent()) {
            throw new IllegalStateException("Unrecognized asset type " + assetType);
          }
        }
      }
      if (annotation != null) {
        //TODO:
        //URI annotationUri = new URI(annotation);
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private List<Pointer> aggregateVersions(List<Pointer> pointers) {
    Map<URI, List<Pointer>> versions = Maps.newHashMap();

    pointers.forEach((pointer -> {
      URI id = pointer.getEntityRef().getUri();
      if (!versions.containsKey(id)) {
        versions.put(id, Lists.newArrayList());
      }
      versions.get(id).add(pointer);
    }));

    List<Pointer> returnList = Lists.newArrayList();
    for (URI assetId : versions.keySet()) {
      Pointer latest = versions.get(assetId).get(0);
      returnList.add(latest);
    }

    return returnList;
  }


  private enum HrefType {ASSET, ASSET_VERSION, ASSET_CARRIER}

  private Pointer toPointer(URIIdentifier entityRef, HrefType hrefType) {
    VersionIdentifier id = DatatypeHelper.toVersionIdentifier(entityRef);
    return this.toPointer(new IndexPointer(id.getTag(), id.getVersion()), hrefType);
  }

  private Pointer toPointer(IndexPointer pointer, HrefType hrefType) {
    String id = pointer.getId();
    String version = pointer.getVersion();

    Pointer p = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
    p.setEntityRef(DatatypeHelper.uri(URI_BASE, id, version));

    Index.DescriptiveMetadata metadata = this.index.getDescriptiveMetadataForAsset(pointer);
    p.setName(metadata.name);
    p.setSummary(metadata.description);
    if (metadata.type != null) {
      p.setType(URI.create(metadata.type));
    }

    URI href;
    switch (hrefType) {
      case ASSET:
        href = this.hrefBuilder.getAssetHref(id);
        break;
      case ASSET_VERSION:
        href = this.hrefBuilder.getAssetVersionHref(id, version);
        break;
      default:
        throw new IllegalStateException();
    }
    p.setHref(href);
    return p;
  }

  protected Optional<byte[]> resolve(IndexPointer pointer) {
    if (pointer == null) {
      return Optional.empty();
    } else {
      String location = this.index.getLocation(pointer);

      Matcher matcher = Pattern.compile("^.*/artifacts/(.*)/versions/(.*)$").matcher(location);
      if (matcher.matches() && matcher.groupCount() == 2) {
        return knowledgeArtifactApi
            .getKnowledgeArtifactVersion(REPOSITORY_ID,
                ensureUUID(matcher.group(1)).get(),
                matcher.group(2),
                false).getOptionalValue();
      } else {
        URI uri = URI.create(location);
        return FileUtil.readBytes(uri);
      }
    }
  }

  private UUID getNewArtifactId() {
    return UUID.randomUUID();
  }

  protected <T> ResponseEntity<T> wrap(T obj) {
    return new ResponseEntity<>(obj, HttpStatus.OK);
  }


}
