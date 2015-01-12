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

/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// Platform.resolve(plugin.getBundle().getEntry(".")).toString(); 

/*
 * Browser example snippet: Render HTML from memory in response to a link click.
 *
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */

import java.io.ByteArrayOutputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import any.Linker;
import any.model.Message;
import any.model.Servable;

// import org.eclipse.swt.internal.cocoa.NSWindow;

// TODO incomplete, untested

public class ScreenshotServable implements Servable {

	protected final Log logger = LogFactory.getLog(this.getClass());

//	private BlockingQueue<Message> queue;

	private Shell shell;

	@Override
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {

		this.shell = (Shell) properties.get("shell");

		logger.debug("ready");

	}

	@Override
	public void process(Message cmd, final BlockingQueue<Message> outQueue) {

		final String dest = cmd.getFullContent();
		
		Display display = Display.getDefault();
		display.syncExec(new Runnable() {
			public void run() {
		        final Image image = new Image(shell.getDisplay(), shell.getSize().x, shell.getSize().y);
		        GC gc = new GC(image);
		        shell.getChildren()[0].print(gc);
		        gc.dispose();

		        ImageLoader imageLoader = new ImageLoader();
		        imageLoader.data = new ImageData[] {image.getImageData()};

		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        imageLoader.save(baos, SWT.IMAGE_PNG);
		        
		        outQueue.add(new Message("put://host/" + dest, 
		        		baos.toByteArray()) );

			}
		});


	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}