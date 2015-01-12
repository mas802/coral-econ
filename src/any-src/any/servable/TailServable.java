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

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import any.Linker;
import any.model.Message;
import any.model.Servable;



public class TailServable implements Servable {

	
	public boolean _running = true;
	public int _updateInterval = 200;

	
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}
	
	public void process(String cmd, String content, BlockingQueue<Message> outQueue) {
		System.out.println("start tailing: ---" + content + "---");
		
		File _file = new File(content);
		long _filePointer = 0;
	    try {
	        while (_running) {
	            Thread.sleep(_updateInterval);
	            long len = _file.length();
	            if (len < _filePointer) {
	                // Log must have been jibbled or deleted.
	            	outQueue.add(new Message("vset:/_error","Log file was reset. Restarting logging from start of file.".getBytes()));
	                _filePointer = len;
	            }
	            else if (len > _filePointer) {
	                // File must have had something added to it!
	                RandomAccessFile raf = new RandomAccessFile(_file, "r");
	                raf.seek(_filePointer);
	                String line = null;
	                StringBuilder tosend = new StringBuilder();
	                while ((line = raf.readLine()) != null) {
	                	tosend.append(line);
	                	tosend.append("\n");
	                }
	                tosend.deleteCharAt(tosend.length()-1);
	            	outQueue.add(new Message("vappend:/_std", tosend.toString().getBytes()));
	                _filePointer = raf.getFilePointer();
	                raf.close();
	            }
	        }
	    }
	    catch (Exception e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "YES", result.toString().getBytes()));
		}
	    // dispose();
	}
	
	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}

