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

import org.apache.tools.ant.Project;

import ws.quokka.core.util.AnnotatedProperties;

import java.util.Set;


/**
 * RepositoryFactory provides the mechanism to obtain instances of configured repositories
 */
public interface RepositoryFactory {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Either gets or creates (and caches) a repository instance for the id specified
     * @param throwIfUndefined if true, it will throw an exception if no repository is defined with the id
     * given. Otherwise, it will return null
     */
    Repository getOrCreate(String id, boolean throwIfUndefined);

    /**
     * Registers a new type with the repository.
     * @param type the new type to register
     */
    void registerType(RepoType type);

    /**
     * Returns the type for a given id
     */
    RepoType getType(String id);

    /**
     * Returns any repositories that have been created via {@link #getOrCreate(String, boolean)}
     */
    Set getRepositories();

    /**
     * Returns any types that have been been registered via {@link #registerType(RepoType)}
     */
    Set getTypes();

    /**
     * Returns the project associated with this factory
     */
    Project getProject();

    /**
     * Returns the properties associated with this factory
     */
    AnnotatedProperties getProperties();
}
