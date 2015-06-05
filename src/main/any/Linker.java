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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// import any.model.AnyCmd;
// import any.model.Response;
import any.model.Message;
import any.model.Servable;

public class Linker {

	protected final Log logger = LogFactory.getLog(this.getClass());

	public static final char SEPCHAR = (char) 127;

	private ServerSocket serverSocket;

	// all available connections
	// List<Connection> conns = new ArrayList<Connection>();

	// named cons
	Map<String, IConnection> namedCon = new HashMap<String, IConnection>();

	// all servables
	Map<String, Servable> servableMap = new HashMap<String, Servable>();

	// messages to handle
	BlockingQueue<Message> inQueue = new ArrayBlockingQueue<Message>(1280);

//	private List<Message> inHistory = new ArrayList<Message>();
	
	private Properties properties;
	
	/*
	public List<Message> getInHistory() {
		return inHistory;
	}
	*/
	
	public Map<String, IConnection> getNamedCon() {
		return namedCon;
	} 

	public Map<String, Servable> getServableMap() {
		return servableMap;
	}

	/*
	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, FileNotFoundException, IOException {

		String prop = (args.length < 1) ? "any.properties" : args[0];

		Properties properties = new Properties();
		properties.load(new FileInputStream(prop));

		boolean hasdisplay = properties.containsKey("swtvset") && properties.get("swtvset").equals("true");

		if (hasdisplay) {
			// FIXME sort out swt issue
			Display display = Display.getDefault();
			Shell shell = new Shell(display);

			properties.put("shell", shell);
			
			// Start the main thread
			final Service s = new Service(properties);
			
			VsetServable vset = new VsetServable();
			vset.init( properties );
			s.servableMap.put("vset", vset);

			new Thread() {
				public void run() {
					IConnection loop = s.namedCon.get("loop");
					s.servableMap.get("vset").process(null, loop.getOutQueue() );
				}
			}.start();
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					s.serve();
				}
			}).start();

			// FIXME swt legacy
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} else {
			final Service s = new Service(properties);
			s.serve();
		}
	}
	*/
	
	/** 
	 * initialise with properties
	 * 
	 * @param properties
	 */
	public Linker(Properties properties) {
		this.properties = properties;
		
		final LoopConnection loop = new LoopConnection(this);
//		namedCon.put("host", loop);
		namedCon.put("loop", loop); // FIXME

		// setup services from properties file
		ClassLoader cl;
		cl = Linker.class.getClassLoader();
		try {

			for (Map.Entry<Object, Object> p : properties.entrySet()) {

				String key = p.getKey().toString();

				if (key.startsWith("serve.")) {

					String name = key.substring(6);
					String classstr = p.getValue().toString();

					if (logger.isDebugEnabled())
						logger.debug("setup servable with name: " + name
								+ " and " + classstr);

					Servable s = (Servable) cl.loadClass(classstr)
							.newInstance();
					servableMap.put(name, s);
					s.init(properties, loop.getOutQueue(), this );
				}
			}

		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		ConfigServable configServable = new ConfigServable();
		servableMap.put("config", configServable);
		configServable.init(properties, null, null);
		
	}

	public void serve() {
		this.serve( Integer.parseInt(properties.getProperty( "any.port", "43802")) );
	}
	
	/*
	 * open connections to the world with a specific port
	 */
	public void serve(final int port) {
		logger.info("+SERVICE: started to serve");

		try {
			// Start the server socket (?)
			serverSocket = new ServerSocket(port);
		
			while (true) {
				final Socket socket = serverSocket.accept();
				logger.info( socket.getInetAddress().getCanonicalHostName()+":"+socket.getPort() + " is connected");
				
				IConnection conn = new Connection( Thread.currentThread().getName(), this, socket);
     			namedCon.put( socket.getInetAddress().getCanonicalHostName()+":"+socket.getPort(), conn);
     			// namedCon.put( "host", conn);
     			logger.info("+SERVICE: connected to client " + socket.getInetAddress().getCanonicalHostName()+":"+socket.getPort() );
			}
		} catch (IOException e) {
			logger.error("+SERVICE, serve IO exception", e);
		} catch (InterruptedException e) {
			logger.error("+SERVICE, main loop interupted", e);
		}
	}

	public void connect(String name, String host, int port) {
		try {
			Socket s;
			s = new Socket(host, port);
			
			Connection conn = new Connection( name, this, s);
			
			namedCon.put(name, conn);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	

	/**
	 * Local configurations.
	 * 
	 * @author mas
	 *
	 */
	class ConfigServable implements Servable {

		@Override
		public void process(Message cmd, BlockingQueue<Message> outQueue) {
			logger.debug( "config -- " + cmd.getFullContent() );
			
			String command = cmd.getContent().replaceFirst("/__", "__");
			
			if ( command.equals("__SET/conn") || command.equals("/__SET/conn") ) {
				
				String name = cmd.getQuery().get("name");
				logger.debug( "set name with " + command + " to " + name );
				if ( name != null ) {
					IConnection conn = null;
					String oldname = null;
					for ( Map.Entry<String, IConnection> connEntry: namedCon.entrySet() ) {
						if ( (connEntry.getValue()).getOutQueue() == outQueue ) {
							logger.debug( "set name for " + connEntry.getKey() + " connection to " + name );
							conn = connEntry.getValue();
							oldname = connEntry.getKey();
						}
					}
					if ( conn != null ) {
						namedCon.remove( oldname );
						namedCon.put( name, conn );
					}
				}
				
				/*
				StringBuilder sb = new StringBuilder();
				sb.append("<br><br><br>done<a href='vswitch:_main'>_main</a>");
				outQueue.add(new Message("vset", "_error.html","text/html", "YES", sb
						.toString().getBytes()));
			    */
			} else if ( command.equals("__INIT/connection") ) {

				String name = cmd.getQuery().get("name");
				String host = cmd.getQuery().get("host");
				int port = Integer.parseInt(cmd.getQuery().get("port"));
				String myname = cmd.getQuery().get("myname");
				
				// TODO, remove
				connect(name, host, port);				
				BlockingQueue<Message> q = getNamedCon().get(name).getOutQueue();
				q.add(new Message( "config:__SET/conn?name=" + myname ));
				

			} else if ( command.equals("__LIST/connections") ) {
				StringBuilder sb = new StringBuilder();
				for (String s : namedCon.keySet() ) {
					// // TODO add icon
					// TODO do as template
					TblUtil tbl = new TblUtil();
					tbl.set(TblUtil.DISPLAYTEXT, s + ":");
					tbl.set(TblUtil.TABCMD, "sethost:" + s + "");
					sb.append(tbl.makeCell());
				}
				outQueue.add(new Message("vset", "__servables.anytable", TblUtil.TYPE, "YES", sb
						.toString().getBytes()));
			} else if ( command.equals("__LIST/servable") ) {
				StringBuilder sb = new StringBuilder();
				for (String s : servableMap.keySet()) {
					// // TODO add icon
					// TODO do as template
					TblUtil tbl = new TblUtil();
					tbl.set(TblUtil.DISPLAYTEXT, s + ":");
					tbl.set(TblUtil.TABCMD, "setcmd:" + s + "");
					sb.append(tbl.makeCell());
				}
				outQueue.add(new Message("vset", "__servables.anytable", TblUtil.TYPE, "YES", sb
						.toString().getBytes()));
			} else {
				/*
				StringBuilder sb = new StringBuilder();
				sb.append("error invalid config comman");
				outQueue.add(new Message("vset", "_error.html","text/plain", "YES", sb
						.toString().getBytes()));
						*/

			}
		}

		
		@Override
		public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
			// TODO nothing to do here, as we have access to the full service anyway
			
		}

		@Override
		public void disconnect(BlockingQueue<Message> outQueue) {
			// default, ignore 
		}

	}
	
	
	/**
	 * LoopConnection handles messages either locally or sends them of to
	 * foreign hosts.
	 * 
	 * @author mas
	 *
	 */
	class LoopConnection implements IConnection {

		BlockingQueue<Message> outQueue = new ArrayBlockingQueue<Message>(1280);

		Thread loopThread;
		Linker service;

		public LoopConnection(Linker service) {
			this.service = service;

			loopThread = new LoopThread(this);
			loopThread.setName( loopThread.getName() + " (loop)");
			loopThread.start();
		}

		public void tearDown() {
			logger.info("+SERVICE: tear down this loop: ");
			loopThread.interrupt();
		}

		class LoopThread extends Thread {

			final LoopConnection con;

			public LoopThread(LoopConnection con) {
				this.con = con;
			}

			@Override
			public void run() {

				try {
					while (true) {
						final Message acmd = con.outQueue.take();

						final String fcmd = acmd.getScheme();
						final String fhost = acmd.getHost();

						logger.info("+SERVICE: start " + fcmd + " --" + fhost );
						
						if (fhost == null || !namedCon.containsKey(fhost)) {
							new Thread("run loop " + fcmd) {
								public void run() {
									logger.info("+SERVICE: start looped " + fcmd + " --" + fhost + "-- "
											+ servableMap.get(fcmd) + " "
											+ con.outQueue.getClass());
									if (servableMap.containsKey(fcmd)) {
										servableMap.get(fcmd).process(acmd,
												con.outQueue);
//										inHistory.add(acmd);
									}
								}
							}.start();
						} else {
							Message striphost = new Message(acmd.getScheme()
									+ ":" + acmd.getFullContent(),
									acmd.getData());

							namedCon.get( fhost ).getOutQueue().add(striphost);
						}
					}
				} catch (InterruptedException e) {
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


}
