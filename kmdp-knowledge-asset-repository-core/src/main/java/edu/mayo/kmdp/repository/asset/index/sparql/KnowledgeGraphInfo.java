package edu.mayo.kmdp.repository.asset.index.sparql;

import static java.nio.charset.Charset.defaultCharset;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings.DEFAULT;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Assertional_Knowledge;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Descriptive metadata about a Knowledge Asset Repsitory's Knowledge Graph
 *
 * Includes 'well known' Identifiers of the Graph as an Asset, Artifact, Surrogate and KB
 * as well as the Graph's Asset canonical Surrogate.
 *
 * All the IDs and metadata are derived deterministically from the Graph Asset ID,
 * which can either use a default value, or be configurable (statically via an env. property)
 */
@Component
public class KnowledgeGraphInfo {

  private static final String LABEL = "Knowledge Asset Repository Knowledge Graph";

  public static final UUID DEFAULT_GRAPH_UUID = UUID
      .fromString("4bea6c68-25a8-4c9b-9b5e-41b4cd1fe29b");

  public static final String API4KP = "https://www.omg.org/spec/API4KP/api4kp/";
  public static final String ASSET = "KnowledgeAsset";
  public static final String KBASE_URI = API4KP + "KnowledgeBase";
  public static final URI ASSET_URI = URI.create(API4KP + ASSET);

  public static final SyntacticRepresentation graphCodedRepresentation =
      rep(OWL_2, XML_1_1, defaultCharset(), DEFAULT);
  public static final SyntacticRepresentation graphAbstractRepresentation =
      rep(OWL_2);

  @Value("${edu.mayo.kmdp.repository.graph.identifier:4bea6c68-25a8-4c9b-9b5e-41b4cd1fe29b}")
  private static final UUID graphUUID = DEFAULT_GRAPH_UUID;

  private ResourceIdentifier graphAssetId;
  private ResourceIdentifier graphKBaseId;
  private ResourceIdentifier graphArtifactId;
  private ResourceIdentifier graphSurrogateId;

  private KnowledgeAsset surrogate;

  public static KnowledgeGraphInfo newKnowledgeGraphInfo() {
    var kgi = new KnowledgeGraphInfo();
    kgi.init();
    return kgi;
  }

  /**
   * Initializes the internal data structures, including IDs and the canonical Surrogate
   */
  @PostConstruct
  public void init() {
    this.graphAssetId = newId(graphUUID, VERSION_ZERO);
    this.graphKBaseId = newId(Util.hashUUID(graphUUID, Util.uuid(KBASE_URI)), VERSION_ZERO);
    this.graphArtifactId = defaultArtifactId(graphAssetId, OWL_2);
    this.graphSurrogateId = defaultSurrogateId(graphAssetId, Knowledge_Asset_Surrogate_2_0);

    this.surrogate = SurrogateBuilder.newSurrogate(graphAssetId)
        .withFormalType(Terminology_Ontology_And_Assertional_KBs, Assertional_Knowledge)
        .withName(LABEL, null)
        .get()
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(graphSurrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON, defaultCharset(), DEFAULT)))
        .withSurrogate(new KnowledgeArtifact()
            .withArtifactId(graphSurrogateId)
            .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, defaultCharset(), DEFAULT)))
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(graphArtifactId)
            .withRepresentation(rep(OWL_2, XML_1_1, defaultCharset(), DEFAULT)));
  }

  /**
   * @return the Graph Asset ID
   */
  public ResourceIdentifier knowledgeGraphAssetId() {
    return this.graphAssetId;
  }

  /**
   * @return the Graph Canonical Surrogate ID
   */
  public ResourceIdentifier knowledgeGraphSurrogateId() {
    return this.graphSurrogateId;
  }

  /**
   * @return the Graph Artifact ID, for the default RDF graph manifestation of the Graph
   */
  public ResourceIdentifier knowledgeGraphArtifactId() {
    return this.graphArtifactId;
  }

  /**
   * @return the Graph Knowledge Base ID
   */
  public ResourceIdentifier graphKnowledgeBaseId() {
    return this.graphKBaseId;
  }

  /**
   * @return the Graph Canonical Surrogate
   */
  public KnowledgeAsset getKnowledgeGraphSurrogate() {
    return surrogate;
  }

  /**
   * @return the Graph label
   */
  public String getKnowledgeGraphLabel() {
    return LABEL;
  }


  /**
   * @param assetId the Id to be tested
   * @return true if the id is the ID of the Graph as an Asset
   */
  public boolean isKnowledgeGraphAsset(UUID assetId) {
    return this.graphAssetId.getUuid().equals(assetId);
  }

  /**
   * @param surrogateId the Id to be tested
   * @return true if the id is the ID of the Graph's canonical Surrogate
   */
  public boolean isKnowledgeGraphSurrogate(UUID surrogateId) {
    return this.graphSurrogateId.getUuid().equals(surrogateId);
  }

  /**
   * @param carrierId the Id to be tested
   * @return true if the id is the ID of the Graph's Artifact Manifestation
   */
  public boolean isKnowledgeGraphCarrier(UUID carrierId) {
    return this.graphArtifactId.getUuid().equals(carrierId);
  }
}
