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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import any.Linker;
import any.model.Message;
import any.model.Servable;


public class PutServable implements Servable {

	public void process(Message cmd, BlockingQueue<Message> outQueue) {

		String content = cmd.getFullContent();
		int l = content.indexOf('?');
		l = (l < 1)?content.length():l;
		
		String filename = content.substring(0, l);

		System.out.println( filename + " - ###" + filename.substring(1,2) + "###" );
		if ( filename.substring(1, 2).equals(".") ) {
			filename = filename.substring(1);
		}
		System.out.println( filename);
		
		/*
		File save = new File(filename);
		File savefile = new File(filename + "_saved" + System.currentTimeMillis());
		save.renameTo(savefile);
        */
		
		File f = new File(filename);
		
		System.out.println(f.getAbsolutePath());
		FileOutputStream fw;
		try {
			fw = new FileOutputStream(f);

			fw.write(cmd.getData());
			fw.flush();
			fw.close();

// TODO report back			outQueue.put(new Response("setcmd:success",new byte[] {}));
			// MAKE RESPONSE
		} catch (IOException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "YES", result.toString().getBytes()));
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		    final Writer result = new StringWriter();
//		    final PrintWriter printWriter = new PrintWriter(result);
//		    e.printStackTrace(printWriter);
//			outQueue.add(new Response("vset","_error","text/plain", "YES", result.toString().getBytes()));
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
