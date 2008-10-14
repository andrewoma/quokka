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


package ws.quokka.core.repo_standard;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryFactory;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * RepositoryFactoryImpl creates and caches repository instances
 */
public class RepositoryFactoryImpl implements RepositoryFactory {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String prefix = "q.repo.";
    private AnnotatedProperties properties;
    private Map classes = new HashMap();
    private Map repositories = new HashMap();
    private Project project;
    private Map types = new HashMap();
    private String repositoryVersion;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepositoryFactoryImpl() {
        // Add aliases for known implementations
        classes.put("file", FileRepository.class.getName());
        classes.put("url", UrlRepository.class.getName());
        classes.put("checksum", ChecksumRepository.class.getName());
        classes.put("delegate", DelegatingRepository.class.getName());
        classes.put("bundle", BundledRepository.class.getName());
        classes.put("indexed", IndexedRepository.class.getName());
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setProperties(AnnotatedProperties properties) {
        this.properties = properties;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public AnnotatedProperties getProperties() {
        return properties;
    }

    public void setRepositoryVersion(String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }

    public synchronized Repository getOrCreate(String id, boolean throwIfUndefined) {
        // Treat 'shared' as a special case and try looking for a versioned shared repository
        // This allows multiple incompatible repositories to be defined in the user's properties
        // and for the correct version to be selected automatically.
        if (id.equals("shared") && (repositoryVersion != null)) {
            String newId = "shared-" + repositoryVersion;
            Repository repository = _getOrCreate(newId);

            if (repository != null) {
                return repository;
            }
        }

        Repository repository = _getOrCreate(id);
        Assert.isTrue(!throwIfUndefined || (repository != null), "Repository with id '" + id + "' has not been defined");

        return repository;
    }

    private Repository _getOrCreate(String id) {
        Repository repository = (Repository)repositories.get(id);

        if (repository != null) {
            return repository;
        }

        String url = properties.getProperty(prefix + id + ".url");

        if ((url == null) && (getClassName(id) == null)) {
            return null; // no repository defined with this id
        }

        if (url != null) {
            properties.putAll(parseUrl(prefix + id + ".", url)); // Convert url format to ordinary properties
        }

        String className = getClassName(id);

        if (classes.containsKey(className)) {
            className = (String)classes.get(className);
        }

        Assert.isTrue(className != null, "Class name must be specified for repository '" + id + "'");
        repository = createInstance(id, className);
        repositories.put(id, repository); // Add before initalising? Allow circular?
        repository.initialise();

        return repository;
    }

    private String getClassName(String id) {
        return properties.getProperty(prefix + id + ".class");
    }

    protected Repository createInstance(String id, String className) {
        try {
            Repository repository = (Repository)Class.forName(className).newInstance();
            repository.setName(id);
            repository.setFactory(this);

            return repository;
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Converts a repository url into a set of properties:
     * <pre>
     * q.repo.snapshot.url=file:${user.home}/.quokka/snapshots;snapshots=true
     * q.repo.release.url=file:${user.home}/.quokka/releases;parents=global;confirmImport=false
     * q.repo.global.url=url:http://quokka.ws/repository/
     * </pre>
     */
    public static Properties parseUrl(String prefix, String url) {
        Properties properties = new Properties();
        String message = "Invalid repository url. Should be in format 'class:root;key1=value1;key2=value2': " + url;
        int index = url.indexOf(":");
        properties.setProperty(prefix + "class", url.substring(0, (index == -1) ? url.length() : index));

        if (index != -1) {
            String[] tokens = Strings.trim(Strings.split(url.substring(index + 1), ";"));
            Assert.isTrue(tokens.length > 0, message);
            properties.put(prefix + "root", tokens[0]);

            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i];
                index = token.indexOf("=");
                Assert.isTrue(index != -1, message);
                properties.setProperty(prefix + token.substring(0, index).trim(), token.substring(index + 1));
            }
        }

        return properties;
    }

    public void registerType(RepoType type) {
        types.put(type.getId(), type);
    }

    public RepoType getType(String id) {
        RepoType type = (RepoType)types.get(id);
        Assert.isTrue(type != null, "Type has not been registered with the repository: " + id);

        return type;
    }

    public Set getTypes() {
        return Collections.unmodifiableSet(new HashSet(types.values()));
    }

    public Set getRepositories() {
        return Collections.unmodifiableSet(new HashSet(repositories.values()));
    }

    public RepositoryFactoryImpl copy() {
        RepositoryFactoryImpl copy = new RepositoryFactoryImpl();
        copy.setProject(project);
        copy.setProperties(properties);
        copy.types = types;
        copy.repositoryVersion = repositoryVersion;
        copy.repositories = repositories;

        return copy;
    }
}
