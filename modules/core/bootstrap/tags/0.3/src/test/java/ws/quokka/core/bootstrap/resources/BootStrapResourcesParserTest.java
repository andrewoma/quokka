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


package ws.quokka.core.bootstrap.resources;

import org.apache.tools.ant.util.JavaEnvUtils;

import ws.quokka.core.test.AbstractTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Iterator;
import java.util.Map;


/**
 *
 */
public class BootStrapResourcesParserTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParse() throws IOException {
        File cacheDir = new File(normalise(getModuleHome().getAbsolutePath()
                    + "/target/test/BootStrapResourcesParserTest"));
        BootStrapResourcesParser parser = new BootStrapResourcesParser();
        BootStrapResources resources = parser.parse(getBootstrapXml(), getTestCaseResource("libs"), cacheDir);
        assertEquals(3, resources.getAvailableLibraries().size());
        assertEquals(System.getProperty("os.arch"),
            ((Jdk)resources.getJdks().get(0)).getProperties().getProperty("os.arch"));
    }

    public File getBootstrapXml() throws IOException {
        String java = JavaEnvUtils.getJreExecutable("java");
        String xml = "<bootstrap>\n" + "    <jdks>\n" + "        <jdk location=\"" + java + "\"/>\n" + "    </jdks>\n"
            + "</bootstrap>";
        File file = File.createTempFile("bootstrap", ".xml");
        file.deleteOnExit();

        Writer writer = new FileWriter(file);

        try {
            writer.write(xml);
        } finally {
            writer.close();
        }

        return file;
    }
}
