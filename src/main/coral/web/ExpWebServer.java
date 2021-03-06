package coral.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.IConnection;
import any.Linker;
import coral.CoralHead;
import coral.utils.CoralUtils;

public class ExpWebServer {

    protected final Log logger = LogFactory.getLog(this.getClass());

    Map<String, WebServable> map = new HashMap<String, WebServable>();

    private String hoststr;
    private String path;

    private int coralport;
    private ServerSocket server;

    private String startMarker;
    private String refreshMarker;
    private String serverMarker;

    // private String stopMarker = "__STOP";

    public ExpWebServer(String hoststr, String path, String host, int port,
            int coralport) throws IOException {
        this.hoststr = hoststr;
        // this.host = host;
        // this.port = port;
        this.path = path;
        this.coralport = coralport;
        this.server = new ServerSocket(port);
        Thread t = new Thread(new ServerExec());
        t.start();
    }
    
    public ExpWebServer(Properties properties) throws IOException {

        this.path = properties.getProperty("coral.web.path", "" );
        
        String host = properties.getProperty("coral.web.hostname", "localhost" );
        int port = Integer.parseInt( properties.getProperty("coral.web.port", "8080" ));
        
        this.hoststr = properties.getProperty("coral.web.exphost","http://" + host + ":" + port + "/");
        this.coralport =  Integer.parseInt(properties.getProperty( "any.port", "43802"));

        this.startMarker = properties.getProperty("coral.cmd.start",
                CoralUtils.START_KEY);
        this.refreshMarker = properties.getProperty("coral.cmd.refresh",
                CoralUtils.REFRESH_KEY);
        this.serverMarker = properties.getProperty("coral.cmd.server",
                CoralUtils.SERVER_KEY);

        this.server = new ServerSocket(port);
        Thread t = new Thread(new ServerExec());
        t.start();
    }
    

    public class ServerExec implements Runnable {

        public void run() {

            int i = 0;
            while (true) {

                try {

                    Socket client = server.accept();

                    int nbRunning = 0;
                    for (Thread t : Thread.getAllStackTraces().keySet()) {
                        if (t.getState() == Thread.State.RUNNABLE)
                            nbRunning++;
                    }

                    logger.info(client + " - " + nbRunning);

                    new WorkThread(i++, client).start();

                    logger.info("thread dispatched");

                } catch (Exception e) {
                    logger.error("Exception in server ", e);
                    // ignore
                }
            }
        }
    }

    class WorkThread extends Thread {

        Socket client;
        int i;

        public WorkThread(Integer i, Socket client) {
            this.client = client;
            this.i = i;
        }

        public void run() {

            try {
                BufferedReader reader = null;

                reader = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));
                DataOutputStream out = new DataOutputStream(
                        client.getOutputStream());

                String cond = null;
                String cookieid = "NO-COOKIE";

                // Get all input from header
                String line = null;
                while ((line = reader.readLine()) != null) {
                    logger.debug(line);
                    if (line.isEmpty()) {
                        break;
                    } else if (line.startsWith("GET /")) {
                        cond = line.split(" ")[1].replaceFirst(path, "");
                    } else if (line.startsWith("Cookie: ")) {
                        logger.info("COOKIE: " + line);
                        try {
                            cookieid = line.split("CORALID=")[1].split(";")[0];
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
                // s.close();

                logger.info(client + " - " + cond + " (" + cookieid + ")");

                if (cond != null
                        && (cond.startsWith("/" + startMarker)
                                || cond.startsWith("/" + refreshMarker) || cond
                                .startsWith("/" + serverMarker)
                                && (cookieid == null || !map
                                        .containsKey(cookieid)))) {

                    String myid = "CLIENT" + i;
                    Properties prop = new Properties();
                    final Linker s = new Linker(prop);

                    WebServable vset = new WebServable(myid, s, hoststr);
                    vset.init(prop, s.getNamedCon().get("loop").getOutQueue(),
                            null);

                    s.getServableMap().put("vset", vset);

                    s.connect("host", "localhost", coralport);

                    new Thread("vset init") {
                        public void run() {
                            IConnection host = s.getNamedCon().get("loop");
                            s.getServableMap().get("vset")
                                    .process(null, host.getOutQueue());
                        }
                    }.start();

                    map.put(myid, vset);
                    vset.in(cond, out);

                } else if (cond != null && cond.startsWith("/" + serverMarker)
                        && map.containsKey(cookieid)) {
                    // this is a server request

                    WebServable vset = map.get(cookieid);
                    vset.in(cond, out);

                } else if (cond != null
                        && (cond.startsWith("/exp") || cond.startsWith("/"
                                + refreshMarker)) && map.containsKey(cookieid)) {
                    // this is a standard contiuation

                    WebServable vset = map.get(cookieid);
                    vset.in(cond, out);

                } else if (map.containsKey(cookieid)) {
                    // treat it as a file request

                    WebServable vset = map.get(cookieid);
                    vset.file(cond, out);

                } else {

                    out.writeBytes("HTTP/1.1 500 Internal Server Error\n");
                    out.writeBytes("Cache-Control: no-cache, no-store, must-revalidate, max-stale=0, post-check=0, pre-check=0\n");
                    out.writeBytes("Expires: -1\n");
                    out.writeBytes("Pragma: no-cache\n");
                    out.writeBytes("Vary: *\n");
                    // out.writeBytes("Transfer-Encoding: chunked\n");
                    out.writeBytes("Content-Type: text/html\n\n");
                    out.writeBytes("<html>ERROR");
                    out.close();

                }

            } catch (IOException e) {
                logger.error("IO Exception in server ", e);
                try {
                    client.close();
                } catch (IOException e1) {
                    logger.error("IO Exception in server ", e);
                }
            }

        }

    }

    public static void main(String[] args) {

        String host = "localhost";
        int port = 8080;
        if (args.length > 0) {
            host = args[0];
        }

        try {
            ExpWebServer webServer = new ExpWebServer("http://" + host + ":"
                    + port + "/", "", host, port, 43802);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (args.length > 1) {
            args = new String[] { args[1] };
        } else {
            args = new String[] { "coral.properties" };
        }

        CoralHead.build(args);
    }
}