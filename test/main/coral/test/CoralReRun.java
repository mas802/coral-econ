package coral.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.misc.resources.Messages;
import any.model.Message;
import coral.model.ExpData;
import coral.model.ExpStage;
import coral.service.ExpServable;
import coral.service.ExpServiceImpl;
import coral.service.IExpService;

public class CoralReRun {

    protected final Log logger = LogFactory.getLog(this.getClass());

    ExpServable testHandler;

    public static void main(String[] args) {

        CoralReRun ce = new CoralReRun();
        
        ce.init("290");
    }


    ArrayList<ArrayBlockingQueue<Message>> clients;
    ArrayList<ArrayList<String>> messages;
    ArrayList<Integer> counters;
    ArrayList<ArrayList<Integer>> globalposs;

    Integer globalpos = 0;
    
    
    public void init(String no) {
	globalpos = 0;
	
        testHandler = new ExpServable();

        Properties p = new Properties();

        File DIR = new File("~/Dropbox/EXPERIMENTS/snapPond (1)/");

        // p.setProperty("coral.exp.stages", "test/testwait.csv");
        p.setProperty("exp.basepath",
                "/home/schaffne/Dropbox/EXPERIMENTS/snapPond (1)/");
        p.setProperty("exp.stagesfile", "stages_color.csv");
        p.setProperty("coral.db.mode", "mem");

        testHandler.init(p, null, null);

        
        File file = new File(
                "/home/schaffne/Dropbox/EXPERIMENTS/snapPond (1)/experimentlogfile/snapPond"+no+".log");

        int N = 30;

        clients = new ArrayList<ArrayBlockingQueue<Message>>(N);
        messages = new ArrayList<ArrayList<String>>(N);
        counters = new ArrayList<Integer>(N);
        globalposs = new ArrayList<ArrayList<Integer>>(N);

        for (int i = 0; i < N; i++) {
            clients.add(new ArrayBlockingQueue<Message>(1000));
            messages.add( new ArrayList<String>() );
            globalposs.add( new ArrayList<Integer>() );
            globalposs.get(i).add(-99);
            counters.add(1);
        }

        System.out.println("reading " + no);

        int n = 0;
        Integer globcounter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String line = br.readLine();
//            line = br.readLine();

            
            while (line != null) {
                // System.out.println("start add stage: " + line);
                String[] seg = line.split("::");

                Integer i = Integer.parseInt(seg[0]) - 1;

                if (seg[3].startsWith("__BEGIN")) {
                    // testHandler.getService().addClient(i);
                    CoralReRun.CE test = this.new CE(i, clients.get(i));
                    test.start();
                    n++;
                    System.out.print( i + ".");
                }

                messages.get(i).add(seg[3]);
                globalposs.get(i).add(globcounter);
                
                // System.out.println( i + " <- " + seg[3] );
                // testHandler.process(new Message(seg[3]),
                // clients.get(i));

                globcounter++;
                line = br.readLine();
//                System.out.print( i + ".");

            }

            br.close();

        } catch (FileNotFoundException e) {
            logger.error("file not found", e);
        } catch (IOException e) {
            logger.error("I/O", e);
        }

        System.out.println(" done " + no);

        
        for( Integer s:globalposs.get(0)) {
            System.out.println(s);
        }
        

        System.out.println("starting ");

        for (int i = 0; i < n; i++) {
            //System.out.print("."+i);
            messages.get(i).add("FINISHED");
            testHandler.process(new Message("__BEGIN"), clients.get(i));
        }
        
        System.out.println(" done ");

        boolean running = true;

        while (running) {
            System.out.print("still running " + globalpos + "/" + globcounter + " ");
            boolean allfinished = true;
            for (int i = 0; i < n; i++) {

                Integer counter = counters.get(i);
                Integer size = messages.get(i).size();
                allfinished = allfinished && counter.equals(size);
                System.out.print(" ( " + i + ":" + counter + "/" + size + "-"
                        + allfinished + ")");
            }
            running = !allfinished;
            System.out.println("  ");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println(" end " + no);

        testHandler.process(new Message("__SERVER/?export=/home/schaffne/Dropbox/EXPERIMENTS/snapPond (1)/experimentRawfile/RERUN2_snapPond"+no+".raw"),
                clients.get(0));
        
        System.out.println(" done " + no);

    }

    class CE extends Thread {
        ArrayBlockingQueue<Message> queue;

        Integer id;

        CE(Integer id, ArrayBlockingQueue<Message> queue) {
            this.id = id;
            this.queue = queue;
        }

        @Override
        public void run() {

            try {

                ExpServiceImpl service = null;
                ExpData data = null;
                int length = -1;

                boolean finished = false;

                while (!finished) {
                    
                    Integer counter = counters.get(id);
                    while ( globalposs.get(id).size() < 2 ) {
                        Thread.sleep(100);
                    }
                    
                    while ( globalposs.get(id).get(counter) > globalpos ) {
                        // System.out.println( "id and counter: " + id + " " + counter + " " + globalpos + " " + globalposs.get(id).get(counter));
                        Thread.sleep(100);
                    }
                    
                    final Message acmd = queue.take();

                    Integer id2 = testHandler.getClientId(queue);
                    service = (ExpServiceImpl) testHandler.getService();
                    data = service.getData( id2 );

                    ExpStage stage = service.getStages()
                            .get(data._stageCounter);

                    Integer x = id;
                    if (id == x) {
                        System.out.println(id + " " + id2 + " " + data.get("number") + " " + stage.getTemplate()
                                + " <- " + acmd.getFullContent());
                    }


                    if (acmd.getFullContent().equals(
                            "/_exp.html?type=text/html&show=YES")) {
                        String msg;
                        do {
                            msg = messages.get(id).get(counter);
                            if (id == x) {
                                System.out.println(id + " ... n?: " + msg);
                            }
                            counter++;
                            globalpos++;
                        } while (msg.equals("?nextmsg=true") || (msg.startsWith("?refreshid=")));

                        finished = msg.equals("FINISHED");

                        if ( !stage.getWaitFor().equals("all") ) { //msg.startsWith("?wait=_waited") ) {

                            while ( msg.equals("?nextmsg=true") || msg.startsWith("?wait=_waited") ) {
                                msg = messages.get(id).get(counter);
                                if (id == x) {
                                    System.out.println(id + " ... x?: " + msg);
                                }
                                counter++;
                                globalpos++;
                                Thread.sleep(1000);
                            } 
                            
                            if (id == x) {
                                System.out.println(id + " -> " + msg);
                            }
                            testHandler.process(new Message(msg), queue);

                            Thread.sleep(10);
                        } else {
                            if (id == x) {
                                System.out.println(id + " WAIT ");
                            }
                            Thread.sleep(1000);
                        }
                        counters.set(id, counter);

                    }
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
