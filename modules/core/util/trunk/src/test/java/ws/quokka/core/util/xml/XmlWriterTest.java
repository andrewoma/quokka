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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Tests TextWriter class
 *
 * @version $Revision: 512591 $, $Date: 2007-02-27 22:35:44 -0500 (Tue, 27 Feb 2007) $
 */
public class XmlWriterTest extends TestCase {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String FRAGMENT = "<element attr1=\"value1\" attr2=\"value2\">\n" + "<!-- Comment Node -->\n"
        + "<empty-element />\n" + "<![CDATA[ CDATA Node ]]>\n"
        + "Text &lt;Node&gt; &amp; &apos;Escaped &quot;Text&quot;&apos;" + "</element>";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + FRAGMENT;

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Document dom;
    private XmlWriter writer;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setUp() throws Exception {
        dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new StringInputStream(XML));

        //        dom = Document.parse(new StringReader(XML)).getDocument();
        writer = new XmlWriter(dom);
    }

    public void testToString() throws Exception {
        assertEquals(XML, writer.toString());
    }

    public void testWriter() throws Exception {
        StringWriter out = new StringWriter();
        writer.write(out);
        assertEquals(XML, out.toString());
    }

    public void testOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(out);
        assertEquals(XML, out.toString());
    }

    public void testWriter1() throws Exception {
        StringWriter out = new StringWriter();
        XmlWriter.write(dom, out);
        assertEquals(XML, out.toString());
    }

    public void testOutputStream1() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter.write(dom, out);
        assertEquals(XML, out.toString());
    }

    public void testFragment() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DocumentFragment fragment = dom.createDocumentFragment();
        fragment.appendChild(dom.getDocumentElement());
        XmlWriter.write(fragment, out);
        assertEquals(FRAGMENT, out.toString());
    }
}
