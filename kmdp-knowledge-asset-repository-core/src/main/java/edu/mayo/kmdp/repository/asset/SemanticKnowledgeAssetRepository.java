/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.repository.asset;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.kmdp.comparator.Contrastor;
import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.*;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactApiInternal;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactRepositoryApiInternal;
import edu.mayo.kmdp.repository.artifact.v3.server.KnowledgeArtifactSeriesApiInternal;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.bundler.DefaultBundler;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.kmdp.tranx.v3.server.DeserializeApiInternal;
import edu.mayo.kmdp.tranx.v3.server.DetectApiInternal;
import edu.mayo.kmdp.tranx.v3.server.TransxionApiInternal;
import edu.mayo.kmdp.tranx.v3.server.ValidateApiInternal;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRole;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetrole.KnowledgeAssetRoleSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import edu.mayo.ontology.taxonomies.skos.relatedconcept.RelatedConceptSeries;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.services.*;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;

import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.SurrogateBuilder.assetId;
import static edu.mayo.kmdp.SurrogateHelper.getComputableCarrier;
import static edu.mayo.kmdp.util.Util.ensureUUID;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.JSON;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;
import static org.omg.spec.api4kp._1_0.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

/**
 * An {@link KnowledgeAssetRepositoryService} implementation based on a JCR document
 * store and a Jenna RDF index.
 */
@Named
@KPServer
public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepositoryService {

  private static final Logger logger = LoggerFactory
      .getLogger(SemanticKnowledgeAssetRepository.class);


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

  private DefaultBundler bundler;

  public SemanticKnowledgeAssetRepository(
      @Autowired @KPServer KnowledgeArtifactRepositoryService artifactRepo,
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
    this.bundler = new DefaultBundler(this, index);

    this.parser = parser;
    this.detector = detector;
    this.validator = validator;
    this.translator = translator;

    this.repositoryId = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);

    if (this.knowledgeArtifactRepositoryApi == null ||
        !this.knowledgeArtifactRepositoryApi.getKnowledgeArtifactRepository(repositoryId)
            .isSuccess()) {
      throw new IllegalStateException(
          "Unable to construct an Asset repository on an inconsistent Artifact repository");
    }
  }

  @Override
  public Answer<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth, String xAccept) {
    return bundler.getKnowledgeArtifactBundle(assetId, versionTag, null, null, null);
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
    surrogate.setAssetId(DatatypeHelper.uri(IndexPointer.URI_BASE, assetId.toString(), assetVersion));

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
        .withId(assetId(UUID.randomUUID().toString(), null))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetTypeSeries.values()
        ));
  }

  /**
   * Attempts to find the best manifestation of a given asset, based on the client's preference,
   * as per content standard negotiation
   *
   * If no preference is expressed,
   *   returns the "default" carrier, assuming that there is one and only one carrier
   * (TODO there should be a default preference order, or a non-deterministic selection
   *  TODO in case two or more carriers are present)
   *
   * If one or more preferences are expressed,
   *   an attempt is made to find the best available carrier that matches the preference(s):
   *     returns the binary artifact if inlined or stored in the underlying repository
   *     redirects the client if a URL locator is provided
   *   otherwise if no carrier matches the preferences,
   *     tries to find a translator that can map from any of the available carriers into a desired representation
   *        if a translator is found, it is invoked
   *        othrewise falls back to the "default" carrier
   *
   * If all else fails, a NOT FOUND error is raised
   *
   * @param assetId the ID of the asset to find a manifestation of
   * @param versionTag the version of the asset to find a manifestation of
   * @param xAccept the client's preference, as per content negotiation
   * @return The chosen Knowledge Artifact, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String xAccept) {

    final KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElse(null);
    if (surrogate == null) {
      return Answer.notFound();
    }

    List<SyntacticRepresentation> preferences = getPreferences(xAccept);
    if (preferences.isEmpty()) {
      return getDefaultCarrier(surrogate, assetId, versionTag);
    }
    Optional<ComputableKnowledgeArtifact> bestAvailableCarrier =
        negotiate(surrogate.getCarriers(), preferences);

    if (bestAvailableCarrier.isPresent()) {
      ComputableKnowledgeArtifact chosenCarrier = bestAvailableCarrier.get();

      if (chosenCarrier.getLocator() != null) {
        return Answer.referTo(chosenCarrier.getLocator(), false);
      } else {
        if (!Util.isEmpty(chosenCarrier.getInlined().getExpr())) {
          return Answer.of(AbstractCarrier.of(chosenCarrier.getInlined().getExpr())
              .withRepresentation(rep(chosenCarrier.getRepresentation()))
              .withAssetId(surrogate.getAssetId())
              .withArtifactId(chosenCarrier.getArtifactId())
              .withLabel(surrogate.getName()));
        } else {
          // TODO fix the identifiers...
          VersionedIdentifier artifactId = DatatypeHelper
              .toVersionIdentifier(chosenCarrier.getArtifactId());
          return getKnowledgeAssetCarrierVersion(
              assetId,
              versionTag,
              Util.ensureUUID(artifactId.getTag())
                  .orElseThrow(IllegalStateException::new),
              artifactId.getVersion());
        }
      }
    } else {
      Answer<KnowledgeCarrier> ephemeralArtifact =
          Answer.anyDo(preferences,
              preferred -> attemptTranslation(surrogate, assetId, versionTag, preferred));
      return ephemeralArtifact.isSuccess()
          ? ephemeralArtifact
          : getDefaultCarrier(surrogate, assetId, versionTag);
    }
  }

  private Answer<KnowledgeCarrier> attemptTranslation(KnowledgeAsset surrogate, UUID assetId,
      String versionTag,
      SyntacticRepresentation targetRepresentation) {
    Optional<IndexPointer> artifactPtr = lookupDefaultCarriers(assetId, versionTag);
    SyntacticRepresentation from;
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
    return Answer.of(translator.listOperators(from, targetRepresentation, null)
        .map(l -> l.get(0))
        .map(KnowledgeProcessingOperator::getOperatorId)
        .flatMap(id -> translator.applyTransrepresentation(
            id,
            resolveInlined(surrogate).map(AbstractCarrier::of).get(),
            new Properties()))
        .getOptionalValue());

  }

  private Answer<KnowledgeCarrier> getDefaultCarrier(KnowledgeAsset surrogate, UUID assetId,
      String versionTag) {
    BinaryCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
        .withLevel(Encoded_Knowledge_Expression)
        .withLabel(surrogate.getName())
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

  }


  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(
      UUID assetId,
      String versionTag,
      UUID artifactId,
      String artifactVersionTag) {
    IndexPointer artifactPointer = new IndexPointer(artifactId, artifactVersionTag);
    byte[] data = this.resolve(artifactPointer).orElse(new byte[]{});
    KnowledgeCarrier carrier = AbstractCarrier.of(data);

    return getKnowledgeAsset(assetId, versionTag)
        .flatOpt(surr -> getComputableCarrier(artifactId, artifactVersionTag, surr)
            .map(artifactMeta ->
                carrier.withLabel(surr.getName())
                    .withRepresentation(artifactMeta.getRepresentation() != null
                        ? rep(artifactMeta.getRepresentation())
                        : null)
                    .withArtifactId(artifactMeta.getArtifactId())
                    .withAssetId(surr.getAssetId())));
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
                  .getAssetCarrierVersionHref(assetId.toString(), versionTag, pointer.toStringId(),
                      pointer.getVersion()));

              return p;
            })
            .collect(Collectors.toList()));
  }


  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    KnowledgeAsset knowledgeAsset;
    try {
      knowledgeAsset = this.getLatestKnowledgeAssetSurrogate(assetId);
    } catch (ResourceNotFoundException e) {
      return Answer.of(ResponseCodeSeries.NotFound, null);
    }

    Optional<ComputableKnowledgeArtifact> bestSurrogate =
        negotiate(knowledgeAsset.getSurrogate(), getPreferences(xAccept));
    if (bestSurrogate.isPresent() && bestSurrogate.get().getLocator() != null) {
      return Answer.referTo(bestSurrogate.get().getLocator(), false);
    }

    return Answer.of(knowledgeAsset);
  }

  private KnowledgeAsset getLatestKnowledgeAssetSurrogate(UUID assetId) throws ResourceNotFoundException {
    Answer<byte[]> artifact = this.knowledgeArtifactSeriesApi.getLatestKnowledgeArtifact(repositoryId, assetId);

    if (! artifact.getOptionalValue().isPresent()) {
      throw new ResourceNotFoundException();
    }

    Optional<KnowledgeAsset> latestAsset = JSonUtil.readJson(artifact.get(), KnowledgeAsset.class);

    return latestAsset.orElseThrow(() -> new RuntimeException("Unable to parse Surrogate."));
  }

  private List<SyntacticRepresentation> getPreferences(String xAccept) {
    List<String> codes = MimeTypeUtils.tokenize(xAccept);

    return codes.stream()
        .map(c -> ModelMIMECoder.decode(c)
            .orElse(rep(Knowledge_Asset_Surrogate)))
        .collect(Collectors.toList());
  }

  private Optional<ComputableKnowledgeArtifact> negotiate(List<KnowledgeArtifact> artifacts,
      List<SyntacticRepresentation> reps) {
    return reps.stream()
        .map(rep -> getBestCandidate(artifacts, rep))
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

    setIdAndVersionIfMissing(assetSurrogate, assetId, versionTag);

    if (!identifiersConsistent(assetSurrogate, assetId, versionTag)) {
      return Answer.of(ResponseCodeSeries.Conflict);
    }

    if (!hasDefaultSurrogateManifestation(assetSurrogate)) {
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

    URIIdentifier surrogateId = assetSurrogate.getAssetId();
    String surrogateVersion = assetSurrogate.getAssetId().getVersion();

    logger.debug("SAVING ASSET {} : {}", assetId, versionTag);

    this.knowledgeArtifactApi.setKnowledgeArtifactVersion(repositoryId,
        ensureUUID(surrogateId.getTag())
            .orElseThrow(IllegalStateException::new),
        surrogateVersion,
        JSonUtil.writeJson(assetSurrogate).map(ByteArrayOutputStream::toByteArray)
            .orElseThrow(RuntimeException::new));

    IndexPointer surrogatePointer = new IndexPointer(surrogateId);

    logger.debug("REGISTERING ASSET {} : {}", assetId, versionTag);

    this.index.registerAsset(
        new IndexPointer(assetId, versionTag),
            surrogatePointer,
            assetSurrogate.getFormalType(),
            assetSurrogate.getRole(),
            assetSurrogate.getSubject(),
            assetSurrogate.getRelated());

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

  private void setIdAndVersionIfMissing(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    URIIdentifier existingAssetId = assetSurrogate.getAssetId();
    URIIdentifier parameterAssetId = DatatypeHelper.uri(IndexPointer.URI_BASE, assetId.toString(), versionTag);
    if (existingAssetId == null) {
      //If the entire assetId is missing, set it based on parameters.
      assetSurrogate.setAssetId(parameterAssetId);
    } else {
      //If the version tag is missing, set it based on parameter
      if (existingAssetId.getVersionId() == null) {
        assetSurrogate.getAssetId().setVersionId(parameterAssetId.getVersionId());
      }
      //If the asset tag is missing, set it based on parameter
      if (existingAssetId.getTag() == null) {
        assetSurrogate.getAssetId().setUri(parameterAssetId.getUri());
      }
    }
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

    Set<IndexPointer> assetList = null;
    Set<IndexPointer> annotationList = null;

    if (StringUtils.isNotBlank(assetType)) {
      URI typeUri;
      Optional<KnowledgeAssetType> type = KnowledgeAssetTypeSeries.resolve(assetType);
      if (!type.isPresent()) {
        Optional<KnowledgeAssetRole> role = KnowledgeAssetRoleSeries.resolve(assetType);
        if (!role.isPresent()) {
          throw new IllegalStateException("Unrecognized asset type " + assetType);
        } else {
          typeUri = role.get().getConceptId();
        }
      } else {
        typeUri = type.get().getConceptId();
      }

      assetList = this.index.getAssetIdsByType(typeUri);
    }

    if (StringUtils.isNotBlank(annotation)) {
      annotationList = this.index.getAssetIdsByAnnotation(RelatedConceptSeries.resolve(
              annotation).orElseThrow(() -> new RuntimeException("Annotation could not be resolved: " + annotation)).getConceptId());
    }

    Set<IndexPointer> all = Sets.newHashSet();
    if (assetList != null && annotationList != null) {
      all = Sets.intersection(assetList, annotationList);
    } else {
      if (assetList == null && annotationList == null) {
        all = this.index.getAllAssetIds();
      } else {
        if (assetList != null) {
          all = assetList;
        }
        if (annotationList != null) {
          all = annotationList;
        }
      }
    }

    pointers = all.stream()
            .map(IndexPointer::getTag)
            .map(UUID::fromString)
            .map(this::getLatestKnowledgeAssetSurrogate)
            .map(KnowledgeAsset::getAssetId)
            .map(id -> this.toPointer(id, HrefType.ASSET))
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

    this.index.registerArtifactToAsset(new IndexPointer(assetId, versionTag),
        new IndexPointer(artifactId, artifactVersion));

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
    Optional<IndexPointer> surrogatePointer = Optional.ofNullable(
            this.index.getSurrogateForAsset(new IndexPointer(assetId, versionTag)));

    if (surrogatePointer.isPresent()) {
      return this.resolve(surrogatePointer.get())
              .map(sr -> AbstractCarrier.of(sr)
                      .withRepresentation(rep(Knowledge_Asset_Surrogate, JSON)))
              .flatMap(kc -> parser.lift(kc, Abstract_Knowledge_Expression).getOptionalValue())
              .flatMap(kc -> kc.as(KnowledgeAsset.class));
    } else {
      return Optional.empty();
    }
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
    return vid.getTag().equals(artifactId.getTag()) && vid.getVersion()
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
      logger.error(e.getMessage(), e);
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
    for (Entry<URI, List<Pointer>> entry : versions.entrySet()) {
      Pointer latest = entry.getValue().get(0);
      returnList.add(latest);
    }

    return returnList;
  }


  private enum HrefType {ASSET, ASSET_VERSION}

  private Pointer toPointer(URIIdentifier entityRef, HrefType hrefType) {
    return this.toPointer(new IndexPointer(entityRef), hrefType);
  }

  private Pointer toPointer(IndexPointer pointer, HrefType hrefType) {
    Pointer p = new org.omg.spec.api4kp._1_0.identifiers.resources.Pointer();
    p.setEntityRef(pointer);

    Optional<KnowledgeAsset> asset = this.retrieveAssetSurrogate(UUID.fromString(pointer.getTag()), pointer.getVersion());

    if (asset.isPresent()) {
      p.setName(asset.get().getName());
      p.setSummary(asset.get().getDescription());

      // only set the Pointer type of the asset has one (and only one) type
      if (asset.get().getFormalType() != null && asset.get().getFormalType().size() == 1) {
        p.setType(asset.get().getFormalType().get(0).getRef());
      }
    }

    URI href;
    switch (hrefType) {
      case ASSET:
        href = this.hrefBuilder.getAssetHref(pointer.getTag());
        break;
      case ASSET_VERSION:
        href = this.hrefBuilder.getAssetVersionHref(pointer.getTag(), pointer.getVersion());
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

      Matcher matcher = Pattern.compile("^.*/(?:artifacts|assets)/(.*)/versions/(.*)$").matcher(location);
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
