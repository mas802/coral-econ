/*
 *   Copyright 2009-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package coral;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import any.IConnection;
import any.Linker;
// import coral.test.CoralLogRun;

/*
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Shell;
 */
import coral.web.ExpWebServer;

public class CoralHead {

    public static void build(String... args) {

        Properties generalProp = new Properties();

        // add commandline properties
        for (String a : args) {
            if (a.contains("=")) {
                String[] sp = a.split("=");
                generalProp.put(sp[0], sp[1]);
            } else if (a.endsWith(".properties")) {
                try {
                    generalProp.load(new FileInputStream(a));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    lastresort(e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    lastresort(e.getMessage());
                    e.printStackTrace();
                }
                /*
                 * } else if (a.endsWith(".log")) { CoralLogRun lr = new
                 * CoralLogRun();
                 * 
                 * if (a.length() == 1) { try { generalProp.load(new
                 * FileInputStream("coral.properties")); } catch
                 * (FileNotFoundException e) { // TODO Auto-generated catch
                 * block lastresort(e.getMessage()); e.printStackTrace(); }
                 * catch (IOException e) { // TODO Auto-generated catch block
                 * lastresort(e.getMessage()); e.printStackTrace(); } }
                 * 
                 * lr.init(new File(generalProp.getProperty( "exp.basepath", "."
                 * )), generalProp.getProperty( "exp.stagesfile", "stages.csv"
                 * ), a, "rerun.raw"); return;
                 */
            }

        }

        /*
         * TODO this never worked properly // FIND coral.ask parameters for
         * (Object key : generalProp.keySet()) { String value =
         * generalProp.getProperty((String) key); if (value != null &&
         * (value.startsWith("coral.ask"))) {
         * 
         * String s = (String) JOptionPane.showInputDialog( (Component) null,
         * (String) key, "enter value", JOptionPane.PLAIN_MESSAGE);
         * 
         * generalProp.put(key, s);
         * 
         * } }
         */

        boolean webserver = generalProp
                .getProperty("coral.web.server", "false").equals("true");

        if (webserver) {
            try {
                new ExpWebServer(generalProp);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                lastresort(e.getMessage());
                e.printStackTrace();
            }
        }

        boolean hasdisplay = generalProp.containsKey("swtvset")
                && generalProp.get("swtvset").equals("true");

        if (hasdisplay) {

            final org.eclipse.swt.widgets.Shell shell;
            org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display
                    .getDefault();

            //
            // the server process
            //
            if (true) { // TODO is server?
                shell = new org.eclipse.swt.widgets.Shell(display);
                shell.setLayout(new org.eclipse.swt.layout.FillLayout());
                generalProp.put("shell", shell);

                final Linker s = new Linker(generalProp);

                if (generalProp.getProperty("coral.vset", "true")
                        .equals("true")) {
                    CoralHeadServable vset = new CoralHeadServable();
                    vset.init(generalProp, null, null);
                    s.getServableMap().put("vset", vset);
                    s.getServableMap().put("info", vset.new SetInfoServable());

                    new Thread() {
                        public void run() {
                            IConnection loop = s.getNamedCon().get("loop");
                            s.getServableMap().get("vset")
                                    .process(null, loop.getOutQueue());
                        }
                    }.start();
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        s.serve();
                    }
                }).start();

                display.addFilter(org.eclipse.swt.SWT.Close,
                        new org.eclipse.swt.widgets.Listener() {

                            @Override
                            public void handleEvent(
                                    org.eclipse.swt.widgets.Event e) {
                                if (e.widget == shell) {
                                    // TODO Auto-generated method stub
                                    System.exit(0);
                                }
                            }
                        });

                shell.open();
            }

            int noclients = Integer.parseInt(generalProp.getProperty(
                    "coral.polyp.number", "0"));

            if (noclients > 0) {
                CoralPolyp.makeClient(generalProp);
            }

            // FIXME swt legacy
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } else {
            final Linker s = new Linker(generalProp);
            s.serve();
        }

    }

    public static void main(String[] args) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            FileNotFoundException, IOException {

        if (args.length == 0) {
            args = new String[] { "coral.properties" };
        }

        build(args);

    }

    /*
     * not working properly private static void loadSwtJar(String swtPath) { if
     * (swtPath == null) { String osName =
     * System.getProperty("os.name").toLowerCase(); String osArch =
     * System.getProperty("os.arch").toLowerCase();
     * 
     * String swtFileNameOsPart = osName.contains("win") ? "win32" :
     * osName.contains("mac") ? "osx" : osName.contains("linux") ||
     * osName.contains("nix") ? "linux" : "";
     * 
     * String swtFileNameArchPart = osArch.contains("64") ? "_x86_64" : "";
     * swtPath = "/Users/mas/workspace/java/Coral/lib/swt-" + swtFileNameOsPart
     * + swtFileNameArchPart + ".jar"; }
     * 
     * try { URL swtFileUrl = new URL("file:" + swtPath);
     * 
     * URLClassLoader classLoader = (URLClassLoader) CoralHead.class
     * .getClassLoader(); Method addUrlMethod =
     * URLClassLoader.class.getDeclaredMethod( "addURL", URL.class);
     * addUrlMethod.setAccessible(true);
     * 
     * addUrlMethod.invoke(classLoader, swtFileUrl);
     * System.out.println("Added the swt jar to the class path: " + swtPath); }
     * catch (Exception e) {
     * System.out.println("Unable to add the swt jar to the class path: " +
     * swtPath); lastresort(e.getMessage()); e.printStackTrace(); } }
     */

    private static void lastresort(String s) {
        System.out.println(s);
        javax.swing.JOptionPane.showMessageDialog(null, s);
    }
}
