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

import org.w3c.dom.Node;

import java.io.Writer;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * TransformingXmlWriter writes out XML pretty printing it in a manner that is compatible with
 * JDKs 1.4 to 1.6 (which is annoyingly inconsistent)
 */
public class TransformingXmlWriter {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Writes the XML node to the writer given
     * @param outputProperties will be set on the internal transformer instance. By default it sets
     * standalone=no, method=xml, indent=yes and {http://xml.apache.org/xslt}indent-amount=4
     */
    public void write(Writer writer, Node node, Properties outputProperties) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            try {
                // Required for jdk1.5 as it ignores indent-amount below
                transformerFactory.setAttribute("indent-number", new Integer(4));
            } catch (Exception e) {
                // Ignore ... dies on jdk1.4, but indent-amount below works
            }

            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.STANDALONE, "no"); // Doesn't seem to be a way to kill this ...
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            for (Iterator i = outputProperties.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                transformer.setOutputProperty((String)entry.getKey(), (String)entry.getValue());
            }

            transformer.transform(new DOMSource(node), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new BuildException(e);
        }
    }
}
