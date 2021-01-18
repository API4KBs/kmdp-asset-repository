package edu.mayo.kmdp.repository.asset.composite;

import static edu.mayo.kmdp.util.JenaUtil.objA;
import static java.nio.charset.Charset.defaultCharset;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.of;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.SPARQL_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Concrete_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;
import static org.omg.spec.api4kp._20200801.taxonomy.structuralreltype.StructuralPartTypeSeries.Has_Structuring_Component;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.language.parsers.sparql.SparqlLifter;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.apache.jena.query.ParameterizedSparqlString;
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
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

public class CompositeHelper {

  private String structQuery;

  private _applyLift sparqlLifter;

  private _applyLower rdfLowerer;

  public CompositeHelper() {
    this.structQuery =
        FileUtil.read(SemanticKnowledgeAssetRepository.class.getResourceAsStream("/struct.sparql"))
            .orElseThrow(() -> new IllegalStateException("Unable to load struct.sparql"));

    this.sparqlLifter = new SparqlLifter();
    this.rdfLowerer = new JenaRdfParser();
  }

  public Answer<KnowledgeCarrier> getStructQuery(ResourceIdentifier rootId) {
    Answer<KnowledgeCarrier> query = sparqlLifter.applyLift(
        of(structQuery)
            .withRepresentation(rep(SPARQL_1_1,TXT,defaultCharset())),
        Concrete_Knowledge_Expression,null, null);

    return query.map(qkc -> {
      qkc.as(ParameterizedSparqlString.class).ifPresent(pss ->
          pss.setIri("?root", rootId.getVersionId().toString()));
      return qkc;
    });
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

  public KnowledgeCarrier toStructGraph(ResourceIdentifier structId, List<Bindings> bindings) {
    Model model = ModelFactory.createDefaultModel();
    bindings.forEach(b -> model.add(
        objA(b.get("s").toString(), b.get("p").toString(), b.get("o").toString())));
    return ofAst(model)
        .withAssetId(structId)
        .withRepresentation(rep(OWL_2));
  }

  public Answer<KnowledgeCarrier> toEncodedStructGraph(ResourceIdentifier structId,
      List<Bindings> bindings) {
    return rdfLowerer.applyLower(
        toStructGraph(structId, bindings),
        Encoded_Knowledge_Expression,
        codedRep(OWL_2, Turtle, TXT, defaultCharset(), Encodings.DEFAULT),
        null);
  }



}
