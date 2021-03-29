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

import static edu.mayo.kmdp.id.helper.DatatypeHelper.getDefaultVersionId;
import static edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions.CLEARABLE;
import static edu.mayo.kmdp.repository.asset.negotiation.ContentNegotiationHelper.decodePreferences;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static edu.mayo.kmdp.util.StreamUtil.filterAs;
import static edu.mayo.kmdp.util.Util.as;
import static edu.mayo.kmdp.util.Util.coalesce;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.kmdp.util.Util.paginate;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Conflict;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.Created;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NoContent;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotAcceptable;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.OK;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.PreconditionFailed;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofMixedAnonymousComposite;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofMixedNamedComposite;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformNamedComposite;
import static org.omg.spec.api4kp._20200801.Answer.conflict;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.Answer.notFound;
import static org.omg.spec.api4kp._20200801.Answer.succeed;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.timedSemverComparator;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.services.CompositeStructType.GRAPH;
import static org.omg.spec.api4kp._20200801.services.CompositeStructType.NONE;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decode;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getCanonicalSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getComputableCarrierMetadata;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getComputableSurrogateMetadata;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getSurrogateId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getSurrogateMetadata;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRoleSeries.Composite_Knowledge_Asset;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.RDF_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Serialized_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;

import edu.mayo.kmdp.knowledgebase.introspectors.struct.CompositeAssetMetadataIntrospector;
import edu.mayo.kmdp.language.parsers.owl2.JenaOwlParser;
import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.repository.artifact.ClearableKnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.artifact.exceptions.ResourceNotFoundException;
import edu.mayo.kmdp.repository.asset.HrefBuilder.HrefType;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerConfig.KnowledgeAssetRepositoryOptions;
import edu.mayo.kmdp.repository.asset.composite.CompositeHelper;
import edu.mayo.kmdp.repository.asset.index.IdentityMapper;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.StaticFilter;
import edu.mayo.kmdp.repository.asset.negotiation.ContentNegotiationHelper;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.ServerSideException;
import org.omg.spec.api4kp._20200801.api.inference.v4.server.ReasoningApiInternal._askQuery;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactSeriesApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DetectApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.ValidateApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.VersionIdentifier;
import org.omg.spec.api4kp._20200801.id.VersionTagType;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.WeightedRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Component;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateDiffer;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


/**
 * An {@link KnowledgeAssetRepositoryService} implementation based on a JCR document store and a
 * Jenna RDF index.
 */
@Named
@KPServer
public class SemanticKnowledgeAssetRepository implements KnowledgeAssetRepositoryService {

  private static final Logger logger = LoggerFactory
      .getLogger(SemanticKnowledgeAssetRepository.class);

  /**
   * Canonical Knowledge Asset Surrogate metamodel
   */
  private static final KnowledgeRepresentationLanguage
      defaultSurrogateModel = Knowledge_Asset_Surrogate_2_0;

  /**
   * Canonical Knowledge Asset Surrogate metamodel serialization format
   */
  private static final SerializationFormat
      defaultSurrogateFormat = JSON;

  /**
   * Additional serialization formats supported by the Canonical Knowledge Asset Surrogate
   */
  private static final List<SerializationFormat>
      supportedDefaultSurrogateFormats = Arrays.asList(JSON, XML_1_1);

  /**
   * Canonical Concrete Representation of Surrogates, used by this server to exchange surrogates as
   * Knowledge Artifacts
   */
  private static final SyntacticRepresentation
      defaultSurrogateRepresentation = rep(defaultSurrogateModel, defaultSurrogateFormat);

  /**
   * Unique identifier of this repository instance
   */
  private final String repositoryId;

  /* Knowledge Artifact Repository Service Client*/
  private KnowledgeArtifactApiInternal knowledgeArtifactApi;
  private KnowledgeArtifactSeriesApiInternal knowledgeArtifactSeriesApi;

  /* Language Service Client */
  private DeserializeApiInternal parser;

  private DetectApiInternal detector;

  private ValidateApiInternal validator;

  private TransxionApiInternal translator;

  private _askQuery queryExecutor;

  private _applyNamedIntrospectDirect compositeStructIntrospector;

  /* Internal helpers */
  private Index index;

  private HrefBuilder hrefBuilder;

  private IdentityMapper identityMapper;

  private ContentNegotiationHelper negotiator;

  private CompositeHelper compositeHelper;

  /**
   * IMPORTANT! If true this will expose an endpoint to clear all tables.
   */
  @Value("${allowClearAll:false}")
  private boolean allowClearAll = false;

  /**
   * Initializes a new Knowledge Asset Repository Server
   *
   * @param artifactRepo  The Artifact Repository that provides the persistence layer
   * @param parser        De/serialization server to de/serialize Artifacts. At a minimum, MUST
   *                      support the canonical Asset Surrogate metamodel
   * @param detector      Optional Representation Detection service
   * @param validator     Optional Representation validation service
   * @param translator    Optional Transrepresentation service used for content negotiation
   * @param queryExecutor Query interface to get consult the index
   * @param index         The KnowledgeBase that indexes Assets, Artifacts and relationships and
   *                      annotations thereof
   * @param cfg           Configuration object
   */
  public SemanticKnowledgeAssetRepository(
      @Autowired @KPServer KnowledgeArtifactRepositoryService artifactRepo,
      @Autowired @KPServer DeserializeApiInternal parser,
      @Autowired(required = false) @KPServer DetectApiInternal detector,
      @Autowired(required = false) @KPServer ValidateApiInternal validator,
      @Autowired(required = false) @KPServer TransxionApiInternal translator,
      @Autowired @KPComponent _askQuery queryExecutor,
      @Autowired Index index,
      @Autowired(required = false) HrefBuilder hrefBuilder,
      @Autowired KnowledgeAssetRepositoryServerConfig cfg) {

    super();

    this.knowledgeArtifactApi = artifactRepo;
    this.knowledgeArtifactSeriesApi = artifactRepo;

    this.index = index;
    this.hrefBuilder = hrefBuilder != null
        ? hrefBuilder : new HrefBuilder(cfg);
    this.negotiator = new ContentNegotiationHelper(this.hrefBuilder);

    this.parser = parser;
    this.detector = detector;
    this.validator = validator;
    this.translator = translator;

    this.queryExecutor = queryExecutor;

    this.compositeStructIntrospector = new CompositeAssetMetadataIntrospector();

    this.repositoryId = cfg.getTyped(KnowledgeAssetRepositoryOptions.DEFAULT_REPOSITORY_ID);

    this.identityMapper = new IdentityMapper(cfg);

    if (artifactRepo == null ||
        !artifactRepo.getKnowledgeArtifactRepository(repositoryId)
            .isSuccess()) {
      throw new IllegalStateException(
          "Unable to construct an Asset repository on an inconsistent Artifact repository");
    }

    this.compositeHelper = new CompositeHelper();

    if (!allowClearAll && cfg.containsKey(CLEARABLE.getName())) {
      allowClearAll = cfg.getTyped(CLEARABLE);
    }

    index.reset();
  }

  @PostConstruct
  private void logClearAll() {
    if (this.allowClearAll) {
      logger.warn(
          "!!! WARNING !!! The REST endpoint to clear all Asset Repository content is ACTIVE! "
              + "If you did not intend this,"
              + " ensure the environment variable `allowClearAll` is either unset or set to false.");
    } else {
      logger.info("The REST endpoint to clear all Asset Repository content is INACTIVE.");
    }
  }


  private ResourceIdentifier toAssetId(UUID assetId) {
    return identityMapper.toAssetId(assetId);
  }

  private ResourceIdentifier toAssetId(UUID assetId, String versionTag) {
    return identityMapper.toAssetId(assetId, versionTag);
  }

  private ResourceIdentifier toArtifactId(UUID artifactId) {
    return identityMapper.toArtifactId(artifactId);
  }

  private ResourceIdentifier toArtifactId(UUID artifactId, String versionTag) {
    return identityMapper.toArtifactId(artifactId, versionTag);
  }

  //*****************************************************************************************/
  //* Service Metadata
  //*****************************************************************************************/

  /**
   * Retrieves a summary of the capabilities of this Knowledge Asset Repository
   *
   * @return A descriptor of the capabilities of this Knowledge Asset Repository
   */
  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withId(SemanticIdentifier.newId(UUID.randomUUID()))
        .withName("Knowledge Asset Repository")
        .withSupportedAssetTypes(
            KnowledgeAssetTypeSeries.values())
        .withSupportedAnnotations(
            Arrays.stream(SemanticAnnotationRelTypeSeries.values())
                .map(Enum::name)
                .collect(Collectors.joining(","))
        ).withSurrogateModels(
            getAdditionalRepresentations()
        )
    );
  }

  /**
   * Additional representations of Surrogates supported by this server, either by storage,
   * redirection, or translation on demand
   *
   * @return A collection of supported Representations for Surrogates
   */
  public static List<SyntacticRepresentation> getAdditionalRepresentations() {
    return Arrays.asList(
        rep(Knowledge_Asset_Surrogate_2_0, XML_1_1),
        rep(Knowledge_Asset_Surrogate_2_0, JSON),
        rep(Knowledge_Asset_Surrogate_2_0, RDF_1_1),
        rep(HTML, TXT),
        rep(FHIR_STU3, JSON));
  }

  @Override
  public Answer<Void> clearKnowledgeAssetCatalog() {
    if (this.allowClearAll) {
      clear();
      return succeed();
    } else {
      return unsupported();
    }
  }

  //*****************************************************************************************/
  //* Knowledge Graph
  //*****************************************************************************************/

  /**
   * Executes a query on the repository's knowledge graph
   *
   * @param graphQuery The query, as a Knowledge Artifact
   * @return The bindings of the query variables
   */
  @Override
  public Answer<List<Bindings>> queryKnowledgeAssetGraph(KnowledgeCarrier graphQuery) {
    if (queryExecutor == null) {
      return Answer.unsupported();
    }
    ResourceIdentifier kbId = index.getKnowledgeBaseId();
    return queryExecutor.askQuery(kbId.getUuid(), kbId.getVersionTag(), graphQuery, null);
  }

  /**
   * Returns a copy of the repository Knowledge graph
   *
   * @param xAccept A formal MIME type to drive the serialization of the graph
   * @return the Knowledge graph, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeGraph(String xAccept) {
    SyntacticRepresentation rep = decode(xAccept)
        .orElse(rep(OWL_2, RDF_XML_Syntax, XML_1_1));
    if (rep.getLanguage() == null) {
      rep.setLanguage(OWL_2);
    }
    if (!OWL_2.isSameEntity(rep.getLanguage())) {
      return Answer.unacceptable();
    }

    return parser.applyNamedLower(
        JenaOwlParser.id,
        index.asKnowledgeBase().getManifestation(),
        Serialized_Knowledge_Expression,
        ModelMIMECoder.encode(rep),
        null);
  }

  //*****************************************************************************************/
  //* Canonical Surrogate
  //*****************************************************************************************/


  /**
   * Returns a list of Pointers to the Asset (series) currently registered in this repository
   * Supports filtering by semantic annotations. Supports pagination
   *
   * @param assetTypeTag           filter to include assets that have type or role denoted by this
   *                               tag
   * @param assetAnnotationTag     filter to include assets annotated with the asset/concept
   *                               relationship denoted by this tag
   * @param assetAnnotationConcept filter to include assets annotated with this concept
   * @param offset                 (Pagination: start at element offset)
   * @param limit                  (Pagination: do not return more than limit)
   * @return A list of assets
   * @see org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType
   * @see org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole
   * @see edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelType
   */
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
        .map(id -> this.toKnowledgeAssetPointer(
            id,
            HrefType.ASSET,
            codedRep(defaultSurrogateRepresentation)))
        .collect(toList());

    return Answer.of(
        paginate(
            this.aggregateVersions(pointers),
            offset, limit, SemanticIdentifier.timedSemverComparator())
    );
  }

  /**
   * Initializes a new asset with a random ID and an empty surrogate. Version is set to 0.0.0
   *
   * @return the UUID of the newly created surrogate series.
   */
  @Override
  public Answer<UUID> initKnowledgeAsset() {
    ResourceIdentifier newId = randomAssetId(identityMapper.getAssetNamespace());

    KnowledgeAsset surrogate = new KnowledgeAsset()
        .withAssetId(newId);

    this.setKnowledgeAssetVersion(newId.getUuid(), newId.getVersionTag(), surrogate);

    return Answer.of(Created, newId.getUuid());
  }

  /**
   * Lists Assets in this Repository, possibly filtering by type or annotation. For each asset in
   * the collection, deletes it
   *
   * @param assetTypeTag           filter to include assets that have type or role denoted by this
   *                               tag
   * @param assetAnnotationTag     filter to include assets annotated with the asset/concept
   *                               relationship denoted by this tag
   * @param assetAnnotationConcept filter to include assets annotated with this concept
   * @return Success, or the most severe error
   * @see SemanticKnowledgeAssetRepository#deleteKnowledgeAsset(UUID)
   */
  @Override
  public Answer<Void> deleteKnowledgeAssets(String assetTypeTag, String assetAnnotationTag,
      String assetAnnotationConcept) {
    return listKnowledgeAssets(assetTypeTag, assetAnnotationTag, assetAnnotationConcept, 0, -1)
        .mapList(Pointer.class, ptr -> deleteKnowledgeAsset(ptr.getUuid()))
        .flatMap(l -> l.stream().reduce(Answer::merge)
            .orElseGet(() -> Answer.of(NoContent)));
  }

  /**
   * Retrieves the latest version of the canonical surrogate for the latest version of a knowledge
   * asset, in AST/object form
   * <p>
   * Supports a limited form of content negotiation that can only handle the default asset
   * representation itself, or HTML by means of a redirect. The actual serialization of the
   * canonical asset, if needed (e.g. when deployed as a web service) is handled by externally.
   * <p>
   * For full control on the content negotiation and format,
   *
   * @param assetId the id of the asset for which the canonical surrogate is requested
   * @param xAccept MIME type for advanced content negotiation support
   * @return The canonical surrogate in serializable AST/object form, or a redirect to a URL where
   * an HTML variant can be found
   * @see KnowledgeAssetRepositoryApiInternal#getKnowledgeAssetCanonicalSurrogate(UUID)  and related
   * operations
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    return retrieveLatestCanonicalSurrogateForLatestAsset(assetId)
        .flatMap(latestCanonicalSurrogate ->
            negotiator.negotiateCanonicalSurrogate(latestCanonicalSurrogate, xAccept,
                defaultSurrogateRepresentation));
  }

  /**
   * Returns a list of (pointers to the) versions of a given Knowledge Asset
   *
   * @param assetId   The ID of the asset (series)
   * @param offset    (Pagination: start at element offset)
   * @param limit     (Pagination: do not return more than limit)
   * @param beforeTag (upper version tag limit - not implemented)
   * @param afterTag  (lower version tag limit - not implemented)
   * @param sort      sort order (not implemented)
   * @return A list of known version for the given asset
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetVersions(UUID assetId, Integer offset,
      Integer limit, String beforeTag, String afterTag, String sort) {

    if (index.resolve(assetId).isEmpty()) {
      return Answer.notFound();
    }

    List<Pointer> pointers = index.getAssetVersions(assetId).stream()
        .map(ax -> toKnowledgeAssetPointer(
            ax,
            HrefType.ASSET_VERSION,
            codedRep(defaultSurrogateRepresentation)))
        .collect(toList());

    return Answer.of(
        paginate(pointers, offset, limit, SemanticIdentifier.mostRecentFirstComparator()));
  }


  /**
   * Iterates over the (pointers to the) versions of a given Knowledge Asset, attempting to delete
   * each one
   *
   * @param assetId The ID of the asset (series)
   * @return Success, or the most severe error
   */
  @Override
  public Answer<Void> deleteKnowledgeAsset(UUID assetId) {
    return listKnowledgeAssetVersions(assetId).flatMap(
        versions -> {
          Optional<Pointer> assetSeriesId = versions.stream().findAny();
          if (assetSeriesId.isEmpty()) {
            return notFound();
          }
          return versions.stream()
              .map(ptr -> deleteKnowledgeAssetVersion(ptr.getUuid(), ptr.getVersionTag()))
              .reduce(Answer::merge)
              .map(ans -> {
                index.unregisterAsset(assetSeriesId.get());
                return ans;
              }).orElse(failed());
        });
  }

  /**
   * Retrieves the latest version of the Surrogate for a specific version of a knowledge asset, in
   * AST/object form
   * <p>
   * Supports a limited form of content negotiation that can only handle the default asset
   * representation itself, or HTML by means of a redirect. The actual serialization of the
   * canonical asset, if needed (e.g. when deployed as a web service) is handled by externally.
   * <p>
   * For full control on the content negotiation and format,
   *
   * @param assetId    the id of the asset for which the canonical surrogate is requested
   * @param versionTag the version tag of the asset
   * @param xAccept    MIME type for advanced content negotiation support
   * @return The canonical surrogate in serializable AST/object form, or a redirect to a URL where
   * an HTML variant can be found
   * @see KnowledgeAssetRepositoryApiInternal#getKnowledgeAssetVersionCanonicalSurrogate and related
   * operations
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(UUID assetId, String versionTag,
      String xAccept) {
    return retrieveLatestCanonicalSurrogateForAssetVersion(assetId, toSemVer(versionTag))
        .flatMap(assetVersionCanonicalSurrogate ->
            negotiator.negotiateCanonicalSurrogate(assetVersionCanonicalSurrogate, xAccept,
                defaultSurrogateRepresentation));
  }

  /**
   * Registers a Canonical Surrogate (version) for a specific Asset Version The Asset must not have
   * a different canonical surrogate, unless it is equal to the one provided
   * <p>
   * This operation will also check for consistency between the asset Identifier in the provided
   * Surrogate, and the assetId/version provided by the client
   *
   * @param assetId        the Asset (series) ID
   * @param versionTag     the Asset version tag
   * @param assetSurrogate the Canonical Asset Surrogate
   * @return Void
   */
  @Override
  public Answer<Void> setKnowledgeAssetVersion(UUID assetId, String versionTag,
      KnowledgeAsset assetSurrogate) {
    String semVerTag = toSemVer(versionTag);

    setIdAndVersionIfMissing(assetSurrogate, assetId, semVerTag);

    if (!testIdentifiersConsistency(assetSurrogate, assetId, semVerTag)) {
      return Answer.of(Conflict);
    }

    ResourceIdentifier assetIdentifier = toAssetId(assetId, semVerTag);
    ResourceIdentifier surrogateIdentifier = ensureHasCanonicalSurrogateManifestation(
        assetSurrogate);

    ensureSemanticVersionedIdentifiers(assetSurrogate);
    try {
      detectCanonicalSurrogateConflict(assetIdentifier, surrogateIdentifier, assetSurrogate);
    } catch (ServerSideException sse) {
      return Answer.failedOnServer(sse);
    }
    persistCanonicalKnowledgeAssetVersion(
        assetSurrogate.getAssetId(), surrogateIdentifier, assetSurrogate);

    return Answer.of(NoContent);
  }


  @Override
  public Answer<Void> deleteKnowledgeAssetVersion(UUID assetId, String versionTag) {
    Answer<KnowledgeAsset> asset =
        retrieveLatestCanonicalSurrogateForAssetVersion(assetId, toSemVer(versionTag));
    if (asset.isNotFound()) {
      // nothing to delete
      return succeed();
    }

    PublicationStatus status = asset
        .map(KnowledgeAsset::getLifecycle)
        .map(Publication::getPublicationStatus)
        .orElse(Draft);
    switch (asEnum(status)) {
      case Archived:
        return asset.flatMap(ax -> removeAssetVersion(ax, !allowClearAll));
      case Published:
        if (!allowClearAll) {
          return Answer.of(ResponseCodeSeries.Unauthorized);
        }
        return asset.flatMap(ax -> removeAssetVersion(ax, allowClearAll));
      default:
        return asset.flatMap(ax -> removeAssetVersion(ax, true));
    }
  }

  private Answer<Void> removeAssetVersion(KnowledgeAsset asset, boolean hardDelete) {
    Answer<Void> artfs = asset.getCarriers().stream()
        .map(carrier -> removeArtifact(carrier, hardDelete))
        .reduce(Answer::merge)
        // succeed automatically if no artifact to delete
        .orElseGet(Answer::succeed);

    Answer<Void> surrs = asset.getSurrogate().stream()
        .map(surrogate -> removeArtifact(surrogate, hardDelete))
        .reduce(Answer::merge)
        // it should be impossible for no surrogate to exist
        .orElseGet(Answer::failed);

    index.unregisterAssetVersion(asset.getAssetId());
    return Answer.merge(artfs, surrs);
  }

  private Answer<Void> removeArtifact(KnowledgeArtifact artifact, boolean hardDelete) {
    ResourceIdentifier id = artifact.getArtifactId();
    Answer<Void> ans = knowledgeArtifactApi.deleteKnowledgeArtifactVersion(
        repositoryId,
        id.getUuid(), id.getVersionTag(),
        hardDelete);
    if (knowledgeArtifactSeriesApi.isKnowledgeArtifactSeries(repositoryId,id.getUuid(),hardDelete).isSuccess()) {
      knowledgeArtifactSeriesApi.deleteKnowledgeArtifact(repositoryId, id.getUuid(), hardDelete);
    }
    return ans;
  }

  //*****************************************************************************************/
  //* Kowledge Artifacts
  //*****************************************************************************************/

  /**
   * Attempts to find the best manifestation of a given asset, for the latest version of that asset,
   * based on the client's preference, as per content standard negotiation
   *
   * @param assetId the ID of the asset to find a manifestation of
   * @param xAccept the client's preference, as per content negotiation
   * @return The chosen Knowledge Artifact, wrapped in a KnowledgeCarrier
   * @see SemanticKnowledgeAssetRepository#getKnowledgeAssetVersionCanonicalCarrier(UUID, String,
   * String)
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCanonicalCarrier(
      UUID assetId, String xAccept) {
    return getLatestAssetVersion(assetId)
        .map(assetVersionId ->
            getKnowledgeAssetVersionCanonicalCarrier(
                assetVersionId.getUuid(),
                assetVersionId.getVersionTag(),
                xAccept))
        .orElseGet(Answer::notFound);
  }

  /**
   * Attempts to find the best manifestation of a given asset, for the latest version of that asset,
   * based on the client's preference, as per content standard negotiation
   *
   * @param assetId the ID of the asset to find a manifestation of
   * @param xAccept the client's preference, as per content negotiation
   * @return The chosen Knowledge Artifact
   * @see SemanticKnowledgeAssetRepository#getKnowledgeAssetVersionCanonicalCarrier(UUID, String,
   * String)
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetCanonicalCarrierContent(
      UUID assetId, String xAccept) {
    return getKnowledgeAssetCanonicalCarrier(assetId, xAccept)
        .flatOpt(AbstractCarrier::asBinary);
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
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(
      UUID assetId, String versionTag, String xAccept) {
    boolean withNegotiation = !isEmpty(xAccept);
    // retrieves the surrogate, which has the representation information
    return retrieveLatestCanonicalSurrogateForAssetVersion(assetId, toSemVer(versionTag))
        .flatMap(
            surrogate -> {
              List<WeightedRepresentation> preferences = withNegotiation
                  ? decodePreferences(xAccept)
                  : Collections.emptyList();
              // tries to honor the client's preferences,
              // or returns one of the artifacts non-deterministically (usually the first)
              Answer<KnowledgeArtifact> bestAvailableCarrier = withNegotiation
                  ? negotiator.negotiateOrDefault(surrogate.getCarriers(), preferences)
                  : negotiator.anyCarrier(surrogate.getCarriers());

              Answer<KnowledgeCarrier> carrier = bestAvailableCarrier.isSuccess()
                  ? bestAvailableCarrier.flatMap(artf -> getKnowledgeAssetCarrierVersion(
                  assetId,
                  versionTag,
                  artf.getArtifactId().getUuid(),
                  artf.getArtifactId().getVersionTag(),
                  xAccept))
                  : tryConstructEphemeral(surrogate, preferences);

              if (carrier.isNotFound() && withNegotiation
                  && bestAvailableCarrier.map(this::isRedirectable).orElse(false)) {
                return bestAvailableCarrier.flatMap(ka -> Answer.referTo(ka.getLocator(), false));
              } else {
                return carrier;
              }
            }
        );
  }

  /**
   * Attempts to find the best manifestation of a given asset, based on the client's preference, as
   * per content standard negotiation, in binary format
   *
   * @param assetId    the ID of the asset to find a manifestation of
   * @param versionTag the version of the asset to find a manifestation of
   * @param xAccept    the client's preference, as per content negotiation
   * @return The chosen Knowledge Artifact, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetVersionCanonicalCarrierContent(
      UUID assetId, String versionTag, String xAccept) {
    return getKnowledgeAssetVersionCanonicalCarrier(assetId, versionTag, xAccept)
        .flatOpt(KnowledgeCarrier::asBinary);
  }

  /**
   * Lists the Carrier Artifacts for a given Knowledge Asset version Groups the version of each
   * Carrier, and returns the latest
   *
   * @param assetId    The ID of the asset for which to retrieve Carriers
   * @param versionTag The version of the asset
   * @param offset     (Pagination: start at element offset)
   * @param limit      (Pagination: do not return more than limit)
   * @param beforeTag  (upper version tag limit - not implemented)
   * @param afterTag   (lower version tag limit - not implemented)
   * @param sort       sort order (not implemented)
   * @return A sorted and grouped list of known Carriers for the given asset
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetCarriers(UUID assetId, String versionTag,
      Integer offset, Integer limit, String beforeTag, String afterTag, String sort) {

    Optional<ResourceIdentifier> assetRefOpt = index.resolve(assetId, toSemVer(versionTag));

    if (assetRefOpt.isEmpty()) {
      return Answer.notFound();
    }
    ResourceIdentifier assetRef = assetRefOpt.get();

    List<Pointer> pointers = index.getArtifactsForAsset(assetRef).stream()
        .map(artifactId -> index.getCarrierVersions(artifactId.getUuid()))
        .flatMap(Collection::stream)
        .map(artifactId -> this.toKnowledgeArtifactPointer(
            assetRef, artifactId, HrefType.ASSET_CARRIER_VERSION))
        .collect(toList());

    return Answer.of(
        paginate(
            this.aggregateVersions(pointers),
            offset, limit, SemanticIdentifier.timedSemverComparator()));
  }

  /**
   * Retrieves the latest version of a given Carrier for a given asset
   * <p>
   * Supports content negotiation.
   *
   * @param assetId    The Asset (series) id
   * @param versionTag The Asset version tag
   * @param artifactId the Carrier artifact id
   * @param xAccept    formal mime type to negotiate the form of the result
   * @return A Knowledge Carrier with the latest version of the chosen artifact, respecting the user
   * preferences (if any)
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrier(UUID assetId, String versionTag,
      UUID artifactId, String xAccept) {
    return Answer.of(getLatestCarrierVersion(artifactId))
        .flatMap(artifactVersionId ->
            getKnowledgeAssetCarrierVersion(
                assetId,
                versionTag,
                artifactVersionId.getUuid(),
                artifactVersionId.getVersionTag(),
                xAccept));
  }

  /**
   * Retrieves a specific version of a Knowledge Artifact, in its role of carrier of a given
   * Knowledge Asset
   * <p>
   * If content negotiation preferences are specified, this operation will validate that the actual
   * artifact fits the client's preferences, or respond with 'not acceptable' In particular, this
   * operation will not attempt to translate/transform the Artifact (if needed, @see {@link
   * SemanticKnowledgeAssetRepository._getKnowledgeAssetVersionCanonicalCarrier})
   *
   * @param assetId            The id of the Asset for which the Artifact is a Carrier
   * @param versionTag         The version of the Asset for which the Artifact is a Carrier
   * @param artifactId         The id of the Carrier Artifact
   * @param artifactVersionTag The version of the Carrier Artifact
   * @param xAccept            Client's preferences on the Artifact representation, which must fit
   *                           at least one of the preferences, if preferences are specified
   * @return The Carrier Artifact, in binary form, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(
      UUID assetId,
      String versionTag,
      UUID artifactId,
      String artifactVersionTag,
      String xAccept) {
    boolean withNegotiation = !isEmpty(xAccept);

    Answer<KnowledgeAsset> assetMetadata = getKnowledgeAssetVersion(assetId, toSemVer(versionTag));
    Answer<KnowledgeArtifact> artifactMetadata = assetMetadata
        .flatOpt(surr -> getComputableCarrierMetadata(artifactId, artifactVersionTag, surr));

    if (!artifactMetadata.isSuccess()) {
      return Answer.notFound();
    }
    if (!negotiator.isAcceptable(artifactMetadata.get(), xAccept)) {
      return failed(NotAcceptable);
    }

    Answer<KnowledgeCarrier> carrier = artifactMetadata
        .flatMap(meta ->
            retrieveBinaryArtifact(meta)
                .map(bytes -> buildKnowledgeCarrier(
                    assetId, versionTag,
                    artifactId, artifactVersionTag,
                    meta.getRepresentation(),
                    coalesce(meta.getName(), assetMetadata.get().getName()),
                    bytes))
        );

    if (carrier.isNotFound() && withNegotiation && artifactMetadata.map(this::isRedirectable)
        .orElse(false)) {
      return artifactMetadata.flatMap(ka -> Answer.referTo(ka.getLocator(), false));
    } else {
      return carrier;
    }
  }

  /**
   * Retrieves a specific version of a Knowledge Artifact, in its role of carrier of a given
   * Knowledge Asset, in binary form
   *
   * @param assetId            The id of the Asset for which the Artifact is a Carrier
   * @param versionTag         The version of the Asset for which the Artifact is a Carrier
   * @param artifactId         The id of the Carrier Artifact
   * @param artifactVersionTag The version of the Carrier Artifact
   * @param xAccept            Client's preferences on the Artifact representation, which must fit
   *                           at least one of the preferences, if preferences are specified
   * @return The Carrier Artifact, in binary form
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetCarrierVersionContent(
      UUID assetId,
      String versionTag,
      UUID artifactId,
      String artifactVersionTag,
      String xAccept) {
    return getKnowledgeAssetCarrierVersion(assetId, versionTag, artifactId, artifactVersionTag,
        xAccept)
        .flatOpt(KnowledgeCarrier::asBinary);
  }


  /**
   * Stores a (binary) exemplar artifact in the repository, associating it to a knowledge asset
   *
   * @param assetId         the id of the asset
   * @param versionTag      the version of the asset
   * @param artifactId      the id of the carrier artifact
   * @param artifactVersion the version of the carrier artifact
   * @param exemplar        a binary-encoded copy of the artifact
   * @return Void
   */
  @Override
  public Answer<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersion, byte[] exemplar) {

    KnowledgeAsset asset = retrieveLatestCanonicalSurrogateForAssetVersion(assetId,
        toSemVer(versionTag))
        .orElseThrow(IllegalStateException::new);
    ResourceIdentifier artifactRef = toArtifactId(artifactId, artifactVersion);

    logger.debug(
        "ADDING CARRIER TO ASSET {} : {} >>> {} : {}",
        assetId, toSemVer(versionTag), artifactId, artifactVersion);

    persistKnowledgeCarrier(asset.getAssetId(), artifactRef, exemplar);
    updateCanonicalSurrogateWithCarrier(asset, artifactRef, exemplar);

    logger.debug("Artifact has been set on asset {}", asset.getAssetId());

    return Answer.of(OK);
  }

  //*****************************************************************************************/
  //* Surrogates
  //*****************************************************************************************/

  /**
   * Returns the LATEST version of the Canonical Surrogate for the LATEST version of the given
   * Knowledge Asset
   *
   * @param assetId The asset for which to retrieve a canonical surrogate
   * @param xAccept The client's preferences on the representation of the surrogate's content
   * @return A carrier that wraps the Canonical Surrogate, or transrepresentation thereof
   * @see SemanticKnowledgeAssetRepository#getKnowledgeAssetVersionCanonicalSurrogate(UUID, String,
   * String)
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCanonicalSurrogate(UUID assetId,
      String xAccept) {
    return getLatestAssetVersion(assetId)
        .map(assetVersionId ->
            getKnowledgeAssetVersionCanonicalSurrogate(
                assetVersionId.getUuid(),
                assetVersionId.getVersionTag(),
                xAccept))
        .orElseGet(Answer::notFound);
  }

  /**
   * Returns the Canonical Surrogate for the given Knowledge Asset, serialized using the server's
   * default format, and encoded in binary form. To get the Surrogate as an object, use {@link
   * SemanticKnowledgeAssetRepository#}getVersionedKnowledgeAsset} and related operations instead.
   * <p>
   * Supports content negotiation to create alternative representation of the same Surrogate
   *
   * @param assetId    The asset for which to retrieve a canonical surrogate
   * @param versionTag The asset version for which to retrieve a canonical surrogate
   * @param xAccept    The client's preferences on the representation of the surrogate's content
   * @return A carrier that wraps the Canonical Surrogate, or transrepresentation thereof
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalSurrogate(UUID assetId,
      String versionTag, String xAccept) {
    boolean withNegotiation = !Util.isEmpty(xAccept);
    List<WeightedRepresentation> preferences =
        decodePreferences(xAccept, defaultSurrogateRepresentation);
    SerializationFormat preferredFormat = negotiator.getPreferredFormat(preferences).orElse(null);

    return getKnowledgeAssetVersion(assetId, versionTag)
        .flatMap(asset -> {
              KnowledgeArtifact self = getCanonicalSurrogateMetadata(
                  asset,
                  preferredFormat)
                  .orElseThrow(
                      () -> new IllegalStateException(
                          "Surrogates should have self-referential metadata"));
              Answer<KnowledgeCarrier> binaryCarrier =
                  buildCanonicalSurrogateCarrier(self.getArtifactId(), asset, preferredFormat);

              return withNegotiation ?
                  binaryCarrier.flatMap(bin ->
                      Answer.firstDo(
                          preferences,
                          preferredRep -> attemptTranslation(bin, preferredRep.getRep())))
                  : binaryCarrier;
            }
        );
  }

  /**
   * Returns a list of pointers to the Surrogates registered for a given Knowledge Asset
   *
   * @param assetId
   * @param versionTag
   * @return
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetSurrogates(UUID assetId, String versionTag,
      Integer offset, Integer limit, String beforeTag, String afterTag, String sort) {
    Optional<ResourceIdentifier> assetRefOpt = index.resolve(assetId, versionTag);

    if (assetRefOpt.isEmpty()) {
      return Answer.notFound();
    }
    ResourceIdentifier assetRef = assetRefOpt.get();

    List<Pointer> pointers = index.getSurrogatesForAsset(assetRef).stream()
        .map(surrId -> index.getSurrogateVersions(surrId.getUuid()))
        .flatMap(Collection::stream)
        .map(surrId -> this.toKnowledgeArtifactPointer(
            assetRef, surrId, HrefType.ASSET_SURROGATE_VERSION))
        .collect(toList());

    return Answer.of(
        paginate(
            this.aggregateVersions(pointers),
            offset, limit, SemanticIdentifier.timedSemverComparator()));
  }


  /**
   * Retrieves a specific version of a Knowledge Artifact, in its role of Surrogate of a given
   * Knowledge Asset
   * <p>
   * If content negotiation preferences are specified, this operation will validate that the actual
   * artifact fits the client's preferences, or respond with 'not acceptable' In particular, this
   * operation will not attempt to translate/transform the Artifact (if needed, @see {@link
   * SemanticKnowledgeAssetRepository#getKnowledgeAssetVersionCanonicalSurrogate} )
   *
   * @param assetId             The id of the Asset for which the Artifact is a Carrier
   * @param versionTag          The version of the Asset for which the Artifact is a Carrier
   * @param surrogateId         The id of the Surrogate Artifact
   * @param surrogateVersionTag The version of the Surrogate Artifact
   * @param xAccept             Client's preferences on the Artifact representation, which must fit
   *                            at least one of the preferences, if preferences are specified
   * @return The Surrogate Artifact, in binary form, wrapped in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetSurrogateVersion(
      UUID assetId, String versionTag, UUID surrogateId, String surrogateVersionTag,
      String xAccept) {

    Answer<KnowledgeAsset> assetMetadata = getKnowledgeAssetVersion(assetId, versionTag);

    // get the specific surrogate requested by the client
    Answer<KnowledgeArtifact> surrogateMetadata = assetMetadata
        .flatOpt(surr -> getComputableSurrogateMetadata(surrogateId, surrogateVersionTag, surr));

    if (!surrogateMetadata.isSuccess()) {
      return Answer.notFound();
    }

    boolean isAcceptable = negotiator.isAcceptable(surrogateMetadata.get(), xAccept);

    Answer<KnowledgeCarrier> surrogate = Answer.notFound();
    if (isAcceptable) {
      // retrieve a copy of the surrogate
      surrogate = surrogateMetadata
          .flatMap(meta ->
              retrieveBinaryArtifact(meta)
                  .map(bytes -> buildKnowledgeCarrier(
                      assetId, versionTag,
                      surrogateId, surrogateVersionTag,
                      meta.getRepresentation(),
                      "Metadata - " + coalesce(meta.getName(), assetMetadata.get().getName()),
                      bytes))
          );
    }

    if (!isAcceptable || surrogate.isNotFound()) {
      // fall back to the canonical surrogate
      Answer<KnowledgeArtifact> canonicalSurrogate = assetMetadata
          .flatOpt(this::getCanonicalSurrogateMetadata);

      return decodePreferences(
          xAccept, surrogateMetadata.get().getRepresentation()).stream()
          .map(tgtRep ->
              // retrieve it and see if it can be trans*ed into the required format
              canonicalSurrogate
                  .flatMap(canonicalSurrogateMeta ->
                      attemptTranslation(
                          assetMetadata.get(),
                          canonicalSurrogateMeta,
                          rep(tgtRep.getRep().getLanguage(), tgtRep.getRep().getFormat(),
                              defaultCharset(), Encodings.DEFAULT))))
          .findFirst()
          .orElse(Answer.unacceptable());
    } else {
      return surrogate;
    }
  }

  //*****************************************************************************************/
  //* Composites and Structs
  //*****************************************************************************************/


  /**
   * Registers a Canonical Asset Surrogate in this server, for a given Asset and Version
   * <p>
   * Supports Composite Assets
   *
   * @param assetId               The Asset for which a Surrogate is being registered
   * @param versionTag            The version of the Asset for which a Surrogate is being
   *                              registered
   * @param assetSurrogateCarrier The Carrier of a (Composite) Canonical Surrogate
   * @return
   */
  @Override
  public Answer<Void> addCanonicalKnowledgeAssetSurrogate(UUID assetId, String versionTag,
      KnowledgeCarrier assetSurrogateCarrier) {

    if (assetSurrogateCarrier instanceof CompositeKnowledgeCarrier) {
      CompositeKnowledgeCarrier ckc = ((CompositeKnowledgeCarrier) assetSurrogateCarrier);

      Answer<Void> ans = ckc.getComponent().stream()
          .map(comp -> addCanonicalKnowledgeAssetSurrogate(
              comp.getAssetId().getUuid(), comp.getAssetId().getVersionTag(), comp))
          .reduce(Answer::merge)
          .orElseGet(() -> Answer.of(NoContent));

      // exclude aggregates with no struct
      // exclude anonymous composites, where the 'root' Surrogate is already one of the components
      if (ckc.getStructType() != null && ckc.getStructType() != NONE
          && ckc.tryMainComponentAs(KnowledgeAsset.class).isEmpty()) {
        Answer<Void> compositeAns =
            compositeStructIntrospector.applyNamedIntrospectDirect(
                CompositeAssetMetadataIntrospector.id, ckc, null)
                .flatOpt(kc -> kc.as(KnowledgeAsset.class))
                .flatMap(ax -> setKnowledgeAssetVersion(
                    ax.getAssetId().getUuid(), ax.getAssetId().getVersionTag(), ax));
        ans = Answer.merge(ans, compositeAns);
      }

      return ans;
    } else {
      return liftCanonicalSurrogate(assetSurrogateCarrier)
          .flatMap(ax -> setKnowledgeAssetVersion(
              ax.getAssetId().getUuid(), ax.getAssetId().getVersionTag(), ax));
    }
  }


  /**
   * Constructs an Anonymous Composite Knowledge Asset (Surrogate), using a given Asset as a seed,
   * by means of a query that traverses the relationships between that Asset and other Assets, which
   * will be considered the Components.
   *
   * @param assetId    The id of the seed Asset
   * @param versionTag The version of the seed Asset
   * @param xAccept    A content negotiation parameter to control the format in which the Components
   *                   are returned(NOT IMPLEMENTED)
   * @return a Composite Knowledge Carrier that includes the Canonical Surrogates (in AST form)
   */
  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetSurrogate(
      UUID assetId, String versionTag, String xAccept) {
    SerializationFormat fmt = negotiator.decodePreferredFormat(xAccept, defaultSurrogateFormat);

    return Answer.of(index.resolve(assetId, versionTag))
        .flatMap(rootId -> compositeHelper.getComponentsQuery(rootId, Depends_On))
        .flatMap(this::getComponentIds)
        .flatMap(componentIds ->
            componentIds.stream()
                // retrieve available surrogates for components
                .map(comp -> getKnowledgeAssetVersion(comp.getUuid(), comp.getVersionTag(), xAccept)
                    // encode
                    .flatMap(surr -> encodeCanonicalSurrogate(surr, fmt)))
                .collect(Answer.toList())
                // combine into a Composite - NO Struct
                .map(components -> ofUniformAnonymousComposite(
                    toAssetId(assetId, versionTag), components)));
  }


  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetCarrier(
      UUID assetId, String versionTag, String xAccept) {
    ResourceIdentifier rootId = toAssetId(assetId, versionTag);
    if (!index.isKnownAsset(rootId)) {
      return Answer.notFound();
    }

    return compositeHelper.getComponentsQuery(rootId, Depends_On)
        .flatMap(this::getComponentIds)
        .flatMap(componentIds -> {

          Answer<Set<KnowledgeCarrier>> componentSurrogates = componentIds.stream()
              .map(comp -> getKnowledgeAssetVersionCanonicalCarrier(comp.getUuid(),
                  comp.getVersionTag(),
                  xAccept))
              .collect(Answer.toSet());

          return componentSurrogates
              .map(comps -> ofMixedAnonymousComposite(rootId, comps));
        });
  }


  /**
   * Returns Pointers to the versions of a given surrogate of a given (version of a) KnowledgeAsset
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetSurrogateVersions(UUID assetId, String versionTag,
      UUID surrogateId, Integer offset, Integer limit, String beforeTag, String afterTag,
      String sort) {
    Optional<ResourceIdentifier> axIdOpt = index.resolve(assetId, versionTag);

    if (axIdOpt.isEmpty()) {
      return Answer.notFound();
    }
    ResourceIdentifier axId = axIdOpt.get();

    if (index.getSurrogatesForAsset(axId).stream()
        .noneMatch(cId -> cId.getUuid().equals(surrogateId))) {
      return Answer.notFound();
    }

    return Answer.of(
        paginate(
            index.getSurrogateVersions(surrogateId).stream()
                .map(SemanticIdentifier::toPointer)
                .map(ptr -> ptr.withHref(
                    hrefBuilder.getHref(
                        axId,
                        ptr,
                        HrefType.ASSET_SURROGATE_VERSION)
                )).collect(toList()),
            offset, limit, SemanticIdentifier.timedSemverComparator()));
  }

  /**
   * Returns Pointers to the versions of a given carrier of a given (version of a) KnowledgeAsset
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetCarrierVersions(UUID assetId, String versionTag,
      UUID artifactId) {
    Optional<ResourceIdentifier> axIdOpt = index.resolve(assetId, versionTag);

    if (axIdOpt.isEmpty()) {
      return Answer.notFound();
    }
    ResourceIdentifier axId = axIdOpt.get();
    if (index.getArtifactsForAsset(axId).stream()
        .noneMatch(cId -> cId.getUuid().equals(artifactId))) {
      return Answer.notFound();
    }

    return Answer.of(
        paginate(
            index.getCarrierVersions(artifactId).stream()
                .map(SemanticIdentifier::toPointer)
                .map(ptr -> ptr.withHref(
                    hrefBuilder.getHref(
                        axId,
                        ptr,
                        HrefType.ASSET_CARRIER_VERSION)
                )).collect(toList()),
            0, -1, SemanticIdentifier.timedSemverComparator()));
  }


  @Override
  public Answer<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID assetId,
      String versionTag, String xAccept) {
    ResourceIdentifier rootId = toAssetId(assetId, versionTag);

    Answer<List<Bindings>> ans = compositeHelper.getStructQuery(rootId)
        .flatMap(this::queryKnowledgeAssetGraph);

    Answer<ResourceIdentifier> structId =
        ans.flatOpt(compositeHelper::getStructId);

    if (!structId.isSuccess()) {
      return failed(new IllegalStateException("Unable to find structuring asset ID"));
    }

    return ans.flatMap(binds -> compositeHelper.toEncodedStructGraph(structId.get(), binds));
  }

  /**
   * @param assetId
   * @param versionTag
   * @param flat
   * @param xAccept
   * @return
   */
  @Override
  public Answer<CompositeKnowledgeCarrier> getCompositeKnowledgeAssetSurrogate(UUID assetId,
      String versionTag, Boolean flat, String xAccept) {
    SerializationFormat fmt = negotiator.decodePreferredFormat(xAccept, defaultSurrogateFormat);

    Answer<KnowledgeAsset> compositeSurr = getKnowledgeAssetVersion(assetId, versionTag, xAccept);
    if (!compositeSurr.isSuccess()) {
      return failed(compositeSurr.getOutcomeType());
    }

    Answer<ResourceIdentifier> structId = compositeSurr.flatOpt(compositeHelper::getStructId);
    if (structId.isFailure()
        && compositeSurr.map(KnowledgeAsset::getRole).map(Composite_Knowledge_Asset::isNoneOf)
        .orElse(true)) {
      return Answer.failedOnServer(new ServerSideException(PreconditionFailed));
    }

    Answer<ResourceIdentifier> rootId = compositeHelper.getRootQuery(toAssetId(assetId, versionTag))
        .flatMap(this::queryKnowledgeAssetGraph)
        .flatOpt(binds -> compositeHelper.getRootId(binds));

    return compositeSurr.flatMap(composite -> {
      List<KnowledgeCarrier> components =
          index.getRelatedAssets(composite.getAssetId(),
              Has_Structural_Component.getReferentId()).stream()
              .map(cid -> getKnowledgeAssetVersion(cid.getUuid(), cid.getVersionTag(), xAccept)
                  .flatMap(ax -> encodeCanonicalSurrogate(ax, fmt)))
              .flatMap(Answer::trimStream)
              .collect(Collectors.toList());

      Answer<KnowledgeCarrier> structure =
          getCompositeKnowledgeAssetStructure(assetId, versionTag)
              .or(() -> inferBasicStruct(composite, structId.orElse(null)));

      return structure.map(struct ->
          ofUniformNamedComposite(
              composite.getAssetId(),
              null,
              rootId.orElse(null),
              compositeSurr.map(KnowledgeAsset::getName).orElse(""),
              GRAPH,
              struct,
              components));
    });
  }

  /**
   * Uses the Links in a Surrogate to derive a (TREE-based) struct for a composite
   * <p>
   * This method is not recusrive, and is used as a fallback when getCompositeKnowledgeAssetStructure
   * fails or is not supported Conversely, it is more efficient since it does not have to rely on a
   * SPARQL query
   * <p>
   * Note: this method performs the inverse of operation of a CompositeMetadataIntrospector, which
   * derives a Surrogate from a Structure instead
   *
   * @param composite The Surrogate of a Composite Knowledge Asset that has Component Links
   * @param structId
   * @see SemanticKnowledgeAssetRepository::getCompositeKnowledgeAssetStructure
   * @see Component
   * @see edu.mayo.kmdp.knowledgebase.introspectors.struct.CompositeAssetMetadataIntrospector
   */
  private Answer<KnowledgeCarrier> inferBasicStruct(
      KnowledgeAsset composite, SemanticIdentifier structId) {
    Model model = ModelFactory.createDefaultModel();
    composite.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Component.class))
        .map(link ->
            objA(composite.getAssetId().getVersionId(),
                link.getRel().getReferentId(),
                link.getHref().getVersionId()))
        .forEach(model::add);
    return new JenaRdfParser().applyLower(
        ofAst(model)
            .withAssetId(
                as(structId, ResourceIdentifier.class).orElseGet(SurrogateBuilder::randomAssetId))
            .withRepresentation(rep(OWL_2)),
        Encoded_Knowledge_Expression,
        codedRep(OWL_2, Turtle, TXT, defaultCharset(), Encodings.DEFAULT),
        null);
  }


  @Override
  public Answer<KnowledgeCarrier> getAnonymousCompositeKnowledgeAssetStructure(
      UUID assetId, String versionTag, String xAccept) {
    ResourceIdentifier rootId = toAssetId(assetId, versionTag);
    if (!index.isKnownAsset(rootId)) {
      return Answer.notFound();
    }
    return compositeHelper.getAnonStructQuery(rootId)
        .flatMap(this::queryKnowledgeAssetGraph)
        .flatMap(binds -> compositeHelper.toEncodedStructGraph(binds));
  }

  @Override
  public Answer<CompositeKnowledgeCarrier> getCompositeKnowledgeAssetCarrier(UUID assetId,
      String versionTag,
      Boolean flat, String xAccept) {
    ResourceIdentifier compositeAssetId = toAssetId(assetId, versionTag);

    Answer<KnowledgeAsset> compositeSurr = getKnowledgeAssetVersion(assetId, versionTag, xAccept);
    if (!compositeSurr.isSuccess()) {
      return failed(compositeSurr.getOutcomeType());
    }

    Answer<KnowledgeCarrier> struct = getCompositeKnowledgeAssetStructure(assetId, versionTag);
    if (!struct.isSuccess()) {
      return Answer.failedOnServer(new ServerSideException(PreconditionFailed));
    }

    Answer<ResourceIdentifier> rootId = compositeHelper.getRootQuery(compositeAssetId)
        .flatMap(this::queryKnowledgeAssetGraph)
        .flatOpt(binds -> compositeHelper.getRootId(binds));

    return compositeHelper.getComponentsQuery(compositeAssetId, Has_Structural_Component)
        .flatMap(this::getComponentIds)
        .map(comps -> comps.stream()
            .map(compId ->
                getKnowledgeAssetVersionCanonicalCarrier(compId.getUuid(), compId.getVersionTag(),
                    xAccept))
            .flatMap(Answer::trimStream)
            .collect(toList()))
        .map(carriers -> ofMixedNamedComposite(
            compositeAssetId,
            null,
            rootId.orElse(null),
            compositeSurr.map(KnowledgeAsset::getName).orElse(null),
            GRAPH,
            struct.get(),
            carriers
        ));
  }

  @Override
  public Answer<Void> addKnowledgeAssetCarrier(UUID assetId, String versionTag,
      KnowledgeCarrier assetCarrier) {
    Answer<KnowledgeAsset> currentSurrogate = getKnowledgeAsset(assetId, versionTag);
    if (!currentSurrogate.isSuccess()) {
      return failed(currentSurrogate);
    }

    ResourceIdentifier artifactId = assetCarrier.getArtifactId();
    KnowledgeAsset surr = currentSurrogate.get();

    Optional<KnowledgeArtifact> registeredCarrier =
        getComputableCarrierMetadata(artifactId.getUuid(), artifactId.getVersionTag(), surr);
    if (registeredCarrier.isPresent()) {
      Answer<KnowledgeCarrier> existing =
          this.getKnowledgeAssetCarrierVersion(assetId, versionTag, artifactId.getUuid(),
              artifactId.getVersionTag());
      if (existing.isSuccess()) {
        byte[] existingBinary = existing.get().asBinary().orElseThrow();
        byte[] addedBinary = assetCarrier.asBinary().orElseThrow();
        return Arrays.equals(existingBinary, addedBinary)
            ? succeed()
            : conflict();
      }
    }

    // add the Carrier
    surr.withCarriers(
        new KnowledgeArtifact()
            .withName(assetCarrier.getLabel())
            .withArtifactId(artifactId)
            .withRepresentation(assetCarrier.getRepresentation()));

    SurrogateBuilder.updateSurrogateVersion(surr);

    Answer<Void> a1 = setKnowledgeAssetVersion(assetId, versionTag, surr);
    if (!a1.isSuccess()) {
      return failed(a1);
    }

    Answer<Void> a2 = assetCarrier.asBinary().map(binary ->
        setKnowledgeAssetCarrierVersion(
            assetId, versionTag,
            artifactId.getUuid(), artifactId.getVersionTag(),
            binary))
        .orElseGet(Answer::failed);

    return Answer.merge(a1, a2);
  }

  //*****************************************************************************************/
  //* Not yet implemented
  //*****************************************************************************************/

  @Override
  public Answer<Void> addKnowledgeAssetSurrogate(UUID uuid, String s,
      KnowledgeCarrier knowledgeCarrier) {
    return Answer.unsupported();
  }

// ****************************************************************************************************/
// Internal functions and helpers
// ****************************************************************************************************/


  /**
   * Queries the Knowledge Graph to select a set of Knowledge Asset's version identifiers
   *
   * @param query The query
   * @return The set of Asset version identifiers selected by the query
   */
  private Answer<Set<ResourceIdentifier>> getComponentIds(KnowledgeCarrier query) {
    return this.queryKnowledgeAssetGraph(query)
        .map(binds -> binds.stream()
            // assume there is only one binding, and the binding is an asset Id
            .map(bind -> URI.create(bind.values().iterator().next().toString()))
            .map(SemanticIdentifier::newVersionId)
            .collect(Collectors.toSet()));
  }

  /**
   * Ensures that minimal metadata about a carrier KnowledgeArtifact appear in the Canonical
   * Surrogate's Carriers section If an Artifact with the same id is not already included, updates
   * the Surrogate and persists the new version
   *
   * @param asset     the Canonical Surrogate
   * @param carrierId the Id of the artifact
   * @param exemplar  a copy of the carrier Artifact, used to infer metadata
   * @return A flag that indicates whether an update was actually performed or not
   */
  private boolean updateCanonicalSurrogateWithCarrier(KnowledgeAsset asset,
      ResourceIdentifier carrierId, byte[] exemplar) {
    if (asset.getCarriers().stream()
        .noneMatch(
            art -> art.getArtifactId().sameAs(carrierId.getUuid(), carrierId.getVersionTag()))) {
      // the Artifact needs to be attached to the surrogate
      ResourceIdentifier newSurrogateId = attachCarrier(asset, carrierId, exemplar);
      persistCanonicalKnowledgeAssetVersion(asset.getAssetId(), newSurrogateId, asset);
      return true;
    }
    return false;
  }

  /**
   * Adds metadata about a Knowledge Carrier to a Canonical Surrogate, updating the Surrogate in the
   * process Increments the identifier of the Surrogate itself by a minor version increment.
   *
   * @param asset      the Canonical Surrogate
   * @param artifactId the Id of the artifact
   * @param exemplar   a copy of the carrier Artifact, used to infer metadata
   * @return A flag that indicates whether an update was actually performed or not
   */
  private ResourceIdentifier attachCarrier(
      KnowledgeAsset asset, ResourceIdentifier artifactId, byte[] exemplar) {
    asset.withCarriers(
        new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withRepresentation(
                detector.applyDetect(of(exemplar))
                    .map(KnowledgeCarrier::getRepresentation)
                    .orElse(rep(null, null, defaultCharset(), Encodings.DEFAULT))
            ));

    return getCanonicalSurrogateId(asset)
        .map(id -> toArtifactId(id.getUuid(),
            id.getSemanticVersionTag().incrementMinorVersion().toString()))
        .map(newId -> setCanonicalSurrogateId(asset, newId))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Tries to construct a Knowledge Artifact that matches one of the given preferences, using the
   * existing Asset and its material Carriers as inputs
   *
   * @param asset       The Asset for which an ephemeral Artifact is desired
   * @param preferences The preferred representations, sorted by preference
   * @return The result of a transrepresentation the Asset's material Carrier(s) into an ephemeral
   * Knowledge Artifact
   */
  private Answer<KnowledgeCarrier> tryConstructEphemeral(KnowledgeAsset asset,
      List<WeightedRepresentation> preferences) {
    if (translator == null) {
      return Answer.unacceptable();
    }
    return Answer.firstDo(
        preferences,
        preferred -> attemptTranslation(asset, preferred.getRep()),
        Answer::unacceptable);
  }

  /**
   * Tries to construct a Knowledge Artifact that matches the given preference, using the existing
   * Asset and its material Carriers as inputs
   *
   * @param asset                The Asset for which an ephemeral Artifact is desired
   * @param targetRepresentation The preferred representation
   * @return The result of a transrepresentation the Asset's material Carrier(s) into an ephemeral
   * Knowledge Artifact
   */
  private Answer<KnowledgeCarrier> attemptTranslation(KnowledgeAsset asset,
      SyntacticRepresentation targetRepresentation) {
    if (translator == null) {
      return Answer.unacceptable();
    }
    List<KnowledgeArtifact> computableCarriers =
        asset.getCarriers().stream()
            .flatMap(filterAs(KnowledgeArtifact.class))
            .collect(toList());

    return Answer.anyDo(computableCarriers,
        carrier -> attemptTranslation(asset, carrier, targetRepresentation));
  }

  /**
   * Tries to construct a Knowledge Artifact that matches the given preference, using an existing
   * Carrier metadata as input Will resolve the metadata into an actual Artifact, then attempt the
   * transformation
   *
   * @param asset                The Asset for which the Artifact is provided
   * @param carrier              The Artifact to use as a source for the transrepresentation
   * @param targetRepresentation The preferred representation
   * @return The result of a transrepresentation the Artifact into an ephemeral Knowledge Artifact
   */
  private Answer<KnowledgeCarrier> attemptTranslation(KnowledgeAsset asset,
      KnowledgeArtifact carrier, SyntacticRepresentation targetRepresentation) {
    if (translator == null) {
      return Answer.unacceptable();
    }
    return retrieveWrappedBinaryArtifact(asset, carrier)
        .flatMap(sourceCarrier -> attemptTranslation(sourceCarrier, targetRepresentation));
  }

  /**
   * Tries to construct a Knowledge Artifact that matches the given preference, using an existing
   * Artifact as input
   *
   * @param sourceBinaryArtifact The Artifact to use as a source for the transrepresentation
   * @param targetRepresentation The preferred representation
   * @return The result of a transrepresentation the Artifact into an ephemeral Knowledge Artifact
   */
  private Answer<KnowledgeCarrier> attemptTranslation(
      KnowledgeCarrier sourceBinaryArtifact, SyntacticRepresentation targetRepresentation) {
    if (translator == null) {
      return Answer.unacceptable();
    }
    SyntacticRepresentation from = sourceBinaryArtifact.getRepresentation();
    if (from.getLanguage().sameAs(targetRepresentation.getLanguage())
        && from.getFormat().sameAs(targetRepresentation.getFormat())) {
      return Answer.of(sourceBinaryArtifact);
    }

    return translator.listTxionOperators(encode(from), encode(targetRepresentation))
        .flatMap(tranxOperators ->
            Answer.anyDo(
                tranxOperators,
                txOp -> translator.applyNamedTransrepresent(
                    txOp.getOperatorId().getUuid(),
                    sourceBinaryArtifact,
                    encode(targetRepresentation),
                    null)
            ));
  }

  /**
   * Instantiates a KnowledgeCarrier wrapper for a given binary encoding of a Knowledge Artifact
   *
   * @param assetId            the Id of the Asset the Artifact is a manifestation of
   * @param versionTag         the version of the Asset
   * @param artifactId         the Id of the Artifact itself
   * @param artifactVersionTag the version of the Artifact
   * @param representation     the Representation metadata of the artitfact
   * @param bytes              the Artifact itself, binary encoded
   * @return the Artifact wrapped in a KnowledgeCarrier
   */
  private KnowledgeCarrier buildKnowledgeCarrier(UUID assetId, String versionTag, UUID artifactId,
      String artifactVersionTag,
      SyntacticRepresentation representation, String label, byte[] bytes) {
    ResourceIdentifier axtId = index.resolve(assetId, toSemVer(versionTag))
        .orElseThrow();
    ResourceIdentifier artId = index.resolve(artifactId, toSemVer(artifactVersionTag))
        .orElseThrow();

    SyntacticRepresentation rep = (SyntacticRepresentation) representation.clone();

    return of(bytes)
        .withAssetId(axtId)
        .withArtifactId(artId)
        .withLevel(Encoded_Knowledge_Expression)
        .withHref(hrefBuilder.getHref(axtId, artId, HrefType.ASSET_CARRIER_VERSION))
        .withLabel(label)
        .withRepresentation(rep
            .withCharset(defaultCharset().name())
            .withEncoding("default"));
  }

  /**
   * Instantiates a KnowledgeCarrier wrapper for a Canonical Knowledge Surrogate, serializing it
   * into binary form in the process
   *
   * @param surrId the Id of the Canonical Surrogate
   * @param asset  The Canonical Surrogate
   */
  private Answer<KnowledgeCarrier> buildCanonicalSurrogateCarrier(ResourceIdentifier surrId,
      KnowledgeAsset asset, SerializationFormat format) {
    return encodeCanonicalSurrogate(asset, format)
        // level and representation are set during the encoding process
        .map(kc -> kc
            .withAssetId(surrId)
            .withArtifactId(surrId)
            .withHref(hrefBuilder
                .getHref(asset.getAssetId(), surrId, HrefType.ASSET_SURROGATE_VERSION))
            .withLabel("Metadata - " + asset.getName()));
  }

  /**
   * Uses the Knowledge Artifact metadata to retrieve an actual copy of an artifact, in this order:
   * * Inlined representation * Local Knowledge Artifact Repository * External Locations
   *
   * @param artifact The Knowledge Artifact Metadata
   * @return a byte-encoded copy of the artifact
   */
  private Answer<byte[]> retrieveBinaryArtifact(KnowledgeArtifact artifact) {
    return Answer.of(extractInlinedArtifact(artifact))
        .or(() -> retrieveBinaryArtifactFromRepository(artifact.getArtifactId()))
        .or(() -> retrieveArtifactFromExternalLocation(artifact));
  }

  /**
   * Determines whether a given Artifact should be preferentially resolved by means of redirecting
   * the client to an external URL, as opposed to retrieving the data from an underlying Artifact
   * Repository
   *
   * @param artifactSurrogate the metadata about the Artifact
   * @return true if the client should be redirected (vs the server retrieving a copy of the
   * Artifact)
   */
  private boolean isRedirectable(KnowledgeArtifact artifactSurrogate) {
    return HTML.sameAs(artifactSurrogate.getRepresentation().getLanguage())
        && Util.isEmpty(artifactSurrogate.getInlinedExpression())
        && artifactSurrogate.getLocator() != null;
  }

  /**
   * Retrieves a binary carrier for a given asset, wrapped in a KnowledgeCarrier
   *
   * @param asset   The Asset for which to retrieve a binary carrier
   * @param carrier The Knowledge Artifact Metadata
   * @return a byte-encoded copy of the artifact
   */
  private Answer<KnowledgeCarrier> retrieveWrappedBinaryArtifact(KnowledgeAsset asset,
      KnowledgeArtifact carrier) {
    return retrieveBinaryArtifact(carrier)
        .map(bytes -> buildKnowledgeCarrier(
            asset.getAssetId().getUuid(), asset.getAssetId().getVersionTag(),
            carrier.getArtifactId().getUuid(), carrier.getArtifactId().getVersionTag(),
            carrier.getRepresentation(),
            coalesce(asset.getName(), carrier.getName()),
            bytes));
  }


  /**
   * Uses a locator (URL) to get a copy of an external artifact TODO: this method assumes the URL is
   * openly available. This method may need to delegate to a helper
   *
   * @param artifact The KnowledgeArtifact metadata, which could include the locator URL
   * @return The bytes streamed from the locator URL
   */
  private Answer<? extends byte[]> retrieveArtifactFromExternalLocation(
      KnowledgeArtifact artifact) {
    return Answer.of(artifact)
        .cast(KnowledgeArtifact.class)
        .filter(cka -> URIUtil.isDereferencingURL(cka.getLocator()))
        .flatOpt(cka -> FileUtil.readBytes(cka.getLocator()));
  }

  /**
   * Extracts a representation inlined in a surrogate
   *
   * @param artifact The Knowledge Artifact Surrogate
   * @return the inlined artifact, if present
   */
  private Optional<byte[]> extractInlinedArtifact(KnowledgeArtifact artifact) {
    return Optional.of(artifact)
        .flatMap(as(KnowledgeArtifact.class))
        // && carrier has the right artifactId
        .filter(c -> !Util.isEmpty(c.getInlinedExpression()))
        .map(KnowledgeArtifact::getInlinedExpression)
        .map(String::getBytes);
  }


  /**
   * Ensures that Asset, Carrier and Surrogate IDs follow the SemVer pattern, rewriting any ID that
   * does not
   * <p>
   * This method might be later changed to throw an exception
   *
   * @param assetSurrogate
   */
  private void ensureSemanticVersionedIdentifiers(KnowledgeAsset assetSurrogate) {
    ResourceIdentifier axId = assetSurrogate.getAssetId();
    if (VersionIdentifier.detectVersionTag(axId.getVersionTag()) != VersionTagType.SEM_VER) {
      logger.warn("Asset ID {}:{} does not follow the SemVer pattern - will be rewritten",
          axId.getTag(), axId.getVersionTag());
      assetSurrogate.getSecondaryId().add(axId);
      assetSurrogate.setAssetId(toAssetId(axId.getUuid(), axId.getVersionTag()));
    }

    assetSurrogate.getCarriers().forEach(carrier -> {
      ResourceIdentifier cId = carrier.getArtifactId();
      if (VersionIdentifier.detectVersionTag(cId.getVersionTag()) != VersionTagType.SEM_VER) {
        logger.warn("Carrier ID {}:{} does not follow the SemVer pattern - will be rewritten",
            cId.getTag(), cId.getVersionTag());
        carrier.getSecondaryId().add(cId);
        carrier.setArtifactId(toArtifactId(cId.getUuid(), cId.getVersionTag()));
      }
    });
    assetSurrogate.getSurrogate().forEach(surrogate -> {
      ResourceIdentifier sId = surrogate.getArtifactId();
      if (VersionIdentifier.detectVersionTag(sId.getVersionTag()) != VersionTagType.SEM_VER) {
        logger.warn("Carrier ID {}:{} does not follow the SemVer pattern - will be rewritten",
            sId.getTag(), sId.getVersionTag());
        surrogate.getSecondaryId().add(sId);
        surrogate.setArtifactId(toArtifactId(sId.getUuid(), sId.getVersionTag()));
      }
    });
  }


  /**
   * Detects whether : * the given Asset has a Canonical Surrogate, AND *  that Canonical Surrogate
   * is not the same as the provided Surrogate *  OR *  that Canonical Surrogate is the same, AND
   * same version, but the existing and given representation differ
   *
   * @param assetIdentifier     the id of the existing Asset
   * @param surrogateIdentifier the Id of the existing Surrogate
   * @param assetSurrogate      a newly provided Surrogate
   * @throws ServerSideException if the new Surrogate conflicts (i.e. replaces without being
   *                             identical) with the old
   */
  private void detectCanonicalSurrogateConflict(
      ResourceIdentifier assetIdentifier,
      ResourceIdentifier surrogateIdentifier,
      KnowledgeAsset assetSurrogate) {
    Optional<ResourceIdentifier> surrId = index.getCanonicalSurrogateForAsset(assetIdentifier);
    if (surrId.isPresent()
        && (!surrId.get().getUuid().equals(surrogateIdentifier.getUuid()))) {
      throw new ServerSideException(Conflict, emptyMap(),
          "Asset already existing with a different Surrogate ID".getBytes());
    }

    if (surrId.isPresent() &&
        surrogateIdentifier.getUuid().equals(surrId.get().getUuid())) {
      Answer<KnowledgeAsset> existingSurrogate = retrieveCanonicalSurrogateVersion(
          surrogateIdentifier);
      if (existingSurrogate.isSuccess()) {
        KnowledgeAsset existing = existingSurrogate.get();
        if (!SurrogateDiffer.isEquivalent(assetSurrogate, existing)) {
          SurrogateDiffer differ = new SurrogateDiffer();
          String msg = differ.diff(assetSurrogate, existing).prettyPrint();
          throw new ServerSideException(Conflict, emptyMap(), msg.getBytes());
        }
      }
    }
  }

  /**
   * Registers and saves a version of a canonical Surrogate for a given Asset version
   * <p>
   * Assigns a random surrogate ID if not present (TODO should reject?)
   * <p>
   * If the Asset already has a Canonical Surrogate, it must be the same Surrogate, or a version
   * thereof.
   *
   * @param assetId        the Id of the Asset
   * @param surrogateId    the Id of the Surrogate
   * @param assetSurrogate the Canonical Surrogate
   */
  private void persistCanonicalKnowledgeAssetVersion(
      ResourceIdentifier assetId,
      ResourceIdentifier surrogateId,
      KnowledgeAsset assetSurrogate) {

    Answer<KnowledgeCarrier> surrogateBinary = encodeCanonicalSurrogate(assetSurrogate);

    if (surrogateBinary.isSuccess()) {
      this.knowledgeArtifactApi.setKnowledgeArtifactVersion(
          repositoryId,
          surrogateId.getUuid(),
          surrogateId.getVersionTag(),
          surrogateBinary.flatOpt(AbstractCarrier::asBinary).get());

      Index.registerAssetByCanonicalSurrogate(
          assetSurrogate,
          surrogateId,
          surrogateBinary.map(KnowledgeCarrier::getRepresentation)
              .map(ModelMIMECoder::encode)
              .orElseThrow(IllegalStateException::new),
          index);
    }
  }

  /**
   * Persists and indexes a Knowledge Carrier (version) for a given Asset (version)
   *
   * @param assetId    the Id of the Asset
   * @param artifactId the Id of the Carrier Artifact
   * @param exemplar   A binary-encoded copy of the Carrier Artifact
   */
  private void persistKnowledgeCarrier(ResourceIdentifier assetId, ResourceIdentifier artifactId,
      byte[] exemplar) {

    this.knowledgeArtifactApi.setKnowledgeArtifactVersion(
        repositoryId,
        artifactId.getUuid(), artifactId.getVersionTag(),
        exemplar);

    this.index.registerArtifactToAsset(assetId, artifactId, null);
  }

  /**
   * Checks that the Canonical Surrogate has an entry for 'self' in the Surrogates section. If not,
   * it will add one Returns the (artifact) Id of the canonical Surrogate
   *
   * @param assetSurrogate the Canonical Surrogate
   * @return the Id of the Surrogate itself, registered within the Surrogate
   */
  private ResourceIdentifier ensureHasCanonicalSurrogateManifestation(
      KnowledgeAsset assetSurrogate) {
    Optional<KnowledgeArtifact> canonicalSurrogate =
        getSurrogateMetadata(assetSurrogate, defaultSurrogateModel, null);

    if (canonicalSurrogate.isPresent()) {
      KnowledgeArtifact meta = canonicalSurrogate.get();
      if (meta.getRepresentation().getFormat() == null) {
        meta.getRepresentation().setFormat(defaultSurrogateFormat);
      }
      return meta.getArtifactId();
    } else {
      ResourceIdentifier rid = SurrogateBuilder.defaultArtifactId(
          identityMapper.getArtifactNamespace(),
          assetSurrogate.getAssetId(),
          defaultSurrogateModel);
      assetSurrogate.withSurrogate(
          new KnowledgeArtifact()
              .withArtifactId(rid)
              .withRepresentation(rep(defaultSurrogateModel, defaultSurrogateFormat,
                  defaultCharset(), Encodings.DEFAULT)));
      return rid;
    }
  }

  /**
   * Extracts the metadata self-descriptor of the canonical surrogate, from the list of Surrogates
   * registered in a canonical surrogate itself
   *
   * @param assetSurrogate The Surrogate to extract the self-referential metadata from
   * @return The id of the Surrogate it'self'
   */
  private Optional<KnowledgeArtifact> getCanonicalSurrogateMetadata(KnowledgeAsset assetSurrogate) {
    return getSurrogateMetadata(
        assetSurrogate, defaultSurrogateModel, defaultSurrogateFormat);
  }

  /**
   * Extracts the metadata self-descriptor of the canonical surrogate, from the list of Surrogates
   * registered in a canonical surrogate itself
   *
   * @param assetSurrogate The Surrogate to extract the self-referential metadata from
   * @param format         The variant of the Surrogate that matches the given format
   * @return The id of the Surrogate it'self'
   */
  private Optional<KnowledgeArtifact> getCanonicalSurrogateMetadata(
      KnowledgeAsset assetSurrogate,
      SerializationFormat format) {
    return getSurrogateMetadata(
        assetSurrogate, defaultSurrogateModel, format != null ? format : defaultSurrogateFormat)
        .or(() -> getSurrogateMetadata(assetSurrogate, defaultSurrogateModel,
            defaultSurrogateFormat));
  }

  /**
   * Updates the ID of the canonical surrogate, from the list of Surrogates registered in a
   * canonical surrogate itself. Should be invoked as a consequence of updating a Surrogate, before
   * persisting it back, to preserve immutablility
   *
   * @param assetSurrogate The Surrogate to extract the Id from
   * @param newSurrogateId The new Surrogate Id
   */
  private ResourceIdentifier setCanonicalSurrogateId(KnowledgeAsset assetSurrogate,
      ResourceIdentifier newSurrogateId) {
    return SurrogateHelper.setSurrogateId(
        assetSurrogate, defaultSurrogateModel, defaultSurrogateFormat, newSurrogateId);
  }

  /**
   * Validates that a Canonical Surrogate declares the same Asset Id for which it was used in a
   * call
   *
   * @param assetSurrogate The Canonical Surrogate, which contains an asset id and version
   * @param assetId        the asset id associated to the surrogate
   * @param versionTag     the version tag associated to the surrogate
   * @return true if the assetId/version are the same as the ones in the Surrogate
   */
  private boolean testIdentifiersConsistency(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    //checks that assetId and versionTag provided in surrogate match those provided as parameters
    return (assetSurrogate.getAssetId().getUuid().equals(assetId)
        && toSemVer(assetSurrogate.getAssetId().getVersionTag()).equals(toSemVer(versionTag)));
  }

  /**
   * Reconciles any missing asset Id and version in a Canonical Surrogate, using the parameters of
   * an asset-related call (e.g. getKnowledgeAsset)
   * <p>
   * TODO this method should probably throw a 403-BAD REQUEST exception
   *
   * @param assetSurrogate
   * @param assetId
   * @param versionTag
   */
  private void setIdAndVersionIfMissing(KnowledgeAsset assetSurrogate, UUID assetId,
      String versionTag) {
    ResourceIdentifier surrogateId = assetSurrogate.getAssetId();
    if (surrogateId == null || surrogateId.getUuid() == null || surrogateId.getTag() == null) {
      //If the entire assetId is missing, set it based on parameters.
      assetSurrogate.setAssetId(toAssetId(assetId, versionTag));
    } else if (surrogateId.getVersionTag() == null) {
      //If the version tag is missing, set it based on parameter
      assetSurrogate.getAssetId()
          .withVersionTag(toSemVer(versionTag))
          .withVersionId(
              getDefaultVersionId(assetSurrogate.getAssetId().getResourceId(),
                  toSemVer(versionTag)));
    }
  }


  /**
   * Aggregates identifiers of different versions of different entities by the same entity. For each
   * entity, returns the identifier of the most recent version of that entity
   *
   * @param versionIdentifiers the identifiers of the different versions of the different entities
   * @return the identifier of the most recent version of each entity
   */
  private <T extends SemanticIdentifier> List<T> aggregateVersions(List<T> versionIdentifiers) {
    return versionIdentifiers.stream()
        .collect(groupingBy(SemanticIdentifier::getUuid))
        .values().stream()
        .map(l -> {
          l.sort(timedSemverComparator());
          return l.get(0);
        }).collect(toList());
  }

  /**
   * Converts the identifier of a Knowledge Asset to a Pointer, including additional information
   * such as the name, type(s) and URL on this server at which thise resource can be provided.
   *
   * @param assetId  the Id of the Asset to be mapped to a Pointer
   * @param hrefType the type of resource (only ASSET and ASSET_VERSION are supported)
   * @param mime     the MIME type of the resource the Pointer resolves to
   * @return a Pointer that includes a URL to this server
   */
  private Pointer toKnowledgeAssetPointer(
      ResourceIdentifier assetId,
      HrefType hrefType,
      String mime) {
    Pointer pointer = assetId.toPointer();

    // TODO: Assess if the information is worthy the cost of the queries
    // or the queries should be optimized
    // e.g. into a single query that returns the Id information with name and type
    index.getAssetName(assetId)
        .ifPresent(pointer::setName);
    index.getAssetTypes(assetId)
        .forEach(ci -> pointer.setType(ci.getReferentId()));

    return pointer
        .withHref(hrefBuilder.getHref(assetId, hrefType))
        .withMimeType(mime);
  }

  /**
   * Converts the identifier of a Knowledge Artifact to a Pointer,
   *
   * @param assetId    the Id of the Asset carried by the Artifact
   * @param artifactId the Id of the Artifact to be mapped to a Pointer
   * @param hrefType   the type of resource
   * @return a Pointer that includes a URL to this server
   */
  private Pointer toKnowledgeArtifactPointer(
      ResourceIdentifier assetId, ResourceIdentifier artifactId, HrefType hrefType) {
    Pointer pointer = artifactId.toPointer();

    return pointer
        .withHref(hrefBuilder.getHref(assetId, artifactId, hrefType));
  }

  /**
   * Returns the latest version of the surrogate for the latest version of the given asset
   *
   * @param assetId the uuid of the asset (series)
   * @return the latest Canonical Surrogate for the latest version of the given asset
   */
  private Answer<KnowledgeAsset> retrieveLatestCanonicalSurrogateForLatestAsset(UUID assetId) {
    Optional<ResourceIdentifier> surrogateId = getLatestAssetVersion(assetId)
        .flatMap(latestAssetId -> index.getCanonicalSurrogateForAsset(latestAssetId));
    return Answer.of(surrogateId)
        .flatMap(this::retrieveLatestCanonicalSurrogate);
  }


  /**
   * Checks whether or not a specific version of a given Knowledge Asset is known to this
   * repository
   *
   * @param assetId the id of the asset for which the canonical surrogate is requested
   * @return true if the Asset is indexed, false otherwise
   */
  public boolean hasKnowledgeAsset(ResourceIdentifier assetId) {
    return index.isKnownAsset(assetId);
  }

  /**
   * Returns the latest version of the surrogate for the latest version of the given asset
   *
   * @param assetId    the uuid of the asset (series)
   * @param versionTag the version tag of the asset
   * @return the latest Canonical Surrogate for the latest version of the given asset
   */
  private Answer<KnowledgeAsset> retrieveLatestCanonicalSurrogateForAssetVersion(UUID assetId,
      String versionTag) {
    Optional<ResourceIdentifier> surrogateId =
        index.resolve(assetId, versionTag)
            .flatMap(rid -> index.getCanonicalSurrogateForAsset(rid));
    return Answer.of(surrogateId)
        .flatMap(this::retrieveLatestCanonicalSurrogate);
  }

  /**
   * Retrieves the latest Surrogate in a Surrogate Series
   *
   * @param surrogateIdentifier the identifier of the surrogate series
   * @return the latest canonical surrogate
   */
  private Answer<KnowledgeAsset> retrieveLatestCanonicalSurrogate(
      ResourceIdentifier surrogateIdentifier) {
    return Answer.of(getLatestSurrogateVersion(surrogateIdentifier.getUuid()))
        .flatMap(this::retrieveCanonicalSurrogateVersion);
  }

  /**
   * Retrieves the identifier of the lastest known version of the given Knowledge Asset (series)
   *
   * @param assetId The uuid of the asset series
   * @return the ResourceIdentifier of the latest version of that asset (series)
   */
  protected Optional<ResourceIdentifier> getLatestAssetVersion(UUID assetId) {
    List<ResourceIdentifier> versions = index.getAssetVersions(assetId);
    versions.sort(timedSemverComparator());
    return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
  }

  /**
   * Retrieves the identifier of the lastest known version of the given Knowledge Asset (series)
   *
   * @param surrogateId The uuid of the surrogate series
   * @return the ResourceIdentifier of the latest version of that surrogate (series)
   */
  protected Optional<ResourceIdentifier> getLatestSurrogateVersion(UUID surrogateId) {
    List<Pointer> versions = index.getSurrogateVersions(surrogateId);
    versions.sort(timedSemverComparator());
    return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
  }

  /**
   * Retrieves the identifier of the lastest known version of a given Knowledge Artifact
   *
   * @param carrierId The uuid of the artifact series
   * @return the ResourceIdentifier of the latest version of that artifact (series)
   */
  protected Optional<ResourceIdentifier> getLatestCarrierVersion(UUID carrierId) {
    List<Pointer> versions = index.getCarrierVersions(carrierId);
    versions.sort(timedSemverComparator());
    return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
  }


  /**
   * Retrieves a specific version of a surrogate, and parses it to its AST form
   *
   * @param surrogateId the ID of the surrogate
   * @return the parsed Canonical KnowledgeAsset
   */
  private Answer<KnowledgeAsset> retrieveCanonicalSurrogateVersion(ResourceIdentifier surrogateId) {
    return retrieveBinaryArtifactFromRepository(surrogateId)
        .flatMap(this::decodeCanonicalSurrogate);

  }

  /**
   * Retrieves a binary artifact from the underlying Knowledge Artifact Repository
   *
   * @param artifactId the ID of the artifact
   * @return the binary encoding of the artifact
   */
  private Answer<byte[]> retrieveBinaryArtifactFromRepository(ResourceIdentifier artifactId) {
    try {
      return knowledgeArtifactApi.getKnowledgeArtifactVersion(
          repositoryId, artifactId.getUuid(), artifactId.getVersionTag());
    } catch (ResourceNotFoundException rnfe) {
      return Answer.notFound();
    }
  }

  /**
   * Lifts a binary Canonical Knowledge Asset Surrogate into its AST/object form
   *
   * @param encodedCanonicalSurrogate the binary encoding of the canonical surrogate
   * @return The KnowledgeAsset surrogate
   * @see SemanticKnowledgeAssetRepository#encodeCanonicalSurrogate(KnowledgeAsset)
   */
  protected Answer<KnowledgeAsset> decodeCanonicalSurrogate(byte[] encodedCanonicalSurrogate) {
    return parser
        .applyLift(
            of(encodedCanonicalSurrogate)
                .withRepresentation(rep(defaultSurrogateModel, defaultSurrogateFormat,
                    defaultCharset(), Encodings.DEFAULT)),
            Abstract_Knowledge_Expression)
        .flatOpt(kc -> kc.as(KnowledgeAsset.class));
  }

  /**
   * Lifts a wrapped Knowledge Asset Surrogate into its AST/object form
   *
   * @param surrogate a KnowledgeCarrier that wraps a Canonical Knowledge Asset Surrogate
   * @return The KnowledgeAsset surrogate
   */
  protected Answer<KnowledgeAsset> liftCanonicalSurrogate(KnowledgeCarrier surrogate) {
    return parser
        .applyLift(surrogate, Abstract_Knowledge_Expression)
        .flatOpt(kc -> kc.as(KnowledgeAsset.class));
  }

  /**
   * Serializes a canonical Knowledge Asset Surrogate into binary form, and returns it wrapped in a
   * KnowledgeCarrier
   *
   * @param assetSurrogate A KnowledgeAsset surrogate
   * @return A KnowledgeCarrier that wraps the binary encoding of that surrogate
   * @see SemanticKnowledgeAssetRepository#decodeCanonicalSurrogate(byte[])
   */
  protected Answer<KnowledgeCarrier> encodeCanonicalSurrogate(KnowledgeAsset assetSurrogate) {
    return encodeCanonicalSurrogate(assetSurrogate, defaultSurrogateFormat);
  }

  protected Answer<KnowledgeCarrier> encodeCanonicalSurrogate(KnowledgeAsset assetSurrogate,
      SerializationFormat format) {
    SerializationFormat actualFormat =
        (format != null && supportedDefaultSurrogateFormats.stream()
            .anyMatch(f -> f.sameAs(format)))
            ? format : defaultSurrogateFormat;
    SyntacticRepresentation rep
        = rep(defaultSurrogateModel, actualFormat, defaultCharset(), Encodings.DEFAULT);
    return parser.applyLower(
        ofAst(assetSurrogate)
            .withRepresentation(rep)
            .withAssetId(assetSurrogate.getAssetId())
            .withArtifactId(getSurrogateId(assetSurrogate, defaultSurrogateModel, actualFormat)
                .orElseGet(SurrogateBuilder::randomArtifactId))
            .withLabel(assetSurrogate.getName()),
        Encoded_Knowledge_Expression,
        encode(rep),
        null
    );
  }

  /**
   * Disruptive method that clears the underlying Knowledge Artifact Repository, as well as the
   * index/knowledge graph - effectively resetting this Asset Repository to an empty state
   */
  private void clear() {
    if (this.knowledgeArtifactApi instanceof ClearableKnowledgeArtifactRepositoryService) {
      ((ClearableKnowledgeArtifactRepositoryService) (this.knowledgeArtifactApi)).clear();
      this.index.reset();
    } else {
      logger.warn("Clear requested, but clearable Artifact Repository instance was not found,.");
    }
  }

}
