package ca.phon.dict.randomizer;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.*;

/**
 * Randomizes dictionary files for better
 * efficiency with the ternary trie.
 *
 * @goal randomize 
 * 
 * @phase generate-resources
 */
public class RandomizerMojo
    extends AbstractMojo
{
    /**
     * @parameter property="${project.build.directory}/dict"
     * @optional
     */
    private File outputDirectory;
    
    /**
     * @parameter property="/src/main/dict" 
     * @optional
     */
    private File libDirectory;
    
    /**
     * Dictionary list
     * @parameter property="dicts.list"
     * @optional
     */
    private String startFile;
    
    /**
     * File encoding of chunks
     * @parameter property="UTF-8"
     * @optional
     */
    private String encoding;
    
    public void execute()
        throws MojoExecutionException
    {
    	if(!outputDirectory.exists()) {
    		getLog().info("Making directory '" + outputDirectory.getAbsolutePath() + "'");
    		if(!outputDirectory.mkdirs()) {
    			throw new MojoExecutionException("Failed to create output directory.");
    		}
    	}
    	
    	if(!libDirectory.exists()) {
    		throw new MojoExecutionException("'" + libDirectory.getAbsolutePath() + "' not found");
    	}
    	
    	final File indexFile = new File(libDirectory, startFile);
    	if(!indexFile.exists()) {
    		throw new MojoExecutionException("Index file '" + indexFile.getAbsolutePath() + "' not found.");
    	}
    	
	try(BufferedReader indexReader = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile), encoding))) {
		String line = null;
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDirectory, startFile))));
		while((line = indexReader.readLine()) != null) {
			if(line.startsWith("#") || line.trim().length() == 0) continue;

			int extIdx = line.lastIndexOf(".");
			String name = line.substring(0, extIdx);
			String prefixName = name + "-prefix.txt";
			
			File inputFile = new File(libDirectory, line);
			File prefixFile = new File(libDirectory, prefixName);
			File outputFile = new File(outputDirectory, line);

			getLog().info("Randomizing dictionary " + line + " saving as " + outputFile.getAbsolutePath());
			randomizeDictionary(inputFile, prefixFile, outputFile);

			writer.write(line);
			writer.write("\n");
		}	
		writer.close();
	} catch(IOException e) {
		throw new MojoExecutionException("Failed to randomize dictionary", e);
	}	
    }

    private void randomizeDictionary(File dictFile, File prefixFile, File outputFile) throws IOException {
	final BufferedReader prefixReader = new BufferedReader(new InputStreamReader(new FileInputStream(prefixFile), encoding));
	final BufferedReader dictReader = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), encoding));

	if(!outputFile.getParentFile().exists())
		outputFile.getParentFile().mkdirs();
	final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), encoding));

	String line = null;
	while((line = prefixReader.readLine()) != null) {
		writer.write(line);
		writer.write("\n");
	}
	prefixReader.close();
	
	Set<String> hashSet = new HashSet<>();		
	while((line = dictReader.readLine()) != null) {
		hashSet.add(line);
	}
	dictReader.close();
	
	for(String hashedLine:hashSet) {
		writer.write(hashedLine);
		writer.write("\n");
	}
	writer.flush();
	writer.close();
    }
}

