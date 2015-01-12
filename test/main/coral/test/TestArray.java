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
import coral.service.ExpTemplateUtil;


@RunWith(JUnit4.class)
public class TestArray {

	@Test
	public void testRunTemplates() {
	    
	    ExpTemplateUtil tu = new ExpTemplateUtil("./");
	    
	    ExpData data = new ExpData();
	    
	    System.out.println("arraytest.js");
	    String output = tu.eval( "test/main/arraytest.js", data, null, null);
	    System.out.println(output);
	    
	    System.out.println("arraytest2.js: ");
	    // tu.eval( "test/main/arraytest2.js", data, null, null);
	    
	    System.out.println("arraytest.vm");
	    String template = tu.eval( "test/main/arraytest.vm", data, null, null);
	    
	    String[] lines = template.split("\n");
	    
	    System.out.println(template);

	    // only works with java 1.7
	    // 				System.out.println(System.getProperty("java.version"));
	    // assertTrue(lines[0].equals("4.1"));
	    // assertTrue(lines[5].equals("1"));
	}
	
}
