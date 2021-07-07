package edu.mayo.kmdp.repository.asset.index;

import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;

import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetRepositoryServerProperties.KnowledgeAssetRepositoryOptions;
import java.net.URI;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

/**
 * Mapper that resolves UUIDs (+ versionTag) pairs to the
 * full (version) URIs of the Resources identified by that pair.
 *
 * This implementation assumes ONE default base namespace per Resource type
 * (i.e. one for Assets, one for Artifacts)
 *
 *
 */
public class IdentityMapper {

  private final URI assetNamespace;

  private final URI artifactNamespace;

  public IdentityMapper() {
    this(KnowledgeAssetRepositoryServerProperties.emptyProperties());
  }

  public IdentityMapper(KnowledgeAssetRepositoryServerProperties cfg) {
    this.assetNamespace =
        URI.create(cfg.getTyped(KnowledgeAssetRepositoryOptions.ASSET_NAMESPACE));
    this.artifactNamespace =
        URI.create(cfg.getTyped(KnowledgeAssetRepositoryOptions.ARTIFACT_NAMESPACE));
  }

  public ResourceIdentifier toAssetId(UUID assetId) {
    return SemanticIdentifier.newId(assetNamespace, assetId);
  }

  public ResourceIdentifier toAssetId(UUID assetId, String versionTag) {
    return SemanticIdentifier.newId(assetNamespace, assetId, toSemVer(versionTag));
  }

  public ResourceIdentifier toArtifactId(UUID assetId) {
    return SemanticIdentifier.newId(artifactNamespace, assetId);
  }

  public ResourceIdentifier toArtifactId(UUID assetId, String versionTag) {
    return SemanticIdentifier.newId(artifactNamespace, assetId, toSemVer(versionTag));
  }

  public URI getAssetNamespace() {
    return assetNamespace;
  }

  public URI getArtifactNamespace() {
    return artifactNamespace;
  }
}
