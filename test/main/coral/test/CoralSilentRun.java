package coral.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.model.Message;
import coral.model.ExpData;
import coral.model.ExpStage;
import coral.service.ExpServable;
import coral.service.ExpServiceImpl;
import coral.utils.CoralUtils;

public class CoralSilentRun {

    protected final Log logger = LogFactory.getLog(this.getClass());

    ExpServable testHandler;

    public static void main(String[] args) {

        CoralSilentRun ce = new CoralSilentRun();
        ce.init();
    }

    public void init() {
        testHandler = new ExpServable();

        Properties p = new Properties();

        // p.setProperty("coral.exp.stages", "test/testwait.csv");
        p.setProperty("exp.stagesfile", "coralpres/coralpres.csv");
        p.setProperty("coral.db.mode", "mem");

        testHandler.init(p, null, null);

        int x = 2;

        ArrayList<ArrayBlockingQueue<Message>> clients = new ArrayList<ArrayBlockingQueue<Message>>(
                16);
        for (int i = 0; i < x; i++) {
            clients.add(new ArrayBlockingQueue<Message>(1000));

            testHandler.getService().addClient(i);

            CoralSilentRun.CE test = this.new CE(i, clients.get(i));
            test.start();
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
                } else if (prop.contains("::")) {
                    // number check
                    String[] p = prop.split("::");
                    double min = Double.parseDouble(p[1]);
                    double max = Double.parseDouble(p[2]);
                    int precision = (p[1].contains(".")) ? p[1].substring(
                            p[1].indexOf(".")).length() : 0;
                    str.append(p[0] + "=" + min);
                } else if (prop.contains(":")) {
                    int i = prop.indexOf(':');
                    // TODO
                } else {
                    // null or empty
                    str.append(prop + "=notnull");
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

                    String vals = values(data, validate);

                    testHandler.process(new Message("auto" + id + "?" + vals),
                            queue);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // TODO Auto-generated method stub
            super.run();
        }

    }

}
