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

import ws.quokka.core.test.AbstractTest;

import java.util.Properties;


/**
 *
 */
public class RepositoryFactoryImplTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParseUrl() {
        Properties properties = RepositoryFactoryImpl.parseUrl("prefix.", "file:C:\\SomeDir;key1=value1;key2=value2");
        Properties expected = new Properties();
        expected.put("prefix.class", "file");
        expected.put("prefix.root", "C:\\SomeDir");
        expected.put("prefix.key1", "value1");
        expected.put("prefix.key2", "value2");
        assertEquals(expected, properties);
    }
}
