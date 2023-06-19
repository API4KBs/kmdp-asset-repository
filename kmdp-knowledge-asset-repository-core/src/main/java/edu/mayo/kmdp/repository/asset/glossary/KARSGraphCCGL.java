package edu.mayo.kmdp.repository.asset.glossary;

import static edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2ToCcgEntry.mintGlossaryEntryId;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
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
import javax.inject.Named;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeprocessingtechnique.KnowledgeProcessingTechnique;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link GlossaryLibraryApiInternal} backed by an Asset Repository's RDF
 * Knowledge Graph.
 * <p>
 * Uses 'memberOf (Collection)' to identify Glossaries, and builds entries from Assets that define
 * Concepts, and relationships thereof.
 */
@Named
@KPServer
public class KARSGraphCCGL implements GlossaryLibraryApiInternal {

  /**
   * The backing Asset Repository, holding the Knowledge Graph
   */
  protected final KnowledgeAssetRepositoryService kars;

  /**
   * The backing Artifact Repository, which may store some of the Operational Definitions
   */
  protected final KnowledgeArtifactApiInternal artifactRepo;

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
   */
  @Autowired
  public KARSGraphCCGL(
      KnowledgeAssetRepositoryService kars,
      KnowledgeArtifactRepositoryService artifactRepo) {
    this.kars = kars;
    this.glossaryQuery = readQuery();
    this.artifactRepo = artifactRepo;

    this.artifactRepoId = artifactRepo.listKnowledgeArtifactRepositories()
        .flatOpt(repos -> repos.stream()
            .filter(KnowledgeArtifactRepository::isDefaultRepository)
            .map(KnowledgeArtifactRepository::getName)
            .findFirst())
        .orElse("default");
  }


  @Override
  public Answer<Glossary> getGlossary(String glossaryId) {
    return listGlossaries().flatOpt(
        gls -> gls.stream()
            .filter(g -> g.getGlossaryId().equals(glossaryId))
            .findFirst());
  }

  @Override
  public Answer<List<Glossary>> listGlossaries() {
    var query = JenaQuery.ofSparqlQuery(
        "SELECT DISTINCT ?coll WHERE { "
            + "  _:a <https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/isMemberOf> ?coll. }"
    );
    return kars.queryKnowledgeAssetGraph(query)
        .map(l -> l.stream()
            .map(b -> (String) b.get("coll"))
            .map(n -> n.replace(Registry.BASE_UUID_URN, ""))
            .map(n -> new Glossary().glossaryId(n))
            .collect(Collectors.toList()));
  }

  @Override
  public Answer<List<GlossaryEntry>> listGlossaryEntries(
      String glossaryId,
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
  public Answer<GlossaryEntry> getGlossaryEntry(
      String glossaryId,
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
   * @param glossaryId       the Glossary to construct, mapped to an Asset collection
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
      String glossaryId,
      UUID definedConceptId, UUID scopingConceptId, String processingMethod,
      Boolean publishedOnly, Boolean greatestOnly,
      String qAccept) {
    Answer<List<PartialEntry>> partialEntries =
        bind(glossaryId, qAccept)
            .flatMap(q -> kars.queryKnowledgeAssetGraph(q))
            .map(bl -> applyFilters(bl, definedConceptId, scopingConceptId, processingMethod))
            .flatList(Bindings.class, b -> this.toPartialEntry(b, qAccept));

    return partialEntries
        .map(pes ->
            consolidate(pes, publishedOnly, greatestOnly));
  }

  /**
   * Post-processes the Graph Query result set, applying the optional semantic filters
   * <p>
   * TODO: Consider using a query template instead
   *
   * @param bindingsList     the Graph Query Results
   * @param definedConceptId if present, builds the single Glossary Entry that defines this concept
   *                         (note: the entry may still include multiple definitions)
   * @param scopingConceptId if present, filters the Glossary to entries whose applicability is
   *                         delimited by this concept
   * @param processingMethod if present, filters the Operational Definitions in the Glossary to the
   *                         ones that use the given method
   * @return the result set, filtered
   */
  private List<Bindings> applyFilters(
      List<Bindings> bindingsList,
      UUID definedConceptId,
      UUID scopingConceptId,
      String processingMethod) {
    if (definedConceptId == null && scopingConceptId == null && processingMethod == null) {
      return bindingsList;
    }

    return bindingsList.stream()
        .filter(b -> keep(b, definedConceptId, scopingConceptId, processingMethod))
        .collect(Collectors.toList());
  }

  /**
   * Post-processes a single Result set entry, as a prototype of a partial Glossary entry, applying
   * the optional semantic filters
   * <p>
   * TODO: Consider using a query template instead
   *
   * @param bindings         the Graph Query Results
   * @param definedConceptId if present, builds the single Glossary Entry that defines this concept
   *                         (note: the entry may still include multiple definitions)
   * @param scopingConceptId if present, filters the Glossary to entries whose applicability is
   *                         delimited by this concept
   * @param processingMethod if present, filters the Operational Definitions in the Glossary to the
   *                         ones that use the given method
   * @return the result set, filtered
   */
  private boolean keep(
      Bindings<String, String> bindings,
      UUID definedConceptId,
      UUID scopingConceptId,
      String processingMethod) {
    if (definedConceptId != null &&
        !bindings.getOrDefault("concept", "")
            .contains(definedConceptId.toString())) {
      return false;
    }
    if (scopingConceptId != null &&
        !bindings.getOrDefault("applicabilityScope", "")
            .contains(scopingConceptId.toString())) {
      return false;
    }
    return processingMethod == null ||
        bindings.getOrDefault("method", "")
            .contains(processingMethod);
  }


  /**
   * Constructs a Partial Glossary Entry from a set of SPARQL variable bindings
   *
   * @param b       the bindings
   * @param qAccept the form of the operational definition
   * @return the mapped {@link PartialEntry}
   */
  protected Answer<PartialEntry> toPartialEntry(
      Bindings<String, String> b,
      String qAccept) {
    var assetId = newVersionId(URI.create(b.get("asset")));
    var type = b.get("assetType");
    var name = b.get("name");
    var defined = b.get("concept");
    var method = b.get("method");
    var shape = b.get("shape");
    var inlined = b.get("inlined");
    var artifactId = newVersionId(URI.create(b.get("artifact")));
    var mime = Optional.ofNullable(b.get("mime")).orElse(qAccept);

    var partial = new GlossaryEntry()
        .id(mintGlossaryEntryId(assetId, defined))
        .defines(defined)
        .addDefItem(new OperationalDefinition()
            .id(assetId.getVersionId().toString())
            .name(name)
            .defines(defined)
            .processingMethod(getTechniques(method))
            .computableSpec(new KnowledgeResourceRef()
                .assetId(assetId.getVersionId().toString())
                .href(assetId.getVersionId().toString())
                .artifactId(artifactId.getVersionId().toString())
                .publicationStatus(inferPublicationStatus(assetId.getVersionTag()))
                .addAssetTypeItem(type)
                .mimeCode(mime)
                .inlinedExpr(inlined))
            .effectuates(shape));
    var pe = new PartialEntry(assetId.asKey(), artifactId.asKey(), partial);
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
    if (!pe.partial.getDef().isEmpty() &&
        pe.partial.getDef().get(0).getComputableSpec().getInlinedExpr() != null) {
      return pe;
    }
    if (pe.partial.getDef().get(0).getComputableSpec().getInlinedExpr() == null) {
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
    if (Natural_Technique.getConceptId().toString().equals(method)
        || Computational_Technique.getConceptId().toString().equals(method)) {
      return List.of(method);
    } else {
      return List.of(method, Computational_Technique.getConceptId().toString());
    }
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
      String qAccept) {
    if (glossaryQuery == null) {
      return Answer.unsupported();
    }
    if (qAccept == null) {
      qAccept = codedRep(CQL_Essentials, TXT, Charset.defaultCharset());
    }
    var params = new Bindings<String, String>();
    params.put("mime", qAccept);
    params.put("coll", newTerm(glossaryId).getResourceId().toString());
    return new SparqlQueryBinder().bind(JenaQuery.ofSparqlQuery(glossaryQuery), params);
  }

  /**
   * Reads the Graph Query from the SPARQL resource
   *
   * @return the content of the source file
   */
  protected String readQuery() {
    return FileUtil
        .read(KARSGraphCCGL.class.getResourceAsStream("/glossary.sparql"))
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
