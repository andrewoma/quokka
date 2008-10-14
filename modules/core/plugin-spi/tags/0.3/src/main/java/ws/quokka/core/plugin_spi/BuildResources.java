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


package ws.quokka.core.plugin_spi;

import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.Map;


/**
 * BuildResources is essentially an abstraction for resources available in certain well known
 * locations with well known keys. They provide a mechanism for bundling things like XML configuration
 * files within dependency sets. The resources themselves might be available on the local file system
 * or may be bundled with a jar file.
 */
public interface BuildResources {
    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * Returns a URL corresponding to the given key
     * @throws org.apache.tools.ant.BuildException if no resource is found
     */
    URL getURL(String key);

    /**
     * Returns a Map of URLs available under the root specified.
     * @throws org.apache.tools.ant.BuildException if no resources exist under the root specified
     */
    Map getURLs(String root);

    /**
     * Returns a File corresponding to the given key. A temporary file is created if necessary
     * @throws org.apache.tools.ant.BuildException if no resource is found
     */
    File getFile(String key);

    /**
     * Returns a Map of files available under the root specified
     * @throws org.apache.tools.ant.BuildException if no resources exist under the root specified
     */
    Map getFiles(String root);

    /**
     * Returns a file corresponding to the key given. If only a single resource is availble under
     * the key given it will return that file. If there are multiple resources it will return
     * a directory containing the files.
     * @throws org.apache.tools.ant.BuildException if no resources exist under the root specified
     */
    File getFileOrDir(String key);

    /**
     * Opens a stream for the content of the resource corresponding to the key given.
     * It is the responsibility of the caller to close the stream when complete.
     * @throws org.apache.tools.ant.BuildException if no resources exist under the root specified
     */
    InputStream getAsStream(String key);
}
