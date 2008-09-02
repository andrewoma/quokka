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

import org.xml.sax.EntityResolver;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * XmlConverter provides a framework for converting objects to and from XML. In general, a Converter
 * is added for each class that is responsible for converting a single object of that class.
 */
public class XmlConverter {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List converters = new ArrayList();
    private Map context = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Adds a converter
     */
    public void add(Converter converter) {
        converters.add(converter);
        converter.setXmlConverter(this);
    }

    /**
     * Returns a converter for the given class
     * @throws BuildException if no converter is defined for the given class
     */
    public Converter getConverter(Class clazz) {
        for (Iterator i = converters.iterator(); i.hasNext();) {
            Converter converter = (Converter)i.next();

            if (converter.supports(clazz)) {
                return converter;
            }
        }

        throw new BuildException("No xml converter is defined for class: " + clazz.getName());
    }

    /**
     * Adds context so that is accessible to other converters during the conversion process
     */
    public void addContext(String key, Object context) {
        this.context.put(key, context);
    }

    /**
     * Returns the context for the given key, or null if it doesn't exist
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * Removes context for the given key
     */
    public void removeContext(String key) {
        this.context.remove(key);
    }

    /**
     * Converts and object to and XML string
     * @param object the object to convert
     * @param rootElementName to root element name of the generated document
     * @param publicId for the dtd
     * @param systemId for the dtd
     * @return a string representation
     */
    public String toXml(Object object, String rootElementName, String publicId, String systemId) {
        return toXml(object, new StringWriter(), rootElementName, publicId, systemId).toString();
    }

    /**
     * Converts and object to XML, writing it to the writer provided
     * @param object the object to convert
     * @param rootElementName to root element name of the generated document
     * @param publicId for the dtd
     * @param systemId for the dtd
     * @return the writer given for chaining purposes
     */
    public Writer toXml(Object object, final Writer writer, String rootElementName, String publicId, String systemId) {
        Converter converter = getConverter(object.getClass());
        Document document = Document.create();
        document.addRootElement(rootElementName);
        converter.toXml(object, document.getRoot());
        document.toXML(writer, false, publicId, systemId);

        return writer;
    }

    /**
     * Converts an element to an object of the given class
     */
    public Object fromXml(Class clazz, Element element) {
        Converter converter = getConverter(clazz);

        return converter.fromXml(element);
    }

    /**
     * Converts an XML string to an object, using the entity resolver given
     */
    public Object fromXml(Class clazz, String xml, EntityResolver entityResolver) {
        return fromXml(clazz, new StringReader(xml), entityResolver);
    }

    /**
     * Converts an XML stream from a reader to an object, using the entity resolver given
     */
    public Object fromXml(Class clazz, Reader reader, EntityResolver entityResolver) {
        Document document = Document.parse(reader, entityResolver);

        return fromXml(clazz, document.getRoot());
    }
}
