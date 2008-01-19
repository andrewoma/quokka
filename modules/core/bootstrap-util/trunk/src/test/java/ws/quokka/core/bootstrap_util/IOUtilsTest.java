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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Properties;


/**
 *
 */
public class IOUtilsTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private IOUtils utils = new IOUtils();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testSaveLoadProperties() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");

        File temp = File.createTempFile("ioutilstest", ".properties");

        try {
            utils.saveProperties(temp, properties);

            Properties loaded = utils.loadProperties(temp);
            assertEquals(properties, loaded);
        } finally {
            temp.delete();
        }
    }

    public void testCopyStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("Hello".getBytes());
        utils.copyStream(in, out);
        assertEquals("Hello", out.toString());
    }
}
