package edu.mayo.kmdp.repository.asset;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.assetId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.KNART_1_3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.language.test.MockTranslator;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toLibraryTranslator;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.asset.negotiation.SurrogateEnricher;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class SurrogateEnrichmentTest {

  KARSHrefBuilder hrefBuilder = new KARSHrefBuilder(new Properties());
  TransrepresentationExecutor txor = new TransrepresentationExecutor(Arrays.asList(
      new SurrogateV2toHTMLTranslator(),
      new SurrogateV2toLibraryTranslator(),
      new MockTranslator()
  ));

  @Test
  void testDefaultLocators() {
    var surr = buildAsset(UUID.randomUUID(), "1.0.0", "");
    var enriched = SurrogateEnricher.enrichSurrogate(surr, hrefBuilder, txor, null, false);

    enriched.getCarriers().stream()
        .filter(x -> HTML.sameAs(x.getRepresentation().getLanguage()))
        .filter(x -> "http://localhost:8920/test".equals(x.getLocator().toString()))
        .findFirst().orElseGet(Assertions::fail);
    enriched.getCarriers().stream()
        .filter(x -> KNART_1_3.sameAs(x.getRepresentation().getLanguage()))
        .filter(x -> x.getLocator() != null)
        .findFirst().orElseGet(Assertions::fail);

  }


  private KnowledgeAsset buildAsset(UUID assetId, String versionTag, String inlined) {
    return new KnowledgeAsset()
        .withAssetId(assetId(Registry.DID_URN_URI, assetId, versionTag))
        .withFormalCategory(Rules_Policies_And_Guidelines)
        .withFormalType(Clinical_Rule)
        .withName("Mock Rule")
        .withCarriers(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocalization(English)
                .withName("Mock Rule - KNART version")
                .withRepresentation(rep(KNART_1_3, XML_1_1))
                .withInlinedExpression(inlined),
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withLocalization(English)
                .withName("Some Text")
                .withRepresentation(rep(HTML, TXT))
                .withLocator(URI.create("http://localhost:8920/test")))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(randomArtifactId())
                .withName("((Self))")
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        );
  }


}
