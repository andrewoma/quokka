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


package ws.quokka.core.repo_resolver;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap_util.MockLogger;
import ws.quokka.core.repo_spi.*;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.Strings;
import ws.quokka.core.version.Version;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.*;


/**
 *
 */
public class ResolverTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Resolver resolver;
    private MockRepository repo;
    private Map paths = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        repo = new MockRepository();
        resolver = new Resolver(repo, new MockLogger());
    }

    public void testResolvePathMandatoryDescend() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1");
    }

    public void testPrintPath() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");

        ResolvedPath path = resolver.resolvePath("root", root);
        assertEquals("Path: root\n" + "    dep1:dep1:jar:1.0\n" + "        dep1_1:dep1_1:jar:1.0\n"
            + "            dep1_1_1:dep1_1_1:jar:1.0\n" + "        dep1_2:dep1_2:jar:1.0\n"
            + "            dep1_2_1:dep1_2_1:jar:1.0\n", resolver.formatPath(path, false));
        assertEquals("", resolver.formatPath(path, true));
    }

    public void testCycleDetection() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");
        dep(get("dep1_1"), get("dep1"), "runtime");

        try {
            resolver.resolvePath("root", root);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().indexOf("Cycle detected") != -1);
        }
    }

    public void testOptionsWithMultipleLevels() {
        RepoArtifact root = createRoot();
        artifact("dep1");
        artifact("dep1_1");
        artifact("dep1_1_1");
        artifact("dep1_2");
        artifact("dep1_2_1");

        // Add a heap of optional dependencies
        dep(root, get("dep1"), "root+(dep1_1(dep1_1_1),dep1_2)");
        dep(get("dep1"), get("dep1_1"), "runtime+");
        dep(get("dep1"), get("dep1_2"), "runtime+");
        dep(get("dep1_2"), get("dep1_2_1"), "runtime+");
        dep(get("dep1_1"), get("dep1_1_1"), "runtime+");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2");
    }

    public void testExplicitOverride() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root(dep1_1:dep1_1(dep1_1_1@2.0))");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1:dep1_1_1:jar:2.0, dep1_2, dep1_2_1");
    }

    public void testPrintPathWithConflicts() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");
        dep(root, get("dep1:dep1:jar:2.0"), "root");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);

        try {
            resolver.merge(Collections.singleton(path));
            fail("Expected exception");
        } catch (BuildException e) {
            System.out.println(resolver.formatPath(path, true));
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testPrintPathWithConflictsMultiple() {
        createArtifacts1();

        RepoArtifact root = artifact("root");
        root.addPath(path("path1", true, true));
        dep(root, get("dep1"), "path1");
        dep(get("dep1"), get("dep3"), "runtime");

        root.addPath(path("path2", true, true));
        dep(root, get("dep2"), "path2");
        dep(get("dep2"), get("dep3:dep3:jar:2.0"), "runtime");

        ResolvedPath path1 = resolver.resolvePath("path1", root);
        printPath(path1);

        ResolvedPath path2 = resolver.resolvePath("path2", root);
        printPath(path2);

        Set paths = new HashSet();
        paths.add(path1);
        paths.add(path2);

        try {
            resolver.merge(paths);
            fail("Expected exception");
        } catch (BuildException e) {
            System.out.println(resolver.formatPaths(paths, true));
            assertTrue(e.getMessage().indexOf("Conflicts have occurred") != -1);
        }
    }

    public void testMerging() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");
        dep(get("dep1_1"), get("dep3"), "runtime"); // Duplicate dep3
        dep(get("dep1_2"), get("dep3"), "runtime");

        // Test with merging
        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertEquals(7, path.getArtifacts().size());
        path = resolver.merge(Collections.singleton(path));
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep3, dep1_2, dep1_2_1");
    }

    public void testResolvePathOptionalDescend() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root?<");

        ResolvedPath path = resolver.resolvePath("root", root);
        assertPath(path, "");
    }

    public void testResolvePathMandatoryNotDescend() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root!+");

        ResolvedPath path = resolver.resolvePath("root", root);
        assertPath(path, "dep1");
    }

    public void testResolvePathOptionalWithPathSpecs() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root!+(dep1_1)");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1");
    }

    public void testResolvePathOptionalWithPathSpecs2() {
        createArtifacts1();

        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root!+(dep1_1, dep1_2)");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1");
    }

    public void testResolvePathOptionalWithMultiplePaths() {
        createArtifacts1();

        RepoArtifact root = artifact("root");
        root.addPath(path("path1", true, true));
        root.addPath(path("path2", true, true));
        dep(root, get("dep1"), "path1+");
        dep(root, get("dep1"), "path2");

        ResolvedPath path1 = resolver.resolvePath("path1", root);
        printPath(path1);
        assertPath(path1, "dep1");

        ResolvedPath path2 = resolver.resolvePath("path2", root);
        printPath(path2);
        assertPath(path2, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1");
    }

    public void testOverrideVersion() {
        createArtifacts1();

        // Override a simple leaf version
        //    dep1:dep1:jar:1.0
        //        dep1_1:dep1_1:jar:1.0
        //            dep1_1_1:dep1_1_1:jar:1.0
        //        dep1_2:dep1_2:jar:1.0
        //            dep1_2_1:dep1_2_1:jar:1.0
        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");
        artifact("dep1_2_1:dep1_2_1:jar:2.0");
        override(root, "root", "dep1_2_1", "1.0", "2.0", null);

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1:dep1_2_1:jar:2.0");

        // Override the version occuring throughout the tree
        root = createRoot();
        dep(root, get("dep1"), "root");
        dep(get("dep1_1"), get("dep1_2_1"), "runtime"); // Add to 1.2.1 to 1.1
        override(root, "root", "dep1_2_1", "1.0", "2.0", null);
        path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1:dep1_2_1:jar:2.0");

        // Test it doesn't alter other paths
        root = createRoot();
        root.addPath(path("root2", true, true));
        dep(root, get("dep1"), "root");
        dep(root, get("dep1"), "root2");
        artifact("dep1_2_1:dep1_2_1:jar:2.0");
        override(root, "root", "dep1_2_1", "1.0", "2.0", null);
        path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1:dep1_2_1:jar:2.0");
        path = resolver.resolvePath("root2", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_2, dep1_2_1");
    }

    public void testOverridePathSpecification() {
        createArtifacts1();

        // Override a simple leaf version
        //    dep1:dep1:jar:1.0
        //        dep1_1:dep1_1:jar:1.0
        //            dep1_1_1:dep1_1_1:jar:1.0
        //        dep1_2:dep1_2:jar:1.0
        //            dep1_2_1:dep1_2_1:jar:1.0
        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");

        // Override the path specification to make it not descend by default an include dep1_1 only
        override(root, "root", "dep1", "1.0", null, "+(dep1_1)");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1");

        // Repeat, changing version at the same time
        root = createRoot();
        dep(root, get("dep1"), "root");

        // Override the path specification to make it not descend and change the version
        artifact("dep1:dep1:jar:2.0");
        override(root, "root", "dep1", "1.0", "2.0", "+");
        path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1:dep1:jar:2.0");
    }

    public void testOverridePathSpecificationWithinTree() {
        createArtifacts1();

        // Override a simple leaf version
        //    dep1:dep1:jar:1.0
        //        dep1_1:dep1_1:jar:1.0
        //            dep1_1_1:dep1_1_1:jar:1.0
        //        dep1_2:dep1_2:jar:1.0
        //            dep1_2_1:dep1_2_1:jar:1.0
        RepoArtifact root = createRoot();
        dep(root, get("dep1"), "root");

        // Add an optional dependency from 1_1 -> 1_1_2
        artifact("dep1_1_2");
        dep(get("dep1_1"), get("dep1_1_2"), "runtime?+");

        // Override dep1_1 to include the new dependency
        override(root, "root", "dep1_1", null, null, "(dep1_1_2)");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_1_2, dep1_2, dep1_2_1");
    }

    public void testOverridePathSpecificationWithinTreeWithUniquePaths() {
        // Create a tree with unique paths to test the chaining of froms and to is correct
        RepoArtifact root = artifact("root", path("root", true, true));
        artifact("root", path("root", true, true));
        artifact("dep1", path("dep1", true, true));
        artifact("dep1_1", path("dep1_1", true, true));
        artifact("dep1_1_1", path("dep1_1_1", true, true));
        artifact("dep1_1_2", path("dep1_1_2", true, true));
        artifact("dep2", path("dep2", true, true));

        dep(root, get("dep1"), "root=dep1");
        dep(get("dep1"), get("dep1_1"), "dep1=dep1_1");
        dep(get("dep1_1"), get("dep1_1_1"), "dep1_1=dep1_1_1");
        dep(get("dep1_1"), get("dep1_1_2"), "dep1_1?=dep1_1_2"); // Optional
        dep(root, get("dep2"), "root=dep2");

        // Override dep1_1 to include dep1_1_2
        override(root, "root", "dep1_1", null, null, "dep1_1(dep1_1_2)");

        ResolvedPath path = resolver.resolvePath("root", root);
        printPath(path);
        assertPath(path, "dep1, dep1_1, dep1_1_1, dep1_1_2, dep2");
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

    private RepoArtifact createRoot() {
        RepoArtifact root = artifact("root");
        root.addPath(path("root", true, true));

        return root;
    }

    private RepoArtifact get(String id) {
        return repo.resolve(id(id));
    }

    private void createArtifacts1() {
//      This method creates the following structure
//        dep1:dep1:jar:1.0
//            dep1_1:dep1_1:jar:1.0
//                dep1_1_1:dep1_1_1:jar:1.0
//            dep1_2:dep1_2:jar:1.0
//                dep1_2_1:dep1_2_1:jar:1.0
//        dep2:dep2:jar:1.0
//            dep2_1:dep2_1:jar:1.0
//        dep3:dep3:jar:1.0
//        dep3:dep3:jar:2.0
//            dep3_1:dep3_1:jar:2.0
        artifact("dep1");
        artifact("dep1:dep1:jar:2.0");
        artifact("dep1_1");
        artifact("dep1_1_1");
        artifact("dep1_1_1:dep1_1_1:jar:2.0");
        artifact("dep1_2");
        artifact("dep1_2_1");
        artifact("dep2");
        artifact("dep2_1");
        artifact("dep3");
        artifact("dep3:dep3:jar:2.0");
        artifact("dep3_1:dep3_1:jar:2.0");

        // Add dependencies
        dep(get("dep1"), get("dep1_1"), "runtime");
        dep(get("dep1"), get("dep1_2"), "runtime");
        dep(get("dep1_2"), get("dep1_2_1"), "runtime");
        dep(get("dep1_1"), get("dep1_1_1"), "runtime");
        dep(get("dep2"), get("dep2_1"), "runtime");
        dep(get("dep3:dep3:jar:2.0"), get("dep3_1:dep3_1:jar:2.0"), "runtime");
    }

    private RepoPath path(String id, boolean descendDefault, boolean mandatoryDefault) {
        RepoPath path = new RepoPath(id, id, descendDefault, mandatoryDefault);
        paths.put(id, path);

        return path;
    }

    private void override(RepoArtifact artifact, String paths, String id, String version, String withVersion,
        String withPathSpec) {
        RepoOverride override = new RepoOverride();

        for (Iterator i = Strings.commaSepList(paths).iterator(); i.hasNext();) {
            override.addPath((String)i.next());
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

        artifact.addOverride(override);
    }

    private void dep(RepoArtifact parent, RepoArtifact child, String pathSpec) {
        RepoDependency dependency = new RepoDependency();
        dependency.setId(child.getId());

        RepoPathSpec spec = new RepoPathSpec(pathSpec);
        spec.mergeDefaults((RepoPath)paths.get(spec.getTo()));
        dependency.addPathSpec(spec);
        parent.addDependency(dependency);
    }

    private RepoArtifact artifact(String id) {
        RepoArtifact artifact = new RepoArtifact(id(id));
        artifact.addPath(path("runtime", true, true)); // Default path
        repo.add(artifact);

        return artifact;
    }

    private RepoArtifact artifact(String id, RepoPath path) {
        RepoArtifact artifact = new RepoArtifact(id(id));
        artifact.addPath(path); // Default path
        repo.add(artifact);

        return artifact;
    }

    private RepoArtifactId id(String id) {
        String[] tokens = Strings.split(id, ":");

        return new RepoArtifactId(tokens[0], (tokens.length > 1) ? tokens[1] : tokens[0],
            (tokens.length > 2) ? tokens[2] : "jar", (tokens.length > 3) ? tokens[3] : "1.0");
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

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    public class MockRepository extends AbstractRepository {
        private Map artifacts = new HashMap();

        public void add(RepoArtifact artifact) {
            artifacts.put(artifact.getId(), artifact);
        }

        public RepoArtifact resolve(RepoArtifactId id) {
            RepoArtifact artifact = (RepoArtifact)artifacts.get(id);

            if (artifact == null) {
                throw new UnresolvedArtifactException(id);
            }

            return artifact;
        }

        public void initialise() {
        }

        public void install(RepoArtifact artifact) {
        }

        public void remove(RepoArtifactId artifactId) {
        }

        public Collection listArtifactIds() {
            return null;
        }

        public boolean supportsReslove(RepoArtifactId artifactId) {
            return false;
        }

        public boolean supportsInstall(RepoArtifactId artifactId) {
            return false;
        }
    }
}
