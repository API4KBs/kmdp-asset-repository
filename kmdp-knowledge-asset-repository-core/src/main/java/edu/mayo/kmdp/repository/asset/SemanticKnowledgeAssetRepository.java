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
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.JSON;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;
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
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactApiInternal;
import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactRepositoryApiInternal;
import edu.mayo.kmdp.repository.artifact.server.KnowledgeArtifactSeriesApiInternal;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.bundler.DefaultBundler;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.kmdp.tranx.server.DeserializeApiInternal;
import edu.mayo.kmdp.tranx.server.DetectApiInternal;
import edu.mayo.kmdp.tranx.server.TransxionApiInternal;
import edu.mayo.kmdp.tranx.server.ValidateApiInternal;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
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
import javax.inject.Named;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KPComponent;
import org.omg.spec.api4kp._1_0.services.KPServer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeProcessingOperator;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

@Named
@KPServer
public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepositoryService {

  private static final Logger logger = LoggerFactory.getLogger(SemanticKnowledgeAssetRepository.class);


  private static final String URI_BASE = Registry.MAYO_ASSETS_BASE_URI;

  private final String repositoryId;

  /* Knowledge Artifact Repository Service Client*/
  private KnowledgeArtifactRepositoryApiInternal knowledgeArtifactRepositoryApi;

  private KnowledgeArtifactApiInternal knowledgeArtifactApi;

  private KnowledgeArtifactSeriesApiInternal knowledgeArtifactSeriesApi;

  /* Language Service Client */
  private DeserializeApiInternal parser;

  private DetectApiInternal detector;

  private ValidateApiInternal validator;

  private TransxionApiInternal translator;

  /* Internal helpers */
  private Index index;

  private HrefBuilder hrefBuilder;

  private Bundler bundler;

  public SemanticKnowledgeAssetRepository(
      @Autowired(required = false) @KPComponent KnowledgeArtifactRepositoryService artifactRepo,
      @Autowired @KPServer DeserializeApiInternal parser,
      @Autowired(required = false) @KPServer DetectApiInternal detector,
      @Autowired(required = false) @KPServer ValidateApiInternal validator,
      @Autowired(required = false) @KPServer TransxionApiInternal translator,
      @Autowired Index index,
      @Autowired KnowledgeAssetRepositoryServerConfig cfg) {

    super();

    this.knowledgeArtifactRepositoryApi = artifactRepo;
    this.knowledgeArtifactApi = artifactRepo;
    this.knowledgeArtifactSeriesApi = artifactRepo;

    this.index = index;
    this.hrefBuilder = new HrefBuilder(cfg);
    this.bundler = new DefaultBundler(this);

    this.parser = parser;
    this.detector = detector;
    this.validator = validator;
    this.translator = translator;

    this.repositoryId = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);

    if (this.knowledgeArtifactRepositoryApi == null ||
        ! this.knowledgeArtifactRepositoryApi.getKnowledgeArtifactRepository(repositoryId).isSuccess()) {
      throw new IllegalStateException(
          "Unable to construct an Asset repository on an inconsistent Artifact repository");
    }
  }

  @Override
  public Answer<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth, String xAccept) {
    return Answer.of(bundler.bundle(assetId, versionTag));
  }

  @Override
  public Answer<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth) {
    return Answer.unsupported();
  }

  @Override
  public Answer<Void> queryKnowledgeAssets(String query) {
    return Answer.unsupported();
  }

  @Override
  public Answer<UUID> initKnowledgeAsset() {
    UUID assetId;
    String assetVersion;

    KnowledgeAsset surrogate = new KnowledgeAsset();
    assetId = UUID.randomUUID();
    assetVersion = "0.0.0";
    surrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), assetVersion));

    this.setVersionedKnowledgeAsset(assetId, assetVersion, surrogate);

    return Answer.of(ResponseCodeSeries.Created, assetId);
  }

  @Override
  public Answer<Void> addKnowledgeAssetCarrier(UUID assetId, String versionTag,
      byte[] exemplar) {
    UUID artifactId = this.getNewArtifactId();
    String artifactVersion = UUID.randomUUID().toString();

    return setKnowledgeAssetCarrierVersion(assetId, versionTag, artifactId, artifactVersion,
        exemplar);
  }


  @Override
  public Answer<KnowledgeAssetCatalog> getAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withId(id(UUID.randomUUID().toString(), null))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetTypeSeries.values()
        ));
  }

  @Override
  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String xAccept) {

    final KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElseGet(KnowledgeAsset::new);

    Optional<SyntacticRepresentation> preferred = ModelMIMECoder.decode(xAccept);

    if (!preferred.isPresent() ||
        isCarrierNativelyAvailable(surrogate, preferred.get())) {

      //Should not be always binary?
      BinaryCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
          .withLevel(Encoded_Knowledge_Expression)
          .withAssetId(surrogate.getAssetId());

      Optional<IndexPointer> artifactPtr = lookupDefaultCarriers(assetId, versionTag);

      artifactPtr
          .flatMap(ptr -> getRepresentation(surrogate, ptr))
          .ifPresent(lang -> carrier.withRepresentation(rep(lang)));

      return Answer.of(
          artifactPtr.isPresent()
              ? resolve(artifactPtr.get()).map(carrier::withEncodedExpression)
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
        return Answer.of(Optional.empty());
      }
      return Answer.of(translator.listOperators(from, preferred.orElse(null), null)
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
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(
      UUID assetId,
      String versionTag,
      UUID artifactId,
      String artifactVersionTag) {
    IndexPointer artifactPointer = new IndexPointer(artifactId.toString(), artifactVersionTag);

    byte[] data = this.resolve(artifactPointer).orElse(new byte[]{});
    return Answer.of(new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
        .withEncodedExpression(data));
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeAssetCarriers(UUID assetId,
      String versionTag) {
    IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
    return Answer.of(
        this.index.getArtifactsForAsset(assetPointer).stream()
            .map(pointer -> {
              Pointer p = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
              p.setHref(this.hrefBuilder
                  .getAssetCarrierVersionHref(assetId.toString(), versionTag, pointer.getId(),
                      pointer.getVersion()));

              return p;
            })
            .collect(Collectors.toList()));
  }


  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    Optional<IndexPointer> pointer = Optional.ofNullable(this.index.getLatestAssetForId(assetId.toString()));

    Optional<KnowledgeAsset> latestAsset = pointer
        .map(IndexPointer::getVersion)
        .flatMap(v -> retrieveAssetSurrogate(assetId,v));
    if (latestAsset.isPresent()) {
      KnowledgeAsset asset = latestAsset.get();
      Optional<ComputableKnowledgeArtifact> bestSurrogate =
          negotiate(asset.getSurrogate(), xAccept);
      if (bestSurrogate.isPresent() && bestSurrogate.get().getLocator() != null) {
        return Answer.referTo(bestSurrogate.get().getLocator(),false);
      }
    }
    return Answer.of(latestAsset);
  }

  private Optional<ComputableKnowledgeArtifact> negotiate(List<KnowledgeArtifact> artifacts, String xAccept) {
    List<String> codes = ModelMIMECoder.splitCodes(xAccept);

    List<SyntacticRepresentation> reps = codes.stream()
        .map(c -> ModelMIMECoder.toModelCode(c,Knowledge_Asset_Surrogate))
        .flatMap(StreamUtil::trimStream)
        .map(ModelMIMECoder::decode)
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());

    return reps.stream()
        .map(rep -> getBestCandidate(artifacts,rep))
        .flatMap(StreamUtil::trimStream)
        .findFirst();
  }

  private Optional<ComputableKnowledgeArtifact> getBestCandidate(List<KnowledgeArtifact> artifacts,
      SyntacticRepresentation rep) {
    return artifacts.stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .filter(x -> Contrastor
            .isBroaderOrEqual(theRepContrastor.contrast(rep, rep(x.getRepresentation()))))
        .findAny();
  }

  @Override
  public Answer<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId,
      String versionTag) {
    return Answer.of(retrieveAssetSurrogate(assetId, versionTag));
  }

  @Override
  public Answer<Void> setVersionedKnowledgeAsset(UUID assetId, String versionTag,
      KnowledgeAsset assetSurrogate) {
    logger.debug("INITIALIZING ASSET {} : {}", assetId, versionTag);

    if (assetSurrogate.getAssetId() == null) {
      assetSurrogate.setAssetId(DatatypeHelper.uri(URI_BASE, assetId.toString(), versionTag));
    } else {
      if (!identifiersConsistent(assetSurrogate, assetId, versionTag)) {
        return Answer.of(ResponseCodeSeries.Conflict);
      }
    }

    if (! hasDefaultSurrogateManifestation(assetSurrogate)) {
      // add the canonical representation of the surrogate
      assetSurrogate.withSurrogate(
          new ComputableKnowledgeArtifact()
              .withRepresentation(new Representation()
                  .withLanguage(Knowledge_Asset_Surrogate)
                  .withFormat(XML_1_1)),
          new ComputableKnowledgeArtifact()
              .withRepresentation(new Representation()
                  .withLanguage(Knowledge_Asset_Surrogate)
                  .withFormat(JSON)));
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

            setVersionedKnowledgeAsset(
                ensureUUID(id).orElseThrow(IllegalArgumentException::new),
                version,
                dependency);
          }
        });

    return Answer.of(ResponseCodeSeries.NoContent);
  }

  private boolean identifiersConsistent(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    //checks that assetId and versionTag provided in surrogate match those provided as parameters
    return (assetSurrogate.getAssetId().getTag().equals(assetId.toString())
        && assetSurrogate.getAssetId().getVersion().equals(versionTag));
  }

  private boolean hasDefaultSurrogateManifestation(KnowledgeAsset assetSurrogate) {
    return assetSurrogate.getSurrogate().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .anyMatch(surr -> surr.getRepresentation().getLanguage() == Knowledge_Asset_Surrogate);
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeAssetVersions(UUID assetId, Integer offset,
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

    return Answer.of(versionPointers);
  }


  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(
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

    return Answer.of(this.aggregateVersions(pointers));
  }


  @Override
  public Answer<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
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

    return Answer.of(ResponseCodeSeries.OK);
  }

  @Override
  public Answer<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID assetId,
      String versionTag, Boolean flat, String xAccept) {
    return Answer.unsupported();
  }

  @Override
  public Answer<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID assetId,
      String versionTag) {
    return Answer.unsupported();
  }

  private Optional<KnowledgeAsset> retrieveAssetSurrogate(UUID assetId, String versionTag) {
    IndexPointer surrogatePointer = this.index
        .getSurrogateForAsset(new IndexPointer(assetId, versionTag));
    return this.resolve(surrogatePointer)
        .map(sr -> AbstractCarrier.of(sr)
            .withRepresentation(rep(Knowledge_Asset_Surrogate, JSON)))
        .flatMap(kc -> parser.lift(kc, Abstract_Knowledge_Expression).getOptionalValue())
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
        Optional<KnowledgeAssetType> type = KnowledgeAssetTypeSeries.resolve(assetType);
        if (!type.isPresent()) {
          Optional<KnowledgeAssetRole> role = KnowledgeAssetRoleSeries.resolve(assetType);
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

  private Optional<byte[]> resolveInlined(KnowledgeAsset surrogate) {
    return resolveInlinedArtifact(surrogate)
        .map(inlined -> inlined.getInlined().getExpr().getBytes());
  }

  private Optional<ComputableKnowledgeArtifact> resolveInlinedArtifact(KnowledgeAsset surrogate) {
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

}
