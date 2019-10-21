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
import static edu.mayo.kmdp.comparator.Contrastor.Comparison.BROADER;
import static edu.mayo.kmdp.comparator.Contrastor.Comparison.EQUIVALENT;
import static edu.mayo.kmdp.util.Util.ensureUUID;
import static edu.mayo.kmdp.util.ws.ResponseHelper.attempt;
import static edu.mayo.kmdp.util.ws.ResponseHelper.fail;
import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.Knowledge_Asset_Surrogate;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;
import static org.omg.spec.api4kp._1_0.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.comparator.Contrastor;
import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.Association;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
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
import edu.mayo.kmdp.util.ws.ResponseHelper;
import edu.mayo.ontology.taxonomies.api4kp.parsinglevel._20190801.ParsingLevel;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole._20190801.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeProcessingOperator;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;


public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepository {

  private static final Logger logger = LoggerFactory.getLogger(SemanticKnowledgeAssetRepository.class);


  private static final String URI_BASE = Registry.MAYO_ASSETS_BASE_URI;

  private final String repositoryId;

  /* Knowledge Artifact Repository Service Client*/
  private KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi;

  private KnowledgeArtifactApi knowledgeArtifactApi;

  private KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi;

  /* Language Service Client*/
  @Autowired(required = false)
  @KPComponent
  private DeserializeApi parser;

  @Autowired(required = false)
  @KPComponent
  private DetectApi detector;

  @Autowired(required = false)
  @KPComponent
  private ValidateApi validator;

  @Autowired(required = false)
  @KPComponent
  private TransxionApi translator;

  /* Internal helpers */
  private Index index;

  private HrefBuilder hrefBuilder;

  private Bundler bundler;


  public SemanticKnowledgeAssetRepository() {
    this.repositoryId = new KnowledgeAssetRepositoryServerConfig()
        .getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);
  }

  public SemanticKnowledgeAssetRepository(
      KnowledgeArtifactRepositoryApi knowledgeArtifactRepositoryApi,
      KnowledgeArtifactApi knowledgeArtifactApi,
      KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi,
      DeserializeApi parserApi,
      Index index,
      KnowledgeAssetRepositoryServerConfig cfg) {
    super();

    this.knowledgeArtifactRepositoryApi = knowledgeArtifactRepositoryApi;
    this.knowledgeArtifactApi = knowledgeArtifactApi;
    this.knowledgeArtifactSeriesApi = knowledgeArtifactSeriesApi;

    this.parser = parserApi;

    this.index = index;
    this.hrefBuilder = new HrefBuilder(cfg);
    this.bundler = new DefaultBundler(this);

    this.repositoryId = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);

    if (this.knowledgeArtifactRepositoryApi == null ||
        ! this.knowledgeArtifactRepositoryApi.getKnowledgeArtifactRepository(repositoryId).isSuccess()) {
      throw new IllegalStateException(
          "Unable to construct an Asset repository on an inconsistent Artifact repository");
    }
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth, String xAccept) {
    return attempt(bundler.bundle(assetId, versionTag));
  }

  @Override
  public ResponseEntity<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth) {
    return ResponseHelper.notSupported();
  }

  @Override
  public ResponseEntity<Void> queryKnowledgeAssets(String query) {
    return ResponseHelper.notSupported();
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    UUID assetId;
    String assetVersion;

    KnowledgeAsset surrogate = new KnowledgeAsset();
    assetId = UUID.randomUUID();
    assetVersion = "0.0.0";
    surrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), assetVersion));

    this.setVersionedKnowledgeAsset(assetId, assetVersion, surrogate);

    return ResponseHelper.succeed(assetId, HttpStatus.CREATED);
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
    return succeed(new KnowledgeAssetCatalog()
        .withId(id(UUID.randomUUID().toString(), null))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetType.values()
        ));
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String xAccept) {

    final KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElseGet(KnowledgeAsset::new);

    Optional<SyntacticRepresentation> preferred = ModelMIMECoder.decode(xAccept);

    if (!preferred.isPresent() ||
        isCarrierNativelyAvailable(surrogate, preferred.get())) {

      //Should not be always binary?
      BinaryCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
          .withLevel(ParsingLevel.Encoded_Knowledge_Expression)
          .withAssetId(surrogate.getAssetId());

      Optional<IndexPointer> artifactPtr = lookupDefaultCarriers(assetId, versionTag);

      artifactPtr
          .flatMap(ptr -> getRepresentation(surrogate, ptr))
          .ifPresent(lang -> carrier.withRepresentation(rep(lang)));

      return attempt(
          artifactPtr.isPresent() ? resolve(artifactPtr.get()).map(carrier::withEncodedExpression)
              : resolveInlined(surrogate).map(carrier::withEncodedExpression)
      );

    } else {
      Optional<IndexPointer> artifactPtr = lookupDefaultCarriers(assetId, versionTag);

      SyntacticRepresentation from = null;
      if (artifactPtr.isPresent()) {
        from = artifactPtr
            .flatMap(ptr -> getRepresentation(surrogate, ptr))
            .map(AbstractCarrier::rep)
            .orElse(null);
      } else {
        from = resolveInlinedArtifact(surrogate)
            .map(ComputableKnowledgeArtifact::getRepresentation)
            .map(AbstractCarrier::rep)
            .orElse(null);
      }

      if (translator == null) {
        return attempt(Optional.empty());
      }
      return attempt(translator.listOperators(from, preferred.orElse(null), null)
          .map(l -> l.get(0))
          .map(KnowledgeProcessingOperator::getOperatorId)
          .flatMap(id -> translator.applyTransrepresentation(
              id,
              resolveInlined(surrogate).map(KnowledgeCarrier::of).get(),
              new Properties()))
          .getOptionalValue());

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
    return succeed(carrier);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID assetId,
      String versionTag) {
    IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
    return succeed(this.index.getArtifactsForAsset(assetPointer).stream().map(pointer -> {
      Pointer p = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
      p.setHref(this.hrefBuilder
          .getAssetCarrierVersionHref(assetId.toString(), versionTag, pointer.getId(),
              pointer.getVersion()));

      return p;
    }).collect(Collectors.toList()));
  }


  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    Optional<IndexPointer> pointer = Optional.ofNullable(this.index.getLatestAssetForId(assetId.toString()));

    Optional<KnowledgeAsset> latestAsset = pointer
        .map(p -> p.getVersion())
        .flatMap(v -> retrieveAssetSurrogate(assetId,v));
    if (latestAsset.isPresent()) {
      KnowledgeAsset asset = latestAsset.get();
      Optional<ComputableKnowledgeArtifact> bestSurrogate =
          negotiate(asset.getSurrogate(), xAccept);
      if (bestSurrogate.isPresent() && bestSurrogate.get().getLocator() != null) {
        return ResponseHelper.redirectTo(bestSurrogate.get().getLocator());
      }
    }
    return attempt(latestAsset);
  }

  private Optional<ComputableKnowledgeArtifact> negotiate(List<KnowledgeArtifact> artifacts, String xAccept) {
    List<String> codes = ModelMIMECoder.splitCodes(xAccept);

    List<SyntacticRepresentation> reps = codes.stream()
        .map(c -> ModelMIMECoder.toModelCode(c,Knowledge_Asset_Surrogate))
        .flatMap(Util::trimStream)
        .map(ModelMIMECoder::decode)
        .flatMap(Util::trimStream)
        .collect(Collectors.toList());

    return reps.stream()
        .map(rep -> getBestCandidate(artifacts,rep))
        .flatMap(Util::trimStream)
        .findFirst();
  }

  private Optional<ComputableKnowledgeArtifact> getBestCandidate(List<KnowledgeArtifact> artifacts,
      SyntacticRepresentation rep) {
    return artifacts.stream()
        .flatMap(x -> Util.streamAs(x, ComputableKnowledgeArtifact.class))
        .filter(x -> Contrastor
            .isBroaderOrEqual(theRepContrastor.contrast(rep, rep(x.getRepresentation()))))
        .findAny();
>>>>>>> b1dab16... [#2392061] Improve support for content negotiation with redirect to HTML variants
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId,
      String versionTag) {
    return attempt(retrieveAssetSurrogate(assetId, versionTag));
  }

  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID assetId, String versionTag,
      KnowledgeAsset assetSurrogate) {
    logger.debug("INITIALIZING ASSET {} : {}", assetId, versionTag);

    if (assetSurrogate.getAssetId() == null) {
      assetSurrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), versionTag));
    } else {
      if (!identifiersConsistent(assetSurrogate, assetId, versionTag)) {
        return new ResponseEntity<>(HttpStatus.CONFLICT);
      }
    }

    if (! hasDefaultSurrogateManifestation(assetSurrogate)) {
      // add the canonical representation of the surrogate
      assetSurrogate.withSurrogate(
          new ComputableKnowledgeArtifact()
              .withRepresentation(new Representation()
                  .withLanguage(Knowledge_Asset_Surrogate)
                  .withFormat(SerializationFormat.XML_1_1)),
          new ComputableKnowledgeArtifact()
              .withRepresentation(new Representation()
                  .withLanguage(Knowledge_Asset_Surrogate)
                  .withFormat(SerializationFormat.JSON)));
    }

    String surrogateId = assetSurrogate.getAssetId().getTag();
    String surrogateVersion = assetSurrogate.getAssetId().getVersion();

    logger.debug("SAVING ASSET {} : {}", assetId, versionTag);

    this.knowledgeArtifactApi.setKnowledgeArtifactVersion(repositoryId,
        ensureUUID(surrogateId)
            .orElseThrow(IllegalStateException::new),
        surrogateVersion,
        JSonUtil.writeJson(assetSurrogate).map(ByteArrayOutputStream::toByteArray)
            .orElseThrow(RuntimeException::new));

    IndexPointer surrogatePointer = new IndexPointer(surrogateId, surrogateVersion);

    logger.debug("REGISTERING ASSET {} : {}", assetId, versionTag);

    this.index.registerAsset(
        new IndexPointer(assetId, versionTag),
        surrogatePointer,
        assetSurrogate.getFormalType(),
        assetSurrogate.getRole(),
        assetSurrogate.getSubject(),
        assetSurrogate.getName(),
        assetSurrogate.getDescription());

    this.index.registerLocation(surrogatePointer,
        hrefBuilder.getArtifactRef(repositoryId, surrogateId, surrogateVersion).toString());

    assetSurrogate.getCarriers()
        .forEach(carrier -> {
          URI masterLocation = carrier.getLocator();
          if (masterLocation != null) {
            // 'masterLocation' can be set with or without actually embedding the artifact.
            // Reserving 'EMBEDDED' also seems brittle
            IndexPointer carrierPointer = new IndexPointer(masterLocation.toString(), "EMBEDDED");
            this.index
                .registerArtifactToAsset(new IndexPointer(assetId, versionTag), carrierPointer);
            this.index.registerLocation(carrierPointer, masterLocation.toString());
          }
        });

    logger.debug("INITIALIZING SUB-ASSETS {} : {}", assetId, versionTag);


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

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  protected boolean identifiersConsistent (KnowledgeAsset assetSurrogate, UUID assetId, String versionTag) {
    //checks that assetId and versionTag provided in surrogate match those provided as parameters
    return (assetSurrogate.getAssetId().getTag().equals(assetId.toString()) && assetSurrogate.getAssetId().getVersion().equals(versionTag));
  }

  private boolean hasDefaultSurrogateManifestation(KnowledgeAsset assetSurrogate) {
    return assetSurrogate.getSurrogate().stream()
        .flatMap(x -> Util.streamAs(x,ComputableKnowledgeArtifact.class))
        .anyMatch(surr -> surr.getRepresentation().getLanguage() == Knowledge_Asset_Surrogate);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(UUID assetId, Integer offset,
      Integer limit, String beforeTag, String afterTag, String sort) {

    List<Pointer> pointers = this.knowledgeArtifactSeriesApi
        .getKnowledgeArtifactSeries(repositoryId,
            assetId,
            false, -1, -1,
            null, null, null)
        .orElse(Collections.emptyList());

    List<Pointer> versionPointers = pointers.stream()
        .map(pointer -> this.toPointer(pointer.getEntityRef(), HrefType.ASSET_VERSION))
        .collect(Collectors.toList());

    return succeed(versionPointers);
  }


  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(
      String assetType,
      final String annotation,
      Integer offset,
      Integer limit) {
    List<Pointer> pointers;

    validateFilters(assetType, annotation);

    Set<IndexPointer> list = this.index.getAssetIdsByType(assetType);
    Set<IndexPointer> annos = this.index.getAssetIdsByAnnotation(annotation);
    list.retainAll(annos);

    pointers = list.stream().map(asset -> this.toPointer(asset, HrefType.ASSET))
        .collect(Collectors.toList());

    return succeed(this.aggregateVersions(pointers));
  }


  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersion, byte[] exemplar) {

    logger.debug(
        "ADDING CARRIER TO ASSET {} : {} >>> {} : {}",
        assetId,
        versionTag,
        artifactId,
        artifactVersion);
    this.knowledgeArtifactApi
        .setKnowledgeArtifactVersion(repositoryId,
            artifactId,
            artifactVersion,
            exemplar);

    // href from artifact in repository
    this.index.registerLocation(new IndexPointer(artifactId.toString(), artifactVersion),
        hrefBuilder.getArtifactRef(repositoryId, artifactId.toString(), artifactVersion)
            .toString());

    this.index.registerArtifactToAsset(new IndexPointer(assetId, versionTag),
        new IndexPointer(artifactId.toString(), artifactVersion));

    KnowledgeAsset asset = retrieveAssetSurrogate(assetId, versionTag)
        .orElseThrow(IllegalStateException::new);
    logger.debug("Artifact has been set on asset {}", asset.getAssetId());

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

  private Optional<KnowledgeAsset> retrieveAssetSurrogate(UUID assetId, String versionTag) {
    IndexPointer surrogatePointer = this.index
        .getSurrogateForAsset(new IndexPointer(assetId, versionTag));
    return this.resolve(surrogatePointer)
        .map(sr -> AbstractCarrier.of(sr)
            .withRepresentation(rep(Knowledge_Asset_Surrogate, SerializationFormat.JSON)))
        .flatMap(kc -> parser.lift(kc, ParsingLevel.Abstract_Knowledge_Expression).getOptionalValue())
        .flatMap(kc -> kc.as(KnowledgeAsset.class));
  }

  private Optional<Representation> getRepresentation(
      KnowledgeAsset surrogate, IndexPointer artifactId) {
    return surrogate.getCarriers().stream()
        .filter(c -> same(c.getArtifactId(), artifactId))
        .filter(ComputableKnowledgeArtifact.class::isInstance)
        .map(ComputableKnowledgeArtifact.class::cast)
        .filter(c -> c.getRepresentation() != null)
        .map(ComputableKnowledgeArtifact::getRepresentation)
        .findFirst();
  }

  private boolean same(URIIdentifier resourceId, IndexPointer artifactId) {
    // Just use VID?
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
    if (artifacts.isEmpty()) {
      return Optional.empty();
    } else if (artifacts.size() > 1) {
      // EMBEDDED should not be used
      artifact = artifacts.stream().filter(a -> !"EMBEDDED".equals(a.getVersion())).findFirst()
          .orElse(null);
      if (artifact == null) {
        return Optional.empty();
      }
    } else {
      // just one artifact
      artifact = artifacts.iterator().next();
    }
    return Optional.of(artifact);
  }

  private boolean isCarrierNativelyAvailable(KnowledgeAsset surrogate,
      SyntacticRepresentation preferredRep) {
    return surrogate.getCarriers().stream()
        .filter(ComputableKnowledgeArtifact.class::isInstance)
        .map(ComputableKnowledgeArtifact.class::cast)
        .map(ComputableKnowledgeArtifact::getRepresentation)
        .map(AbstractCarrier::rep)
        .anyMatch(carrierRep ->
            Contrastor.isBroaderOrEqual(theRepContrastor.contrast(carrierRep, preferredRep)));
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
        // Need to check the annotation
      }
      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(),e);
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
    for (Entry<URI,List<Pointer>> entry : versions.entrySet()) {
      Pointer latest = entry.getValue().get(0);
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
    p.setName(metadata.getName());
    p.setSummary(metadata.getDescription());
    if (metadata.getType()!= null) {
      p.setType(URI.create(metadata.getType()));
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
            .getKnowledgeArtifactVersion(repositoryId,
                ensureUUID(matcher.group(1))
                    .orElseThrow(IllegalStateException::new),
                matcher.group(2),
                false)
            .getOptionalValue();
      } else {
        URI uri = URI.create(location);
        return FileUtil.readBytes(uri);
      }
    }
  }

  protected Optional<byte[]> resolveInlined(KnowledgeAsset surrogate) {
    return resolveInlinedArtifact(surrogate)
        .map(inlined -> inlined.getInlined().getExpr().getBytes());
  }

  protected Optional<ComputableKnowledgeArtifact> resolveInlinedArtifact(KnowledgeAsset surrogate) {
    return surrogate.getCarriers().stream()
        // && carrier has the right artifactId
        .filter(c -> c.getInlined() != null && !Util.isEmpty(c.getInlined().getExpr()))
        .filter(ComputableKnowledgeArtifact.class::isInstance)
        .map(ComputableKnowledgeArtifact.class::cast)
        .findFirst();
  }


  private UUID getNewArtifactId() {
    return UUID.randomUUID();
  }

  public DeserializeApi getParser() {
    return parser;
  }

  public void setParser(DeserializeApi parser) {
    this.parser = parser;
  }

  public DetectApi getDetector() {
    return detector;
  }

  public void setDetector(DetectApi detector) {
    this.detector = detector;
  }

  public ValidateApi getValidator() {
    return validator;
  }

  public void setValidator(ValidateApi validator) {
    this.validator = validator;
  }

  public TransxionApi getTranslator() {
    return translator;
  }

  public void setTranslator(TransxionApi translator) {
    this.translator = translator;
  }
}
