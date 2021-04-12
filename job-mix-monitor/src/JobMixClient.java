import lia.Monitor.JiniClient.Store.Main;

import lia.Monitor.monitor.monPredicate;
import lia.Monitor.monitor.Result;
import lia.Monitor.monitor.eResult;
import lia.Monitor.monitor.ExtResult;
import lia.Monitor.monitor.AccountingResult;
import lia.Monitor.monitor.ShutdownReceiver;
import lia.Monitor.monitor.DataReceiver;
import lia.Monitor.monitor.MFarm;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.System.console;
import static java.lang.System.exit;

public class JobMixClient {
    private static final Logger logger = Logger.getLogger(JobMixClient.class.getName());

    public static void main(String[] args){
        String url = null;
        String clusterInput = "";
        String clusterInputEos = "";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-u":
                    url = args[i + 1];
                    break;
                case "-c":
                    clusterInput = args[i + 1];
                    break;
                case "-e":
                    clusterInputEos = args[i + 1];
                    break;
            }
        }

//        TODO Make this smarter and take log level from App.properties
        try {
            FileHandler fh;
            // This block configure the logger with handler and formatter
            fh = new FileHandler("log/app");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.FINEST);

        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        logger.log(Level.INFO, "App is starting");
        if(clusterInput == null){
            logger.log(Level.SEVERE, "No cluster requested");
            exit(0);
        }

        String []clusters = clusterInput.split(":");
        String []clustersEos = clusterInputEos.split(":");

        // start the repository service
        final Main jClient = new Main();

        // register a MyDataReceiver object to receive any new information.
//        logger.log(Level.INFO, "Sending data to " + url);
        jClient.addDataReceiver(new MyDataReceiver(url));

        int c_check=0;
        boolean ret;

        for (String cluster : clusters) {
            ret = jClient.registerPredicate(new monPredicate(cluster, "Site_UserJobs_Summary", "*", -1, -1,
                    new String[]{"count", "cpu_time", "run_time", "rss", "virtualmem", "workdir_size"}, null));
            if (!ret)
                logger.log(Level.WARNING, "predicate for " + cluster + " not registered");
            c_check += ret ? 1 : 0;
        }

        for (String cluster : clustersEos) {
            ret = jClient.registerPredicate(new monPredicate(cluster, "ALICE::"+cluster+"::EOS_xrootd_Nodes", "*", -1, -1,
                    new String[]{"AVG_IOUtil", "MAXIOUTIL_IOPS", "MAXIOUTIL_IOUtil", "MAXIOUTIL_ReadIOPS", "MAXIOUTIL_ReadMBps", "MAXIOUTIL_TotalMBps", "MAXIOUTIL_WriteIOPS", "MAXIOUTIL_WriteMBps",
                                 "TOTAL_IOPS", "TOTAL_IOUtil", "TOTAL_ReadIOPS", "TOTAL_ReadMBps", "TOTAL_TotalMBps", "TOTAL_WriteIOPS", "TOTAL_WriteMBps", "TOTAL_devices", "TOTAL_NET_IN", "TOTAL_NET_OUT"}, null));
            if (!ret)
                logger.log(Level.WARNING, "predicate for EOS " + cluster + " not registered");
            c_check += ret ? 1 : 0;
        }

        if (c_check == clusters.length + clustersEos.length)
            logger.log(Level.INFO, "All requests were successful!");
        else
            logger.log(Level.WARNING,"Not all requests were successful!");

        if(c_check == 0){
            logger.log(Level.SEVERE, "No cluster request found");
            exit(0);
        }

        logger.log(Level.INFO, String.format("Registered predicates: %d, from a total of %d requested predicates\n", c_check, clusters.length + clustersEos.length));
    }

    /**
     * This is a very simple data receiver that puts some filters on the received data
     * and outputs the matching values on the console.
     */
    private static class MyDataReceiver implements DataReceiver, ShutdownReceiver {
        private ConcurrentHashMap<String, List<Result>> hashMap = new ConcurrentHashMap<>();
        private HttpClient httpClient    = HttpClient.newBuilder().build();
        private HttpRequest.Builder requestTemplate, requestTemplateEos;

/*      Url example
 *      postUrl = "http://localhost:5000/api/";
 */
        MyDataReceiver(String postUrl) {
            requestTemplate = HttpRequest.newBuilder()
                    .uri(URI.create(postUrl + "/jobmix/"))
                    .header("Content-Type", "application/data");
            requestTemplateEos = HttpRequest.newBuilder()
                    .uri(URI.create(postUrl + "/eos/"))
                    .header("Content-Type", "application/data");
        }

        public void Shutdown(){
            System.out.flush();
        }

        public void addResult(eResult r) {}

        private String buildMessage(List<Result> results) {
            StringBuilder sb = new StringBuilder();

            for (var r : results) {
                sb.append("time=").append(r.time).append(" Farm=").append(r.FarmName).append(" Node=").append(r.NodeName);
                for (int i = 0; i < r.param.length; i++)
                    sb.append(" ").append(r.param_name[i]).append("=").append(String.format("%f", r.param[i]));
                sb.append(";");
            }

            if (sb.length() > 0)
                sb.deleteCharAt(sb.length()-1);

            logger.log(Level.FINEST, sb.toString());
            return sb.toString();
        }

        //time=$val Farm=$val Node=$val $param_name=$param ...
        private String buildMessageEos(Result r) {
            StringBuilder sb = new StringBuilder();

            sb.append("eos,").append("farm=").append(r.FarmName).append(",node=").append(r.NodeName).append(" ");

            for (int i = 0; i < r.param.length; i++)
                sb.append(r.param_name[i]).append("=").append(String.format("%f", r.param[i])).append(",");
            sb.deleteCharAt(sb.length()-1);

            sb.append(" ").append(r.time);

            logger.log(Level.FINEST, sb.toString());
            return sb.toString();
        }

//      Send data to parser service
        private void sendData(String message, HttpRequest.Builder requestTemplate) {
            HttpRequest request = requestTemplate
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build();

            try {
                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.log(Level.FINEST, String.valueOf(response.statusCode()));
                logger.log(Level.FINEST, response.body());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* Because rows(NodeName) for a site(FarmName) do not come all together
        *  we store all entries that arrived in the last 2 seconds in a map<FarmName>.
        *  This way we would send a whole entry to parser, containing all rows
        *  Usually a site sends data every 30 seconds
        * */
        private void submitData(Result r) {
            if (!hashMap.containsKey(r.FarmName)) {
                List<Result> results = new ArrayList<>();
                results.add(r);
                hashMap.put(r.FarmName, results);
            }
            else {
                List<Result> results = hashMap.get(r.FarmName);
                Result lastResult = results.get(results.size()-1);
                long lastTime = lastResult.time;
                // compare time in seconds
                if ((r.time - lastTime) / 1000 < 2) {
                    results.add(r);
                } else {
//                  Send to parser previous entry
                    String message = buildMessage(hashMap.get(r.FarmName));
                    sendData(message, requestTemplate);
                    hashMap.remove(r.FarmName);
//                  Need to save this entry too
//                  There is no recursion here, just wanted to not duplicate code for the true branch of the if.
                    submitData(r);
                }
            }
        }

/*      We received a new entry
 * */
        public void addResult(Result r){
            if (r.ClusterName.equals("Site_UserJobs_Summary")) {
//              Do hashmap stuff and send it when we received all of them
                submitData(r);
            }
            else {
//              If it is Eos just send it
                String message = buildMessageEos(r);
                sendData(message, requestTemplateEos);
            }
            logger.log(Level.FINEST, r.toString());

        }

        public void addResult(ExtResult er){}

        public void addResult(AccountingResult ar){}

        public void updateConfig(MFarm f){}
    }
}
