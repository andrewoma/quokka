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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * XmlParser supports the parsing requirements for bootstrappings. You should use the core utils Document
 * and Element classes outside of bootstrap util
 */
public class XmlParser {
    //~ Methods --------------------------------------------------------------------------------------------------------

    protected static Element parseXml(final File file) {
        return (Element)new ExceptionHandler() {
                public Object run() throws Exception {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setValidating(false);

                    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                    documentBuilder.setEntityResolver(new QuokkaEntityResolver());

                    return documentBuilder.parse(file).getDocumentElement();
                }
            }.soften();
    }

    protected static String getAttribute(Element element, String name) {
        Attr attribute = (Attr)element.getAttributes().getNamedItem(name);

        return (attribute == null) ? null : attribute.getValue();
    }

    protected static Element getChild(Element element, String name, boolean mandatory) {
        NodeList nodes = element.getChildNodes();
        org.w3c.dom.Element el = null;
        int count = 0;

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if ((node.getNodeType() == Node.ELEMENT_NODE) && node.getNodeName().equals(name)) {
                el = (org.w3c.dom.Element)node;
                count++;
            }
        }

        Assert.isTrue((mandatory && (count == 1)) || (!mandatory && ((count == 0) || (count == 1))),
            "'" + element.getNodeName() + "' expects " + (mandatory ? "1" : "0 or 1") + " child node of '" + name + "'");

        return el;
    }

    protected static List getChildren(Element element, String name, boolean mandatory) {
        return getChildren(element, new String[] { name }, mandatory);
    }

    protected static List getChildren(Element element, String[] names, boolean mandatory) {
        List namesList = Arrays.asList(names);
        List elements = new ArrayList();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if ((node.getNodeType() == Node.ELEMENT_NODE) && namesList.contains(node.getNodeName())) {
                elements.add(node);
            }
        }

        Assert.isTrue(!mandatory || (elements.size() != 0),
            "'" + element.getNodeName() + "' must have at least one '" + namesList + "' child");

        return elements;
    }
}
