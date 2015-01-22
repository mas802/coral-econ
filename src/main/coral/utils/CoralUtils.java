/*
 *   Copyright 2009-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package coral.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import coral.model.ExpStage;

/**
 * Class with helper functions that cannot be found somewhere else.
 * 
 * @author Markus Schaffner
 * 
 */
public class CoralUtils {

    protected final static Log logger = LogFactory.getLog(CoralUtils.class);

    /**
     * Helper method to trims the quotes.
     * <p>
     * For example,
     * <ul>
     * <li>("a.b") => a.b
     * <li>("a.b) => "a.b
     * <li>(a.b") => a.b"
     * </ul>
     * 
     * @param value
     *            the string may have quotes
     * @return the string without quotes
     */

    public static String trimQuotes(String value) {
	if (value == null)
	    return value;

	value = value.trim();
	if (value.startsWith("\"") && value.endsWith("\""))
	    return value.substring(1, value.length() - 1);

	return value;
    }

    /**
     * convert string into parameter map
     * 
     * @param arg
     * @return
     */
    public static Map<String, String> urlToMap(String arg) {
	Map<String, String> args = new HashMap<String, String>();
	int startOfQuery = arg.indexOf('?') + 1;
	for (String part : arg.substring(startOfQuery, arg.length()).split("&")) {
	    String[] var = part.split("=");
	    if (var[0] != null) {
		try {
		    args.put(
			    var[0],
			    (var.length > 1) ? URLDecoder.decode(var[1],
				    "UTF-8").trim() : "");
		} catch (UnsupportedEncodingException e) {
		    throw new RuntimeException(
			    "Just rethrow as a RuntimeException, something went wrong here (i.e. no support for UTF-8, which is universal).",
			    e);
		}
	    }
	}
	return args;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	for (Entry<T, E> entry : map.entrySet()) {
	    if (value.equals(entry.getValue())) {
		return entry.getKey();
	    }
	}
	return null;
    }

    public static String hoststr = "";

    public static String getHostStr() {
	return hoststr;
    }

    /**
     * Returns a ordered list of lines from a stages file, resolving includes
     * and striping away comments.
     * 
     * @param file
     *            the stages file
     * @param variants
     * @return ordered list of stages lines
     * 
     */
    public static List<ExpStage> readStages(File file, String variants,
	    File templateFolder) {
	ArrayList<String> lines = new ArrayList<String>();

	try {
	    BufferedReader br = new BufferedReader(new FileReader(file));

	    String line = br.readLine();
	    logger.info("start add stage: " + line);

	    while ((line = br.readLine()) != null) {
		if (!line.startsWith("#") || line.startsWith("#include(")) {
		    lines.add(line);
		    logger.info(" add stage: " + line);
		}
	    }

	    br.close();

	} catch (FileNotFoundException e) {
	    logger.error("file not found", e);
	} catch (IOException e) {
	    logger.error("I/O", e);
	}

	logger.debug("lines read");

	int counter = 0;
	Map<String, Integer> loopMap = new HashMap<String, Integer>();

	List<ExpStage> stages = new ArrayList<ExpStage>();

	for (String line : lines) {
	    if (line.startsWith("#include(")) {
		String filename = line.substring(line.indexOf('(') + 1,
			line.lastIndexOf(')'));
		logger.debug("include file: " + filename);
		stages.addAll(readStages(new File(file.getParentFile(),
			filename), variants, templateFolder));
	    } else {

		counter++;

		String[] parts = line.split(",");
		int l = parts.length;

		String[] condition = new String[] {};
		String[] valid = new String[] {};

		String waitFor = "";
		int loopstage = 0;
		int looprepeat = 0;

		if (l > 1) {
		    try {
			logger.debug("add stage: 2 : " + Arrays.toString(parts));

			if (parts[1].startsWith(":")) {
			    loopMap.put(parts[1].substring(1), counter);
			}
			if (parts[1].endsWith(":")) {
			    loopstage = counter
				    - loopMap.get(parts[1].substring(0,
					    parts[1].length() - 1));
			} else {
			    loopstage = (parts[1] == null) ? 0 : Integer
				    .parseInt(CoralUtils.trimQuotes(parts[1]));
			}
			looprepeat = (parts[2] == null) ? 0 : Integer
				.parseInt(CoralUtils.trimQuotes(parts[2]));
		    } catch (NumberFormatException e) {
			// e.printStackTrace();
		    }
		}
		if (l > 3) {
		    logger.debug("add stage: condition : " + parts[3]);
		    condition = (parts[3].length() > 0) ? CoralUtils
			    .trimQuotes(parts[3]).split(";") : condition;
		}
		if (l > 4) {
		    logger.debug("add stage: validate : " + parts[4]);
		    valid = (parts[4].length() > 0) ? CoralUtils.trimQuotes(
			    parts[4]).split(";") : valid;
		}
		if (l > 5) {
		    logger.debug("add stage: waitFor : "
			    + CoralUtils.trimQuotes(parts[5]));
		    waitFor = CoralUtils.trimQuotes(parts[5]);
		}

		String name = CoralUtils.trimQuotes(parts[0]);

		if (variants != null) {
		    int extpos = name.lastIndexOf(".");
		    if (extpos > 0) {
			String localised = name.substring(0, extpos + 1)
				+ variants + name.substring(extpos);
			File test = new File(file.getParent(), localised);
			logger.debug("check for variant: "
				+ test.getAbsolutePath());
			if (test.exists()) {
			    name = localised;
			}
		    }
		}

		// only add the file ref if the file exists, otherwise just 
		// add the name for an error to show when running
		// TODO this should trigger some sort of alarm
		File test = new File(file.getParent(), name);
		if (!test.exists()
			&& !(parts.length > 3 && parts[3].equals("*"))) {
		    logger.warn("stage file " + name
			    + " does not exist in path " + file.getParent()
			    + "  ---  " + test.getAbsolutePath());
		    
		    ExpStage stage = new ExpStage(name + " (does not exist)", loopstage,
			    looprepeat, condition, valid, waitFor);
		    stages.add(stage);
		} else {
		    String templateRef = test.getAbsolutePath().replace(
			    templateFolder.getAbsolutePath(), "");
		    ExpStage stage = new ExpStage(templateRef, loopstage,
			    looprepeat, condition, valid, waitFor);
		    stages.add(stage);
		}
	    }
	}
	return stages;
    }
}
