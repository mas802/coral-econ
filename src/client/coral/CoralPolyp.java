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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import any.IConnection;
import any.Linker;

public class CoralPolyp {

    public static void main(String[] args) throws InstantiationException,
	    IllegalAccessException, ClassNotFoundException,
	    FileNotFoundException, IOException {

	Properties generalProp = new Properties();

	// args = new String[] { "exp.debug=true",
	// "coral.polyp.number=coral.ask", "coral.polyp.ontop=false",
	// "coral.polyp.robot=robot.js" };

	// add commandline properties
	for (String a : args) {
	    if (a.contains("=")) {
		String[] sp = a.split("=");
		generalProp.put(sp[0], sp[1]);
	    } else if (a.endsWith(".properties")) {
		generalProp.load(new FileInputStream(a));
	    }
	}

	// generalProp.setProperty("coral.host", "131.181.43.53");
	generalProp.setProperty("any.port", "64321");

	makeClient(generalProp);
    }

    public static void invokeClient(final Properties generalProp) {

	Display.getDefault().asyncExec(new Runnable() {
	    public void run() {
		makeClient(generalProp);
	    }
	});

    }

    public static void makeClient(Properties generalProp) {

	// SETUP shell and add to properties
	Display display = Display.getDefault();
	int shellprop = SWT.DIALOG_TRIM | SWT.TITLE | SWT.RESIZE;
	if (generalProp.getProperty("coral.polyp.ontop", "true").equals("true")) {
	    shellprop = SWT.NO_TRIM | SWT.ON_TOP;
	} else if (generalProp.getProperty("coral.polyp.fullscreen", "true").equals("true")) {
		System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
	    shellprop = SWT.NO_TRIM;
	}
	Shell shell = new Shell(display, shellprop);

	int noclients = Integer.parseInt(generalProp.getProperty(
		"coral.polyp.number", "1"));

	if (noclients > 1) {
	    GridLayout gridLayout = new GridLayout();
	    gridLayout.numColumns = (int) Math.round(Math.ceil(Math
		    .sqrt(noclients)));
	    gridLayout.makeColumnsEqualWidth = true;
	    shell.setLayout(gridLayout);
	} else {
	    shell.setLayout(new FillLayout());
	}

	if (generalProp.getProperty("coral.polyp.fullscreen", "true").equals(
		"true")) {
	    shell.setBounds(Display.getDefault().getPrimaryMonitor().getClientArea());
	} else {
	    int top = Integer.parseInt(generalProp.getProperty(
		    "coral.polyp.top", "0"));
	    int left = Integer.parseInt(generalProp.getProperty(
		    "coral.polyp.left", "0"));
	    int width = Integer.parseInt(generalProp.getProperty(
		    "coral.polyp.width", "1024"));
	    int height = Integer.parseInt(generalProp.getProperty(
		    "coral.polyp.height", "768"));
	    shell.setBounds(left, top, width, height);
	}

	generalProp.put("shell", shell);

	if (!generalProp.containsKey("coral.polyp.res")) {
	    try {
		File f = File.createTempFile("coral", "res");
		f.delete();
		generalProp.put("coral.polyp.res", f.getAbsolutePath());
	    } catch (IOException e1) {
		throw new RuntimeException(
			"problem creating temporary directory for polyp, please specify with coral.polyp.res",
			e1);
	    }
	}

	for (int i = 0; i < noclients; i++) {

	    Properties prop = new Properties();

	    prop.putAll(generalProp);

	    if (i > 0) {
		prop.put("coral.polyp.res",
			prop.getProperty("coral.polyp.res", "res/") + i + "/");
	    }

	    final Linker s = new Linker(prop);

	    PolypVsetServable vset = new PolypVsetServable();
	    vset.init(prop, s.getNamedCon().get("loop").getOutQueue(), null);
	    s.getServableMap().put("vset", vset);
	    s.getServableMap().put("info", vset.new SetInfoServable());

	    ScreenshotServable sss = new ScreenshotServable();
	    sss.init(prop, s.getNamedCon().get("loop").getOutQueue(), null);
	    s.getServableMap().put("scrn", sss);
	    
	    
	    s.connect("host", prop.getProperty("coral.host", "localhost"),
		    Integer.parseInt(prop.getProperty("coral.port", "43802")));

	    
	    new Thread(new Runnable() {
		@Override
		public void run() {
		    s.serve();
		}
	    }).start();
        
	    
	    new Thread("vset init") {
		public void run() {
		    IConnection host = s.getNamedCon().get("loop");
		    s.getServableMap().get("vset")
			    .process(null, host.getOutQueue());
		}
	    }.start();

	}

	if (generalProp.getProperty("coral.polyp.headless", "false").equals(
		"false")) {
	    shell.open();
	}

	// FIXME swt legacy
	while (!shell.isDisposed()) {
	    if (!display.readAndDispatch()) {
		display.sleep();
	    }
	}
    }

}
