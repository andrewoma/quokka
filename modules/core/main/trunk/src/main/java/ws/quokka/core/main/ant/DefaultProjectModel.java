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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;

import org.xml.sax.Locator;

import ws.quokka.core.bootstrap.BootStrapper;
import ws.quokka.core.bootstrap.resources.DependencyResource;
import ws.quokka.core.bootstrap_util.ArtifactPropertiesParser;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Logger;
import ws.quokka.core.bootstrap_util.ProfilesMatcher;
import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.main.parser.PluginParser;
import ws.quokka.core.metadata.Metadata;
import ws.quokka.core.metadata.MetadataAware;
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.ModelFactory;
import ws.quokka.core.model.ModelFactoryAware;
import ws.quokka.core.model.Override;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathGroup;
import ws.quokka.core.model.Plugin;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.PluginDependencyTarget;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.model.Project;
import ws.quokka.core.model.ProjectModel;
import ws.quokka.core.model.Target;
import ws.quokka.core.plugin_spi.BuildResources;
import ws.quokka.core.plugin_spi.PluginState;
import ws.quokka.core.plugin_spi.ResourcesAware;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.repo_spi.RepoPathSpec;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryAware;
import ws.quokka.core.util.*;
import ws.quokka.core.version.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;


/**
 *
 */
public class DefaultProjectModel implements ProjectModel {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;
    private Profiles profiles;
    private ModelFactory modelFactory;
    private List aliases = new ArrayList();
    private Map coreClassPath = new HashMap();
    private BootStrapper bootStrapper;
    private Logger log;

    // Attributes
    private Map resolvedPaths = new HashMap();

    // Helpers
    private Map pluginCache = new HashMap();
    private org.apache.tools.ant.Project antProject;
    private Repository repository;
    private PluginParser pluginParser;

    // Derived attributes after initialisation
    private Map resolvedTargets = new HashMap();
    private DefaultBuildResources buildResources = new DefaultBuildResources();
    private List resolvedImports = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setBootStrapper(BootStrapper bootStrapper) {
        this.bootStrapper = bootStrapper;
    }

    public ModelFactory getModelFactory() {
        return modelFactory;
    }

    public void setModel(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public Profiles getProfiles() {
        return profiles;
    }

    public void setProfiles(Profiles profiles) {
        this.profiles = profiles;
    }

    public BootStrapper getBootStrapper() {
        return bootStrapper;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setPluginParser(PluginParser pluginParser) {
        this.pluginParser = pluginParser;
    }

    private void resolveTargets(Map resolved, PluginDependency pluginDependency) {
        List dependencyTargets = new ArrayList(pluginDependency.getTargets());
        Plugin plugin = getPluginInstance(pluginDependency.getId());
        plugin.setDependency(pluginDependency);

        for (Iterator i = plugin.getTargets().iterator(); i.hasNext();) {
            Target target = (Target)i.next();

            // If the target is based on a template internally, merge with the template
            if (target.getTemplateName() != null) {
                String template = target.getTemplateName();
                template = (template.indexOf(":") != -1) ? template : (
                        target.getPlugin().getNameSpace() + ":" + template
                    );
                target.merge(plugin.getTarget(template));
            }

            // Process explicit target definitions
            boolean added = false;

            for (Iterator j = dependencyTargets.iterator(); j.hasNext();) {
                PluginDependencyTarget dependencyTarget = (PluginDependencyTarget)j.next();
                Target targetInstance = null;

                // Test of enabling a target
                if (dependencyTarget.getTemplate() == null) {
                    String name = dependencyTarget.getName();
                    name = (name.indexOf(":") != -1) ? name : (target.getPlugin().getNameSpace() + ":" + name);

                    if (target.getName().equals(name)) {
                        Assert.isTrue(!target.isTemplate(), dependencyTarget.getLocator(),
                            "The named target '" + name + "' is a template");

                        if (dependencyTarget.getAlias() != null) {
                            target.setAlias(dependencyTarget.getAlias());
                        }

                        targetInstance = target;
                    }
                } else {
                    // Test instantiation of a template
                    String template = dependencyTarget.getTemplate();
                    template = (template.indexOf(":") != -1) ? template
                                                             : (target.getPlugin().getNameSpace() + ":" + template);

                    if (target.getName().equals(template)) {
                        Assert.isTrue(target.isTemplate(), dependencyTarget.getLocator(),
                            "The named target '" + template + "' is not a template");
                        targetInstance = (Target)target.clone();
                        targetInstance.setPrefix(dependencyTarget.getPrefix());
                        targetInstance.setTemplateName(target.getName());
                        targetInstance.setName(dependencyTarget.getName());
                    }
                }

                if (targetInstance != null) {
                    added = true;

                    for (Iterator k = dependencyTarget.getDependencies().iterator(); k.hasNext();) {
                        String dependency = (String)k.next();
                        targetInstance.addDependency(dependency);
                    }

                    addTarget(resolved, targetInstance);
                    j.remove();
                }
            }

            // Use defaults
            if (!added && pluginDependency.isUseDefaults() && target.isEnabledByDefault()) {
                addTarget(resolved, target);
            }
        }

        if (dependencyTargets.size() != 0) {
            List names = new ArrayList();

            for (Iterator i = dependencyTargets.iterator(); i.hasNext();) {
                PluginDependencyTarget target = (PluginDependencyTarget)i.next();
                names.add(target.getName());
            }

            Assert.isTrue(false, pluginDependency.getLocator(),
                "The following targets are not defined in plugin '" + plugin.getArtifact().getId().toShortString()
                + "': " + Strings.join(names.iterator(), ","));
        }
    }

    private void addTarget(Map resolved, Target target) {
        //        if (target.getPlugin().getNameSpace().equals("quokka.devreport")) {
        //            System.out.println("");
        //        }
        //        System.out.println("Adding " + target.getName() + " from " + parents);
        if (target.getPrefix() != null) {
            target.setDefaultProperties(expandPrefix(target.getPrefix(), target.getDefaultProperties()));
        }

        Target existing = (Target)resolved.get(target.getName());

        if (existing != null) {
            RepoArtifactId targetId = target.getPlugin().getArtifact().getId();
            RepoArtifactId existingId = existing.getPlugin().getArtifact().getId();
            Assert.isTrue(targetId.equals(existingId), target.getLocator(),
                "Multiple targets are defined with the name '" + target.getName() + "'. Declared in " + targetId
                + " and " + existingId);

            return;
        }

        resolved.put(target.getName(), target);
        registerTypes(target);
        registerProjectPaths(target);
        buildResources.putAll(target.getPlugin().getBuildResources());

        RepoArtifact targetPlugin = target.getPlugin().getArtifact();

        // PluginDependency declares the target
        addDependentTargets(resolved, target);

        if (target.getImplementsPlugin() != null) {
            // PluginDependency implements the target declared in another plugin
            String[] implementsPlugin = parseImplements(target);
            RepoArtifactId declaringPluginId = findMatchingDependency(targetPlugin,
                    new RepoArtifactId(implementsPlugin[0], implementsPlugin[1], "jar", (Version)null));

            Plugin declaringPlugin = getPluginInstance(declaringPluginId);
            String declaringTargetName = declaringPlugin.getNameSpace() + ":" + implementsPlugin[2];

            // Get the declaring plugin and find the matching target
            Target declaringTarget = (Target)resolved.get(declaringTargetName);

            if (declaringTarget == null) {
                declaringTarget = declaringPlugin.getTarget(declaringTargetName);
                Assert.isTrue(declaringTarget != null, target.getLocator(),
                    "'" + declaringTargetName + "' is not defined in '" + declaringPluginId.toShortString() + "'");
                addTarget(resolved, declaringTarget);
            }

            Assert.isTrue(declaringTarget.isAbstract(), target.getLocator(),
                "Target is attempting to implement a non-abstract target: target=" + target.getName() + ", implements="
                + declaringTarget.getName());
            target.getPlugin().setDeclaringPlugin(declaringTarget.getPlugin());

            if (!declaringTarget.isImplemented()) {
                declaringTarget.setImplemented(true);
                declaringTarget.clearDependencies();
            }

            declaringTarget.addDependency(target.getName());

            // Add the declaring targets dependencies to ensure the implementation is executed before them
            for (Iterator i = declaringTarget.getOriginalDependencies().iterator(); i.hasNext();) {
                String dependency = (String)i.next();
                target.addDependency(dependency);
            }
        }
    }

    private AnnotatedProperties expandPrefix(final String prefix, AnnotatedProperties properties) {
        return properties.replaceReferences(new PropertyProvider() {
                public String getProperty(String key) {
                    int index = key.startsWith("[") ? (key.indexOf("]") + 1) : 0;
                    String prefixMarker = "prefix.";

                    if (key.indexOf(prefixMarker, index) != -1) {
                        key = (index == 0) ? (prefix + "." + key.substring(prefixMarker.length()))
                                           : (
                                key.substring(0, index) + prefix + "." + key.substring(index + prefixMarker.length())
                            );
                    }

                    return key;
                }
            });
    }

    private String[] parseImplements(Target target) {
        // TODO: parse during plugin parsing ... add them properly to target
        String[] implementsPlugin = Strings.trim(Strings.split(target.getImplementsPlugin(), ":"));

        if ((implementsPlugin == null) || (implementsPlugin.length != 3)) {
            throw new BuildException(
                "'implements' attribute of 'target' element is not in 'group:name:target' format. plugin="
                + target.getPlugin().getArtifact().getId() + ", target=" + target.getName() + ", value="
                + target.getImplementsPlugin());
        }

        return implementsPlugin;
    }

    private void addDependentTargets(Map resolved, Target target) {
        for (Iterator i = target.getDependencies().iterator(); i.hasNext();) {
            String dependency = (String)i.next();

            for (Iterator j = target.getPlugin().getTargets().iterator(); j.hasNext();) {
                Target siblingTarget = (Target)j.next();

                if (siblingTarget.getName().equals(dependency)) {
                    addTarget(resolved, siblingTarget);
                }
            }
        }
    }

    // TODO: make it search only paths that are valid for this target
    private RepoArtifactId findMatchingDependency(RepoArtifact targetArtifact, RepoArtifactId id) {
        for (Iterator i = targetArtifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            if (dependency.getId().matches(id)) {
                return dependency.getId();
            }
        }

        throw new BuildException(targetArtifact.getId() + " does not declare a dependency that matches " + id);
    }

    private Plugin getPluginInstance(RepoArtifactId id) {
        Plugin plugin = (Plugin)pluginCache.get(id);

        if (plugin == null) {
            plugin = pluginParser.getPluginInstance(getArtifact(id));
            pluginCache.put(id, plugin);
        }

        return plugin;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void initialise() {
        // Add ant-types path
        project.getDependencySet().addPath(new Path("ant-types",
                "Any entires added to this path are automatically added to the ant type definition class loader. "
                + "e.g. Add commons.net and ant's optional commons.net library for using the ftp task"));

        // Get dependency sets, applying profiles and overrides
        List sets = new ArrayList();
        depthFirst(sets, project.getDependencySet());

        // 1st pass: Define targets, paths, imported URLs
        Map targets = new HashMap();

        for (Iterator i = sets.iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();

            // Get targets
            for (Iterator j = set.getDependencies().iterator(); j.hasNext();) {
                Dependency dependency = (Dependency)j.next();

                if (dependency instanceof PluginDependency) {
                    resolveTargets(targets, (PluginDependency)dependency);
                }
            }

            // Get paths
            for (Iterator j = set.getPaths().values().iterator(); j.hasNext();) {
                addPath((Path)j.next());
            }

            if (set.getImportURL() != null) {
                resolvedImports.add(set.getImportURL());
            }
        }

        // 2nd pass: Define default paths specs, add imported targets
        for (Iterator i = sets.iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();

            // Fill in path spec defaults now that the project paths have been defined
            addPathSpecDefaults(set);
        }

        // 3rd pass: Reverse order for build resources
        List reverse = new ArrayList(sets);
        Collections.reverse(reverse);

        for (Iterator i = reverse.iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();
            buildResources.putAll(set.getBuildResources()); // Parent overrides children
        }

        resolvedTargets = targets;

        List modules = getCoreModules();

        for (Iterator i = modules.iterator(); i.hasNext();) {
            addBootClassPathModule((RepoArtifactId)i.next());
        }
    }

    private void addBootClassPathModule(RepoArtifactId id) {
        RepoArtifactId key = toUnversionedId(id);
        coreClassPath.put(key, id);
    }

    private RepoArtifactId toUnversionedId(RepoArtifactId id) {
        return new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), Version.parse("0"));
    }

    private Version getBootClassPathVersion(RepoArtifactId id) {
        RepoArtifactId bootClassPathId = (RepoArtifactId)coreClassPath.get(toUnversionedId(id));

        return (bootClassPathId == null) ? null : bootClassPathId.getVersion();
    }

    private void depthFirst(List list, DependencySet dependencySet) {
        list.add(dependencySet);

        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            DependencySet subset = (DependencySet)i.next();
            depthFirst(list, subset);
        }
    }

    private void addPath(Path path) {
        resolvedPaths.put(path.getId(), path);
    }

    private void addPathSpecDefaults(DependencySet dependencySet) {
        for (Iterator i = dependencySet.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)j.next();

                if (dependency instanceof PluginDependency) {
                    // TODO: use defaults for plugin dependencies (from path defined in the repository?)
                } else {
                    RepoPath path = (RepoPath)resolvedPaths.get(pathSpec.getTo());
                    Assert.isTrue(path != null, pathSpec.getLocator(),
                        "The 'to' path '" + pathSpec.getTo() + "' is not defined in the project");
                    pathSpec.mergeDefaults(path);
                }
            }
        }
    }

    public Plugin getPlugin(RepoArtifactId id) {
        for (Iterator i = resolvedTargets.values().iterator(); i.hasNext();) {
            List pluginTargets = (List)i.next();

            for (Iterator j = pluginTargets.iterator(); j.hasNext();) {
                Target target = (Target)j.next();

                if (target.getPlugin().getArtifact().getId().equals(id)) {
                    return target.getPlugin();
                }
            }
        }

        return null;
    }

    private void registerProjectPaths(Target target) {
        for (Iterator i = target.getProjectPaths().iterator(); i.hasNext();) {
            addPath((Path)i.next());
        }
    }

    private void registerTypes(Target target) {
        // TODO: prevent registration of duplicates?
        for (Iterator i = target.getPlugin().getTypes().iterator(); i.hasNext();) {
            repository.registerType((RepoType)i.next());
        }
    }

    public Map getTargets() {
        return resolvedTargets;
    }

    /**
     * Returns a set of properties for the project. The precedence is:
     * quokka.properties
     * quokka.properties of inherited projects
     * plugin.properties
     * No expansion or ordering of properties is done
     */
    public AnnotatedProperties getProperties() {
        AnnotatedProperties resolvedProperties = new AnnotatedProperties();

        // Add the global defaults
        resolvedProperties.put("quokka.project.targetDir", "${basedir}/target");
        resolvedProperties.put("quokka.project.sourceDir", "${basedir}/src");
        resolvedProperties.put("quokka.project.resourcesDir", "${basedir}/resources");

        // Add artifact related properties
        if (project.getArtifacts().size() > 0) {
            // Add properties common to all (group & version)
            RepoArtifactId artifactId = ((Artifact)getProject().getArtifacts().iterator().next()).getId();
            resolvedProperties.put("quokka.project.artifact.group", artifactId.getGroup());
            resolvedProperties.put("quokka.project.artifact.version", artifactId.getVersion().toString());

            // Build up a list of names by type
            Map namesByType = new HashMap();

            for (Iterator i = project.getArtifacts().iterator(); i.hasNext();) {
                Artifact artifact = (Artifact)i.next();
                artifactId = artifact.getId();

                List names = (List)namesByType.get(artifactId.getType());

                if (names == null) {
                    names = new ArrayList();
                    namesByType.put(artifactId.getType(), names);
                }

                names.add(artifactId.getName());
            }

            // Output the names
            for (Iterator i = namesByType.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                List names = (List)entry.getValue();
                resolvedProperties.put("quokka.project.artifact.name[" + entry.getKey() + "]",
                    Strings.join(names.iterator(), ","));
            }
        }

        // Put the plugin properties in first. Order is not important as plugin properties should be
        // unique to their plugin
        for (Iterator i = resolvedTargets.values().iterator(); i.hasNext();) {
            Target target = (Target)i.next();
            AnnotatedProperties targetProperties = target.getDefaultProperties();
            resolvedProperties.putAll(applyProfiles(targetProperties, project.getActiveProfiles().getElements()));
        }

        // Put in any properties defined in dependency sets (processed in reverse to ensure high levels override low levels)
        resolveProperties(resolvedProperties, project.getDependencySet());

        resolvedProperties.putAll(applyProfiles(project.getProperties(), project.getActiveProfiles().getElements()));

        // Put the project paths as properties
        for (Iterator i = resolvedPaths.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            resolvedProperties.put("quokka.project.path." + entry.getKey(),
                toAntPath(getProjectPath((String)entry.getKey(), false, true)).toString());
        }

        return resolvedProperties;
    }

    public org.apache.tools.ant.types.Path toAntPath(List artifacts) {
        org.apache.tools.ant.types.Path path = new org.apache.tools.ant.types.Path(antProject);

        for (Iterator i = artifacts.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            path.add(new org.apache.tools.ant.types.Path(antProject, artifact.getLocalCopy().getAbsolutePath()));
        }

        return path;
    }

    private void resolveProperties(AnnotatedProperties properties, DependencySet dependencySet) {
        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            resolveProperties(properties, (DependencySet)i.next());
        }

        if (dependencySet.getProperties() != null) {
            properties.putAll(applyProfiles(dependencySet.getProperties(), project.getActiveProfiles().getElements()));
        }
    }

    public List resolvePath(Target target, String pathGroupId, boolean flatten) {
        Plugin plugin = target.getPlugin();
        List path = new ArrayList();
        PathGroup pathGroup = target.getPathGroup(pathGroupId);

        if (pathGroup == null) {
            if (plugin.getDeclaringPlugin() != null) {
                String declaringTargetName = plugin.getDeclaringPlugin().getNameSpace() + ":"
                    + parseImplements(target)[2];
                pathGroup = plugin.getDeclaringPlugin().getTarget(declaringTargetName).getPathGroup(pathGroupId);

                // TODO: Check the path group only refers to project paths?
            }

            Assert.isTrue(pathGroup != null,
                "Target '" + target.getName() + "' has requested path group '" + pathGroupId + "' that does not exist");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolving path: target=" + target.getName() + ", pathGroup=" + pathGroupId
                + ", pathGroupElements=" + pathGroup.getPaths());
        }

        resolvePath(path, pathGroup.getPaths(), pathGroup.getMergeWithCore().booleanValue(), target, flatten);

        StringBuffer sb = new StringBuffer();

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            sb.append(artifact.getId().toShortString()).append(";");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved path: target=" + target.getName() + ", pathGroup=" + pathGroupId + ", path="
                + sb.toString());
        }

        return path;
    }

    private void resolvePath(List path, List pathIds, boolean mergeWithCore, Target target, boolean flatten) {
        String projectPrefix = "project.";
        String pluginPrefix = "plugin.";
        String propertyPrefix = "property.";

        for (Iterator i = pathIds.iterator(); i.hasNext();) {
            String id = (String)i.next();

            if (id.startsWith(projectPrefix)) {
                String projectPathId = id.substring(projectPrefix.length());
                Assert.isTrue(resolvedPaths.get(projectPathId) != null,
                    "Project path '" + projectPathId + "' is not defined");
                path.addAll(getProjectPath(projectPathId, mergeWithCore, flatten));
            } else if (id.startsWith(pluginPrefix)) {
                String pluginPathId = id.substring(pluginPrefix.length());
                resolvePluginPath(path, target.getPlugin(), pluginPathId, mergeWithCore, flatten);
            } else if (id.startsWith(propertyPrefix)) {
                String property = id.substring(propertyPrefix.length());
                property = Strings.replace(property, "prefix", target.getPrefix());

                String value = project.getProperties().getProperty(property);

                if (value != null) {
                    resolvePath(path, Strings.commaSepList(value), mergeWithCore, target, flatten);
                }
            } else if (id.equals("plugin")) {
                path.add(target.getPlugin().getArtifact());
            } else {
                throw new BuildException(
                    "A path group must contain a comma separated list of: plugin | plugin.<pluginpath> | project.<projectpath> | property.<reference to a property containing additional paths>: id="
                    + id);
            }
        }
    }

    public List getPluginPath(Plugin plugin, String pathId, boolean mergeWithCore, boolean flatten) {
        List path = new ArrayList();
        resolvePluginPath(path, plugin, pathId, mergeWithCore, flatten);

        return path;
    }

    private void resolvePluginPath(List path, Plugin plugin, String pathId, boolean mergeWithCore, boolean flatten) {
        // Check if the plugin declaration provides options to either add optional dependencies or override versions
        Set options = new HashSet();

        if (plugin.getDependency() != null) {
            for (Iterator j = plugin.getDependency().getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec pluginPathSpec = (RepoPathSpec)j.next();

                if (pluginPathSpec.getFrom().equals(pathId)) {
                    options.add(pluginPathSpec.getOptions());
                }
            }
        }

        boolean containsPlugin = false;

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();

            if (artifact.getId().equals(plugin.getArtifact().getId())) {
                containsPlugin = true;

                break;
            }
        }

        RepoPathSpec pathSpec = new RepoPathSpec(pathId, null, null, Boolean.TRUE, Boolean.TRUE);
        RepoDependency pluginDependency = new RepoDependency();
        pluginDependency.setId(plugin.getArtifact().getId());
        pluginDependency.addPathSpec(pathSpec);

        // TODO: Simplify this mess ... so that the plugin artifact is never added in the first place
        int size = path.size();
        resolvePath(pathId, path, pathSpec, options, true, plugin.getArtifact().getId(), mergeWithCore, flatten);

        if (!containsPlugin) {
            path.remove(size);
        }
    }

    private RepoArtifactId handleConflict(String pathId, List path, RepoArtifactId id, boolean autoFix,
        boolean flatten, RepoArtifactId declaredBy) {
        RepoArtifactId unversioned = toUnversionedId(id);

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();

            if (toUnversionedId(artifact.getId()).equals(unversioned)) {
                if (artifact.getId().getVersion().equals(id.getVersion())) {
                    return flatten ? null : id;
                } else {
                    // Conflict
                    Assert.isTrue(autoFix,
                        "A conflict has occurred between " + artifact.getId().toShortString() + " and "
                        + id.getVersion() + " for path '" + pathId + "'" + "\n\t" + artifact.getId().getVersion()
                        + " <- " + getDeclarations((RepoArtifactId)artifact.getId().getAnnotations().get("declaredBy"))
                        + "\n\t" + id.getVersion() + " <- " + getDeclarations(declaredBy));

                    return override(id, artifact.getId().getVersion());
                }
            }
        }

        return id;
    }

//    private String getLocation(AnnotatedObject annotatedObject) {
//        Locator locator = annotatedObject.getLocator();
//        if (locator == null) {
//            return "unknown";
//        } else {
//            return locator.toString();
//        }
//    }
    private String getDeclarations(RepoArtifactId id) {
        if (id == null) {
            return "project";
        }

        StringBuffer sb = new StringBuffer();
        getDeclarations(id, sb);

        return sb.toString();
    }

    private void getDeclarations(RepoArtifactId id, StringBuffer sb) {
        sb.append(id.toShortString());

        RepoArtifactId declaredBy = (RepoArtifactId)id.getAnnotations().get("declaredBy");

        if (declaredBy != null) {
            sb.append(" <- ");
            getDeclarations(declaredBy, sb);
        }
    }

    private void resolvePath(String pathId, List path, RepoPathSpec pathSpec, Set options, boolean force,
        RepoArtifactId declaredBy, boolean mergeWithCore, boolean flatten) {
        if (path.size() > 150) {
            System.err.println("Cycle detected!");

            for (Iterator i = path.iterator(); i.hasNext();) {
                RepoArtifact artifact = (RepoArtifact)i.next();
                System.err.println(artifact.getId().toShortString());
            }

            System.exit(1);
        }

        if (pathSpec.getOptions() != null) {
            options = new HashSet(options);
            options.add(pathSpec.getOptions());
        }

        if ((options.size() == 0) && !pathSpec.isMandatory().booleanValue() && !force) {
            return; // The artifact is not mandatory and has not been added as an option
        }

        // Add the artifact to the path
        RepoArtifact artifact = getArtifact(pathSpec.getDependency().getId());
        RepoArtifactId id = handleConflict(pathId, path, artifact.getId(), false, flatten, declaredBy);

        if (id != null) {
            artifact = id.equals(artifact.getId()) ? artifact : getArtifact(id);
            artifact.getId().getAnnotations().put("declaredBy", declaredBy);
            path.add(artifact);
        }

        if ((options.size() == 0) && !pathSpec.isDescend().booleanValue()) {
            return; // Not required to descend and no options to force it
        }

        // Process dependencies
        Set topLevelOptions = splitTopLevelOptions(options);

        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            // Handle global overrides
            for (Iterator j = project.getOverrides().iterator(); j.hasNext();) {
                Override override = (Override)j.next();

                if (override.matches(Override.SCOPE_ALL, dependency.getId())) {
                    dependency.setId(override(dependency.getId(), override.getWith()));
                }
            }

            for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec dependencyPathSpec = (RepoPathSpec)j.next();

                if (dependencyPathSpec.getTo().equals(pathSpec.getFrom())) {
                    // This dependency is path of the path
                    Set matchingOptions = new HashSet();
                    Version override = findMatchingOptions(artifact, dependencyPathSpec, topLevelOptions,
                            matchingOptions);

                    // Handle explicit overrides
                    if (override != null) {
                        dependency.setId(override(dependency.getId(), override));
                    }

                    if (pathSpec.isDescend().booleanValue() || (matchingOptions.size() > 0)) {
                        if (!mergeWithCore || !isCore(dependency.getId())) {
                            resolvePath(pathId, path, dependencyPathSpec, nextLevelOptions(matchingOptions),
                                matchingOptions.size() > 0, artifact.getId(), mergeWithCore, flatten);
                        }
                    }
                }
            }
        }

        Assert.isTrue(topLevelOptions.size() == 0, pathSpec.getLocator(),
            "Options do not match dependencies of artifact: artifact=" + artifact.getId() + ", options="
            + topLevelOptions + ", dependencies=" + artifact.getDependencies());
    }

    private RepoArtifactId override(RepoArtifactId id, Version version) {
        if (log.isDebugEnabled()) {
            log.debug("Overriding " + id.toShortString() + " to " + version);
        }

        RepoArtifactId overridden = new RepoArtifactId(id.getGroup(), id.getName(), id.getType(), version);
        overridden.setAnnotations((Annotations)id.getAnnotations().clone());

        return overridden;
    }

    private boolean isCore(RepoArtifactId id) {
        RepoArtifactId core = (RepoArtifactId)coreClassPath.get(toUnversionedId(id));

        if (core != null) {
            boolean conflict = !core.getVersion().equals(id.getVersion());

            if (conflict) {
                String message = "conflict with the core: dependency=" + id.toShortString() + ", core="
                    + core.getVersion();
                boolean overrideCore = "true".equals(antProject.getProperty("quokka.project.overrideCore"));
                Assert.isTrue(overrideCore, id.getLocator(), message);
                log.verbose("Overriding " + message);
            }

            log.verbose("Dropping " + id.toShortString() + " from path as it exists in the core");
        }

        return core != null;
    }

    private Version findMatchingOptions(RepoArtifact artifact, RepoPathSpec pathSpec, Set options, Set matching) {
        //        System.out.println("    matching options: options=" + options + ", artifact=" + artifact.getId() + ", pathSpec=" + pathSpec);
        Version override = null;

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();

            // Find matching artifact dependency
            String[] groupName = Strings.split(Strings.split(option, "(")[0], RepoArtifactId.ID_SEPARATOR);
            String name = groupName[0].trim();
            String group = (groupName.length > 1) ? groupName[1] : null;
            String[] nameVersion = Strings.split(name, "@");
            Assert.isTrue((nameVersion.length == 1) || (nameVersion.length == 2),
                "Invalid option format: valid format is [group][:]<name>[@][version]: options=" + option);

            if (nameVersion.length == 2) {
                name = nameVersion[0];

                Version version = new Version(nameVersion[1]);
                Assert.isTrue((override == null) || override.equals(version),
                    "Multiple overrides are specified for " + artifact.getId().toShortString()
                    + " that are inconsistent: " + version + " and " + override);
                override = version;
            }

            if (matches(group, name, artifact, pathSpec)) {
                i.remove(); // To see if any remain unmatched later

                //                System.out.println("Matched " + dependency + " to " + option);
                matching.add(option);
            }
        }

        return override;
    }

    private boolean matches(String group, String name, RepoArtifact artifact, RepoPathSpec pathSpec) {
        RepoArtifactId id = pathSpec.getDependency().getId();

        if (group != null) {
            return id.getGroup().equals(group) && id.getName().equals(name); // Exact match
        }

        if (!id.getName().equals(name)) {
            return false; // names don't match
        }

        // Make sure matching by name is unambiguos for all dependencies in the path
        int count = 0;

        for (Iterator i = artifact.getDependencies().iterator(); i.hasNext();) {
            RepoDependency dependency = (RepoDependency)i.next();

            for (Iterator j = dependency.getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec dependencyPathSpec = (RepoPathSpec)j.next();

                if (dependencyPathSpec.getTo().equals(pathSpec.getTo())) {
                    // This dependency is path of the path
                    if (id.getName().equals(dependencyPathSpec.getDependency().getId().getName())) {
                        count++;
                    }

                    if (count > 1) {
                        throw new BuildException(
                            "Option does not uniquely identify the dependency. Specify the group as well");
                    }
                }
            }
        }

        return true;
    }

    private Set nextLevelOptions(Set options) {
        Set nextLevel = new HashSet();

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();

            if (option.indexOf("(") > 0) {
                nextLevel.add(option.substring(option.indexOf('(') + 1, option.lastIndexOf(')')));
            }
        }

        return nextLevel;
    }

    private RepoArtifact getArtifact(RepoArtifactId artifactId) {
        return (RepoArtifact)repository.resolve(artifactId).clone(); // Clone to allow additional annotations to be added within context
    }

    private Set splitTopLevelOptions(Set options) {
        Set topLevelOptions = new HashSet();

        for (Iterator i = options.iterator(); i.hasNext();) {
            String option = (String)i.next();
            String[] split = Strings.splitTopLevel(option, '(', ')', ',');
            topLevelOptions.addAll(Arrays.asList(split));
        }

        return topLevelOptions;
    }

    public List getProjectPath(String id, boolean mergeWithCore, boolean flatten) {
        ArrayList path = new ArrayList();
        log.debug("Getting project path: id=" + id + ", mergeWithCore=" + mergeWithCore + ", flatten=" + flatten);
        resolveProjectPath(path, id, project.getDependencySet(), mergeWithCore, flatten);

        return path;
    }

    public Map getResolvedPaths() {
        return resolvedPaths;
    }

    private void resolveProjectPath(List path, String id, DependencySet dependencySet, boolean mergeWithCore,
        boolean flatten) {
        for (Iterator i = dependencySet.getDependencies().iterator(); i.hasNext();) {
            resolveProjectPath(path, id, (Dependency)i.next(),
                (dependencySet.getArtifact() == null) ? null : dependencySet.getArtifact().getId(), mergeWithCore,
                flatten);
        }

        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            resolveProjectPath(path, id, (DependencySet)i.next(), mergeWithCore, flatten);
        }
    }

    private void resolveProjectPath(List path, String pathId, Dependency dependency, RepoArtifactId declaredBy,
        boolean mergeWithCore, boolean flatten) {
        for (Iterator i = dependency.getPathSpecs().iterator(); i.hasNext();) {
            RepoPathSpec pathSpec = (RepoPathSpec)i.next();

            if (pathSpec.getTo().equals(pathId)) {
                resolvePath(pathId, path, pathSpec, new HashSet(), false, declaredBy, mergeWithCore, flatten);
            }
        }
    }

    public Metadata getMetadata() {
        return new ProjectMetadata(this);
    }

    public org.apache.tools.ant.Project getAntProject() {
        return antProject;
    }

    public void setAntProject(org.apache.tools.ant.Project antProject) {
        this.antProject = antProject;
        log = new ProjectLogger(antProject);
    }

    public BuildResources getBuildResources() {
        return buildResources;
    }

    public List getResolvedImports() {
        return resolvedImports;
    }

    public Map getProjectPaths() {
        return resolvedPaths;
    }

    public List getAliases() {
        return aliases;
    }

    public Runnable createTargetInstance(Target target, Properties localProperties, Logger logger) {
        //        System.out.println("DefaultProjectModel.createTargetInstance");
        AntClassLoader loader = null;
        DefaultResources resources;

        try {
            org.apache.tools.ant.types.Path classPath = toAntPath(resolvePath(target, "classpath", true));

            // Allow additional classes to added to the plugin classpath. This is primarily designed to
            // allow the additional of instrumented testing classes and libraries for code coverage of integration
            // tests with Cobertura
            String key = "quokka.classpath." + target.getPlugin().getArtifact().getId().getGroup();

            if (log.isDebugEnabled()) {
                log.debug("Searching for additional classpath with key: " + key);
            }

            String additionalPath = System.getProperty(key);

            if ((additionalPath != null) && !additionalPath.trim().equals("")) {
                org.apache.tools.ant.types.Path existing = classPath;
                classPath = new org.apache.tools.ant.types.Path(antProject, additionalPath);
                log.verbose("Prefixing classpath with: " + classPath);
                classPath.append(existing); // Make sure additions override existing
            }

            if ("true".equals(antProject.getProperty("quokka.project.debugclassloaders"))) {
                loader = new QuokkaLoader(target, antProject.getClass().getClassLoader(), antProject, classPath);
            } else {
                loader = antProject.createClassLoader(classPath);
            }

            loader.setParent(antProject.getCoreLoader());
            loader.setParentFirst(true);
            loader.setIsolated(false);
            loader.setThreadContextLoader();

            // Initialise this plugin
            Plugin plugin = target.getPlugin();
            loader.forceLoadClass(plugin.getClassName());

            Class pluginClass = Class.forName(plugin.getClassName(), true, loader);
            ws.quokka.core.plugin_spi.Plugin actualPlugin = (ws.quokka.core.plugin_spi.Plugin)pluginClass.newInstance();

            if (actualPlugin instanceof MetadataAware) {
                ((MetadataAware)actualPlugin).setMetadata(getMetadata());
            }

            if (actualPlugin instanceof ResourcesAware) {
                resources = new DefaultResources(this, target, antProject,
                        (PluginState)antProject.getReference("quokka.pluginState"), logger);
                ((ResourcesAware)actualPlugin).setResources(resources);
            }

            if (actualPlugin instanceof RepositoryAware) {
                ((RepositoryAware)actualPlugin).setRepository(getRepository());
            }

            if (actualPlugin instanceof ModelFactoryAware) {
                ((ModelFactoryAware)actualPlugin).setModelFactory(getModelFactory());
            }

            actualPlugin.initialise();

            return actualPlugin.getTarget((target.getTemplateName() != null) ? target.getTemplateName() : target
                .getName());
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            if (loader != null) {
                loader.resetThreadContextLoader();
                loader.cleanup();
            }
        }
    }

    public List mergePaths(List classPaths) {
        return null;
    }

    public void mergeWithCoreClassPath(List classPath) {
        for (Iterator i = classPath.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            Version version = (Version)coreClassPath.get(toUnversionedId(artifact.getId()));

            if (version == null) {
                continue; // Not in boot class path at all
            }

            Assert.isTrue(artifact.getId().getVersion().equals(version), artifact.getLocator(),
                "The artifact conflicts with core classpath: artifact=" + artifact.getId().toShortString()
                + ", core version=" + version.toString());
            i.remove();
        }
    }

    protected List getCoreModules() {
        List modules = new ArrayList();

        String[] module = new String[] {
                "main", "metadata", "model", "plugin-spi", "repo-spi", "repo-standard", "util", "bootstrap"
            };

        for (int i = 0; i < module.length; i++) {
            String name = module[i];
            modules.add(coreModuleId(name));
        }

        // Add the ant jars
        Properties properites = new ArtifactPropertiesParser().parse("quokka.core.main", "main", "jar");
        List dependencies = BootStrapper.getDependencies(properites, "dist");

        // Add any additional user specified dependencies
        // TODO: Support additional dependencies for non-bootstrapped builds?
        if (bootStrapper != null) {
            dependencies.addAll(bootStrapper.getAdditionalDependencies());
        }

        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            DependencyResource dependency = (DependencyResource)i.next();
            modules.add(new RepoArtifactId(dependency.getGroup(), dependency.getName(), "jar", dependency.getVersion()));
        }

        return modules;
    }

    protected RepoArtifactId coreModuleId(String module) {
        String group = "quokka.core" + "." + module;
        String type = "jar";
        Properties properites = new ArtifactPropertiesParser().parse(group, module, type);

        return new RepoArtifactId(group, module, type, Version.parse(properites.getProperty("artifact.id.version")));
    }

    /**
     * Stuck here for now ... don't want to introduce dependency on ProfilesMatcher into AnnotatedProperties ...
     */
    public static AnnotatedProperties applyProfiles(AnnotatedProperties properties, Set activeProfiles) {
        AnnotatedProperties applied = new AnnotatedProperties();

        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();
            Annotations annotations = properties.getAnnotation(key);

            if (key.startsWith("[")) {
                int index = key.indexOf("]");

                if (index != -1) {
                    Set profiles = new HashSet(Strings.commaSepList(key.substring(1, index)));
                    ProfilesMatcher profilesMatcher = new ProfilesMatcher();

                    if (profilesMatcher.matches(profiles, activeProfiles)) {
                        applied.setProperty(key.substring(index + 1), value, annotations);
                    }
                }
            } else {
                applied.setProperty(key, value, annotations);
            }
        }

        return applied;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * QuokkaLoader is a class loader that keeps track of the number of class loaders
     * allocated versus finalized to check for class laoder leaks. At present, the jalopy
     * plugin is known the leak loaders, although the underlying cause has not been identified.
     *
     * Useful options:
     *      Debugging:   QUOKKA_OPTS=-verbose:gc -XX:+PrintClassHistogram -XX:+PrintGCDetails
     *      Work-around: QUOKKA_OPTS=-XX:MaxPermSize=128m
     */
    public static class QuokkaLoader extends AntClassLoader {
        private static Map loaders = new TreeMap();
        private String name;

        public QuokkaLoader(Target target, ClassLoader parent, org.apache.tools.ant.Project project,
            org.apache.tools.ant.types.Path classpath) {
            super(parent, project, classpath);
            name = target.getName();
            add(1);
        }

        private static void initialise() {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        System.out.println("Checking for leaking class loaders ...");
                        System.gc();

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        System.gc();

                        for (Iterator i = loaders.entrySet().iterator(); i.hasNext();) {
                            Map.Entry entry = (Map.Entry)i.next();
                            System.out.println(entry.getKey() + " -> " + entry.getValue());
                        }
                    }
                });
        }

        protected void finalize() throws Throwable {
            super.finalize();
            add(-1);
        }

        private void add(int i) {
            synchronized (loaders) {
                if (loaders.size() == 0) {
                    initialise();
                }

                Integer count = (Integer)loaders.get(name);

                if (count == null) {
                    count = new Integer(0);
                }

                count = new Integer(count.intValue() + i);
                loaders.put(name, count);
            }
        }
    }
}
