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
package any.servable;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import any.Linker;
import any.model.Message;
import any.model.Servable;

public class OpenServable implements Servable {

	
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}
	
	public void process(String cmd, String content, BlockingQueue<Message> outQueue) {

		int l = content.indexOf('?');
		l = (l < 1)?content.length():l;
		
		String filename = content.substring(0, l);

		System.out.println("### open file " + filename + " with native system");
		
		File file = new File(filename);

		try {
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}
