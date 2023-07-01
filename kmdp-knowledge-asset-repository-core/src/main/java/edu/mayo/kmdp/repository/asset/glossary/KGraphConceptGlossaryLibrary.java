package edu.mayo.kmdp.repository.asset.glossary;

import static edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry.mintGlossaryEntryId;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newIdAsUUID;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newName;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.Term.newTerm;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Computational_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries.Natural_Technique;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CQL_Essentials;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.api.ccgl.v3.server.GlossaryLibraryApiInternal;
import edu.mayo.kmdp.api.terminology.v4.server.TermsApiInternal;
import edu.mayo.kmdp.ccg.model.Glossary;
import edu.mayo.kmdp.ccg.model.GlossaryEntry;
import edu.mayo.kmdp.ccg.model.KnowledgeResourceRef;
import edu.mayo.kmdp.ccg.model.OperationalDefinition;
import edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery;
import edu.mayo.kmdp.knowledgebase.binders.sparql.v1_1.SparqlQueryBinder;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactRepositoryService;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryService;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechnique;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechniqueSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link GlossaryLibraryApiInternal} backed by an Asset Repository's RDF
 * Knowledge Graph.
 * <p>
 * Uses 'memberOf (Collection)' to identify Glossaries, and builds entries from Assets that define
 * Concepts, and relationships thereof.
 */
public class KGraphConceptGlossaryLibrary implements GlossaryLibraryApiInternal {

  Logger logger = LoggerFactory.getLogger(KGraphConceptGlossaryLibrary.class);

  /**
   * The backing Asset Repository, holding the Knowledge Graph
   */
  protected final KnowledgeAssetCatalogApiInternal cat;

  /**
   * The backing Artifact Repository, which may store some of the Operational Definitions
   */
  protected final KnowledgeArtifactApiInternal artifactRepo;

  protected final TermsApiInternal terms;

  /**
   * The (default) Artifact repository Id where Operational Definitions are looked up
   */
  protected String artifactRepoId;

  /**
   * The SPARQL query used to build a Glossary
   */
  protected String glossaryQuery;

  /**
   * Constructor
   *
   * @param kars         the Asset Repository
   * @param artifactRepo the ArtifactRepository
   * @param terms        the Terminology Provider
   */
  public KGraphConceptGlossaryLibrary(
      KnowledgeAssetRepositoryService kars,
      KnowledgeArtifactRepositoryService artifactRepo,
      TermsApiInternal terms) {
    this.cat = kars;
    this.glossaryQuery = readQuery();
    this.artifactRepo = artifactRepo;
    this.terms = terms;

    this.artifactRepoId = artifactRepo.listKnowledgeArtifactRepositories()
        .flatOpt(repos -> repos.stream()
            .filter(KnowledgeArtifactRepository::isDefaultRepository)
            .map(KnowledgeArtifactRepository::getName)
            .findFirst())
        .orElse("default");
  }

  public KGraphConceptGlossaryLibrary(
      KnowledgeAssetCatalogApiInternal cat,
      TermsApiInternal terms) {
    this.cat = cat;
    this.artifactRepo = null;
    this.artifactRepoId = null;

    this.glossaryQuery = readQuery();
    this.terms = terms;
  }


  @Override
  public Answer<List<Glossary>> getGlossary(List<String> glossaryIds) {
    return listGlossaries().map(
        gls -> gls.stream()
            .filter(g -> glossaryIds.contains(g.getGlossaryId()))
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<List<Glossary>> listGlossaries() {
    var query = JenaQuery.ofSparqlQuery(
        "SELECT DISTINCT ?coll WHERE { "
            + "  _:a <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/isMemberOf> ?coll. }"
    );
    return cat.queryKnowledgeAssetGraph(query)
        .map(l -> l.stream()
            .map(b -> (String) b.get("coll"))
            .map(n -> n.replace(Registry.URN, ""))
            .map(n -> new Glossary().glossaryId(n))
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<List<GlossaryEntry>> listGlossaryEntries(
      List<String> glossaryId,
      UUID scopingConceptId,
      String processingMethod,
      Boolean publishedOnly,
      Boolean greatestOnly,
      String qAccept) {
    return buildEntries(
        glossaryId,
        null, scopingConceptId, processingMethod,
        publishedOnly, greatestOnly,
        qAccept);
  }

  @Override
  public Answer<List<GlossaryEntry>> pickGlossaryEntries(
      List<String> glossaryId,
      List<UUID> definedConceptIds,
      UUID scopingConceptId,
      String processingMethod,
      Boolean publishedOnly,
      Boolean greatestOnly,
      String qAccept) {
    return listGlossaryEntries(
        glossaryId,
        scopingConceptId, processingMethod,
        publishedOnly, greatestOnly,
        qAccept)
        .map(l -> l.stream()
            .filter(ge -> definedConceptIds.contains(newIdAsUUID(ge.getDefines())))
            .collect(Collectors.toList()));
  }


  @Override
  public Answer<GlossaryEntry> getGlossaryEntry(
      List<String> glossaryId,
      UUID definedConceptId,
      UUID scopingConceptId,
      String processingMethod,
      Boolean publishedOnly,
      Boolean greatestOnly,
      String qAccept) {
    return buildEntries(
        glossaryId,
        definedConceptId, scopingConceptId, processingMethod,
        publishedOnly, greatestOnly,
        qAccept)
        .flatMap(
            l -> l.isEmpty()
                ? Answer.notFound()
                : Answer.of(l.get(0))
        );
  }

  /* ------------------------------------------------------------------------------------------ */


  /**
   * Constructs one or more Glossary Entries using the Asset Repository Knowledge Graph.
   * <p>
   * Binds the client-provided filter criteria to the Graph Query, which is used to query the RDF
   * Graph. The result set is mapped to a set of partial Glossary Entries, which are further
   * filtered and combined into the final entries
   *
   * @param glossaries       the Glossary to construct, mapped to an Asset collection
   * @param definedConceptId if present, builds the single Glossary Entry that defines this concept
   *                         (note: the entry may still include multiple definitions)
   * @param scopingConceptId if present, filters the Glossary to entries whose applicability is
   *                         delimited by this concept
   * @param processingMethod if present, filters the Operational Definitions in the Glossary to the
   *                         ones that use the given method
   * @param publishedOnly    if true, filters the Operational Definitions in the Glossary to only
   *                         include the ones that are 'published'
   * @param greatestOnly     if true, when multiple versions of the same Operational Definitions
   *                         exist, filters the Glossary to only include the one with the greatest
   *                         version tag, sorted according to SemVer (or CalVer) criteria
   * @param qAccept          content negotiation: if present, retrieves and inlines the Operational
   *                         Definitions in the given form
   * @return the {@link GlossaryEntry} for the Glossary with the given glossaryId
   */
  private Answer<List<GlossaryEntry>> buildEntries(
      List<String> glossaries,
      UUID definedConceptId, UUID scopingConceptId, String processingMethod,
      Boolean publishedOnly, Boolean greatestOnly,
      String qAccept) {
    Answer<List<PartialEntry>> partialEntries =
        glossaries.stream()
            .map(glossaryId -> getPartialEntries(glossaryId, definedConceptId, scopingConceptId,
                processingMethod, qAccept))
            .filter(Answer::isSuccess)
            .reduce((a1, a2) -> Answer.merge(a1, a2, (l1, l2) ->
                Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())))
            .orElse(Answer.of(Collections.emptyList()));

    return partialEntries
        .map(pes ->
            consolidate(pes, publishedOnly, greatestOnly));
  }

  private Answer<List<PartialEntry>> getPartialEntries(String glossaryId,
      UUID definedConceptId, UUID scopingConceptId, String processingMethod,
      String qAccept) {

    Term def = Answer.ofNullable(definedConceptId)
        .flatMap(c -> terms.lookupTerm(c.toString()))
        .orElse(null);
    if (definedConceptId != null && def == null) {
      return Answer.notFound();
    }

    Term app = Answer.ofNullable(scopingConceptId)
        .flatMap(c -> terms.lookupTerm(c.toString()))
        .orElse(null);
    Term met = Optional.ofNullable(processingMethod)
        .flatMap(KnowledgeProcessingTechniqueSeries::resolveTag)
        .orElse(null);

    return (Answer<List<PartialEntry>>)
        bind(glossaryId, def, app, met, qAccept)
            .flatMap(cat::queryKnowledgeAssetGraph)
            .flatList(Bindings.class, b ->
                this.toPartialEntry(glossaryId, b, def, app, met, qAccept));
  }

  /**
   * Constructs a Partial Glossary Entry from a set of SPARQL variable bindings.
   * <p/>
   * Ensures that client-provided filter values are reasserted into the result
   *
   * @param b       the bindings
   * @param def     the defined Concept, as a filter
   * @param app     the appplicability scope, as a filter
   * @param met     the processing method, as a filter
   * @param qAccept the form of the operational definition
   * @return the mapped {@link PartialEntry}
   */
  protected Answer<PartialEntry> toPartialEntry(
      String glossaryId,
      Bindings<String, String> b,
      Term def, Term app, Term met,
      String qAccept) {
    var assetId = newVersionId(URI.create(b.get("asset")));
    var glossary = b.getOrDefault("coll", glossaryId);
    var type = b.get("assetType");
    var name = b.get("name");
    var defined = b.getOrDefault("concept",
        def != null ? def.getConceptId().toString() : null);
    var scope = b.getOrDefault("applicabilityScope",
        app != null ? app.getConceptId().toString() : null);
    var method = b.getOrDefault("method",
        met != null ? met.getConceptId().toString() : null);
    var shape = b.get("shape");
    var inlined = b.get("inlined");
    var artifactId = Optional.ofNullable(b.get("artifact"))
        .map(a -> newVersionId(URI.create(a)));
    var mime = b.getOrDefault("mime", qAccept);

    var od = new OperationalDefinition()
        .id(assetId.getVersionId().toString())
        .name(name)
        .applicabilityScope(scope != null ? List.of(scope) : null)
        .declaringGlossaries(List.of(glossary))
        .defines(newTerm(URI.create(defined)).getUuid())
        .processingMethod(getTechniques(method))
        .effectuates(shape);

    artifactId.ifPresent(aid ->
        od.computableSpec(new KnowledgeResourceRef()
            .assetId(assetId.getVersionId().toString())
            .href(assetId.getVersionId().toString())
            .artifactId(aid.toString())
            .publicationStatus(inferPublicationStatus(assetId.getVersionTag()))
            .assetType(getAssetType(type))
            .mimeCode(mime)
            .inlinedExpr(inlined)));

    var partial = new GlossaryEntry()
        .id(mintGlossaryEntryId(assetId, defined))
        .defines(defined)
        .addDefItem(od);

    var pe = new PartialEntry(
        assetId.asKey(),
        artifactId.map(SemanticIdentifier::asKey)
            .orElse(null), partial);
    return Answer.of(pe);
  }


  /**
   * Combines the partial Entries that define the same concept into a single {@link GlossaryEntry}.
   * <p>
   * Partial entries may derive from different assets, or different versions of the same asset
   *
   * @param partialEntries the List of partial entries
   * @param publishedOnly  if true, filters the Operational Definitions in the Glossary to only
   *                       include the ones that are 'published'
   * @param greatestOnly   if true, when multiple versions of the same Operational Definitions
   *                       exist, filters the Glossary to only include the one with the greatest
   *                       version tag, sorted according to SemVer (or CalVer) criteria
   * @return a {@link GlossaryEntry} that consolidates the given partial entries, possibly filtered
   */
  protected List<GlossaryEntry> consolidate(
      List<PartialEntry> partialEntries,
      Boolean publishedOnly,
      Boolean greatestOnly) {
    var grouped = partialEntries.stream()
        .filter(ge -> !ge.partial.getDef().isEmpty())
        .filter(ge -> publishedOnly == null || !publishedOnly || isPublished(ge))
        .collect(Collectors.groupingBy(ge -> ge.partial.getDefines()));
    if (greatestOnly != null && greatestOnly) {
      grouped.entrySet()
          .forEach(e -> e.setValue(keepAllGreatest(e.getValue())));
    }
    return grouped.values().stream()
        .map(pes -> pes.stream()
            .map(this::ensureInlined)
            .map(pe -> pe.partial)
            .reduce(SurrogateV2ToCcgEntry::merge).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /* ------------------------------------------------------------------------------------------ */


  /**
   * Ensures that a partial Glossary Entry containes an inlined representation of its Operational
   * Definition. If not, tries to load it from the Artifact Repository
   *
   * @param pe the {@link PartialEntry}
   * @return the partial GlossaryEntry, with an inlined expression if not already present
   */
  private PartialEntry ensureInlined(PartialEntry pe) {
    if (pe.partial.getDef().isEmpty()) {
      // no defs found
      return pe;
    }
    if (pe.partial.getDef().get(0).getComputableSpec() == null) {
      // no computable defs found
      return pe;
    }
    if (!pe.partial.getDef().isEmpty() &&
        pe.partial.getDef().get(0).getComputableSpec().getInlinedExpr() != null) {
      // already inlined
      return pe;
    }

    if (artifactRepo != null &&
        pe.partial.getDef().get(0).getComputableSpec().getInlinedExpr() == null) {
      artifactRepo.getKnowledgeArtifactVersion(
              artifactRepoId, pe.artifactId.getUuid(), pe.artifactId.getVersionTag())
          .map(String::new)
          .ifPresent(xpr -> pe.partial.getDef().get(0).getComputableSpec().inlinedExpr(xpr));
    }
    return pe;
  }

  /**
   * Filters a collection of partial entries, so that only the greatest version of each asset series
   * is retained.
   * <p>
   * Gropus by asset UUID, sorts each group by version tag, and retains the greatest in each group
   *
   * @param partialsByConcept all the partial entries that define the same concept
   * @return the filtered list
   */
  protected List<PartialEntry> keepAllGreatest(List<PartialEntry> partialsByConcept) {
    if (partialsByConcept.isEmpty()) {
      return Collections.emptyList();
    }
    var byAsset = partialsByConcept.stream()
        .collect(Collectors.groupingBy(ge -> ge.assetId.getUuid()));
    byAsset.entrySet()
        .forEach(e -> e.setValue(keepGreatest(e.getValue())));
    return byAsset.values().stream()
        .map(pes -> pes.stream().findFirst())
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());
  }

  /**
   * Filters a collection of partial entries, consisting of multiple versions of the same asset, so
   * that only the greatest version is retained
   *
   * @param partialEntrySeries the list of partial entries
   * @return a singleton list with the greatest version
   */
  protected List<PartialEntry> keepGreatest(List<PartialEntry> partialEntrySeries) {
    if (partialEntrySeries.isEmpty()) {
      return Collections.emptyList();
    }
    partialEntrySeries.sort(PartialEntry.comparator.reversed());
    return Collections.singletonList(partialEntrySeries.get(0));
  }


  /**
   * Infers the publication status from the version tag, assumed to follow a SemVer pattern.
   * <p>
   * SNAPSHOT denotes Draft, RC tags denote Final_Draft, while a stable version denotes Published
   * <p>
   * This is necessary because the Knowledge Graph does not support publication statuses completely
   *
   * @param versionTag the version tag
   * @return the publication status, inferred
   */
  protected String inferPublicationStatus(String versionTag) {
    if (versionTag.contains(IdentifierConstants.SNAPSHOT)) {
      return Draft.getTag();
    } else if (versionTag.indexOf('-') >= 0) {
      return Final_Draft.getTag();
    } else {
      return Published.getTag();
    }
  }


  /**
   * Checks the publication status of a (partial) Glossary Entry
   *
   * @param partial the entry to check
   * @return true if Published
   */
  protected boolean isPublished(PartialEntry partial) {
    return Objects.equals(Published.getTag(),
        partial.partial.getDef().get(0).getComputableSpec().getPublicationStatus());
  }


  /**
   * Reconciles the List of {@link KnowledgeProcessingTechnique}, for compatibility with legacy
   * clients. Only the primary method is pulled from the graph, but clients may expect 'inferred'
   * ones as well, including 'Computational Technique' which is used for Operational Definitions
   * that are formalized for machine execution.
   * <p>
   * This method is likely to be revisited, as Techniques may be refactored
   *
   * @param method the concept Id of a {@link KnowledgeProcessingTechnique}
   * @return the list of {@link KnowledgeProcessingTechnique}
   */
  @Deprecated
  protected List<String> getTechniques(String method) {
    if (Util.isEmpty(method)) {
      return List.of();
    }
    var methodType = KnowledgeProcessingTechniqueSeries.resolveId(method)
        .orElse(Natural_Technique);
    if (Computational_Technique.sameAs(methodType) || Natural_Technique.sameAs(methodType)) {
      return List.of(methodType.getTag());
    } else {
      return List.of(methodType.getTag(), Computational_Technique.getTag());
    }
  }

  protected List<String> getAssetType(String type) {
    if (Util.isEmpty(type)) {
      return List.of();
    }
    var assetType = KnowledgeAssetTypeSeries.resolveRef(type)
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveRef(type));
    return assetType
        .map(knowledgeAssetType -> List.of(knowledgeAssetType.getTag()))
        .orElseGet(List::of);
  }

  /* ------------------------------------------------------------------------------------------ */

  /**
   * Prepares the SPARQL query for the KARS Knowledge Graph
   *
   * @param glossaryId the Id of the Glossary to construct
   * @param qAccept    the form of the Operational Definitions
   * @return the Query, variables bound and wrapped in a {@link KnowledgeCarrier}
   */
  protected Answer<KnowledgeCarrier> bind(
      String glossaryId,
      Term definedConcept, Term scopingConcept, Term processingMethod,
      String qAccept) {
    if (glossaryQuery == null) {
      return Answer.unsupported();
    }
    var params = new Bindings<String, String>();
    params.put("coll", newName(glossaryId).getResourceId().toString());

    if (qAccept == null) {
      qAccept = codedRep(CQL_Essentials, TXT, Charset.defaultCharset());
    }
    params.put("mime", qAccept);

    if (definedConcept != null) {
      params.put("concept", definedConcept.getConceptId().toString());
    }
    if (scopingConcept != null) {
      params.put("applicabilityScope", scopingConcept.getConceptId().toString());
    }
    if (processingMethod != null) {
      params.put("method", processingMethod.getConceptId().toString());
    }
    var query = new SparqlQueryBinder()
        .bind(JenaQuery.ofSparqlQuery(glossaryQuery), params);

    if (logger.isDebugEnabled()) {
      logger.debug("Running GL query \n {}",
          query.flatOpt(AbstractCarrier::asString).orElse("ERROR"));
    }
    return query;
  }

  /**
   * Reads the Graph Query from the SPARQL resource
   *
   * @return the content of the source file
   */
  protected String readQuery() {
    return FileUtil
        .read(KGraphConceptGlossaryLibrary.class.getResourceAsStream("/glossary.sparql"))
        .orElseThrow(() -> new IllegalStateException("Unable to load resource /glossary.sparql"));
  }

  /* ------------------------------------------------------------------------------------------ */

  /**
   * Glossary Entry component, derived from a specific version of a specific asset that defines a
   * given concept. In order to build full Entries, partial Entries are grouped by asset and
   * concept, then consolidated
   */
  protected static class PartialEntry {

    static Comparator<PartialEntry> comparator;

    static {
      Comparator<PartialEntry> c = Comparator.comparing(pe ->
          Version.valueOf(pe.assetId.getVersionTag()));
      comparator = c.reversed();
    }

    final KeyIdentifier assetId;
    final KeyIdentifier artifactId;
    final GlossaryEntry partial;

    public PartialEntry(KeyIdentifier assetKey, KeyIdentifier artifactKey, GlossaryEntry partial) {
      this.assetId = assetKey;
      this.artifactId = artifactKey;
      this.partial = partial;
    }
  }

}
