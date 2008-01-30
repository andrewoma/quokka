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


package ws.quokka.core.util;

import ws.quokka.core.test.AbstractTest;

import java.net.URL;

import java.util.Iterator;
import java.util.Map;


/**
 *
 */
public class URLsTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testToURLEntriesWithJar() {
        Map entries = URLs.toURLEntries(getTestCaseResource("dir.jar"), "dir2/");
        assertEquals(2, entries.size());
        assertTrue(entries.containsKey("dir2.1.txt"));
        assertTrue(entries.containsKey("dir2.2.txt"));

        entries = URLs.toURLEntries(getTestCaseResource("dir.jar"), "bogus");
        assertEquals(0, entries.size());
    }

    public void testToURLEntriesWithDir() {
        Map entries = stripSvn(URLs.toURLEntries(getTestCaseResource("dir/"), "dir2/"));
        assertEquals(2, entries.size());
        assertTrue(entries.containsKey("dir2.1.txt"));
        assertTrue(entries.containsKey("dir2.2.txt"));

        entries = URLs.toURLEntries(getTestCaseResource("dir/"), "bogus");
        assertEquals(0, entries.size());
    }

    public void testCompatibility() {
        // Check the dir-based and jar-based versions return the same keys
        assertCompatibility("", "dir1/");
        assertCompatibility("/", "dir1/");
        assertCompatibility("dir1/", "file1.1.txt");
        assertCompatibility("/dir1/", "file1.1.txt");
        assertCompatibility("dir2", "dir2.1.txt");
    }

    public void testToURLWithJar() {
        String path = "dir1/file1.1.txt";
        URL url = URLs.toURL(getTestCaseResource("dir.jar"), path);
        checkURL(url, path);

        path = "dir1/";
        url = URLs.toURL(getTestCaseResource("dir.jar"), path);

        path = "";
        url = URLs.toURL(getTestCaseResource("dir.jar"), path);
        checkURL(url, path);

        path = "bogus";
        url = URLs.toURL(getTestCaseResource("dir.jar"), path);
        assertNull(url);
    }

    private void checkURL(URL url, String path) {
        assertNotNull(url);
        assertTrue(url.toString().endsWith(path));
    }

    public void testToURLWithDir() {
        String path = "dir1/file1.1.txt";
        URL url = URLs.toURL(getTestCaseResource("dir/"), path);
        checkURL(url, path);

        path = "dir1/";
        url = URLs.toURL(getTestCaseResource("dir/"), path);
        checkURL(url, path);

        path = "/";
        url = URLs.toURL(getTestCaseResource("dir/"), path);
        checkURL(url, path);

        path = "bogus";
        url = URLs.toURL(getTestCaseResource("dir/"), path);
        assertNull(url);
    }

    public void assertCompatibility(String path, String sampleKey) {
        Map jarEntries = URLs.toURLEntries(getTestCaseResource("dir.jar"), path);
        Map dirEntries = stripSvn(URLs.toURLEntries(getTestCaseResource("dir/"), path));

        //        System.out.println(dirEntries.toString().replace(',', '\n'));
        assertEquals("Comparing path: " + path, jarEntries.keySet(), dirEntries.keySet());
        assertTrue(jarEntries.containsKey(sampleKey));
    }

    private Map stripSvn(Map map) {
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();

            if (key.indexOf("svn") != -1) {
                i.remove();
            }
        }

        return map;
    }
}
