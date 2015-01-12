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
package any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.model.Message;
import any.model.Servable;

/**
 * 
 * Main class for information transfer.
 * 
 * @author Markus Schaffner
 *
 */
class Connection implements IConnection {

	protected final Log logger = LogFactory.getLog(this.getClass());

	BlockingQueue<Message> outQueue = new ArrayBlockingQueue<Message>(1280);

	Linker service;
	final OutputStream out;
	final InputStream in;

	final Thread outThread;
	final Thread inThread;

	public Connection(String name, Linker service, Socket socket)
			throws InterruptedException, IOException {
		this.service = service;

		out = socket.getOutputStream();
		in = socket.getInputStream();

		// br.mark(256);
		Thread.sleep(100);

				
		outThread = new OutThread(out, this);
		inThread = new InThread(in, this);

		outThread.setName(outThread.getName()+ " " +name+ " (out con)");
		inThread.setName(inThread.getName()+ " " +name+ "  (in con)");
		
		inThread.start();
		outThread.start();
		// setup and negotiation, send available protocols first

		logger.info("+SERVICE: new connection established with name " + name);
	}

	public void tearDown() {
		logger.info("+SERVICE: tear down this connection: ");
		
        for ( String name : service.namedCon.keySet() ) {
        	if ( this == service.namedCon.get(name) ) {
        		service.namedCon.remove(name);
        	}
        }
		
		for( Servable serv: service.getServableMap().values() ) {
			serv.disconnect(outQueue);
		}
		
		outThread.interrupt();
		inThread.interrupt();
	}

	class InThread extends Thread {

		InputStream in;
		Connection con;

		public InThread(InputStream in, Connection con) {
			this.in = in;
			this.con = con;
		}

		@Override
		public void run() {

			try {

				while (true) {
					String cmd = "";
					long length = 0;

					int c = -1;
					int separatorCount = 0;
					int lencount = 0;
					while (separatorCount < 2 && (c = in.read()) != -1) {
						// System.out.print((char)c);
						// System.out.print("." + length);
						if ((c == (int) Linker.SEPCHAR) && separatorCount == 0) {
							separatorCount++;
						} else if (separatorCount == 0) {
							cmd += (char) c;
						} else if (lencount >= 8) {
							separatorCount++;
						} else if (separatorCount == 1) {
							// System.out.print(c+"-"+lencount+":");
							length <<= 8;
							length ^= (long) c & 0xFF;
							lencount++;
						}
					}

					logger.info("+SERVICE: in cmd: " + cmd + " length: " + length);

					if (c == -1) {
						logger.info("+SERVICE: Connection ended (?)");
						con.tearDown();
						break;
						// FIXME: IMPORTANT take down outthread
						// !!!!!!!!!!!!!!!!!!!!!!!
					}

					// create request object from in
					byte[] buffer = new byte[8 * 1024 * 1024];
					int read;
					int lenleft = (int) length;

					ByteBuffer content = ByteBuffer.allocate(lenleft);
					while (lenleft > 0
							&& (read = in.read(buffer, 0, lenleft)) > 0) {
						content.put(Arrays.copyOf(buffer, read));
						lenleft -= read;
						logger.debug("+SERVICE: read: " + read + " lengh: " + length
								+ " lenleft: " + lenleft);
					}

					// TODO fallback
					if (!cmd.contains(":")) {
						cmd = cmd + ":" + content;
					}

					logger.debug("+SERVICE: read cmd  " + cmd + " length:"
							+ length + " " + lenleft);
					final Message acmd = new Message(cmd, content.array());
					final String fcmd = acmd.getScheme().trim();

					logger.info("+SERVICE: ready for cmd: " + fcmd
							+ " length: " + length + " content: ***"
							+ acmd.getFullContent() + "***");

					// TODO take care here
					if (fcmd != null && fcmd != ""
							&& service.servableMap.containsKey(fcmd)) {
						new Thread() {
							public void run() {
								logger.info("+SERVICE: start " + fcmd);
								service.servableMap.get(fcmd).process(acmd,
										con.outQueue);
//								service.getInHistory().add(acmd);
							}
						}.start();
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class OutThread extends Thread {

		OutputStream out;
		Connection con;

		public OutThread(OutputStream out, Connection con) {
			this.out = out;
			this.con = con;
		}

		@Override
		public void run() {

			try {
				while (true) {
					logger.info("+SERVICE:  =========== waiting to send next ==========="
							+ con);
					Message r = con.outQueue.take();
					logger.info("+SERVICE:  =========== start send next ===========");

					byte[] x = r.toSent();
					logger.debug(" send: " + r.getFullContent());

					// Thread.sleep(10);

					out.write(x);
					out.flush();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			con.tearDown();
		}
	}

	@Override
	public BlockingQueue<Message> getOutQueue() {
		return outQueue;
	}
}