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
package coral.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.MathTool;

import coral.model.ExpData;
import coral.utils.CoralUtils;

/**
 * Provide template (and script) evaluation features.
 * 
 * @author Markus Schaffner
 * 
 */
public class ExpTemplateUtil {

	static ScriptEngineManager m = new ScriptEngineManager();

	static protected final Log logger = LogFactory
			.getLog(ExpTemplateUtil.class);
	private static Process ztreeProcess = null;

	String basepath = "";

	public ExpTemplateUtil(String basepath) {

		this.basepath = basepath;

		try {

			Properties p = new Properties();

			p.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
			p.setProperty("file.resource.loader.path", basepath);
			// p.setProperty("file.resource.loader.cache", "false");

			// TODO the following two options are probably less then optimal
			p.setProperty("velocimacro.library.autoreload", "true");
			p.setProperty(
					"velocimacro.permissions.allow.inline.to.replace.global",
					"true");
			
			// TODO do we care about velocity logs? probably not in production
			p.setProperty("runtime.log.logsystem.class",
					"org.apache.velocity.runtime.log.NullLogSystem");

			Velocity.init(p);

		} catch (Exception e1) {
			logger.error("Error in velocity setup", e1);
		}

	}

	// main entry method
	public String eval(String template, ExpData data, ErrorFlag error,
			ExpServiceImpl service) {

		String result = null;

		if (template.endsWith(".vm")) {
			result = evalVM(template, data, error, service);
		} else if (template.endsWith(".html") || template.endsWith(".htm")) {
			result = evalVM(template, data, error, service);
		} else if (template.endsWith(".js")) {
			result = evalScript(template, data, error, service);
		} else if (template.endsWith(".ztt")) {
			result = runZtree(template, data, error, service);
		} else {
			result = errorPage("cannot process this type of file: " + template);
		}

		return result;
	}

	// velocity template evaluation
	public static String evalVM(String t, Map data, ErrorFlag error,
			ExpServiceImpl service, Map<String, Object>... adds) {

		/*
		 * Make a context object and populate with the data. This is where the
		 * Velocity engine gets the data to resolve the references (ex. $list)
		 * in the template
		 */

		VelocityContext context = new VelocityContext();
		context.put("data", data);
		context.put("error", error);
		context.put("_coralhost", CoralUtils.getHostStr());
		
		context.put("_math", new MathTool());
		
		for (Map<String, Object> add : adds) {
			for (Map.Entry<String, Object> a : add.entrySet()) {
				context.put(a.getKey(), a.getValue());
			}
		}

		if (service != null) {
			context.put("debug", service.debug);
		}

		for (Object o : data.entrySet()) {
			Map.Entry e = (Map.Entry) o;
			if (e != null && e.getKey() != null
					&& !e.getKey().toString().equals("")) {
				Object value = data.get(e.getKey());
				if (logger.isDebugEnabled()) {
					logger.debug("entered value: " + e.getKey() + " == "
							+ e.getValue().toString() + " \t\t | "
							+ e.getClass());
				}
				context.put(e.getKey().toString(), value);
			}
		}

		/*
		 * get the Template object. This is the parsed version of your template
		 * input file.
		 */

		Template template = null;

		try {
			template = Velocity.getTemplate(t);
		} catch (ResourceNotFoundException rnfe) {
			logger.error("error : cannot find template " + t, rnfe);
			return errorPage(template + ": " + rnfe.getMessage());
		} catch (ParseErrorException pee) {
			logger.error("Syntax error in template " + t + ":", pee);
			return errorPage(template + ": " + pee.getMessage());
		} catch (Exception e) {
			logger.error("Velocity exception in template: " + t + ":", e);
			return errorPage(template + ": " + e.getMessage());
		}

		/*
		 * Now have the template engine process your template using the data
		 * placed into the context. Think of it as a 'merge' of the template and
		 * the data to produce the output stream.
		 */

		StringWriter writer = new StringWriter();
		try {
			template.merge(context, writer);
		} catch (ResourceNotFoundException e) {
			logger.error("vm file not found", e);
			return errorPage(template + ": " + e.getMessage());
		} catch (ParseErrorException e) {
			logger.error("vm parsing problem", e);
			return errorPage(template + ": " + e.getMessage());
		} catch (MethodInvocationException e) {
			logger.error("vm mie", e);
			return errorPage(template + ": " + e.getMessage());
		} catch (Exception e) {
			logger.error("vm exp", e);
			return errorPage(template + ": " + e.getMessage());
		}

		return writer.toString();
	}

	// javascript evaluation
	public String evalScript(String scriptname, ExpData data, ErrorFlag error,
			ExpServiceImpl service) {

		// Map<String, String> newOrChangedMap = new LinkedHashMap<String,
		// String>();

		ScriptEngine jsEngine = m.getEngineByName("js");
		ScriptContext context = new SimpleScriptContext();

		// context.setAttribute("data", data, ScriptContext.ENGINE_SCOPE);
		// context.setAttribute("error", error, ScriptContext.ENGINE_SCOPE);

		for (Map.Entry<String, Object> e : data.entrySet()) {
			if (e != null && e.getKey() != null
					&& !e.getKey().toString().equals("")) {
				Object value = data.get(e.getKey());

				if (logger.isDebugEnabled()) {
					logger.debug("entered value: " + e.getKey() + " == "
							+ value + " \t\t | " + value.getClass());
				}
				context.setAttribute(e.getKey(), value,
						ScriptContext.ENGINE_SCOPE);
			}
		}

		if (service != null) {
			context.setAttribute("agents", service.getAllData().values()
					.toArray(), ScriptContext.ENGINE_SCOPE);
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					basepath + scriptname)));

			Object o = jsEngine.eval(br, context);
			if (logger.isDebugEnabled())
				logger.debug("JS OBJECT: " + o);

			Map<String, Object> outmap = context
					.getBindings(ScriptContext.ENGINE_SCOPE);
			
			for (Map.Entry<String, Object> e : outmap.entrySet()) {
				Object value = e.getValue();

				// TODO dirty way to unwrap NativeJavaObjects
				if (!(value instanceof Number) && !(value instanceof String)) {
					try {
						Method m = value.getClass().getMethod("unwrap");
						value = m.invoke(value);
					} catch (SecurityException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (NoSuchMethodException e1) {
						// TODO Auto-generated catch block
						// e1.printStackTrace();
					} catch (IllegalArgumentException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

				if (value instanceof Number || value instanceof String) {
					if (!data.containsKey(e.getKey())
							|| data.get(e.getKey()) == null
							|| !data.get(e.getKey()).toString()
									.equals(value.toString())) {
						data.put(e.getKey(), value);
						// newOrChangedMap.put(e.getKey(), value.toString());
						if (logger.isDebugEnabled()) {
							logger.debug("SCRIPTED VALUE: " + e.getKey()
									+ " == " + value.toString() + " \t\t | "
									+ value.getClass());
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("retained: " + e.getKey() + " == "
									+ value.toString());
						}
					}
				} else if (value instanceof List<?>) {
					Object[] array = ((List<?>) value).toArray();

					if (!data.containsKey(e.getKey())
							|| data.get(e.getKey()) == null
							|| !data.get(e.getKey()).toString()
									.equals(Arrays.asList(array).toString())) {
						data.put(e.getKey(), array);
						// newOrChangedMap.put(e.getKey(), array);
						if (logger.isDebugEnabled()) {
							logger.debug("SCRIPTED ARRAY: " + e.getKey()
									+ " == " + value.toString() + " \t\t | "
									+ value.getClass());
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("ARRAY retained: " + e.getKey()
									+ " == " + value.toString());
						}
					}

					logger.debug("ARRAY: " + e.getKey() + " == "
							+ value.toString() + " \t\t | "
							+ e.getValue().getClass());
				} else {
					logger.debug("NONVALUE: " + e.getKey() + " == "
							+ value.toString() + " \t\t | "
							+ e.getValue().getClass());
				}
				// context.removeAttribute(e.getKey(),
				// ScriptContext.ENGINE_SCOPE);
			}

		} catch (FileNotFoundException e1) {
			logger.error("File Not Found Exception ", e1);
			return errorPage(scriptname + " " + e1.getMessage());
		} catch (ScriptException e) {
			logger.error("Script failed with Exception " + e.getMessage());
			return errorPage(scriptname + " " + e.getMessage());
		}

		return null;
	}

	// evaluate simple javascript expression with data
	public static Object evalExp(String exp, Map<String, Object> data) {

		ScriptEngine jsEngine = m.getEngineByName("js");
		ScriptContext context = new SimpleScriptContext();

		for (Map.Entry<String, Object> e : data.entrySet()) {
			if (e != null && e.getKey() != null
					&& !e.getKey().toString().equals("")) {
				Object value = e.getValue();

				try {
					try {
						value = Integer.parseInt(value.toString());
					} catch (NumberFormatException ex) {
						double v = Double.parseDouble(value.toString());
						if (Math.round(v) == v) {
							value = Math.round(v);
						} else {
							value = v;
						}
					}
				} catch (NumberFormatException ex) {
					// value = value;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("entered value: " + e.getKey() + " == "
							+ e.getValue().toString() + " \t\t | "
							+ e.getClass());
				}
				context.setAttribute(e.getKey(), value,
						ScriptContext.ENGINE_SCOPE);
			}
		}

		Object o = null;
		try {
			o = jsEngine.eval(exp, context);
			if (logger.isDebugEnabled())
				logger.debug("JS OBJECT: " + o);
		} catch (ScriptException e) {
			logger.error("script failed", e);
		}

		return o;
	}

	// TODO experimental ztree evaluation, is probably broken (untested)
	public String runZtree(String template, ExpData data, ErrorFlag error,
			final ExpServiceImpl service) {
		if (ztreeProcess == null) {
			Runtime rt = Runtime.getRuntime();
			try {
				ztreeProcess = rt.exec("ztree.exe /treatment " + template);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			new Thread() {
				public void run() {
					File f = new File(basepath + "ztree_end.txt");
					while (!f.exists()) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					// TODO read f and post to database

					ztreeProcess.destroy();
					ztreeProcess = null;

					// TODO delete file
					f.delete();

					// TODO progress client
					for (Integer id : service.dataMap.keySet()) {
						// todo add table comps as args
						service.process(id, "zleaf");
					}
				};
			}.start();
		}

		return null;
	}

	/**
	 * helper function to generate errorpage with a message
	 * 
	 * @param message
	 *            The message to display on the error page (might be stacktrace)
	 * @return
	 */
	static String errorPage(String message) {

		logger.error("produce error message for client: " + message);

		StringBuilder sb = new StringBuilder();

		sb.append("<html><body><h1>An error has occured</h1><p>");
		sb.append(message);
		sb.append("</p><p>See log file for details<p>");
		sb.append("<form action='"+CoralUtils.getHostStr()+"'><p><input type='submit' name='skiperror' value='SKIP'> <br><input type='submit' name='reload' value='RELOAD'> <br></form>");
		sb.append("</body></html>");

		return sb.toString();
	}
}
