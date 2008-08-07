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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.quokka.core.bootstrap_util.Assert;

import java.util.ArrayList;
import java.util.List;


public class Element {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private org.w3c.dom.Element element;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Element(org.w3c.dom.Element element) {
        this.element = element;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public org.w3c.dom.Element getElement() {
        return element;
    }

    /**
     * Returns a child element of the given name that has a attribute with a certain value
     *
     * @param name the name of the child node.
     */
    public Element getDescendent(String name, String attribute, String value) {
        return getChild(name, attribute, value, true);
    }

    public Element getChild(String name, String attribute, String value) {
        return getChild(name, attribute, value, false);
    }

    private Element getChild(String name, String attribute, String value, boolean descend) {
        NodeList nodes = this.element.getChildNodes();
        Element match = null;
        int count = 0;

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = new Element((org.w3c.dom.Element)node);

                if (el.getName().equals(name) && ((attribute == null) || value.equals(el.getAttribute(attribute)))) {
                    match = el;
                    count++;
                }

                if (descend) {
                    el = el.getChild(name, attribute, value, descend);

                    if (el != null) {
                        match = el;
                        count++;
                    }
                }
            }
        }

        Assert.isTrue(count <= 1, "Invalid use of getChild(), node count > 1");

        return (match == null) ? null : match;
    }

    /**
     * Returns the child element of this element.
     *
     * @param name the name of the child node.
     */
    public Element getChild(String name) {
        return getChild(name, null, null);
    }

    public List getChildren(String name) {
        List elements = new ArrayList();
        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if ((node.getNodeType() == Node.ELEMENT_NODE) && node.getNodeName().equals(name)) {
                elements.add(new Element((org.w3c.dom.Element)node));
            }
        }

        return elements;
    }

    public String getAttribute(String name) {
        return _getAttribute(name);
    }

    private String _getAttribute(String name) {
        Attr attribute = (Attr)element.getAttributes().getNamedItem(name);

        return (attribute == null) ? null : attribute.getValue();
    }

    public String getName() {
        return element.getNodeName();
    }

    public Element addChild(String name) {
        return new Element((org.w3c.dom.Element)element.appendChild(element.getOwnerDocument().createElement(name)));
    }

    public Element addText(String text) {
        Assert.isTrue(text != null, "Text is null");
        element.appendChild(element.getOwnerDocument().createTextNode(text));

        return this;
    }

    /**
     * Convenience method to get the text content of a child node, or null if the node doesn't exist
     */
    public String getChildText(String child) {
        Element childEl = getChild(child);

        return (childEl == null) ? null : childEl.getText();
    }

    public String getText() {
        // element.getTextContent() breaks in jdk 1.4, so is implemented manually
        return getTextContent(element, new StringBuffer()).toString();
    }

    public Element addChild(String name, String value) {
        org.w3c.dom.Element child = (org.w3c.dom.Element)element.appendChild(element.getOwnerDocument().createElement(name));
        child.appendChild(element.getOwnerDocument().createTextNode(value));

        return new Element(child);
    }

    public Element setAttribute(String name, String value) {
        element.setAttribute(name, value);

        return this;
    }

    //    public Element clone(boolean deep) {
    //        return new Element((org.w3c.dom.Element) element.cloneNode(deep));
    //    }
    //
    //    public void addChild(Element element) {
    //        this.element.appendChild(element.getElement());
    //    }
    private static StringBuffer getTextContent(Node node, StringBuffer sb)
            throws DOMException {
        Node child = node.getFirstChild();

        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                String nodeValue = child.getNodeValue();

                if (nodeValue != null) {
                    sb.append(nodeValue);
                }
            }

            child = child.getNextSibling();
        }

        return sb;
    }
}
