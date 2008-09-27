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
import ws.quokka.core.repo_spi.RepoType;


/**
 *
 */
public class BundledRepositoryTest extends AbstractRepositoryTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        name = "bundle";
    }

    public void testGetArtifactId() {
        BundledRepository repo = new BundledRepository();
        assertEquals(new RepoArtifactId("group.name", "name", "jar", "1.1"), repo.getArtifactId("group.name:1.1"));
        assertEquals(new RepoArtifactId("name", "name", "jar", "1.1"), repo.getArtifactId("name:1.1"));
        assertEquals(new RepoArtifactId("group.sub", "name", "jar", "1.1"), repo.getArtifactId("group.sub:name:1.1"));
    }

    public void testResolveArtifact() {
        put("repository", "bundle");
        put("artifact", "mygroup:bundle1:1.0");
        properties.put("q.repo.bundle.class", "file");
        properties.put("q.repo.bundle.hierarchical", "false");
        properties.put("q.repo.bundle.root", getTestCaseResource("repository").getAbsolutePath());
        initialise();
        factory.registerType(new RepoType("jar", "jar", "jar"));
        resolveArtifact(new RepoArtifactId("apache.ant", "trax", "jar", "1.7"), 0);
    }
}
