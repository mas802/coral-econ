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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import coral.data.DataService;
import coral.model.ExpData;
import coral.model.ExpStage;
import coral.utils.CoralUtils;

/**
 * Default implementation of the ExpService class, implements the setup and core
 * loop.
 * 
 * @author Markus Schaffner
 * 
 */
public class ExpServiceImpl implements IExpService {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private ExpHandler ch;
    // private int id;
    private DataService dataService;
    private ExpTemplateUtil baseService;;

    private FileWriter logwriter = null;

    public Map<Integer, ExpData> dataMap = new LinkedHashMap<Integer, ExpData>();

    private List<ExpStage> stages = new ArrayList<ExpStage>();

    Map<Integer, ArrayList<Integer>> clientstagecounter = new HashMap<Integer, ArrayList<Integer>>();

    public boolean debug = false;

    private String _coralhost;

    public ExpData getData(Integer id) {
	return dataMap.get(id);
    }

    public List<ExpStage> getStages() {
	return stages;
    }

    // private OETData logic = data;

    public ExpServiceImpl(ExpHandler clientHandler, String basepath,
	    DataService ds) {
	this._coralhost = CoralUtils.getHostStr();
	this.dataService = ds;
	this.ch = clientHandler;
	this.baseService = new ExpTemplateUtil(basepath);
	try {
	    this.logwriter = new FileWriter(new File("coral.log"), true);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void init(String basepath, String stagefile, String variants) {

	ExpStage startstage = new ExpStage("_START");
	stages.add(startstage);

	ArrayList<String> lines = CoralUtils.readStages(new File(basepath,
		stagefile));

	int counter = 0;
	Map<String, Integer> loopMap = new HashMap<String, Integer>();

	for (String line : lines) {

	    counter++;
	    logger.debug("add stage: 1 : " + line);
	    String[] parts = line.split(",");
	    int l = parts.length;

	    String[] condition = new String[] {};
	    String[] valid = new String[] {};
	    String waitFor = "";
	    int loopstage = 0;
	    int looprepeat = 0;

	    if (l > 1) {
		try {
		    logger.debug("add stage: 2 : " + Arrays.toString(parts));

		    if (parts[1].startsWith(":")) {
			loopMap.put(parts[1].substring(1), counter);
		    }
		    if (parts[1].endsWith(":")) {
			loopstage = counter
				- loopMap.get(parts[1].substring(0,
					parts[1].length() - 1));
		    } else {
			loopstage = (parts[1] == null) ? 0 : Integer
				.parseInt(CoralUtils.trimQuotes(parts[1]));
		    }
		    looprepeat = (parts[2] == null) ? 0 : Integer
			    .parseInt(CoralUtils.trimQuotes(parts[2]));
		} catch (NumberFormatException e) {
		    // e.printStackTrace();
		}
	    }
	    if (l > 3) {
		logger.debug("add stage: condition : " + parts[3]);
		condition = (parts[3].length() > 0) ? CoralUtils.trimQuotes(
			parts[3]).split(";") : condition;
	    }
	    if (l > 4) {
		logger.debug("add stage: validate : " + parts[4]);
		valid = (parts[4].length() > 0) ? CoralUtils.trimQuotes(
			parts[4]).split(";") : valid;
	    }
	    if (l > 5) {
		logger.debug("add stage: waitFor : "
			+ CoralUtils.trimQuotes(parts[5]));
		waitFor = CoralUtils.trimQuotes(parts[5]);
	    }

	    String name = CoralUtils.trimQuotes(parts[0]);

	    if (variants != null) {
		int extpos = name.lastIndexOf(".");
		if (extpos > 0) {
		    String localised = name.substring(0, extpos + 1) + variants
			    + name.substring(extpos);
		    File test = new File(basepath, localised);
		    logger.debug("check for variant: " + test.getAbsolutePath());
		    if (test.exists()) {
			name = localised;
		    }
		}
	    }

	    File test = new File(basepath, name);
	    if (!test.exists() && !(parts.length > 3 && parts[3].equals("*"))) {
		throw new RuntimeException("stage file " + name
			+ " does not exist in path " + basepath + "  ---  "
			+ test.getAbsolutePath());
	    }
	    ExpStage stage = new ExpStage(name, loopstage, looprepeat,
		    condition, valid, waitFor);
	    stages.add(stage);
	    logger.debug("add stage: " + stage);
	}
    }

    public void addClient(Integer id) {

	ExpData data = new ExpData();
	data.put("id", id);

	synchronized (dataMap) {
	    int agent = dataMap.size();
	    data.put("agent", agent);
	    dataMap.put(id, data);
	}

	// Initialise Randomness
	int r = (int) (Math.random() * 99999);

	data.put("randomseed", Integer.toString(r));

	data.setNewpage(true);

	ArrayList<Integer> l = new ArrayList<Integer>(stages.size());
	for (int i = 0; i < stages.size(); i++) {
	    l.add(-1);
	}

	clientstagecounter.put(id, l);
	data._stageCounter = 0;

    }

    @Override
    public void removeClient(Integer id) {
	synchronized (dataMap) {
	    dataMap.remove(id);
	}
    }

    public void setOETData(Integer id, ExpData data) {
	dataMap.put(id, data);
    }

    public void evalTemplate(Integer id, String filename) {
	ExpData data = dataMap.get(id);
	logger.info("enter process template " + id + " with filename: "
		+ filename);

	String now = Long.toString(System.currentTimeMillis());
	String nownano = Long.toString(System.nanoTime());
	try {
	    String out = id + "::" + now + "::" + nownano + "::template="
		    + filename + "\n";
	    logwriter.write(out);
	    logwriter.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	ErrorFlag error = new ErrorFlag(true);

	// EVALUATE SCRIPT
	String output = null;
	logger.info("evaluate template " + filename);
	output = baseService.eval(filename, data, error, this);
	ch.broadcast(id, output);
    }

    /**
     * Implementation of the core loop as described.
     */
    public synchronized void process(Integer id, String arg) {
	// TODO, get an appropriate data objects (no reuse!)
	ExpData data = dataMap.get(id);
	logger.info("enter process client " + id + " with args: " + arg
		+ " stage: " + data._stageCounter);

	// set start conditions if requested might be obsolete
	if (arg.equals(IExpService.START_KEY)) {
	    data._msgCounter = 0;
	    data._stageCounter = 0;
	}
	data.inmsg = arg;

	String now = Long.toString(System.currentTimeMillis());
	String nownano = Long.toString(System.nanoTime());
	try {
	    String out = id + "::" + now + "::" + nownano + "::" + arg + "\n";
	    logwriter.write(out);
	    logwriter.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	// SAVE BEFORE INCOMING DATA
	data.put("_now", now);
	data.put("_nownano", nownano);
	data.put("_mode", "enter");
	data._msgCounter++;
	dataService.saveOETData(Integer.toString(id), data);

	// convert url entry to data
	Map<String, String> args = CoralUtils.urlToMap(arg);
	data.putAll(args);

	// VALIDATION and debug/flow messages
	ErrorFlag condition = new ErrorFlag(false);
	boolean skiperror = args.containsKey("skiperror");
	boolean reload = args.containsKey("reload")
		|| args.containsKey("refreshid");
	boolean skipback = args.containsKey("skipback");
	boolean noloop = args.containsKey("noloop");
	boolean skipederror = args.containsKey("skipederror");

	ErrorFlag error = new ErrorFlag(skiperror || reload);

	ExpStage enterstage = stages.get(data.stageCounter());
	boolean isvalid;
	if (!reload) {
	    logger.info("validate stage  " + data.stageCounter() + " -> "
		    + enterstage.getTemplate() + "");
	    isvalid = error.validateFormated(data, enterstage.getValidate());
	} else {
	    isvalid = true;
	}

	// SAVE INCOMING DATA
	data.put("_now", Long.toString(System.currentTimeMillis()));
	data.put("_nownano", Long.toString(System.nanoTime()));
	data.put("_mode", "incoming");
	data.put("_valid", isvalid);
	data.template = enterstage.getTemplate();
	data._msgCounter++;
	dataService.saveOETData(Integer.toString(id), data);

	// CHANGE STAGE AND LOOP
	ExpStage thisstage = stages.get(data.stageCounter());
	boolean meetscondition = condition.validateFormated(data,
		thisstage.getCondition());
	if (logger.isDebugEnabled())
	    logger.debug("is condition valid: " + condition.isValid());

	if (!skipback
		&& !reload
		&& (isvalid || !meetscondition)
		&& (thisstage.getWaitFor().equals("") || (args
			.containsKey("wait") && args.get("wait").equals(
			"_waited")))) {

	    // set this repeat if not already countingIs
	    if (isvalid
		    && clientstagecounter.get(id).get(data.stageCounter()) < 0
		    && enterstage.getLooprepeat() >= 0) {
		clientstagecounter.get(id).set(data.stageCounter(),
			enterstage.getLooprepeat());
	    }

	    // check if loop or progress
	    if (isvalid
		    && ((clientstagecounter.get(id).get(data.stageCounter()) > 0 && !noloop) || ((clientstagecounter
			    .get(id).get(data.stageCounter()) < 0 && !skipederror)))) {
		logger.debug("repeat stage");
		int repeat = clientstagecounter.get(id)
			.get(data.stageCounter());
		clientstagecounter.get(id).set(data.stageCounter(),
			(repeat - 1));
		data._stageCounter -= enterstage.getLoopback();
	    } else if ((data.stageCounter() + 1) < stages.size()) {
		logger.debug("next stage");
		int repeat = clientstagecounter.get(id)
			.get(data.stageCounter());
		clientstagecounter.get(id).set(data.stageCounter(),
			(repeat - 1));
		data._stageCounter++;
	    }
	    if (logger.isDebugEnabled())
		logger.debug("loop counter state: "
			+ Arrays.toString(clientstagecounter.get(id).toArray()));

	    thisstage = stages.get(data.stageCounter());
	    meetscondition = condition.validateFormated(data,
		    thisstage.getCondition());

	    while (!meetscondition
		    && ((data.stageCounter() + 1) < stages.size())) {
		logger.debug("next stage with condition: ###"
			+ Arrays.toString(thisstage.getCondition()) + "###");
		data._stageCounter++;
		thisstage = stages.get(data.stageCounter());
		meetscondition = condition.validateFormated(data,
			thisstage.getCondition());
	    }
	    data.setNewpage(true);
	} else if (skipback) {

	    if ((data.stageCounter()) > 1) {
		logger.debug("previous stage");
		data._stageCounter--;
	    }

	    thisstage = stages.get(data.stageCounter());
	    data.setNewpage(true);
	}

	// EVALUATE SCRIPT

	String output = null;

	// TODO clean up code to ressync
	if ((thisstage.getCondition() != null
		&& thisstage.getCondition().length > 0 && thisstage
		    .getCondition()[0].equals("*"))) {
	    ch.sendRes(id, thisstage.getTemplate());
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		logger.error("main prosses interupted", e);
	    }
	} else {
	    logger.info("evaluate stage " + data.stageCounter() + " -> "
		    + thisstage.getTemplate() + "");

	    data.put("template", thisstage.getTemplate());
	    output = baseService.eval(thisstage.getTemplate(), data, error,
		    this);

	    data.setNewpage(false);
	}

	// SAVE OUTGOING DATA

	data.put("_now", Long.toString(System.currentTimeMillis()));
	data.put("_nownano", Long.toString(System.nanoTime()));
	data.put("_mode", "outgoing");
	data.put("_coralhost", _coralhost);
	data._msgCounter++;
	dataService.saveOETData(Integer.toString(id), data);

	// BROADCAST OR ADVANCE

	if (output == null) {
	    data.setNewpage(true);
	    String append = (skiperror || skipederror) ? "&skipederror=true"
		    : "";
	    process(id, "?nextmsg=true" + append + "");
	} else {
	    ch.broadcast(id, output);
	}

	if (!thisstage.getWaitFor().equals("")) {
	    data.put("_waitnr", "");
	    ch.wait(id, thisstage.getWaitFor(), data.stageCounter(),
		    clientstagecounter.get(id).get(data.stageCounter()));
	}
    }

    public Map<Integer, ExpData> getAllData() {
	return dataMap;
    }

    @Override
    public boolean isDebug() {
	return debug;
    }

}
