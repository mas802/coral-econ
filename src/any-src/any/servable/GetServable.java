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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import javax.activation.MimetypesFileTypeMap;

import any.Linker;
import any.model.Message;
import any.model.Servable;


// get@r1:/mnt/hdrive/workspace/java/AnyService/test.txt

public class GetServable implements Servable {

	
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}
	
	public void process(String cmd, String content, BlockingQueue<Message> outQueue) {

		int l = content.indexOf('?');
		l = (l < 1)?content.length():l;
		
		String filename = content.substring(0, l);

		File file = new File(filename);
		String mime = "";
		
		InputStream is;
		byte[] bytes = null;
		try {
			is = new FileInputStream(file);
			
			mime = getMime(file);
			
			// Get the size of the file
			long length = file.length();

			if (length > Integer.MAX_VALUE) {
				// File is too large
			}

			// Create the byte array to hold the data
			bytes = new byte[(int) length];

			// Read in the bytes
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}

			// Ensure all the bytes have been read in
			if (offset < bytes.length) {
				is.close();
				throw new IOException("Could not completely read file "
						+ file.getName());
			}

			Long version = file.lastModified();
			
			outQueue.put(new Message("vset", file.getName(), mime, "YES&version="+version+"&srcpath=" + filename, bytes));

			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "NO", result.toString().getBytes()));			
		} catch (IOException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "NO", result.toString().getBytes()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "NO", result.toString().getBytes()));
		}
	}

	public static String getMime(File file) {
		
		String mime = "text/plain";
		try {
			mime = new MimetypesFileTypeMap().getContentType(file);
		} catch( Error ex ) {
			// TODO log error or use specific error
			System.err.println("Problem with mime type resolution.");
			ex.printStackTrace();
			
			String n = file.getName();
			String e = n.substring(n.lastIndexOf('.')+1).toLowerCase();
			
			String[][] types = new String[][] {
					new String[] { "pdf", "application/pdf" },
					new String[] { "html", "text/html" },
					new String[] { "png", "image/png" },
					new String[] { "anytable", "text/anytable" },
					new String[] { "jpg", "image/jpg" }
			};
			
			for (String[] r:types) {
				if ( r[0].equals(e)) {
					mime = r[1];
				}
			}
		}
		
		return mime;
	}
	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}
