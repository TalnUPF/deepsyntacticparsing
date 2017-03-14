/**
 *
 */
package transducer_svm_models;

import java.io.StringWriter;
import java.util.*;

import treeTransducer.CoNLLHash;

/**
 * * @author Miguel Ballesteros
 * Universitat Pompeu Fabra.
 */
public class ModelLabellingClassification
{
	private int featureCodeCounter = 0;
	private int deepLabelCodeCounter = 0;
	private final Map<String, String> featureTranslation = new HashMap<>();
	private final Map<String, String> deprelTranslation = new HashMap<>();
	private final StringWriter writer = new StringWriter();

	// Used for training
	public ModelLabellingClassification() { }
	// Used during testing
	public ModelLabellingClassification(Map<String, String> inFeatureTranslations, Map<String, String> inDepRelTranslations)
	{
		featureTranslation.putAll(inFeatureTranslations);
		featureCodeCounter = featureTranslation.size();
		deprelTranslation.putAll(inDepRelTranslations);
		deepLabelCodeCounter = deprelTranslation.size();
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

	private String getCodeForDeepLabel(String inDeepLabel)
	{
		if (deprelTranslation.containsKey(inDeepLabel))
			return deprelTranslation.get(inDeepLabel);
		else
		{
			String code = Integer.toString(++deepLabelCodeCounter);
			deprelTranslation.put(inDeepLabel, code);
			return code;
		}
	}

	public String getDeepLabelForCode(String inFeatureCode)
	{
		for (String label : deprelTranslation.keySet())
		{
			if (deprelTranslation.get(label).equals(inFeatureCode))
				return label;
		}

		return null;
	}

	public void addLineTrain(String form, String lemma, String pos, String feats, String deprel, String deepDeprel, String surfaceId, CoNLLHash surfaceSentence, boolean hypernode)
	{
		if (deepDeprel.equals("ROOT"))
			return;

		String line = getCodeForDeepLabel(deepDeprel) + " " + getCodeForFeature("dep=" + deprel);

					/*this.addNewFeature("pos="+pos);
					line += " " + getCodeForFeature("pos=" + pos);*/

					/*StringTokenizer st = new StringTokenizer(feats);
					while(st.hasMoreTokens()) {
						String s = st.nextToken("|");
						if (s.contains("spos")) {
							line += " " + getCodeForFeature(s);
						}
					}*/

		line += " " + getCodeForFeature("lemma=" + lemma);

		String head = surfaceSentence.getHead(surfaceId);
		//list of deprels of siblings.
		ArrayList<String> siblings = surfaceSentence.getSiblings(head);
		Iterator<String> itSib = siblings.iterator();

		while (itSib.hasNext())
		{
			String sib = itSib.next();
			if (!sib.equals(surfaceId))
			{
				String sibDeprel = surfaceSentence.getDeprel(sib);

				line += " " + getCodeForFeature("sibDeprel=" + sibDeprel);
			}
		}

		if (head.equals("0"))
			line += " " + getCodeForFeature("govDeprel=" + "ROOT");
		else
		{
			String govDeprel = surfaceSentence.getDeprel(head);
			line += " " + getCodeForFeature("govDeprel=" + govDeprel);
		}

		writer.append(line + "\n");
	}

	public void addLineTest(String form, String lemma, String pos, String feats, String deprel, String deepDeprel, String surfaceId, CoNLLHash surfaceSentence, boolean hypernode)
	{
		String line = "1";//Esto solo para testing. Se ponen las etiquetas para su clasificación, todo 1s, se hará la clasificación luego.
		line += " " + getCodeForFeature("dep=" + deprel);
		line += " " + getCodeForFeature("lemma=" + lemma);

		String head = surfaceSentence.getHead(surfaceId);

		//list of deprels of siblings.
		ArrayList<String> siblings = surfaceSentence.getSiblings(head);
		Iterator<String> itSib = siblings.iterator();

		while (itSib.hasNext())
		{
			String sib = itSib.next();
			if (!sib.equals(surfaceId))
			{
				String sibDeprel = surfaceSentence.getDeprel(sib);
				line += " " + getCodeForFeature("sibDeprel=" + sibDeprel);
			}
		}

		if (head.equals("0"))
		{
			line += " " + getCodeForFeature("govDeprel=" + "ROOT");
		}
		else
		{
			String govDeprel = surfaceSentence.getDeprel(head); //...
			line += " " + getCodeForFeature("govDeprel=" + govDeprel);
			//String govFeats = surfaceSentence.getFEAT(head);
		}

		writer.append(line + "\n");
	}

	public String toString()
	{
		return this.writer.toString();
	}

	public String getTranslations()
	{
		String translations = "";
		for (String feature : this.featureTranslation.keySet())
		{
			translations += feature + "=" + this.featureTranslation.get(feature) + ",";
		}
		if (!featureTranslation.isEmpty())
			translations = translations.substring(0, translations.length()-1); // remove last comma

		translations += "\n";
		for (String depRel : this.deprelTranslation.keySet())
		{
			translations += depRel + "=" + this.deprelTranslation.get(depRel) + ",";
		}
		if (!deprelTranslation.isEmpty())
			translations = translations.substring(0, translations.length()-1); // remove last comma

		return translations + "\n";
	}
}
