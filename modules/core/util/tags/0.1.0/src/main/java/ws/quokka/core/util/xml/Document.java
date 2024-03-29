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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ws.quokka.core.bootstrap_util.ExceptionHandler;

import java.io.BufferedInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 *
 */
public class Document {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private org.w3c.dom.Document document;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Document(org.w3c.dom.Document document) {
        this.document = document;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public Element getRoot() {
        return new Element(document.getDocumentElement());
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final File file) {
        return parse(file, null);
    }

    public static Document parse(final File file, EntityResolver entityResolver) {
        return parse(new Source() {
                public InputSource getSource() throws Exception {
                    InputSource source = new InputSource(new FileInputStream(file));
                    source.setSystemId(file.getAbsolutePath());

                    return source;
                }
            }, entityResolver);
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final URL url) {
        return parse(url, null);
    }

    public static Document parse(final URL url, final EntityResolver entityResolver) {
        return parse(new Source() {
                public InputSource getSource() throws Exception {
                    InputSource source = new InputSource(new BufferedInputStream(url.openStream()));
                    source.setSystemId(url.toExternalForm());

                    return source;
                }
            }, entityResolver);
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final InputStream inputStream) {
        return parse(inputStream, null);
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final InputStream inputStream, final EntityResolver entityResolver) {
        return parse(new Source() {
                public InputSource getSource() throws Exception {
                    return new InputSource(inputStream);
                }
            }, entityResolver);
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final Reader reader) {
        return parse(new Source() {
                public InputSource getSource() throws Exception {
                    return new InputSource(reader);
                }
            }, null);
    }

    /**
     * Parses an XML file. It softens any exceptions to RuntimeExceptions.
     */
    public static Document parse(final String xml) {
        return parse(new Source() {
                public InputSource getSource() throws Exception {
                    return new InputSource(new CharArrayReader(xml.toCharArray()));
                }
            }, null);
    }

    private static Document parse(final Source source, final EntityResolver entityResolver) {
        return (Document)new ExceptionHandler() {
                public Object run() throws Exception {
                    InputSource inputSource = null;

                    try {
                        inputSource = source.getSource();

                        return new Document(LocatorDomParser.parse(inputSource, entityResolver));
                    } finally {
                        if ((inputSource != null) && (inputSource.getByteStream() != null)) {
                            inputSource.getByteStream().close();
                        }
                    }
                }
            }.soften();
    }

    public static Document create() {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        return (Document)new ExceptionHandler() {
                public Object run() throws ParserConfigurationException {
                    DocumentBuilder documentBuilder = factory.newDocumentBuilder();

                    return new Document(documentBuilder.newDocument());
                }
            }.soften();
    }

    public Element addRootElement(String name) {
        return new Element((org.w3c.dom.Element)document.appendChild(document.createElement(name)));
    }

    public Writer toXML(final Writer writer, final boolean close) {
        return (Writer)new ExceptionHandler() {
                public Object run() throws IOException {
                    try {
                        XmlWriter xmlWriter = new XmlWriter(document.getFirstChild());
                        xmlWriter.write(writer);

                        return writer;
                    } finally {
                        if (close) {
                            writer.close();
                        }
                    }
                }
            }.soften();
    }

    public org.w3c.dom.Document getDocument() {
        return document;
    }

    //~ Inner Interfaces -----------------------------------------------------------------------------------------------

    private static interface Source {
        InputSource getSource() throws Exception;
    }

    //~ Inner Classes --------------------------------------------------------------------------------------------------

    /**
     * NullEntityResolver can be used to prevent parsing from retrieving entities from external sources
     */
    public static class NullEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }
    }
}
