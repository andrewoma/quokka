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


package ws.quokka.core.bootstrap_util;

import ws.quokka.core.test.AbstractTest;

import java.util.Properties;


/**
 *
 */
public class ArtifactPropertiesParserTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    ArtifactPropertiesParser parser = new ArtifactPropertiesParser();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParseClassPath() {
        Properties properties = parser.parse("group1", "name1", "type1");
        assertProperties(properties);
    }

    public void testParseFile() {
        Properties properties = parser.parse(getTestCaseResource("properties.jar"), "group1", "name1", "type1");
        assertProperties(properties);
    }

    private void assertProperties(Properties properties) {
        assertEquals(2, properties.size());
        assertEquals("value1", properties.get("key1"));
        assertEquals("value2", properties.get("key2"));
    }
}
