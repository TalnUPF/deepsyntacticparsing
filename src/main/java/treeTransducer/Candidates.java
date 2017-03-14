/**
 *
 */
package treeTransducer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * @author Miguel Ballesteros
 *         <p/>
 *         Universitat Pompeu Fabra
 */
public class Candidates
{
	private final String patterns;
	private List<Map<String, List<String>>> candidates;
	//conflictive nodes from surface and possible candidates in deep. Keys: surface nodes. Values: new deep ids.
	private List<Map<String, List<String>>> candidatesPatterns;
	//the patterns of the candidates shown above. One by one, in the same order in the arraylist which is the value of the hash.
	private List<Map<String, List<String>>> candidatesPaths;
	private List<Map<String, List<String>>> selectedCandidates;
	private List<Map<String, List<String>>> selectedSiblingCandidates;

	//Example
	//{24=[1, 17, 18]}
	//{24=[ROOT+subj, ROOT+analyt_perf+analyt_pass, ROOT+adv]}
	//1 corresponds to "ROOT+subj"
	//17 corresponds to ROOT+analyt_perf+analyt_pass
	//18 corresponds to ROOT+adv

	public List<Map<String, List<String>>> getSelectedSiblingCandidates()
	{
		return selectedSiblingCandidates;
	}

	public void setSelectedSiblingCandidates(List<Map<String, List<String>>> selectedSiblingCandidates)
	{
		this.selectedSiblingCandidates = selectedSiblingCandidates;
	}

	public Candidates(String inPatterns)
	{
		patterns = inPatterns;
		candidates = new ArrayList<>();
		candidatesPatterns = new ArrayList<>();
		selectedCandidates = new ArrayList<>();
		selectedSiblingCandidates = new ArrayList<>();
		candidatesPaths = new ArrayList<>();
	}

	public List<Map<String, List<String>>> getCandidates()
	{
		return candidates;
	}

	public List<Map<String, List<String>>> getCandidatesPatterns()
	{
		return candidatesPatterns;
	}

	public List<Map<String, List<String>>> getSelectedCandidates()
	{
		return selectedCandidates;
	}

	public void calculateCandidates(List<CoNLLHash> deepPartialTreebank, List<CoNLLHash> surfaceTreebank)
	{
		for (int i = 0; i < deepPartialTreebank.size(); i++)
		{
			Map<String, List<String>> candSentence = new HashMap<>();
			Map<String, List<String>> candSentencePattern = new HashMap<>();
			Map<String, List<String>> candSentencePaths = new HashMap<>();
			CoNLLHash sentence = deepPartialTreebank.get(i);
			ArrayList<String> ids = sentence.getIds();
			Iterator<String> itIds = ids.iterator();
			boolean anyConflict = false;

			while (itIds.hasNext())
			{
				String id = itIds.next();
				String head = sentence.getHead(id);
				String deprel = sentence.getDeprel(id);
				String candidate = getDeepCandidate(head);
				String pattern = getPattern(deprel, head, i, surfaceTreebank);

				if (candidate != null)
				{
					anyConflict = true;
					List<String> spCandidates = candSentence.get(candidate);
					List<String> spPatterns = candSentencePattern.get(candidate);
					List<String> spPaths = candSentencePaths.get(candidate);
					if (spCandidates == null)
					{
						spCandidates = new ArrayList<>();
						spPatterns = new ArrayList<>();
						spPaths = new ArrayList<>();
						spCandidates.add(id);
						spPatterns.add(pattern);
						spPaths.add(head);
						candSentence.put(candidate, spCandidates);
						candSentencePattern.put(candidate, spPatterns);
						candSentencePaths.put(candidate, spPaths);
					}
					else
					{
						spCandidates.add(id);
						spPatterns.add(pattern);
						spPaths.add(head);
						candSentence.remove(candidate);
						candSentence.put(candidate, spCandidates);
						candSentencePattern.remove(candidate);
						candSentencePattern.put(candidate, spPatterns);
						candSentencePaths.remove(candidate);
						candSentencePaths.put(candidate, spPaths);
					}
				}
			}
			if (!anyConflict)
			{
				candidates.add(null);
				candidatesPatterns.add(null);//add a null hashmap, because there are no conflicts.
				candidatesPaths.add(null);
			}
			else
			{
				candidates.add(candSentence);
				candidatesPatterns.add(candSentencePattern);
				candidatesPaths.add(candSentencePaths);
			}
		}
	}

	//Auxiliar function that returns the "possible" corresponding surface ID. That is, the first node in the list in the head column (when there is a conflict) from deep_2_1.conll
	public String getDeepCandidate(String head)
	{
		if (head.contains("_["))
		{
			int delim = 0;
			for (int i = 0; i < head.length(); i++)
			{
				if (head.charAt(i) == '[')
				{
					delim = i;
				}
			}
			String headPath = head.substring(delim + 1, head.length());
			StringTokenizer st2 = new StringTokenizer(headPath);

			ArrayList<String> listCand = new ArrayList<>();
			String cand;
			while (st2.hasMoreTokens())
			{
				cand = st2.nextToken("_");
				listCand.add(cand);

			}
			for (int i = listCand.size() - 1; i >= 0; i--)
			{
				if (!listCand.get(i).equals("0"))
				{
					return listCand.get(i);
				}
			}
		}

		return null;
	}

	public String getPattern(String deprel, String head, int sentenceCounter, List<CoNLLHash> testSetHash)
	{
		String pattern = "";
		if (head.contains("_["))
		{
			int delim = 0;
			for (int i = 0; i < head.length(); i++)
			{
				if (head.charAt(i) == '[')
				{
					delim = i;
				}
			}
			String headPath = head.substring(delim + 1, head.length());
			StringTokenizer stok = new StringTokenizer(headPath);
			while (stok.hasMoreTokens())
			{
				String next = stok.nextToken("_");
				if (stok.hasMoreTokens())
				{ /////// (last node no)
					String deprelNext = TransducerSurfToDeep.findDeprelGovernor(sentenceCounter, next, testSetHash);
					if (!(deprelNext.equals("ROOT") && pattern.contains("ROOT")))
					{
						pattern = deprelNext + "+" + pattern;
					}
				}
			}

			pattern = pattern + deprel;

		}

		return pattern;
	}

	public String getSetOfNodes(String head, int sentenceCounter, List<CoNLLHash> testSetHash)
	{
		String pattern = "";
		if (head.contains("_["))
		{
			int delim = 0;
			for (int i = 0; i < head.length(); i++)
			{
				if (head.charAt(i) == '[')
				{
					delim = i;
				}
			}
			String headPath = head.substring(delim + 1, head.length());
			StringTokenizer stok = new StringTokenizer(headPath);
			while (stok.hasMoreTokens())
			{
				String next = stok.nextToken("_");
				String deprelNext = TransducerSurfToDeep.findDeprelGovernor(sentenceCounter, next, testSetHash);
				if (!(deprelNext.equals("ROOT") && pattern.contains("ROOT")))
				{
					pattern = next + "+" + pattern;
				}
			}

			pattern = pattern + head;
		}

		return pattern;
	}

	/**
	 * This method check the patterns inferred in the training process and selects the best candidates.
	 */
	public void selectCandidates() throws IOException
	{
		for (int i = 0; i < this.candidates.size(); i++)
		{
			Map<String, List<String>> candSentence = candidates.get(i);
			Map<String, List<String>> candSentencePattern = candidatesPatterns.get(i);
			Map<String, List<String>> candSentencePaths = candidatesPaths.get(i);
			Map<String, List<String>> candidateSelected = new HashMap<>();
			Map<String, List<String>> noConflictsSelected = new HashMap<>();
			if (candSentence == null)
			{
				selectedCandidates.add(null);
				selectedSiblingCandidates.add(null);
			}
			else
			{
				Set<String> surfaceIds = candSentence.keySet();
				for (String surfId : surfaceIds)
				{
					List<String> candidatesId = candSentence.get(surfId);
					List<String> candidatesIdPattern = candSentencePattern.get(surfId);
					List<String> candidatesIdPaths = candSentencePaths.get(surfId);

					//check whether there is conflict or not
					List<String> noConflictiveNodes = checkConflicts(candidatesIdPaths, candidatesId);
					noConflictsSelected.put(surfId, noConflictiveNodes);
					if (candidatesId.size() == 1)
					{
						ArrayList<String> aux = new ArrayList<>();
						aux.add(candidatesId.get(0));
						candidateSelected.put(surfId, aux); //there is no need to check the patterns
						noConflictsSelected.put(surfId, aux);
					}
					else
					{
						//CHECK THE PATTERNS FILE
						if (noConflictiveNodes.containsAll(candidatesId))
						{
							List<String> aux = new ArrayList<>();
							aux.addAll(noConflictiveNodes);
							noConflictsSelected.put(surfId, aux);
						}
						else
						{
							String selected = checkPatterns(candidatesId, candidatesIdPattern, noConflictiveNodes);
							if (!selected.isEmpty())
							{
								ArrayList<String> aux = new ArrayList<>();
								aux.add(selected);
								candidateSelected.put(surfId, aux);
							}
							else
							{
								selected = checkPatternsApproximately(candidatesId, candidatesIdPattern, noConflictiveNodes);
								ArrayList<String> aux = new ArrayList<>();
								aux.add(selected);
								candidateSelected.put(surfId, aux);
							}
							//PARCHE para caso MUY extraño: este caso era cuando hay 2 que si y 1 que no.
							//[12_[20_19, 12_[29_19, 12_[29_19] (en el development set)
							//Si se descomenta los system.out de algunas líneas arriba se ve.
							if (selected.isEmpty())
							{
								selected = candidatesId.get(0);
								ArrayList<String> aux = new ArrayList<>();
								aux.add(selected);
								candidateSelected.put(surfId, aux);
							}
						}
					}
				}
				selectedCandidates.add(candidateSelected);
				selectedSiblingCandidates.add(noConflictsSelected);
			}
		}
	}

	private List<String> checkConflicts(List<String> paths, List<String> candidatesId)
	{
		// TODO Auto-generated method stub
		List<String> noConflicts = new ArrayList<>();

		boolean conflict = false;
		for (int i = 0; i < paths.size(); i++)
		{
			for (int j = 0; j < paths.size(); j++)
			{
				if (i != j)
				{
					if (paths.get(i).equals(paths.get(j)))
					{
						conflict = true;
					}
					if (paths.get(i).charAt(0) == '0')
					{
						conflict = true;
					}
				}
			}
			if (!conflict)
			{
				noConflicts.add(candidatesId.get(i));
				conflict = false;
			}
		}

		return noConflicts;
	}

	private String checkPatterns(List<String> candidatesId, List<String> candidatesIdPattern, List<String> noConflictiveNodes) throws IOException
	{
		// TODO Auto-generated method stub

		String best = "";
		Integer bestFreqPattern = 0;
		for (int i = 0; i < candidatesId.size(); i++)
		{
			String id = candidatesId.get(i);
			if (!noConflictiveNodes.contains(id))
			{
				String pattern = candidatesIdPattern.get(i);

				Integer freq = findPattern(pattern);
				if (freq > bestFreqPattern)
				{
					best = id;
					bestFreqPattern = freq;
				}
			}
		}
		return best;
	}

	private String checkPatternsApproximately(List<String> candidatesId, List<String> candidatesIdPattern, List<String> noConflictiveNodes) throws IOException
	{
		String best = "";
		Integer bestFreqPattern = 0;
		for (int i = 0; i < candidatesId.size(); i++)
		{
			String id = candidatesId.get(i);
			if (!noConflictiveNodes.contains(id))
			{
				String pattern = candidatesIdPattern.get(i);

				if (i == 0)
				{
					best = id;
				}

				Integer freq = findApproximatePattern(pattern);
				if (freq > bestFreqPattern)
				{
					best = id;
					bestFreqPattern = freq;
				}
			}
		}
		return best;
	}

	private Integer findPattern(String pattern) throws IOException
	{
		Integer freq = 0;
		BufferedReader br = new BufferedReader(new StringReader(patterns));
		String line = br.readLine();

		while (line != null)
		{
			if (!line.isEmpty())
			{
				String[] splittedLine = line.split("\t");
				if (pattern.equals(splittedLine[0]))
				{
					freq = Integer.parseInt(splittedLine[1]);
					return freq;
				}
			}
			line = br.readLine();
		}

		return freq;
	}

	private Integer findApproximatePattern(String pattern) throws IOException
	{
		Integer freq = 0;
		StringTokenizer st = new StringTokenizer(pattern);
		String regex = st.nextToken("+");
		String subPattern = pattern.substring(regex.length() + 1);
		BufferedReader br = new BufferedReader(new StringReader(patterns));
		String line = br.readLine();

		while (line != null)
		{
			if (!line.isEmpty())
			{
				String[] splittedLine = line.split("\t");
				if (subPattern.contains(splittedLine[0]))
				{ //FIND SUBPATTERNS
					freq = Integer.parseInt(splittedLine[1]);
					return freq;
				}
			}

			line = br.readLine();
		}

		if (subPattern.contains("+"))
		{
			return findApproximatePattern(subPattern);
		}
		return freq;
	}

	public String getCalculatedHead(String head)
	{
		String headOutput;
		StringTokenizer st = new StringTokenizer(head);
		if (st.hasMoreTokens())
		{
			headOutput = st.nextToken("_");
			return headOutput;
		}
		return null;
	}
}


