package edu.mayo.kmdp.repository.asset;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.VersionedIdentifier;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.Association;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.asset.bundler.DefaultBundler;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.terms.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import edu.mayo.kmdp.terms.krformat._2018._08.KRFormat;
import edu.mayo.kmdp.terms.krlanguage._2018._08.KRLanguage;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.ASTCarrier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;

public class SemanticRepository implements KnowledgeAssetCatalogApiDelegate,
     {

	private static final String URI_BASE = Registry.MAYO_ASSETS_BASE_URI;

    private KnowledgeArtifactApi knowledgeArtifactApi;

    private KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi;

//    private TransxionApi txExecutor;
//
//    private DiscoveryApi txDiscovery;

    private Index index;

    private HrefBuilder hrefBuilder = new HrefBuilder();

    private Bundler bundler;

    public SemanticRepository() {
        //
    }

    public SemanticRepository( KnowledgeArtifactApi knowledgeArtifactApi,
                               KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi,
                               Index index) {
        super();
        this.knowledgeArtifactApi = knowledgeArtifactApi;
        this.knowledgeArtifactSeriesApi = knowledgeArtifactSeriesApi;
        this.index = index;
        this.bundler = new DefaultBundler(this);
//        this.txDiscovery = DiscoveryApi.newInstance( new LanguageServiceManager() );
//        this.txExecutor = TransxionApi.newInstance( new TransrepresentationExecutor() );
    }

    protected String getNewArtifactId() {
        return UUID.randomUUID().toString();
    }

    protected <T> ResponseEntity<T> wrap(T obj) {
        return new ResponseEntity<T>(obj, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> addKnowledgeAsset(KnowledgeAsset surrogate) {
        String assetId;
        String assetVersion;

        if(surrogate.getResourceId() == null) {
            assetId = UUID.randomUUID().toString();
            assetVersion = UUID.randomUUID().toString();

            surrogate.setResourceId(DatatypeHelper.uri(URI_BASE, assetId, assetVersion));
        } else {
            assetId = surrogate.getResourceId().getTag();
            assetVersion = surrogate.getResourceId().getVersion();
        }

        ResponseEntity<KnowledgeAsset> ka = this.initAsset(assetId, assetVersion, surrogate);

        return this.wrap(ka.getBody());
    }

    @Override
    public ResponseEntity<KnowledgeAsset> addKnowledgeAssetCarrier( String assetId, String versionTag, byte[] exemplar ) {
        String artifactId = this.getNewArtifactId();
        String artifactVersion = UUID.randomUUID().toString();

        return this.setKnowledgeAssetCarrier( assetId, versionTag, artifactId, artifactVersion, exemplar );
    }

    @Override
    public ResponseEntity<List<KnowledgeAsset>> filterAssets(KnowledgeCarrier queryExpression) {
        return null;
    }

    @Override
    public ResponseEntity<KnowledgeAssetCatalog> getAssetCatalog() {
        return this.wrap(new KnowledgeAssetCatalog());
    }

    @Override
    public ResponseEntity<KnowledgeCarrier> getDefaultKnowledgeAssetCarrier( String assetId, String versionTag, String xAccept ) {
        KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag).get();

        if (isCarrierNativelyAvailable(surrogate, xAccept)) {
            BinaryCarrier carrier = new BinaryCarrier();
            Optional<IndexPointer> artifactPtr = lookupDefaultCarrier(assetId, versionTag);
            getRepresentationLanguage(surrogate).ifPresent((lang) ->
                                                                   carrier.withRepresentation(new SyntacticRepresentation().withLanguage(lang)));
            carrier.withAssetId(surrogate.getResourceId());

            if ( artifactPtr.isPresent() ) {
                carrier.withEncodedExpression(resolve(artifactPtr.get()).get());
                return this.wrap(carrier);
            } else if ( surrogate.getExpression().getInlined() != null && !Util.isEmpty( surrogate.getExpression().getInlined().getExpr() ) ) {
                carrier.withEncodedExpression( surrogate.getExpression().getInlined().getExpr().getBytes() );
                return this.wrap(carrier);
            } else {
                System.err.println(" ASSET NOT FOUND");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {

            System.err.println("TODO: Lookup the appropriate translator... with  " + txExecutor);

            KnowledgeCarrier input = new ASTCarrier().withParsedExpression(surrogate)
                                                     .withRepresentation(new SyntacticRepresentation().withLanguage(KRLanguage.Asset_Surrogate));
            List<KnowledgeCarrier> assets = new LinkedList<>();
            assets.add(input);

            // TODO MOVE OUT: THIS IS THE SEED of a new Helper class : Struct-urer (the counterpart of Bundler)
            List<KnowledgeCarrier> deps = SurrogateHelper.closure(surrogate, false).stream()
                                                         .map((dep) -> retrieveAssetSurrogate(dep.getResourceId()))
                                                         .filter(Optional::isPresent)
                                                         .map(Optional::get)
                                                         .map((dep) -> new ASTCarrier().withParsedExpression(dep)
                                                                                       .withRepresentation(new SyntacticRepresentation().withLanguage(KRLanguage.Asset_Surrogate)))
                                                         .collect(Collectors.toList());
            assets.addAll(deps);

            KnowledgeCarrier kcarrier = this.txExecutor.applyOperator("SurrToCCG", assets);

            // TODO Need proper parse/serializtion APIs
            if (kcarrier instanceof ASTCarrier) {
                BinaryCarrier bCarrier = new edu.mayo.kmdp.common.model.BinaryCarrier()
                        .withEncodedExpression(JSonUtil.writeJson(((ASTCarrier) kcarrier).getParsedExpression())
                                                       .map(ByteArrayOutputStream::toByteArray)
                                                       .orElseThrow(IllegalStateException::new))
                        .withRepresentation(kcarrier.getRepresentation().withFormat(KRFormat.JSON));
                return this.wrap(bCarrier);
            }
            return this.wrap(kcarrier);
        }
   }

    private Optional<KnowledgeAsset> retrieveAssetSurrogate( URIIdentifier resourceId ) {
        VersionedIdentifier id = DatatypeHelper.toVersionIdentifier( resourceId );
        return retrieveAssetSurrogate( id.getTag(), id.getVersion() );
    }

    private Optional<KRLanguage> getRepresentationLanguage(KnowledgeAsset surrogate) {
		if ( surrogate.getExpression() != null && surrogate.getExpression().getRepresentation() != null ) {
			return Optional.of( surrogate.getExpression().getRepresentation().getLanguage() );
		} else {
			return Optional.empty();
		}
	}

	private Optional<IndexPointer> lookupDefaultCarrier( String assetId, String versionTag ) {
		IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
		return lookupDefaultCarrier( assetPointer );
	}

	private Optional<IndexPointer> lookupDefaultCarrier(IndexPointer assetPointer) {
		Set<IndexPointer> artifacts = this.index.getArtifactsForAsset(assetPointer);
		IndexPointer artifact;
		if( artifacts.size() == 0 ) {
			return Optional.empty();
		} else if(artifacts.size() > 1) {
			//TOOD FIXME
			artifact = artifacts.stream().filter( (a) -> ! "EMBEDDED".equals( a.getVersion() ) ).findFirst().orElse( null );
			if ( artifact == null ) {
				return Optional.empty();
			}
		} else {
			artifact = artifacts.iterator().next();
		}
    	return Optional.of( artifact );
	}

	private boolean isCarrierNativelyAvailable( KnowledgeAsset surrogate, String xAccept) {
        //TODO This is obviously much more complex...
        return ! StringUtils.equals( xAccept, CCG_MIME );
    }

    @Override
    public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrier(
            String assetId,
            String versionTag,
            String artifactId,
            String artifactVersionTag,
            String xAccept) {
        IndexPointer artifactPointer = new IndexPointer(artifactId, artifactVersionTag);

        byte[] data = this.resolve(artifactPointer).get();
        edu.mayo.kmdp.common.model.BinaryCarrier carrier = new edu.mayo.kmdp.common.model.BinaryCarrier().withEncodedExpression( data );
        return this.wrap(carrier);
    }

    @Override
    public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(String assetId, String versionTag) {
        IndexPointer assetPointer = new IndexPointer(assetId, versionTag);
        return this.wrap(this.index.getArtifactsForAsset(assetPointer).stream().map(pointer -> {
            Pointer p = new edu.mayo.kmdp.common.model.Pointer();
            p.setHref(this.hrefBuilder.getAssetCarrierVersionHref(assetId, versionTag, pointer.getId(), pointer.getVersion()));

            return p;
        }).collect(Collectors.toList()));
    }


    @Override
    public ResponseEntity<byte[]> getKnowledgeAssetExpression(String assetId, String versionTag) {
        return null;
    }

    @Override
    public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(String assetId) {
        IndexPointer pointer = this.index.getLatestAssetForId(assetId);

        return this.getVersionedKnowledgeAsset(pointer.getId(), pointer.getVersion());
    }

    @Override
    public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(String assetId, String versionTag) {
        return this.wrap( retrieveAssetSurrogate( assetId, versionTag ).get() );
    }

    @Override
    public ResponseEntity<KnowledgeAsset> initAsset(String assetId, String versionTag, KnowledgeAsset assetSurrogate) {
        System.err.println( "INITIALIZING ASSET " + assetId + ":" + versionTag );
        if(assetSurrogate.getResourceId() == null) {
            assetSurrogate.setResourceId(DatatypeHelper.uri(URI_BASE, assetId, versionTag));
        } else {
            if(! assetSurrogate.getResourceId().getTag().equals(assetId) ||
                ! assetSurrogate.getResourceId().getVersion().equals(versionTag)) {
                throw new RuntimeException("Surrogate ID/version must match asset ID/version.");
            }
        }

        String surrogateId = assetSurrogate.getResourceId().getTag();
        String surrogateVersion = assetSurrogate.getResourceId().getVersion();

        Pointer surrogate;
        surrogate = this.knowledgeArtifactApi.setKnowledgeArtifactVersion( REPOSITORY_ID,
                                                                           surrogateId,
                                                                           surrogateVersion,
                                                                           JSonUtil.writeJson( assetSurrogate ).map( ByteArrayOutputStream::toByteArray )
                                                                                   .orElseThrow( RuntimeException::new ) );

        IndexPointer surrogatePointer = new IndexPointer(surrogateId, surrogateVersion);

        this.index.registerAsset(
                new IndexPointer(assetId, versionTag),
                surrogatePointer,
                assetSurrogate.getType(),
                assetSurrogate.getSubject(),
                assetSurrogate.getName(),
                assetSurrogate.getDescription());

        this.index.registerLocation(surrogatePointer, surrogate.getHref().toString());

        if(assetSurrogate.getExpression() != null && assetSurrogate.getExpression().getCarrier() != null) {
            assetSurrogate.getExpression().getCarrier().stream().map(c -> (KnowledgeArtifact) c).forEach(carrier -> {
                URI masterLocation = carrier.getMasterLocation();
                if(masterLocation != null) {
                    // TODO FIXME 'masterLocation' can be set with or without actually embedding the artifact.
                    // Reserving 'EMBEDDED' also seems brittle
                    IndexPointer carrierPointer = new IndexPointer(masterLocation.toString(), "EMBEDDED");
                    this.index.registerArtifactToAsset(new IndexPointer(assetId, versionTag), carrierPointer);
                    this.index.registerLocation(carrierPointer, masterLocation.toString());
                }
            });
        }

        // recurse to register dependencies
        assetSurrogate.getRelated().stream().
                map(Association::getTgt).
                filter(knowledgeResource -> knowledgeResource instanceof KnowledgeAsset).
                map(knowledgeResource -> (KnowledgeAsset) knowledgeResource).
                forEach(dependency -> {
                    // if the resource id is null must be anonymous
                    // only do this if it has a 'type'. This is to distinguish 'stubs' vs full resources.
                    if(dependency.getResourceId() != null && ! CollectionUtils.isEmpty(dependency.getType())) {
                        String id = dependency.getResourceId().getTag();
                        String version = dependency.getResourceId().getVersion();

                        initAsset(id, version, dependency);
                    }
        });


        return this.wrap(assetSurrogate);
    }

    @Override
    public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(String assetId) {
        List<Pointer> pointers = this.knowledgeArtifactSeriesApi.getKnowledgeArtifactSeries(REPOSITORY_ID, assetId);

        List<Pointer> versionPointers = pointers.stream().map(pointer -> {
            return this.toPointer(pointer.getEntityRef(), HrefType.ASSET_VERSION);
        }).collect(Collectors.toList());

        return this.wrap(versionPointers);
    }


    @Override
    public ResponseEntity<List<Pointer>> listKnowledgeAssets( String assetType, final String annotation ) {
        List<Pointer> pointers;

        validateFilters( assetType, annotation );

        Set<IndexPointer> list = this.index.getAssetIdsByType( assetType );
        Set<IndexPointer> annos = this.index.getAssetIdsByAnnotation( annotation );
        list.retainAll( annos );

        List<Pointer> returnList = list.stream().map(asset -> this.toPointer( asset, HrefType.ASSET ) ).collect( Collectors.toList() );

        pointers = returnList;

        return this.wrap(this.aggregateVersions(pointers));
    }

    private boolean validateFilters( String assetType, String annotation ) {
        // Defensive programming: ensure assetType is a known type, and that annotation is a fully qualified URI
        try {
        	if ( assetType != null ) {
		        Optional<KnowledgeAssetType> type = KnowledgeAssetType.resolve(assetType);
		        if (!type.isPresent()) {
			        throw new IllegalStateException("Unrecognized asset type " + assetType);
		        }
	        }
	        if ( annotation != null ) {
	            //TODO:
		        //URI annotationUri = new URI(annotation);
	        }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<Pointer> aggregateVersions(List<Pointer> pointers) {
        Map<URI, List<Pointer>> versions = Maps.newHashMap();

        pointers.stream().forEach((pointer -> {
            URI id = pointer.getEntityRef().getUri();
            if(! versions.containsKey(id)) {
                versions.put(id, Lists.newArrayList());
            }
            versions.get(id).add(pointer);
        }));

        List<Pointer> returnList = Lists.newArrayList();
        for(URI assetId : versions.keySet()) {
            Pointer latest = versions.get(assetId).get(0);
            returnList.add(latest);
        }

        return returnList;
    }

    @Override
    public ResponseEntity<List<KnowledgeCarrier>> bundle(String assetId,
                                                         String versionTag) {
        return this.wrap(this.bundler.bundle(assetId, versionTag));
    }

    @Override
    public ResponseEntity<List<KnowledgeAsset>> searchAssets(List<String> searchTerms) {
        return null;
    }

    @Override
    public ResponseEntity<KnowledgeAsset> setKnowledgeAssetCarrier(String assetId, String versionTag, String artifactId, String artifactVersion, byte[] exemplar) {
        System.err.println( "ADDING CARRIER TO ASSET " + assetId + ":" + versionTag + " >>> " + artifactId + ":" + artifactVersion );
        Pointer artifactPointer = this.knowledgeArtifactApi.setKnowledgeArtifactVersion(REPOSITORY_ID, artifactId, artifactVersion, exemplar);
        this.index.registerLocation(new IndexPointer(artifactId, artifactVersion), artifactPointer.getHref().toString());

        this.index.registerArtifactToAsset(new IndexPointer(assetId, versionTag), new IndexPointer(artifactId, artifactVersion));

        KnowledgeAsset surrogate = retrieveAssetSurrogate(assetId, versionTag).get();

        return this.wrap(surrogate);
    }

	private Optional<KnowledgeAsset> retrieveAssetSurrogate(String assetId, String versionTag) {
		IndexPointer surrogatePointer = this.index.getSurrogateForAsset(new IndexPointer(assetId, versionTag));
        return this.resolve(surrogatePointer).flatMap( (sr) -> JSonUtil.readJson( sr, KnowledgeAsset.class) );
	}


    @Override
    public ResponseEntity<KnowledgeAsset> updateVersionedKnowledgeAsset(String assetId, KnowledgeAsset assetSurrogate, byte[] exemplar) {
        return null;
    }

    private enum HrefType {ASSET, ASSET_VERSION, ASSET_CARRIER}

    private Pointer toPointer(URIIdentifier entityRef, HrefType hrefType) {
        VersionIdentifier id = DatatypeHelper.toVersionIdentifier( entityRef );
        return this.toPointer(new IndexPointer(id.getTag(), id.getVersion()), hrefType);
    }

    private Pointer toPointer(IndexPointer pointer, HrefType hrefType) {
        String id = pointer.getId();
        String version = pointer.getVersion();

        Pointer p = new edu.mayo.kmdp.common.model.Pointer();
        p.setEntityRef(DatatypeHelper.uri(URI_BASE,id, version));

        Index.DescriptiveMetadata metadata = this.index.getDescriptiveMetadataForAsset(pointer);
        p.setName(metadata.name);
        p.setSummary(metadata.description);
        if(metadata.type != null) {
            p.setType(URI.create(metadata.type));
        }

        URI href;
        switch (hrefType) {
            case ASSET: href = this.hrefBuilder.getAssetHref(id); break;
            case ASSET_VERSION: href = this.hrefBuilder.getAssetVersionHref(id, version); break;
            default: throw new IllegalStateException();
        }
        p.setHref(href);
        return p;
    }

    protected Optional<byte[]> resolve(IndexPointer pointer) {
        if(pointer == null) {
            return Optional.empty();
        } else {
            String location = this.index.getLocation(pointer);

            Matcher matcher = Pattern.compile("^.*/artifacts/(.*)/versions/(.*)$").matcher(location);
            if (matcher.matches() && matcher.groupCount() == 2) {
                return Optional.ofNullable(knowledgeArtifactApi.getKnowledgeArtifactVersion(REPOSITORY_ID, matcher.group(1), matcher.group(2)));
            } else {
                URI uri = URI.create(location);

                return SemanticRepositoryUtils.resolve(uri);
            }
        }
    }

}
