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
package coral.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.util.CSVWriter;

import any.model.Message;
import coral.CoralPolyp;
import coral.data.DataService;
import coral.model.ExpData;

public class ExpServer {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private ExpHandler ch;
    private DataService dataService;

    /*
     * private JFrame frame; private JScrollPane sp; private JTable jsp; private
     * DefaultTableModel messageTableModel;
     * 
     * private boolean scroll = true;
     * 
     * // private boolean jpanel = false;
     */

    public ExpServer(int port, String type, ExpHandler ech, DataService ds) {

	this.dataService = ds;
	this.ch = ech;
    }
    
    public void process(Integer client, String request,
	    BlockingQueue<Message> outQueue) {

	ExpServiceImpl service = ((ExpServiceImpl) ch.getService());

	int qindex = request.indexOf('?');
	String arg = "";
	if (qindex < 1) {
	    qindex = request.length();
	} else {
	    arg = request.substring(qindex + 1);
	}

	logger.info(" request: " + request + " qindex: " + qindex);

	String cmd = request.substring(9, qindex);

	// get args
	logger.debug("arg: ###" + arg + "###");
	Map<String, String> args = new HashMap<String, String>();
	for (String s : arg.substring(0, arg.length()).split("&")) {
	    String[] ss = s.split("=");
	    if (ss[0] != null) {
		args.put(ss[0], (ss.length > 1) ? ss[1] : "");
	    }
	}

	logger.info("server command " + cmd);

	String output = null;

	// do the deed
	if (cmd.length() > 0) {

	    StringBuilder msg = new StringBuilder();

	    Map<Integer, ExpData> data = service.getAllData();

	    Map<String, Object> adds = new HashMap<String, Object>();
	    // adds.put("_agentdata", data);
	    adds.put("_stages", service.getStages());
	    adds.put("_query", args);
	    adds.put("_clients", ch.getClientInfoMapList());

	    String content = ExpTemplateUtil.evalVM(cmd, data, null, service,
		    adds);

	    msg.append(content);

	    output = msg.toString();
	}

	if (args.containsKey("debug")) {
	    System.out.println("" + args.get("debug") + " - serv: "
		    + service.debug);
	    if (args.get("debug").equals("ON")) {
		service.debug = true;
		outQueue.add(new Message("info:?debug=true"));
	    }
	    if (args.get("debug").equals("OFF")) {
		service.debug = false;
		outQueue.add(new Message("info:?debug=false"));
	    }
	    System.out.println("" + args.get("debug") + " - serv: "
		    + service.debug);
	}

	if (args.containsKey("export")) {
	    String filename = args.get("export");

	    if (filename != null && !filename.equals("")) {

		File file = new File(filename);

		String msg = "Exporting data to " + file.getAbsolutePath()
			+ ", please wait...";
		outQueue.add(new Message("vset", "_exp.html", "text/plain",
			"YES", msg.getBytes()));

		try {
		    Thread.sleep(100);
		} catch (InterruptedException e2) {
		    // TODO Auto-generated catch block
		    e2.printStackTrace();
		}

		CSVWriter writer;
		try {
		    writer = new CSVWriter(file, "UTF-8");
		    List<String[]> stages = dataService.stageInfos();

		    List<String> headers = new ArrayList<String>();
		    List<String[]> datas = new ArrayList<String[]>();

		    headers.add("_id");
		    headers.add("_collection");
		    headers.add("_template");
		    headers.add("_stage");
		    headers.add("_inmsg");

		    for (String[] s : stages) {
			String[] data = new String[headers.size()];

			for (int i = 0; i < s.length; i++) {
			    if (i < s.length) {
				data[i] = s[i];
			    }
			}

			Map<String, String> map = dataService.getMap(s[1],
				Long.parseLong(s[0]));
			for (Map.Entry<String, String> e : map.entrySet()) {
			    if (!headers.contains(e.getKey())) {
				headers.add(e.getKey());
				data = Arrays.copyOf(data, headers.size());
			    }
			    data[headers.indexOf(e.getKey())] = e.getValue();
			}

			// writer.writeData(data.toArray(new String[] {}));
			datas.add(data);
		    }

		    System.out.println(Arrays.toString(headers.toArray()));
		    writer.writeHeader(headers.toArray(new String[] {}));

		    for (String[] s : datas) {
			writer.writeData(s);
			// System.out.println(Arrays.toString(s));
		    }

		    writer.close();
		} catch (IOException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		} catch (NumberFormatException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		} catch (SQLException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}

		String msg2 = "Exporting data to " + file.getAbsolutePath()
			+ ", sucessful...";
		outQueue.add(new Message("vset", "_exp.html", "text/plain",
			"YES", msg2.getBytes()));

	    }

	}

	if (args.containsKey("makeclient")) {
	    String props = args.get("makeclient");
	    try {
		props = URLDecoder.decode(props, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    logger.debug("make client " + props);

	    Properties generalProp = new Properties();

	    InputStream is = new ByteArrayInputStream(props.getBytes());
	    try {
		generalProp.load(is);
		is.close();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    // generalProp.put("coral.polyp.ontop", "false");

	    CoralPolyp.invokeClient(generalProp);

	    outQueue.add(new Message("info:?client=started", new byte[] {}));
	}

	if (output != null) {
	    outQueue.add(new Message("vset", "_exp.html", "text/html", "YES",
		    output.getBytes()));
	}
    }

}