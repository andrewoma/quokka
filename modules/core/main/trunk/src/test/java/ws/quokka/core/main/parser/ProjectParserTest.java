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


package ws.quokka.core.main.parser;

import ws.quokka.core.bootstrap_util.ProjectLogger;
import ws.quokka.core.main.AbstractMainTest;
import ws.quokka.core.model.*;
import ws.quokka.core.repo_spi.*;
import ws.quokka.core.util.AnnotatedProperties;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.io.File;

import java.util.*;


/**
 *
 */
public class ProjectParserTest extends AbstractMainTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;
    private MockRepository repository;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testMin() {
        parse("min");
        assertEquals(1, project.getArtifacts().size());
        assertEquals(new Artifact("group", "name", "type", "version"), project.getArtifacts().iterator().next());
    }

    public void testMultipleArtifacts() {
        parse("multiple-artifacts");

        Set artifacts = project.getArtifacts();
        assertEquals(2, artifacts.size());
        assertTrue(artifacts.contains(new Artifact("group", "name1", "type", "version")));
        assertTrue(artifacts.contains(new Artifact("group", "name2", "type", "version")));
    }

    public void testDepSetSimple() {
        parse("depset-simple");

        DependencySet set = project.getDependencySet();
        List dependencies = set.getDependencies();
        assertEquals(3, dependencies.size());
        assertContains(dependencies, defaultDependency("bsh", "2.1"));
        assertContains(dependencies, defaultDependency("qdox", "1.5"));
        assertContains(dependencies, defaultPlugin("plugin", "1.0"));
    }

    private void assertContains(List dependencies, Dependency dependency) {
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Dependency dependency1 = (Dependency)i.next();

            if (dependency1.getId().equals(dependency.getId())) {
                return;
            }
        }

        fail("Missing dependency: " + dependency.getId().toShortString());
    }

    private PluginDependency defaultPlugin(String name, String version) {
        PluginDependency pluginDependency = new PluginDependency();
        pluginDependency.setId(new RepoArtifactId(name, null, "plugin", version).mergeDefaults());

        return pluginDependency;
    }

    private Dependency defaultDependency(String name, String version) {
        Dependency dependency = new Dependency();
        dependency.setId(new RepoArtifactId(name, null, null, version).mergeDefaults());

        return dependency;
    }

    public void testProfiles1Default() {
        parse("profiles1");
        assertTrue((project.getArtifacts().size() == 1) && artifactsContains("artifact1"));
        assertTrue((project.getDependencySet().getPaths().size() == 1) && pathsContains("runtime"));
        assertTrue((project.getDependencySet().getDependencies().size() == 2) && dependenciesContains("ant")
            && dependenciesContains("junitplugin"));

        PluginDependency plugin = (PluginDependency)getDependency("junitplugin");
        assertTrue((plugin.getTargets().size() == 1) && (plugin.getTarget("test3") != null));
    }

    public void testProfiles1Profile1() {
        parse("profiles1", new Profiles("p1"));
        assertTrue((project.getArtifacts().size() == 2) && artifactsContains("artifact1")
            && artifactsContains("artifact2"));
        assertTrue((project.getDependencySet().getPaths().size() == 2) && pathsContains("runtime")
            && pathsContains("test"));
        assertTrue((project.getDependencySet().getDependencies().size() == 3) && dependenciesContains("ant")
            && dependenciesContains("junitplugin") && dependenciesContains("trax"));

        PluginDependency plugin = (PluginDependency)getDependency("junitplugin");
        assertTrue((plugin.getTargets().size() == 2) && (plugin.getTarget("test3") != null)
            && (plugin.getTarget("test1") != null));
    }

    public void testProfiles1Profile1And2() {
        parse("profiles1", new Profiles("p1, p2"));
        assertTrue((project.getArtifacts().size() == 3) && artifactsContains("artifact1")
            && artifactsContains("artifact2") && artifactsContains("artifact3"));
        assertTrue((project.getDependencySet().getPaths().size() == 3) && pathsContains("runtime")
            && pathsContains("test") && pathsContains("compile"));
        assertTrue((project.getDependencySet().getDependencies().size() == 4) && dependenciesContains("ant")
            && dependenciesContains("junitplugin") && dependenciesContains("trax") && dependenciesContains("junit"));

        PluginDependency plugin = (PluginDependency)getDependency("junitplugin");
        assertTrue((plugin.getTargets().size() == 3) && (plugin.getTarget("test3") != null)
            && (plugin.getTarget("test1") != null) && (plugin.getTarget("test2") != null));
    }

    public void testParseFull() {
        repository = new MockRepository();

        RepoArtifact artifact = new RepoArtifact(id("nested:name:jar:1.2"));
        artifact.setLocalCopy(getTestCaseResource("nested"));
        repository.install(artifact);

        parse("full2", new Profiles("-skip"));
        assertEquals("name", project.getName());
        assertEquals("default-target", project.getDefaultTarget());
        assertEquals("Some project", project.getDescription());

        // Artifacts
        Set artifacts = new HashSet();
        artifacts.add(artifact("group.subgroup:pack1:jar:0.1", null, "path1:path1"));
        artifacts.add(artifact("group.subgroup:pack2:war:0.1", "Some artifact", "path1:something"));
        artifacts.add(artifact("group.subgroup:pack4:jar:0.1", "pack4 description", ""));

        Set results = project.getArtifacts();
        assertEquals(artifacts, results);

        // Overrides
        DependencySet dependencySet = project.getDependencySet();
        List overrides = dependencySet.getOverrides();
        assertEquals(2, overrides.size());

        ws.quokka.core.model.Override override = (ws.quokka.core.model.Override)overrides.get(0);
        assertEquals("quokka.plugin.javac", override.getGroup());
        assertEquals("javac", override.getName());
        assertEquals(Collections.singleton("*"), override.getPaths());
        assertEquals(Collections.singleton("plugin:path1"), override.getPluginPaths());
        assertEquals("war", override.getType());
        assertEquals(VersionRangeUnion.parse("1.0"), override.getVersion());
        assertEquals(Version.parse("2.0"), override.getWithVersion());
        assertEquals(1, override.getWithPathSpecs().size());
        assertEquals(new PathSpec("blah(blah)", false), override.getWithPathSpecs().iterator().next());

        override = (ws.quokka.core.model.Override)overrides.get(1);
        assertEquals("quokka.plugin.jee", override.getGroup());
        assertEquals(null, override.getName());
        assertEquals(Collections.EMPTY_SET, override.getPaths());
        assertEquals(Collections.EMPTY_SET, override.getPluginPaths());
        assertEquals(null, override.getType());
        assertEquals(null, override.getVersion());
        assertEquals(null, override.getWithVersion());
        assertEquals(new PathSpec("!+from(hello, there)", false), override.getWithPathSpecs().iterator().next());

        // Profiles
        Profile profile = (Profile)dependencySet.getProfiles().get(0);
        assertEquals("skip", profile.getId());
        assertEquals("Skip description", profile.getDescription());
        profile = (Profile)dependencySet.getProfiles().get(1);
        assertEquals("profile2", profile.getId());
        assertEquals("Another profile", profile.getDescription());
        assertTrue(project.getActiveProfiles().getElements().contains("-skip"));
        assertTrue(project.getActiveProfiles().getElements().contains("source1.4"));

        // Paths
        Map paths = dependencySet.getPaths();
        assertEquals(3, paths.size());
        assertEquals(new Path("path1", "path1 description", true, true), paths.get("path1"));
        assertEquals(new Path("path2", "path2 description", false, false), paths.get("path2"));
        assertEquals(new Path("path3", "path3 description", true, true), paths.get("path3"));

        // Properties
        AnnotatedProperties props = new AnnotatedProperties();
        props.put("prop1", "value1");
        props.put("prop2", "value2");
        props.put("prop3", "value3");
        props.put("prop5", "Hello there");
        props.put("quokka.project.java.source", "1.4");
        props.put("t1prefix.prop1", "value1");
        props.put("t1prefix.prop2", "value2");
        assertEquals(props, dependencySet.getProperties());

        // Dependencies (includes plugins)
        List dependencies = new ArrayList();
        dependencies.add(pdep("some.plugin:plugin:jar:0.2", "path1"));
        dependencies.add(pdep("some.plugin:blah:jar:0.2", "path1"));
        dependencies.add(pdep("quokka.core.repo-standard", "path2+(opt1,opt2)"));
        dependencies.add(pdep("quokka.core.repo-resolver:somename:type1:0.1.1-ss", "path2+"));
        dependencies.add(pdep("some.dep1", "to?+from(opt1, opt2)"));

        dependencies.add(pldep("quokka.plugin1:plugin1:plugin", "", ""));
        dependencies.add(pldep("quokka.plugin3:plugin3:plugin", "path1(opt1)", "target1, target2"));

        PluginDependency dependency = pldep("quokka.plugin4:plugin4:plugin", "?+from(opt3, opt4)", "t1");
        PluginDependencyTarget target = dependency.getTarget("t1");
        target.setAlias("t1alias");
        target.setPrefix("t1prefix");
        target.setTemplate("t1template");
        target.addDependency("ta");
        target.addDependency("tb");
        dependencies.add(dependency);
        assertEquals(dependencies, dependencySet.getDependencies());

        // Licenses
        assertEquals(3, dependencySet.getLicenses().size());
        assertEquals(new License(null, new RepoArtifactId("license.apache", "apache", "license", "2.0")),
            dependencySet.getLicenses().get(0));
        assertEquals(new License(new File("NOTICE.txt"), new RepoArtifactId(null, null, "license", (Version)null)),
            dependencySet.getLicenses().get(1));
        assertEquals(new License(new File("license.txt"), new RepoArtifactId(null, "somename", "license", (Version)null)),
            dependencySet.getLicenses().get(2));

        List subsets = dependencySet.getSubsets();
        assertEquals(1, subsets.size());
        props = new AnnotatedProperties();
        props.put("nestedfileprop1", "value1");
        props.put("nestedprop1", "value1");

        DependencySet subset = (DependencySet)subsets.iterator().next();
        assertEquals(props, subset.getProperties());
    }

    public void testGetProperties() {
        Map antProps = new HashMap();
        antProps.put("antprop1", "value1");

        AnnotatedProperties properties = ProjectParser.getProjectProperties(getTestCaseResource("full2-quokka.xml"),
                antProps);
        assertEquals("value1", properties.get("antprop1"));
        assertEquals("value1", properties.get("projectprop1"));

//        properties.dump(System.out);
    }

    private Dependency getDependency(String name) {
        for (Iterator i = project.getDependencySet().getDependencies().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency)i.next();

            if (dependency.getId().getName().equals(name)) {
                return dependency;
            }
        }

        return null;
    }

    private boolean dependenciesContains(String name) {
        return getDependency(name) != null;
    }

    private boolean pathsContains(String id) {
        return project.getDependencySet().getPaths().get(id) != null;
    }

    private Artifact artifact(String id, String description, String pathMappings) {
        Artifact artifact = new Artifact(id(id));
        artifact.setDescription(description);

        for (Iterator i = Strings.commaSepList(pathMappings).iterator(); i.hasNext();) {
            String mapping = (String)i.next();
            String[] tokens = Strings.split(mapping, ":");
            artifact.addExportedPath(tokens[0], tokens[1]);
        }

        return artifact;
    }

    private boolean artifactsContains(String name) {
        for (Iterator i = project.getArtifacts().iterator(); i.hasNext();) {
            Artifact artifact = (Artifact)i.next();

            if (artifact.getId().getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void parse(String projectFile) {
        parse(projectFile, new Profiles((String)null));
    }

    private void parse(String projectFile, Profiles profiles) {
        org.apache.tools.ant.Project antProject = new org.apache.tools.ant.Project();
        antProject.init();

        ProjectParser parser = new ProjectParser(getTestCaseResource(projectFile + "-quokka.xml"), profiles,
                repository, true, new AnnotatedProperties(), new ProjectLogger(antProject));
        project = parser.parse();
    }

    public void testGetProjectProperties() {
        Map antProperties = new HashMap();
        antProperties.put("prop3", "antvalue3");
        antProperties.put("prop6", "antvalue6");
        antProperties.put("prop7", "antvalue7");

        AnnotatedProperties properties = ProjectParser.getProjectProperties(getTestCaseResource(
                    "properties-test-quokka.xml"), antProperties);
        assertEquals("xmlvalue1", properties.get("prop1"));
        assertEquals("xmlvalue2", properties.get("prop2"));
        assertEquals("antvalue3", properties.get("prop3"));
        assertEquals("filevalue4", properties.get("prop4"));
        assertEquals("filevalue5", properties.get("prop5"));
        assertEquals("antvalue6", properties.get("prop6"));
        assertEquals("antvalue7", properties.get("prop7"));

//        properties.dump(System.out);
    }
}
