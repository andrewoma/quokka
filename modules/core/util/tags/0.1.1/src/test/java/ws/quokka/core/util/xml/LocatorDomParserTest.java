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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import ws.quokka.core.test.AbstractTest;

import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;


/**
 *
 */
public class LocatorDomParserTest extends AbstractTest {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void testParse() throws IOException, ParserConfigurationException, SAXException {
        System.out.println("LocatorDomParserTest.testParse");

        InputSource source = new InputSource();
        source.setPublicId("publicId");
        source.setSystemId("/systemId");
        source.setCharacterStream(new FileReader(getTestCaseResource("sample.xml")));

        Document document = LocatorDomParser.parse(source, null);

        Element child = (Element)document.getDocumentElement().getElementsByTagName("child").item(0);
        Locator locator = LocatorDomParser.getLocator(child);
        assertEquals("publicId", locator.getPublicId());
        assertTrue(locator.getSystemId().endsWith("/systemId")); // Gets converted to path
        assertEquals(4, locator.getLineNumber());

        // TODO: work out what happens to the column number ... seems to be offset
        //        assertEquals(2, locator.getColumnNumber());
    }
}
