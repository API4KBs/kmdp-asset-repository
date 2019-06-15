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
package edu.mayo.kmdp.repository.asset;


import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import edu.mayo.kmdp.metadata.annotations.resources.SimpleAnnotation;
import edu.mayo.kmdp.repository.asset.index.IndexPointer;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;

public class MapDbIndexTest {

  @Test
  public void testNuke() {
    MapDbIndex index = new MapDbIndex();

    index.registerAnnotations(new IndexPointer("1", "1"),
        Sets.newHashSet(new SimpleAnnotation().withExpr(
            new ConceptIdentifier().withRef(
                URI.create("http://something")))));

    assertEquals(1, index.getAssetIdsByAnnotation("http://something").size());

    index.reset();

    assertEquals(0, index.getAssetIdsByAnnotation("http://something").size());

  }
}
