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


package ws.quokka.core.model;

import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.Strings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 */
public class OverrideTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private RepoArtifactId id1;
    private RepoArtifactId id2;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        id1 = new RepoArtifactId("group1", "name1", "type1", "1.0");
        id2 = new RepoArtifactId("group2.name2", "name2", "type1", "1.0");
    }

    public void testMatchingPluginPath() {
        assertMatching("*", id1, "*"); // Global wildcard
        assertMatching("group1:name1=*", id1, "*"); // Plugin specific wildcard
        assertMatching("group2.name2=*", id1, ""); // Shouldn't match
        assertMatching("group2.name2=*", id2, "*"); // Wildcard, no name
        assertMatching("group2.name2=path", id2, "path"); // Path, no name
        assertMatching("group2.name2=path1:path2", id2, "path1, path2"); // Path, no name
        assertMatching("group1:name1=path1:path2", id1, "path1, path2"); // Paths with name
        assertMatching("group1:name1=path1:path2, group2.name2=path3:path4", id1, "path1, path2"); // Multiple
        assertMatching("group1:name1=path1:path2, group2.name2=path3:path4", id2, "path3, path4"); // Multiple
    }

    private void assertMatching(String paths, RepoArtifactId id, String expectedPaths) {
        Override override = new Override();

        for (Iterator i = Strings.commaSepList(paths).iterator(); i.hasNext();) {
            String path = (String)i.next();
            override.addPluginPath(path);
        }

        Set expected = new HashSet(Strings.commaSepList(expectedPaths));
        assertEquals("Test: paths=" + paths + ", id=" + id.toShortString(), expected, override.matchingPluginPaths(id));
    }
}
