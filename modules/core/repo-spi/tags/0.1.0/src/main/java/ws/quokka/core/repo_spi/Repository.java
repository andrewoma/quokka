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


package ws.quokka.core.repo_spi;

import ws.quokka.core.util.AnnotatedProperties;

import java.util.Collection;


/**
 *
 */
public interface Repository {
    //~ Methods --------------------------------------------------------------------------------------------------------

    void initialise(Object antProject, AnnotatedProperties properties);

    void registerType(RepoType type);

    RepoType getType(String id);

    RepoArtifact resolve(RepoArtifactId artifactId);

    void install(RepoArtifact artifact);

    void remove(RepoArtifactId artifactId);

    Collection listArtifactIds();

    boolean supportsReslove(RepoArtifactId artifactId);

    boolean supportsInstall(RepoArtifactId artifactId);

    String getName();

    /**
     * Returns any repositories used by this repository, for example, when delegating to
     * other repositories internally. Implementations should not to recursively process
     * references, just return repositories directly referenced by this repository.
     */
    Collection getReferencedRepositories();
}
