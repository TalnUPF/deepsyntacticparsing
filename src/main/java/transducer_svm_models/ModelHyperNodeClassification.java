/**
 *
 */
package transducer_svm_models;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import treeTransducer.CoNLLHash;

/**
 * @author Miguel Ballesteros
 *         * @author Miguel Ballesteros
 *         Universitat Pompeu Fabra.
 */
public class ModelHyperNodeClassification
{
	private int featureCodeCounter = 0;
	private Map<String, String> featureTranslation = new HashMap<>();
	private final StringWriter writer = new StringWriter();

	// Used for training
	public ModelHyperNodeClassification() { }
	// Used during testing
	public ModelHyperNodeClassification(Map<String, String> inTranslations)
	{
		featureTranslation.putAll(inTranslations);
		featureCodeCounter = featureTranslation.size();
	}

	public String getSVMProblem()
	{
		return writer.toString();
	}

	private String getCodeForFeature(String inFeature)
	{
		if (featureTranslation.containsKey(inFeature))
			return featureTranslation.get(inFeature);
		else
		{
			String code = ++featureCodeCounter + ":1";
			featureTranslation.put(inFeature, code);
			return code;
		}
	}

	public void addLineTrain(String form, String lemma, String pos, String feats, String deprel, String surfaceId, CoNLLHash surfaceSentence, boolean inHyperNode)
	{
		String line = "";
		if (inHyperNode)
			line += "+1";
		else
			line += "-1";

		addLine(line, form, lemma, pos, feats, deprel, surfaceId, surfaceSentence);
	}

	public void addLineTest(String form, String lemma, String pos, String feats, String deprel, String surfaceId, CoNLLHash surfaceSentence)
	{
		String line = "1";
		addLine(line, form, lemma, pos, feats, deprel, surfaceId, surfaceSentence);
	}


	private void addLine(String line, String form, String lemma, String pos, String feats, String deprel, String surfaceId, CoNLLHash surfaceSentence)
	{
		//////////////////////////////////////
		//LOCAL FEATURES/////////////////////
		////////////////////////////////////

		//line += " " + getCodeForFeature("form=" + form);
		line += " " + getCodeForFeature("lemma=" + lemma);
		//line += " " + getCodeForFeature("pos=" + pos);
		line += " " + getCodeForFeature("dep=" + deprel);
		//line += " " + getCodeForFeature("feats=" + feats);

		StringTokenizer st = new StringTokenizer(feats);
		while (st.hasMoreTokens())
		{
			String s = st.nextToken("|");
			if (s.contains("spos"))
			{
				line += " " + getCodeForFeature(s);
			}
		}

		///////////////////////////////////////
		//GOVERNOR FEATURES///////////////////
		/////////////////////////////////////

		String head = surfaceSentence.getHead(surfaceId);
		//deprel of the governor.
		if (head.equals("0"))
		{
//			line += " " + getCodeForFeature("govdep=" + "ROOT");
		}
		else
		{
//			String govDeprel = surfaceSentence.getDeprel(head);
//			line += " " + getCodeForFeature("govdep=" + govDeprel);
			String govFeats = surfaceSentence.getFEAT(head);
			StringTokenizer st2 = new StringTokenizer(govFeats);

			while (st2.hasMoreTokens())
			{
				String s = st2.nextToken("|");
				if (s.contains("spos"))
				{
					line += " " + getCodeForFeature("sposhead=" + s);
				}
			}
		}

		writer.append(line + "\n");
	}

	public String getTranslations()
	{
		String translations = "";
		for (String feature : this.featureTranslation.keySet())
		{
			translations += feature + "=" + this.featureTranslation.get(feature);
		}
		if (!featureTranslation.isEmpty())
			translations = translations.substring(0, translations.length()-1); // remove last comma
		return translations + "\n";
	}
}
