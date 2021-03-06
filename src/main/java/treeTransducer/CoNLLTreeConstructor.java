/**
 *
 */
package treeTransducer;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;


/**
 * @author Miguel Ballesteros
 *         Universitat Pompeu Fabra
 *         <p/>
 *         This object of this class would receive a file and then generates a Tree<T> that stores the info associated.
 */
public class CoNLLTreeConstructor
{
	public static ArrayList<CoNLLHash> loadTreebank(String inContents) throws IOException
	{
		return loadTreebank(new BufferedReader(new StringReader(inContents)));
	}

	/**
	 * This method stores a treebank in the return object
	 *
	 * @return Treebank in ArrayList<CoNLLHash> format
	 */
	public static ArrayList<CoNLLHash> loadTreebank(BufferedReader inReader) throws IOException
	{

		ArrayList<CoNLLHash> list = new ArrayList<>();

		CoNLLHash hash = new CoNLLHash();
		String l = inReader.readLine();
		while (l != null)
		{
			if (l.trim().isEmpty())
			{
				if (!hash.isEmpty())
					list.add(hash); // add it even if it's empty (some dsynts may consist of no tokens)
				hash = new CoNLLHash();
			}
			else
			{
				hash.addLine(l);
			}

			l = inReader.readLine();
		}

		// process last line
		if (!hash.isEmpty())
			list.add(hash);
		return list;
	}
}
