package coral.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.model.Message;
import coral.model.ExpData;
import coral.model.ExpStage;
import coral.service.ExpServable;
import coral.service.ExpServiceImpl;

public class CoralSilentRun {

    protected final Log logger = LogFactory.getLog(this.getClass());

    ExpServable testHandler;
    Random r = new Random();
    
    public static void main(String[] args) throws InterruptedException {

        
        Thread.sleep(2000);
        
        
        CoralSilentRun ce = new CoralSilentRun();
        
        Properties p = new Properties();

        p.setProperty("exp.basepath", "/Users/mas/Dropbox/PEACH/peachSurvey");
        p.setProperty("exp.stagesfile", "stages.csv");

        p.setProperty("coral.db.name", "rerundb");
        p.setProperty("coral.db.mode", "mem");
        p.setProperty("coral.db.reset", "true");
        
        ce.init(p);
    }

    public void init(Properties p) {
        testHandler = new ExpServable();

        testHandler.init(p, null, null);

        int x = 200;

        Set<Thread> threads = new HashSet<Thread>();
        
        ArrayList<ArrayBlockingQueue<Message>> clients = new ArrayList<ArrayBlockingQueue<Message>>(
                x);
        for (int i = 0; i < x; i++) {
            clients.add(new ArrayBlockingQueue<Message>(100));

            testHandler.getService().addClient(i);

            CoralSilentRun.CE test = this.new CE(i, clients.get(i));
            test.setDaemon(false);
            test.start();
            
            threads.add(test);
        
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // kick off
        for (int i = 0; i < x; i++) {
            testHandler.process(new Message("/" + CoralUtils.START_KEY),
                    clients.get(i));
        }
        /*
         * testHandler.process(new Message("/" + ExpService.START_KEY, new
         * byte[] {}), client2); testHandler.process(new Message("/" +
         * ExpService.START_KEY, new byte[] {}), client3);
         * 
         * testHandler.process(new Message("/?test=1", new byte[] {}), client1);
         * testHandler.process(new Message("/?test=1", new byte[] {}), client2);
         * testHandler.process(new Message("/?test=1", new byte[] {}), client3);
         */
        
        boolean finished = false;
        while ( !finished ) {
            finished = true;
            for ( Thread t:threads) {
                if ( t.isAlive() ) {
                    finished = false;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // while ( true ) {}
        System.out.println(" end " );

        testHandler.process(new Message("__SERVER/?export=silentrun.raw"),
                clients.get(0));
        
        System.out.println(" done " );

    }

    public String values(ExpData values, String... props) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate formated " + Arrays.toString(props));
        }

        StringBuilder str = new StringBuilder();

        if (props != null) {
            for (String prop : props) {
                if (logger.isDebugEnabled()) {
                    logger.debug("return formated single: " + prop);
                }
                if (prop.equals("*")) {
                    // ressync condition
                } else if (prop.contains("==")) {
                    // equals value
                    String[] p = prop.split("==");
                    str.append(p[0] + "=" + p[1]);
                } else if (prop.contains(":=")) {
                    // equals variable
                    String[] p = prop.split(":=");
                    str.append(p[0] + "=" + values.markGet(p[1]));
                } else if (prop.contains("={")) {
                    // equals one of a set of values
                    String[] p = prop.split("=\\{");
                    String[] set = p[1].substring(0, p[1].length() - 1).split(
                            "::");
                    int i = r.nextInt(set.length);
                    str.append(p[0] + "=" + set[i]);
                } else if (prop.contains("::")) {
                    // number check
                    String[] p = prop.split("::");
                    double min = Double.parseDouble(p[1]);
                    double max = Double.parseDouble(p[2]);
                    int precision = (p[1].contains(".")) ? p[1].substring(
                            p[1].indexOf(".")).length() : 0;
                    str.append(p[0] + "="
                            + (min + r.nextInt((int)Math.ceil(max - min)+1)) );
                } else if (prop.contains(":")) {
                    int i = prop.indexOf(':');
                    // TODO
                } else {
                    // null or empty
                    str.append(prop + "=FREE_ENTRY");
                }
                str.append("&");
            }
        }
        return str.toString();
    }

    class CE extends Thread {
        ArrayBlockingQueue<Message> queue;

        // Integer id;

        CE(Integer id, ArrayBlockingQueue<Message> queue) {
            // this.id = id;
            this.queue = queue;
        }

        @Override
        public void run() {

            try {

                ExpServiceImpl service = null;
                ExpData data = null;
                int length = -1;

                while (data == null || data._stageCounter < length) {
                    final Message acmd = queue.take();

                    Integer id = testHandler.getClientId(queue);

                    service = (ExpServiceImpl) testHandler.getService();
                    data = service.getData(id);

                    ExpStage stage = service.getStages()
                            .get(data._stageCounter);
                    length = service.getStages().size() - 1;

                    String[] validate = stage.getValidate();
                    String[] simulated = stage.getSimulated();

                    String vals = values(data, validate);
                    vals += "&" + values(data, simulated);

                    testHandler.process(new Message("auto" + id + "?" + vals),
                            queue);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
