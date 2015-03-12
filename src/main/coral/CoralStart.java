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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class CoralStart {

	public static void main(String[] args) {

		loadSwtJar(null);
		
		if (args.length == 0) {
			args = new String[] { "coral.properties" };
		}

		try {
		    CoralHead.build(args);
		} catch (Throwable e) {
			lastresort("Problem starting Coral:" , e);
			e.printStackTrace();
		}
	}

	private static void loadSwtJar(String swtPath) {
		if (swtPath == null) {
			String osName = System.getProperty("os.name").toLowerCase();
			String osArch = System.getProperty("os.arch").toLowerCase();

			String swtFileNameOsPart = osName.contains("win") ? "win32"
					: osName.contains("mac") ? "osx" : osName.contains("linux")
							|| osName.contains("nix") ? "linux" : ""; 
			
			String swtFileNameArchPart = osArch.contains("64") ? "_x86_64" : "";
			swtPath = "lib/swt-" + swtFileNameOsPart + swtFileNameArchPart
					+ ".jar";
		}

		try {
			URL swtFileUrl = new URL("file:" + swtPath);

			URLClassLoader classLoader = (URLClassLoader) CoralHead.class
					.getClassLoader();
			Method addUrlMethod = URLClassLoader.class.getDeclaredMethod(
					"addURL", URL.class);
			addUrlMethod.setAccessible(true);

			addUrlMethod.invoke(classLoader, swtFileUrl);
			System.out.println("Added the swt jar to the class path: "
					+ swtPath);
		} catch (Exception e) {
			System.out.println("Unable to add the swt jar to the class path: "
					+ swtPath);
			lastresort("Unable to add the swt jar to the class path: "
				+ swtPath + " " , e);
			e.printStackTrace();
		}
	}
	
	private static void lastresort(String msg, Throwable e){
	    System.out.println( msg );
	    String s = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
	    System.out.println( s );
	    javax.swing.JOptionPane.showMessageDialog(null, msg + "\n" + s);
	}
}
