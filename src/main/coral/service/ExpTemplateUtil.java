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
import java.io.FileReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import coral.model.ExpData;
import coral.utils.CoralUtils;

/**
 * Provide template (and script) evaluation features.
 * 
 * @author Markus Schaffner
 * 
 */
public class ExpTemplateUtil {

    protected final Log logger = LogFactory
            .getLog(ExpTemplateUtil.class);

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
            throw new RuntimeException(e1);
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
            result = evalScriptR(template, data, error, service);
        } else {
            result = errorPage("cannot process this type of file: " + template);
        }

        return result;
    }

    // velocity template evaluation
    public String evalVM(String t, Map data, ErrorFlag error,
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
            template = Velocity.getTemplate(t, "UTF-8");
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
    public String evalScriptR(String scriptname, ExpData data, ErrorFlag error,
            ExpServiceImpl service) {

        // Map<String, String> newOrChangedMap = new LinkedHashMap<String, sss
        // String>();

        // ScriptEngine jsEngine = m.getEngineByName("js");
        // ScriptContext context = new SimpleScriptContext();

        Context context = Context.enter();

        Scriptable scope = context.initStandardObjects();

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
                scope.put(e.getKey(), scope, value);
            }
        }

        if (service != null) {
            scope.put("agents", scope, service.getAllData().values().toArray());
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(
                    basepath + scriptname)));

            Object o = context.evaluateReader(scope, br, "coral", 1, null);
            if (logger.isDebugEnabled())
                logger.debug("JS OBJECT: " + o);

            for (Object keyOb : scope.getIds()) {
                String key = keyOb.toString();
                Object value = scope.get(key.toString(), scope);

                if (logger.isDebugEnabled())
                    logger.debug("KEYS: " + key + " value: " + value);

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
                    if (!data.containsKey(key)
                            || data.get(key) == null
                            || !data.get(key).toString()
                                    .equals(value.toString())) {
                        data.put(key, value);
                        // newOrChangedMap.put(key, value.toString());
                        if (logger.isDebugEnabled()) {
                            logger.debug("SCRIPTED VALUE: " + key + " == "
                                    + value.toString() + " \t\t | "
                                    + value.getClass());
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("retained: " + key + " == "
                                    + value.toString());
                        }
                    }
                } else if (value instanceof List<?>) {
                    Object[] array = ((List<?>) value).toArray();

                    if (!data.containsKey(key)
                            || data.get(key) == null
                            || !data.get(key).toString()
                                    .equals(Arrays.asList(array).toString())) {
                        data.put(key, array);
                        // newOrChangedMap.put(key, array);
                        if (logger.isDebugEnabled()) {
                            logger.debug("SCRIPTED ARRAY: " + key + " == "
                                    + value.toString() + " \t\t | "
                                    + value.getClass());
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ARRAY retained: " + key + " == "
                                    + value.toString());
                        }
                    }

                    logger.debug("ARRAY: " + key + " == " + value.toString()
                            + " \t\t | " + value.getClass());
                } else {
                    logger.debug("NONVALUE: " + key + " == " + value.toString()
                            + " \t\t | " + value.getClass());
                }
                // context.removeAttribute(key,
                // ScriptContext.ENGINE_SCOPE);

            }

        } catch (Exception e) {
            logger.error("Script failed with Exception "
                    + e.getMessage());
            return errorPage(scriptname + " " + e.getMessage());
        }

        return null;
    }

    // evaluate simple javascript expression with data
    public Object evalExp(String exp, Map<String, Object> data) {

        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();

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
                scope.put(e.getKey(), scope, value);
            }
        }

        Object o = null;
        try {
            o = context.evaluateString(scope, exp, "expression", 1, null);
            if (logger.isDebugEnabled())
                logger.debug("JS OBJECT: " + o);
        } catch (Exception e) {
            logger.error("script failed", e);
        }

        return o;
    }

    /**
     * helper function to generate errorpage with a message
     * 
     * @param message
     *            The message to display on the error page (might be stacktrace)
     * @return
     */
    private String errorPage(String message) {

        logger.error("produce error message for client: " + message);

        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><h1>An error has occured</h1><p>");
        sb.append(message);
        sb.append("</p><p>See log file for details<p>");
        sb.append("<form action='"
                + CoralUtils.getHostStr()
                + "'><p><input type='submit' name='skiperror' value='SKIP'> <br><input type='submit' name='reload' value='RELOAD'> <br></form>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
