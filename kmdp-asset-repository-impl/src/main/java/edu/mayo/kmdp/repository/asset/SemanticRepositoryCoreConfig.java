package edu.mayo.kmdp.repository.asset;

import edu.mayo.kmdp.repository.artifact.ApiClient;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactApi;
import edu.mayo.kmdp.repository.artifact.KnowledgeArtifactSeriesApi;
import edu.mayo.kmdp.repository.artifact.jcr.JcrKnowledgeArtifactRepository;
import edu.mayo.kmdp.repository.asset.index.Index;
import edu.mayo.kmdp.repository.asset.index.MapDbIndex;
import java.io.File;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan
public class SemanticRepositoryCoreConfig {

    @Bean
    @Profile({"integration"})
    public SemanticRepository selfContainedRepository() {
        JcrKnowledgeArtifactRepository repos;
        try {
            repos = new JcrKnowledgeArtifactRepository(new Jcr(new Oak()).createRepository());
        } catch (IOException | InvalidFileStoreVersionException e) {
            e.printStackTrace();
            return null;
        }

        MapDbIndex index = new MapDbIndex();

        KnowledgeArtifactApi knowledgeArtifactApi = KnowledgeArtifactApi.newInstance(repos);
        KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi = KnowledgeArtifactSeriesApi.newInstance(repos);

        return new SemanticRepository(knowledgeArtifactApi, knowledgeArtifactSeriesApi, index);
    }

    @Bean
    @Profile({"default", "inmemory"})
    public SemanticRepository semanticRepository( Index index ) throws Exception {
        return new SemanticRepository(
                knowledgeArtifactApi(),
                knowledgeArtifactSeriesApi(),
                index );
    }

    @Bean
    @Profile({"default"})
    public Index fileSystemIndex() {
        File dataDir = new File(Mea3Config.getConfigDir(), "sem-repo-data");
        dataDir.mkdir();

        File indexFile = new File(dataDir, "index");

        return new MapDbIndex(indexFile);
    }

    @Bean
    @Profile({"inmemory"})
    public Index inMemoryIndex() {
        return new MapDbIndex();
    }

    @Bean
    @Profile({"default", "inmemory"})
    public KnowledgeArtifactApi knowledgeArtifactApi() {
        return KnowledgeArtifactApi.newInstance(apiClient());
    }

    @Bean
    @Profile({"default", "inmemory"})
    public KnowledgeArtifactSeriesApi knowledgeArtifactSeriesApi() {
        return KnowledgeArtifactSeriesApi.newInstance(apiClient());
    }

    @Bean
    @Profile({"default", "inmemory"})
    public ApiClient apiClient() {
        ApiClient client = new ApiClient();
        client.setBasePath(Mea3Config.getRepositoryUrl());

        return client;
    }

}
