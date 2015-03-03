package coral.web;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.Linker;
import any.model.Message;
import any.model.Servable;
import any.servable.GetServable;
import coral.utils.CoralUtils;

public class WebServable implements Servable {

	DataOutputStream out = null;
	Linker service = null;
	String id = null;

	String hoststr;

	public WebServable(String id, Linker s, String hoststr) {
		this.hoststr = hoststr;
		this.service = s;
		this.id = id;
	}

	protected final Log logger = LogFactory.getLog(this.getClass());

	// private Map<String, String> infos = new HashMap<String, String>();

	protected File res;

	private BlockingQueue<Message> loopqueue;

	protected String mainfilename = null;
	protected String robotfilename = null;
	private File currentfile = null;

	private boolean blockpage = false;

	@Override
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker) {

		this.loopqueue = loopQueue;

		this.res = new File(properties.getProperty("any.res", "web/"));

		this.mainfilename = properties.getProperty("any.main", "main.html");

		setup();
	}

	public void setup() {

		if (!res.exists()) {
			res.mkdirs();
		}

		File mainfile = new File(mainfilename);

		String maintext = "<html><a href='" + CoralUtils.getHostStr()
				+ "__BEGIN'>BEGIN</a><br><a href='" + CoralUtils.getHostStr()
				+ "__RES'>RES</a></html>";
		if (mainfile.exists()) {
			try {
				maintext = new Scanner(mainfile).useDelimiter("\\Z").next();
			} catch (FileNotFoundException e) {
				logger.warn("Could not read main file " + mainfilename, e);
			}
		}

		logger.debug("ready");

	}

	/*
	 * private void updateInfos(Map<String,String> infos) {
	 * 
	 * final StringBuilder rtxt = new StringBuilder("try{ infoCallback({");
	 * 
	 * int i = 0; for (String key : infos.keySet() ) { String escvalue =
	 * infos.get(key).replaceAll("\"","\\\"");
	 * 
	 * if ( i>0 ) rtxt.append(", "); i++; rtxt.append( key
	 * +": \""+escvalue+"\" "); }
	 * 
	 * rtxt.append("} );} catch (err) { // alert(err); }");
	 * 
	 * Display.getDefault().asyncExec(new Runnable() { public void run() {
	 * browser.execute( rtxt.toString() ); } });
	 * 
	 * logger.debug("JS CALLBACK: "+rtxt+" --\n-- " + infos);
	 * 
	 * blockpage = false; }
	 */

	public void in(String msg, DataOutputStream out) throws IOException {

		this.out = out;

		out.writeBytes("HTTP/1.1 200 OK\n");
		out.writeBytes("Cache-Control: no-cache, no-store, must-revalidate, max-stale=0, post-check=0, pre-check=0\n");
		out.writeBytes("Expires: -1\n");
		out.writeBytes("Pragma: no-cache\n");
		out.writeBytes("Vary: *\n");
		out.writeBytes("Content-Type: text/html\n");
		out.writeBytes("Set-Cookie: CORALID=" + id + "; path=/\n; HttpOnly");
		// out.writeBytes("Transfer-Encoding: chunked\n");

		service.getNamedCon().get("host").getOutQueue()
				.add(new Message("exp://host" + msg));

	}

	public void file(String msg, DataOutputStream out) throws IOException {

		// FIXME check for a better way to do this
		msg = msg.replaceAll("\\.\\.", "__");
		File file = new File(res, msg);
		
		
        if ( file.exists() ) {
		out.writeBytes("HTTP/1.1 200 OK\n");
		out.writeBytes("Cache-Control: no-cache, must-revalidate\n");
		out.writeBytes("Expires: Mon, 26 Jul 1997 05:00:00 GMT\n");
		// out.writeBytes("Transfer-Encoding: chunked\n");
		out.writeBytes("Content-Type: " + GetServable.getMime(file)
				+ "\n\n");

		InputStream is;
		byte[] bytes = null;
		// try {
		is = new FileInputStream(file);

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

		out.write(bytes);

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			is.close();
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		is.close();
        } else {
			out.writeBytes("HTTP/1.1 404 Bad Request\n");
			// out.writeBytes("Transfer-Encoding: chunked\n");
			out.writeBytes("Content-Type: text/html\n\n");
            out.writeBytes("<html>File not found");
        }
        
		out.close();

	}

	@Override
	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		if (cmd == null) {
			loopqueue = outQueue;
		} else {

			String filename = cmd.getContent();

			logger.debug("send file " + filename + " to browser & res folder");

			currentfile = new File(res, filename);

			logger.debug(currentfile.getAbsolutePath());
			FileOutputStream fw;
			try {
				if (filename.endsWith(".html")) {
					out.writeBytes("Content-Type: "
							+ GetServable.getMime(currentfile) + "\n\n");

					String c = new String(cmd.getData());
					c = c.replace("exp://host/", hoststr);

					out.write(c.getBytes());
					out.flush();
					out.close();
				} else {

					fw = new FileOutputStream(currentfile);

					fw.write(cmd.getData());
					fw.flush();
					fw.close();

				}
			} catch (IOException e) {
				e.printStackTrace();
				final Writer result = new StringWriter();
				final PrintWriter printWriter = new PrintWriter(result);
				e.printStackTrace(printWriter);
				outQueue.add(new Message("vset", "_error", "text/plain", "YES",
						result.toString().getBytes()));
			}

			logger.debug("sent file " + filename + " to browser or res folder");
			// show
		}
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore
	}

}
