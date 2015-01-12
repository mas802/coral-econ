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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import any.Linker;
import any.model.Message;
import any.model.Servable;

// import org.eclipse.swt.internal.cocoa.NSWindow;

public class VsetServable implements Servable {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private Map<String,String> infos = new HashMap<String, String>();
	
	protected File res;
	protected Shell shell;
	
	private BlockingQueue<Message> loopqueue;

	protected Browser browser;
	private LocationListener locationListener;
	protected LocationListener getLocationListener() { return locationListener; }
	
	protected String mainfilename = null;
	protected String robotfilename = null;
	private File currentfile = null;

	private boolean blockpage = false;

	@Override
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {

		this.loopqueue = loopQueue;
		
		this.shell = (Shell) properties.get("shell");
		
		this.res = new File(properties.getProperty("any.res", "res/"));

		this.mainfilename = properties.getProperty("any.main", "main.html");

		setup();
	}
	
	public void setup() {

		if (!res.exists()) {
			res.mkdirs();
		}
		

		try {
			browser = new Browser(shell, SWT.PUSH);

		} catch (SWTError e) {
			logger.warn("Could not instantiate Browser: ", e);
			throw new RuntimeException("Could not instantiate Browser", e);
		}
		
		File mainfile = new File( mainfilename );
		
		String maintext = "<html>Please define a main.html file.</html>";
		if ( mainfile.exists() ) {
			try {
				maintext = new Scanner( mainfile ).useDelimiter("\\Z").next();
			} catch (FileNotFoundException e) {
				logger.warn("Could not read main file " + mainfilename, e );
			}
		}

		browser.setText(maintext);
		

		locationListener = new LocationAdapter() {
			public void changing(LocationEvent event) {
				logger.warn("location: " + event + " q: " + loopqueue + "###"
						+ event.location + "###" + event.data);
				String location = event.location;
				// TODO

				// IE work around
				if (event.location
						.startsWith("res://ieframe.dll/unknownprotocol.htm")) {
					event.doit = false;
					return;
				}

				// todo the whole blockpage thing is strange/exp dependent
				if ( (!location.startsWith("exp") || !blockpage) && loopqueue != null ) {
					// queue.add(new Message(location.replace("//host", ""), ""
					// .getBytes()));
					loopqueue.add(new Message(location, "".getBytes()));
					if ( location.startsWith("exp") ) {
						blockpage = true;
					} else {
						blockpage = false;
					}
				}
				
				if (!location.startsWith("http") && !location.startsWith("file") ) {
					event.doit = false;
				}
				
			}

		};
		
		browser.addLocationListener(locationListener);

		browser.addProgressListener(new ProgressListener() {

			@Override
			public void completed(ProgressEvent arg0) {
				logger.debug("progress complete: " + arg0);

				String jsrobot = null;

				if (robotfilename != null) {

					try {
						jsrobot = new Scanner(new File(robotfilename))
								.useDelimiter("\\A").next();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				blockpage = false;

				if (jsrobot != null) {
					browser.execute(jsrobot);
				}
				
				updateInfos(infos);
			}

			@Override
			public void changed(ProgressEvent arg0) {
				// TODO Auto-generated method stub
				logger.debug("progress changed: " + arg0);
			}
		});

		browser.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				event.doit = false;
			}
		});

		logger.debug("ready");

	}

	private void updateInfos(Map<String,String> infos) {

	    final StringBuilder rtxt = new StringBuilder("try{ infoCallback({");
	    
	    int i = 0;
	    for (String key : infos.keySet() ) {
	        String escvalue = infos.get(key).replaceAll("\"","\\\"");
	        
	        if ( i>0 ) rtxt.append(", ");
	        i++;
	        rtxt.append( key +": \""+escvalue+"\" ");
	    }
	    
	    rtxt.append("} );} catch (err) { // alert(err); }");
	    
	    Display.getDefault().asyncExec(new Runnable() {
	    	 public void run() {	
	    		 browser.execute( rtxt.toString() );
	    	 }
	    });
	    
	    logger.debug("JS CALLBACK: "+rtxt+" --\n-- " + infos);
	    
	    blockpage = false;
	}
	

	
	
	@Override
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		if (cmd == null) {
			loopqueue = outQueue;
		} else {

			String filename = cmd.getContent();

			currentfile = new File(res, filename);

			logger.debug(currentfile.getAbsolutePath());
			FileOutputStream fw;
			try {
				fw = new FileOutputStream(currentfile);

				fw.write(cmd.getData());
				fw.flush();
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
				final Writer result = new StringWriter();
				final PrintWriter printWriter = new PrintWriter(result);
				e.printStackTrace(printWriter);
				outQueue.add(new Message("vset", "_error", "text/plain", "YES",
						result.toString().getBytes()));
			}

			if (cmd.getFullContent().contains("show=YES")) {
				Display display = Display.getDefault();
				display.syncExec(new Runnable() {
					public void run() {
						URL pageUrl = null;
						try {
							pageUrl = currentfile.toURI().toURL();
							browser.setUrl(pageUrl.toString());
							blockpage = false;
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		}
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

	public class SetInfoServable implements Servable {

		@Override
		public void process(Message cmd, BlockingQueue<Message> outQueue) {
			
			logger.debug( " info with " + cmd.getFullContent() + " -- " + cmd.getQuery().size() );
			infos.putAll( cmd.getQuery() );
			
			updateInfos(infos);
		}

		@Override
		public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void disconnect(BlockingQueue<Message> outQueue) {
			// TODO Auto-generated method stub
			
		}
		
	}
}