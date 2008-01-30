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

import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;


/**
 *
 */
public class XmlParserTest extends TestCase {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void test() throws IOException {
        String xml = "<root> <el1 id='el1'/> <el2/> <el2/> </root>";
        File file = File.createTempFile("xmlparsertest", ".xml");

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(xml.toCharArray());
            writer.close();

            Element el = XmlParser.parseXml(file);
            assertEquals("root", el.getNodeName());

            Element el1 = XmlParser.getChild(el, "el1", true);
            assertEquals("el1", XmlParser.getAttribute(el1, "id"));

            Element unknown = XmlParser.getChild(el, "xxx", false);
            assertNull(unknown);

            List children = XmlParser.getChildren(el, "xxx", false);
            assertEquals(0, children.size());

            children = XmlParser.getChildren(el, "el2", true);
            assertEquals(2, children.size());
        } finally {
            file.delete();
        }
    }
}
