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


package ws.quokka.core.repo_spi;

import ws.quokka.core.test.AbstractTest;


/**
 *
 */
public class RepoPathSpecTest extends AbstractTest {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final Boolean TRUE = Boolean.TRUE;
    private static final Boolean FALSE = Boolean.FALSE;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testShortHand() {
        RepoPath path = new RepoPath("path1", "desc", true, true);
        assertBi("path1", path, new RepoPathSpec("runtime", "path1", null, TRUE, TRUE));
        assertBi("path1=crap", path, new RepoPathSpec("crap", "path1", null, TRUE, TRUE));
        assertBi("path1+crap", path, new RepoPathSpec("crap", "path1", null, FALSE, TRUE));
        assertBi("path1?+crap", path, new RepoPathSpec("crap", "path1", null, FALSE, FALSE));

        path = new RepoPath("path1", "desc", false, true);
        assertBi("path1<", path, new RepoPathSpec("runtime", "path1", null, TRUE, TRUE));
        assertBi("path1<crap", path, new RepoPathSpec("crap", "path1", null, TRUE, TRUE));
        assertBi("path1=crap", path, new RepoPathSpec("crap", "path1", null, FALSE, TRUE));
        assertBi("path1?=crap", path, new RepoPathSpec("crap", "path1", null, FALSE, FALSE));

        path = new RepoPath("path1", "desc", true, false);
        assertBi("path1!", path, new RepoPathSpec("runtime", "path1", null, TRUE, TRUE));
        assertBi("path1!=crap", path, new RepoPathSpec("crap", "path1", null, TRUE, TRUE));
        assertBi("path1!+crap", path, new RepoPathSpec("crap", "path1", null, FALSE, TRUE));
        assertBi("path1+crap", path, new RepoPathSpec("crap", "path1", null, FALSE, FALSE));

        path = new RepoPath("path1", "desc", false, false);
        assertBi("path1!<", path, new RepoPathSpec("runtime", "path1", null, TRUE, TRUE));
        assertBi("path1!<crap", path, new RepoPathSpec("crap", "path1", null, TRUE, TRUE));
        assertBi("path1!=crap", path, new RepoPathSpec("crap", "path1", null, FALSE, TRUE));
        assertBi("path1=crap", path, new RepoPathSpec("crap", "path1", null, FALSE, FALSE));

        path = new RepoPath("path1", "desc", false, false);
        assertBi("path1!<(dep1(dep1.1,dep1.2))", path,
            new RepoPathSpec("runtime", "path1", "dep1(dep1.1,dep1.2)", TRUE, TRUE));
        assertBi("path1!<crap(dep1(dep1.1,dep1.2))", path,
            new RepoPathSpec("crap", "path1", "dep1(dep1.1,dep1.2)", TRUE, TRUE));
        assertBi("path1!=crap(dep1(dep1.1,dep1.2))", path,
            new RepoPathSpec("crap", "path1", "dep1(dep1.1,dep1.2)", FALSE, TRUE));
        assertBi("path1=crap(dep1(dep1.1,dep1.2))", path,
            new RepoPathSpec("crap", "path1", "dep1(dep1.1,dep1.2)", FALSE, FALSE));
    }

    public void testShortHandNoTo() {
//        System.out.println(new RepoPathSpec("crap(a,b)", true));
//        System.out.println(new RepoPathSpec("?<crap(a,b)", true));
//        System.out.println(new RepoPathSpec("!+crap(a,b)", true));
        System.out.println(new RepoPathSpec("(a,b)", true));

//        System.out.println(new RepoPathSpec("crap(a,b)"));
    }

    public void assertBi(String expected, RepoPath path, RepoPathSpec original) {
        String shortHand = original.toShortHand(path);
        assertEquals(expected, shortHand);

        RepoPathSpec converted = new RepoPathSpec(shortHand);
        converted.mergeDefaults(path);
        assertEquals(original, converted);
    }

    public void testParseShortHand1() {
        RepoPathSpec pathSpec = new RepoPathSpec("runtime", "path1", null, Boolean.TRUE, null);
        pathSpec.setOptions("bsh,qdox");
        assertEquals(pathSpec, new RepoPathSpec("path1 < runtime(bsh,qdox)"));
        assertEquals(pathSpec, new RepoPathSpec("path1<runtime(bsh,qdox)"));
        assertEquals(pathSpec, new RepoPathSpec("  path1  <   runtime   (  bsh,qdox  )  "));
    }

    public void testParseShortHand2() {
        RepoPathSpec pathSpec = new RepoPathSpec(null, "path1", null, null, null);
        assertEquals(pathSpec, new RepoPathSpec("path1"));
        assertEquals(pathSpec, new RepoPathSpec("  path1  "));

        pathSpec = new RepoPathSpec(null, "path1", null, Boolean.TRUE, null);
        assertEquals(pathSpec, new RepoPathSpec("path1<"));
        assertEquals(pathSpec, new RepoPathSpec("  path1  <  "));
    }

    public void testParseShortHand3() {
        RepoPathSpec pathSpec = new RepoPathSpec(null, "path1", null, Boolean.FALSE, null);
        assertEquals(pathSpec, new RepoPathSpec("path1+"));
        assertEquals(pathSpec, new RepoPathSpec("  path1+  "));
        assertEquals(pathSpec, new RepoPathSpec("path1+"));
        assertEquals(pathSpec, new RepoPathSpec("  path1  +  "));
    }

    public void testParseShortHand4() {
        RepoPathSpec pathSpec = new RepoPathSpec(null, "path1", null, Boolean.FALSE, Boolean.FALSE);
        assertEquals(pathSpec, new RepoPathSpec("path1?+"));
    }

    public void testParseShortHand5() {
        RepoPathSpec pathSpec = new RepoPathSpec("runtime", "path1", null, null, null);
        assertEquals(pathSpec, new RepoPathSpec("path1=runtime"));
        assertEquals(pathSpec, new RepoPathSpec(" path1 = runtime "));
    }

    public void testParseShortHand6() {
        RepoPathSpec pathSpec = new RepoPathSpec("runtime", "path1", null, null, Boolean.TRUE);
        assertEquals(pathSpec, new RepoPathSpec("path1!=runtime"));
    }
}
