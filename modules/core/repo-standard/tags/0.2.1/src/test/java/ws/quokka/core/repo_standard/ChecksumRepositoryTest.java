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
public class ChecksumRepositoryTest extends AbstractRepositoryTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        name = "checksum";
    }

    public void testResolveFromParents() {
        put("root", getTestCaseResource("repository").getAbsolutePath());
        put("hierarchical", "false");
        put("confirmImport", "false");
        put("parents", "parent");
        put("parent", "class", "file");
        put("parent", "root", getTestResource("FileRepositoryTest/hierarchical-repository").getAbsolutePath());
        initialise();

        RepoArtifactId id = new RepoArtifactId("group1", "name1", "jar", new Version("version1"));
        resolveArtifact(id, 1);
    }
}
