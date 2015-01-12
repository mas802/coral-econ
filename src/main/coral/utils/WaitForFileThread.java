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
package coral.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import coral.service.ExpHandler;

public class WaitForFileThread extends Thread {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private Set<Integer> clients = new HashSet<Integer>();
	private ExpHandler handler = null;
	
	private File file;
	
	public WaitForFileThread(ExpHandler handler, File file) {
		this.handler = handler;
		this.file = file;
	}
	
	@Override
	public void run() {
        
		logger.debug("start waiting for file " + file);
		try {
			while ( !file.exists() ) {
				Thread.sleep(20);
			}
			
			logger.debug("done waiting for file " + file);
			for (Integer i:clients) {
				handler.getService().process(i, "/?wait=_waited&_waitForFile=done");
			}
			
		} catch (InterruptedException e) {
			logger.error("error waiting for file " + file, e);
			
			for (Integer i:clients) {
				handler.getService().process(i, "/?wait=_waited&_waitForFile=failed");
			}
		}
	}

	public boolean add(Integer id) {
		return clients.add(id);
	}
	
}
