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

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.version.Version;

import java.io.File;


/**
 *
 */
public class IndexedRepositoryTest extends AbstractRepositoryTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        name = "indexed";
    }

    public void test() throws InterruptedException {
        deleteOutputDir();
        put("root", "somefile");
        put("indexRoot", getOutputDir().getPath());
        put("somefile", "root", getTestResource("FileRepositoryTest/hierarchical-repository").getAbsolutePath());
        put("somefile", "class", "file");
        initialise();
        resolveArtifact(new RepoArtifactId("group1", "name1", "jar", new Version("version1")), 1);
        repository.rebuildCaches();

        RepoArtifactId newId = new RepoArtifactId("group5.subgroup", "name", "paths", "3.3");
        File out = new File(getOutputDir(), "_index.zip");

        try {
            repository.install(new RepoArtifact(newId));
            assertContainsEntries(out,
                new String[] {
                    "group1_version1_name1_jar.jar.MD5", "group1_version1_name1_jar_repository.xml",
                    "group5.subgroup_3.3_name_paths_repository.xml"
                });
        } finally {
            repository.remove(newId);
            assertNotContainsEntries(out, new String[] { "group5.subgroup_3.3_name_paths_repository.xml" });
        }
    }
}
