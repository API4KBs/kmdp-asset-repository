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

import static edu.mayo.kmdp.metadata.v2.surrogate.SurrogateHelper.getComputableCarrier;
import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static edu.mayo.kmdp.util.Util.ensureUUID;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.api4kp.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.JSON;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.RDF_1_1;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;
import static org.omg.spec.api4kp._1_0.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.comparator.Contrastor;
import edu.mayo.kmdp.inference.v4.server.QueryApiInternal;
import edu.mayo.kmdp.inference.v4.server.QueryApiInternal._askQuery;
import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.SurrogateBuilder;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import edu.mayo.kmdp.repository.artifact.v4.server.KnowledgeArtifactSeriesApiInternal;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.bundler.DefaultBundler;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.StaticFilter;
import edu.mayo.kmdp.tranx.v4.server.DeserializeApiInternal;
import edu.mayo.kmdp.tranx.v4.server.DetectApiInternal;
import edu.mayo.kmdp.tranx.v4.server.TransxionApiInternal;
import edu.mayo.kmdp.tranx.v4.server.ValidateApiInternal;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes.ResponseCodeSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguage;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;
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
import org.omg.spec.api4kp._1_0.datatypes.Bindings;
import org.omg.spec.api4kp._1_0.id.IdentifierConstants;
import org.omg.spec.api4kp._1_0.id.Pointer;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
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
import org.springframework.util.MimeTypeUtils;

/**
 * An {@link KnowledgeAssetRepositoryService} implementation based on a JCR document store and a
 * Jenna RDF index.
 */
@Named
@KPServer
public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepositoryService {

  private static final Logger logger = LoggerFactory
      .getLogger(SemanticKnowledgeAssetRepository.class);

  public static final UUID KB_UUID = UUID.randomUUID();

  private final String repositoryId;

  private static final KnowledgeRepresentationLanguage
      defaultSurrogateModel = Knowledge_Asset_Surrogate_2_0;

  /* Knowledge Artifact Repository Service Client*/
  private KnowledgeArtifactApiInternal knowledgeArtifactApi;

  private KnowledgeArtifactSeriesApiInternal knowledgeArtifactSeriesApi;

  /* Language Service Client */
  private DeserializeApiInternal parser;

  private DetectApiInternal detector;

  private ValidateApiInternal validator;

  private TransxionApiInternal translator;

  private QueryApiInternal._askQuery queryExecutor;

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
      @Autowired @KPComponent _askQuery queryExecutor,
      @Autowired Index index,
      @Autowired KnowledgeAssetRepositoryServerConfig cfg) {

    super();

    KnowledgeArtifactRepositoryService knowledgeArtifactRepositoryApi = artifactRepo;
    this.knowledgeArtifactApi = artifactRepo;
    this.knowledgeArtifactSeriesApi = artifactRepo;

    this.index = index;
    this.hrefBuilder = new HrefBuilder(cfg);
    this.bundler = new DefaultBundler(this, index);

    this.parser = parser;
    this.detector = detector;
    this.validator = validator;
    this.translator = translator;

    this.queryExecutor = queryExecutor;

    this.repositoryId = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);

    if (knowledgeArtifactRepositoryApi == null ||
        !knowledgeArtifactRepositoryApi.getKnowledgeArtifactRepository(repositoryId)
            .isSuccess()) {
      throw new IllegalStateException(
          "Unable to construct an Asset repository on an inconsistent Artifact repository");
    }
  }

  @Override
  public Answer<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth, String xAccept) {
    return bundler.getKnowledgeArtifactBundle(assetId, versionTag, assetRelationship, -1, xAccept);
  }

  @Override
  public Answer<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID assetId,
      String versionTag, String assetRelationship, Integer depth) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<Bindings>> queryKnowledgeAssets(KnowledgeCarrier graphQuery) {
    if (queryExecutor == null) {
      return Answer.unsupported();
    }
    return queryExecutor.askQuery(KB_UUID, IdentifierConstants.VERSION_LATEST, graphQuery);
  }

  @Override
  public Answer<UUID> initKnowledgeAsset() {
    KnowledgeAsset surrogate = new KnowledgeAsset();

    ResourceIdentifier newId = SurrogateBuilder.randomAssetId();
    surrogate.setAssetId(newId);

    this.setVersionedKnowledgeAsset(newId.getUuid(), newId.getVersionTag(), surrogate);

    return Answer.of(ResponseCodeSeries.Created, newId.getUuid());
  }

  @Override
  public Answer<Void> addKnowledgeAssetCarrier(UUID assetId, String versionTag,
      byte[] exemplar) {
    ResourceIdentifier artifatcId = SurrogateBuilder.randomArtifactId();

    return setKnowledgeAssetCarrierVersion(
        assetId,
        versionTag,
        artifatcId.getUuid(),
        artifatcId.getVersionTag(),
        exemplar);
  }


  @Override
  public Answer<KnowledgeAssetCatalog> getAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withId(SemanticIdentifier.newId(UUID.randomUUID()))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetTypeSeries.values())
        .withSupportedAnnotations(
            Arrays.stream(AnnotationRelTypeSeries.values())
                .map(Enum::name)
                .collect(Collectors.joining(","))
        ).withSurrogateModels(
            AbstractCarrier.rep(Knowledge_Asset_Surrogate_2_0, XML_1_1),
            AbstractCarrier.rep(Knowledge_Asset_Surrogate_2_0, JSON),
            AbstractCarrier.rep(Knowledge_Asset_Surrogate_2_0, RDF_1_1),
            AbstractCarrier.rep(Knowledge_Asset_Surrogate, XML_1_1),
            AbstractCarrier.rep(Knowledge_Asset_Surrogate, JSON),
            AbstractCarrier.rep(FHIR_STU3, JSON)
        )
    );
  }

  /**
   * Attempts to find the best manifestation of a given asset, based on the client's preference, as
   * per content standard negotiation
   * <p>
   * If no preference is expressed, returns the "default" carrier, assuming that there is one and
   * only one carrier (TODO there should be a default preference order, or a non-deterministic
   * selection TODO in case two or more carriers are present)
   * <p>
   * If one or more preferences are expressed, an attempt is made to find the best available carrier
   * that matches the preference(s): returns the binary artifact if inlined or stored in the
   * underlying repository redirects the client if a URL locator is provided otherwise if no carrier
   * matches the preferences, tries to find a translator that can map from any of the available
   * carriers into a desired representation if a translator is found, it is invoked othrewise falls
   * back to the "default" carrier
   * <p>
   * If all else fails, a NOT FOUND error is raised
   *
   * @param assetId    the ID of the asset to find a manifestation of
   * @param versionTag the version of the asset to find a manifestation of
   * @param xAccept    the client's preference, as per content negotiation
   * @return The chosen Knowledge Artifact, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(
      UUID assetId, String versionTag, String xAccept) {

    final KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag)
        .orElse(null);
    if (surrogate == null) {
      return Answer.notFound();
    }

    List<SyntacticRepresentation> preferences = getPreferences(xAccept);
    if (preferences.isEmpty()) {
      return getDefaultCarrier(surrogate);
    }

    Optional<ComputableKnowledgeArtifact> bestAvailableCarrier =
        negotiate(surrogate.getCarriers(), preferences);

    if (bestAvailableCarrier.isPresent()) {
      ComputableKnowledgeArtifact chosenCarrier = bestAvailableCarrier.get();

      if (chosenCarrier.getLocator() != null) {
        return Answer.referTo(chosenCarrier.getLocator(), false);
      } else {
        if (!isEmpty(chosenCarrier.getInlinedExpression())) {
          return Answer.of(AbstractCarrier.of(chosenCarrier.getInlinedExpression())
              .withRepresentation(rep(chosenCarrier.getRepresentation()))
              .withAssetId(surrogate.getAssetId())
              .withArtifactId(chosenCarrier.getArtifactId())
              .withLabel(surrogate.getName()));
        } else {
          ResourceIdentifier artifactId = chosenCarrier.getArtifactId();
          return getKnowledgeAssetCarrierVersion(
              assetId,
              versionTag,
              artifactId.getUuid(),
              artifactId.getVersionTag());
        }
      }
    } else {
      Answer<KnowledgeCarrier> ephemeralArtifact =
          Answer.anyDo(preferences,
              preferred -> attemptTranslation(surrogate, assetId, versionTag, preferred));
      return ephemeralArtifact.isSuccess()
          ? ephemeralArtifact
          : getDefaultCarrier(surrogate);
    }
  }


  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetSurrogate(UUID assetId,
      String versionTag, String xAccept) {
    //TODO Implement content negotiation
    return getVersionedKnowledgeAsset(assetId, versionTag)
        .map(surr -> AbstractCarrier.ofAst(surr)
            .withRepresentation(rep(defaultSurrogateModel))
            .withAssetId(surr.getSurrogate().get(0).getArtifactId())
            .withArtifactId(surr.getSurrogate().get(0).getArtifactId())
        );
  }

  private Answer<KnowledgeCarrier> attemptTranslation(KnowledgeAsset surrogate, UUID assetId,
      String versionTag,
      SyntacticRepresentation targetRepresentation) {
    Optional<ResourceIdentifier> artifactPtr = lookupDefaultCarriers(assetId, versionTag);
    SyntacticRepresentation from;
    if (artifactPtr.isPresent()) {
      from = artifactPtr
          .flatMap(ptr -> getRepresentation(surrogate, ptr))
          .map(AbstractCarrier::rep)
          .orElseThrow(IllegalStateException::new);
    } else {
      from = resolveInlinedArtifact(surrogate)
          .map(ComputableKnowledgeArtifact::getRepresentation)
          .map(AbstractCarrier::rep)
          .orElseThrow(IllegalStateException::new);
    }

    if (translator == null) {
      return Answer.of(Optional.empty());
    }
    return Answer.of(
        translator.listOperators(from, targetRepresentation, null)
            .map(l -> l.get(0))
            .map(KnowledgeProcessingOperator::getOperatorId)
            .flatMap(id -> translator.applyTransrepresentation(
                id,
                resolveInlined(surrogate).map(AbstractCarrier::of).get(),
                new Properties()))
            .getOptionalValue());

  }

  private Answer<KnowledgeCarrier> getDefaultCarrier(KnowledgeAsset surrogate) {
    BinaryCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier()
        .withLevel(Encoded_Knowledge_Expression)
        .withLabel(surrogate.getName())
        .withAssetId(surrogate.getAssetId());

    ResourceIdentifier id = surrogate.getAssetId();
    Optional<ResourceIdentifier> artifactPtr =
        lookupDefaultCarriers(id.getUuid(), id.getVersionTag());

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
    ResourceIdentifier artifactPointer = SurrogateBuilder
        .artifactId(artifactId, artifactVersionTag);
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
    ResourceIdentifier assetPointer = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, assetId, versionTag);
    return Answer.of(
        this.index.getArtifactsForAsset(assetPointer).stream()
            .map(resourceIdentifier -> {
              Pointer p = resourceIdentifier.toPointer();
              p.setHref(this.hrefBuilder
                  .getAssetCarrierVersionHref(assetId.toString(), versionTag,
                      resourceIdentifier.getUuid().toString(),
                      resourceIdentifier.getVersionTag()));

              return p;
            })
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetSurrogateVersion(UUID uuid, String s, String s1,
      String s2) {
    return Answer.unsupported();
  }

  @Override
  public Answer<List<Pointer>> getKnowledgeAssetSurrogateVersions(UUID uuid, String s) {
    return Answer.unsupported();
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

  private KnowledgeAsset getLatestKnowledgeAssetSurrogate(UUID assetId) {
    Answer<byte[]> artifact = this.knowledgeArtifactSeriesApi
        .getLatestKnowledgeArtifact(repositoryId, assetId);

    if (!artifact.isSuccess()) {
      throw new ResourceNotFoundException();
    }

    return artifact
        .map(bytes -> AbstractCarrier.of(bytes)
            .withRepresentation(rep(defaultSurrogateModel, JSON)))
        .flatMap(kc -> parser.lift(kc, Abstract_Knowledge_Expression))
        .flatOpt(kc -> kc.as(KnowledgeAsset.class))
        .orElseThrow(() -> new RuntimeException("Unable to parse Surrogate."));
  }

  private List<SyntacticRepresentation> getPreferences(String xAccept) {
    List<String> codes = MimeTypeUtils.tokenize(xAccept);

    return codes.stream()
        .map(c -> ModelMIMECoder.decode(c)
            .orElse(rep(defaultSurrogateModel)))
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
  public Answer<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId, String versionTag) {
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
              .withRepresentation(rep(defaultSurrogateModel, XML_1_1)),
          new ComputableKnowledgeArtifact()
              .withRepresentation(rep(defaultSurrogateModel, JSON)));
    }

    ResourceIdentifier surrogatePointer = assetSurrogate.getAssetId();

    logger.debug("SAVING ASSET {} : {}", assetId, versionTag);

    this.knowledgeArtifactApi.setKnowledgeArtifactVersion(repositoryId,
        ensureUUID(surrogatePointer.getTag())
            .orElseThrow(IllegalStateException::new),
        surrogatePointer.getVersionTag(),
        JSonUtil.writeJson(assetSurrogate).map(ByteArrayOutputStream::toByteArray)
            .orElseThrow(RuntimeException::new));

    logger.debug("REGISTERING ASSET {} : {}", assetId, versionTag);

    this.index.registerAsset(
        SemanticIdentifier.newId(MAYO_ASSETS_BASE_URI_URI, assetId, versionTag),
        surrogatePointer,
        assetSurrogate.getFormalType(),
        assetSurrogate.getRole(),
        assetSurrogate.getAnnotation(),
        assetSurrogate.getLinks());

    logger.debug("INITIALIZING SUB-ASSETS {} : {}", assetId, versionTag);

    return Answer.of(ResponseCodeSeries.NoContent);
  }

  private boolean identifiersConsistent(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    //checks that assetId and versionTag provided in surrogate match those provided as parameters
    return (assetSurrogate.getAssetId().getUuid().equals(assetId)
        && assetSurrogate.getAssetId().getVersionTag().equals(versionTag));
  }

  private void setIdAndVersionIfMissing(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    ResourceIdentifier surrogateId = assetSurrogate.getAssetId();
    if (surrogateId == null || surrogateId.getUuid() == null || surrogateId.getTag() == null) {
      //If the entire assetId is missing, set it based on parameters.
      assetSurrogate.setAssetId(SurrogateBuilder.assetId(assetId, versionTag));
    } else if (surrogateId.getVersionTag() == null) {
      //If the version tag is missing, set it based on parameter
      assetSurrogate.getAssetId().setVersionTag(versionTag);
    }
  }

  private boolean hasDefaultSurrogateManifestation(KnowledgeAsset assetSurrogate) {
    return assetSurrogate.getSurrogate().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .anyMatch(surr -> surr.getRepresentation().getLanguage().sameAs(defaultSurrogateModel));
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

    return Answer.of(pointers); // was versionPointers CAO
  }


  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(
      final String assetTypeTag,
      final String assetAnnotationTag,
      final String assetAnnotationConcept,
      final Integer offset,
      final Integer limit) {

    Set<ResourceIdentifier> all =
        StaticFilter.filter(assetTypeTag, assetAnnotationTag, assetAnnotationConcept, index);

    List<Pointer> pointers = all.stream()
        .map(ResourceIdentifier::getUuid)
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

    this.index.registerArtifactToAsset(
        SurrogateBuilder.assetId(assetId, versionTag),
        SurrogateBuilder.artifactId(artifactId, artifactVersion));

    KnowledgeAsset asset = retrieveAssetSurrogate(assetId, versionTag)
        .orElseThrow(IllegalStateException::new);

    if (asset.getCarriers().stream()
        .noneMatch(art -> matches(art.getArtifactId(), artifactId, artifactVersion))) {
      // the Artifact needs to be attached to the surrogate
      attachCarrier(asset, artifactId, artifactVersion, exemplar);
      // TODO this should increase the version of the surrogate, but not of the asset!
      setVersionedKnowledgeAsset(assetId, versionTag, asset);
    }
    logger.debug("Artifact has been set on asset {}", asset.getAssetId());

    return Answer.of(ResponseCodeSeries.OK);
  }

  private boolean matches(ResourceIdentifier artifactId, UUID artifactTag, String artifactVersion) {
    return artifactId != null &&
        artifactTag.toString().equals(artifactId.getTag()) &&
        artifactVersion.equals(artifactId.getVersionTag());
  }

  private void attachCarrier(KnowledgeAsset asset, UUID artifactId, String artifactVersion,
      byte[] exemplar) {
    SyntacticRepresentation rep = detector.getDetectedRepresentation(AbstractCarrier.of(exemplar))
        .orElse(new SyntacticRepresentation());
    asset.getCarriers().add(
        new ComputableKnowledgeArtifact()
            .withArtifactId(SurrogateBuilder.artifactId(artifactId, artifactVersion))
            .withRepresentation(new SyntacticRepresentation()
                .withLanguage(rep.getLanguage())
                .withSerialization(rep.getSerialization())
                .withFormat(rep.getFormat())
                .withLexicon(rep.getLexicon())
                .withProfile(rep.getProfile())
            ));
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
    Optional<ResourceIdentifier> surrogatePointer = Optional.ofNullable(
        this.index.getSurrogateForAsset(SurrogateBuilder.assetId(assetId, versionTag)));

    return surrogatePointer.flatMap(ptr ->
        knowledgeArtifactApi.getKnowledgeArtifactVersion(
            repositoryId,
            ptr.getUuid(),
            ptr.getVersionTag(),
            false)
            .map(sr -> AbstractCarrier.of(sr)
                .withRepresentation(rep(defaultSurrogateModel, JSON)))
            .flatMap(kc -> parser.lift(kc, Abstract_Knowledge_Expression))
            .flatOpt(kc -> kc.as(KnowledgeAsset.class))
            .getOptionalValue());
  }

  private Optional<SyntacticRepresentation> getRepresentation(
      KnowledgeAsset surrogate, ResourceIdentifier artifactId) {
    return surrogate.getCarriers().stream()
        .filter(c -> same(c.getArtifactId(), artifactId))
        .filter(ComputableKnowledgeArtifact.class::isInstance)
        .map(ComputableKnowledgeArtifact.class::cast)
        .filter(c -> c.getRepresentation() != null)
        .map(ComputableKnowledgeArtifact::getRepresentation)
        .findFirst();
  }

  private boolean same(ResourceIdentifier resourceId, ResourceIdentifier artifactId) {
    // Just use VID?
    if (resourceId == null) {
      return false;
    }
    return resourceId.getTag().equals(artifactId.getTag()) && resourceId.getVersionTag()
        .equals(artifactId.getVersionTag());
  }

  private Optional<ResourceIdentifier> lookupDefaultCarriers(UUID assetId, String versionTag) {
    ResourceIdentifier assetPointer = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, assetId.toString(), versionTag);
    return lookupDefaultCarriers(assetPointer);
  }

  private Optional<ResourceIdentifier> lookupDefaultCarriers(ResourceIdentifier assetPointer) {
    Set<ResourceIdentifier> artifacts = this.index.getArtifactsForAsset(assetPointer);
    ResourceIdentifier artifact;
    if (artifacts.isEmpty()) {
      return Optional.empty();
    } else if (artifacts.size() > 1) {
      // EMBEDDED should not be used
      artifact = artifacts.stream().filter(a -> !"EMBEDDED".equals(a.getVersionTag())).findFirst()
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


  private List<Pointer> aggregateVersions(List<Pointer> pointers) {
    Map<URI, List<Pointer>> versions = Maps.newHashMap();

    pointers.forEach((pointer -> {
      URI id = pointer.getResourceId();
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

  private Pointer toPointer(ResourceIdentifier resourceIdentifier, HrefType hrefType) {
    Pointer p = resourceIdentifier.toPointer();

    Optional<KnowledgeAsset> asset = this
        .retrieveAssetSurrogate(UUID.fromString(resourceIdentifier.getTag()),
            resourceIdentifier.getVersionTag());

    if (asset.isPresent()) {
      p.setName(asset.get().getName());
      p.setDescription(asset.get().getDescription());

      // only set the Pointer type of the asset has one (and only one) type
      if (asset.get().getFormalType() != null && asset.get().getFormalType().size() == 1) {
        p.setType(asset.get().getFormalType().get(0).getRef());
      }
    }

    URI href;
    switch (hrefType) {
      case ASSET:
        href = this.hrefBuilder.getAssetHref(resourceIdentifier.getTag());
        break;
      case ASSET_VERSION:
        href = this.hrefBuilder
            .getAssetVersionHref(resourceIdentifier.getTag(), resourceIdentifier.getVersionTag());
        break;
      default:
        throw new IllegalStateException();
    }
    p.setHref(href);
    return p;
  }

  protected Optional<byte[]> resolve(ResourceIdentifier pointer) {
    URI location = this.index.getLocation(pointer);

    Matcher matcher = Pattern.compile("^.*/(?:artifacts|assets)/(.*)/versions/(.*)$")
        .matcher(location.toString());
    if (matcher.matches() && matcher.groupCount() == 2) {
      return knowledgeArtifactApi
          .getKnowledgeArtifactVersion(repositoryId,
              ensureUUID(matcher.group(1))
                  .orElseThrow(IllegalStateException::new),
              matcher.group(2),
              false)
          .getOptionalValue();
    } else {
      return FileUtil.readBytes(location);
    }
  }

  private Optional<byte[]> resolveInlined(KnowledgeAsset surrogate) {
    return resolveInlinedArtifact(surrogate)
        .map(inlined -> inlined.getInlinedExpression().getBytes());
  }

  private Optional<ComputableKnowledgeArtifact> resolveInlinedArtifact(KnowledgeAsset surrogate) {
    return surrogate.getCarriers().stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        // && carrier has the right artifactId
        .filter(c -> !Util.isEmpty(c.getInlinedExpression()))
        .findFirst();
  }

}
