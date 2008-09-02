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


package ws.quokka.core.util.xml;

import junit.framework.TestCase;

import org.apache.tools.ant.filters.StringInputStream;

import org.w3c.dom.*;
import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;


/**
 *
 */
public class TransformingWriterTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private TransformingXmlWriter writer = new TransformingXmlWriter();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testWriter() throws ParserConfigurationException, IOException, SAXException {
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new StringInputStream(
                    "<root><child><descendent/></child></root>"));
        StringWriter sw = new StringWriter();
        Properties properties = new Properties();
        properties.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        writer.write(sw, dom, properties);
        assertEquals("<root>\n" + "    <child>\n" + "        <descendent/>\n" + "    </child>\n" + "</root>\n",
            sw.toString());
    }
}
