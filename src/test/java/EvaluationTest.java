
import evaluation.Evaluation;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author lpmayos
 */
public class EvaluationTest {

    String deepDatasetsPath = "/Users/lpmayos/code/parsingEval/src/main/resources/datasets_deep";
    String surfaceDatasetsPath = "/Users/lpmayos/code/parsingEval/src/main/resources/datasets_surface";
    
    public EvaluationTest() {
        
    }

    @Test
    public void equalFilesTest() throws Exception {
        // Test: predicted and gold are the same tree

        Path goldPath = Paths.get(this.deepDatasetsPath + "/EN_Deep/gold_ud2.1_ud-treebanks-v2.1_UD_English_en-ud-test.conll_out.conll");
        String parserName = "C2L2_2017-05-13-07-09-39";
        Path candidatePath = Paths.get(this.deepDatasetsPath + "/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_C2L2_2017-05-13-07-09-39_output_en.conll_out.conll");
        
        String id0Str = "id1="; 
        
        Evaluation e = new Evaluation(goldPath, candidatePath, id0Str);
        Map<String, Double> results1 = e.HyperNodeAccuracy();
        Map<String, Double> results2 = e.nodeLabelAndAttachment();
        
//        assertEquals(0.0, distance, 0.0);
    }

    private void addDeepEvaluation(String goldPathStr, String intrinsicEvalResultsPathStr, String candidateStringFormat, String newResultsPathStr) throws Exception {
        // For each predicted surface conll file looks for the deep conll file
        // (using the team name), conducts deep evaluation and adds the deep
        // metrics to the surface metrics, writtig them to a new json file 
        // parsingEval_results_with_extrinsic.json

        Path goldPath = Paths.get(goldPathStr);

        String id0Str = "id1="; 
        
        JSONParser jsonParser = new JSONParser();
        Object obj = jsonParser.parse(new FileReader(intrinsicEvalResultsPathStr));
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray predictions = (JSONArray) jsonObject.get("predictions");
        Iterator<JSONObject> iterator = predictions.iterator();
        while (iterator.hasNext()) {
            JSONObject jsonPrediction = iterator.next();
            String parserName = (String) jsonPrediction.get("parser");  // i.e. "C2L2_2017-05-13-07-09-39";
            Path candidatePath = Paths.get(String.format(candidateStringFormat, parserName));
            
            Evaluation e = new Evaluation(goldPath, candidatePath, id0Str);
            Map<String, Double> results1 = e.HyperNodeAccuracy();
            Map<String, Double> results2 = e.nodeLabelAndAttachment();

            JSONObject jsonMetrics = (JSONObject) jsonPrediction.get("metrics");
            
            Map<String, Double> precision = new HashMap<>();
            precision.put("average", results1.get("precision"));
            jsonMetrics.put("deep_precision", precision);
            
            Map<String, Double> recall = new HashMap<>();
            recall.put("average", results1.get("recall"));
            jsonMetrics.put("deep_recall", recall);
            
            Map<String, Double> f1 = new HashMap<>();
            f1.put("average", results1.get("f1"));
            jsonMetrics.put("deep_f1", f1);

            Map<String, Double> nodesDetected = new HashMap<>();
            nodesDetected.put("average", results1.get("nodes_detected_by_system"));
            jsonMetrics.put("deep_nodes_detected_by_system", nodesDetected);

            Map<String, Double> nodesGs = new HashMap<>();
            nodesGs.put("average", results1.get("nodes_in_gold_standard"));
            jsonMetrics.put("deep_nodes_in_gold_standard", nodesGs);

            Map<String, Double> nodesCorrectlyDetected = new HashMap<>();
            nodesCorrectlyDetected.put("average", results1.get("nodes_correctly_detected"));
            jsonMetrics.put("deep_nodes_correctly_detected", nodesCorrectlyDetected);            

            Map<String, Double> preLas = new HashMap<>();
            preLas.put("average", results2.get("precision_las"));
            jsonMetrics.put("deep_precision_las", preLas); 

            Map<String, Double> preUas = new HashMap<>();
            preUas.put("average", results2.get("precision_uas"));
            jsonMetrics.put("deep_precision_uas", preUas); 
            
            Map<String, Double> preLA = new HashMap<>();
            preLA.put("average", results2.get("precision_la"));
            jsonMetrics.put("deep_precision_la", preLA); 
            
            Map<String, Double> recLas = new HashMap<>();
            recLas.put("average", results2.get("recall_las"));
            jsonMetrics.put("deep_recall_las", recLas); 
            
            Map<String, Double> recUas = new HashMap<>();
            recUas.put("average", results2.get("recall_uas"));
            jsonMetrics.put("deep_recall_uas", recUas); 
            
            Map<String, Double> recLA = new HashMap<>();
            recLA.put("average", results2.get("recall_la"));
            jsonMetrics.put("deep_recall_la", recLA); 
            
            Map<String, Double> ucm = new HashMap<>();
            ucm.put("average", results2.get("ucm"));
            jsonMetrics.put("deep_ucm", ucm); 
            
            Map<String, Double> lcm = new HashMap<>();
            lcm.put("average", results2.get("lcm"));
            jsonMetrics.put("deep_lcm", lcm); 
            
        }
        
        FileWriter file = new FileWriter(newResultsPathStr);
        file.write(jsonObject.toJSONString());
        file.flush();
    }
    
    @Test
    public void completeTestEN() throws Exception {
        String goldPathStr = this.deepDatasetsPath + "/EN_Deep/gold_ud2.1_ud-treebanks-v2.1_UD_English_en-ud-test.conll_out.conll";
        String intrinsicEvalResultsPathStr = this.surfaceDatasetsPath + "/en_well_splitted_dataset/parsingEval_results.json";
        String candidateStringFormat = this.deepDatasetsPath + "/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_%s_output_en.conll_out.conll";
        String newResultsPathStr = this.surfaceDatasetsPath + "en_well_splitted_dataset/parsingEval_results_new_metrics_with_extrinsic.json";
        addDeepEvaluation(goldPathStr, intrinsicEvalResultsPathStr, candidateStringFormat, newResultsPathStr);
    }

    @Test
    public void completeTestENManualGold() throws Exception {
        String goldPathStr = this.deepDatasetsPath + "/EN_Deep_manual_gold/gold_EN_deep_SIMON.conll";
        String intrinsicEvalResultsPathStr = this.surfaceDatasetsPath + "/en_well_splitted_dataset/parsingEval_results_new_metrics.json";
        String candidateStringFormat = this.deepDatasetsPath + "/EN_Deep_manual_gold/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_%s_output_en.conll_out.conll";
        String newResultsPathStr = this.surfaceDatasetsPath + "en_well_splitted_dataset/parsingEval_results_new_metrics_with_manual_gold_with_extrinsic.json";
        addDeepEvaluation(goldPathStr, intrinsicEvalResultsPathStr, candidateStringFormat, newResultsPathStr);
    }
    
    @Test
    public void completeTestFR() throws Exception {
        String goldPathStr = this.deepDatasetsPath + "/FR_Deep/gold_ud2.1_ud-treebanks-v2.1_UD_French_fr-ud-test.conll_out.conll";
        String intrinsicEvalResultsPathStr = this.surfaceDatasetsPath + "/fr_well_splitted_dataset/parsingEval_results.json";
        String candidateStringFormat = this.deepDatasetsPath + "/FR_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_%s_output_fr.conll_out.conll";
        String newResultsPathStr = this.surfaceDatasetsPath + "/fr_well_splitted_dataset/parsingEval_results_with_extrinsic.json";
        addDeepEvaluation(goldPathStr, intrinsicEvalResultsPathStr, candidateStringFormat, newResultsPathStr);
    }
    
}
