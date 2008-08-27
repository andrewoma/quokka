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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 *
 */
public class ArtifactPropertiesParser {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public Properties parse(String group, String name, String type) {
        String entryName = getEntryName(group, name, type);
        InputStream in = getClass().getClassLoader().getResourceAsStream(entryName);
        Assert.isTrue(in != null,
            "Cannot find properties '" + entryName + "' from the class path with class loader: "
            + getClass().getClassLoader().getClass().getName());

        return new IOUtils().loadProperties(in);
    }

    public Properties parse(final File file, final String group, final String name, final String type) {
        return (Properties)new ExceptionHandler() {
                public Object run() throws IOException {
                    return parse_(file, group, name, type);
                }
            }.soften();
    }

    private Properties parse_(File file, String group, String name, String type)
            throws IOException {
        JarFile jarFile = new JarFile(file);

        try {
            String entryName = getEntryName(group, name, type);
            JarEntry entry = jarFile.getJarEntry(entryName);
            Assert.isTrue(entry != null,
                "Cannot find properties '" + entryName + "' within file '" + file.getAbsolutePath() + "'");

            InputStream in = jarFile.getInputStream(entry);

            return new IOUtils().loadProperties(in);
        } finally {
            jarFile.close();
        }
    }

    private String getEntryName(String group, String name, String type) {
        return "META-INF/quokka/" + group + "_" + name + "_" + type + "_artifacts.properties";
    }
}
