package edu.mayo.kmdp.repository.asset.index;

import java.io.Serializable;
import java.util.Objects;

public class IndexPointer implements Serializable {
    private String id;
    private String version;

    public IndexPointer(String id, String version) {
        this.id = id;
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