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

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.MockLogger;
import ws.quokka.core.main.AbstractMainTest;
import ws.quokka.core.main.parser.PluginParser;
import ws.quokka.core.model.*;
import ws.quokka.core.model.Override;
import ws.quokka.core.repo_resolver.ResolvedPath;
import ws.quokka.core.repo_resolver.Resolver;
import ws.quokka.core.repo_spi.*;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.*;


/**
 *
 */
public class DefaultProjectModelTest extends AbstractMainTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultProjectModel model;
    private MockRepository repo;
    private Map paths = new HashMap();
    private Map plugins = new HashMap();
    private Project project;
    private Resolver resolver;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        model = new DefaultProjectModel();
        repo = new MockRepository();
        model.setRepository(repo);
        model.setPluginParser(new MockPluginParser());

        Project project = new Project();
        this.project = project;
        model.setProject(this.project);
        set = project.getDependencySet();

        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
        antProject.init();
        model.setAntProject(antProject);
        resolver = new Resolver(repo, new MockLogger(true, true, true, true, true));
        model.setPathResolver(resolver);
    }

    public void testResolveTargets() {
    }

    public void testResolveProjectPath() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addPath(new Path("path2", null, true, true));
        set.addPath(new Path("path3", null, true, true));
        set.addDependency(pdep("dep1", "path1"));
        set.addDependency(pdep("dep2", "path2"));
        set.addDependency(pdep("dep1", "path3+(dep11)"));
        model.initialise();

        ResolvedPath path = model.getReslovedProjectPath("path1", false, false, false);
        printPath(path);
        assertPath(path, "dep1, dep11, dep111, dep12, dep121");

        path = model.getReslovedProjectPath("path2", false, false, false);
        printPath(path);
        assertPath(path, "dep2, dep21");

        path = model.getReslovedProjectPath("path3", false, false, false);
        printPath(path);
        assertPath(path, "dep1, dep11, dep111");
    }

    public void testResolveProjectPathWithConflict() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addDependency(pdep("dep1", "path1"));
        set.addDependency(pdep("dep2", "path1"));
        dep(get("dep2"), get("dep1:dep1:jar:2.0"), "runtime");
        model.initialise();

        try {
            model.getReslovedProjectPath("path1", false, false, true);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testResolveProjectPathWithCoreConflict() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        artifact("quokka.core.bootstrap-util");
        set.addDependency(pdep("quokka.core.bootstrap-util", "path1"));
        model.initialise();

        try {
            model.getReslovedProjectPath("path1", true, true, true);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testResolveProjectPathWithCoreConflictOverridden() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        artifact("quokka.core.bootstrap-util:bootstrap-util:jar:0.1.1");
        set.addDependency(pdep("quokka.core.bootstrap-util", "path1"));
        set.addDependency(pdep("dep3", "path1"));
        model.getAntProject().setProperty("quokka.project.overrideCore", "true");
        model.initialise();

        ResolvedPath path = model.getReslovedProjectPath("path1", true, true, true);
        printPath(path);
        assertPath(path, "dep3");
    }

    public void testResolveProjectPathMergeWithCore() {
    }

    public void testResolveProjectPathWithOverride() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addDependency(pdep("dep1", "path1"));
        set.addOverride(override("dep1", "path1", "", "1.0", "2.0", null));
        model.initialise();

        ResolvedPath path = model.getReslovedProjectPath("path1", false, false, false);
        printPath(path);
        assertPath(path, "dep1:dep1:jar:2.0");
    }

    public void testResolveProjectPathWithOverrideWildcard() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addDependency(pdep("dep1", "path1"));
        set.addOverride(override("dep1", "*", "", "1.0", "2.0", null));
        model.initialise();

        ResolvedPath path = model.getReslovedProjectPath("path1", false, false, false);
        printPath(path);
        assertPath(path, "dep1:dep1:jar:2.0");
    }

    public void testResolveProjectPathWithOverridePathSpec() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addDependency(pdep("dep1", "path1"));
        set.addOverride(override("dep1", "path1", "", "1.0", null, "+(dep11)"));
        model.initialise();

        ResolvedPath path = model.getReslovedProjectPath("path1", false, false, false);
        printPath(path);
        assertPath(path, "dep1, dep11, dep111");
    }

    private Override override(String id, String paths, String pluginPaths, String version, String withVersion,
        String withPathSpec) {
        Override override = new Override();

        for (Iterator i = Strings.commaSepList(paths).iterator(); i.hasNext();) {
            override.addPath((String)i.next());
        }

        for (Iterator i = Strings.commaSepList(pluginPaths).iterator(); i.hasNext();) {
            override.addPluginPath((String)i.next());
        }

        String[] tokens = Strings.split(id, ":");
        override.setGroup(tokens[0]);
        override.setName((tokens.length > 1) ? tokens[1] : tokens[0]);
        override.setType((tokens.length > 2) ? tokens[2] : "jar");
        override.setVersion((version == null) ? null : VersionRangeUnion.parse(version));
        override.setWithVersion((withVersion == null) ? null : Version.parse(withVersion));

        if (withPathSpec != null) {
            RepoPathSpec spec = new RepoPathSpec(withPathSpec, false);
            override.addWithPathSpec(spec);
        }

        set.addOverride(override);

        return override;
    }

    public void testResolvePluginPath() {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        Target target = target(plugin, "target1", "", "group1=plugin.ppath1;group2=plugin,plugin.ppath1");
        artifact("plugin1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        model.initialise();

        // Test access to the path directly
        ResolvedPath path = model.getResolvedPluginPath(plugin, "ppath1", false, false, false);
        printPath(path);

        String expectedIds = "dep1, dep11, dep111, dep12, dep121";
        assertPath(path, expectedIds);

        // Test metadata interface
        path = new ResolvedPath("id", model.getPluginPath(plugin, "ppath1", false, false));
        assertPath(path, expectedIds);

        // Test access via the path group
        path = new ResolvedPath("id", model.resolvePathGroup(target, "group1"));
        assertPath(path, expectedIds);

        // Test access via the path group including plugin
        path = new ResolvedPath("id", model.resolvePathGroup(target, "group2"));
        assertPath(path, expectedIds + ", plugin1");
    }

    public void testResolvePluginPathMergeWithCore() {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        Target target = target(plugin, "target1", "", "group1=plugin.ppath1;group2=plugin.ppath1");
        target.getPathGroup("group2").setMergeWithCore(Boolean.FALSE); // Don't merge
        artifact("plugin1", path("ppath1", true, true));
        artifact("apache.ant:ant:jar:1.7.1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        dep(get("plugin1"), get("apache.ant:ant:jar:1.7.1"), "ppath1");
        model.initialise();

        ResolvedPath path = new ResolvedPath("id", model.resolvePathGroup(target, "group1"));
        printPath(path);
        assertPath(path, "dep1, dep11, dep111, dep12, dep121");

        // Test access via the path group including plugin
        path = new ResolvedPath("id", model.resolvePathGroup(target, "group2"));
        printPath(path);
        assertPath(path, "dep1, dep11, dep111, dep12, dep121, apache.ant:ant:jar:1.7.1");
    }

    public void testResolvePluginPathWithConflict() {
        createArtifacts1();
        set.addPath(new Path("path1", null, true, true));
        set.addDependency(pdep("dep1:dep1:jar:2.0", "path1")); // Will conflict with plugin path
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        Target target = target(plugin, "target1", "", "group1=plugin.ppath1,project.path1");
        artifact("plugin1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        model.initialise();

        // Test access via the path group
        try {
            model.resolvePathGroup(target, "group1");
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testResolvePluginPathWithCoreConflict() {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        Target target = target(plugin, "target1", "", "group1=plugin.ppath1");
        artifact("plugin1", path("ppath1", true, true));
        artifact("quokka.core.bootstrap-util");
        dep(get("plugin1"), get("quokka.core.bootstrap-util"), "ppath1");
        model.initialise();

        // Test access via the path group
        try {
            model.resolvePathGroup(target, "group1");
            fail("Expected exception");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testResolvePluginPathWithCoreConflictOverridden() {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        Target target = target(plugin, "target1", "", "group1=plugin.ppath1");

        artifact("plugin1", path("ppath1", true, true));
        artifact("quokka.core.bootstrap-util:bootstrap-util:jar:0.1.1");
        artifact("quokka.core.bootstrap-util");
        dep(get("plugin1"), get("quokka.core.bootstrap-util"), "ppath1");
        model.getAntProject().setProperty("quokka.project.overrideCore", "true");
        model.initialise();

        // Test access via the path group
        List path = model.resolvePathGroup(target, "group1");
        assertEquals(0, path.size());
    }

    public void testResolvePluginPathWithPathSpec() {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "ppath1(dep2)", "target1")); // Add optional dep2

        Plugin plugin = plugin("plugin1", "p1");
        target(plugin, "target1", "", "group1=plugin.ppath1");
        artifact("plugin1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        dep(get("plugin1"), get("dep2"), "ppath1?");
        model.initialise();

        ResolvedPath path = model.getResolvedPluginPath(plugin, "ppath1", false, false, false);
        printPath(path);
        assertPath(path, "dep1, dep11, dep111, dep12, dep121, dep2, dep21");
    }

    public void testResolvePluginPathWithOverride1() {
        pluginPathWithOverride("plugin1=ppath2", "dep1, dep11, dep111, dep12, dep121"); // Shouldn't match
    }

    public void testResolvePluginPathWithOverride2() {
        pluginPathWithOverride("*", "dep1:dep1:jar:2.0");
    }

    public void testResolvePluginPathWithOverride3() {
        pluginPathWithOverride("plugin1=ppath1", "dep1:dep1:jar:2.0");
    }

    private void pluginPathWithOverride(String paths, String expectedIds) {
        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        target(plugin, "target1", "", "group1=plugin.ppath1");
        artifact("plugin1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        set.addOverride(override("dep1", "", paths, "1.0", "2.0", null));
        model.initialise();

        ResolvedPath path = model.getResolvedPluginPath(plugin, "ppath1", false, false, false);
        printPath(path);
        assertPath(path, expectedIds);
    }

    public void testPathReferencedByProperty() {
        AnnotatedProperties properties = new AnnotatedProperties();
        properties.setProperty("prop1", "ppath1");
        model.getProject().setProperties(properties);

        createArtifacts1();
        set.addDependency(pldep("plugin1", "", "target1"));

        Plugin plugin = plugin("plugin1", "p1");
        target(plugin, "target1", "", "group1=property.prop1");
        artifact("plugin1", path("ppath1", true, true));
        dep(get("plugin1"), get("dep1"), "ppath1");
        model.initialise();

        // Test access to the path directly
        ResolvedPath path = model.getResolvedPluginPath(plugin, "ppath1", false, false, false);
        printPath(path);
        assertPath(path, "dep1, dep11, dep111, dep12, dep121");
    }

    private void createArtifacts1() {
//      This method creates the following structure
//        dep1:dep1:jar:1.0
//            dep11:dep11:jar:1.0
//                dep111:dep111:jar:1.0
//            dep12:dep12:jar:1.0
//                dep121:dep121:jar:1.0
//        dep2:dep2:jar:1.0
//            dep21:dep21:jar:1.0
//        dep3:dep3:jar:1.0
//        dep3:dep3:jar:2.0
//            dep31:dep31:jar:2.0
        artifact("dep1");
        artifact("dep1:dep1:jar:2.0");
        artifact("dep11");
        artifact("dep111");
        artifact("dep12");
        artifact("dep121");
        artifact("dep2");
        artifact("dep21");
        artifact("dep3");
        artifact("dep3:dep3:jar:2.0");
        artifact("dep31:dep31:jar:2.0");

        // Add dependencies
        dep(get("dep1"), get("dep11"), "runtime");
        dep(get("dep1"), get("dep12"), "runtime");
        dep(get("dep12"), get("dep121"), "runtime");
        dep(get("dep11"), get("dep111"), "runtime");
        dep(get("dep2"), get("dep21"), "runtime");
        dep(get("dep3:dep3:jar:2.0"), get("dep31:dep31:jar:2.0"), "runtime");
    }

    private RepoArtifact artifact(String id) {
        RepoArtifact artifact = new RepoArtifact(id(id));
        artifact.addPath(path("runtime", true, true)); // Default path
        repo.install(artifact);

        return artifact;
    }

    private RepoArtifact artifact(String id, RepoPath path) {
        RepoArtifact artifact = new RepoArtifact(id(id));
        artifact.addPath(path); // Default path
        repo.install(artifact);

        return artifact;
    }

    private RepoPath path(String id, boolean descendDefault, boolean mandatoryDefault) {
        RepoPath path = new RepoPath(id, id, descendDefault, mandatoryDefault);
        paths.put(id, path);

        return path;
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

    private void printPath(ResolvedPath path) {
        System.out.println(resolver.formatPath(path, false));
    }

    private void assertPath(ResolvedPath path, String ids) {
        path = resolver.merge(Collections.singleton(path));

        List fullIds = new ArrayList();

        for (Iterator i = Strings.commaSepList(ids).iterator(); i.hasNext();) {
            String id = (String)i.next();
            fullIds.add(id(id));
        }

        assertPathContains(path, (RepoArtifactId[])fullIds.toArray(new RepoArtifactId[fullIds.size()]));
    }

    private RepoArtifact get(String id) {
        return repo.resolve(id(id));
    }

    private void dep(RepoArtifact parent, RepoArtifact child, String pathSpec) {
        RepoDependency dependency = new RepoDependency();
        dependency.setId(child.getId());

        RepoPathSpec spec = new RepoPathSpec(pathSpec);
        spec.mergeDefaults((RepoPath)paths.get(spec.getTo()));
        dependency.addPathSpec(spec);
        parent.addDependency(dependency);
    }

    private Plugin plugin(String id, String nameSpace) {
        Plugin plugin = new Plugin();
        plugin.setNameSpace(nameSpace);
        plugins.put(id(id), plugin);

        return plugin;
    }

    private Target target(Plugin plugin, String name, String depends, String pathGroups) {
        Target target = new Target();
        plugin.addTarget(target);
        target.setName(plugin.getNameSpace() + ":" + name);
        target.setEnabledByDefault(true);

        for (Iterator i = Strings.asList(Strings.split(pathGroups, ";")).iterator(); i.hasNext();) {
            String pathGroup = (String)i.next();
            String[] tokens = Strings.split(pathGroup, "=");
            target.addPathGroup(new PathGroup(tokens[0], Strings.commaSepList(tokens[1]), Boolean.TRUE));
        }

        for (Iterator i = Strings.commaSepList(depends).iterator(); i.hasNext();) {
            String dependency = (String)i.next();
            target.addDependency(dependency);
        }

        return target;
    }

    private void assertPathContains(ResolvedPath resolvedPath, RepoArtifactId[] repoArtifactIds) {
        List path = resolvedPath.getArtifacts();
        assertEquals("Path contains wrong number of elements", repoArtifactIds.length, path.size());

        List ids = Arrays.asList(repoArtifactIds);

        for (Iterator i = path.iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            assertTrue(ids.contains(artifact.getId()));
        }
    }

    public void testResolveCorePath() {
        ResolvedPath path = model.resolveCorePath();
        printPath(path);

        List mustExist = new ArrayList();
        mustExist.add(id("quokka.core.bootstrap-util"));
        mustExist.add(id("quokka.core.main"));
        mustExist.add(id("apache.ant:ant"));

//        mustExist.add(id("apache.ant:bcel"));
        for (Iterator i = path.getArtifacts().iterator(); i.hasNext();) {
            RepoArtifact artifact = (RepoArtifact)i.next();
            RepoArtifactId artifactId = artifact.getId();

            for (Iterator j = mustExist.iterator(); j.hasNext();) {
                RepoArtifactId id = (RepoArtifactId)j.next();

                if (id.getGroup().equals(artifactId.getGroup()) && id.getName().equals(artifactId.getName())
                        && id.getType().equals(artifactId.getType())) {
                    j.remove();
                }
            }
        }

        Assert.isTrue(mustExist.size() == 0, "Required artifacts missing: " + mustExist);
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public class MockPluginParser implements PluginParser {
        public Plugin getPluginInstance(RepoArtifact artifact) {
            Plugin plugin = (Plugin)plugins.get(artifact.getId());
            plugin.setArtifact(artifact);
            Assert.isTrue(plugin != null, "Plugin does not exist: id=" + artifact.getId().toShortString());

            return plugin;
        }
    }
}
