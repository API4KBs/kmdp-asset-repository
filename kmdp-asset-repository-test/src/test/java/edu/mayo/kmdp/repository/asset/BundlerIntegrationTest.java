package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.common.model.KnowledgeAsset;
import edu.mayo.kmdp.common.model.KnowledgeExpression;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.repository.asset.ApiClient;
import edu.mayo.kmdp.repository.asset.KnowledgeAssetCatalogApi;
import edu.mayo.kmdp.repository.asset.SemanticKnowledgeRepositoryApi;
import edu.mayo.kmdp.terms.kao.rel.dependencyreltype._2018._06.DependencyRelType;
import edu.mayo.kmdp.terms.krlanguage._2018._08.KRLanguage;
import org.junit.Assert;
import org.junit.Test;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class BundlerIntegrationTest extends IntegrationTestBase {

    private ApiClient apiClient = new ApiClient().setBasePath("http://localhost:11111");
    private KnowledgeAssetCatalogApi knowledgeAssetCatalogApi =  KnowledgeAssetCatalogApi.newInstance( apiClient );

    private SemanticKnowledgeRepositoryApi coreApi = SemanticKnowledgeRepositoryApi.newInstance( apiClient );

    @Test
    public void testBundleOnlyOne() {
        knowledgeAssetCatalogApi.initAsset("1", "2", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                    withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))));
        coreApi.addKnowledgeAssetCarrier("1", "2", "HI!".getBytes() );

        List<KnowledgeCarrier> carriers = coreApi.bundle("1", "2");

        Assert.assertEquals( 1, carriers.size() );
    }

    @Test
    public void testBundleWithDependency() {
        edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka = knowledgeAssetCatalogApi.initAsset("a", "b", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                    withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))).
                    withResourceId(DatatypeHelper.uri("http:/some/uri/","a", "b")));

        knowledgeAssetCatalogApi.initAsset("1", "2", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                    withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))).
                    withResourceId(DatatypeHelper.uri("http:/some/uri/", "1", "2")).
                    withRelated(new Dependency().withRel(DependencyRelType.Imports).withTgt(ka)));

        coreApi.addKnowledgeAssetCarrier("a", "b", "Hi!".getBytes());
        coreApi.addKnowledgeAssetCarrier("1", "2", "There!".getBytes());

	    List<KnowledgeCarrier> carriers = coreApi.bundle("1", "2");

	    Assert.assertEquals(2, carriers.size());
	    List<String> strings = carriers.stream().map(BinaryCarrier.class::cast).map(BinaryCarrier::getEncodedExpression).map(String::new).collect(Collectors.toList());

	    assertTrue( strings.contains( "Hi!") );
	    assertTrue( strings.contains( "There!") );
    }

    @Test
    public void testBundleWithDependencyThreeDeep() {
	    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka1 = knowledgeAssetCatalogApi.initAsset("a", "b", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                        withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))).
                withResourceId(DatatypeHelper.uri("http:/some/uri/", "a","b")));

	    edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset ka2 = knowledgeAssetCatalogApi.initAsset("q", "r", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                        withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))).
                withResourceId(DatatypeHelper.uri("http:/some/uri/","q", "r")).
                withRelated(
                        new Dependency().withRel(DependencyRelType.Imports).withTgt(ka1)));

        knowledgeAssetCatalogApi.initAsset("1", "2", new KnowledgeAsset().
                withExpression(new KnowledgeExpression().
                        withRepresentation(new Representation().withLanguage(KRLanguage.ELM_1_2))).
                withResourceId(DatatypeHelper.uri("http:/some/uri/","1", "2")).
                withRelated(
                        new Dependency().withRel(DependencyRelType.Imports).withTgt(ka2)));

        coreApi.addKnowledgeAssetCarrier("a", "b", "Hi!".getBytes());
        coreApi.addKnowledgeAssetCarrier("1", "2", "There!".getBytes());
        coreApi.addKnowledgeAssetCarrier("q", "r", "Zebra!".getBytes());

	    List<KnowledgeCarrier> carriers = coreApi.bundle("1", "2");

        Assert.assertEquals( 3, carriers.size());
        List<String> strings = carriers.stream().map(BinaryCarrier.class::cast).map( BinaryCarrier::getEncodedExpression).map( String::new ).collect(Collectors.toList());

        assertTrue( strings.contains( "Hi!") );
        assertTrue( strings.contains( "There!") );
        assertTrue( strings.contains( "Zebra!") );
    }

}
