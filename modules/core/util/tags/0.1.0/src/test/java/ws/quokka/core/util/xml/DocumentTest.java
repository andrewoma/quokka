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

import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.URLs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;


/**
 *
 */
public class DocumentTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParseString() {
        Document document = Document.parse("<root/>");
        assertEquals("root", document.getRoot().getName());
    }

    public void testParseFile() {
        Document document = Document.parse(getTestCaseResource("minimal.xml"));
        assertEquals("root", document.getRoot().getName());
    }

    public void testParseURL() {
        Document document = Document.parse(URLs.toURL(getTestCaseResource("minimal.xml")));
        assertEquals("root", document.getRoot().getName());
    }

    public void testParseReader() throws FileNotFoundException {
        Document document = Document.parse(new FileReader(getTestCaseResource("minimal.xml")));
        assertEquals("root", document.getRoot().getName());
    }

    public void testParseStream() throws FileNotFoundException {
        Document document = Document.parse(new FileInputStream(getTestCaseResource("minimal.xml")));
        assertEquals("root", document.getRoot().getName());
    }

    public void testGetRootElementOtherNodeTypes() {
        Document document = Document.parse("<!-- Before --> <root/> <!-- After -->");
        assertEquals("root", document.getRoot().getName());
    }

    public void testCreateDocument() {
        Document document = Document.create();
        document.addRootElement("root");
        assertEquals("root", document.getDocument().getDocumentElement().getNodeName());
        assertEquals("root", document.getRoot().getName());
    }

    public void testToXml() {
        Document document = Document.parse(getTestCaseResource("basic.xml"));
        String xml = document.toXML(new StringWriter(), true).toString();
        Document parsed = Document.parse(new StringReader(xml));
        Element root = parsed.getRoot();
        assertEquals("root", root.getName());

        Element e1 = root.getChild("e1");
        assertEquals("e1", e1.getName());
        assertEquals("val1", e1.getAttribute("att1"));
        assertEquals("Text", e1.getText());
    }
}
