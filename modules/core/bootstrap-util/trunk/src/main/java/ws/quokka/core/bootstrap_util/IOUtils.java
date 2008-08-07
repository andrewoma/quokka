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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

import java.io.*;

import java.net.URL;

import java.util.Properties;


/**
 *
 */
public class IOUtils {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public void saveProperties(final File file, final Properties properties) {
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    saveProperties_(file, properties);
                }
            };
    }

    private void saveProperties_(File file, Properties properties)
            throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));

        try {
            properties.store(out, null);
        } finally {
            out.close();
        }
    }

    public Properties loadProperties(final InputStream in) {
        return (Properties)new ExceptionHandler() {
                public Object run() throws Exception {
                    return loadProperties_(in);
                }
            }.soften();
    }

    private Properties loadProperties_(InputStream in)
            throws IOException {
        try {
            Properties properites = new Properties();
            properites.load(in);

            return properites;
        } finally {
            in.close();
        }
    }

    public Properties loadProperties(File file) {
        return loadProperties(createURL(FileUtils.getFileUtils().toURI(file.getAbsolutePath())));
    }

    public Properties loadProperties(final URL url) {
        return (Properties)new ExceptionHandler() {
                public Object run() throws Exception {
                    return loadProperties_(url);
                }
            }.soften();
    }

    private Properties loadProperties_(URL url) throws IOException {
        // JDK 1.2 can't hanlde URLs with escaped spaces
        InputStream in;

        if (url.toExternalForm().startsWith("file:")) {
            in = new BufferedInputStream(new FileInputStream(FileUtils.getFileUtils().fromURI(url.toExternalForm())));
        } else {
            in = url.openStream();
        }

        return loadProperties_(in);
    }

    public void copyStream(InputStream in, OutputStream out)
            throws IOException {
        try {
            byte[] buffer = new byte[4096];

            while (true) {
                int bytes = in.read(buffer);

                if (bytes == -1) {
                    break;
                }

                out.write(buffer, 0, bytes);
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public URL createURL(final String spec) {
        return (URL)new ExceptionHandler() {
                public Object run() throws Exception {
                    return new URL(spec);
                }
            }.soften();
    }

    public void stringToFile(String string, File file) {
        try {
            StringReader reader = new StringReader(string);
            Writer writer = new BufferedWriter(new FileWriter(file));

            try {
                while (true) {
                    int ch = reader.read();

                    if (ch == -1) {
                        return;
                    }

                    writer.write(ch);
                }
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public String fileToString(File file) {
        try {
            StringWriter writer = new StringWriter();
            Reader reader = new BufferedReader(new FileReader(file));

            try {
                while (true) {
                    int ch = reader.read();

                    if (ch == -1) {
                        return writer.toString();
                    }

                    writer.write(ch);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public File createTempFile(String prefix, String suffix, File directory) {
        try {
            return File.createTempFile(prefix, suffix, directory);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
