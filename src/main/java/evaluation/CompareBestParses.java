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
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_C2L2_2017-05-12-09-27-46_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_CLCL_2017-05-15-12-58-58_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_CLCL2_2017-05-15-17-32-57_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_conll17-baseline_2017-05-09-16-57-43_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_darc_2017-05-14-23-40-19_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_IIT-Kharagpur_2017-05-13-18-49-21_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_IMS_2017-05-10-14-37-31_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_Koc-University_2017-05-11-18-53-35_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_LATTICE_2017-05-15-11-18-08_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_LIMSI-LIPN_2017-05-12-15-59-15_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_LyS-FASTPARSE_2017-05-11-14-05-14_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_Mengest_2017-05-27-12-15-27_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_METU_2017-05-13-13-51-31_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_MQuni_2017-05-09-20-35-48_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_naistCL_2017-05-14-05-33-50_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_NAIST-SATO_2017-05-14-15-56-56_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_OpenU-NLP-Lab_2017-05-15-10-14-12_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_Orange-Deskin_2017-05-10-12-02-22_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_ParisNLP_2017-05-11-02-24-27_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_RACAI_2017-05-18-16-05-12_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_Stanford_2017-05-14-10-36-20_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_TurkuNLP_2017-05-14-02-33-45_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_UALING_2017-05-16-12-04-27_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_UParse_2017-05-13-02-21-38_output_en.conll_out.conll");
            add("/home/lpmayos/code/parsingEval/src/main/resources/datasets_deep/EN_Deep/candidate_conll2017-test-runs-v3_conll17-ud-test-2017-05-09_Wenba-NLU_2017-05-20-15-43-16_output_en.conll_out.conll");
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
