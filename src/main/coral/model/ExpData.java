/*
 *   Copyright 2009-2014 Markus Schaffner
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
package coral.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class that offers a transparent access to a flat data structure with option
 * to save only new/updated values.
 * 
 * @author Markus Schaffner
 * 
 */
public class ExpData implements Map<String, Object> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private Map<String, Map<String, String>> refMap = new LinkedHashMap<String, Map<String, String>>();
	private Map<String, String> currentMap = new LinkedHashMap<String, String>();

	public String inmsg = "_";
	public String template = "NONE";

	public int _stageCounter = -99;
	public int _msgCounter = -99;

	public int stageCounter() {
		return _stageCounter;
	}

	public void setNewpage(boolean newpage) {
		this.put("newpage", Boolean.toString(newpage));
	}

	public boolean isNewpage() {
		return this.containsKey("newpage")
				&& Boolean.parseBoolean(this.get("newpage").toString());
	}

	@Override
	public String put(String key, Object value) {
		if (logger.isDebugEnabled())
			logger.debug("data put key: " + key);

		refMap.put(key, currentMap);
		// FIXME conversion

		if (value.getClass().isArray()) {
			value = "[" + Arrays.toString((Object[]) value);
		}
		return currentMap.put(key, value.toString());
	}

	/*
	 * special method to get an move to the current map for recording
	 */
	public Object markGet(String key) {
		Object val = get(key);
		if (refMap.containsKey(key))
			refMap.get(key).remove(key);
		put(key, val);
		return val;
	}

	@Override
	public Object get(Object key) {
		if (logger.isDebugEnabled())
			logger.debug("data get key: " + key);

		Map<String, String> m = refMap.get(key);
		String e = (m != null) ? m.get(key) : currentMap.get(key);

		return stringToObject(e);
	}

	private static Object stringToObject(String e) {
		if (e != null && e.toString().startsWith("[[")) {
			String[] parts = e.substring(2, e.length() - 1).split(", ");
			Object[] array = new Object[parts.length];
			int i = 0;

			long[] longs = new long[parts.length];
			boolean islong = true;

			int[] ints = new int[parts.length];
			boolean isint = true;

			double[] doubles = new double[parts.length];
			boolean isdouble = true;

			for (String part : parts) {
				Object o = stringToObject(part);

				if (!(o instanceof Long) && !(o instanceof Integer)) {
					islong = false;
				} else if ((o instanceof Long)) {
					longs[i] = (Long) o;
				}

				if (!(o instanceof Integer)) {
					isint = false;
				} else {
					ints[i] = (Integer) o;
					longs[i] = (Integer) o;
				}

				if (!(o instanceof Double)) {
					isdouble = false;
				} else {
					doubles[i] = (Double) o;
				}

				array[i++] = o;
			}
			if (isint) {
				return ints;
			} else if (islong) {
				return longs;
			} else if (isdouble) {
				return doubles;
			} else {
				return array;
			}
		}

		if (e != null && !e.toString().equals("")) {
			Object value = e;

			try {
				try {
					try {
						value = Integer.parseInt(value.toString());
					} catch (NumberFormatException ex) {
						value = Long.parseLong(value.toString());
					}
				} catch (NumberFormatException ex) {
					double v = Double.parseDouble(value.toString());
					if (Math.round(v) == v) {
						value = Math.round(v);
						int newvalue = ((Long)value).intValue();
						long compvalue = ((Long)value).longValue();
						if ( (long)newvalue == compvalue ) {
							value = newvalue;
						}
					} else {
						value = v;
					}
				}
			} catch (NumberFormatException ex) {
			}
			// System.out.println(value.getClass());
			return value;
		} else {
			return e;
		}

	}

	public Map<String, String> newMap() {
		Map<String, String> result = currentMap;
		this.currentMap = new LinkedHashMap<String, String>();
		return result;
	}

	public String getString(String name) {
		Object o = this.get(name);
		String s = (o == null) ? "-99" : o.toString();
		return s;
	}

	public long getLong(String name) {
		Object o = this.get(name);
		String s = (o == null) ? "-99" : o.toString();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			logger.debug("number format problem", e);
			try {
				return Math.round(Double.parseDouble(s));
			} catch (NumberFormatException nfe) {
				logger.warn("number format problem", nfe);
				return -99;
			}
		}
	}

	public double getDouble(String name) {
		Object o = this.get(name);
		String s = (o == null) ? "-99" : o.toString();
		double d = -99.0;
		try {
			d = Double.parseDouble(s);
		} catch (NumberFormatException nfe) {
			logger.warn("number format problem", nfe);
		} catch (NullPointerException ne) {
			// ne.printStackTrace();
			logger.warn("number format problem", ne);
		}
		return d;
	}

	public String getRounded(String name, int digits) {
		double s = this.getDouble(name);
		return String.format("%." + digits + "f", s);
	}

	/*
	 * Default overrides
	 */
	@Override
	public boolean isEmpty() {
		return refMap.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return refMap.keySet();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		for (String s : m.keySet()) {
			// FIXME putting stuff in here
			currentMap.put(s, m.get(s).toString());
			refMap.put(s, currentMap);
		}
	}

	@Override
	public String remove(Object key) {
		if (refMap.containsKey(key)) {
			return refMap.remove(key).remove(key);
		} else {
			return null;
		}
	}

	@Override
	public int size() {
		return refMap.size();
	}

	@Override
	public Collection<Object> values() {
		Collection<Object> result = new ArrayList<Object>();
		for (String key : refMap.keySet()) {
			result.add(refMap.get(key).get(key));
		}
		return result;
	}

	@Override
	public void clear() {
		refMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return refMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		boolean result = false;
		for (Map<String, String> m : refMap.values()) {
			result = result || m.containsValue(value);
		}
		return result;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		for (String key : refMap.keySet()) {
			result.put(key, refMap.get(key).get(key));
		}

		return result.entrySet();
	}
}
