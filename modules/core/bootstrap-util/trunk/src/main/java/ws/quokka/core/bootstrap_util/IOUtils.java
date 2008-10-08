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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.net.URL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Properties;


/**
 * IOUtils provide some common IO routines
 */
public class IOUtils {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Saves properties to a file, softening any exceptions that occurs
     */
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

    /**
     * Loads properties from an input stream, softening any exceptions that occurs
     */
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

    /**
     * Loads properties from a file, softening any exceptions that occurs
     */
    public Properties loadProperties(File file) {
        return loadProperties(createURL(FileUtils.getFileUtils().toURI(file.getAbsolutePath())));
    }

    /**
     * Loads properties from a url, softening any exceptions that occurs
     */
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

    /**
     * Copies a stream to a file and closes the input stream when finished.
     */
    public void copyStream(final InputStream in, final File file) {
        new VoidExceptionHandler() {
                public void run() throws Exception {
                    OutputStream out = null;

                    try {
                        out = new FileOutputStream(file);

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
            };
    }

    /**
     * Copies the stream from an url to a file.
     */
    public void copyStream(final URL url, final File file) {
        new VoidExceptionHandler() {
                public void run() throws Exception {
                    InputStream inputStream = url.openStream();
                    Assert.isTrue(inputStream != null, "Cannot open stream from url: " + url);
                    copyStream(inputStream, file);
                }
            };
    }

    /**
     * Creates a URL from the spec, softening any exceptions
     */
    public URL createURL(final String spec) {
        return (URL)new ExceptionHandler() {
                public Object run() throws Exception {
                    return new URL(spec);
                }
            }.soften();
    }

    /**
     * Writes a string to the file using the default encoding
     */
    public void stringToFile(String string, File file) {
        stringToFile(string, file, System.getProperty("file.encoding"));
    }

    /**
     * Writes a string to the file using the specified encoding
     */
    public void stringToFile(String string, File file, String encoding) {
        try {
            StringReader reader = new StringReader(string);
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));

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

    /**
     * Reads a string from the file using the default encoding
     */
    public String fileToString(File file) {
        return fileToString(file, System.getProperty("file.encoding"));
    }

    /**
     * Reads a string from the file using the specified encoding
     */
    public String fileToString(File file, String encoding) {
        try {
            StringWriter writer = new StringWriter();
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));

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

    /**
     * Equivalent to {@link File#createTempFile(String, String)}, but softens any exceptions that occurs
     */
    public File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Equivalent to {@link File#createTempFile(String, String, File)}, but softens any exceptions that occurs
     */
    public File createTempFile(String prefix, String suffix, File directory) {
        try {
            return File.createTempFile(prefix, suffix, directory);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Returns a MD5 hash as a hex string of the bytes given
     */
    public String md5String(byte[] bytes) {
        return toHex(md5(bytes));
    }

    /**
     * Returns a MD5 hash of the bytes given
     */
    public byte[] md5(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.reset();
            messageDigest.update(bytes);

            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Converts the bytes given to a hex string representation
     */
    public String toHex(byte[] fileDigest) {
        StringBuffer hex = new StringBuffer();

        for (int i = 0; i < fileDigest.length; i++) {
            String hexStr = Integer.toHexString(0x00ff & fileDigest[i]);

            if (hexStr.length() < 2) {
                hex.append("0");
            }

            hex.append(hexStr);
        }

        return hex.toString();
    }

    /**
     * Downloads a file from the given url to the destination using Ant's Get task.
     * User and password may be null.
     */
    public void download(Project project, URL url, String user, String password, File destination) {
        try {
            Get get = (Get)project.createTask("get");
            get.setSrc(url);
            get.setUsername(user);
            get.setPassword(password);
            get.setDest(destination);
            get.setUseTimestamp(false);
            get.setIgnoreErrors(false);

            // Get is incredibly noisy ... this limits it's logging to verbose level only
            get.doGet(Project.MSG_VERBOSE, null);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Creates the directory if it doesn't already exist
     */
    public void createDir(File dir) {
        Assert.isTrue(dir.exists() || dir.mkdirs(), "Could not create: " + dir.getPath());
    }
}
