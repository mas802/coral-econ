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
package coral.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import coral.model.ExpData;


@RunWith(JUnit4.class)
public class TestExpData {

	@Test
	public void testVarTypes() {
	    
	    ExpData data = new ExpData();
	    
	    // string
	    data.put("teststr", "A TEST STRING");
	    Object x = data.get("teststr");
	    System.out.println( "type: " + x.getClass() );
	    
	    
	    // int
	    data.put("testint", 5);
	    Object y = data.get("testint");
	    System.out.println( "type: " + y.getClass() );
	    
	    // long
	    data.put("testlong", Long.MAX_VALUE);
	    Object z = data.get("testlong");
	    System.out.println( "type: " + z.getClass() );

	    // double
	    data.put("testdouble", 0.3);
	    Object u = data.get("testdouble");
	    System.out.println( "type: " + u.getClass() );

	    // int arr
	    data.put("testintarr", new String[] { "1","2","3","4","5","5","8"});
	    u = data.get("testintarr");
	    System.out.println( "type: " + u.getClass() );
	    
	    // long arr
	    data.put("testlongarr", new Long[] {1l,2l,3l,4l,Long.MAX_VALUE});
	    u = data.get("testlongarr");
	    System.out.println( "type: " + u.getClass() );
	    
	    // string arr
	    data.put("teststrarr", new String[] {"1","2","3","45 str"});
	    u = data.get("teststrarr");
	    System.out.println( "type: " + u.getClass() );



	    // only works with java 1.7
	    // 				System.out.println(System.getProperty("java.version"));
	    // assertTrue(lines[0].equals("4.1"));
	    // assertTrue(lines[5].equals("1"));
	}
	
}
