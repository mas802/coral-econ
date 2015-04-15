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

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import coral.model.ExpData;
import coral.service.ExpServiceImpl;
import coral.service.ExpTemplateUtil;


@RunWith(JUnit4.class)
public class TestPassingVar {

	@Test
	public void testPassingVar() {
	    
	    ExpTemplateUtil tu = new ExpTemplateUtil("./");
	    
	    
	    ExpServiceImpl service = new ExpServiceImpl(null, new Properties(), null);
	    
	    service.addClient(0);
	    service.addClient(1);
	    
	    ExpData data1 = service.getData(0);
	    
	    data1.put("partner", 1);
	    data1.put("passvarstr", "firstvar");
	    
	    ExpData data2 = service.getData(1);
	    
	    data2.put("partner", 0);
	    data2.put("passvarstr", "secondvar");
	    
	    	    
	    tu.eval( "test/main/passvars.js", data1, null, service);

	    System.out.println( data1.get("passvarstr"));
	    System.out.println( data2.get("passvarstr"));

	    System.out.println( data1.get("passedvarstr"));
	    System.out.println( data2.get("passedvarstr"));

	    System.out.println( data1.get("testcheck"));
	    System.out.println( data2.get("testcheck"));

	}
	
}
