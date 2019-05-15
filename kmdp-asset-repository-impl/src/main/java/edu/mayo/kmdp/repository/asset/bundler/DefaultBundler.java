/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.repository.asset.bundler;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.Bundler;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeAssetRepository;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;

public class DefaultBundler implements Bundler {

    private SemanticKnowledgeAssetRepository coreApi;

    public DefaultBundler(SemanticKnowledgeAssetRepository coreApi) {
        super();
        this.coreApi = coreApi;
    }

    @Override
    public List<KnowledgeCarrier> bundle(String assetId, String version) {
        KnowledgeAsset asset = this.coreApi.getVersionedKnowledgeAsset(assetId, version).getBody();

        KnowledgeCarrier carrier = this.coreApi.getCanonicalKnowledgeAssetCarrier(assetId, version, null).getBody();

        List<KnowledgeCarrier> returnList = Lists.newArrayList();

        if( carrier != null ) {
            returnList.add(carrier);
        }

        Set<edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset> dependencies = SurrogateHelper.closure( asset, false );

        dependencies.forEach( x -> {
            retrieveCarriers( x, returnList );
        });

        return returnList;
    }

    private void retrieveCarriers( KnowledgeAsset x, List<KnowledgeCarrier> returnList ) {
        URIIdentifier uriIdentifier = x.getResourceId();

        if ( uriIdentifier != null ) {
            VersionIdentifier id = DatatypeHelper.toVersionIdentifier(uriIdentifier);
            returnList.add( this.coreApi.getCanonicalKnowledgeAssetCarrier( id.getTag(), id.getVersion(), null ).getBody() );
        } else {
            returnList.addAll(this.getAnnonymousArtifacts(x));
        }
    }

    private List<KnowledgeCarrier> getAnnonymousArtifacts(KnowledgeAsset assetSurrogate) {
        List<KnowledgeCarrier> carriers = Lists.newArrayList();
        if(assetSurrogate.getExpression() != null && assetSurrogate.getExpression().getCarrier() != null) {
            assetSurrogate.getExpression().getCarrier().stream().map(c -> (KnowledgeArtifact) c).forEach(carrier -> {
                URI masterLocation = carrier.getMasterLocation();
                if(masterLocation != null) {
                    BinaryCarrier newCarrier = new BinaryCarrier();
                    if(assetSurrogate.getExpression() != null &&
                            assetSurrogate.getExpression().getRepresentation() != null &&
                            assetSurrogate.getExpression().getRepresentation().getLanguage() != null) {
                        KnowledgeRepresentationLanguage language = assetSurrogate.getExpression().getRepresentation().getLanguage();
                        newCarrier.setRepresentation( new SyntacticRepresentation().withLanguage( language ) );
                    }

                    newCarrier.setAssetId(assetSurrogate.getResourceId());
                    newCarrier.setEncodedExpression(FileUtil.readBytes(masterLocation).orElse(new byte[0]));
                    carriers.add(newCarrier);
                }
            });
        }

        return carriers;
    }

}
