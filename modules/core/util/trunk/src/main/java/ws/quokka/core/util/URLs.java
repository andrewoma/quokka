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


package ws.quokka.core.util;

import org.apache.tools.ant.launch.Locator;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Method;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 *
 */
public class URLs {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns a URL for the file and path specified
     *
     * @param file either an existing directory or .jar file. An exception is thrown otherwise
     * @param path the relative path of the entry to retrieve
     * @return a URL if the entry specified by the path exists, or null otherwise
     */
    public static URL toURL(final File file, String path) {
        validateFile(file);
        path = path.startsWith("/") ? path.substring(1) : path;

        final String finalPath = path;

        return (URL)new ExceptionHandler() {
                public Object run() throws IOException {
                    if (file.isDirectory()) {
                        return toFileURL(file, finalPath);
                    } else {
                        return toJarURL(file, finalPath);
                    }
                }
            }.soften();
    }

    public static Map toURLEntries(final File file, String path) {
        validateFile(file);

        // Normalise path
        if (path.equals("/") || path.equals("")) {
            path = "";
        } else {
            path = path.startsWith("/") ? path.substring(1) : path;
            path = path.endsWith("/") ? path : (path + "/");
        }

        final String finalPath = path;

        return (Map)new ExceptionHandler() {
                public Object run() throws IOException {
                    if (file.isDirectory()) {
                        return toFileURLEntries(file, finalPath);
                    } else {
                        return toJarURLEntries(file, finalPath);
                    }
                }
            }.soften();
    }

    private static void validateFile(File file) {
        Assert.isTrue(file.exists() && (file.isDirectory() || file.getAbsolutePath().toLowerCase().endsWith(".jar")),
            "File specified must be an existing .jar file or directory: " + file.getPath());
    }

    private static Map toJarURLEntries(File file, String path)
            throws IOException {
        Map entries = new HashMap();
        path = path.replace('\\', '/');

        JarFile pluginFile = new JarFile(file);

        for (Enumeration e = pluginFile.entries(); e.hasMoreElements();) {
            JarEntry entry = (JarEntry)e.nextElement();

            if (entry.getName().startsWith(path)) {
                String key = entry.getName().substring(path.length()).toLowerCase();

                if (!key.equals("")) { // Don't add the root itself
                    entries.put(key, new URL("jar:" + toURL(file) + "!/" + entry.getName()));
                }
            }
        }

        return entries;
    }

    private static Map toFileURLEntries(File file, String path) {
        Map entries = new HashMap();
        File rootDir = new File(file, normalise(path));

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return entries;
        }

        addEntries(rootDir, entries, rootDir);

        return entries;
    }

    private static void addEntries(File rootDir, Map entries, File dir) {
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (file.isDirectory()) {
                entries.put(file.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1).toLowerCase()
                    .replace('\\', '/') + "/", toURL(file));
                addEntries(rootDir, entries, file);
            } else {
                entries.put(file.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1).toLowerCase()
                    .replace('\\', '/'), toURL(file));
            }
        }
    }

    private static URL toJarURL(File file, String path)
            throws IOException {
        path = path.replace('\\', '/');

        JarFile pluginFile = new JarFile(file);
        JarEntry repositoryEntry = pluginFile.getJarEntry(path);

        if ((repositoryEntry == null) && !path.equals("")) {
            return null;
        } else {
            return new URL("jar:" + toURL(file) + "!/" + path);
        }
    }

    private static URL toFileURL(File file, String path) {
        File destFile = new File(file, normalise(path));

        if (destFile.exists()) {
            return toURL(destFile);
        } else {
            return null;
        }
    }

    private static String normalise(String path) {
        return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }

    /**
     * Returns a properly escaped URL, unlike File.toURL().
     */
    public static URL toURL(final File file) {
        return (URL)new ExceptionHandler() {
                public Object run() throws UnsupportedEncodingException, MalformedURLException {
                    return new URL(Locator.encodeURI(file.toURL().toString()));
                }
            }.soften();
    }

    /**
     * Opens the url in a browser. If exectuable is null, it attempts to find the native browser automatically
     * Based on public domain code from Bare Bones Browser Launch for Java (http://www.centerkey.com/java/browser/)
     *
     * @param url
     * @param browser
     */
    public static void openBrowser(final URL url, final String browser) {
        new VoidExceptionHandler() {
                public void run() throws Exception {
                    String lUrl = url.toExternalForm();
                    String lBrowser = browser;

                    if (lBrowser == null) {
                        String osName = System.getProperty("os.name");

                        if (osName.startsWith("Mac OS")) {
                            Class fileMgr = Class.forName("com.apple.eio.FileManager");
                            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                            openURL.invoke(null, new Object[] { lUrl });

                            return;
                        }

                        if (osName.startsWith("Windows")) {
                            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + lUrl);

                            return;
                        }

                        // Assume Unix or Linux
                        String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

                        for (int count = 0; (count < browsers.length) && (lBrowser == null); count++) {
                            if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0) {
                                lBrowser = browsers[count];
                            }
                        }

                        Assert.isTrue(lBrowser != null, "Could not find web browser");
                    }

                    Runtime.getRuntime().exec(new String[] { lBrowser, lUrl });
                }
            };
    }
}
