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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
    private ExpTemplateUtil util;

    public ExpTemplateUtil getUtil() {
        return util;
    }

    private Writer logwriter = null;

    public Map<Integer, ExpData> dataMap = new LinkedHashMap<Integer, ExpData>();

    private List<ExpStage> stages = new ArrayList<ExpStage>();

    Map<Integer, ArrayList<Integer>> clientstagecounter = new HashMap<Integer, ArrayList<Integer>>();

    public boolean debug = false;

    private final String startMarker;
    
    public ExpData getData(Integer id) {
        return dataMap.get(id);
    }

    public List<ExpStage> getStages() {
        return stages;
    }

    // private OETData logic = data;

    public ExpServiceImpl(ExpHandler clientHandler, Properties properties,
            DataService ds) {
        this( clientHandler, properties, ds, null );

        String logfilepath = properties.getProperty("coral.log.path", properties.getProperty("exp.basepath", "./"));
        String logfilename = properties.getProperty("coral.log.name", "coral.log");
        try {
            this.logwriter = new FileWriter(new File(logfilepath, logfilename), true);
        } catch (IOException e) {
            // THIS IS NOT GOOD == RUNTIMEEXCEPTION
            throw new RuntimeException( "fatal: log file could not be created ", e );
        }

    }
        
    public ExpServiceImpl(ExpHandler clientHandler, Properties properties,
                DataService ds, Writer logwriter) {
        this.dataService = ds;
        this.ch = clientHandler;
        this.logwriter = logwriter;
        
        this.startMarker = properties.getProperty("coral.cmd.start",
                CoralUtils.START_KEY);
        this.util = new ExpTemplateUtil(properties.getProperty("exp.basepath", "./"));

        logger.info("exp service started: " + this);
    }

    public void init(String basepath, String stagefile, String variants) {

        ExpStage startstage = new ExpStage("_START");
        stages.add(startstage);

        List<ExpStage> lines = CoralUtils.readStages(new File(basepath,
                stagefile), variants, new File(basepath));

        stages.addAll(lines);
    }

    public void addClient(Integer id) {

        if (!dataMap.containsKey(id)) {

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

            if (dataService != null) {
                dataService.retriveData(id + "", data);
            }
        }
    }

    @Override
    public void removeClient(Integer id) {
        synchronized (dataMap) {
            dataMap.remove(id);
        }
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

        ErrorFlag error = new ErrorFlag(true, util);

        // EVALUATE SCRIPT
        String output = null;
        logger.info("evaluate template " + filename);
        output = util.eval(filename, data, error, this);
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
        if (arg.equals(startMarker)) {
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
        
        /*
        // SAVE BEFORE INCOMING DATA
        data.put("_now", now);
        data.put("_nownano", nownano);
        data.put("_mode", "enter");
        data._msgCounter++;
        dataService.saveOETData(Integer.toString(id), data);
        */
        
        Map<String, String> args = CoralUtils.urlToMap(arg);

        // VALIDATION and debug/flow messages
        ErrorFlag condition = new ErrorFlag(false, util);
        boolean skiperror = args.containsKey("skiperror");
        boolean reload = args.containsKey("reload")
                || args.containsKey("refreshid");
        boolean skipback = args.containsKey("skipback");
        boolean noloop = args.containsKey("noloop");
        boolean skipederror = args.containsKey("skipederror");

        // TODO this is a quick fix to allow new clients with refresh
        if (reload && data._stageCounter == 0) {
            reload = false;
        }

        boolean isvalid = false;

        ErrorFlag error = new ErrorFlag(skiperror || reload || skipback, util);

        ExpStage enterstage = stages.get(data.stageCounter());

        data.putAll(args);

        if (!reload) {
            logger.info("validate stage  " + data.stageCounter() + " -> "
                    + enterstage.getTemplate() + " hash: "
                    + enterstage.hashCode());
            isvalid = error.validateFormated(data, enterstage.getValidate());

            /*
             * validate stageId if provided
             */
            if (args.containsKey("_stageIdCheck")
                    && !args.get("_stageIdCheck").equals("")) { // TODO or if
                                                                // this is
                                                                // enforced?
                String stageId = data.getString("_stageId");
                String stageIdCheck = args.get("_stageIdCheck");
                if (!stageId.equals(stageIdCheck)) {
                    isvalid = false;
                    // todo, check adverse effects
                    skipback = false;
                    error.put("_stageId", "invalid");
                }
                data.put("_stageIdCheck", "");

                if (logger.isDebugEnabled()) {
                    logger.debug("validate stage " + enterstage.getTemplate()
                            + " with id " + stageId + " against "
                            + stageIdCheck);
                }
            }

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
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                logger.error("main prosses interupted", e);
            }
        } else {
            logger.info("evaluate stage " + data.stageCounter() + " -> "
                    + thisstage.getTemplate() + " hash: "
                    + thisstage.hashCode());

            data.put("template", thisstage.getTemplate());
            data.put("_stageId",
                    thisstage.hashCode() + "" + (int) (Math.random() * 99999));
            output = util.eval(thisstage.getTemplate(), data, error,
                    this);

            data.setNewpage(false);
        }

        /*
        // SAVE OUTGOING DATA
        data.put("_now", Long.toString(System.currentTimeMillis()));
        data.put("_nownano", Long.toString(System.nanoTime()));
        data.put("_mode", "outgoing");
        data.put("_coralhost", _coralhost);
        data._msgCounter++;
        dataService.saveOETData(Integer.toString(id), data);
        */
        
        // BROADCAST OR ADVANCE

        if (output == null) {
            data.setNewpage(true);
            String append = (skiperror || skipederror) ? "&skipederror=true"
                    : "";
            if (skipback) {
                append += "&skipback=true";
            }
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
