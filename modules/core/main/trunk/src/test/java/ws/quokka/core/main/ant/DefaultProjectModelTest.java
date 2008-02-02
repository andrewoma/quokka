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

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.main.parser.PluginParser;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.Override;
import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathGroup;
import ws.quokka.core.model.PathSpec;
import ws.quokka.core.model.Plugin;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.PluginDependencyTarget;
import ws.quokka.core.model.Project;
import ws.quokka.core.model.Target;
import ws.quokka.core.repo_spi.AbstractRepository;
import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoDependency;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.repo_spi.RepoPathSpec;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class DefaultProjectModelTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultProjectModel projectModel;
    private RepoArtifactId pluginId = new RepoArtifactId("plugin", "plugin", "jar", "1.0");

    // Define a tree of dependencies
    private RepoArtifactId dep1 = new RepoArtifactId("dep1", "dep1", "jar", "1.0");
    private RepoArtifactId dep1v2 = new RepoArtifactId("dep1", "dep1", "jar", "2.0");
    private RepoArtifactId dep1_1 = new RepoArtifactId("dep1_1", "dep1_1", "jar", "1.0");
    private RepoArtifactId dep1_1_1 = new RepoArtifactId("dep1_1_1", "dep1_1_1", "jar", "1.0");
    private RepoArtifactId dep1_2 = new RepoArtifactId("dep1_2", "dep1_2", "jar", "1.0");
    private RepoArtifactId dep1_2_1 = new RepoArtifactId("dep1_2_1", "dep1_2_1", "jar", "1.0");
    private RepoArtifactId dep1_2_2_1 = new RepoArtifactId("dep1_2_2_1", "dep1_2_2_1", "jar", "1.0");
    private RepoArtifactId dep1_2_2 = new RepoArtifactId("dep1_2_2", "dep1_2_2", "jar", "1.0");
    private RepoArtifactId dep2 = new RepoArtifactId("dep2", "dep2", "jar", "1.0");
    private RepoArtifactId dep3 = new RepoArtifactId("dep3", "dep3", "jar", "1.0");
    private RepoArtifactId dep3_1 = new RepoArtifactId("dep3_1", "dep3_1", "jar", "1.0");
    private Map dependencies = new HashMap();
    private Map paths = new HashMap();
    private Map pathSpecs = new HashMap();
    private Map pathGroups = new HashMap();
    private Map targets = new HashMap();
    private List defaultPaths = new ArrayList();
    private List defaultPathSpecs = new ArrayList();
    private List defaultPathGroups = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        projectModel = new DefaultProjectModel();
        projectModel.setRepository(new MockRepository());
        projectModel.setPluginParser(new MockPluginParser());

        Project project = new Project();
        project.getDependencySet().addPath(new Path("projectpath1", "Some project path"));
        projectModel.setProject(project);

        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
        antProject.init();
        projectModel.setAntProject(antProject);

        addDependency(dep1, dep1_1, dep1_2);
        addDependency(dep1_1, dep1_1_1);
        addDependency(dep1_2, dep1_2_1, dep1_2_2);
        addDependency(dep1_2_2, dep1_2_2_1);
        addDependency(dep3, dep3_1);

        defaultPaths.add(new RepoPath("runtime", "Runtime path"));
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.TRUE));
        defaultPathGroups.add(new PathGroup("classpath", Arrays.asList(new String[] { "plugin.runtime" }), Boolean.TRUE));
    }

    public void testResolveTargets() {
        addPluginDependency(dep1, new String[] { "test" });
        addPluginTarget(dep1, "test");
        projectModel.initialise();
        assertContainsTarget(projectModel.getTargets().keySet(), "test");
    }

    public void testResolvePathMandatoryDescend() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", null, Boolean.TRUE, Boolean.TRUE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);

        // Should be whole hierachy from dep1 down
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1, dep1_2_2, dep1_2_2_1 });
    }

    public void testResolvePathWithGlobalOverride() {
        Override override = new Override();
        override.setGroup("dep1");
        override.setName("dep1");
        override.setScope(Override.SCOPE_ALL);
        override.setType("jar");
        override.setWith(Version.parse("2.0"));
        override.setVersionRangeUnion(VersionRangeUnion.parse("[1.0,1.0]"));
        assertTrue(override.matches(Override.SCOPE_ALL, dep1)); // Check the override matches ..

        projectModel.getProject().addOverride(override);
        addPluginDependency(pluginId, new String[] { "test" },
            new RepoPathSpec("runtime", "runtime", "dep1", Boolean.FALSE, Boolean.TRUE));
        addDependency(pluginId, dep1);
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1v2 });
    }

    public void testResolvePathWithOverride() {
        addPluginDependency(pluginId, new String[] { "test" },
            new RepoPathSpec("runtime", "runtime", "dep1@2", Boolean.FALSE, Boolean.TRUE));
        addDependency(pluginId, dep1);
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1v2 });
    }

    public void testPathReferencedByProperty() {
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.setProperty("prop1", "plugin");
        projectModel.getProject().setProperties(properties);
        addPluginDependency(pluginId, new String[] { "test" });
        addPathGroup(pluginId, "test",
            new PathGroup("group1", Arrays.asList(new String[] { "property.prop1" }), Boolean.TRUE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "group1", true);
        assertPathContains(path, new RepoArtifactId[] { pluginId });
    }

    public void testConflict() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addDependency(dep1_2_1, dep1v2);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", null, Boolean.TRUE, Boolean.TRUE));
        projectModel.initialise();

        try {
            projectModel.resolvePath(getTarget("test"), "classpath", true);
            fail("Exception expected");
        } catch (BuildException e) {
            assertTrue(e.getMessage().indexOf("conflict") != -1);
        }
    }

    public void testFlattening() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addDependency(dep1_1_1, dep1_2); // Add duplicated dependencies
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", null, Boolean.TRUE, Boolean.TRUE));
        projectModel.initialise();

        // Test with flattening
        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1, dep1_2_2, dep1_2_2_1 });

        // Test without flattening
        path = projectModel.resolvePath(getTarget("test"), "classpath", false);
        assertEquals(11, path.size()); // dep1_2, dep1_2_1, dep1_2_2, dep1_2_2_1 should be duplicated
    }

    public void testResolvePathOptionalDescend() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", null, Boolean.TRUE, Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.FALSE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);

        // Should be dep1 alone
        assertPathContains(path, new RepoArtifactId[] { dep1 });
    }

    public void testResolvePathMandatoryNotDescend() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", null, Boolean.FALSE, Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.FALSE, Boolean.TRUE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1 });
    }

    public void testResolvePathOptionalWithPathSpecs() {
        addPluginDependency(pluginId, new String[] { "test" });
        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "runtime", "dep1_1", Boolean.FALSE, Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.FALSE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1 });
    }

    public void testResolvePathOptionalWithPathSpecs2() {
        PluginDependency pluginDependency = addPluginDependency(pluginId, new String[] { "test" });

        // Specify options via the plugin (this is where users can specify extra options)
        pluginDependency.addPathSpec(new RepoPathSpec("runtime", "projectpath1", "dep1(dep1_2(dep1_2_2))",
                Boolean.FALSE, Boolean.FALSE));

        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1,
            new RepoPathSpec("runtime", "runtime", "dep1_1, dep1_2(dep1_2_1)", Boolean.FALSE, Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.FALSE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "classpath", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1, dep1_2, dep1_2_1, dep1_2_2 });
    }

    public void testResolvePathOptionalWithMultiplePaths() {
        addPluginDependency(pluginId, new String[] { "test" });
        addPath(pluginId, new RepoPath("path1", "Path number 1"));
        addPath(pluginId, new RepoPath("path2", "Path number 2"));
        addDependency(pluginId, dep1);
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "path1", null, Boolean.FALSE, Boolean.TRUE));
        addPathSpec(pluginId, dep1, new RepoPathSpec("runtime", "path2", null, Boolean.TRUE, Boolean.TRUE));
        addPathGroup(pluginId, "test",
            new PathGroup("group1", Arrays.asList(new String[] { "plugin.path1" }), Boolean.TRUE));
        addPathGroup(pluginId, "test",
            new PathGroup("group2", Arrays.asList(new String[] { "plugin.path2" }), Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.TRUE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "group1", true);
        assertPathContains(path, new RepoArtifactId[] { dep1 });

        path = projectModel.resolvePath(getTarget("test"), "group2", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1, dep1_2_2, dep1_2_2_1 });
    }

    public void testResolvePathForProjectPath() {
        // Add a project dependency
        Dependency dependency = addProjectDependency(dep1);
        projectModel.getProject().getDependencySet().addPath(new Path("compile", "Compilation path"));
        dependency.addPathSpec(new RepoPathSpec("runtime", "compile", null, Boolean.TRUE, Boolean.TRUE));

        addPluginDependency(pluginId, new String[] { "test" });
        addPathGroup(pluginId, "test",
            new PathGroup("group1", Arrays.asList(new String[] { "project.compile" }), Boolean.TRUE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "group1", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1, dep1_2_2, dep1_2_2_1 });
    }

    private Target getTarget(String name) {
        name = "ns:" + name;

        return (Target)projectModel.getTargets().get(name);
    }

    public void testResolvePathForProjectPathWithOptions() {
        // Add a project dependency
        Dependency dependency = addProjectDependency(dep1);
        projectModel.getProject().getDependencySet().addPath(new Path("compile", "Compilation path"));
        dependency.addPathSpec(new RepoPathSpec("runtime", "compile", "dep1_2(dep1_2_2)", Boolean.TRUE, Boolean.TRUE));

        addPluginDependency(pluginId, new String[] { "test" });
        addPathGroup(pluginId, "test",
            new PathGroup("group1", Arrays.asList(new String[] { "project.compile" }), Boolean.TRUE));

        defaultPathSpecs.clear();
        defaultPathSpecs.add(new RepoPathSpec("runtime", "runtime", Boolean.TRUE, Boolean.FALSE));
        projectModel.initialise();

        List path = projectModel.resolvePath(getTarget("test"), "group1", true);
        assertPathContains(path, new RepoArtifactId[] { dep1, dep1_2, dep1_2_2 });
    }

    private void assertPathContains(List path, RepoArtifactId[] repoArtifactIds) {
        assertEquals("Path contains wrong number of elements", repoArtifactIds.length, path.size());

        List ids = Arrays.asList(repoArtifactIds);

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            assertTrue(ids.contains(artifact.getId()));
        }
    }

    private void addPluginTarget(RepoArtifactId pluginId, String target) {
        List targs = (List)targets.get(pluginId);

        if (targs == null) {
            targs = new ArrayList();
            targets.put(pluginId, targs);
        }

        targs.add(target);
    }

    private void addDependency(RepoArtifactId parent, RepoArtifactId child) {
        addDependency(parent, new RepoArtifactId[] { child });
    }

    private void addDependency(RepoArtifactId parent, RepoArtifactId child1, RepoArtifactId child2) {
        addDependency(parent, new RepoArtifactId[] { child1, child2 });
    }

    private void addDependency(RepoArtifactId parent, RepoArtifactId[] children) {
        dependencies.put(parent, Arrays.asList(children));
    }

    private Dependency addProjectDependency(RepoArtifactId id) {
        Dependency dependency = new Dependency();
        dependency.setId(id);
        projectModel.getProject().getDependencySet().addDependency(dependency);

        return dependency;
    }

    private PluginDependency addPluginDependency(RepoArtifactId id, String[] targets, RepoPathSpec pathSpec) {
        PluginDependency pluginDependency = new PluginDependency();

        for (int i = 0; i < targets.length; i++) {
            String target = targets[i];
            pluginDependency.addTarget(new PluginDependencyTarget(target));
            addPluginTarget(id, target);
        }

        if (pathSpec != null) {
            pluginDependency.addPathSpec(pathSpec);
        }

        pluginDependency.setId(id);
        projectModel.getProject().getDependencySet().addDependency(pluginDependency);

        return pluginDependency;
    }

    private PluginDependency addPluginDependency(RepoArtifactId id, String[] targets) {
        return addPluginDependency(id, targets, null);
    }

    private void assertContainsTarget(Collection targets, String name) {
        name = "ns:" + name;

        for (Iterator i = targets.iterator(); i.hasNext();) {
            String target = (String)i.next();

            if (target.equals(name)) {
                return;
            }
        }

        fail("Targets do not include: " + name);
    }

    private String pathSpecKey(RepoArtifactId parent, RepoArtifactId child) {
        return parent.toShortString() + "$$$" + child.toShortString();
    }

    private void addPathSpec(RepoArtifactId parent, RepoArtifactId child, RepoPathSpec pathSpec) {
        String key = pathSpecKey(parent, child);
        List specs = (List)pathSpecs.get(key);

        if (specs == null) {
            specs = new ArrayList();
            pathSpecs.put(key, specs);
        }

        specs.add(pathSpec);
    }

    private void addPathGroup(RepoArtifactId parent, String target, PathGroup pathGroup) {
        String key = pathGroupKey(parent, target);
        List groups = (List)pathGroups.get(key);

        if (groups == null) {
            groups = new ArrayList();
            pathGroups.put(key, groups);
        }

        groups.add(pathGroup);
    }

    private String pathGroupKey(RepoArtifactId parent, String target) {
        return parent.toShortString() + "$$$" + target;
    }

    private void addPath(RepoArtifactId id, RepoPath path) {
        List paths = (List)this.paths.get(id);

        if (paths == null) {
            paths = new ArrayList();
            this.paths.put(id, paths);
        }

        paths.add(path);
    }

    private List getPaths(RepoArtifactId id) {
        List paths = (List)this.paths.get(id);

        if (paths == null) {
            return defaultPaths;
        }

        return paths;
    }

    private List getPathSpecs(RepoArtifactId parent, RepoArtifactId child) {
        List specs = (List)pathSpecs.get(pathSpecKey(parent, child));

        if (specs == null) {
            // Copy defaults ... necessary as they have a dependency reference
            List defaults = new ArrayList();

            for (Iterator i = defaultPathSpecs.iterator(); i.hasNext();) {
                RepoPathSpec pathSpec = (RepoPathSpec)i.next();
                defaults.add(new PathSpec(pathSpec.getFrom(), pathSpec.getTo(), pathSpec.isDescend(),
                        pathSpec.isMandatory()));
            }

            return defaults;
        }

        return specs;
    }

    private List getPathGroups(RepoArtifactId parent, String target) {
        List groups = (List)pathGroups.get(pathGroupKey(parent, target));

        if (groups == null) {
            return defaultPathGroups;
        }

        return groups;
    }

    public void testApplyProfiles() {
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.put("key1", "value1");
        properties.put("[p1]key2", "value2");
        properties.put("[p1, p2]key3", "value3");

        Set activeProfiles = new HashSet();
        AnnotatedProperties applied = DefaultProjectModel.applyProfiles(properties, activeProfiles);
        assertEquals(1, applied.size());
        assertEquals("value1", applied.get("key1"));

        activeProfiles.add("p2");
        applied = DefaultProjectModel.applyProfiles(properties, activeProfiles);
        assertEquals(2, applied.size());
        assertEquals("value1", applied.get("key1"));
        assertEquals("value3", applied.get("key3"));

        activeProfiles.clear();
        activeProfiles.add("p1");
        applied = DefaultProjectModel.applyProfiles(properties, activeProfiles);
        assertEquals(3, applied.size());
        assertEquals("value1", applied.get("key1"));
        assertEquals("value2", applied.get("key2"));
        assertEquals("value3", applied.get("key3"));
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public class MockRepository extends AbstractRepository {
        public void initialise(Object project, AnnotatedProperties properties) {
        }

        public RepoArtifact resolve(RepoArtifactId id) {
            RepoArtifact artifact = new RepoArtifact(id);
            List paths1 = getPaths(id);

            for (Iterator i = paths1.iterator(); i.hasNext();) {
                RepoPath path = (RepoPath)i.next();
                artifact.addPath(path);
            }

            List deps = (List)dependencies.get(id);

            if (deps != null) {
                for (Iterator i = deps.iterator(); i.hasNext();) {
                    RepoArtifactId dependencyId = (RepoArtifactId)i.next();
                    RepoDependency dependency = new RepoDependency();
                    dependency.setId(dependencyId);

                    for (Iterator j = getPathSpecs(id, dependencyId).iterator(); j.hasNext();) {
                        RepoPathSpec pathSpec = (RepoPathSpec)j.next();
                        dependency.addPathSpec(pathSpec);
                    }

                    artifact.addDependency(dependency);
                }
            }

            return artifact;
        }

        public void install(RepoArtifact artifact) {
        }

        public boolean exists(RepoArtifactId artifactId) {
            return false;
        }

        public void remove(RepoArtifactId artifactId) {
        }

        public Collection listArtifactIds() {
            return null;
        }

        public boolean supportsReslove(RepoArtifactId artifactId) {
            return true;
        }

        public boolean supportsInstall(RepoArtifactId artifactId) {
            return false;
        }

        public String getName() {
            return "name";
        }

        public Collection getReferencedRepositories() {
            return Collections.EMPTY_SET;
        }
    }

    public class MockPluginParser implements PluginParser {
        public Plugin getPluginInstance(RepoArtifact artifact) {
            Plugin plugin = new Plugin();
            plugin.setArtifact(artifact);

            List targs = (List)targets.get(artifact.getId());

            if (targs == null) {
                fail("Targets have not been defined for: " + artifact.getId());
            }

            for (Iterator i = targs.iterator(); i.hasNext();) {
                String targetName = (String)i.next();
                Target target = new Target();
                target.setName("ns:" + targetName);
                plugin.addTarget(target);
                plugin.setNameSpace("ns");

                for (Iterator j = getPathGroups(artifact.getId(), targetName).iterator(); j.hasNext();) {
                    PathGroup pathGroup = (PathGroup)j.next();
                    target.addPathGroup(pathGroup);
                }
            }

            return plugin;
        }
    }
}
