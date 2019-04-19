package edu.mayo.mea3.repository.semantic;

import com.google.common.collect.Sets;
import edu.mayo.kmdp.common.model.SimpleAnnotation;
import org.junit.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class MapDbIndexTest {

    @Test
    public void testNuke() {
        MapDbIndex index = new MapDbIndex();

        index.registerAnnotations(new IndexPointer("1", "1"), Sets.newHashSet(new SimpleAnnotation().withExpr(
                new ConceptIdentifier().withRef(
                        URI.create("http://something")))));

        assertEquals(1, index.getAssetIdsByAnnotation("http://something").size());

        index.reset();

        assertEquals(0, index.getAssetIdsByAnnotation("http://something").size());

    }
}
