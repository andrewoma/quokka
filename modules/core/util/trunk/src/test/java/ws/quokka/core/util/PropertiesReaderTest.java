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

import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.test.AbstractTest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 *
 */
public class PropertiesReaderTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void test() throws IOException {
        assertEquals(getTestCaseResource("reader.properties"));
    }

    /**
     * This tests the behaviour of the PropertiesReader against the JDK version
     */
    public void assertEquals(File file) throws IOException {
        Properties properties = loadViaReader(file);

        // Load via JDK
        Properties jdkProperties = new IOUtils().loadProperties(file);

        if (!jdkProperties.equals(properties)) {
            for (Iterator i = jdkProperties.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
                String readerValue = (String)properties.remove(key);
                assertNotNull("Property missing from reader: " + key, readerValue);
                assertEquals("Key does not equal " + key, value, readerValue);
            }

            for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                System.out.println("Additional property: " + entry.getKey() + " -> " + entry.getValue());
            }

            fail("Properties do not match jdk");
        }
    }

    private Properties loadViaReader(File file) throws IOException {
        // Load properties via reader
        Properties properties = new Properties();
        PropertiesReader reader = null;
        FileReader in = new FileReader(file);

        try {
            reader = new PropertiesReader(in);

            while (reader.nextProperty()) {
                properties.put(reader.getPropertyName(), reader.getPropertyValue());
            }
        } finally {
            in.close();
        }

        return properties;
    }
}
