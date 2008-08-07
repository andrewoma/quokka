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


package ws.quokka.core.repo_spi;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.util.AnnotatedObject;
import ws.quokka.core.util.Strings;


/**
 *
 */
public class RepoPathSpec extends AnnotatedObject {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final String RUNTIME = "runtime";

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private String from;
    private String to;
    private String options;
    private Boolean descend;
    private Boolean mandatory;
    private RepoDependency dependency;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    public RepoPathSpec() {
    }

    public RepoPathSpec(String shorthand, boolean toRequired) {
        parseShorthand(shorthand, toRequired);
    }

    public RepoPathSpec(String shorthand) {
        parseShorthand(shorthand, true);
    }

    public RepoPathSpec(String fromPath, String toPath, String options, Boolean descend, Boolean mandatory) {
        this.from = fromPath;
        this.to = toPath;
        this.options = options;
        this.descend = descend;
        this.mandatory = mandatory;
    }

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void parseShorthand(String shorthand, boolean toRequired) {
        if (toRequired) {
            parseShorthand(shorthand);
        } else {
            parseShorthand("*" + (isDelimeter(shorthand.substring(0, 1), "?!<+=") ? "" : "=") + shorthand);
            Assert.isTrue(to.equals("*"), "The to path cannot be specified for this path specification: " + shorthand);
            to = null;
            from = (from == null) ? RUNTIME : from;
        }
    }

    private void parseShorthand(String shorthand) {
        String[] tokens = Strings.trim(Strings.splitIncludeDelimiters(shorthand, "<+="));
        assertPathSpec((tokens.length >= 1) && (tokens.length <= 3), shorthand);
        assertPathSpec(!isDelimeter(tokens[0], "<+=") && !Strings.isBlank(tokens[0]), shorthand);

        String toId = tokens[0];
        assertPathSpec(!isDelimeter(toId, "?!"), shorthand);

        if (toId.endsWith("?") || toId.endsWith("!")) {
            mandatory = toId.endsWith("?") ? Boolean.FALSE : Boolean.TRUE;
            toId = toId.substring(0, toId.length() - 1);
        }

        to = toId;

        if (tokens.length >= 2) {
            assertPathSpec(isDelimeter(tokens[1], "<+="), shorthand);

            if (tokens[1].equals("<")) {
                descend = Boolean.TRUE;
            } else if (tokens[1].equals("+")) {
                descend = Boolean.FALSE;
            }

            // Otherwise "=" ... pick up default from the path
        }

        if (tokens.length == 3) {
            assertPathSpec(!isDelimeter(tokens[2], "<+=") && !Strings.isBlank(tokens[0]), shorthand);

            int optionsStart = tokens[2].indexOf('(');

            if (optionsStart == -1) {
                if (!Strings.isBlank(tokens[2])) {
                    from = tokens[2];
                }
            } else {
                assertPathSpec(tokens[2].charAt(tokens[2].length() - 1) == ')', shorthand);
                options = tokens[2].substring(optionsStart + 1, tokens[2].length() - 1).trim();

                if (optionsStart != 0) {
                    from = tokens[2].substring(0, optionsStart).trim();
                }
            }
        }

        // Hack until replaced with proper grammar.
        int optionsStart = toId.indexOf('(');

        if (optionsStart != -1) {
            to = toId.substring(0, optionsStart);
            from = RUNTIME;
            options = toId.substring(optionsStart + 1, toId.length() - 1).trim();
        }
    }

    private void assertPathSpec(boolean condition, String pathSpec) {
        Assert.isTrue(condition,
            "Path spec '" + pathSpec
            + "' is invalid. Valid syntax is: <toPathId>[?|!] [<|+|=] [fromPathId] [(<option1>, ...)]");
    }

    private boolean isDelimeter(String string, String delimiters) {
        return (string.length() == 1) && (delimiters.indexOf(string.charAt(0)) != -1);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public Boolean isDescend() {
        return descend;
    }

    public void setDescend(Boolean descend) {
        this.descend = descend;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    void setDependency(RepoDependency dependency) {
        this.dependency = dependency;
    }

    public RepoDependency getDependency() {
        return dependency;
    }

    public void mergeDefaults(RepoPath path) {
        // Fill in defaults if not specified
        from = (from == null) ? RUNTIME : from;
        descend = (descend == null) ? ((path.isDescendDefault()) ? Boolean.TRUE : Boolean.FALSE) : descend;
        mandatory = (mandatory == null) ? ((path.isMandatoryDefault()) ? Boolean.TRUE : Boolean.FALSE) : mandatory;
    }

    /**
     * Converts the path spec to its short hand form. It minimises the output by matching
     * elminating settings that match either the path or in-built defaults.
     */
    public String toShortHand(RepoPath path) {
        Assert.isTrue(path.getId().equals(to),
            "Attempt to create shorthand with the wrong path: to=" + to + ", path=" + path.getId());

        StringBuffer sb = new StringBuffer(to);

        if (mandatory.booleanValue() != path.isMandatoryDefault()) {
            sb.append(mandatory.booleanValue() ? "!" : "?");
        }

        if (descend.booleanValue() != path.isDescendDefault()) {
            sb.append(descend.booleanValue() ? "<" : "+");
        }

        if (!from.equals(RUNTIME)) {
            sb.append((descend.booleanValue() == path.isDescendDefault()) ? "=" : "");
            sb.append(from);
        }

        sb.append((options == null) ? "" : ("(" + options + ")"));

        return sb.toString();
    }

    public String toShortString() {
        return toShortHand();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        RepoPathSpec pathSpec = (RepoPathSpec)o;

        if ((dependency != null) ? (!dependency.getId().equals(pathSpec.dependency.getId())) : (
                    pathSpec.dependency != null
                )) {
            return false;
        }

        if ((descend != null) ? (!descend.equals(pathSpec.descend)) : (pathSpec.descend != null)) {
            return false;
        }

        if ((from != null) ? (!from.equals(pathSpec.from)) : (pathSpec.from != null)) {
            return false;
        }

        if ((mandatory != null) ? (!mandatory.equals(pathSpec.mandatory)) : (pathSpec.mandatory != null)) {
            return false;
        }

        if ((options != null) ? (!options.equals(pathSpec.options)) : (pathSpec.options != null)) {
            return false;
        }

        return !((to != null) ? (!to.equals(pathSpec.to)) : (pathSpec.to != null));
    }

    public int hashCode() {
        int result;
        result = ((from != null) ? from.hashCode() : 0);
        result = (31 * result) + ((to != null) ? to.hashCode() : 0);
        result = (31 * result) + ((options != null) ? options.hashCode() : 0);
        result = (31 * result) + ((descend != null) ? descend.hashCode() : 0);
        result = (31 * result) + ((mandatory != null) ? mandatory.hashCode() : 0);

        return result;
    }

    public String toShortHand() {
        StringBuffer sb = new StringBuffer();
        sb.append((to == null) ? "" : to);

        if (mandatory != null) {
            sb.append(mandatory.booleanValue() ? "!" : "?");
        }

        if (descend != null) {
            sb.append(descend.booleanValue() ? "<" : "+");
        }

        if (!from.equals(RUNTIME)) {
            sb.append(((descend == null) && (to != null)) ? "=" : "");
            sb.append(from);
        }

        sb.append((options == null) ? "" : ("(" + options + ")"));

        return sb.toString();
    }
}
