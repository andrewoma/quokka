/*
 * Copyright 2007-2008 Andrew O'Malley
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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.Project;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.util.AnnotatedProperties;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class CachingRepository implements Repository {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Repository repository;
    private final Map cache = Collections.synchronizedMap(new HashMap());
    private Project project;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public CachingRepository(Repository repository) {
        this.repository = repository;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void initialise(Object antProject, AnnotatedProperties properties) {
        repository.initialise(antProject, properties);
        project = (Project)antProject;
    }

    public void registerType(RepoType type) {
        repository.registerType(type);
    }

    public RepoType getType(String id) {
        return repository.getType(id);
    }

    public RepoArtifact resolve(RepoArtifactId artifactId) {
        //        System.out.println("resolving: " + artifactId);
        project.log("\nResolving: " + artifactId.toShortString(), Project.MSG_DEBUG);

        synchronized (cache) {
            RepoArtifact artifact = (RepoArtifact)cache.get(artifactId);

            if (artifact != null) {
                return artifact;
            }

            artifact = repository.resolve(artifactId);
            cache.put(artifactId, artifact);
            project.log("Resolved: " + artifact, Project.MSG_DEBUG);

            //            System.out.println("resolved=" + artifact);
            return artifact;
        }
    }

    public void install(RepoArtifact artifact) {
        repository.install(artifact);
    }

    public void remove(RepoArtifactId artifactId) {
        repository.remove(artifactId);
    }

    public Collection listArtifactIds() {
        return repository.listArtifactIds();
    }

    public boolean supportsReslove(RepoArtifactId artifactId) {
        return repository.supportsReslove(artifactId);
    }

    public boolean supportsInstall(RepoArtifactId artifactId) {
        return repository.supportsInstall(artifactId);
    }

    public String getName() {
        return getClass().getName();
    }

    public Collection getReferencedRepositories() {
        return repository.getReferencedRepositories();
    }
}
