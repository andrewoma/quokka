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
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.xindice.util.StringUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.quokka.core.bootstrap_util.VoidExceptionHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;


/**
 * TextWriter takes a Document, DocumentFragment, or Element and
 * streams it as text into an {@link OutputStream}, {@link Writer},
 * or a {@link String}.
 * <p/>
 * UTF-8 output encoding is assumed.
 *
 * @version $Revision: 541508 $, $Date: 2007-05-24 18:54:12 -0700 (Thu, 24 May 2007) $
 */
public final class XmlWriter {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Node node;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public XmlWriter(Node node) {
        this.node = node;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * write writes the node to the writer as text.
     *
     * @param writer The Writer to write to
     */
    public void write(Writer writer) throws IOException {
        write(node, writer);
    }

    /**
     * write writes the node to the OutputStream as text.
     *
     * @param output The OutputStream to write to
     */
    public void write(OutputStream output) throws IOException {
        write(node, output);
    }

    /**
     * toString returns the node as a String.
     *
     * @return The String value
     */
    public String toString() {
        return toString(node);
    }

    private static void writeNode(Writer writer, Node node)
            throws IOException {
        short type = node.getNodeType();

        switch (type) {
        case Node.DOCUMENT_NODE:
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writeChildren(writer, node);

            break;

        case Node.DOCUMENT_FRAGMENT_NODE:
            writeChildren(writer, node);

            break;

        case Node.DOCUMENT_TYPE_NODE:

            // Not implemented
            break;

        case Node.ELEMENT_NODE:

            Element e = (Element)node;
            String n = e.getTagName();

            writer.write('<');
            writer.write(n);

            NamedNodeMap a = e.getAttributes();
            int size = a.getLength();

            for (int i = 0; i < size; i++) {
                Attr att = (Attr)a.item(i);
                writer.write(' ');
                writeNode(writer, att);
            }

            if (e.hasChildNodes()) {
                writer.write('>');
                writeChildren(writer, node);
                writer.write("</");
                writer.write(n);
                writer.write('>');
            } else {
                writer.write(" />");
            }

            break;

        case Node.ATTRIBUTE_NODE:

            Attr att = (Attr)node;
            writer.write(att.getName());
            writer.write("=\"");
            writer.write(escapeXml(att.getValue(), false));
            writer.write("\"");

            break;

        case Node.ENTITY_REFERENCE_NODE:

            // Not implemented
            break;

        case Node.ENTITY_NODE:

            // Not implemented
            break;

        case Node.NOTATION_NODE:

            // Not implemented
            break;

        case Node.PROCESSING_INSTRUCTION_NODE:

            // Not implemented
            //                ProcessingInstruction pi = (ProcessingInstruction) node;
            //                writer.write("<?");
            //                writer.write(pi.getTarget());
            //                writer.write(" ");
            //                writer.write(pi.getData());
            //                writer.write("?>\n");
            break;

        case Node.TEXT_NODE:
            writer.write(escapeXml(node.getNodeValue(), true));

            break;

        case Node.CDATA_SECTION_NODE:
            writer.write("<![CDATA[");
            writer.write(node.getNodeValue());
            writer.write("]]>");

            break;

        case Node.COMMENT_NODE:
            writer.write("<!--");
            writer.write(node.getNodeValue());
            writer.write("-->");

            break;

        default:}
    }

    private static void writeChildren(Writer writer, Node node)
            throws IOException {
        NodeList l = node.getChildNodes();
        int size = l.getLength();

        for (int i = 0; i < size; i++) {
            writeNode(writer, l.item(i));
        }
    }

    /**
     * write writes the specified node to the writer as text.
     *
     * @param node   The Node to write
     * @param writer The Writer to write to
     */
    public static void write(Node node, Writer writer)
            throws IOException {
        BufferedWriter buf = new BufferedWriter(writer, 4096);
        writeNode(buf, node);
        buf.flush();
    }

    /**
     * write writes the specified node to the OutputStream as text.
     *
     * @param node   The Node to write
     * @param output The OutputStream to write to
     */
    public static void write(Node node, OutputStream output)
            throws IOException {
        OutputStreamWriter o = new OutputStreamWriter(output, "utf-8");
        write(node, o);
    }

    /**
     * toString returns the node as a String.
     *
     * @param node The Node to convert
     * @return The String value
     */
    public static String toString(final Node node) {
        final StringWriter sw = new StringWriter();
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    write(node, sw);
                }
            };

        return sw.toString();
    }

    /**
     * Converts input text into its XML representation by escaping all special symbols,
     * if any are present.
     *
     * @param text Input string
     * @return String with all the special symbols escaped
     */
    public static String escapeXml(String text, boolean includeApos) {
        char[] value = text.toCharArray();
        StringBuffer buf = new StringBuffer();
        int start = 0;
        int len = 0;

        for (int i = 0; i < value.length; i++) {
            String outval = null;

            switch (value[i]) {
            case '&':
                outval = "&amp;";

                break;

            case '\'':
                outval = includeApos ? "&apos;" : "'";

                break;

            case '\"':
                outval = "&quot;";

                break;

            case '<':
                outval = "&lt;";

                break;

            case '>':
                outval = "&gt;";

                break;

            default:
                len++;

                break;
            }

            if (outval != null) {
                if (len > 0) {
                    buf.append(value, start, len);
                }

                buf.append(outval);
                start = i + 1;
                len = 0;
            }
        }

        if (len > 0) {
            buf.append(value, start, len);
        }

        return buf.toString();
    }
}
