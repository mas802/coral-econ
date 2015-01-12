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

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.Linker;
import any.model.Message;
import any.model.Servable;



public class EchoServable implements Servable {

	protected final Log logger = LogFactory.getLog(this.getClass());

	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}
	
//	public List<Object> run(BufferedReader in, PrintStream out) {
//
//		StringBuilder sb = new StringBuilder();
//
//		String line;
//
//		try {
//			while ((line = in.readLine()) != null) {
//				System.out.println(line);
//				if (line.startsWith("END")) break;
//				sb.append(line + "\n");
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		System.out.println("ECHO REQUEST: " + sb.toString());
//		
//		Response r = new Response(sb.toString(), sb.toString(), "", null);
//		
//		r.appendTo(out);
//		
//		out.flush();
//		
//		out.close();
//		
//		System.out.println("SENT ECHO");
//		return null;
//	}
	
	public void process(String cmd, String content, BlockingQueue<Message> outQueue) {
		logger.info("ECHO Servable called with:" + content);
		outQueue.add(new Message("vset","_echo","text/plain", "NO",content.getBytes()));
	}
	
	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}

