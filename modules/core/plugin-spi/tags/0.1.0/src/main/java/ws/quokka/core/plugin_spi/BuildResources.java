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
 *
 */
public interface BuildResources {
    //~ Methods --------------------------------------------------------------------------------------------------------

    URL getURL(String key);

    Map getURLs(String root);

    File getFile(String key);

    Map getFiles(String root);

    File getFileOrDir(String key);

    InputStream getAsStream(String key);
}
