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

import org.apache.tools.ant.util.FileUtils;

import org.xml.sax.Locator;


/**
 * LocatorImpl provides a neutral implementation of Locator that is not tied to a specific
 * XML implementation
 */
public class LocatorImpl implements Locator {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public LocatorImpl(Locator locator) {
        publicId = locator.getPublicId();
        systemId = locator.getSystemId();
        lineNumber = locator.getLineNumber();
        columnNumber = locator.getColumnNumber();
    }

    public LocatorImpl(String publicId, String systemId, int lineNumber, int columnNumber) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public String getPublicId() {
        return publicId;
    }

    public String getSystemId() {
        return systemId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String toString() {
        String system = systemId;

        if ((system != null) && system.startsWith("file:")) {
            system = FileUtils.getFileUtils().fromURI(system);
        }

        return system + ":" + lineNumber; // + ":" + columnNumber; Column number is not accurate
    }
}
