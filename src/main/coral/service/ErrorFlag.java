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

import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import coral.model.ExpData;

public class ErrorFlag extends HashMap<String, String> {

    private static final long serialVersionUID = 1L;

    protected final Log logger = LogFactory.getLog(this.getClass());

    public final static String nullOrEmpty = "NullOrEmpty";
    public final static String noNumber = "noNumber";

    public final static String wrongPrecision = "wrongPrecision";

    public final static String tooSmall = "tooSmall";
    public final static String tooBig = "tooBig";

    public final static String notEqual = "notEqual";

    public final static String evalError = "evalError";
    public final static String evalFalse = "evalFalse";

    private boolean alwaystrue = false;
    private boolean valid = true;

    private ExpTemplateUtil util;
    
    public ErrorFlag(boolean alwaystrue, ExpTemplateUtil util) {
        this.util = util;
        this.alwaystrue = alwaystrue;
    }

    public boolean validateFormated(ExpData values, String... props) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate formated " + Arrays.toString(props));
        }
        boolean ok = true;
        if (props != null) {
            for (String prop : props) {
                if (logger.isDebugEnabled()) {
                    logger.debug("validate formated single: " + prop);
                }
                if (prop.equals("*")) {
                    // ressync condition
                    ok = true;
                } else if (prop.contains(":=")) {
                    // equals variable
                    String[] p = prop.split(":=");
                    ok = validateEqualsVar(values, new String[] { p[0] },
                            new String[] { p[1] }) && ok;
                } else if (prop.contains("={")) {
                    // equals one of a set of values
                    String[] p = prop.split("=\\{");
                    String s = p[1].substring(0, p[1].length() - 1);
                    ok = validateEqualsSet(values, p[0], s.split("::")) && ok;
                } else if (prop.contains("::")) {
                    // number check
                    String[] p = prop.split("::");
                    double min = Double.parseDouble(p[1]);
                    double max = Double.parseDouble(p[2]);
                    int precision = (p[1].contains(".")) ? p[1].substring(
                            p[1].indexOf(".")).length() : 0;
                    ok = validateNumber(values, min, max, precision, p[0])
                            && ok;
                } else if (prop.contains(":")) {
                    int i = prop.indexOf(':');
                    ok = validateJs(prop.substring(0, i),
                            prop.substring(i + 1), values) && ok;
                } else if (prop.contains("==")) {
                    // equals value
                    String[] p = prop.split("==");
                    ok = validateEquals(values, new String[] { p[0] },
                            new String[] { p[1] }) && ok;
                } else {
                    // null or empty
                    ok = validateNotNullOrEmpty(values, prop) && ok;
                }
            }
        }
        return ok || alwaystrue;
    }

    public boolean validateJs(String name, String exp, ExpData values) {
        boolean ok = true;
        Object o = util.evalExp(exp, values);

        if ((o == null) || !(o instanceof Boolean)) {
            ok = false;
            valid = false;
            put(name, evalError);
        } else if (((Boolean) o)) {
            ok = true;
        } else {
            ok = false;
            valid = false;
            put(name, evalFalse);
        }

        return ok || alwaystrue;
    }

    public boolean validateNotNullOrEmpty(ExpData values, String prop) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate null or empty " + prop);
        }
        boolean ok = true;
        if (prop != null) {
            if (values.containsKey(prop) && values.get(prop) != null) {
                String val = values.markGet(prop).toString();
                // System.err.println(prop + " : " + val);
                if (val == null || val.equals("")) {
                    ok = false;
                    valid = false;
                    put(prop, nullOrEmpty);
                }
            } else {
                ok = false;
                valid = false;
                put(prop, nullOrEmpty);
            }
        }
        return ok || alwaystrue;
    }

    public boolean validateEquals(ExpData values, String[] props, String[] vals) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate equals " + Arrays.toString(props));
        }
        boolean ok = true;
        for (int i = 0; i < props.length; i++) {
            String prop = props[i];
            if (values.containsKey(prop)) {
                Object num = values.markGet(prop);
                if (num == null || num.equals("")) {
                    ok = false;
                    valid = false;
                    put(prop, nullOrEmpty);
                } else {
                    if (!num.toString().equals(vals[i])) {
                        ok = false;
                        valid = false;
                        put(prop, notEqual);
                    }
                }
            } else {
                ok = false;
                valid = false;
                put(prop, nullOrEmpty);
            }
        }
        return ok || alwaystrue;
    }

    public boolean validateEqualsSet(ExpData values, String prop, String[] vals) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate equals set " + prop);
        }
        boolean ok = true;

        if (values.containsKey(prop)) {
            Object num = values.markGet(prop);
            if (num == null || num.equals("")) {
                ok = false;
                valid = false;
                put(prop, nullOrEmpty);
            } else {
                boolean hasmatch = false;
                for (String s : vals) {
                    hasmatch = hasmatch || num.toString().equals(s);
                }
                if (!hasmatch) {
                    ok = false;
                    valid = false;
                    put(prop, notEqual);
                }
            }
        } else {
            ok = false;
            valid = false;
            put(prop, nullOrEmpty);
        }

        return ok || alwaystrue;
    }

    public boolean validateEqualsVar(ExpData values, String[] props,
            String[] vals) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate equals var: " + Arrays.toString(props));
        }
        boolean ok = true;
        for (int i = 0; i < props.length; i++) {
            String prop = props[i];
            if (values.containsKey(prop)) {
                Object num = values.markGet(prop);
                if (num == null || num.equals("")) {
                    ok = false;
                    valid = false;
                    put(prop, nullOrEmpty);
                } else {
                    // all the magic here
                    if (values.containsKey(vals[i])) {
                        String comp = values.markGet(vals[i]).toString();
                        try {
                            double numD = Double.parseDouble(num.toString());
                            double compD = Double.parseDouble(comp);
                            if (numD != compD) {
                                ok = false;
                                valid = false;
                                put(prop, notEqual);
                            }
                        } catch (NumberFormatException e) {
                            if (!num.equals(comp)) {
                                ok = false;
                                valid = false;
                                put(prop, notEqual);
                            }
                        }
                    } else {
                        ok = false;
                        valid = false;
                        put(prop, evalError);
                    }
                }
            } else {
                ok = false;
                valid = false;
                put(prop, nullOrEmpty);
            }
        }
        return ok || alwaystrue;
    }

    public boolean validateNumber(ExpData values, double min, double max,
            int precision, String... props) {
        if (logger.isDebugEnabled()) {
            logger.debug("validate number " + Arrays.toString(props));
        }
        boolean ok = true;
        for (String prop : props) {
            if (values.containsKey(prop) && values.get(prop) != null) {
                String num = values.markGet(prop).toString();
                if (num == null || num.equals("")) {
                    ok = false;
                    valid = false;
                    put(prop, nullOrEmpty);
                } else {
                    double d = min;
                    try {
                        d = Double.parseDouble(num);
                    } catch (NumberFormatException e) {
                        ok = false;
                        valid = false;
                        put(prop, noNumber);
                    }
                    // L.println("validate " + d + " min " + min + " max" +
                    // max);
                    if (d < min) {
                        ok = false;
                        valid = false;
                        put(prop, tooSmall);
                    }
                    if (d > max) {
                        ok = false;
                        valid = false;
                        put(prop, tooBig);
                    }
                    int numprecision = (num.contains(".")) ? num.substring(
                            num.indexOf(".")).length() : 0;
                    if (numprecision > precision) {
                        ok = false;
                        valid = false;
                        put(prop, wrongPrecision);
                    }

                }
            } else {
                ok = false;
                valid = false;
                put(prop, nullOrEmpty);
            }
        }
        return ok || alwaystrue;
    }

    public boolean isValid() {
        return valid || alwaystrue;
    }

}
