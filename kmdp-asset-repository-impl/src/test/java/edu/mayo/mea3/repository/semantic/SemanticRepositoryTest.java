package edu.mayo.mea3.repository.semantic;

import edu.mayo.kmdp.common.model.KnowledgeAsset;
import edu.mayo.kmdp.common.model.SimpleAnnotation;
import edu.mayo.kmdp.repository.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.terms.AssetVocabulary;
import edu.mayo.kmdp.terms.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import edu.mayo.mea3.repository.jcr.JcrRepositoryAdapter;
import edu.mayo.terms.pco.PCO;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.Before;
import org.junit.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

import static edu.mayo.kmdp.terms.kao.knowledgeassettype._1_0.KnowledgeAssetType.Care_Process_Model;
import static org.junit.Assert.*;


public class SemanticRepositoryTest {

    SemanticRepository semanticRepository;

    @Before
    public void setUpRepos() throws Exception {
        JcrRepositoryAdapter repos = new JcrRepositoryAdapter(new Jcr(new Oak()).createRepository());

        MapDbIndex index = new MapDbIndex();

        KnowledgeArtifactApi knowledgeArtifactApi = KnowledgeArtifactApi.newInstance( repos );
        KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi = KnowledgeArtifactSeriesApi.newInstance( repos );

        semanticRepository = new SemanticRepository( knowledgeArtifactApi, knowledgeArtifactSeriesApi, index );
    }

    @Test
    public void testInit() {
        assertNotNull( semanticRepository );
    }

    @Test
    public void testPointersHaveType() {
        assertNotNull( semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ) );
        List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 1, assets.size() );

        assertEquals( KnowledgeAssetType.Care_Process_Model.getRef(), assets.get(0).getType() );
    }

    @Test
    public void initAndGetAssetByType() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ) );
        List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 1, assets.size() );
    }

    @Test
    public void getVersions() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ) );
        assertNotNull(  semanticRepository.initAsset("foo", "2", new KnowledgeAsset().withType( Care_Process_Model ) ) );

        List<Pointer> versions = semanticRepository.getKnowledgeAssetVersions("foo").getBody();

        assertEquals( 2, versions.size() );
    }

    @Test
    public void addAndGetAssetByType() {
        assertNotNull(  semanticRepository.addAsset( new KnowledgeAsset().withType( Care_Process_Model ) ));
        List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 1, assets.size());
    }

    @Test
    public void listKnowledgeAssetsMultipleVersions() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ) );
        assertNotNull(  semanticRepository.initAsset("foo", "2", new KnowledgeAsset().withType( Care_Process_Model ) ) );

        List<Pointer> versions = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 1, versions.size() );
    }

    @Test
    public void listKnowledgeAssetsMultipleVersionsCorrectHrefAndId() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ));
        assertNotNull(  semanticRepository.initAsset("foo", "2", new KnowledgeAsset().withType( Care_Process_Model ) ));

        List<Pointer> versions = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 1, versions.size() );

        assertNotNull(  versions.get(0).getEntityRef().getVersionId() );
        assertNotNull(  versions.get(0).getEntityRef().getVersion() );

        assertFalse(versions.get(0).getHref().toString().contains("versions") );

    }

    @Test
    public void addAndGetAssetByTypeWithNone() {
        List<Pointer> assets = semanticRepository.listKnowledgeAssets(Care_Process_Model.getTag(), null).getBody();

        assertEquals( 0, assets.size() );
    }

    @Test
    public void addAndGetAssetByNoType() {
        assertNotNull(  semanticRepository.addAsset( new KnowledgeAsset().withType( Care_Process_Model ) ) );
        List<Pointer> assets = semanticRepository.listKnowledgeAssets(null, null).getBody();

        assertEquals( 1, assets.size() );
    }

    @Test
    public void initAndGetAssetByAnnotation() {
        assertNotNull(  semanticRepository.initAsset("1", "1",
                                                     new KnowledgeAsset().withSubject(
                                                             new SimpleAnnotation().withExpr(
                                                                     new ConceptIdentifier().withRef(
                                                                             URI.create("http://something"))))));

        assertEquals( 1, semanticRepository.listKnowledgeAssets(null, "http://something").getBody().size() );
    }

    @Test
    public void initAndGetAssetByAnnotationAndRel() {
        assertNotNull(  semanticRepository.initAsset("1", "1",
                new KnowledgeAsset().withSubject(
                        new SimpleAnnotation().withExpr(
                                new ConceptIdentifier().withRef(
                                        URI.create("http://something"))).withRel(new ConceptIdentifier()
                                .withRef(URI.create("http://somerel"))))
                )
        );

        assertEquals( 1, semanticRepository.listKnowledgeAssets(null, "http://somerel:http://something").getBody().size() );
    }

    @Test
    public void addKnowledgeAssetCarrier() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ));

        semanticRepository.setKnowledgeAssetCarrier("foo", "1", "q", "z", "there".getBytes() );
        ResponseEntity<KnowledgeCarrier> artifact = semanticRepository.getKnowledgeAssetCarrier("foo", "1", "q", "z", null);

        assertEquals( "there", new String(((BinaryCarrier)artifact.getBody()).getEncodedExpression()));
    }

    @Test
    public void addKnowledgeAssetCarriers() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ));

        semanticRepository.setKnowledgeAssetCarrier("foo", "1", "q", "z", "there".getBytes());
        ResponseEntity<List<Pointer>> artifacts = semanticRepository.getKnowledgeAssetCarriers("foo", "1");

        assertEquals( 1, artifacts.getBody().size() );
    }


    @Test
    public void addKnowledgeAssetCarriersMultiple() {
        assertNotNull(  semanticRepository.initAsset("foo", "1", new KnowledgeAsset().withType( Care_Process_Model ) ));

        semanticRepository.setKnowledgeAssetCarrier("foo", "1", "q", "z", "there".getBytes());
        semanticRepository.setKnowledgeAssetCarrier("foo", "1", "q", "x", "there".getBytes());
        ResponseEntity<List<Pointer>> artifacts = semanticRepository.getKnowledgeAssetCarriers("foo", "1");

        assertEquals( 2, artifacts.getBody().size() );
    }

    @Test
    public void initAndGetAllCcgs() {
        assertNotNull(  semanticRepository.initAsset("1", "1",
                                                     new KnowledgeAsset().withType( KnowledgeAssetType.Operational_Concept_Defintion )
                                                                         .withSubject( new SimpleAnnotation()
                                                                                               .withRel( AssetVocabulary.DEFINES.asConcept() )
                                                                                               .withExpr( PCO.Dizziness_Present.asConcept() ) ) ) );

        assertEquals( 1, semanticRepository.listKnowledgeAssets( KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null ).getBody().size() );
    }

    @Test
    public void initAndGetAllCcgsWithMultiple() {
        assertNotNull(  semanticRepository.initAsset("1", "1",
                                                     new KnowledgeAsset().withType( KnowledgeAssetType.Operational_Concept_Defintion )
                                                                         .withSubject( new SimpleAnnotation()
                                                                                               .withRel( AssetVocabulary.DEFINES.asConcept() )
                                                                                               .withExpr( PCO.Dizziness_Present.asConcept() ) ) ) );

        assertNotNull(  semanticRepository.initAsset("2", "1",
                                                     new KnowledgeAsset().withType( KnowledgeAssetType.Operational_Concept_Defintion )
                                                                         .withSubject( new SimpleAnnotation()
                                                                                               .withRel( AssetVocabulary.DEFINES.asConcept() )
                                                                                               .withExpr( PCO.Atrial_Fibrillation_Risk_Factor_Present.asConcept() ) ) ) );

        assertEquals( 2, semanticRepository.listKnowledgeAssets(KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null).getBody().size() );
    }

    @Test
    public void initAndGetAllCcgsWithMultipleVersions() {
        assertNotNull(  semanticRepository.initAsset("1", "1",
                                                     new KnowledgeAsset().withType( KnowledgeAssetType.Operational_Concept_Defintion )
                                                                         .withSubject( new SimpleAnnotation()
                                                                                               .withRel( AssetVocabulary.DEFINES.asConcept() )
                                                                                               .withExpr( PCO.Dizziness_Present.asConcept() ) ) ) );

        assertNotNull(  semanticRepository.initAsset("1", "2",
                                                     new KnowledgeAsset().withType( KnowledgeAssetType.Operational_Concept_Defintion )
                                                                         .withSubject( new SimpleAnnotation()
                                                                                               .withRel( AssetVocabulary.DEFINES.asConcept() )
                                                                                               .withExpr( PCO.Dizziness_Present.asConcept() ) ) ) );


        assertEquals( 1, semanticRepository.listKnowledgeAssets(KnowledgeAssetType.Operational_Concept_Defintion.getTag(), null).getBody().size() );
    }

}
