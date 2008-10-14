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


package ws.quokka.core.model;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoOverride;
import ws.quokka.core.util.Strings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 */
public class Override extends RepoOverride {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Set pluginPaths = new HashSet();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void addPluginPath(String path) {
        pluginPaths.add(path);
    }

    public Set getPluginPaths() {
        return Collections.unmodifiableSet(pluginPaths);
    }

    /**
     * Returns paths that match the plugin specified
     * Format is: group[:name]=path1[:pathn]
     */
    public Set matchingPluginPaths(RepoArtifactId pluginId) {
        if ((pluginPaths.size() == 1) && pluginPaths.contains("*")) {
            return pluginPaths; // Applies globally to all paths within all plugins
        }

        Set matching = new HashSet();

        for (Iterator i = pluginPaths.iterator(); i.hasNext();) {
            String path = (String)i.next();

            // Split group from paths
            String[] tokens = Strings.split(path, "=");
            assertPath(tokens.length == 2, path);

            // Split group and name
            String[] groupTokens = Strings.split(tokens[0], ":");
            assertPath((groupTokens.length >= 1) && (groupTokens.length <= 2), path);

            String group = groupTokens[0];
            String name;

            if (groupTokens.length == 1) {
                String[] nameTokens = Strings.split(groupTokens[0], ".");
                name = nameTokens[nameTokens.length - 1];
            } else {
                name = groupTokens[1];
            }

            // Check if the group and name match
            if (pluginId.getGroup().equals(group) && pluginId.getName().equals(name)) {
                matching.addAll(Strings.asList(Strings.split(tokens[1], ":")));
            }
        }

        return matching;
    }

    private void assertPath(boolean condition, String path) {
        Assert.isTrue(condition, getLocator(),
            "Override plugin path does not match format '*|group[:name]=*|path1[:pathn]': " + path);
    }
}
