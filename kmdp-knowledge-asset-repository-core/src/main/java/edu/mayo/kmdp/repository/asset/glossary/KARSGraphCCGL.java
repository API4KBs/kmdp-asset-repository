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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * NOTE: Applicability is not supported yet
 */
@Named
@KPServer
public class KARSGraphCCGL implements GlossaryLibraryApiInternal {

  protected final KnowledgeAssetRepositoryService kars;

  protected final KnowledgeArtifactApiInternal artifactRepo;
  @Value("${edu.mayo.kmdp.repository.artifact.identifier:default}")
  private String artifactRepositoryId = "default";

  protected final SparqlQueryBinder binder;

  String glossaryQuery;

  @Autowired
  public KARSGraphCCGL(
      KnowledgeAssetRepositoryService kars,
      KnowledgeArtifactApiInternal artifactRepo) {
    this.kars = kars;
    this.binder = new SparqlQueryBinder();
    this.glossaryQuery = readQuery();
    this.artifactRepo = artifactRepo;
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

    Answer<List<PartialEntry>> partialEntries = bind(glossaryId, qAccept)
        .flatMap(q -> kars.queryKnowledgeAssetGraph(q))
        .flatList(Bindings.class, b -> this.toPartialEntry(b, qAccept));

    return partialEntries
        .map(pes ->
            consolidate(pes, processingMethod, publishedOnly, greatestOnly, qAccept));
  }

  protected List<GlossaryEntry> consolidate(
      List<PartialEntry> partialEntries,
      String processingMethod,
      Boolean publishedOnly,
      Boolean greatestOnly,
      String qAccept) {
    var grouped = partialEntries.stream()
        .filter(ge -> !ge.partial.getDef().isEmpty())
        .filter(ge -> publishedOnly == null || !publishedOnly || isPublished(ge.partial))
        .filter(ge -> processingMethod == null || hasMethod(ge.partial, processingMethod))
        .collect(Collectors.groupingBy(ge -> ge.partial.getDefines()));
    if (greatestOnly != null && greatestOnly) {
      grouped.entrySet()
          .forEach(e -> e.setValue(keepAllGreatest(e.getValue())));
    }
    return grouped.values().stream()
        .map(pes -> pes.stream()
            .map(pe -> loadEntry(pe, qAccept))
            .reduce(SurrogateV2ToCcgEntry::merge).orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private GlossaryEntry loadEntry(PartialEntry pe, String qAccept) {
    artifactRepo.getKnowledgeArtifactVersion(
        artifactRepositoryId, pe.artifactId.getUuid(), pe.artifactId.getVersionTag())
        .map(String::new)
        .ifPresent(xpr -> pe.partial.getDef().get(0).getComputableSpec().inlinedExpr(xpr));
    return pe.partial;
  }

  protected boolean hasMethod(GlossaryEntry partial, String method) {
    return partial.getDef().get(0).getProcessingMethod().contains(method);
  }

  protected boolean isPublished(GlossaryEntry partial) {
    return Objects.equals(Published.getTag(),
        partial.getDef().get(0).getComputableSpec().getPublicationStatus());
  }


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


  protected List<PartialEntry> keepGreatest(List<PartialEntry> value) {
    if (value.isEmpty()) {
      return Collections.emptyList();
    }
    value.sort(getVersionComparator().reversed());
    return List.of(value.get(0));
  }

  protected Comparator<PartialEntry> getVersionComparator() {
    return Comparator.comparing(pe -> Version.valueOf(pe.assetId.getVersionTag()));
  }


  protected Answer<PartialEntry> toPartialEntry(Bindings<String, String> b, String qAccept) {
    var assetId = newVersionId(URI.create(b.get("asset")));
    var type = b.get("assetType");
    var name = b.get("name");
    var defined = b.get("concept");
    var method = b.get("method");
    var shape = b.get("shape");
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
                .mimeCode(mime))
            .effectuates(shape));
    var pe = new PartialEntry(assetId.asKey(), artifactId.asKey(), partial);
    return Answer.of(pe);
  }

  protected String inferPublicationStatus(String versionTag) {
    if (versionTag.contains(IdentifierConstants.SNAPSHOT)) {
      return Draft.getTag();
    } else if (versionTag.indexOf('-') >= 0) {
      return Final_Draft.getTag();
    } else {
      return Published.getTag();
    }
  }


  protected Answer<KnowledgeCarrier> bind(String glossaryId, String qAccept) {
    if (glossaryQuery == null) {
      return Answer.unsupported();
    }
    if (qAccept == null) {
      qAccept = codedRep(CQL_Essentials, TXT, Charset.defaultCharset());
    }
    var params = new Bindings<String, String>();
    params.put("mime", qAccept);
    params.put("coll", newTerm(glossaryId).getResourceId().toString());
    return this.binder.bind(JenaQuery.ofSparqlQuery(glossaryQuery), params);
  }


  protected String readQuery() {
    return FileUtil
        .read(KARSGraphCCGL.class.getResourceAsStream("/glossary.sparql"))
        .orElseThrow(() -> new IllegalStateException("Unable to load resource /glossary.sparql"));
  }


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

  protected static class PartialEntry {

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
