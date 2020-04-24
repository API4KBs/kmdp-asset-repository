package edu.mayo.kmdp.repository.asset.negotiation;

import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static java.util.Collections.singletonList;
import static org.omg.spec.api4kp._1_0.contrastors.SyntacticRepresentationContrastor.theRepContrastor;

import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.api4kp.responsecodes._2011.ResponseCode;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._1_0.Answer;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder;
import org.omg.spec.api4kp._1_0.services.tranx.ModelMIMECoder.WeightedRepresentation;

public class ContentNegotiationHelper {

  private ContentNegotiationHelper() {
    // static functions only
  }

  public static Answer<KnowledgeAsset> negotiateCanonicalSurrogate(
      KnowledgeAsset surrogate, String xAccept,
      SyntacticRepresentation defaultSurrogateRepresentation) {
    // only support HTML, or the default surrogate
    List<SyntacticRepresentation> acceptable =
        decodePreferences(xAccept, defaultSurrogateRepresentation).stream()
            .filter(rep ->
                HTML.sameAs(rep.getLanguage()) ||
                    (defaultSurrogateRepresentation.getLanguage().sameAs(rep.getLanguage()) &&
                        defaultSurrogateRepresentation.getFormat().sameAs(rep.getFormat())))
            .collect(Collectors.toList());

    if (acceptable.isEmpty()) {
      return Answer.of(ResponseCode.NotAcceptable, null);
    }

    Optional<URI> redirectUri =
        negotiate(surrogate.getSurrogate(), acceptable)
            .map(ComputableKnowledgeArtifact::getLocator)
            .getOptionalValue();

    if (redirectUri.isPresent()) {
      return Answer.referTo(redirectUri.get(), false);
    } else {
      return Answer.of(surrogate);
    }
  }

  /**
   * Selects the best Artifact (surrogate), given an ordered list of
   * preferred representations. If no suitable candidate is found, will return
   * one of the candidates, non-deterministically
   *
   * @see ContentNegotiationHelper#negotiate(List, List)
   *
   * @param artifacts The candidate artifact surrogates
   * @return The first artifact that matches a representation that is first in order of preference,
   * or an artifact chosen non-deterministically
   */
  public static Answer<ComputableKnowledgeArtifact> negotiateOrDefault(
      List<KnowledgeArtifact> artifacts,
      List<SyntacticRepresentation> reps) {
    Answer<ComputableKnowledgeArtifact> chosen = Answer.of(reps.stream()
        .map(rep -> getBestCandidateForRepresentation(artifacts, rep))
        .flatMap(StreamUtil::trimStream)
        .findFirst());
    return chosen.isSuccess()
        ? chosen
        : anyCarrier(artifacts);
  }

  /**
   * Returns any carrier, non-deterministically
   * @param artifacts The list of candidates
   * @return One of the carriers
   */
  public static Answer<ComputableKnowledgeArtifact> anyCarrier(List<KnowledgeArtifact> artifacts) {
    return Answer.of(artifacts.stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .findFirst());
  }

  /**
   * Selects the best Artifact (surrogate), given an ordered list of
   * preferred representations
   *
   * @see ContentNegotiationHelper#getBestCandidateForRepresentation(List, SyntacticRepresentation)
   *
   * @param artifacts The candidate artifact surrogates
   * @param reps The preferred representations, in order of preference
   * @return The first artifact that matches a representation that is first in order of preference
   */
  public static Answer<ComputableKnowledgeArtifact> negotiate(
      List<KnowledgeArtifact> artifacts,
      List<SyntacticRepresentation> reps) {
    return Answer.of(reps.stream()
        .map(rep -> getBestCandidateForRepresentation(artifacts, rep))
        .flatMap(StreamUtil::trimStream)
        .findFirst());
  }

  /**
   * Selects the best Artifact (surrogate), that matches a specified representation
   * A match is found when the artifact representation is the same, or more specific, than
   * the desired representation
   *
   * @param artifacts The candidate artifact surrogates
   * @param rep The preferred representation
   * @return The first artifact that matches the given representation
   */
  public static Optional<ComputableKnowledgeArtifact> getBestCandidateForRepresentation(
      List<KnowledgeArtifact> artifacts,
      SyntacticRepresentation rep) {
    return artifacts.stream()
        .flatMap(StreamUtil.filterAs(ComputableKnowledgeArtifact.class))
        .filter(x -> theRepContrastor.isBroaderOrEqual(rep, x.getRepresentation()))
        .findAny();
  }


  /**
   * Decodes a formal MIME type that encodes a client's representation preferences
   *
   * @param xAccept the formal MIME code
   * @param fallbackRepresentation a representation that is returned in case no representations
   *                               can be extracted from the MIME code
   * @return a list of SyntacticRepresentations, ordered by weight
   */
  public static List<SyntacticRepresentation> decodePreferences(String xAccept,
      SyntacticRepresentation fallbackRepresentation) {

    List<WeightedRepresentation> acceptableMimes = xAccept != null
        ? Arrays.stream(xAccept.split(","))
        .map(code -> ModelMIMECoder.decodeWeighted(code, fallbackRepresentation))
        .sorted()
        .collect(Collectors.toList())
        : Collections.emptyList();

    List<SyntacticRepresentation> reps = acceptableMimes.stream()
        .map(WeightedRepresentation::getRep)
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());
    if (reps.isEmpty() && fallbackRepresentation != null) {
      reps.add(fallbackRepresentation);
    }
    return reps;
  }

  /**
   * Decodes a formal MIME type that encodes a client's representation preferences
   *
   * @see ContentNegotiationHelper#decodePreferences(String,SyntacticRepresentation)
   * @param xAccept the formal MIME code
   * @return a list of SyntacticRepresentations, ordered by weight
   */
  public static List<SyntacticRepresentation> decodePreferences(String xAccept) {
    return decodePreferences(xAccept,null);
  }

  /**
   * Checks whether a Computable Knowledge Artifact matches at least one of the preferences
   * indicated in the
   * Returns trivially 'true' if the preference list is empty
   *
   * @param descriptor The metadata descriptor of a Knowledge Artifact/Surrogate,
   *                   which is expected to contain representation information
   * @param xAccept A coded list of preferences
   * @return true if the Artifact's representation is narrower or equal than
   * at least one of the preferences
   */
  public static boolean isAcceptable(ComputableKnowledgeArtifact descriptor, String xAccept) {
    if (! Util.isEmpty(xAccept)) {
      Answer<ComputableKnowledgeArtifact> negotiatedCarrier =
          negotiate(singletonList(descriptor),decodePreferences(xAccept));
      return negotiatedCarrier.isSuccess();
    }
    return true;
  }
}
