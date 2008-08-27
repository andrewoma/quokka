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
import ws.quokka.core.model.License;
import ws.quokka.core.model.ModelFactory;
import ws.quokka.core.model.ModelFactoryAware;
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
import ws.quokka.core.repo_resolver.ResolvedPath;
import ws.quokka.core.repo_resolver.Resolver;
import ws.quokka.core.repo_resolver.ResolverAware;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.RepoOverride;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.repo_spi.RepoPathSpec;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.repo_spi.Repository;
import ws.quokka.core.repo_spi.RepositoryAware;
import ws.quokka.core.repo_spi.RepositoryFactory;
import ws.quokka.core.repo_spi.RepositoryFactoryAware;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Annotations;
import ws.quokka.core.util.PropertyProvider;
import ws.quokka.core.util.Strings;
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


/**
 *
 */
public class DefaultProjectModel implements ProjectModel {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;
    private Profiles profiles;
    private ModelFactory modelFactory;
    private List aliases = new ArrayList();
    private BootStrapper bootStrapper;
    private Logger log;
    private Resolver pathResolver;
    private ResolvedPath corePath = new ResolvedPath();
    private List coreOverrides = new ArrayList();

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

    public Resolver getPathResolver() {
        return pathResolver;
    }

    public void setPathResolver(Resolver pathResolver) {
        this.pathResolver = pathResolver;
    }

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

                if (dependencyTarget.getTemplate() == null) {
                    // Enabling a target
                    String name = dependencyTarget.getName();
                    name = (name.indexOf(":") != -1) ? name : (target.getPlugin().getNameSpace() + ":" + name);

                    if (target.getName().equals(name)) {
                        Assert.isTrue(!target.isTemplate(), dependencyTarget.getLocator(),
                            "The named target '" + name + "' is a template");

                        String prefix = dependencyTarget.getPrefix();
                        Assert.isTrue((prefix == null) || prefix.equals(target.getPrefix()),
                            dependencyTarget.getLocator(),
                            "The prefix '" + prefix + "' should match the target prefix '" + target.getPrefix()
                            + "' if specified");

                        if (dependencyTarget.getAlias() != null) {
                            target.setAlias(dependencyTarget.getAlias());
                        }

                        targetInstance = target;
                    }
                } else {
                    // Instantiation of a template
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

                    // Record the plugin dependency target that introduced the target
                    // This will be used later for dependency-of processing
                    targetInstance.setPluginDependencyTarget(dependencyTarget);

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
        pathResolver = new Resolver(repository, log);

        // Add ant-types path
        project.getDependencySet().addPath(new Path("ant-types",
                "Any entires added to this path are automatically added to the ant type definition class loader. "
                + "e.g. Add commons.net and ant's optional commons.net library for using the ftp task"));

        // Get dependency sets, applying profiles and overrides
        List sets = depthFirst(project.getDependencySet());

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

            // Find the unique names for all declared artifacts
            Set uniqueNames = new HashSet();

            for (Iterator j = project.getArtifacts().iterator(); j.hasNext();) {
                Artifact artifact = (Artifact)j.next();
                uniqueNames.add(artifact.getId().getName());
            }

            // Create artifacts for any licenses referenced by files
            for (Iterator j = set.getLicenses().iterator(); j.hasNext();) {
                License license = (License)j.next();

                if (license.getFile() != null) {
                    Assert.isTrue(project.getArtifacts().size() != 0, license.getLocator(),
                        "There are no artifacts defined for a license to be applied to.");

                    Artifact artifact = (Artifact)project.getArtifacts().iterator().next();
                    RepoArtifactId id = artifact.getId();

                    // If no name is specified, default it to name of the artifact if it is unique,
                    // otherwise use the default from the group
                    String name = license.getId().getName();
                    String defaultName = RepoArtifactId.defaultName(id.getGroup());

                    if (name == null) {
                        if (uniqueNames.size() == 1) {
                            name = (String)uniqueNames.iterator().next();
                        } else {
                            name = defaultName;
                        }
                    }

                    Artifact licenseArtifact = new Artifact(id.getGroup(), name, "license", id.getVersion());
                    licenseArtifact.setDescription("License for " + id.getGroup()
                        + (name.equals(defaultName) ? "" : (":" + name)));
                    project.addArtifact(licenseArtifact);
                    license.setId(licenseArtifact.getId());
                }
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
        corePath = resolveCorePath();

        if ("true".equals(antProject.getProperty("quokka.project.overrideCore"))) {
            coreOverrides = getCoreOverrides();
        }
    }

    private List depthFirst(DependencySet dependencySet) {
        List sets = new ArrayList();
        depthFirst(sets, dependencySet);

        return sets;
    }

    private void depthFirst(List sets, DependencySet dependencySet) {
        sets.add(dependencySet);

        for (Iterator i = dependencySet.getSubsets().iterator(); i.hasNext();) {
            DependencySet subset = (DependencySet)i.next();
            depthFirst(sets, subset);
        }
    }

//    Project parser is depth-first, so keep the logic consistent. Keep for now in case of switch
//    private List setsBreadthFirst() {
//        List sets = new ArrayList();
//        LinkedList queue = new LinkedList();
//        queue.add(project.getDependencySet());
//        while (!queue.isEmpty()) {
//            DependencySet set = (DependencySet) queue.removeFirst();
//            sets.add(set);
//            for (Iterator i = set.getSubsets().iterator(); i.hasNext();) {
//                DependencySet subSet = (DependencySet) i.next();
//                queue.addLast(subSet);
//            }
//        }
//        return sets;
    //    }
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
            repository.getFactory().registerType((RepoType)i.next());
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

            if (artifact.getLocalCopy() != null) { // Possible for "paths" type
                path.add(new org.apache.tools.ant.types.Path(antProject, artifact.getLocalCopy().getAbsolutePath()));
            }
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

    public List resolvePathGroup(Target target, String pathGroupId) {
        Plugin plugin = target.getPlugin();
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

//        System.out.println("Resolving path: target=" + target.getName() + ", pathGroup=" + pathGroupId
//            + ", pathGroupElements=" + pathGroup.getPaths());
        List paths = new ArrayList();

        // Note: merging with core is turned off here so that the proper path tree is maintained
        // However, core overrides are still applied by separating that into a different flag
        resolvePath(pathGroup.getPaths(), paths, false, pathGroup.getMergeWithCore().booleanValue(), target, false);

//        for (Iterator i = paths.iterator(); i.hasNext();) {
//            ResolvedPath path = (ResolvedPath) i.next();
//            System.out.println(pathResolver.formatPath(path, false));
//        }
        ResolvedPath path = pathResolver.merge(paths);

        if (pathGroup.getMergeWithCore().booleanValue()) {
            mergeWithCore(path);
        }

//        System.out.println(pathResolver.formatPath(path, false));
        StringBuffer sb = new StringBuffer();

        for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            sb.append(artifact.getId().toShortString()).append(";");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolved path: target=" + target.getName() + ", pathGroup=" + pathGroupId + ", path="
                + sb.toString());
        }

        return path.getArtifacts();
    }

    private void resolvePath(List ids, List paths, boolean mergeWithCore, boolean overrideCore, Target target,
        boolean flatten) {
        String projectPrefix = "project.";
        String pluginPrefix = "plugin.";
        String propertyPrefix = "property.";

        for (Iterator i = ids.iterator(); i.hasNext();) {
            String id = (String)i.next();

            if (id.startsWith(projectPrefix)) {
                String projectPathId = id.substring(projectPrefix.length());
                Assert.isTrue(resolvedPaths.get(projectPathId) != null,
                    "Project path '" + projectPathId + "' is not defined");
                paths.add(getReslovedProjectPath(projectPathId, mergeWithCore, overrideCore, flatten));
            } else if (id.startsWith(pluginPrefix)) {
                String pluginPathId = id.substring(pluginPrefix.length());
                paths.add(getResolvedPluginPath(target.getPlugin(), pluginPathId, mergeWithCore, overrideCore, flatten));
            } else if (id.startsWith(propertyPrefix)) {
                String property = id.substring(propertyPrefix.length());
                property = Strings.replace(property, "prefix", target.getPrefix());

                String value = project.getProperties().getProperty(property);

                if (value != null) {
                    resolvePath(Strings.commaSepList(value), paths, mergeWithCore, overrideCore, target, flatten);
                }
            } else if (id.equals("plugin")) {
                ResolvedPath path = new ResolvedPath();
                path.setId("Plugin:");
                path.add(target.getPlugin().getArtifact());
                paths.add(path);
            } else {
                throw new BuildException(
                    "A path group must contain a comma separated list of: plugin | plugin.<pluginpath> | project.<projectpath> | property.<reference to a property containing additional paths>: id="
                    + id);
            }
        }
    }

    public List getPluginPath(Plugin plugin, String pathId, boolean mergeWithCore, boolean flatten) {
        return getResolvedPluginPath(plugin, pathId, mergeWithCore, mergeWithCore, flatten).getArtifacts();
    }

    public ResolvedPath getResolvedPluginPath(Plugin plugin, String pathId, boolean mergeWithCore,
        boolean overrideCore, boolean flatten) {
        // Create a mock artifact for the resolver as a way to add user specified path specs and overrides
        RepoArtifact artifact = new RepoArtifact();
        RepoDependency dependency = new RepoDependency();
        RepoArtifactId pluginId = plugin.getArtifact().getId();
        dependency.setId(pluginId);
        artifact.addDependency(dependency);

        String id = "plugin";
        artifact.addPath(new RepoPath(id, "Plugin path", true, true));

        // Add dependencies
        if (plugin.getDependency() != null) {
            for (Iterator j = plugin.getDependency().getPathSpecs().iterator(); j.hasNext();) {
                RepoPathSpec pluginPathSpec = (RepoPathSpec)j.next();

                if (pluginPathSpec.getFrom().equals(pathId)) {
                    // Add user specifications. Ignore the mandatory flag and to paths as they
                    // are not relevant. However, allow descend to be false in case the writer of the
                    // plugin added bogus dependencies.
                    dependency.addPathSpec(new RepoPathSpec(pathId, id, pluginPathSpec.getOptions(),
                            (pluginPathSpec.isDescend() == null) ? Boolean.TRUE : pluginPathSpec.isDescend(),
                            Boolean.TRUE));
                }
            }
        }

        if (dependency.getPathSpecs().size() == 0) {
            // Add default ... user hasn't specified anything
            dependency.addPathSpec(new RepoPathSpec(pathId, id, null, Boolean.TRUE, Boolean.TRUE));
        }

        // Add core overrides if applicable
        if (overrideCore) {
            for (Iterator j = coreOverrides.iterator(); j.hasNext();) {
                RepoOverride override = (RepoOverride)j.next();
                artifact.addOverride(override);
            }
        }

        // Add overrides
        for (Iterator i = depthFirst(project.getDependencySet()).iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();

            for (Iterator j = set.getOverrides().iterator(); j.hasNext();) {
                ws.quokka.core.model.Override override = (ws.quokka.core.model.Override)j.next();
                Set paths = override.matchingPluginPaths(plugin.getArtifact().getId());

                if (paths.contains(pathId) || ((paths.size() == 1) && paths.contains("*"))) {
                    // Create a copy of the override, moving matching plugin paths to be standard paths
                    RepoOverride copy = new RepoOverride(Collections.singleton("*"), override.getGroup(),
                            override.getName(), override.getType(), override.getVersion(), override.getWithVersion(),
                            override.getWithPathSpecs());
                    artifact.addOverride(copy);
                }
            }
        }

        // Remove the plugin itself from the path
        // TODO: Look into a better way of doing this, perhaps modifying Resolver so it has the option
        // of not adding the root in first place.
        ResolvedPath path = pathResolver.resolvePath(id, artifact);
        path.setId("Plugin path '" + pathId + "' from " + pluginId.toShortString());

        for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
            artifact = (RepoArtifact)i.next();

            if (artifact.getId().equals(pluginId)) {
                i.remove();
            }

            if (pluginId.equals(artifact.getId().getAnnotations().get("declaredBy"))) {
                artifact.getId().getAnnotations().remove("declaredBy");
            }
        }

        path = handleMergeAndFlatten(mergeWithCore, flatten, path);

        return path;
    }

    private RepoArtifact getArtifact(RepoArtifactId artifactId) {
        return (RepoArtifact)repository.resolve(artifactId).clone(); // Clone to allow additional annotations to be added within context
    }

    public List getProjectPath(String id, boolean mergeWithCore, boolean flatten) {
        return getReslovedProjectPath(id, mergeWithCore, mergeWithCore, flatten).getArtifacts();
    }

    public ResolvedPath getReslovedProjectPath(String id, boolean mergeWithCore, boolean overrideCore, boolean flatten) {
        return getReslovedProjectPath(id, mergeWithCore, overrideCore, flatten, new ArrayList());
    }

    public ResolvedPath getReslovedProjectPath(String id, boolean mergeWithCore, boolean overrideCore, boolean flatten,
        List appliedOverrides) {
        log.debug("Getting project path: id=" + id + ", mergeWithCore=" + mergeWithCore + ", overrideCore="
            + overrideCore + ", flatten=" + flatten);

        RepoArtifact artifact = new RepoArtifact();

        for (Iterator i = depthFirst(project.getDependencySet()).iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();

            // Add dependencies
            for (Iterator j = set.getDependencies().iterator(); j.hasNext();) {
                RepoDependency dependency = (RepoDependency)j.next();
                artifact.addDependency(dependency);
            }

            // Add core overrides first if applicable
            if (overrideCore) {
                for (Iterator j = coreOverrides.iterator(); j.hasNext();) {
                    RepoOverride override = (RepoOverride)j.next();
                    artifact.addOverride(override);
                }
            }

            // Add overrides
            for (Iterator j = set.getOverrides().iterator(); j.hasNext();) {
                RepoOverride override = (RepoOverride)j.next();
                artifact.addOverride(override);
            }
        }

        // Copy the project paths to the artifact
        for (Iterator i = resolvedPaths.values().iterator(); i.hasNext();) {
            RepoPath path = (RepoPath)i.next();
            artifact.addPath(path);
        }

        ResolvedPath path = pathResolver.resolvePath(id, artifact, appliedOverrides, false, true);
        path.setId("Project path '" + id + "'");

        path = handleMergeAndFlatten(mergeWithCore, flatten, path);

        return path;
    }

    /**
     * Overrides anything that conflicts with the core to the core version
     * This will allow old plugins to potentially work without overriding them
     */
    private List getCoreOverrides() {
        List overrides = new ArrayList();

        for (Iterator i = corePath.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            RepoArtifactId id = artifact.getId();
            overrides.add(new RepoOverride(Collections.singleton("*"), id.getGroup(), id.getName(), id.getType(), null,
                    id.getVersion()));
        }

        return overrides;
    }

    private ResolvedPath handleMergeAndFlatten(boolean mergeWithCore, boolean flatten, ResolvedPath path) {
        if (mergeWithCore) {
            mergeWithCore(path);
        } else if (flatten) {
            path = pathResolver.merge(Collections.singleton(path));
        }

        return path;
    }

    private void mergeWithCore(ResolvedPath path) {
        // Will throw a detailed exception on conflict
        pathResolver.merge(Arrays.asList(new ResolvedPath[] { corePath, path }));

        // Now strip any artifacts that are found in the core
        for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();

            if (corePath.contains(artifact.getId())) {
                i.remove();
            }
        }
    }

    public Map getResolvedPaths() {
        return resolvedPaths;
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
            org.apache.tools.ant.types.Path classPath = toAntPath(resolvePathGroup(target, "classpath"));

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

            if (actualPlugin instanceof RepositoryFactoryAware) {
                ((RepositoryFactoryAware)actualPlugin).setRepositoryFactory((RepositoryFactory)antProject.getReference(
                        ProjectHelper.REPOSITORY_FACTORY));
            }

            if (actualPlugin instanceof ResolverAware) {
                ((ResolverAware)actualPlugin).setResolver(pathResolver);
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

    protected ResolvedPath resolveCorePath() {
        List ids = new ArrayList();

        // Add the ant jars
        Properties properites = new ArtifactPropertiesParser().parse("quokka.core.main", "main", "jar");
        ids.add(new DependencyResource("quokka.core.main", "main",
                Version.parse(properites.getProperty("artifact.id.version"))));
        ids.addAll(BootStrapper.getDependencies(properites, "runtime"));

//        properites = new ArtifactPropertiesParser().parse("quokka.core.ant-optional-1-7-0", "ant-optional-1-7-0", "jar");
//        ids.addAll(BootStrapper.getDependencies(properites, "bundle"));
        // Add any additional user specified dependencies
        // TODO: Support additional dependencies for non-bootstrapped builds?
        if (bootStrapper != null) {
            ids.addAll(bootStrapper.getAdditionalDependencies());
        }

        List artifacts = new ArrayList();

        for (Iterator i = ids.iterator(); i.hasNext();) {
            DependencyResource dependency = (DependencyResource)i.next();
            artifacts.add(new RepoArtifact(
                    new RepoArtifactId(dependency.getGroup(), dependency.getName(), "jar", dependency.getVersion())));
        }

        ResolvedPath path = new ResolvedPath("Quokka core path", artifacts);
        log.debug(pathResolver.formatPath(path, false));

        return path;
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

    public Set getLicenses() {
        Set licenses = new HashSet();

        for (Iterator i = depthFirst(project.getDependencySet()).iterator(); i.hasNext();) {
            DependencySet set = (DependencySet)i.next();
            licenses.addAll(set.getLicenses());
        }

        return licenses;
    }
}
