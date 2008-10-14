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

import ws.quokka.core.bootstrap_util.ProfilesMatcher;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Strings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 *
 */
public class Profiles extends AnnotatedObject {
    //~ Instance fields ------------------------------------------------------------------------------------------------

    private Set elements = new HashSet();

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public Profiles() {
    }

    public Profiles(String elements) {
        setElements(elements);
    }

    public Profiles(Set elements) {
        this.elements = elements;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void add(String profile) {
        elements.add(profile);
    }

    public void setElements(String elements) {
        if (elements == null) {
            this.elements = new HashSet();
        }

        this.elements = new HashSet(Strings.commaSepList(elements));
    }

    public Set getElements() {
        return Collections.unmodifiableSet(elements);
    }

    public boolean matches(String expression) {
        if (expression == null) {
            return true;
        }

        return new ProfilesMatcher().matches(expression, elements);
    }

    public String toShortString() {
        return STRING_GENERATOR.toShortString(elements);
    }
}
