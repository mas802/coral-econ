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
package any;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class TblUtil {

	public static final char TBLSEPCHAR = ',';

	public static int DISPLAYTEXT = 0;
	public static int DETAILTEXT = 1;
	public static int ICONRES = 2;
	public static int TABCMD = 3;
	public static int DETAILCMD = 4;
	public static int DELETECMD = 5;

	public static String TYPE = "text/anytable";

	private String[] content = new String[6]; 

	public void set(int i,String s) {
		content[i] = s;
	}
	
	public String makeCell() {
		StringBuilder sb = new StringBuilder();
		
		try {
			if (content[0]!=null) sb.append(URLEncoder.encode(content[0],"utf-8"));
			for (int i = 1;i<content.length;i++) {
				sb.append(TBLSEPCHAR);
				if ( content[i]!=null ) {
					sb.append(URLEncoder.encode(content[i],"utf-8"));
			
				}
			}
			sb.append("\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
//		System.out.println("tbl cell: " + sb.toString());
		return sb.toString();
	}
	
}
