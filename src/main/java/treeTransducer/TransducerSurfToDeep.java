package treeTransducer;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import libsvm.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import svm_utils.svm_predict;
import svm_utils.svm_train;
import transducer_svm_models.ModelHyperNodeClassification;
import transducer_svm_models.ModelLabellingClassification;

/**
 * @author Miguel Ballesteros
 *         Universitat Pompeu Fabra.
 *         A tree transducer: Transforming from Surface Syntax to Deep Syntax Automatically.
 *         which may be called as a grammar induction system.
 *         <p/>
 *         It is similar to a graph-based parsing strategy.
 *         In which the arcs of the trees are scored to be removed or to be added.
 *         <p/>
 *         The process is the following: the system has the original Surface tree in which all the arcs are highly scored.
 *         Then, the system adds new arcs that are susceptible of remain as permanents in the Syntax tree, however,
 *         they are initially scored differently.
 *         <p/>
 *         TODO Keep defining the system.
 *         <p/>
 *         <p/>
 *         <p/>
 *         <p/>
 *         What if instead of defining a tree transducer, I basically define a tagset transformation
 *         -> Three possible cases.
 *         * Direct transformation of label:
 *         such as.  aux_refl	-> INVERT-I/II|ADD-SE-LEMMA|II+ADD-COREF-I|III+ADD-COREF-I
 *         *  Remove a node:
 *         label --> NULL. Then, all the nodes with label NULL will be removed in postprocessing
 *         *  Add a node (separate in two or more nodes)
 *         refrescarse -- > refrescar | se (two or more nodes)
 *         (it will basically provide a label that is the two new labels... then just separate it in postprocessing)
 *         <p/>
 *         In this way, there is only one thing to solve: a classifier that provides these transformations.
 *         <p/>
 *         <p/>
 *         En el fondo esto se puede definir de la siguiente manera:
 *         <p/>
 *         Transformaciones
 *         nodo --> nodo.
 *         hipernodo --> nodo.
 *         nodo --> hipernodo.
 *         <p/>
 *         Lo que nos lleva a tener:
 *         hipernodo-->hipernodo.
 *         <p/>
 *         Definiendo todo como hipernodo, tanto los nodos, como los hipernodos per se.
 *         <p/>
 *         <p/>
 *         Problema: ¿cómo detectar si un nodo es ya un hiper nodo o o es un nodo que debe integrarse como nodo?, se me ocurre que se deben usar dos libsvms.
 *         <p/>
 *         Esa información está en el training set. Se encuentra en los nodos dsynt
 *         1. En Los id0---idn indican a los nodos que corresponden en el ssynt.
 *         Y además la información está en el lemma. Que encaja directamente con algunos de los idI.
 *         <p/>
 *         El problema es que en tiempo de test también se haga. Sólo se tiene la información de los test sets que es un ssynt.
 *         Hay que incorporar un libsvm que sea capaz de detectar si un nodo es o no es ya "final".
 *         Para eso hay que detectarlo en el training set previamente.
 *         <p/>
 *         y el sistema ML que hace eso se puede testear, (con el corpus de test) con lo cual está bien. Y creo que se puede hacer en menos tiempo.
 *         <p/>
 *         Segunda parte del problema:
 *         <p/>
 *         2. Libsvms que transforma los hipernodos en los hipernodos (o nodos de salida).
 *         Es una clasificación más complicada pero se puede hacer con las transformaciones que se ven arriba, y centrándose en las etiquetas.
 *         Es decir:
 *         det subj (+ features) --> I
 *         etc.
 *         <p/>
 *         <p/>
 *         Esto lo convierte en 2 sistemas ML juntos: 2 libsvms, basicamente:
 *         1- libsvm que clasifica si un nodo es o no es final, y cuando lo clasifica lo hace de esa manera.
 *         2- libsvm que dado un hiper nodo genera el hiper nodo de salida. Su etiquetado vaya.
 *         <p/>
 *         <p/>
 *         A libsvm for each sentence, or a libsvm for each data set?
 */
public class TransducerSurfToDeep
{
	private final svm_model nodeModel;
	private final svm_model labelModel;
	private final Candidates candidates;
	private final Map<String, String> nodeFeatureTranslations;
	private final Map<String, String> labellingFeatureTranslations;
	private final Map<String, String> deepLabelTranslations;
	private static final Path nodeModelFilename = Paths.get("node_train.model");
	private static final Path labelModelFilename = Paths.get("labelling_train.model");
	private static final Path patternsFilename = Paths.get("pathPatterns.txt");
	private static final Path translationsFilename = Paths.get("svm_translations.txt");

	public static TransducerSurfToDeep getTrainTransducer()
	{
		return new TransducerSurfToDeep();
	}

	public static TransducerSurfToDeep getTestTransducer(Path inResourcesFolder) throws URISyntaxException, IOException
	{
		System.out.println("Setting up new test transducer");
		StopWatch timer = new StopWatch();
		timer.start();

		Path nodeModelFile = inResourcesFolder.resolve(nodeModelFilename);
		Path labelModelFile = inResourcesFolder.resolve(labelModelFilename);
		Path patternsFile = inResourcesFolder.resolve(patternsFilename);
		Path translationsFile = inResourcesFolder.resolve(translationsFilename);

		svm.svm_set_print_string_function(new svm_print_interface() { public void print(String s) {}}); // -q
		System.out.println("Loading hypernode detection model from " + nodeModelFile);
		svm_model nodeModel = svm.svm_load_model(nodeModelFile.toString());
		if(svm.svm_check_probability_model(nodeModel) != 0)
			System.out.println("Hypernode detection model supports probability estimates, but disabled in prediction.\n");

		System.out.println("Loading labelling model from " + labelModelFile);
		svm_model labelModel = svm.svm_load_model(labelModelFile.toString()); // ssynt_labelling_svm_test.svm
		if(svm.svm_check_probability_model(labelModel) != 0)
			System.out.println("Labelling model supports probability estimates, but disabled in prediction.\n");

		System.out.println("Loading patterns from " + patternsFile);
		String patterns = new String(Files.readAllBytes(patternsFile));
		Candidates candidates = new Candidates(patterns);

		System.out.println("Reading translations from " + translationsFile);
		String translationsStr = new String(Files.readAllBytes(translationsFile));
		List<Map<String, String>> translations = new ArrayList<>();
		for (String l : translationsStr.split("\n"))
		{
			Map<String, String> dict = new HashMap<>();
			for (String e : l.split(","))
			{
				String label = e.substring(0, e.lastIndexOf('='));
				String code = e.substring(e.lastIndexOf('=') + 1, e.length());
				dict.put(label, code);
			}

			translations.add(dict);
		}

		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		return new TransducerSurfToDeep(nodeModel, labelModel, candidates, translations);
	}

	private TransducerSurfToDeep()
	{
		nodeModel = null;
		labelModel = null;
		candidates = null;
		nodeFeatureTranslations = null;
		labellingFeatureTranslations = null;
		deepLabelTranslations = null;

	}

	private TransducerSurfToDeep(svm_model inHypernodeModel, svm_model inLabellingModel, Candidates inCandidates, List<Map<String, String>> inTranslations)
	{
		nodeModel = inHypernodeModel;
		labelModel = inLabellingModel;
		candidates = inCandidates;
		nodeFeatureTranslations = inTranslations.get(0);
		labellingFeatureTranslations = inTranslations.get(1);
		deepLabelTranslations = inTranslations.get(2);
	}

	/**
	 * Takes as input two files containing training surface and deep treebanks, and produces a patterns file, two
	 * svm model files, one for hypernode detection and another one for labeling, and a file containing translations of
	 * svm symbols to syntactic labels.
	 * @param pathSurface path to file containing ssynt treebank in ConLL format
	 * @param pathDeep path to file containing dsynt treebank in ConLL forma
	 * @throws Exception
	 */
	public void train(Path pathSurface, Path pathDeep, Path outputFolder) throws Exception
	{
		System.out.println("Training process started");

		System.out.println("Reading surface syntax treebank from " + pathSurface);
		StopWatch timer = new StopWatch();
		timer.start();
		BufferedReader ssyntConllReader = new BufferedReader(new FileReader(pathSurface.toString()));
		ArrayList<CoNLLHash> surfaceTreebank = CoNLLTreeConstructor.loadTreebank(ssyntConllReader);
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Reading deep syntax treebank from " + pathDeep);
		BufferedReader dsyntConllReader = new BufferedReader(new FileReader(pathDeep.toString()));
		ArrayList<CoNLLHash> deepTreebank = CoNLLTreeConstructor.loadTreebank(dsyntConllReader);
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Detecting hyper-nodes and preparing libsvm model... ");
		String patterns = storingPatternsTrain(surfaceTreebank, deepTreebank);
		Path patternsPath = outputFolder.resolve(patternsFilename);
		Files.write(patternsPath, patterns.getBytes());
		System.out.println("Patterns saved in " + patternsPath);
		Pair<ModelHyperNodeClassification, ModelLabellingClassification> svmProblems = detectHyperNodesTrain(surfaceTreebank, deepTreebank);
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Training LibSvm model for hypernode detection... (may take a while) ");
//		String[] args = new String[28];
//		args[0] = "-s";
//		args[1] = "0";
//		args[2] = "-t";
//		args[3] = "1";
//		args[4] = "-d";
//		args[5] = "2";
//		args[6] = "-g";
//		args[7] = "0.2";
//		args[8] = "-r";
//		args[9] = "0.0";
//		args[10] = "-n";
//		args[11] = "0.5";
//		args[12] = "-n";
//		args[13] = "0.5";
//		args[14] = "-m";
//		args[15] = "100";
//		args[16] = "-c";
//		args[17] = "1100.0";
//		args[18] = "-e";
//		args[19] = "1.0";
//		args[20] = "-p";
//		args[21] = "0.1";
//		args[22] = "-h";
//		args[23] = "1";
//		args[24] = "-b";
//		args[25] = "0";
//		args[26] = "-q";
//		args[27] = ssyntSVM;
//		svm_train.main(args);

		// Set up params
		svm_parameter params = new svm_parameter(); // sets default values
		params.svm_type = svm_parameter.C_SVC; // -s 0
		params.kernel_type = svm_parameter.POLY; // -t 1
		params.degree = 2; // -d 2
		params.gamma = 0.2;	// -g 0.2
		params.coef0 = 0.0; // -r 0.0
		params.nu = 0.5; // -n 0.5
		params.cache_size = 100; // -m 100
		params.C = 1100.0; //  -c 1100.0
		params.eps = 1.0; // -e 1.0
		params.p = 0.1; // -p 0.1
		params.shrinking = 1; // -h 1
		params.probability = 0; // -b 0
		params.nr_weight = 0; // no '-w' arg
		params.weight_label = new int[0];
		params.weight = new double[0];
		params.cross_validation = false; // no '-v' arg
		svm.svm_set_print_string_function(new svm_print_interface() { public void print(String s) { System.out.println(s);}}); // -q

		// Read training data and check for errors
		BufferedReader reader = new BufferedReader(new StringReader(svmProblems.getLeft().getSVMProblem()));
		svm_problem prob = svm_train.read_problem(reader, params);
		String error = svm.svm_check_parameter(prob, params);
		if (error != null)
			throw new Exception("Error setting up hypernode svm: " + error);

		// Train and save model to file
		svm_model model = svm.svm_train(prob, params);
		Path nodeModelPath = outputFolder.resolve(nodeModelFilename);
		svm.svm_save_model(nodeModelPath.toString(), model);
		System.out.println("Model saved in " + nodeModelPath);
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Training LibSvm for labelling classification ... ");
//		args[2] = "-t";
//		args[3] = "0";
//		args[4] = "-d";
//		args[5] = "1";
//		args[27] = ssyntLabelSVM;

		// Set up params, same as before except for:
		params.kernel_type = svm_parameter.LINEAR; // -t 0
		params.degree = 1; // -d 1

		// Read training data and check for errors
		BufferedReader readerLabel = new BufferedReader(new StringReader(svmProblems.getRight().getSVMProblem()));
		svm_problem probLabel = svm_train.read_problem(readerLabel, params);
		String errorLabel = svm.svm_check_parameter(probLabel, params);
		if (errorLabel != null)
			throw new Exception("Error setting up labelling svm: " + errorLabel);

		// Train and save model to file
		svm_model modelLabel = svm.svm_train(probLabel, params);
		Path labelModelPath = outputFolder.resolve(labelModelFilename);
		svm.svm_save_model(labelModelPath.toString(), modelLabel);
		System.out.println("Model saved in " + labelModelPath);

		String translations = svmProblems.getLeft().getTranslations(); // first node translations
		translations += svmProblems.getRight().getTranslations(); // then labelling translations
		Path translationsPath = outputFolder.resolve(translationsFilename);
		Files.write(translationsPath, translations.getBytes());
		System.out.println("Translations saved in " + translationsPath);

		timer.split();
		System.out.println("Completed in " + timer.toSplitString());
		timer.stop();
	}

	/**
	 * Takes as input a file containing a test surface treebank and produces a deep treebank for it.
	 * Requires the four files generated by the training step.
	 * @param inPathTest path to file containing ssynt treebank in ConLL format
	 * @param inPostProcess if true, the conll output is post-processed
	 * @return predicted dsynt treebank in ConLL format
	 * @throws IOException
	 */
	public String test(Path inPathTest, boolean inPostProcess) throws IOException, URISyntaxException
	{
		System.out.println("Testing process started");
		StopWatch timer = new StopWatch();
		timer.start();

		System.out.println("Reading test surface syntax treebank from " + inPathTest);
		String testConll = new String(Files.readAllBytes(inPathTest));
		List<CoNLLHash> surfaceTestTreebank = CoNLLTreeConstructor.loadTreebank(testConll);
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Creating hypernodes svm problem from test treebank... ");
		String svmNodeProblem = detectHyperNodesTest(surfaceTestTreebank, nodeFeatureTranslations); // "ssynt_svm_test.svm"
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Testing the hypernode classifier... (may take a while)");
//		String[] args = new String[4];
//		args[1] = "ssynt_svm_test.svm";
//		args[2] = this.trainedModel;
//		args[3] = "ssynt_output.svm";
//		args[0] = "-q";
//		svm_predict.main(args);
		BufferedReader nodeReader = new BufferedReader(new StringReader(svmNodeProblem));
		String predictedNodes = svm_predict.predict(nodeReader, nodeModel, 0); // no '-b' arg

		System.out.println("Producing partial output ...  ");
		String partialOutput1 = producePartialOutputTest(testConll, predictedNodes); // dsynt_partial_output_1.conll
		String partialOutput2 = updateIdsTest(partialOutput1, surfaceTestTreebank); // dsynt_partial_output_2.1.conll
		String partialOutput3 = solveInconsistencies(partialOutput2, surfaceTestTreebank, candidates); // dsynt_partial_output_2.conll"
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Creating labelling svm problem from partial treebank... ");
		ArrayList<CoNLLHash> partialTestTreebank = CoNLLTreeConstructor.loadTreebank(partialOutput3);
		ModelLabellingClassification mdLabelClass = prepareHyperNodeLabelling(partialTestTreebank, labellingFeatureTranslations, deepLabelTranslations);
		String svmLabelProblem = mdLabelClass.toString(); // ssynt_labelling_svm_test.svm
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Testing the labeler... (may take a while)");
//		String[] args2 = new String[4];
//		args2[1] = "ssynt_labelling_svm_test.svm";
//		args2[2] = this.trainedLabellerModel;
//		args2[3] = "ssynt_output_labeller.svm";
//		args2[0] = "-q";
//		svm_predict.main(args2);
		BufferedReader labelReader = new BufferedReader(new StringReader(svmLabelProblem));
		String predictedLabels = svm_predict.predict(labelReader, labelModel, 0); // no '-b' arg, ssynt_output_labeller.svm
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());

		System.out.println("Producing final conll output...");
		String finalOutput = updateLabelsTest(partialOutput3, predictedLabels, mdLabelClass); // dsynt_final_output.conll
		if (inPostProcess)
			finalOutput = postProcessingTest(testConll, finalOutput); // dsynt_final_output_post.conll
		timer.split();
		System.out.println("Completed in " + timer.toSplitString());
		return finalOutput;
	}

	private static String storingPatternsTrain(ArrayList<CoNLLHash> surfaceTreebank, ArrayList<CoNLLHash> deepTreebank) throws Exception
	{
		Map<String, Integer> patternsFreq = new HashMap<>();

		for (int i = 0; i < deepTreebank.size(); i++)
		{
			try
			{
				CoNLLHash deepSentence = deepTreebank.get(i);
				CoNLLHash surfaceSentence = surfaceTreebank.get(i);

				List<String> ids = deepSentence.getIds();
				for (String itt : ids)
				{
					String deepFeats = deepSentence.getFEAT(itt);
					if (deepFeats.contains("id1"))
					{
						StringTokenizer st = new StringTokenizer(deepFeats);
						List<String> surfaceNodes = new ArrayList<>();

						String id0 = "";

						while (st.hasMoreTokens())
						{
							String feat = st.nextToken("|");
							if (feat.startsWith("id"))
							{
								boolean process = !feat.contains("elid") && !feat.contains("coref") && !feat.startsWith("prosubj");
								if (process)
								{
									if (feat.startsWith("id0"))
									{
										id0 = feat.substring(4, feat.length());
									}
									else
									{
										String idSurface = feat.substring(4, feat.length());
										surfaceNodes.add(idSurface);
									}
								}
							}
						}

						if (!id0.isEmpty())
						{
							Integer id0Int = Integer.parseInt(id0);
							String path = "";
							Iterator<String> itIds = surfaceNodes.iterator();
							boolean interestingPattern = false;
							while (itIds.hasNext())
							{
								String id = itIds.next();
								Integer idInt = Integer.parseInt(id);
								if (idInt < id0Int)
								{
									interestingPattern = true;
								}
								if (path.isEmpty())
								{
									path = surfaceSentence.getDeprel(id);
								}
								else
								{
									path = path + "+" + surfaceSentence.getDeprel(id);
								}
							}
							if (interestingPattern)
							{
								Integer freq = patternsFreq.get(path);
								if (freq != null)
								{
									freq++;
									patternsFreq.remove(path);
									patternsFreq.put(path, freq);
								}
								else
								{
									patternsFreq.put(path, 1);
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("Failed to parse line + i");
				throw e;
			}
		}

		String output = "";
		Set<String> keysPatterns = patternsFreq.keySet();
		for (String pattern : keysPatterns)
		{
			output += pattern + "\t" + patternsFreq.get(pattern) + "\n";
		}

		return output;
	}

	/**
	 * This method updates the labels considering the outcomes of the svm classifier. It also sets to ROOT the nodes that have head=0
	 */
	private static String updateLabelsTest(String inPartialOutput, String inPredictedLabels, ModelLabellingClassification mdLabelClass) throws IOException
	{
		BufferedReader predictedLabelsReader = new BufferedReader(new StringReader(inPredictedLabels));
		BufferedReader partialOutputReader = new BufferedReader(new StringReader(inPartialOutput));
		String output = "";

		String line = partialOutputReader.readLine();
		while (line != null)
		{
			if (!line.equals(""))
			{
				String svmLine = predictedLabelsReader.readLine();
				StringTokenizer st = new StringTokenizer(svmLine);
				if (st.hasMoreTokens())
				{
					svmLine = st.nextToken(".");
				}
				String label = mdLabelClass.getDeepLabelForCode(svmLine);
				StringTokenizer st2 = new StringTokenizer(line);
				int cont = 1;
				String newLine = "";
				String head = "";
				while (st2.hasMoreTokens())
				{
					String tok = st2.nextToken("\t");
					if (cont == 11 || cont == 12)
					{
						if (head.equals("0"))
						{
							label = "ROOT";
						}
						newLine += label + "\t";
					}
					else if (cont == 14)
					{
						newLine += tok + "\n";
					}
					else
					{
						newLine += tok + "\t";
					}

					if (cont == 9)
					{
						head = tok;
					}
					cont++;
				}
				output += newLine;
			}
			else
			{
				output += "\n";
			}

			line = partialOutputReader.readLine();
		}

		return output + "\n";
	}

	private static String updateIdsTest(String inPartialOutput, List<CoNLLHash> inSurfaceTestTreebank) throws IOException
	{
		List<HashMap<String, String>> matchingIds = new ArrayList<>();
		HashMap<String, String> sentenceIdMatch = new HashMap<>();
		int sentenceCounter = 0;
		int idCounter = 1;

		BufferedReader partialOutputReader = new BufferedReader(new StringReader(inPartialOutput));
		String output = "";
		String line = partialOutputReader.readLine();
		while (line != null)
		{
			StringTokenizer st = new StringTokenizer(line);
			if (!line.equals(""))
			{
				if (st.hasMoreTokens())
				{
					String id = st.nextToken("\t");
					sentenceIdMatch.put(id, idCounter + "");
					idCounter++;
				}
			}
			else
			{
				HashMap<String, String> sentenceIdMatchClone = new HashMap<>(sentenceIdMatch);
				matchingIds.add(sentenceCounter, sentenceIdMatchClone);
				sentenceIdMatch = new HashMap<>();
				sentenceCounter++;
				idCounter = 1;
			}

			line = partialOutputReader.readLine();
		}

		// Process last line
		HashMap<String, String> sentenceIdMatchClone = new HashMap<>(sentenceIdMatch);
		matchingIds.add(sentenceCounter, sentenceIdMatchClone);

		//Now, I have all the ids that exist in the list of hashmaps (one hashmap for each sentence)
		partialOutputReader = new BufferedReader(new StringReader(inPartialOutput));
		sentenceCounter = 0;
		sentenceIdMatch = matchingIds.get(sentenceCounter);

		String newSentence = "";
		line = partialOutputReader.readLine();
		while (line != null)
		{
			String newLine = "";
			String oldId = "";
			StringTokenizer st = new StringTokenizer(line);
			if (!line.equals(""))
			{
				int cont = 0;
				while (st.hasMoreTokens())
				{
					String tok = st.nextToken("\t");

					if ((cont == 0) || (cont == 8))
					{
						String value = sentenceIdMatch.get(tok);
						if (cont == 0)
						{
							oldId = tok;
						}

						if (value == null)
						{
							//several REASONS:... we should fix this. There are more than one.
							if ((cont == 8))
							{
								if (tok.equals("0"))
								{
									value = "0"; //it was the root node. Okay. Solved.
								}
								else //OTHERWISE the node does not exist. then it should be the governor
								{
									//must find the correct head in the surface test set.
									String aux = tok;
									String governor = findGovernor(sentenceCounter, tok, inSurfaceTestTreebank); //search in the test set for the correc head. Which is basically the head of tok

									aux += "_" + governor;
									String correctHead = sentenceIdMatch.get(governor);
									if (correctHead != null)
									{ //ESTE SECTOR DE  CODIGO ES PARA METER PATRONES DE 2.
										correctHead += "_[" + aux;
									}

									while (correctHead == null)
									{
										governor = findGovernor(sentenceCounter, governor, inSurfaceTestTreebank);
										aux += "_" + governor;

										if (governor.equals("0"))
										{
											correctHead = "0" + "_[" + aux;
										}
										else
										{
											correctHead = sentenceIdMatch.get(governor);
											if (correctHead != null)
											{
												correctHead += "_[" + aux;
											}
										}
									}
									value = correctHead;
								}
							}
						}
						newLine += value + "\t";
					}
					else if (cont == 6)
					{
						String newTok = tok + "|id0=" + oldId;
						newLine += newTok + "\t";
					}
					else if (cont == 13)
					{
						newLine += tok + "\n";
						newSentence += newLine;
						newLine = "";
					}
					else
					{
						newLine += tok + "\t";
					}
					cont++;
				}
			}
			else
			{
				output += newSentence;
				newSentence = "";
				sentenceCounter++;
				if (sentenceCounter < matchingIds.size())
				{
					sentenceIdMatch = matchingIds.get(sentenceCounter);
					output += "\n";
				}
			}
			line = partialOutputReader.readLine();
		}

		// process last line
		output += newSentence;
		output += "\n";

		return output;
	}

	//very inefficient version, just for testing.
	private static String solveInconsistencies(String inPartialOutput, List<CoNLLHash> testSetHash, Candidates candidates) throws IOException
	{
		List<CoNLLHash> deepPartialTreebank = CoNLLTreeConstructor.loadTreebank(inPartialOutput);

		candidates.calculateCandidates(deepPartialTreebank, testSetHash);
		candidates.selectCandidates();
		List<Map<String, List<String>>> selectedCandidates = candidates.getSelectedCandidates();
		List<Map<String, List<String>>> selectedSiblingCandidates = candidates.getSelectedSiblingCandidates();

		String output = "";
		BufferedReader outputReader = new BufferedReader(new StringReader(inPartialOutput));
		String line = outputReader.readLine();
		int sentenceCounter = 0;
		while (line != null)
		{
			String newLine = "";

			if (!line.equals(""))
			{
				String id = "";
				int cont = 0;
				StringTokenizer st = new StringTokenizer(line);

				while (st.hasMoreTokens())
				{
					String tok = st.nextToken("\t");
					if (cont == 0)
					{
						id = tok;
						newLine += tok + "\t";
					}
					else if (cont == 6)
					{
						newLine += tok + "\t";
					}
					else if (cont == 8)
					{
						if (tok.contains("_["))
						{
							String calculatedHead = candidates.getCalculatedHead(tok);
							String surfaceHeadNode = candidates.getDeepCandidate(tok);
							Map<String, List<String>> localCandidates = selectedCandidates.get(sentenceCounter);
							Map<String, List<String>> localSiblingCandidates = selectedSiblingCandidates.get(sentenceCounter);
							List<String> selected = localCandidates.get(surfaceHeadNode);
							List<String> selectedSiblings = localSiblingCandidates.get(surfaceHeadNode);

							if (selectedSiblings.contains(id))
							{
								newLine += calculatedHead + "\t";
							}
							else
							{
								if (!selected.get(0).equals(id))
								{
									newLine += selected.get(0) + "\t";
								}
								else
								{
									newLine += calculatedHead + "\t";
								}
							}
						}
						//recalculate and check pattern if it is the case
						else
						{
							newLine += tok + "\t";
						}
					}
					else if (cont == 10)
					{
						newLine += tok + "\t";
					}
					else if (cont == 13)
					{
						newLine += tok + "\n";
						output += newLine;
						newLine = "";
					}
					else
					{
						newLine += tok + "\t";
					}
					cont++;
				}
			}
			else
			{
				output += "\n";
				sentenceCounter++;
			}

			line = outputReader.readLine();
		}

		return output;
	}

	public static String findGovernor(int sentenceCounter, String tok, List<CoNLLHash> testConllHash)
	{

		CoNLLHash sentence = testConllHash.get(sentenceCounter);
		if (tok.equals("0"))
		{
			return "0";
		}
		return sentence.getHead(tok); //En el caso de heroe enfrentado (primera frase) no es el HEAD de tok si no el "hijo" de tok: ERROR

	}

	public static String findDeprelGovernor(int sentenceCounter, String tok, List<CoNLLHash> testConllHash)
	{

		CoNLLHash sentence = testConllHash.get(sentenceCounter);
		if (tok.equals("0"))
		{
			return "ROOT";
		}
		return sentence.getDeprel(tok); //AQUI SE PODRIA HEREDAR EL DEPREL
	}

	private static String producePartialOutputTest(String inTestConll, String inPredictedNodes) throws IOException
	{
		BufferedReader testConllReader = new BufferedReader(new StringReader(inTestConll));
		BufferedReader predictedNodesReader = new BufferedReader(new StringReader(inPredictedNodes));
		String partialOutput = "";
		String testLine = testConllReader.readLine();
		String svmLine = predictedNodesReader.readLine();
		while (testLine != null && svmLine != null)
		{
			if (!testLine.isEmpty())
			{
				if (svmLine.equals("1.0"))
				{
					partialOutput += testLine + "\n";
				}
				svmLine = predictedNodesReader.readLine();
			}
			else
			{
				partialOutput += "\n";
			}

			testLine = testConllReader.readLine();
		}

		return partialOutput;
	}

	private static ModelLabellingClassification prepareHyperNodeLabelling(List<CoNLLHash> surfacePrunedTreebank,
                                  Map<String, String> inFeatureTranslations, Map<String, String> inLabelTranslations)
	{
		ModelLabellingClassification mdLabelclass = new ModelLabellingClassification(inFeatureTranslations, inLabelTranslations);
		for (CoNLLHash surfaceSentence : surfacePrunedTreebank)
		{
			List<String> idsSurface = surfaceSentence.getIds();
			for (String surfaceId : idsSurface)
			{
				mdLabelclass.addLineTest(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), null, surfaceId, surfaceSentence, true);
			}
		}

		return mdLabelclass;
	}

	private static Pair<ModelHyperNodeClassification, ModelLabellingClassification> detectHyperNodesTrain(List<CoNLLHash> surfaceTreebank, List<CoNLLHash> deepTreebank) throws Exception
	{
		if (surfaceTreebank.size() != deepTreebank.size())
		{
			throw new Exception("Error: Number of sentences do not match");
		}

		ModelHyperNodeClassification mdclass = new ModelHyperNodeClassification();
		ModelLabellingClassification mdLabelclass = new ModelLabellingClassification();

		Iterator<CoNLLHash> its = surfaceTreebank.iterator();
		Iterator<CoNLLHash> itd = deepTreebank.iterator();
		while (its.hasNext() && itd.hasNext())
		{
			CoNLLHash surfaceSentence = its.next();
			CoNLLHash deepSentence = itd.next();
			ArrayList<String> idsSurface = surfaceSentence.getIds();
			ArrayList<String> idsDeep = deepSentence.getIds();

			for (String surfaceId : idsSurface)
			{
				Iterator<String> itD = idsDeep.iterator();
				boolean encontrado = false;
				while (itD.hasNext())
				{
					String deepId = itD.next();
					String featsDeep = deepSentence.getFEAT(deepId);

					if (featsDeep.contains("id0=" + surfaceId))
					{
						StringTokenizer featsTokenizer = new StringTokenizer(featsDeep);
						while (featsTokenizer.hasMoreTokens())
						{
							String feat = featsTokenizer.nextToken("|");
							if (feat.contains("id0"))
							{
								String idValue = feat.replaceAll("id0=", "");
								if (idValue.contains("_"))
								{
									int regex = 0;
									for (int i = 0; i < idValue.length(); i++)
									{
										char c = idValue.charAt(i);
										if (c == '_')
										{
											regex = i;
										}
									}
									idValue = idValue.substring(0, regex);
								}

								//FALTA VER QUE HACER CON LAS CORREFERENCIAS
								if (surfaceId.equals(idValue) && !encontrado)
								{
									mdclass.addLineTrain(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), surfaceId, surfaceSentence, true);
									mdLabelclass.addLineTrain(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), deepSentence.getDeprel(deepId), surfaceId, surfaceSentence, true);
									encontrado = true;
								}
							}
						}
					}
				}

				if (!encontrado)
				{
					itD = idsDeep.iterator();
					int belongs;
					boolean found = false;
					while (itD.hasNext())
					{
						String deepId = itD.next();
						String featsDeep = deepSentence.getFEAT(deepId);

						if (featsDeep.contains("id1") && featsDeep.contains(surfaceId))
						{
							belongs = checkNodeBelonging(surfaceId, featsDeep);
							if ((belongs != 0) && (!found))
							{
								mdclass.addLineTrain(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), surfaceId, surfaceSentence, false);
								found = true;
							}
						}
					}
					if (!found)
					{
						mdclass.addLineTrain(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), surfaceId, surfaceSentence, false);
					}
				}
			}
		}

		return Pair.of(mdclass,	mdLabelclass);
	}

	private static String detectHyperNodesTest(List<CoNLLHash> surfaceTreebank, Map<String, String> inTranslations)
	{
		ModelHyperNodeClassification mdclass = new ModelHyperNodeClassification(inTranslations);
		for (CoNLLHash surfaceSentence : surfaceTreebank)
		{
			ArrayList<String> idsSurface = surfaceSentence.getIds();
			for (String surfaceId : idsSurface)
			{
				mdclass.addLineTest(surfaceSentence.getForm(surfaceId), surfaceSentence.getLemma(surfaceId), surfaceSentence.getPOS(surfaceId), surfaceSentence.getFEAT(surfaceId), surfaceSentence.getDeprel(surfaceId), surfaceId, surfaceSentence);
			}
		}
		return mdclass.getSVMProblem();
	}

	private static int checkNodeBelonging(String surfaceId, String feats)
	{
		StringTokenizer st = new StringTokenizer(feats);
		int id0;
		while (st.hasMoreTokens())
		{
			String feat = st.nextToken("|");
			if (feat.contains("id0"))
			{
				String idValue = feat.replaceAll("id0=", "");
				if (idValue.contains("_"))
				{
					int regex = 0;
					for (int i = 0; i < idValue.length(); i++)
					{
						char c = idValue.charAt(i);
						if (c == '_')
						{
							regex = i;
						}
					}
					idValue = idValue.substring(0, regex);
				}

				id0 = Integer.parseInt(idValue);
				for (int i = 0; i < 5; i++)
				{
					//System.out.println("id"+i+"="+surfaceId+"|");
					if (feats.contains("id" + i + "=" + surfaceId + "|"))
					{
						return id0;
					}
				}
			}
		}

		return 0;
	}

	private static String postProcessingTest(String inSurfaceInput, String inDeepOutput) throws IOException
	{
		ArrayList<CoNLLHash> deepOutput = CoNLLTreeConstructor.loadTreebank(inDeepOutput);
		ArrayList<CoNLLHash> surfaceInput = CoNLLTreeConstructor.loadTreebank(inSurfaceInput);
		BufferedReader br = new BufferedReader(new StringReader(inDeepOutput));
		String output = "";

		ArrayList<String> zeroSubjects = new ArrayList<>();
		boolean isZero;
		int tokenCounter = 1;
		int sentenceCounter = 0;

		String line = br.readLine();
		while (line != null)
		{
			if (line.isEmpty())
			{
				for (String zeroSubject : zeroSubjects)
				{
					String newSubject = tokenCounter + "\t" + zeroSubject;
					output += newSubject + "\n";
					tokenCounter++;
				}
				zeroSubjects = new ArrayList<>();
				sentenceCounter++;
				tokenCounter = 1;
				output += "\n";
			}
			else
			{
				String feats = deepOutput.get(sentenceCounter).getFEAT(tokenCounter + "");
				String id0 = CoNLLHash.getSubFeat(feats, "id0");
				CoNLLHash surfaceSentence = surfaceInput.get(sentenceCounter);
				String ssyntdeprel = surfaceSentence.getDeprel(id0);

				line = putLemmaInForm(line, surfaceSentence.getLemma(id0));
				if (ssyntdeprel.equals("analyt_fut"))
				{
					line = addFeats(line, "tense=FUT");
				}
				if (ssyntdeprel.equals("analyt_pass"))
				{
					line = addFeats(line, "voice=PASS");
				}
				if (ssyntdeprel.equals("analyt_perf"))
				{
					line = addFeats(line, "tense=PAST");
				}
				if (ssyntdeprel.equals("analyt_progr"))
				{
					line = addFeats(line, "tem_constituency=PROGR");
				}
				if (ssyntdeprel.equals("analyt_refl_pass"))
				{
					line = addFeats(line, "voice=PASS");
				}
				if (ssyntdeprel.equals("analyt_refl_lex"))
				{
					line = addReflexiveSe(line);
				}

				String child = detChild(surfaceSentence, id0);
				if (child != null)
				{
					if (child.contains("un"))
					{
						line = addFeats(line, "definiteness=INDEF");
					}
					else
					{
						line = addFeats(line, "definiteness=DEF");
					}
				}

				if (line.contains("VV") && line.contains("number") && line.contains("person"))
				{
					//it is a verb, let's check whether there are zero subjects.
					isZero = areZeroSubjects(surfaceSentence, id0);
					if (isZero)
					{
						String pers = CoNLLHash.getSubFeat(feats, "person");
						String numb = CoNLLHash.getSubFeat(feats, "number");
						String newSubject = "pers" + pers + "_" + "num" + numb + "\t" + "pers" + pers + "_" + "num" + numb + "\t" + "_" + "\t" + "NN" + "\t" + "NN" + "\t" + "dpos=N|" + "id0=" + id0 + "_prosubj|" + "number_coref=" + numb + "|spos_coref=noun" + "\t" + "_" + "\t" + tokenCounter + "\t" + "_" + "\t" + "I\tI\t_\t_";
						zeroSubjects.add(newSubject);
					}
				}
				output += line + "\n";
				tokenCounter++;
			}

			line = br.readLine();
		}

		// process last line
		for (String zeroSubject : zeroSubjects)
		{
			String newSubject = tokenCounter + "\t" + zeroSubject;
			output += newSubject + "\n";
			tokenCounter++;
		}
		output += "\n";
		return output;
	}

	private static String putLemmaInForm(String line, String lemma)
	{
		String output = "";
		StringTokenizer st = new StringTokenizer(line);
		int cont = 1;
		while (st.hasMoreTokens())
		{
			String col = st.nextToken("\t");
			if (cont == 2)
			{
				output += lemma + "\t";
			}
			else if (cont == 14)
			{
				output += col;
			}
			else
			{
				output += col + "\t";
			}

			cont++;
		}

		return output;
	}

	private static String detChild(CoNLLHash surfaceSentence, String id0)
	{
		ArrayList<String> ids = surfaceSentence.getUnsortedIds();
		for (String id : ids)
		{
			String headId = surfaceSentence.getHead(id);
			if (headId.equals(id0))
			{
				String deprel = surfaceSentence.getDeprel(id);
				if (deprel.equals("det"))
				{
					return surfaceSentence.getForm(id);
				}
			}
		}
		return null;
	}

	private static String addReflexiveSe(String line)
	{
		String output = "";
		StringTokenizer st = new StringTokenizer(line);
		int cont = 1;
		while (st.hasMoreTokens())
		{
			String col = st.nextToken("\t");
			if ((cont == 2) || (cont == 3))
			{
				output += col + "\t";
				//output+=col+"se"\t"; //UNCOMENT THIS ONE IF WE WANT TO ADD THE REFLEXIVE SE
			}
			else if (cont == 14)
			{
				output += col;
			}
			else
			{
				output += col + "\t";
			}

			cont++;
		}

		return output;
	}

	private static String addFeats(String line, String string)
	{
		String output = "";
		StringTokenizer st = new StringTokenizer(line);
		int cont = 1;
		while (st.hasMoreTokens())
		{
			String col = st.nextToken("\t");
			if (cont == 7)
			{
				output += string + "|" + col + "\t";
			}
			else if (cont == 14)
			{
				output += col;
			}
			else
			{
				output += col + "\t";
			}

			cont++;
		}

		return output;
	}

//	private static boolean isReflexiveSet(CoNLLHash surfaceSentence, String id0)
//	{
//		ArrayList<String> siblings = surfaceSentence.getSiblings(id0);
//		for (String sib : siblings)
//		{
//			String form = surfaceSentence.getForm(sib);
//			String deprel = surfaceSentence.getDeprel(sib);
//			if (form.equals("se") && (deprel.equals("aux_refl_lex")))
//			{
//				return true;
//			}
//		}
//		return false;
//	}

	private static boolean areZeroSubjects(CoNLLHash surfaceSentence, String id0)
	{
		ArrayList<String> siblings = surfaceSentence.getSiblings(id0);
		Iterator<String> itSib = siblings.iterator();
		boolean thereIsSubject = false;
		while (itSib.hasNext())
		{
			String sib = itSib.next();
			String deprel = surfaceSentence.getDeprel(sib);
			if (deprel.equals("subj"))
			{
				thereIsSubject = true;
			}
		}
		return !thereIsSubject;
	}


	public static void main(String[] args) throws Exception
	{
		System.out.println("-----------------------------------------------------------------------------");
		System.out.println("                     Tree-Transducer 1.0                             ");
		System.out.println("            From Surface Representation to Deep Representation                             ");
		System.out.println("-----------------------------------------------------------------------------");
		//System.out.println("                     Miguel Ballesteros and Leo Wanner                           ");
		//System.out.println("                     @TALN Research Group                             ");
		//System.out.println("                     taln.upf.edu                             ");
		//System.out.println("                     @Pompeu Fabra University                             ");
		//System.out.println("-----------------------------------------------------------------------------");

		Option ssOpt = Option.builder("s").argName("path to SSynt treebank file")
				.hasArg(true)
				.required(false)
				.desc("training SSynt treebank")
				.longOpt("ssynt")
				.build();
		Option dsOpt = Option.builder("d").argName("path to DSynt treebank file")
				.hasArg(true)
				.required(false)
				.desc("training DSynt treebank")
				.longOpt("dsynt")
				.build();
		Option tOpt = Option.builder("t").argName("path to SSynt test file")
				.hasArg(true)
				.required(false)
				.desc("test SSynt treebank")
				.longOpt("test")
				.build();
		Option rOpt = Option.builder("r").argName("path to resources folder")
				.hasArg(true)
				.required(true)
				.desc("resources folder containing model files")
				.longOpt("resources")
				.build();
		Options options = new Options();
		options.addOption(ssOpt);
		options.addOption(dsOpt);
		options.addOption(tOpt);
		options.addOption(rOpt);

		CommandLineParser parser = new DefaultParser();
		CommandLine line = parser.parse(options, args);
		Path resources = Paths.get(line.getOptionValue("r"));

		if (line.getOptions().length == 2 && line.hasOption("t"))
		{
			TransducerSurfToDeep testTransducer = getTestTransducer(resources);

			Path testPath = Paths.get(line.getOptionValue("t"));
			boolean postProcess = true; // for Spanish only?
			String finalOutput = testTransducer.test(testPath, postProcess);

			Path outFilename = Paths.get("predicted_" + testPath.getFileName());
			Path outFile = testPath.getParent().resolve(outFilename);
			System.out.println("Writting output to " + outFile);
			Files.write(outFile, finalOutput.getBytes());
		}
		else if (line.getOptions().length == 3 && line.hasOption("s") && line.hasOption("d"))
		{
			Path surfPath = Paths.get(line.getOptionValue("s"));
			Path deepPath = Paths.get(line.getOptionValue("d"));
			TransducerSurfToDeep trainTransducer = getTrainTransducer();
			trainTransducer.train(surfPath, deepPath, resources);
		}
		else
		{
			System.err.println("Wrong usage.");
		}
	}
}
