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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
     * @return ordered list of stages lines
     * 
     */
    public static ArrayList<String> readStages(File file) {
	ArrayList<String> lines = new ArrayList<String>();

	try {
	    BufferedReader br = new BufferedReader(new FileReader(file));

	    String line = br.readLine();
	    logger.info("start add stage: " + line);
	    line = br.readLine();

	    while (line != null) {
		if (!line.startsWith("#")) {
		    lines.add(line);
		    line = br.readLine();
		} else if (!line.startsWith("#include(")) {
		    String filename = line.substring(9, line.length() - 10);
		    logger.debug("include file: " + filename);
		    lines.addAll(readStages(new File(filename)));
		}
	    }

	    br.close();

	} catch (FileNotFoundException e) {
	    logger.error("file not found", e);
	} catch (IOException e) {
	    logger.error("I/O", e);
	}

	return lines;
    }
}
