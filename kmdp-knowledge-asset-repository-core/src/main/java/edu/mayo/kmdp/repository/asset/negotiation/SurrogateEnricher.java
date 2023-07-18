package edu.mayo.kmdp.repository.asset.negotiation;

import static edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository.defaultSurrogateModel;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.encode;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.derivationreltype.DerivationTypeSeries.Is_Transcreation_Of;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.language.TransrepresentationExecutor;
import edu.mayo.kmdp.repository.asset.index.Index;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder.HrefType;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.TransrepresentationOperator;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

/**
 * Temporary helper class that augments a {@link KnowledgeAsset} with graph and server-specific
 * information: URL locators, ephemeral carriers and links
 * <p>
 * Eventually, this class be rewritten/absorbed into a new version that (re)constructs a
 * KnowledgeAsset from the graph content, probably using GrpahQL
 */
public final class SurrogateEnricher {

  private SurrogateEnricher() {
    // functions ony
  }


  public static KnowledgeAsset enrichSurrogate(
      KnowledgeAsset asset,
      KARSHrefBuilder hrefBuilder,
      TransxionApiInternal translator,
      Index index,
      boolean withInverses) {
    if (hrefBuilder != null) {
      addDefaultCarrierLocators(asset, hrefBuilder);
      addDefaultSurrogateLocators(asset, hrefBuilder);
    }

    if (index != null && withInverses) {
      rewriteLinks(asset, index);
    }

    if (hrefBuilder != null && translator != null) {
      addEphemeralTranslations(asset, translator, hrefBuilder);
    }
    return asset;
  }


  /* ---------------------------------------------------------------------------------------- */


  private static void rewriteLinks(KnowledgeAsset asset, Index index) {
    var fullLinks = index.getNeighbourAssets(asset.getAssetId());
    var oldLinks = new ArrayList<>(asset.getLinks());

    assert oldLinks.stream().allMatch(oldL -> fullLinks.stream().anyMatch(
        newL -> Objects.equals(oldL.getHref().asKey(), newL.getHref().asKey())
            && oldL.getRel().sameTermAs(newL.getRel())
    ));
    asset.getLinks().clear();
    asset.withLinks(fullLinks);
  }

  /* ---------------------------------------------------------------------------------------- */

  private static void addEphemeralTranslations(
      KnowledgeAsset asset,
      TransxionApiInternal translator,
      KARSHrefBuilder hrefBuilder) {
    if (translator instanceof TransrepresentationExecutor) {
      addEphemeralTranslations(asset, (TransrepresentationExecutor) translator, hrefBuilder);
    }
  }

  private static void addEphemeralTranslations(
      KnowledgeAsset asset,
      TransrepresentationExecutor translator,
      KARSHrefBuilder hrefBuilder) {
    addTranslatableSurrogates(asset, translator, hrefBuilder);
    addTranslatableCarriers(asset, translator, hrefBuilder);
  }

  private static void addTranslatableCarriers(
      KnowledgeAsset asset,
      TransrepresentationExecutor translator,
      KARSHrefBuilder hrefBuilder) {
    var mapped = asset.getCarriers().stream().flatMap(carrier ->
            translatableCarriers(carrier, translator, asset, hrefBuilder))
        .filter(ka -> !superseded(ka, asset.getCarriers()))
        .collect(Collectors.toList());
    asset.getCarriers().addAll(mapped);
  }


  private static void addTranslatableSurrogates(
      KnowledgeAsset asset,
      TransrepresentationExecutor translator,
      KARSHrefBuilder hrefBuilder) {
    var mapped = asset.getSurrogate().stream().flatMap(surr ->
            translatableSurrogates(surr, translator, asset, hrefBuilder))
        .filter(ka -> !superseded(ka, asset.getSurrogate()))
        .collect(Collectors.toList());
    asset.getSurrogate().addAll(mapped);
  }

  private static Stream<KnowledgeArtifact> translatableCarriers(
      KnowledgeArtifact carrier,
      TransrepresentationExecutor translator,
      KnowledgeAsset asset,
      KARSHrefBuilder hrefBuilder) {
    return matchingTranslators(carrier, translator).map(tx -> {
      var tgtRep = getCanonicalRepresentation(tx);
      return new KnowledgeArtifact()
          .withArtifactId(defaultArtifactId(asset.getAssetId(), tgtRep.getLanguage()))
          .withDescription("(Ephemeral)")
          .withLinks(new Derivative()
              .withHref(carrier.getArtifactId())
              .withRel(Is_Transcreation_Of))
          .withLocator(hrefBuilder.getContentHref(
              asset.getAssetId(), carrier.getArtifactId(), tgtRep, HrefType.EPHEMERAL_CARRIER))
          .withRepresentation(tgtRep)
          .withMimeType(ModelMIMECoder.encode(tgtRep))
          .withName(carrier.getName());
    });
  }

  private static SyntacticRepresentation getCanonicalRepresentation(
      TransrepresentationOperator tx) {
    return tx.getInto().stream()
        .filter(rep -> rep.getEncoding() != null).findFirst()
        .or(() -> tx.getInto().stream()
            .filter(rep -> rep.getCharset() != null).findFirst())
        .or(() -> tx.getInto().stream()
            .filter(rep -> rep.getFormat() != null).findFirst())
        .or(() -> tx.getInto().stream()
            .filter(rep -> rep.getLanguage() != null).findFirst())
        .or(() -> tx.getInto().stream().findFirst())
        .orElseGet(SyntacticRepresentation::new);
  }


  private static Stream<KnowledgeArtifact> translatableSurrogates(
      KnowledgeArtifact carrier,
      TransrepresentationExecutor translator,
      KnowledgeAsset asset,
      KARSHrefBuilder hrefBuilder) {
    return matchingTranslators(carrier, translator).map(tx -> {
      var tgtRep = getCanonicalRepresentation(tx);
      return new KnowledgeArtifact()
          .withArtifactId(defaultSurrogateId(asset.getAssetId(), tgtRep.getLanguage()))
          .withDescription("(Ephemeral)")
          .withLinks(new Derivative()
              .withHref(carrier.getArtifactId())
              .withRel(Is_Transcreation_Of))
          .withLocator(hrefBuilder.getContentHref(
              asset.getAssetId(), carrier.getArtifactId(), tgtRep, HrefType.EPHEMERAL_SURROGATE))
          .withRepresentation(tgtRep)
          .withMimeType(ModelMIMECoder.encode(tgtRep))
          .withName(carrier.getName());
    });
  }

  private static Stream<TransrepresentationOperator> matchingTranslators(
      KnowledgeArtifact carrier,
      TransrepresentationExecutor translator) {
    return translator.listTxionOperators(encode(carrier.getRepresentation()), null)
        .orElseGet(Collections::emptyList).stream()
        .filter(tx -> tx.getInto().stream()
            // exclude the canonical form
            .noneMatch(rep -> Knowledge_Asset_Surrogate_2_0.sameAs(rep.getLanguage())))
        .filter(tx -> tx.getFrom().stream().anyMatch(
            srcRep -> theRepContrastor.isBroaderOrEqual(srcRep, carrier.getRepresentation())));
  }


  private static boolean superseded(KnowledgeArtifact ephemeral,
      List<KnowledgeArtifact> concretes) {
    return concretes.stream().anyMatch(c -> supersedes(c, ephemeral));
  }

  private static boolean supersedes(KnowledgeArtifact concrete, KnowledgeArtifact ephemeral) {
    return Objects.equals(concrete.getLocator(), ephemeral.getLocator())
        || theRepContrastor.isBroaderOrEqual(
        concrete.getRepresentation(), ephemeral.getRepresentation());
  }

  /* ---------------------------------------------------------------------------------------- */


  private static void addDefaultSurrogateLocators(
      KnowledgeAsset surrogate,
      KARSHrefBuilder hrefBuilder) {
    surrogate.getSurrogate().stream()
        .filter(ka -> ka.getLocator() == null)
        .forEach(ka -> {
          if (!defaultSurrogateModel.sameAs(ka.getRepresentation().getLanguage())) {
            ka.setLocator(hrefBuilder.getContentHref(
                surrogate.getAssetId(),
                ka.getArtifactId(),
                ka.getRepresentation(),
                HrefType.ASSET_SURROGATE_VERSION_CONTENT));
          } else {
//            ka.setLocator(hrefBuilder.getHref(
//                surrogate.getAssetId(),
//                ka.getArtifactId(),
//                HrefType.CANONICAL_SURROGATE));
          }
        });
  }

  private static void addDefaultCarrierLocators(KnowledgeAsset surrogate,
      KARSHrefBuilder hrefBuilder) {
    surrogate.getCarriers().stream()
        .filter(ka -> ka.getLocator() == null)
        .forEach(ka ->
            ka.setLocator(hrefBuilder.getContentHref(
                surrogate.getAssetId(),
                ka.getArtifactId(),
                ka.getRepresentation(),
                HrefType.ASSET_CARRIER_VERSION_CONTENT)
            ));
  }


}
