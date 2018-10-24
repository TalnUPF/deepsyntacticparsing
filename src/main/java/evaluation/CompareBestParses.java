/**
 *
 */
package evaluation;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author Miguel Ballesteros Universitat Pompeu Fabra
 *
 */
public class CompareBestParses {

    public static void main(String[] args) throws IOException {

        String id0Str = "id1=";

        ArrayList<String> systems = new ArrayList<String>() {{
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/C2L2_2017-05-12-09-27-46.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/CLCL_2017-05-15-12-58-58.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/CLCL2_2017-05-15-17-32-57.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/conll17-baseline_2017-05-09-16-57-43.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/darc_2017-05-14-23-40-19.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/IIT-Kharagpur_2017-05-13-18-49-21.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/IMS_2017-05-10-14-37-31.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/Koc-University_2017-05-11-18-53-35.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/LATTICE_2017-05-15-11-18-08.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/LIMSI-LIPN_2017-05-12-15-59-15.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/LyS-FASTPARSE_2017-05-11-14-05-14.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/Mengest_2017-05-27-12-15-27.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/METU_2017-05-13-13-51-31.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/MQuni_2017-05-09-20-35-48.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/naistCL_2017-05-14-05-33-50.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/NAIST-SATO_2017-05-14-15-56-56.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/OpenU-NLP-Lab_2017-05-15-10-14-12.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/Orange-Deskin_2017-05-10-12-02-22.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/ParisNLP_2017-05-11-02-24-27.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/RACAI_2017-05-18-16-05-12.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/Stanford_2017-05-14-10-36-20.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/TurkuNLP_2017-05-14-02-33-45.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/UALING_2017-05-16-12-04-27.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/UParse_2017-05-13-02-21-38.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/Wenba-NLU_2017-05-20-15-43-16.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/gold.conll");
//            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/gold_manual.conll");
        }};
        
        Double[][] similarities = new Double[systems.size()][systems.size()];
        
        int i = 0;
        for (String system1_path : systems) {
            int j = 0;
            for (String system2_path : systems) {
                if (j > i) {
                    Evaluation e = new Evaluation(Paths.get(system1_path), Paths.get(system2_path), id0Str);
                    Map<String, Double> aux1 = e.HyperNodeAccuracy();
                    Map<String, Double> aux2 = e.nodeLabelAndAttachment();

                    similarities[i][j] = aux1.get("f1");
                    similarities[j][i] = aux1.get("f1");
                }
                j++;
            }
            i++;
        }
        
        for(i=0; i<similarities.length; i++){
            for(int j=0; j<similarities[0].length; j++){
                System.out.print(String.format("%20s", similarities[i][j]));
            }
            System.out.println("");
        }
    }
}
