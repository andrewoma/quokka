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


package ws.quokka.core.main.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.util.FileUtils;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.ExceptionHandler;
import ws.quokka.core.bootstrap_util.IOUtils;
import ws.quokka.core.plugin_spi.BuildResources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 */
public class DefaultBuildResources implements BuildResources {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Map resources = new HashMap();
    private File tempDir;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public File getTempDir() {
        return tempDir;
    }

    public URL getURL(String key) {
        URL url = (URL)resources.get(toKey(key));

        if (url == null) {
            throw new BuildException("Build resource '" + key + "' does not exist.");
        }

        return url;
    }

    public Map getURLs(String root) {
        Map urls = new HashMap();
        root = toKey(root);

        String dirKey = root + "/";

        for (Iterator i = resources.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();

            if (entry.getKey().equals(root) || ((String)entry.getKey()).startsWith(dirKey)) {
                urls.put(entry.getKey(), entry.getValue());
            }
        }

        if (urls.size() == 0) {
            throw new BuildException("Build resource '" + root + "' does not exist.");
        }

        return urls;
    }

    private String toKey(String key) {
        return key.replace('\\', '/');
    }

    /**
     * Returns a file matching the resource key. If the key returns multiple resources, a directory
     * will be returned containing the resources.
     */
    public File getFile(String key) {
        Map urls = new HashMap();
        urls.put(toKey(key), getURL(key));

        return (File)urlsToFiles(urls, false).values().iterator().next();
    }

    private Map urlsToFiles(Map urls, boolean commonRootRequired) {
        // Check to see if the URLs are all file URLs (and optionally, if they have the same root)
        Map files = inplaceUrlsToFiles(urls, commonRootRequired);

        if (files != null) {
            return files;
        }

        // Copy URLs to files
        return copyUrlsToFiles(urls);
    }

    private Map copyUrlsToFiles(Map urls) {
        Map files = new HashMap();

        for (Iterator i = urls.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();

            if (!key.endsWith("/")) { // TODO: fix this; it will silently drop empty dirs

                File destFile = copyUrl(key, (URL)entry.getValue());
                files.put(entry.getKey(), destFile);
            }
        }

        return files;
    }

    private File copyUrl(String key, URL url) {
        File destFile = new File(tempDir, key);

        try {
            copy(url, FileUtils.getFileUtils().normalize(destFile.getAbsolutePath()));
        } catch (IOException e) {
            throw new BuildException("Unable to copy build resources", e);
        }

        return destFile;
    }

    /**
     * Attempts to convert URLs to files. If commonRootRequired = true, it ensures that all files
     * descend from a common root. Null is return if the conversion fails.
     */
    private Map inplaceUrlsToFiles(Map urls, boolean commonRootRequired) {
        Map files = new HashMap();
        String root = null;

        for (Iterator i = urls.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            File file;

            try {
                file = new File(Locator.fromURI(entry.getValue().toString()));

                if (commonRootRequired) {
                    String fileRoot = getRoot(file, (String)entry.getKey());
                    root = (root == null) ? fileRoot : root;

                    if (!root.equals(fileRoot)) {
                        return null;
                    }
                }
            } catch (Exception e) {
                // Not a file URL
                if (commonRootRequired) {
                    return null;
                }

                file = copyUrl((String)entry.getKey(), (URL)entry.getValue());
            }

            files.put(entry.getKey(), file);
        }

        return files;
    }

    private String getRoot(File file, String key) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - key.length());
    }

    /**
     * Copies a URL to a file
     */
    private void copy(URL url, File file) throws IOException {
        // Check if the file is up to date
        if (isUpToDate(url, file)) {
            return;
        }

        // Create the parent directory if it doesn't exist
        File parentFile = file.getParentFile();
        Assert.isTrue(parentFile.exists() || parentFile.mkdirs(), "Unable to create directory: " + parentFile.getPath());

        // Remove any existing files
        Assert.isTrue(!file.exists() || file.delete(), "Unable to delete file: " + file.getPath());

        File tempFile = new File(parentFile, file.getName() + "$quokka.tmp");
        Assert.isTrue(!tempFile.exists() || tempFile.delete(), "Unable to delete file: " + tempFile.getPath());

        // Not a valid file URL, so retrieve data from URL and write to a file
        new IOUtils().copyStream(url, tempFile);

        Assert.isTrue(tempFile.renameTo(file), "Unable to rename " + tempFile.getPath() + " to " + file.getPath());
    }

    private boolean isUpToDate(URL url, File file) {
        return false; // TODO: implement checking to prevent unecessary copying
    }

    public void putAll(Map buildResources) {
        resources.putAll(buildResources);
    }

    public void put(String entry, URL resource) {
        resources.put(entry, resource);
    }

    public Map getFiles(String root) {
        return urlsToFiles(getURLs(root), false);
    }

    public File getFileOrDir(String key) {
        Map files = urlsToFiles(getURLs(key), true);
        Map.Entry entry = (Map.Entry)files.entrySet().iterator().next(); // Always at least one
        String firstKey = (String)entry.getKey();
        File firstValue = (File)entry.getValue();

        if (key.equals(firstKey) && (files.size() == 1)) {
            return firstValue;
        }

        int current = key.length();

        while ((current = firstKey.indexOf('/', current) + 1) != 0) {
            firstValue = firstValue.getParentFile();
        }

        return firstValue;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Iterator i = new TreeMap(resources).entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    public InputStream getAsStream(final String key) {
        return (InputStream)new ExceptionHandler() {
                public Object run() throws IOException {
                    return getURL(key).openStream();
                }
            }.soften();
    }
}
