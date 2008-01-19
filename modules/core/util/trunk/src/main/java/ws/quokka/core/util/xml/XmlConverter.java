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
 *
 */
public class XmlConverter {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List converters = new ArrayList();
    private Map context = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void add(Converter converter) {
        converters.add(converter);
        converter.setXmlConverter(this);
    }

    public Converter getConverter(Class clazz) {
        for (Iterator i = converters.iterator(); i.hasNext();) {
            Converter converter = (Converter)i.next();

            if (converter.supports(clazz)) {
                return converter;
            }
        }

        throw new BuildException("No xml converter is defined for class: " + clazz.getName());
    }

    public void removeContext(String key) {
        this.context.remove(key);
    }

    public void addContext(String key, Object context) {
        this.context.put(key, context);
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public String toXml(Object object, String rootElementName) {
        return toXml(object, new StringWriter(), rootElementName).toString();
    }

    public Writer toXml(Object object, final Writer writer, String rootElementName) {
        Converter converter = getConverter(object.getClass());
        Document document = Document.create();
        document.addRootElement(rootElementName);
        converter.toXml(object, document.getRoot());
        document.toXML(writer, false);

        return writer;
    }

    public Object fromXml(Class clazz, Element element) {
        Converter converter = getConverter(clazz);

        return converter.fromXml(element);
    }

    public Object fromXml(Class clazz, String xml) {
        return fromXml(clazz, new StringReader(xml));
    }

    public Object fromXml(Class clazz, Reader reader) {
        Document document = Document.parse(reader);

        return fromXml(clazz, document.getRoot());
    }
}
