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

import java.util.Map;

import coral.model.ExpData;

public interface IExpService {

    public void process(Integer id, String msg);

    public void process(Integer id, ExpData data, Map<String,Object> args);

    public ExpData getData(Integer id);

    public boolean isDebug();

    public void evalTemplate(Integer id, String filename);

    public void addClient(Integer clientCount);

    public void removeClient(Integer id);
}
