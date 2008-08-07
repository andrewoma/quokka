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


package ws.quokka.core.repo_standard;

import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.version.Version;


/**
 *
 */
public class FileRepositoryTest extends AbstractRepositoryTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        name = "file";
    }

    public void testResolveArtifactHierachical() {
        put("root", getTestCaseResource("hierarchical-repository").getAbsolutePath());
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1")), 1);
    }

    public void testResolveArtifactHierachicalWithRepoVersion() {
        put("root", getTestCaseResource("hierarchical-repository").getAbsolutePath());
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1~2")), 1);
    }

    public void testResolveArtifactFlat() {
        put("root", getTestCaseResource("flat-repository").getAbsolutePath());
        put("hierarchical", "false");
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1")), 1);
    }

    public void testResolvePathsFlat() {
        put("root", getTestCaseResource("flat-repository").getAbsolutePath());
        put("hierarchical", "false");
        initialise();
        resolveArtifact(new RepoArtifactId("group2", "name1", "paths", new Version("2.0")), 1);
    }

    public void testResolveArtifactFlatWithRepoVersion() {
        put("root", getTestCaseResource("flat-repository").getAbsolutePath());
        put("hierarchical", "false");
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1~2")), 1);
    }

    public void testResolveArtifactFlatWithUrl() {
        String root = getTestCaseResource("flat-repository").getAbsolutePath();
        put("url", "file:" + root + ";hierarchical=false");
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1")), 1);
    }

    public void testResolveFromParents() {
        put("root", getTestCaseResource("flat-repository").getAbsolutePath());
        put("hierarchical", "false");
        put("parents", "parent");
        put("confirmImport", "false");
        put("parent", "class", "file");
        put("parent", "root", getTestCaseResource("hierarchical-repository").getAbsolutePath());

        RepoArtifactId id = new RepoArtifactId("group1", "name2", "jar", new Version("version1"));
        initialise();
        remove(id);

        try {
            resolveArtifact(id, 0);
        } finally {
            remove(id);
        }
    }
}
