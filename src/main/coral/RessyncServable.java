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

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.Linker;
import any.model.Message;
import any.model.Servable;
import coral.utils.CoralUtils;

public class RessyncServable implements Servable {

	protected final Log logger = LogFactory.getLog(this.getClass());

//	private BlockingQueue<Message> queue;
	private File res;

	@Override
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {

		res = new File(properties.getProperty("coral.polyp.res", "res/"));
		if (!res.exists()) {
			res.mkdirs();
		}

		logger.debug("ready");
		
	}

	/**
	 * check if the file needs to be updated, if yes, send out a get request
	 */
	@Override
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		if (cmd == null) {
//			queue = outQueue;
		} else {


			String filename = cmd.getContent();

			if (logger.isDebugEnabled()) { 
				logger.debug( "check  " + filename + " - " + cmd.getFullContent());
			}

			Map<String, Object> q = CoralUtils.urlToMap(cmd.getFullContent());

			if (logger.isDebugEnabled()) { 
				logger.debug( "check map " + q.toString() );
			}
			
			final File file = new File(res, filename);

			logger.debug( "check file " + file.getAbsolutePath());

			boolean request = false;
			if ( file.exists() ) {
				long version = file.lastModified();
				
				long expectedVersion = Long.parseLong(q.get("version").toString());
				
				if ( expectedVersion > version ) {
					request = true;
				}
			} else {
				request = true;
			}
		
			if (request) {
				outQueue.add(new Message(q.get("synccmd").toString(), new byte[] {}));
			}
		}
	}
	
	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}


}