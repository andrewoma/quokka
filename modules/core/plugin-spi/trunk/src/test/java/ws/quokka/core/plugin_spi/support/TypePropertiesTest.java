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


package ws.quokka.core.plugin_spi.support;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ResourceCollection;

import ws.quokka.core.test.AbstractTest;

import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 *
 */
public class TypePropertiesTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project = new Project();
    private TypedProperties tp = new TypedProperties("");
    private Properties p = new Properties();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        tp.setProperties(p);
        project.init();
        tp.setProject(project);
    }

    public void testMapString() {
        p.put("map[key1]", "value1");
        p.put("map[key2]", "value2");

        Map result = tp.getMap("map", true, String.class);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    public void testListString() {
        p.put("list[0]", "value1");
        p.put("list[1]", "value2");

        List result = tp.getList("list", true, String.class, false);
        assertEquals(2, result.size());
        assertEquals("value1", result.get(0));
        assertEquals("value2", result.get(1));
    }

    public void testListComplex() {
        p.put("list[0]fork", "true");
        p.put("list[0]rc.set.dir", "C:\\SomeDir");

        List result = tp.getList("list", true, null, false);
        assertEquals(1, result.size());

        TypedProperties value = (TypedProperties)result.iterator().next();
        assertEquals(true, value.getBoolean("fork"));
        assertTrue(value.getFileSet("rc").getDir().getPath().indexOf("SomeDir") != -1);
    }

    public void testMapFileSet() {
        p.put("map[set1]set.dir", "C:\\SomeDir");

        Map result = tp.getMap("map", true, FileSet.class);
        FileSet fileSet = (FileSet)result.get("set1");
        assertTrue(fileSet.getDir().getPath().indexOf("SomeDir") != -1);
    }

    public void testResourceCollectionFileSet() {
        p.put("rc.set.dir", "C:\\SomeDir");

        ResourceCollection rc = tp.getResourceCollection("bogus", false);
        assertNull(rc);

        rc = tp.getResourceCollection("rc", true);
        assertTrue(rc instanceof FileSet);
        assertTrue(((FileSet)rc).getDir().getPath().indexOf("SomeDir") != -1);

        // Try bogus property
        p.put("rc.set.crud", "C:\\SomeDir");

        try {
            tp.getResourceCollection("rc", true);
            fail("Expected exception");
        } catch (Exception e) {
        }

        // Try bogus sibling
        p.remove("rc.set.crud");
        p.put("rc.other", "blah");

        try {
            tp.getResourceCollection("rc", true);
            fail("Expected exception");
        } catch (Exception e) {
        }
    }

    public void testResourceCollectionFileList() {
        p.put("rc.list.dir", "C:\\SomeDir");
        p.put("rc.list.files", "cat.txt, dog.txt");

        ResourceCollection rc = tp.getResourceCollection("bogus", false);
        assertNull(rc);

        rc = tp.getResourceCollection("rc", true);
        assertTrue(rc instanceof FileList);

        String[] files = ((FileList)rc).getFiles(project);
        assertEquals(2, files.length);
        assertEquals("cat.txt", files[0]);
        assertEquals("dog.txt", files[1]);
        assertTrue(((FileList)rc).getDir(project).getPath().indexOf("SomeDir") != -1);

        // Try bogus property
        p.put("rc.list.crud", "C:\\SomeDir");

        try {
            tp.getResourceCollection("rc", true);
            fail("Expected exception");
        } catch (Exception e) {
        }

        // Try bogus sibling
        p.remove("rc.set.crud");
        p.put("rc.other", "blah");

        try {
            tp.getResourceCollection("rc", true);
            fail("Expected exception");
        } catch (Exception e) {
        }
    }

    public void testVerifySimple() {
        p.put("key1", "value1");
        p.put("key2", "value2");
        tp.verify(new Keys("key1").add("key2"));

        try {
            tp.verify(new Keys("key1"));
            fail("Expected exception");
        } catch (Exception e) {
        }
    }

    public void testVerifyExclusions() {
        p.put("key1", "value1");
        p.put("key2", "value2");
        p.put("key3[0]", "value3.0");
        p.put("key3[1]", "value3.1");
        tp.verify(new Keys("key1").add("key2"), new Keys("key3"));

        try {
            tp.verify(new Keys("key1").add("key2"), new Keys("key4"));
            fail("Expected exception");
        } catch (Exception e) {
        }
    }
}
