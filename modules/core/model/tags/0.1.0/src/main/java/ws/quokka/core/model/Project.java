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

import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.AnnotatedProperties;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class Project extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    // Attributes
    private String name;
    private Set artifacts = new HashSet();
    private DependencySet dependencySet = new DependencySet();
    private Profiles activeProfiles;
    private Profiles knownProfiles;
    private String defaultTarget;
    private String description;
    private AnnotatedProperties properties;
    private File projectFile;
    private Map profiles = new HashMap();
    private List overrides = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public File getProjectFile() {
        return projectFile;
    }

    public void setProjectFile(File projectFile) {
        this.projectFile = projectFile;
    }

    public Profiles getActiveProfiles() {
        return activeProfiles;
    }

    public void setActiveProfiles(Profiles activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public AnnotatedProperties getProperties() {
        return properties;
    }

    public void setProperties(AnnotatedProperties properties) {
        this.properties = properties;
    }

    public Profiles getKnownProfiles() {
        return knownProfiles;
    }

    public void setKnownProfiles(Profiles knownProfiles) {
        this.knownProfiles = knownProfiles;
    }

    public void setDependencySet(DependencySet dependencySet) {
        this.dependencySet = dependencySet;
    }

    public Set getArtifacts() {
        return Collections.unmodifiableSet(artifacts);
    }

    public void addArtifact(Artifact artifact) {
        artifacts.add(artifact);
    }

    public DependencySet getDependencySet() {
        return dependencySet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addProfile(Profile profile) {
        profiles.put(profile.getId(), profile);
    }

    public Map getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public void addOverride(Override override) {
        overrides.add(override);
    }

    public List getOverrides() {
        return Collections.unmodifiableList(overrides);
    }

    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public String getDefaultTarget() {
        return defaultTarget;
    }
}
