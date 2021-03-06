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

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


public interface ExpHandler {

	public void broadcast(Integer id, String msg);
	
	public void wait(Integer id, String mode, int stage, int loop);

	public IExpService getService();
	
	public Set<Integer> getServiceIds();

	public ArrayList<Map<String,Object>> getClientInfoMapList();

	void sendRes(Integer id, String... res);
	
}
