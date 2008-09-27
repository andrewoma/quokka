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

import ws.quokka.core.plugin_spi.BuildResources;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.util.AnnotatedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class Plugin extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifact artifact;
    private String className;
    private String nameSpace;
    private List targets = new ArrayList();
    private List types = new ArrayList();
    private Plugin declaringPlugin;
    private PluginDependency dependency;
    private Map buildResources = new HashMap();
    private BuildResources localResources;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setArtifact(RepoArtifact artifact) {
        this.artifact = artifact;
    }

    public String getClassName() {
        return className;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public List getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public RepoArtifact getArtifact() {
        return artifact;
    }

    public List getTypes() {
        return Collections.unmodifiableList(types);
    }

    public void addTarget(Target target) {
        target.setPlugin(this);
        targets.add(target);
    }

    public void addType(RepoType type) {
        types.add(type);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public Plugin getDeclaringPlugin() {
        return declaringPlugin;
    }

    public void setDeclaringPlugin(Plugin declaringPlugin) {
        this.declaringPlugin = declaringPlugin;
    }

    public Target getTarget(String name) {
        for (Iterator i = targets.iterator(); i.hasNext();) {
            Target target = (Target)i.next();

            if (target.getName().equals(name)) {
                return target;
            }
        }

        return null;
    }

    public PluginDependency getDependency() {
        return dependency;
    }

    public void setDependency(PluginDependency dependency) {
        this.dependency = dependency;
    }

    public void addBuildResources(Map resources) {
        buildResources.putAll(resources);
    }

    public Map getBuildResources() {
        return Collections.unmodifiableMap(buildResources);
    }

    public BuildResources getLocalResources() {
        return localResources;
    }

    public void setLocalResources(BuildResources localResources) {
        this.localResources = localResources;
    }

    public String toShortString() {
        return (artifact == null) ? super.toShortString() : artifact.toShortString();
    }
}
