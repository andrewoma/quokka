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


package ws.quokka.core.bootstrap.constraints;

import org.apache.tools.ant.BuildException;

import ws.quokka.core.bootstrap.resources.BootStrapResources;
import ws.quokka.core.bootstrap.resources.DependencyResource;
import ws.quokka.core.bootstrap.resources.Jdk;
import ws.quokka.core.bootstrap.resources.SuppliedDependencyResource;
import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Log;
import ws.quokka.core.version.VersionRange;
import ws.quokka.core.version.VersionRangeUnion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class BootStrapConstraints {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private List coreConstraints = new ArrayList();
    private List jdkConstraints = new ArrayList();
    private List dependencyContraints = new ArrayList();

    //~ Methods --------------------------------------------------------------------------------------------------------

    public List getCoreConstraints() {
        return coreConstraints;
    }

    public void setCoreConstraints(List coreConstraints) {
        this.coreConstraints = coreConstraints;
    }

    public List getJdkConstraints() {
        return jdkConstraints;
    }

    public void setJdkConstraints(List jdkConstraints) {
        this.jdkConstraints = jdkConstraints;
    }

    public List getDependencyContraints() {
        return dependencyContraints;
    }

    public void setDependencyContraints(List dependencyContraints) {
        this.dependencyContraints = dependencyContraints;
    }

    public Jdk findMatchingJdk(BootStrapResources resources) {
        Jdk match = findMatchingJdk(resources, true);
        match = (match == null) ? findMatchingJdk(resources, false) : match;

        return match;
    }

    private Jdk findMatchingJdk(BootStrapResources resources, boolean matchOptional) {
        for (Iterator i = jdkConstraints.iterator(); i.hasNext();) {
            JdkConstraint required = (JdkConstraint)i.next();

            for (Iterator j = resources.getJdks().iterator(); j.hasNext();) {
                Jdk available = (Jdk)j.next();

                if (Log.get().isDebugEnabled()) {
                    Log.get().debug("Matching against jdk: " + available.getLocation().getAbsolutePath());
                }

                if (required.matches(available, matchOptional)) {
                    available.setMatchedConstraint(required);

                    return available;
                }
            }
        }

        return null;
    }

    public DependencyResource findMatchingCore(BootStrapResources resources) {
        DependencyResource match = null;

        for (Iterator i = coreConstraints.iterator(); i.hasNext();) {
            CoreConstraint required = (CoreConstraint)i.next();
            DependencyConstraint requiredDependency = new DependencyConstraint("quokka.bundle", "core",
                    required.getVersion(), null, null);
            match = findMatch(resources, requiredDependency, !i.hasNext());

            if (match != null) {
                break;
            }
        }

        return match;
    }

    public boolean isEmpty() {
        return (jdkConstraints.size() == 0) && (dependencyContraints.size() == 0) && (coreConstraints.size() == 0);
    }

    private DependencyResource findMatch(BootStrapResources resources, DependencyConstraint dependency,
        boolean mandatory) {
        if (Log.get().isDebugEnabled()) {
            Log.get().debug("Matching " + dependency);
        }

        for (Iterator j = resources.getAvailableLibraries().iterator(); j.hasNext();) {
            DependencyResource available = (DependencyResource)j.next();
            boolean match = dependency.matches(available);

            if (Log.get().isDebugEnabled()) {
                Log.get().debug("   " + available + (match ? " matches" : " doesn't match"));
            }

            if (match) {
                return available;
            }
        }

        if ((dependency.getFile() != null) || (dependency.getUrl() != null)) {
            // Not available in the quokka library dir, so use the file/url provided
            // Note: this will effectively force bootstrapping
            VersionRangeUnion union = dependency.getVersion();
            String message = "When supplying a boot dependency, the version must be an exact version of format [n,n]: "
                + union;
            Assert.isTrue(union.getRanges().size() == 1, message);

            VersionRange range = (VersionRange)union.getRanges().get(0);
            Assert.isTrue(range.getHigh() != null && range.getLow() != null, message);
            Assert.isTrue(range.getHigh().equals(range.getLow()), message);
            Assert.isTrue(range.isHighInclusive() && range.isLowInclusive(), message);

            return new SuppliedDependencyResource(dependency.getGroup(), dependency.getName(), range.getHigh(),
                dependency.getFile(), dependency.getUrl());
        }

        if (mandatory) {
            throw new BuildException("There are no boostrap libraries available that satifisy: " + dependency);
        }

        return null;
    }

    public List findMatchingDependencies(BootStrapResources resources) {
        List matches = new ArrayList();

        for (Iterator i = dependencyContraints.iterator(); i.hasNext();) {
            DependencyConstraint dependency = (DependencyConstraint)i.next();
            matches.add(findMatch(resources, dependency, true));
        }

        return matches;
    }
}
