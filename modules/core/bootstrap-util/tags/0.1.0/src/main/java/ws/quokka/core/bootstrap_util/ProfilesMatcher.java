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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 *
 */
public class ProfilesMatcher {
    //~ Methods --------------------------------------------------------------------------------------------------------

    public boolean matches(Set profiles, Set activeProfiles) {
        if (profiles.size() == 0) {
            return true;
        }

        boolean positive = convertNegatives(profiles);

        return positive ? containsAny(activeProfiles, profiles) : (!containsAny(activeProfiles, profiles));
    }

    private boolean containsAny(Set activeProfiles, Set profiles) {
        for (Iterator i = profiles.iterator(); i.hasNext();) {
            String profile = (String)i.next();

            if (activeProfiles.contains(profile)) {
                return true;
            }
        }

        return false;
    }

    private boolean convertNegatives(Set profiles) {
        List negatives = new ArrayList();

        for (Iterator i = profiles.iterator(); i.hasNext();) {
            String profile = (String)i.next();

            if (profile.startsWith("-")) {
                Assert.isTrue(profile.length() > 1, "Invalid profile id. '-' must be followed by a profile name");
                negatives.add(profile.substring(1));
            }
        }

        Assert.isTrue((negatives.size() == 0) || (negatives.size() == profiles.size()),
            "Profiles must be either all negative or all positive");

        if (negatives.size() != 0) {
            profiles.clear();
            profiles.addAll(negatives);
        }

        return negatives.size() == 0;
    }
}
