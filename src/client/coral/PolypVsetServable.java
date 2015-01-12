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
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;

import any.Linker;
import any.model.Message;
import any.servable.VsetServable;

// import org.eclipse.swt.internal.cocoa.NSWindow;

public class PolypVsetServable extends VsetServable {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {

		shell = (Shell) properties.get("shell");

		res = new File(properties.getProperty("coral.polyp.res", "res/"));

		robotfilename = properties.getProperty("coral.polyp.robot");
		mainfilename = properties.getProperty("coral.polyp.main", "main.html");

		setup();

		int noclients = Integer.parseInt(properties.getProperty(
				"coral.polyp.number", "1"));

		int perrow = (int) Math.round(Math.ceil(Math.sqrt(noclients)));

		if (noclients > 1) {
			GridData gd = new GridData();
			gd.grabExcessHorizontalSpace = true;
			gd.grabExcessVerticalSpace = true;
			gd.widthHint = shell.getSize().x / perrow;
			gd.heightHint = shell.getSize().y / perrow;
			gd.horizontalAlignment = GridData.FILL;
			gd.verticalAlignment = GridData.FILL;

			browser.setLayoutData(gd);
			browser.setSize(shell.getSize().x / perrow, shell.getSize().y
					/ perrow);

			logger.debug("set client to size with x:" + shell.getSize().x
					+ " y:" + shell.getSize().y + " perrow:" + perrow + "");
			logger.debug("layout " + browser.getLayout());
			logger.debug("layout " + shell.getLayout());

		}
	}
}