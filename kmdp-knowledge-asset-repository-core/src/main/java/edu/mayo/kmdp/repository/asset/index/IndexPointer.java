package edu.mayo.kmdp.repository.asset.index;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.registry.Registry;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;

import java.net.URI;
import java.util.UUID;

/**
 * A wrapper over {@link URIIdentifier} used to index Assets and Artifacts.
 *
 * NOTE: this will most likely get replaced with some other ID abstraction, or fall
 * back to just the {@link URIIdentifier}.
 */
public class IndexPointer extends URIIdentifier {

  public static final String URI_BASE = Registry.MAYO_ASSETS_BASE_URI;

  public IndexPointer(UUID id, String version) {
    super();
    URIIdentifier uriIdentifier = DatatypeHelper.uri(URI_BASE, id.toString(), version);
    uriIdentifier.copyTo(this);
  }

  public IndexPointer(URIIdentifier id) {
    super();
    id.copyTo(this);
  }

  public IndexPointer(URI id, String version) {
    super();
    URIIdentifier uriIdentifier = DatatypeHelper.uri(id.toString(), version);
    uriIdentifier.copyTo(this);
  }

}
