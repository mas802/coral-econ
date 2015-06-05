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

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import any.Linker;
import any.TblUtil;
import any.model.Message;
import any.model.Servable;



public class CopyCutPasteServable implements Servable {

	static int mod_key = KeyEvent.VK_META;
	
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}
	
	public void process(String cmd, String content, BlockingQueue<Message> outQueue) {
		
		Robot robot;
		try {
			robot = new Robot();

	        robot.keyPress(mod_key);              // Copy
	        robot.keyPress(KeyEvent.VK_C);
	        robot.keyRelease(KeyEvent.VK_C);
	        robot.keyRelease(mod_key);
        
			Thread.sleep(50);
			
			String clip = getClipboardContents();
			
			TblUtil tbl = new TblUtil();
			tbl.set(TblUtil.DISPLAYTEXT, clip.trim());
			tbl.set(TblUtil.TABCMD, "paste://host/"+clip);
			tbl.set(TblUtil.DETAILCMD, "tmpl:paste//host/:"+clip);
			
//			String tblclip = clip + Service.TBLSEPCHAR + "paste@r:" + clip + Service.TBLSEPCHAR + "tmpl:paste@r:" + clip;
			
			// TODO, prepend
			if (!clip.equals("")) {
				outQueue.add( new Message("vprepend","_clipboard",TblUtil.TYPE, "NO", tbl.makeCell().getBytes()));
			}
			
			if ( cmd.equals("paste") || cmd.equals("cut") ) {
			    StringSelection stringSelection = new StringSelection( content );
			    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents( stringSelection, new ClipboardOwner() {
					
					@Override
					public void lostOwnership(Clipboard clipboard, Transferable contents) {
						// TODO Auto-generated method stub
						
					}
				});
			    
		        robot.keyPress(mod_key);              // Paste
		        robot.keyPress(KeyEvent.VK_V);
		        robot.keyRelease(KeyEvent.VK_V);
		        robot.keyRelease(mod_key);		    
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "NO", result.toString().getBytes()));
		} catch (AWTException e) {
			e.printStackTrace();
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
			outQueue.add(new Message("vset","_error","text/plain", "NO", result.toString().getBytes()));
		}	
		
		
	}
	
	  public String getClipboardContents() {
		    String result = "";
		    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		    //odd: the Object param of getContents is not currently used
		    Transferable contents = clipboard.getContents(null);
		    boolean hasTransferableText =
		      (contents != null) &&
		      contents.isDataFlavorSupported(DataFlavor.stringFlavor)
		    ;
		    if ( hasTransferableText ) {
		      try {
		        result = (String)contents.getTransferData(DataFlavor.stringFlavor);
		      }
		      catch (UnsupportedFlavorException ex){
		        //highly unlikely since we are using a standard DataFlavor
		    	  // TODO should throw and handle in main process
		        System.out.println(ex);
		        ex.printStackTrace();
		      }
		      catch (IOException ex) {
		        System.out.println(ex);
		        ex.printStackTrace();
		      }
		    }
		    
		    System.out.println("clipboard "+result);
		    
		    return result;
		}

	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}
}

