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

import java.util.List;


/**
 *
 */
public class ElementTest extends TestCase {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Element root;

    //~ Methods --------------------------------------------------------------------------------------------------------

    private void setRoot(String xml) {
        root = getRoot(xml);
    }

    private Element getRoot(String xml) {
        Document document = Document.parse(xml);

        return document.getRoot();
    }

    public void testGetChildElementMandatory() {
        setRoot("<root> <!-- --> <child/></root>");

        Element child = root.getChild("child");
        assertTrue((child != null) && child.getName().equals("child"));
    }

    public void testGetChildElementOptional() {
        setRoot("<root><child/></root>");

        Element child = root.getChild("child");
        assertTrue((child != null) && child.getName().equals("child"));
    }

    public void testGetChildElementOptionalNotPresent() {
        setRoot("<root/>");

        Element child = root.getChild("child");
        assertNull(child);
    }

    public void testGetAttribute() {
        setRoot("<root att1=\"att1value\"/>");
        assertEquals("att1value", root.getAttribute("att1"));
    }

    public void testGetAttributeMissing() {
        setRoot("<root/>");
        assertNull(root.getAttribute("att1"));
    }

    public void testGetChildElements() {
        setRoot("<root><!-- --> <child/></root>");

        List children = root.getChildren("child");
        assertTrue((children.size() == 1) && ((Element)children.get(0)).getName().equals("child"));
    }

    public void testGetChildElementsMulitple() {
        setRoot("<root><child/><child/></root>");

        List children = root.getChildren("child");
        assertTrue((children.size() == 2) && ((Element)children.get(0)).getName().equals("child")
            && ((Element)children.get(1)).getName().equals("child"));
    }

    public void testGetChildElementsOptionalPresent() {
        setRoot("<root><child/></root>");

        List children = root.getChildren("child");
        assertTrue((children.size() == 1) && ((Element)children.get(0)).getName().equals("child"));
    }

    public void testGetChildElementsOptionalMissing() {
        setRoot("<root/>");

        List children = root.getChildren("child");
        assertEquals(0, children.size());
    }

    public void testAddChild() {
        setRoot("<root/>");

        String name = "child";
        root.addChild(name);
        assertEquals(name, root.getChild(name).getName());
    }

    public void testAddChildWithValue() {
        setRoot("<root/>");

        String name = "child";
        String value = "value";
        root.addChild(name, value);
        assertEquals(name, root.getChild(name).getName());
        assertEquals(value, root.getChild(name).getText());
    }

    public void testAddText() {
        setRoot("<root/>");

        String text = "Hello there";
        root.addText(text);
        assertEquals(text, root.getText());
    }

    public void testSetAttribute() {
        setRoot("<root/>");
        root.setAttribute("attribute1", "value1");
        assertEquals("value1", root.getAttribute("attribute1"));
    }
}
