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
import org.apache.tools.ant.types.FileSet;

import ws.quokka.core.test.AbstractTest;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 *
 */
public class PropertiesHelperTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Project project = new Project();
    private TypedProperties ph = new TypedProperties("");
    private Properties p = new Properties();

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected void setUp() throws Exception {
        super.setUp();
        ph.setProperties(p);
        project.init();
        ph.setProject(project);
    }

    public void testMapString() {
        p.put("map[key1]", "value1");
        p.put("map[key2]", "value2");

        Map result = ph.getMap("map", true, String.class);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    public void testListString() {
        p.put("list[0]", "value1");
        p.put("list[1]", "value2");

        List result = ph.getList("list", true, String.class, false);
        assertEquals(2, result.size());
        assertEquals("value1", result.get(0));
        assertEquals("value2", result.get(1));
    }

    public void testListComplex() {
        p.put("list[0]fork", "true");
        p.put("list[0]rc.set.dir", "C:\\SomeDir");

        List result = ph.getList("list", true, null, false);
        assertEquals(1, result.size());

        TypedProperties value = (TypedProperties)result.iterator().next();
        assertEquals(true, value.getBoolean("fork"));
        assertTrue(value.getFileSet("rc").getDir().getPath().contains("SomeDir"));
    }

    public void testMapFileSet() {
        p.put("map[set1]set.dir", "C:\\SomeDir");

        Map result = ph.getMap("map", true, FileSet.class);
        FileSet fileSet = (FileSet)result.get("set1");
        assertTrue(fileSet.getDir().getPath().contains("SomeDir"));
    }
}
