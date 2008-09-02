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

import ws.quokka.core.repo_spi.RepoArtifact;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.test.AbstractTest;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class ResolvedPathTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testContains() {
        List artifacts = new ArrayList();
        RepoArtifactId id1 = new RepoArtifactId("group1", "name1", "type1", "1.0");
        artifacts.add(new RepoArtifact(id1));

        RepoArtifactId id2 = new RepoArtifactId("group1", "name1", "type1", "2.0");
        artifacts.add(new RepoArtifact(id2));

        ResolvedPath path = new ResolvedPath("id", artifacts);
        assertTrue(path.contains(id1));
        assertTrue(path.contains(id2));
        assertTrue(path.contains(new RepoArtifactId("group1", "name1", "type1", "2.0")));
        assertTrue(!path.contains(new RepoArtifactId("group1", "name1", "type1", "3.0")));
    }
}
