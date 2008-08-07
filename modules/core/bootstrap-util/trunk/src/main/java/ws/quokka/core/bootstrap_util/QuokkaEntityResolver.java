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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import java.util.*;


/**
 *
 */
public class QuokkaEntityResolver implements EntityResolver {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map versions = new HashMap();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void addVersion(String entity, String[] versions) {
        this.versions.put(entity, versions);
    }

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String prefix = "quokka.ws/dtd/";

        if (publicId.startsWith(prefix)) {
            for (Iterator i = versions.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String entity = (String)entry.getKey();
                String[] versions = (String[])entry.getValue();

                assertVersion(publicId, prefix + entity + "-", versions);
            }

            String dtd = "META-INF/" + publicId + ".dtd";
            InputStream in = getClass().getClassLoader().getResourceAsStream(dtd);
            Assert.isTrue(in != null, "Cannot find " + dtd + " on the class path");

            return new InputSource(in);
        }

        return null;
    }

    public String[] getVersion(String entity) {
        return (String[])versions.get(entity);
    }

    private void assertVersion(String publicId, String prefix, String[] versions)
            throws SAXException {
        if (!publicId.startsWith(prefix)) {
            return;
        }

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];

            if (publicId.substring(prefix.length()).equals(version)) {
                return;
            }
        }

        throw new SAXException("The dtd referenced by '" + publicId + "' does not match the required version(s) of '"
            + Arrays.asList(versions).toString() + "'");
    }
}
