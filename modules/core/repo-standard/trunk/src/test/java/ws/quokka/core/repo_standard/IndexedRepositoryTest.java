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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Untar;

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
        File out = new File(getOutputDir(), "_index.tar.bz2");
        System.out.println(out.getPath());

        File extractedDir = new File(getOutputDir(), "untarred-index");

        try {
            repository.install(new RepoArtifact(newId));
            untar(out, extractedDir);

            assertExists(extractedDir, "group1/version1/name1_jar.jar.MD5");
            assertExists(extractedDir, "group1/version1/name1_jar_repository.xml");
            assertExists(extractedDir, "group5/subgroup/3.3/name_paths_repository.xml");
        } finally {
            repository.remove(newId);
            delete(extractedDir);
            untar(out, extractedDir);
            assertNotExists(extractedDir, "group5/subgroup/3.3/name_paths_repository.xml");
        }
    }

    private void untar(File out, File extractedDir) {
        Project project = new Project();
        project.init();

        Untar untar = new Untar();
        untar.setProject(project);
        untar.setSrc(out);
        untar.setDest(extractedDir);

        Untar.UntarCompressionMethod method = new Untar.UntarCompressionMethod();
        method.setValue("bzip2");
        untar.setCompression(method);
        untar.execute();
    }

    private void assertNotExists(File extractedDir, String path) {
        assertNotExists(new File(normalise(new File(extractedDir, path).getPath())));
    }

    private void assertExists(File extractedDir, String path) {
        assertExists(new File(normalise(new File(extractedDir, path).getPath())));
    }
}
