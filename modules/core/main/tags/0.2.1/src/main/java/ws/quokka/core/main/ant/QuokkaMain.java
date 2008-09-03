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
import org.apache.tools.ant.Main;

import ws.quokka.core.bootstrap_util.Assert;
import ws.quokka.core.bootstrap_util.Reflect;
import ws.quokka.core.bootstrap_util.VoidExceptionHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


/**
 *
 */
public class QuokkaMain extends Main {
    //~ Static fields/initializers -------------------------------------------------------------------------------------

    private static final List SPECIAL_TARGETS = Arrays.asList(new String[] { "archetype" });

    //~ Instance fields ------------------------------------------------------------------------------------------------

    private File tempFile;

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void startAnt(String[] argsArray, Properties additionalUserProperties, ClassLoader coreLoader) {
        tempFile = null;

        // Set the default file to "build-quokka.xml" if it hasn't been supplied
        boolean buildFileSpecified = false;

        for (int i = 0; (i < argsArray.length) && !buildFileSpecified; i++) {
            String arg = argsArray[i];
            buildFileSpecified = arg.equals("-f") || arg.equals("-file") || arg.equals("-buildfile");
        }

        // See if the archetype target is active
        ArrayList args = new ArrayList(Arrays.asList(argsArray));
        String specialTarget = getSpecial(args);
        boolean special = specialTarget != null;
        Assert.isTrue(!special || !buildFileSpecified, "Do not specify a build file when using the archetype or help");

        if (special || !buildFileSpecified) {
            String buildFile = special ? tempFile.getAbsolutePath() : "build-quokka.xml";
            args.add("-f");
            args.add(buildFile);
        }

        if (additionalUserProperties == null) {
            additionalUserProperties = new Properties();
        }

        if (specialTarget != null) {
            additionalUserProperties.put("quokka.project.specialTarget", specialTarget);
        }

        // Store the arguments in case bootstrapping is needed
        int count = 0;

        for (Iterator i = args.iterator(); i.hasNext();) {
            String arg = (String)i.next();
            additionalUserProperties.put("quokka.bootstrap.args[" + count++ + "]", arg);

            // Intercept version processing to print out the quokka version instead of the ant version
            if (arg.equals("-version")) {
                System.out.println("Quokka version " + getVersion());

                return; // Bypass further processing
            }
        }

        if (tempFile != null) {
            tempFile.deleteOnExit();
        }

        super.startAnt((String[])args.toArray(new String[args.size()]), additionalUserProperties, coreLoader);
    }

    private String getSpecial(ArrayList args) {
        args = (ArrayList)args.clone();

        for (Iterator i = args.iterator(); i.hasNext();) {
            String arg = (String)i.next();

            if (SPECIAL_TARGETS.contains(arg)) { // Optimisation to only proceed if an special argument might exist

                try {
                    tempFile = File.createTempFile("temp-", ".xml", new File(System.getProperty("user.dir")));
                } catch (IOException e) {
                    throw new BuildException(e);
                }

                args.add("-f");
                args.add(tempFile.getAbsolutePath());
                args.add("-quiet");

                Main main = new Main();
                Reflect reflect = new Reflect();
                reflect.invoke(main, "processArgs", new Object[] { args.toArray(new String[args.size()]) });

                Vector targets = (Vector)reflect.get(reflect.getField(Main.class, "targets"), main);

                if ((targets.size() == 1) && SPECIAL_TARGETS.contains(targets.get(0))) {
                    return (String)targets.get(0);
                }
            }
        }

        return null;
    }

    private String getVersion() {
        String location = "META-INF/quokka/quokka.bundle_core_jar_artifacts.properties";
        final InputStream in = getClass().getClassLoader().getResourceAsStream(location);
        Assert.isTrue(in != null, "Cannot locate bundle properties at: " + location);

        final Properties properites = new Properties();
        new VoidExceptionHandler() {
                public void run() throws IOException {
                    properites.load(in);
                }
            };

        return properites.getProperty("artifact.id.version");
    }
}
