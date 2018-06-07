
import evaluation.Evaluation;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Test;


/**
 *
 * @author lpmayos
 */
public class CompareFilesTest {

    public CompareFilesTest() {
        
    }

    @Test
    public void CompareGoldsEnTest() throws Exception {
        
        String manualGoldFolder = "/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/";
        Path manualGoldPath = Paths.get(manualGoldFolder + "gold_EN_deep_SIMON.conll");
        
        String goldsFolder = "/home/lpmayos/code/parsingEval/src/main/resources/golds_test/en/";
        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(goldsFolder + "UD-Deep_EN-A1.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_EN-A1INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_EN-A2.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_EN-A2INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_EN-AM.conll"));
        
        String id0Str = "id1="; 

        PrintWriter writer = new PrintWriter(goldsFolder + "results.txt", "UTF-8");
        
        String name = manualGoldPath.toString().split(manualGoldFolder)[1];
        writer.println("English: deep_recall_las of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_las = results.get("recall_las");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_las.toString() + "\t\t" + name);
        }

        writer.println("\nEnglish: deep_recall_uas of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_uas = results.get("recall_uas");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_uas.toString() + "\t\t" + name);
        }
        
        writer.close();
    }

    @Test
    public void CompareGoldsFrTest() throws Exception {
        
        String manualGoldFolder = "/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/FR_Deep/";
        Path manualGoldPath = Paths.get(manualGoldFolder + "gold_FR_deep_SIMON.conll");
        
        String goldsFolder = "/home/lpmayos/code/parsingEval/src/main/resources/golds_test/fr/";
        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(goldsFolder + "UD-Deep_FR-A1.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_FR-A1INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_FR-A2.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_FR-A2INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_FR-AM.conll"));
                
        String id0Str = "id1="; 

        PrintWriter writer = new PrintWriter(goldsFolder + "results.txt", "UTF-8");
        
        String name = manualGoldPath.toString().split(manualGoldFolder)[1];
        writer.println("French: deep_recall_las of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_las = results.get("recall_las");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_las.toString() + "\t\t" + name);
        }

        writer.println("\nFrench: deep_recall_uas of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_uas = results.get("recall_uas");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_uas.toString() + "\t\t" + name);
        }
        
        writer.close();
    }

    @Test
    public void CompareGoldsEsTest() throws Exception {
        
        String manualGoldFolder = "/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/ES_Deep/";
        Path manualGoldPath = Paths.get(manualGoldFolder + "gold_ES_deep_SIMON.conll");
        
        String goldsFolder = "/home/lpmayos/code/parsingEval/src/main/resources/golds_test/es/";
        ArrayList<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(goldsFolder + "UD-Deep_ES-A1.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_ES-A1INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_ES-A2.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_ES-A2INV.conll"));
        candidates.add(Paths.get(goldsFolder + "UD-Deep_ES-AM.conll"));
                
        String id0Str = "id1="; 

        PrintWriter writer = new PrintWriter(goldsFolder + "results.txt", "UTF-8");
        
        String name = manualGoldPath.toString().split(manualGoldFolder)[1];
        writer.println("Spanish: deep_recall_las of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_las = results.get("recall_las");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_las.toString() + "\t\t" + name);
        }

        writer.println("\nSpanish: deep_recall_uas of the diferent gold files compared with " + name);
        writer.println("--------------------------------------------------------------------------------------------------------");

        for (Path candidateGoldPath : candidates) {
            Evaluation e = new Evaluation(manualGoldPath, candidateGoldPath, id0Str);
            Boolean printResults = false;
            Map<String, Double> results = e.nodeLabelAndAttachment(printResults);
            Double deep_recall_uas = results.get("recall_uas");
            
            name = candidateGoldPath.toString().split(goldsFolder)[1];
            writer.println(deep_recall_uas.toString() + "\t\t" + name);
        }
        
        writer.close();
    }
    
}
