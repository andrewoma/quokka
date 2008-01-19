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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.AnnotatedProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class Target extends AnnotatedObject implements Cloneable {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Plugin plugin;
    private String name;
    private String templateName;
    private String description;
    private List dependencies = new ArrayList();
    private List originalDependencies = new ArrayList();
    private String implementsPlugin;
    private List pathGroups = new ArrayList();
    private List projectPaths = new ArrayList();
    private boolean isAbstract;
    private boolean template;
    private AnnotatedProperties defaultProperties = new AnnotatedProperties();
    private String prefix;
    private boolean enabledByDefault = true;
    private boolean implemented = false;
    private String alias;
    private boolean main = false;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getOriginalDependencies() {
        return Collections.unmodifiableList(originalDependencies);
    }

    public void addOriginalDependency(String dependencies) {
        originalDependencies.add(dependencies);
    }

    public boolean isImplemented() {
        return implemented;
    }

    public void setImplemented(boolean implemented) {
        this.implemented = implemented;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public String getImplementsPlugin() {
        return implementsPlugin;
    }

    public void setImplementsPlugin(String implementsPlugin) {
        this.implementsPlugin = implementsPlugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public List getPathGroups() {
        return Collections.unmodifiableList(pathGroups);
    }

    public void addPathGroup(PathGroup pathGroup) {
        this.pathGroups.add(pathGroup);
    }

    public List getProjectPaths() {
        return Collections.unmodifiableList(projectPaths);
    }

    public void addProjectPath(Path path) {
        this.projectPaths.add(path);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public PathGroup getPathGroup(String id) {
        for (Iterator i = pathGroups.iterator(); i.hasNext();) {
            PathGroup pathGroup = (PathGroup)i.next();

            if (pathGroup.getId().equals(id)) {
                return pathGroup;
            }
        }

        return null;
    }

    public AnnotatedProperties getDefaultProperties() {
        return defaultProperties;
    }

    public void setDefaultProperties(AnnotatedProperties defaultProperties) {
        this.defaultProperties = defaultProperties;
    }

    public String toShortString() {
        return name;
    }

    public Object clone() {
        try {
            Target clone = (Target)super.clone();
            clone.defaultProperties = (AnnotatedProperties)defaultProperties.clone();
            clone.dependencies = (List)((ArrayList)dependencies).clone();

            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(); //

            return null;
        }
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(boolean template) {
        this.template = template;
    }

    public void merge(Target template) {
        Assert.isTrue(template.isTemplate(), getLocator(),
            "Attempt to merge target against as non-template: target=" + name + ", template=" + template.getName());

        for (Iterator i = template.getDependencies().iterator(); i.hasNext();) {
            String dependency = (String)i.next();
            dependencies.add(dependency);
        }

        if (template.getImplementsPlugin() != null) {
            Assert.isTrue(implementsPlugin == null, getLocator(),
                "Attempt to override implements plugin from template: target=" + name);
            implementsPlugin = template.getImplementsPlugin();
        }

        // this targets properties override templates
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.putAll(template.getDefaultProperties());
        properties.putAll(defaultProperties);
        defaultProperties = properties;

        // this targets path groups override templates
        for (Iterator i = template.getPathGroups().iterator(); i.hasNext();) {
            PathGroup pathGroup = (PathGroup)i.next();

            if (getPathGroup(pathGroup.getId()) == null) {
                pathGroups.add(pathGroup);
            }
        }

        for (Iterator i = template.getProjectPaths().iterator(); i.hasNext();) {
            Path path = (Path)i.next();
            projectPaths.add(path);
        }
    }

    public void clearDependencies() {
        dependencies.clear();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isMain() {
        return main;
    }
}
