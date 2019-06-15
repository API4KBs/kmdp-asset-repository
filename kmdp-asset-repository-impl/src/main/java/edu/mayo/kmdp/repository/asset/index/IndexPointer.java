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
package edu.mayo.kmdp.repository.asset.index;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class IndexPointer implements Serializable {
    private String id;
    private String version;

    public IndexPointer(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public IndexPointer(UUID id, String version) {
        this.id = id.toString();
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexPointer that = (IndexPointer) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return "IndexPointer{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}