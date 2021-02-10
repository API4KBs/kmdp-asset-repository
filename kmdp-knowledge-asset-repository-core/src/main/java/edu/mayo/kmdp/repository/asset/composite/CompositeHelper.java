package edu.mayo.kmdp.repository.asset.composite;

import static edu.mayo.kmdp.id.adapter.CopyableHashMap.toBinds;
import static edu.mayo.kmdp.kbase.query.sparql.v1_1.JenaQuery.bind;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static java.nio.charset.Charset.defaultCharset;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structural_Component;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structuring_Component;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.language.parsers.sparql.SparqlLifter;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Link;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class CompositeHelper {

  private final String structQuery;

  private final String anonStructQuery;

  private final String componentsQuery;

  private final String rootQuery;

  private final _applyLift sparqlLifter;

  private final _applyLower rdfLowerer;

  public static final String ROOT_ID = "?root";
  public static final String CLOSURE_REL = "?closureRel";

  public CompositeHelper() {
    this.anonStructQuery =
        FileUtil
            .read(SemanticKnowledgeAssetRepository.class.getResourceAsStream("/anonStruct.sparql"))
            .orElseThrow(() -> new IllegalStateException("Unable to load anonStruct.sparql"));
    this.structQuery =
        FileUtil.read(SemanticKnowledgeAssetRepository.class.getResourceAsStream("/struct.sparql"))
            .orElseThrow(() -> new IllegalStateException("Unable to load struct.sparql"));
    this.componentsQuery =
        FileUtil
            .read(SemanticKnowledgeAssetRepository.class.getResourceAsStream("/components.sparql"))
            .orElseThrow(() -> new IllegalStateException("Unable to load components.sparql"));
   this.rootQuery =
        FileUtil
            .read(SemanticKnowledgeAssetRepository.class.getResourceAsStream("/root.sparql"))
            .orElseThrow(() -> new IllegalStateException("Unable to load components.sparql"));

    this.sparqlLifter = new SparqlLifter();
    this.rdfLowerer = new JenaRdfParser();
  }

  public Answer<KnowledgeCarrier> getAnonStructQuery(ResourceIdentifier rootId) {
    return prepareQuery(anonStructQuery, rootId, Depends_On);
  }

  public Answer<KnowledgeCarrier> getStructQuery(ResourceIdentifier rootId) {
    return prepareQuery(structQuery, rootId, Has_Structural_Component);
  }

  public Answer<KnowledgeCarrier> getComponentsQuery(
      ResourceIdentifier rootId, Term closureRel) {
    return prepareQuery(componentsQuery, rootId, closureRel);
  }

  public Answer<KnowledgeCarrier> getRootQuery(ResourceIdentifier compositeId) {
    return prepareQuery(rootQuery, compositeId, Depends_On);
  }

  private Answer<KnowledgeCarrier> prepareQuery(
      String srcQuery, ResourceIdentifier rootId, Term closureRel) {
    return sparqlLifter.applyLift(
        of(srcQuery)
            .withRepresentation(rep(SPARQL_1_1, TXT, defaultCharset())),
        Concrete_Knowledge_Expression, null, null)
        .flatMap(q -> bind(q,
            toBinds(
                ROOT_ID, rootId.getVersionId(),
                CLOSURE_REL, closureRel.getReferentId().toString())));
  }

  public Optional<ResourceIdentifier> getStructId(List<Bindings> bindings) {
    return bindings.stream()
        .filter(b -> Has_Structuring_Component.getReferentId().toString().equals(b.get("p")))
        .map(b -> b.get("o").toString())
        .map(URI::create)
        .findFirst()
        .map(SemanticIdentifier::newVersionId);
  }

  public Optional<ResourceIdentifier> getStructId(KnowledgeAsset compositeSurr) {
    return compositeSurr.getLinks().stream()
        .filter(link -> Has_Structuring_Component.sameTermAs(link.getRel()))
        .map(Link::getHref)
        .flatMap(StreamUtil.filterAs(ResourceIdentifier.class))
        .findFirst();
  }

  public Optional<ResourceIdentifier> getRootId(List<Bindings> bindings) {
    if (bindings.size() != 1) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        newVersionId(
            URI.create(bindings.get(0).get("comp").toString())));
  }

  public KnowledgeCarrier toStructGraph(ResourceIdentifier structId, List<Bindings> bindings) {
    Model model = ModelFactory.createDefaultModel();
    bindings.forEach(b -> model.add(
        objA(b.get("s").toString(), b.get("p").toString(), b.get("o").toString())));
    return ofAst(model)
        .withAssetId(structId)
        .withArtifactId(defaultArtifactId(BASE_UUID_URN_URI, structId, OWL_2))
        .withRepresentation(rep(OWL_2));
  }

  public Answer<KnowledgeCarrier> toEncodedStructGraph(List<Bindings> bindings) {
    ResourceIdentifier structId = bindings.stream()
        .map(b -> b.get("s").toString())
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .reduce(SemanticIdentifier::hashIdentifiers)
        .orElseThrow();
    return toEncodedStructGraph(structId, bindings);
  }

  public Answer<KnowledgeCarrier> toEncodedStructGraph(ResourceIdentifier structId, List<Bindings> bindings) {
    return rdfLowerer.applyLower(
        toStructGraph(structId, bindings),
        Encoded_Knowledge_Expression,
        codedRep(OWL_2, Turtle, TXT, defaultCharset(), Encodings.DEFAULT),
        null);
  }


}
