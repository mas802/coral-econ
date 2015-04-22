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
package coral.data;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import coral.model.ExpData;

public interface DataService {

	// FIXME INSERT not sure what to fix
	public abstract void saveOETData(String collection, ExpData stage);

	// SELECT
	public abstract Map<String, String> getMap(String collection, long id)
			throws SQLException;

	// ALL DATA
	public abstract List<String[]> getAllData() throws SQLException;

	// SELECT
	// public abstract List<String[]> getKeyValue(String collection, long id)
	//		throws SQLException;

	// SELECT
	// public abstract Object[] getMaps(String collection, String name,
	//		String value) throws SQLException;

	// SELECT
	public abstract ExpData retriveState(String collection, long id);

	// SELECT
	boolean retriveData(String collection, ExpData data);
	
	// SELECT
	public abstract List<String[]> stageInfos();

	// SELECT
	// public abstract List<String[]> getTableWithId(String by, String who,
	//		String... cols);

	// SELECT 
	public abstract int getNewId(int min);

	// UTIL
	public abstract void close();

	// UTIL
	public abstract Object[][] stageInfo();

	// SELECT
    public abstract List<String> getAllVariableNames() throws SQLException;


}