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

import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.version.Version;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 */
public class UrlRepositoryTest extends AbstractRepositoryTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        name = "url";
    }

    public void testIndexing() throws InterruptedException, IOException {
        deleteOutputDir();

        File index = new File(getOutputDir(), "index");
        File indexArchive = new File(index, "_index.zip");
        FileUtils.getFileUtils().copyFile(getTestCaseResource("index.zip"), indexArchive);

//        indexArchive.setLastModified(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
        put("root", "http://quokka.ws/repository");
        put("index", index.getPath());
        initialise();
        ((UrlRepository)repository).extractIndex(); // Needed as we've moved the index file in by stealth

        Set artifacts = new HashSet();

        for (Iterator i = repository.listArtifactIds(false).iterator(); i.hasNext();) {
            RepoArtifactId id = (RepoArtifactId)i.next();
            RepoArtifact artifact = repository.resolve(id, false);
            artifacts.add(id.toShortString() + ": " + artifact.getHash());
        }

        assertEquals(6, artifacts.size());
        assertTrue(artifacts.contains("group1:name1:jar:version1: d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(artifacts.contains("group1:name2:jar:version1: d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(artifacts.contains("group1:name1:jar:version1~2: d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(artifacts.contains("group1:name2:jar:version1~2: d41d8cd98f00b204e9800998ecf8427e"));
        assertTrue(artifacts.contains("group2.subgroup1:name3:paths:1.1: null"));
        assertTrue(artifacts.contains("group2.subgroup1:name3:paths:1.1~3: null"));
    }

    public void testRemoteResolve() {
        put("root", "http://quokka.ws/repository");
        initialise();

        RepoArtifact artifact = repository.resolve(new RepoArtifactId("apache.ant", "ant-launcher", "jar", "1.7"));
        assertNotNull(artifact);
        System.out.println(artifact);
    }
}
