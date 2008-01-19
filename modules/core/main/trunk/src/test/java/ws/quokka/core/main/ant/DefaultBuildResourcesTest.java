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


package ws.quokka.core.main.ant;

import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.URLs;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.util.Iterator;
import java.util.Map;


/**
 *
 */
public class DefaultBuildResourcesTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private DefaultBuildResources resources = new DefaultBuildResources();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        resources.setTempDir(new File("C:\\Temp\\buildresources"));

        Map entries = URLs.toURLEntries(new File(
                    "C:\\Data\\Dev\\Projects\\quokka\\core\\main\\src\\test\\resources\\DefaultBuildResourcesTest\\resources\\root1"),
                "");
        entries.putAll(URLs.toURLEntries(
                new File(
                    "C:\\Data\\Dev\\Projects\\quokka\\core\\main\\src\\test\\resources\\DefaultBuildResourcesTest\\resources\\root2"),
                ""));
        entries.putAll(URLs.toURLEntries(
                new File(
                    "C:\\Data\\Dev\\Projects\\quokka\\core\\main\\src\\test\\resources\\DefaultBuildResourcesTest\\resources\\root3.jar"),
                ""));

        for (Iterator i = entries.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();

            if (key.indexOf(".svn") != -1) {
                i.remove();
            }
        }

        resources.putAll(entries);
        System.out.println(resources.toString());
    }

    public void testInPlaceFile() {
        String key = "file1.txt";
        File file = resources.getFile(key);
        assertEquals(URLs.toURL(file), resources.getURL(key));
    }

    public void testJarFile() throws IOException {
        String key = "dir5/file5.1.txt";
        File file = resources.getFile(key);
        assertContentEquals(URLs.toURL(file), resources.getURL(key));
    }

    public void testDirDifferentRoots() throws IOException {
        String key = "share1";
        Map urls = resources.getURLs(key);
        File dir = resources.getFileOrDir(key);

        String subkey = "share1.txt";
        assertContentEquals(URLs.toURL(new File(dir, subkey)), (URL)urls.get(key + "/" + subkey));
    }
}
