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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;


/**
 *
 */
public class QuokkaEntityResolverTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    QuokkaEntityResolver resolver = new QuokkaEntityResolver();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testUnknownEntity() throws IOException, SAXException {
        InputSource source = resolver.resolveEntity("xxx", "xxx");
        assertNull(source);
    }

    public void testKnownEntity() throws IOException, SAXException {
        InputSource source = resolver.resolveEntity("quokka.ws/dtd/project-1.0", "");
        assertNotNull(source);
        source.getByteStream().close();
    }

    public void testKnownEntityCorrectVersion() throws IOException, SAXException {
        resolver.addVersion("project", "1.0");

        InputSource source = resolver.resolveEntity("quokka.ws/dtd/project-1.0", "");
        assertNotNull(source);
        source.getByteStream().close();

        resolver.addVersion("plugin", "2.0");
        source = resolver.resolveEntity("quokka.ws/dtd/plugin-2.0", "");
        assertNotNull(source);
        source.getByteStream().close();
    }

    public void testKnownEntityWrongVersion() throws IOException {
        resolver.addVersion("project", "1.2");

        try {
            resolver.resolveEntity("quokka.ws/dtd/project-1.0", "");
            fail();
        } catch (SAXException e) {
            assertTrue(e.getMessage().indexOf("does not match the required version") != -1);
        }
    }
}
