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
import ws.quokka.core.model.Artifact;
import ws.quokka.core.model.Dependency;
import ws.quokka.core.model.DependencySet;
import ws.quokka.core.model.PluginDependency;
import ws.quokka.core.model.Profiles;
import ws.quokka.core.model.Project;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.AnnotatedProperties;

import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class ProjectParserTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project;

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
        assertContains(dependencies, defaultDependency("plugin", "1.0"));
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
        pluginDependency.setId(new RepoArtifactId(null, name, "quokka-plugin", version).mergeDefaults());

        return pluginDependency;
    }

    public void testParseShortHand1() {
        //        ProjectParser parser = new ProjectParser(null, null, null, true, null, null);
        //        PathSpec pathSpec = new PathSpec("path1", "runtime", Boolean.TRUE, Boolean.TRUE);
        //        pathSpec.setOptions("bsh,qdox");
        //        assertEquals(pathSpec, parser.parseShorthand("path1 < runtime(bsh,qdox)"));
        //        assertEquals(pathSpec, parser.parseShorthand("path1<runtime(bsh,qdox)"));
        //        assertEquals(pathSpec, parser.parseShorthand("  path1  <   runtime   (  bsh,qdox  )  "));
    }

    public void testParseShortHand2() {
        //        ProjectParser parser = new ProjectParser(null, null, null, true, null, null);
        //        PathSpec pathSpec = new PathSpec("path1", null, Boolean.TRUE, Boolean.TRUE);
        //        assertEquals(pathSpec, parser.parseShorthand("path1"));
        //        assertEquals(pathSpec, parser.parseShorthand("  path1  "));
        //        assertEquals(pathSpec, parser.parseShorthand("path1<"));
        //        assertEquals(pathSpec, parser.parseShorthand("  path1  <  "));
    }

    public void testParseShortHand3() {
        //        ProjectParser parser = new ProjectParser(null, null, null, true, null, null);
        //        PathSpec pathSpec = new PathSpec("path1", null, Boolean.FALSE, Boolean.TRUE);
        //        assertEquals(pathSpec, parser.parseShorthand("path1+"));
        //        assertEquals(pathSpec, parser.parseShorthand("  path1+  "));
        //        assertEquals(pathSpec, parser.parseShorthand("path1+"));
        //        assertEquals(pathSpec, parser.parseShorthand("  path1  +  "));
    }

    public void testParseShortHand4() {
        //        ProjectParser parser = new ProjectParser(null, null, null, true, null, null);
        //        PathSpec pathSpec = new PathSpec("path1", null, Boolean.FALSE, Boolean.FALSE);
        //        assertEquals(pathSpec, parser.parseShorthand("path1?+"));
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

        ProjectParser parser = new ProjectParser(getTestCaseResource(projectFile + "-quokka.xml"), profiles, null,
                true, new AnnotatedProperties(), new ProjectLogger(antProject));
        project = parser.parse();
    }
}
