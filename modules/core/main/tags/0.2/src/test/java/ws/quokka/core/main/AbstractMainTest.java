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


package ws.quokka.core.main;

import ws.quokka.core.model.*;
import ws.quokka.core.repo_spi.RepoArtifactId;
import ws.quokka.core.repo_spi.RepoPath;
import ws.quokka.core.test.AbstractTest;
import ws.quokka.core.util.Strings;

import java.util.Iterator;


/**
 *
 */
public class AbstractMainTest extends AbstractTest {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    protected DependencySet set;

    //~ Methods --------------------------------------------------------------------------------------------------------

    protected RepoArtifactId id(String id) {
        String[] tokens = Strings.split(id, ":");

        return new RepoArtifactId(tokens[0], (tokens.length > 1) ? tokens[1] : name(tokens[0]),
            (tokens.length > 2) ? tokens[2] : "jar", (tokens.length > 3) ? tokens[3] : "1.0");
    }

    private String name(String group) {
        String[] tokens = Strings.split(group, ".");

        return tokens[tokens.length - 1];
    }

    protected PluginDependency pldep(String id, String pathSpecs, String targets) {
        PluginDependency dependency = new PluginDependency();
        dependency.setId(id(id));

        String[] specs = Strings.trim(Strings.splitTopLevel(pathSpecs, '(', ')', ','));

        for (int i = 0; i < specs.length; i++) {
            PathSpec pathSpec = new PathSpec(specs[i], false);
            dependency.addPathSpec(pathSpec);
        }

        for (Iterator i = Strings.commaSepList(targets).iterator(); i.hasNext();) {
            String name = (String)i.next();
            PluginDependencyTarget target = new PluginDependencyTarget(name);
            dependency.addTarget(target);
        }

        if (set != null) {
            set.addDependency(dependency);
        }

        return dependency;
    }

    protected Dependency pdep(String id, String pathSpecs) {
        Dependency dependency = new Dependency();
        dependency.setId(id(id));

        String[] specs = Strings.trim(Strings.splitTopLevel(pathSpecs, '(', ')', ','));

        for (int i = 0; i < specs.length; i++) {
            PathSpec pathSpec = new PathSpec(specs[i]);

            if (set != null) {
                pathSpec.mergeDefaults((RepoPath)set.getPaths().get(pathSpec.getTo()));
            }

            dependency.addPathSpec(pathSpec);
        }

        if (set != null) {
            set.addDependency(dependency);
        }

        return dependency;
    }
}
