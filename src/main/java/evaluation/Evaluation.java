/**
 *
 */
package evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import treeTransducer.CoNLLHash;
import treeTransducer.CoNLLTreeConstructor;

/**
 * @author Miguel Ballesteros Universitat Pompeu Fabra
 *
 */
public class Evaluation {

    private double precision;
    private double recall;
    private double f;

    private double LAS;
    private double UAS;
    private double LA;

    private String id0Str;

    private ArrayList<CoNLLHash> goldStandardHash;
    private ArrayList<CoNLLHash> outputHash;

    public Evaluation(Path goldStandard, Path output, String id0Str) throws IOException {
        this.id0Str = id0Str;
        String gold = new String(Files.readAllBytes(goldStandard));
        String system = new String(Files.readAllBytes(output));
        goldStandardHash = CoNLLTreeConstructor.loadTreebank(gold);
        outputHash = CoNLLTreeConstructor.loadTreebank(system);
        System.out.println("\n-----------Evaluation-------------\n");
    }

    private String getId0(String feats) {
        String id0 = "";
        StringTokenizer st = new StringTokenizer(feats);
        while (st.hasMoreTokens()) {
            String feat = st.nextToken("|");
            if (feat.contains(this.id0Str)) {
                id0 = feat.substring(4, feat.length());
            }
        }
        return id0;
    }

    public Map<String, Double> HyperNodeAccuracy() {

        int nodesGs = 0;
        int nodesDetected = 0;
        int nodesCorrectlyDetected = 0;

        for (int i = 0; i < outputHash.size(); i++) {
            CoNLLHash outputSentence = outputHash.get(i);
            CoNLLHash goldSentence = goldStandardHash.get(i);

            ArrayList<String> outputIds = outputSentence.getIds();
            nodesDetected += countNodes(outputIds, outputSentence);

            ArrayList<String> goldIds = goldSentence.getIds();
            nodesGs += countNodes(goldIds, goldSentence); //this method counts the nodes removing the correferences.

            for (int i_id = 0; i_id < outputIds.size(); i_id++) {
                String id = outputIds.get(i_id);
                String feats = outputSentence.getFEAT(id);
                String id0 = this.getId0(feats);
                if (!id0.contains("_")) {
                    if (findNode(id0, goldSentence)) {
                        nodesCorrectlyDetected++;
                    }
                }
            }
        }

        System.out.println("----HyperNode Detection:----");

        precision = (double) nodesCorrectlyDetected / (double) nodesDetected * 100;
        recall = (double) nodesCorrectlyDetected / (double) nodesGs * 100;
        f = (2 * precision * recall) / (precision + recall);

        System.out.println("Precision:" + precision);
        System.out.println("Recall:" + recall);
        System.out.println("F1:" + f);

        System.out.println(" #Nodes detected by the system: " + nodesDetected);
        System.out.println(" #Nodes in the gold-standard: " + nodesGs + " (by removing corefs)");
        System.out.println(" #Nodes correctly detected: " + nodesCorrectlyDetected);

        Map<String, Double> results = new HashMap<>();
        results.put("precision", precision);
        results.put("recall", recall);
        results.put("f1", f);
        results.put("nodes_detected_by_system", (double) nodesDetected);
        results.put("nodes_in_gold_standard", (double) nodesGs);
        results.put("nodes_correctly_detected", (double) nodesCorrectlyDetected);
        return results;

    }

    public Map<String, Double> nodeLabelAndAttachment() {
        int nodesGs = 0;
        int nodesDetected = 0;
        int nodesCorrectlyAttached = 0;
        int nodesCorrectlyLabelled = 0;
        int nodesCorrectlyAttachedAndLabelled = 0;

        int contSentences = 0;
        int completeAttachment = 0;
        int completeLabelAttachment = 0;

        boolean errorLabel = false;
        boolean errorAttach = false;
        for (int i = 0; i < outputHash.size(); i++) {  // for each sentence

            CoNLLHash outputSentence = outputHash.get(i);
            ArrayList<String> outputIds = outputSentence.getIds();
            nodesDetected += countNodes(outputIds, outputSentence);  // this method counts the nodes removing the correferences.

            CoNLLHash goldSentence = goldStandardHash.get(i);
            ArrayList<String> goldIds = goldSentence.getIds();
            nodesGs += countNodes(goldIds, goldSentence);  // this method counts the nodes removing the correferences.

            for (int i_id = 0; i_id < outputIds.size(); i_id++) {  // for each node inside a sentence

                String id = outputIds.get(i_id);

                String feats = outputSentence.getFEAT(id);
                String id0 = this.getId0(feats);
                String goldNode = this.returnNode(id0, goldSentence);

                if (!id0.contains("_")) {

                    if (goldNode != null && !goldNode.isEmpty()) {

                        String label = outputSentence.getDeprel(id);

                        String labelGold = goldSentence.getDeprel(goldNode);

                        boolean labelOk = false;
                        if (labelGold.equals(label)) {
                            nodesCorrectlyLabelled++;
                            labelOk = true;
                        } else {
                            errorLabel = true;
                        }

                        String head = outputSentence.getHead(id);

                        String headGold = goldSentence.getHead(goldNode);

                        if (head.equals("0") || (headGold.equals("0"))) {
                            if (head.equals(headGold)) {
                                nodesCorrectlyAttached++;
                                if (labelOk) {
                                    nodesCorrectlyAttachedAndLabelled++;
                                }
                            }
                        } else {
                            if (outputIds.contains(head)) {

                                String featsHead = outputSentence.getFEAT(head);
                                String featsHeadGold = goldSentence.getFEAT(headGold);

                                String id0Head = getId0(featsHead);
                                String id0HeadGold = getId0(featsHeadGold);

                                if (id0Head.equals(id0HeadGold)) {
                                    nodesCorrectlyAttached++;
                                    if (labelOk) {
                                        nodesCorrectlyAttachedAndLabelled++;
                                    }
                                } else {
                                    errorAttach = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!errorAttach) {
                completeAttachment++;
            }

            if (!errorAttach && !errorLabel) {
                completeLabelAttachment++;
            }

            errorAttach = false;
            errorLabel = false;
        }

        System.out.println("----Labelling and Attachment:----");
        double preLas = (double) nodesCorrectlyAttachedAndLabelled / (double) nodesDetected * 100;
        double preUas = (double) nodesCorrectlyAttached / (double) nodesDetected * 100;
        double preLA = (double) nodesCorrectlyLabelled / (double) nodesDetected * 100;

        double recLas = (double) nodesCorrectlyAttachedAndLabelled / (double) nodesGs * 100;
        double recUas = (double) nodesCorrectlyAttached / (double) nodesGs * 100;
        double recLA = (double) nodesCorrectlyLabelled / (double) nodesGs * 100;

        System.out.println("Precision LAS:" + preLas + " (" + nodesCorrectlyAttachedAndLabelled + "/" + nodesDetected + ")");
        System.out.println("Precision UAS:" + preUas + " (" + nodesCorrectlyAttached + "/" + nodesDetected + ")");
        System.out.println("Precision LA:" + preLA + " (" + nodesCorrectlyLabelled + "/" + nodesDetected + ")");
        System.out.println("-------------------");
        System.out.println("Recall LAS:" + recLas + " (" + nodesCorrectlyAttachedAndLabelled + "/" + nodesGs + ")");
        System.out.println("Recall UAS:" + recUas + " (" + nodesCorrectlyAttached + "/" + nodesGs + ")");
        System.out.println("Recall LA:" + recLA + " (" + nodesCorrectlyLabelled + "/" + nodesGs + ")");

        contSentences = outputHash.size();

        double ucm = (double) completeAttachment / (double) contSentences * 100;
        double lcm = (double) completeLabelAttachment / (double) contSentences * 100;
        System.out.println("-------------------");
        System.out.println("UCM:" + ucm + " (" + completeAttachment + "/" + contSentences + ")");
        System.out.println("LCM:" + lcm + " (" + completeLabelAttachment + "/" + contSentences + ")");

        Map<String, Double> results = new HashMap<>();
        results.put("precision_las", preLas);
        results.put("precision_uas", preUas);
        results.put("precision_la", preLA);
        results.put("recall_las", recLas);
        results.put("recall_uas", recUas);
        results.put("recall_la", recLA);
        results.put("ucm", ucm);
        results.put("lcm", lcm);
        return results;
    }

    /* SO FAR, IT DOES NOT COUNT NODES THAT ARE COREF NODES (FIRST VERSION!) */
    private int countNodes(ArrayList<String> ids, CoNLLHash goldSentence) {
        int cont = 0;

        for (int i_id = 0; i_id < ids.size(); i_id++) {
            String id = ids.get(i_id);
            String form = goldSentence.getForm(id);
            String feats = goldSentence.getFEAT(id);
            String id0 = this.getId0(feats);
            if (!(id0.contains("_"))) {  // IT DOES NOT COUNT NODES THAT ARE COREF NODES (FIRST VERSION!)
                cont++;
            }
        }

        return cont;
    }

    private boolean findNode(String id0, CoNLLHash goldSentence) {
        ArrayList<String> goldIds = goldSentence.getIds();

        for (int i_id = 0; i_id < goldIds.size(); i_id++) {
            String id = goldIds.get(i_id);
            String feats = goldSentence.getFEAT(id);
            String id0Gold = this.getId0(feats);
            if (id0Gold.equals(id0)) {
                return true;
            }
        }

        return false;
    }

    private String returnNode(String id0, CoNLLHash goldSentence) {
        ArrayList<String> goldIds = goldSentence.getIds();
        String node = "";

        for (int i_id = 0; i_id < goldIds.size(); i_id++) {
            String id = goldIds.get(i_id);
            String feats = goldSentence.getFEAT(id);
            String id0Gold = this.getId0(feats);
            if (id0Gold.equals(id0)) {
                return id;
            }
        }

        return node;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getF() {
        return f;
    }

    public double getLAS() {
        return LAS;
    }

    public double getUAS() {
        return UAS;
    }

    public double getLA() {
        return LA;
    }

    public static void main(String[] args) throws IOException {

        Option gsOpt = OptionBuilder.withArgName("Gold standard")
                .hasArg(true)
                .isRequired(false)
                .withDescription("Dsynt Gold")
                .withLongOpt("gsdsynt")
                .create("g");

        Option soOpt = OptionBuilder.withArgName("System output")
                .hasArg(true)
                .isRequired(false)
                .withDescription("dsynt treebank")
                .withLongOpt("tdsynt")
                .create("s");

        Options options = new Options();
        options.addOption(gsOpt);
        options.addOption(soOpt);

        CommandLineParser parser = new BasicParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            String id0Str = "id0=";
            Evaluation e = new Evaluation(Paths.get(line.getOptionValue("g")), Paths.get(line.getOptionValue("s")), id0Str);
            e.HyperNodeAccuracy();
            e.nodeLabelAndAttachment();

        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println(exp.getMessage());

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Evaluation Tree Transducer", options, true);
        }
    }
}
