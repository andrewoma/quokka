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


package ws.quokka.core.main.parser;

import ws.quokka.core.model.Path;
import ws.quokka.core.model.PathGroup;
import ws.quokka.core.model.Plugin;
import ws.quokka.core.model.Target;
import ws.quokka.core.repo_spi.RepoType;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.AnnotatedProperties;

import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class StandardPluginParserTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParse() throws MalformedURLException {
        StandardPluginParser parser = new StandardPluginParser();
        Plugin plugin = parser.parsePluginXml(getTestCaseResource("plugin.xml").toURI().toURL());
        assertEquals("somepackage.someclass", plugin.getClassName());
        assertEquals("Plugin description", plugin.getDescription());
        assertEquals("some.name.space", plugin.getNameSpace());
        assertEquals(2, plugin.getTargets().size());

        // Targets
        Target target1 = plugin.getTarget("some.name.space:target1");
        assertNotNull(target1);
        assertEquals("some.name.space:target1", target1.getName());
        assertEquals("alias1", target1.getAlias());

        List dependencies = new ArrayList();
        dependencies.add("some.name.space:target2");
        dependencies.add("some.name.space:target3");
        assertEquals(dependencies, target1.getDependencies());
        assertEquals("lifecyce:clean", target1.getImplementsPlugin());
        assertEquals("prefix1", target1.getPrefix());
        assertEquals(true, target1.isTemplate());
        assertEquals("template-name1", target1.getTemplateName());
        assertEquals(false, target1.isEnabledByDefault()); // Gets overridden if a template
        assertEquals(true, target1.isAbstract());
        assertEquals("Description1", target1.getDescription());
        assertEquals(true, target1.isMain());

        Target target2 = plugin.getTarget("some.name.space:target2");
        assertNotNull(target2);
        assertEquals("some.name.space:target2", target2.getName());
        assertNull(target2.getAlias());
        assertEquals(new ArrayList(), target2.getDependencies());
        assertNull(target2.getImplementsPlugin());
        assertNull(target2.getPrefix());
        assertEquals(false, target2.isTemplate());
        assertNull(target2.getTemplateName());
        assertEquals(false, target2.isEnabledByDefault());
        assertEquals(false, target2.isAbstract());
        assertNull(target2.getDescription());
        assertEquals(false, target2.isMain());

        // Path groups
        assertEquals(3, target1.getPathGroups().size());

        PathGroup group = target1.getPathGroup("group1");
        assertNotNull(group);
        assertEquals("group1", group.getId());

        List paths = new ArrayList();
        paths.add("path1");
        paths.add("path2");
        assertEquals(paths, group.getPaths());
        assertEquals(Boolean.TRUE, group.getMergeWithCore());

        group = target1.getPathGroup("group2");
        assertNotNull(group);
        assertEquals("group2", group.getId());
        assertEquals(Collections.singletonList("path3"), group.getPaths());
        assertEquals(Boolean.FALSE, group.getMergeWithCore());

        group = target1.getPathGroup("group3");
        assertNotNull(group);
        assertEquals("group3", group.getId());
        assertEquals(Collections.singletonList("path4"), group.getPaths());
        assertEquals(Boolean.TRUE, group.getMergeWithCore());

        // Project paths
        assertEquals(3, target1.getProjectPaths().size());

        Path path = (Path)target1.getProjectPaths().get(0);
        assertNotNull(path);
        assertEquals("path1", path.getId());
        assertEquals("Some path1", path.getDescription());
        assertEquals(true, path.isDescendDefault());
        assertEquals(true, path.isMandatoryDefault());

        path = (Path)target1.getProjectPaths().get(1);
        assertNotNull(path);
        assertEquals("path2", path.getId());
        assertEquals("Some path2", path.getDescription());
        assertEquals(false, path.isDescendDefault());
        assertEquals(false, path.isMandatoryDefault());

        path = (Path)target1.getProjectPaths().get(2);
        assertNotNull(path);
        assertEquals("path3", path.getId());
        assertEquals("Some path3", path.getDescription());
        assertEquals(true, path.isDescendDefault());
        assertEquals(true, path.isMandatoryDefault());

        // Properties
        AnnotatedProperties properties = target1.getDefaultProperties();
        assertNotNull(properties);
        assertEquals(4, properties.size());
        assertEquals("t1value1", properties.get("t1prop1"));
        assertEquals("t1value2", properties.get("t1prop2"));
        assertEquals("value1", properties.get("prop1"));
        assertEquals("value2", properties.get("prop2"));

        properties = target2.getDefaultProperties();
        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals("value2", properties.get("prop2"));
        assertEquals("value3", properties.get("prop3"));
    }
}
