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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import ws.quokka.core.bootstrap_util.QuokkaEntityResolver;

import java.io.IOException;

import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;


/**
 * LocatorDomParser parses XML to a DOM document, recording the locations of the parsed elements
 * in the source.<br>
 * Inspired by DomHelper from the XWork 2.0, but completely rewritten
 */
public class LocatorDomParser {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static Map locations = Collections.synchronizedMap(new WeakHashMap());

    //~ Methods --------------------------------------------------------------------------------------------------------

    public static Locator getLocator(Element element) {
        return (Locator)locations.get(element);
    }

    private static void addLocation(Element element, Locator locator) {
        locations.put(element, locator);
    }

    /**
     * Creates a W3C Document that remembers the location of each element in
     * the source file. The location of element nodes can then be retrieved
     * using the {@link #getLocator(Element)} method.
     *
     * @param inputSource the inputSource to read the document from
     */
    public static Document parse(InputSource inputSource)
            throws IOException, ParserConfigurationException, SAXException {
        return parse(inputSource, null);
    }

    /**
     * Creates a W3C Document that remembers the location of each element in
     * the source file. The location of element nodes can then be retrieved
     * using the {@link #getLocator(Element)} method.
     *
     * @param inputSource the inputSource to read the document from
     */
    public static Document parse(InputSource inputSource, EntityResolver entityResolver)
            throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating((entityResolver != null)
            && !(entityResolver instanceof ws.quokka.core.util.xml.Document.NullEntityResolver));
        factory.setNamespaceAware(false);

        SaxToLocatorDom handler = new SaxToLocatorDom(entityResolver);
        factory.newSAXParser().parse(inputSource, handler);

        return handler.getDocument();
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * SaxToLocatorDom converts from SAX to DOM, saving the location of each node during parsing.
     * This is necessary as DOM parsing does not store the
     */
    public static class SaxToLocatorDom extends DefaultHandler {
        private Document document;
        private Stack elements = new Stack();
        private Text currentTextNode;
        private Locator locator;
        private EntityResolver entityResolver;

        public SaxToLocatorDom(EntityResolver entityResolver)
                throws ParserConfigurationException {
            this.entityResolver = entityResolver;
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }

        public Document getDocument() {
            return document;
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;

            //            System.out.println("locator=" + new Location(locator));
        }

        public void characters(char[] ch, int start, int length) {
            if (getLast() == document) {
                return;
            }

            String characters = new String(ch, start, length);

            if (currentTextNode != null) {
                currentTextNode.appendData(characters);
            } else {
                currentTextNode = (Text)getLast().appendChild(document.createTextNode(characters));
            }
        }

        public void startDocument() {
            elements.push(document); // Root
        }

        public void endDocument() {
        }

        public void startElement(String namespace, String localName, String qName, Attributes attributes) {
            // Create element
            Element element = document.createElement(qName);

            //            Element element = document.createElementNS(namespace, qName);
            // Add location attributes to the element
            addLocation(element, new LocatorImpl(locator));

            // Add attributes
            int count = attributes.getLength();

            for (int i = 0; i < count; i++) {
                String name = attributes.getQName(i);
                String value = attributes.getValue(i);

                //                if (attributes.getLocalName(i) == null) {
                element.setAttribute(name, value);

                //                } else {
                //                    element.setAttributeNS(attributes.getURI(i), name, value);
                //                }
            }

            // Add element to the last element
            getLast().appendChild(element);

            // Push this node onto stack
            elements.push(element);
            currentTextNode = null;
        }

        public Node getLast() {
            return (Node)elements.peek();
        }

        public void endElement(String namespace, String localName, String qName) {
            elements.pop();
            currentTextNode = null;
        }

        public void startPrefixMapping(String prefix, String uri) {
            // Namespaces not copied to DOM
        }

        public void processingInstruction(String target, String data) {
            // Processing instructions not copied to DOM
        }

        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException {
            if (entityResolver == null) {
                return null;
            }

            try {
                return entityResolver.resolveEntity(publicId, systemId);
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        public void warning(SAXParseException e) throws SAXException {
            System.err.println(e.getMessage());
        }

        public void error(SAXParseException e) throws SAXException {
            // Provide a friendly error message for a missing doctype declaration as the default xerces one is confusing
            if (e.getMessage().equals("Document root element \"project\", must match DOCTYPE root \"null\".")
                    && entityResolver instanceof QuokkaEntityResolver) {
                String version = ((QuokkaEntityResolver)entityResolver).getVersion("project");
                throw new BuildException(
                    "Quokka requires a DOCTYPE declaration in project files. For this version of quokka use:\n\t"
                    + "<!DOCTYPE project PUBLIC \"quokka.ws/dtd/project-" + version
                    + "\" \"http://quokka.ws/dtd/project-" + version + ".dtd\">", e, new Location(locator));
            }

            throw new BuildException(e, new Location(locator));
        }

        public void fatalError(SAXParseException e) throws SAXException {
            throw new BuildException(e, new Location(locator));
        }
    }
}
