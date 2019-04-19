package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.common.model.KnowledgeAsset;
import edu.mayo.kmdp.repository.asset.ApiClient;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.terms.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import org.junit.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;

import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class SemanticRepositoryIntegrationTest extends IntegrationTestBase {

    @Test
    public void testListKnowledgeAssetsType() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.addAsset(new KnowledgeAsset().withType(KnowledgeAssetType.Care_Process_Model));

        List<Pointer> pointers = client.listKnowledgeAssets(KnowledgeAssetType.Care_Process_Model.getTag(), null);

        assertEquals(1, pointers.size());
    }

    @Test
    public void testListKnowledgeAssetsBadType() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.addAsset(new KnowledgeAsset().withType(KnowledgeAssetType.Care_Process_Model));

        List<Pointer> pointers = client.listKnowledgeAssets("ClinicalRule", null);

        assertEquals(0, pointers.size());
    }

    @Test
    public void testListKnowledgeAssetsNoType() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.addAsset(new KnowledgeAsset().withType(KnowledgeAssetType.Care_Process_Model));
        client.addAsset(new KnowledgeAsset().withType(KnowledgeAssetType.Clinical_Rule));

        List<Pointer> pointers = client.listKnowledgeAssets(null, null);

        assertEquals(2, pointers.size());
    }

    @Test
    public void testListKnowledgeAssetsNoTypeWithNone() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        List<Pointer> pointers = client.listKnowledgeAssets(null, null);

        assertEquals(0, pointers.size());
    }

    @Test
    public void testGeKnowledgeAssetsVersions() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.initAsset("1", "1", new KnowledgeAsset());
        client.initAsset("1", "2", new KnowledgeAsset());

        List<Pointer> pointers = client.listKnowledgeAssets(null, null);

        assertEquals(1, pointers.size());
    }

    @Test
    public void testGetLatestKnowledgeAsset() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.initAsset("1", "1", new KnowledgeAsset());
        client.initAsset("1", "2", new KnowledgeAsset());

        assertNotNull( client.getKnowledgeAsset("1") );
    }

    @Test
    public void testGetLatestKnowledgeAssetHasCorrectId() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        client.initAsset("1", "1", new KnowledgeAsset());
        client.initAsset("1", "2", new KnowledgeAsset());

        assertEquals( "1", client.getKnowledgeAsset("1").getResourceId().getTag() );
    }

    @Test
    public void testInitAsset() {
        KnowledgeAssetCatalogApi client = KnowledgeAssetCatalogApi.newInstance(new ApiClient().setBasePath("http://localhost:11111"));

        edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset pointer = client.initAsset("1", "1", new KnowledgeAsset());

        assertNotNull( pointer );
    }

}
