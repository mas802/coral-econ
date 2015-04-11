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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.Linker;
import any.model.Message;
import any.model.Servable;
import any.servable.GetServable;
import coral.data.DataService;
import coral.data.DataServiceJbdcImpl;
import coral.utils.CoralUtils;
import coral.utils.WaitForFileThread;

public class ExpServable implements Servable, ExpHandler {

    protected final static Log logger = LogFactory.getLog(ExpServable.class);

    // Map with all the clients queues
    private Map<Integer, BlockingQueue<Message>> clients = new HashMap<Integer, BlockingQueue<Message>>();

    private IExpService service = null; // Service
    private DataService dataService;

    private ExpServer server;

    private Map<Integer, FileWriter> screenwriter = new HashMap<Integer, FileWriter>();
    private boolean useScreenwriter = false;

    private Integer clientCount = 0;

    private String basepath = "";
    private String stagesfile = "stages.csv";
    private String viewname;

    // SERVER INFO
    // Thread serverthread = null;
    // BlockingQueue<Message> serverqueue = null;

    Linker linker = null;

    String servervm = "info.vm";

    public ExpServable() {
    }

    public void process(Message cmd, BlockingQueue<Message> outQueue) {
        Integer id = getClientId(outQueue);
        String c = cmd.getFullContent();

        c.replaceAll("/__", "__");

        logger.info("run cmd on server: ####" + c + "#### to " + id);

        if (c.startsWith("__RES")) {
            logger.info("send resources");
            sendResources(outQueue);
        } else if (c.startsWith("__FILE/")) {
            logger.info("client " + id + " requests file " + c);
            int pos = c.indexOf("__FILE/");
            String filename = c.substring(pos + 7);
            service.evalTemplate(id, filename);
        } else if (c.startsWith("__SERVER")) {
            logger.info("client " + id + " requests server " + c);
            server.process(id, c, outQueue);
        } else if (c.startsWith("__REFRESH")) {
            id = Integer.parseInt(c.replaceAll("[^\\d]", ""));
            logger.info("refresh client " + id + " " + c);

            BlockingQueue<Message> oldq = clients.get(id);
            if (oldq != null) {
                clients.remove(oldq);
                clients.put(id, outQueue);

                service.process(id, "?refreshid=" + id);
            } else {
                logger.info("START new client with no this id");
                clients.put(id, outQueue);
                service.addClient(id);
                if (useScreenwriter) {
                    try {
                        FileWriter fw = new FileWriter(new File(
                                "client_screens" + id + ".html"));
                        screenwriter.put(id, fw);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                service.process(id, "?refreshid=" + id);
            }
        } else if (id == null) {
            logger.info("START new client with no id");
            clientCount = dataService.getNewId(clientCount + 1);
            clients.put(clientCount, outQueue);
            service.addClient(clientCount);
            id = clientCount;
            if (useScreenwriter) {
                try {
                    FileWriter fw = new FileWriter(new File("client_screens"
                            + id + ".html"));
                    screenwriter.put(id, fw);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            service.process(id, c);
        } else if (c.startsWith(IExpService.START_KEY)) {
            logger.info("START new client (discard old one)");
            clients.remove(outQueue);
            clientCount = dataService.getNewId(clientCount + 1);
            clients.put(clientCount, outQueue);
            service.addClient(clientCount);
            id = clientCount;
            service.process(id, c);
        } else if (c.startsWith(IExpService.KILL_KEY)) {

            // kills the client with the given id (any number in the command)
            id = Integer.parseInt(cmd.getQuery().get("id"));
            logger.info("KILL client " + id + " " + c);

            BlockingQueue<Message> oldq = clients.get(id);
            if (oldq != null) {
                oldq.add(new Message(
                        "vset:_error?content=client has been removed",
                        new byte[] {}));
                clients.remove(oldq);
                service.removeClient(id);
                outQueue.add(new Message("vset:_error?content=client " + id
                        + " has been removed "));
            } else {
                logger.info("client id " + id + " did not exist");
                outQueue.add(new Message("vset:_error?content=client " + id
                        + " does not exist "));
            }
            // clients.remove(outQueue);
            logger.info("client  discarded");
        } else {
            service.process(id, cmd.getFullContent());
        }

    }

    @Deprecated
    private void sendResources(BlockingQueue<Message> outQueue) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File(basepath
                    + "resources.csv")));

            String line = br.readLine();
            logger.info("start add resources: " + line);
            while ((line = br.readLine()) != null) {
                sendRes(outQueue, null, line);
            }
        } catch (FileNotFoundException e) {
            logger.error("file not found", e);
        } catch (IOException e) {
            logger.error("I/O", e);
        }
    }

    /*
     * sends a resourceto id
     */
    @Override
    public void sendRes(Integer id, String... res) {
        BlockingQueue<Message> outQueue = clients.get(id);
        sendRes(outQueue, res);

    }

    /*
     * sends a resource to queue (internal)
     */
    private void sendRes(BlockingQueue<Message> outQueue, String... res) {
        for (String line : res) {
            try {
                File file = new File(basepath, line);

                long version = file.lastModified();

                logger.info("sync file " + line + " version:" + version
                        + " path " + file.getCanonicalPath());

                try {
                    /*
                     * outQueue.put(new Message("ressync:" + file.getName() +
                     * "?show=NO&version=" + version + "&synccmd=" +
                     * URLEncoder.encode("get://host/" +
                     * file.getCanonicalPath(), "utf-8"), new byte[] {}));
                     */

                    // InputStream is;
                    // byte[] bytes = null;

                    String mime = GetServable.getMime(file);

                    BufferedInputStream reader = new BufferedInputStream(
                            new FileInputStream(file));

                    byte[] buffer = new byte[8096]; // == 1024

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int read;
                    while ((read = reader.read(buffer)) >= 0) {
                        baos.write(buffer, 0, read);
                    }
                    byte[] readbytes = baos.toByteArray();

                    outQueue.put(new Message("vset", file.getName(), mime,
                            "NO&version=" + version, readbytes));

                    reader.close();

                } catch (InterruptedException e) {
                    logger.error("i error", e);
                }
            } catch (FileNotFoundException e) {
                logger.error("file not found", e);
            } catch (IOException e) {
                logger.error("I/O", e);
            }
        }
    }

    @Override
    public void broadcast(Integer id, String msg) {
        BlockingQueue<Message> outQueue = clients.get(id);

        FileWriter fw = screenwriter.get(id);
        if (fw != null) {
            try {
                fw.append(msg);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        outQueue.add(new Message("vset", viewname, "text/html", "YES", msg
                .getBytes()));
    }

    public synchronized void wait(Integer id, String mode, int stageid, int loop) {
        if (mode.equals("all")) {
            waitForAll(id, stageid, loop);
        } else if (mode.endsWith(".txt")) {
            waitForFile(id, mode, stageid, loop);
        }
    }

    private Map<Integer, WaitForFileThread> waitForFile;

    private void waitForFile(Integer id, String mode, Integer stageid,
            Integer loop) {
        File fileWaitFor = new File(mode);

        if (fileWaitFor.exists()) {
            // if the file exists, just proceed
            service.process(id, "/?wait=_waited&_waitOnFile=existed");
        } else {
            /*
             * here it get a bit more involved, create or join a Thread that
             * waits for the file to come into existence and then proceed
             */
            int stage = stageid * 1000 + loop;

            if (waitForFile == null) {
                waitForFile = new HashMap<Integer, WaitForFileThread>();
            }

            synchronized (waitForFile) {

                if (!waitForFile.containsKey(stage)
                        || !waitForFile.get(stage).isAlive()) {
                    logger.debug("wait for file " + mode + " + stage " + stage
                            + " first create thread");
                    WaitForFileThread t = new WaitForFileThread(this,
                            fileWaitFor);
                    waitForFile.put(stage, t);
                    t.start();

                }
                // join thread
                if (waitForFile.get(stage).add(id)) {
                    logger.debug("wait for file " + mode + " + stage " + stage
                            + ": " + " == joined thread by client id: " + id);
                }
            }
        }
    }

    private Map<Integer, Set<Integer>> waitForAll;

    private void waitForAll(Integer id, Integer stageid, Integer loop) {

        int stage = stageid * 1000 + loop;

        if (waitForAll == null) {
            waitForAll = new HashMap<Integer, Set<Integer>>();
        }

        synchronized (waitForAll) {

            if (!waitForAll.containsKey(stage)) {
                waitForAll.put(stage, new HashSet<Integer>());
            }
            if (waitForAll.get(stage).add(id)) {
                logger.debug("wait for all stage " + stage + ": "
                        + waitForAll.get(stage).size() + " ==  "
                        + clients.size());
                if (waitForAll.get(stage).size() == clients.size()) {
                    try {
                        // give it a bit breathing time for loops (e.g. only
                        // one client signed in)
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    for (Integer i : waitForAll.get(stage)) {
                        service.process(i,
                                "?wait=_waited&_waitnr=" + clients.size());

                    }
                    waitForAll.remove(stage);
                }
            }
        }

    }

    @Override
    public IExpService getService() {
        return service;
    }

    @Override
    public Set<Integer> getServiceIds() {
        return ((ExpServiceImpl) service).dataMap.keySet();
    }

    public static String getMime(File file) {

        String mime = "text/plain";
        try {
            mime = new MimetypesFileTypeMap().getContentType(file);
        } catch (Error ex) {
            logger.error("Problem with mime type resolution.", ex);

            String n = file.getName();
            String e = n.substring(n.lastIndexOf('.') + 1).toLowerCase();

            String[][] types = new String[][] {
                    new String[] { "pdf", "application/pdf" },
                    new String[] { "html", "text/html" },
                    new String[] { "png", "image/png" },
                    new String[] { "anytable", "text/anytable" },
                    new String[] { "jpg", "image/jpg" } };

            for (String[] r : types) {
                if (r[0].equals(e)) {
                    mime = r[1];
                }
            }
        }

        return mime;
    }

    @Override
    public void init(Properties prop, BlockingQueue<Message> loopQueue,
            Linker linker) {

        this.linker = linker;

        this.basepath = prop.getProperty("exp.basepath", "./");

        String dbname = prop.getProperty("coral.db.name", "db");
        String dbmode = prop.getProperty("coral.db.mode", "file");
        boolean resetdb = (prop.getProperty("coral.db.reset", "false")
                .equals("true"));
        dataService = new DataServiceJbdcImpl(dbname, resetdb, dbmode);
        CoralUtils.hoststr = prop.getProperty("coral.head.coralhost",
                "exp://host/");
        ExpServiceImpl serv = new ExpServiceImpl(this, basepath, dataService);

        if (prop.containsKey("exp.stagesfile")) {
            this.stagesfile = prop.getProperty("exp.stagesfile");
        }
        serv.init(basepath, stagesfile, prop.getProperty("coral.exp.variant"));

        this.viewname = prop.getProperty("exp.viewname", "_exp.html");

        this.server = new ExpServer(0, prop.getProperty("exp.servertype",
                "none"), this, dataService);

        serv.debug = (prop.getProperty("coral.debug", "false").equals("true"));
        logger.debug("debug status is "
                + prop.getProperty("exp.debug", "false") + "  --  "
                + serv.debug);

        useScreenwriter = (prop.getProperty("coral.head.screenwriter", "false")
                .equals("true"));

        this.service = serv;
    }

    @Override
    public void disconnect(BlockingQueue<Message> outQueue) {
        // default, ignore
    }

    public ArrayList<Map<String, Object>> getClientInfoMapList() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (String name : linker.getNamedCon().keySet()) {
            Map<String, Object> c = new HashMap<String, Object>();

            c.put("name", name);

            BlockingQueue<Message> queue = linker.getNamedCon().get(name)
                    .getOutQueue();

            Integer id = getClientId(queue);

            if (id != null) {
                c.put("connected", true);
                c.put("id", id);
                c.put("data", service.getData(id));
            } else {
                c.put("connected", false);
            }

            result.add(c);
        }

        return result;
    }

    public Integer getClientId(BlockingQueue<Message> queue) {
        return CoralUtils.getKeyByValue(clients, queue);
    }

}
