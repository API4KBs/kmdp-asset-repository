package edu.mayo.kmdp.repository.asset.negotiation;

import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotAcceptable;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.WEIGHT_UNSPECIFIED;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.repository.asset.HrefBuilder;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.WeightedRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormat;

public class ContentNegotiationHelper {

  HrefBuilder hrefBuilder;

  public ContentNegotiationHelper(HrefBuilder builder) {
    this.hrefBuilder = builder;
  }

  /**
   * Selects the best Surrogate, given the client preferences and the canonical Surrogate
   * Inspects the canonical Surrogate to determine if a different, better form exists,
   * otherwise returns the canonical Surrogate itself
   *
   * Currently only supports HTML as an alternative representation
   *
   * @param surrogate The canonical Surrogate
   * @param xAccept   A format MIME type expressing the client's preferences
   * @param defaultSurrogateRepresentation The representation of KnowledgeAsset surrogates
   * @return The best Surrogate
   */
  public Answer<KnowledgeAsset> negotiateCanonicalSurrogate(
      KnowledgeAsset surrogate, String xAccept,
      SyntacticRepresentation defaultSurrogateRepresentation) {
    // only support HTML (by redirection), or the default surrogate
    List<WeightedRepresentation> acceptable =
        decodePreferences(xAccept, defaultSurrogateRepresentation).stream()
            .filter(wrep ->
                HTML.sameAs(wrep.getRep().getLanguage()) ||
                    (defaultSurrogateRepresentation.getLanguage()
                        .sameAs(wrep.getRep().getLanguage())))
            .collect(Collectors.toList());

    if (acceptable.isEmpty()) {
      return Answer.of(NotAcceptable, null);
    }

    Optional<URI> redirectUri =
        negotiate(surrogate.getSurrogate(), acceptable)
            .map(KnowledgeArtifact::getLocator)
            .getOptionalValue();

    if (redirectUri.isPresent()) {
      return Answer.referTo(redirectUri.get(), false);
    } else if (HTML.sameAs(acceptable.get(0).getRep().getLanguage())) {
      return Answer.referTo(hrefBuilder.getRelativeURL("/surrogate"), false);
    } else {
      return Answer.of(surrogate);
    }
  }

  /**
   * Selects the best Artifact (surrogate), given an ordered list of preferred representations. If
   * no suitable candidate is found, will return one of the candidates, non-deterministically
   *
   * @param artifacts The candidate artifact surrogates
   * @param reps      The user-provided, weighted preferences
   * @return The first artifact that matches a representation that is first in order of preference,
   * or an artifact chosen non-deterministically
   * @see ContentNegotiationHelper#negotiateOrDefault(List, List, Float)
   */
  public Answer<KnowledgeArtifact> negotiateOrDefault(
      List<KnowledgeArtifact> artifacts,
      List<WeightedRepresentation> reps) {
    return negotiateOrDefault(artifacts, reps, getDefaultTolerance(reps));
  }

  /**
   * Selects the best Artifact (surrogate), given an ordered list of preferred representations. If
   * no suitable candidate is found, will return one of the candidates, non-deterministically
   * <p>
   * A weight threshold determines whether any candidate is still acceptable when the client
   * preferences cannot be honored. In particular, any candidate is considered acceptable if no
   * preference can be honored, but the strongest client-provided preference is below the threshold.
   * A low threshold implies strictness: even less than optimal client preferences must be honored
   *
   * @param artifacts                 The candidate artifact surrogates
   * @param reps                      The user-provided, weighted preferences
   * @param acceptAnyCarrierThreshold A weight threshold: the lower, the more a user preference must
   *                                  be honored
   * @return The first artifact that matches a representation that is first in order of preference,
   * or an artifact chosen non-deterministically
   * @see ContentNegotiationHelper#negotiate(List, List)
   */
  public Answer<KnowledgeArtifact> negotiateOrDefault(
      List<KnowledgeArtifact> artifacts,
      List<WeightedRepresentation> reps,
      Float acceptAnyCarrierThreshold) {

    if (reps.stream().allMatch(wr -> wr.getRep().getLanguage() == null)) {
      return anyCarrier(artifacts);
    }

    Answer<KnowledgeArtifact> chosen = Answer.of(reps.stream()
        .map(rep -> getBestCandidateForRepresentation(artifacts, rep))
        .flatMap(StreamUtil::trimStream)
        .findFirst());

    return (chosen.isSuccess() || getStrongestPreference(reps) > acceptAnyCarrierThreshold)
        ? chosen
        : anyCarrier(artifacts);
  }

  /**
   * the Default tolerance for a set of weighted representations, below which a
   * requested representation is not considered acceptable
   * Defaults to 0.0, except for HTML to support implicit requests by browsers and other user agents
   * @param reps the list of weighted representations
   * @return WEIGHT_UNSPECIFIED for HTML, 0.0 otherwise
   */
  private Float getDefaultTolerance(List<WeightedRepresentation> reps) {
    // HTML is treated with special regards, and always honored
    if (reps.isEmpty() || !HTML.sameAs(reps.get(0).getRep().getLanguage())) {
      return WEIGHT_UNSPECIFIED;
    } else {
      return 0.0f;
    }
  }

  private Float getStrongestPreference(List<WeightedRepresentation> reps) {
    return reps.isEmpty()
        ? ModelMIMECoder.WEIGHT_UNSPECIFIED
        : reps.get(0).getWeight();
  }

  /**
   * Returns any carrier, non-deterministically
   *
   * @param artifacts The list of candidates
   * @return One of the carriers
   */
  public Answer<KnowledgeArtifact> anyCarrier(List<KnowledgeArtifact> artifacts) {
    return Answer.of(artifacts.stream()
        .flatMap(StreamUtil.filterAs(KnowledgeArtifact.class))
        .findFirst());
  }

  /**
   * Selects the best Artifact (surrogate), given an ordered list of preferred representations
   *
   * @param artifacts The candidate artifact surrogates
   * @param reps      The preferred representations, in order of preference
   * @return The first artifact that matches a representation that is first in order of preference
   * @see ContentNegotiationHelper#getBestCandidateForRepresentation(List, WeightedRepresentation)
   */
  public Answer<KnowledgeArtifact> negotiate(
      List<KnowledgeArtifact> artifacts,
      List<WeightedRepresentation> reps) {
    return Answer.of(reps.stream()
        .map(rep -> getBestCandidateForRepresentation(artifacts, rep))
        .flatMap(StreamUtil::trimStream)
        .findFirst());
  }

  /**
   * Selects the best Artifact (surrogate), that matches a specified representation A match is found
   * when the artifact representation is the same, or more specific, than the desired
   * representation
   *
   * @param artifacts The candidate artifact surrogates
   * @param rep       The preferred representation
   * @return The first artifact that matches the given representation
   */
  public Optional<KnowledgeArtifact> getBestCandidateForRepresentation(
      List<KnowledgeArtifact> artifacts,
      WeightedRepresentation rep) {
    return artifacts.stream()
        .flatMap(StreamUtil.filterAs(KnowledgeArtifact.class))
        .filter(x -> theRepContrastor.isBroaderOrEqual(rep.getRep(), x.getRepresentation()))
        .findAny();
  }


  /**
   * Decodes a formal MIME type that encodes a client's representation preferences
   *
   * @param xAccept                the formal MIME code
   * @param fallbackRepresentation a representation that is returned in case no representations can
   *                               be extracted from the MIME code
   * @return a list of SyntacticRepresentations, ordered by weight
   */
  public static List<WeightedRepresentation> decodePreferences(String xAccept,
      SyntacticRepresentation fallbackRepresentation) {

    List<WeightedRepresentation> acceptableMimes
        = new ArrayList<>(decodeAll(xAccept, fallbackRepresentation));

    if (acceptableMimes.isEmpty() && fallbackRepresentation != null) {
      acceptableMimes.add(new WeightedRepresentation(fallbackRepresentation));
    }
    return acceptableMimes;
  }

  /**
   * Decodes a formal MIME type that encodes a client's representation preferences
   *
   * @param xAccept the formal MIME code
   * @return a list of SyntacticRepresentations, ordered by weight
   * @see ContentNegotiationHelper#decodePreferences(String, SyntacticRepresentation)
   */
  public static List<WeightedRepresentation> decodePreferences(String xAccept) {
    return decodePreferences(xAccept, null);
  }

  /**
   * Checks whether a Computable Knowledge Artifact matches at least one of the preferences
   * indicated in the Returns trivially 'true' if the preference list is empty
   *
   * @param descriptor The metadata descriptor of a Knowledge Artifact/Surrogate, which is expected
   *                   to contain representation information
   * @param xAccept    A coded list of preferences
   * @return true if the Artifact's representation is narrower or equal than at least one of the
   * preferences
   */
  public boolean isAcceptable(KnowledgeArtifact descriptor, String xAccept) {
    if (!Util.isEmpty(xAccept)) {
      Answer<KnowledgeArtifact> negotiatedCarrier =
          negotiateOrDefault(
              singletonList(descriptor),
              decodePreferences(xAccept));
      return negotiatedCarrier.isSuccess();
    }
    return true;
  }

  /**
   * Given a set of ranked, sorted preferences, returns the first format i.e. the format of the
   * first preference that specifies a format
   *
   * @param preferences the list of preferences
   * @return the first preference that includes a serialization format
   */
  public Optional<SerializationFormat> getPreferredFormat(
      List<WeightedRepresentation> preferences) {
    return preferences.stream()
        .map(WeightedRepresentation::getRep)
        .map(SyntacticRepresentation::getFormat)
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Decodes a formal MIME type and returns the Format component Uses a default value in case the
   * decoding fails
   *
   * @param xAccept the formal MIME type to be decoded
   * @param defaultFormat the fallback format
   * @return the format specified in the MIME type if present, the default format otherwise
   */
  public SerializationFormat decodePreferredFormat(String xAccept,
      SerializationFormat defaultFormat) {
    return Optional.ofNullable(xAccept)
        .flatMap(ModelMIMECoder::decode)
        .map(SyntacticRepresentation::getFormat)
        .orElse(defaultFormat);
  }
}
