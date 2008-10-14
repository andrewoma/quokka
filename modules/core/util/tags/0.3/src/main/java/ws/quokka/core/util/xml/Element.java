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


/**
 * A thin wrapper over {@link org.w3c.dom.Element} that provides helper methods for working with
 * elements, attributes and text nodes. It also implements certain methods in a manner to ensure
 * compatibility for JDKs 1.4 to 1.6
 */
public class Element {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private org.w3c.dom.Element element;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Element(org.w3c.dom.Element element) {
        this.element = element;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns the underlying element
     */
    public org.w3c.dom.Element getElement() {
        return element;
    }

    /**
     * Returns a descendant element of the given name that has a attribute with a certain value
     *
     * @param name the name of the child node.
     */
    public Element getDescendent(String name, String attribute, String value) {
        return getChild(name, attribute, value, true);
    }

    /**
     * Returns a child element of the given name that has a attribute with a certain value
     *
     * @param name the name of the child node.
     */
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

    /**
     * Returns child elements matching the name given
     * @param name the name of the child nodes.
     */
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

    /**
     * Returns the attribute value, or null if the attribute is not defined
     */
    public String getAttribute(String name) {
        return _getAttribute(name);
    }

    private String _getAttribute(String name) {
        Attr attribute = (Attr)element.getAttributes().getNamedItem(name);

        return (attribute == null) ? null : attribute.getValue();
    }

    /**
     * Returns the name of the element node
     */
    public String getName() {
        return element.getNodeName();
    }

    /**
     * Adds a child element with the name provided
     */
    public Element addChild(String name) {
        return new Element((org.w3c.dom.Element)element.appendChild(element.getOwnerDocument().createElement(name)));
    }

    /**
     * Adds a text node
     */
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

    /**
     * Returns the text content of this element
     */
    public String getText() {
        // element.getTextContent() breaks in jdk 1.4, so is implemented manually
        return getTextContent(element, new StringBuffer()).toString();
    }

    /**
     * Adds a child element with given name setting the value as its text content
     */
    public Element addChild(String name, String value) {
        org.w3c.dom.Element child = (org.w3c.dom.Element)element.appendChild(element.getOwnerDocument().createElement(name));
        child.appendChild(element.getOwnerDocument().createTextNode(value));

        return new Element(child);
    }

    /**
     * Sets the named attribute the value given
     */
    public Element setAttribute(String name, String value) {
        element.setAttribute(name, value);

        return this;
    }

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
