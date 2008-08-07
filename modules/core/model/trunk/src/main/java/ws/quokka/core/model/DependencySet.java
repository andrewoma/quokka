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


package ws.quokka.core.model;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.AnnotatedProperties;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class DependencySet extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifact artifact; // may be null if belongs to a project
    private List dependencies = new ArrayList();
    private List subsets = new ArrayList();
    private Map paths = new HashMap();
    private DependencySet parent;
    private URL importURL;
    private AnnotatedProperties properties = new AnnotatedProperties();
    private boolean inheritProperties = false;
    private boolean inheritResources = false;
    private List overrides = new ArrayList();
    private Map buildResources = new HashMap();
    private List profiles = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public RepoArtifact getArtifact() {
        return artifact;
    }

    public void setArtifact(RepoArtifact artifact) {
        this.artifact = artifact;
    }

    public boolean isInheritProperties() {
        return inheritProperties;
    }

    public void setInheritProperties(boolean inheritProperties) {
        this.inheritProperties = inheritProperties;
    }

    public boolean isInheritResources() {
        return inheritResources;
    }

    public void setInheritResources(boolean inheritResources) {
        this.inheritResources = inheritResources;
    }

    public List getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public List getOverrides() {
        return Collections.unmodifiableList(overrides);
    }

    public void addOverride(Override override) {
        overrides.add(override);
    }

    public DependencySet getParent() {
        return parent;
    }

    public void setParent(DependencySet parent) {
        this.parent = parent;
    }

    public void addSubset(DependencySet dependencySet) {
        subsets.add(dependencySet);
    }

    public List getSubsets() {
        return Collections.unmodifiableList(subsets);
    }

    public AnnotatedProperties getProperties() {
        return properties;
    }

    public void setProperties(AnnotatedProperties properties) {
        this.properties = properties;
    }

    public void addBuildResources(Map resources) {
        buildResources.putAll(resources);
    }

    public Map getBuildResources() {
        return Collections.unmodifiableMap(buildResources);
    }

    public Map getPaths() {
        return Collections.unmodifiableMap(paths);
    }

    public void addPath(Path path) {
        if (paths.containsKey(path.getId())) {
            throw new RuntimeException("A path of id '" + path.getId() + "' is already defined in the project");
        }

        paths.put(path.getId(), path);
    }

    public URL getImportURL() {
        return importURL;
    }

    public void setImportURL(URL importURL) {
        this.importURL = importURL;
    }

    public String toShortString() {
        return (artifact == null) ? super.toString() : artifact.toShortString();
    }

    public void addProfile(Profile profile) {
        profiles.add(profile);
    }

    public List getProfiles() {
        return Collections.unmodifiableList(profiles);
    }
}
