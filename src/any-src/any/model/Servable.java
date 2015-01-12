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
package any.model;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import any.Linker;

public interface Servable {

	/**
	 * Process an incoming Message and submit appropriate responses
	 * to the outQueue (can by asynchroneous)
	 * 
	 * @param cmd The incomming Message
	 * @param outQueue Queue to add responding Messages to (can be retained for async use)
	 */
	public void process(Message cmd, BlockingQueue<Message> outQueue);

	/**
	 * Initialise the Servable with the appropriate properties 
	 * 
	 * @param properties
	 * @param loopQueue the local loop queue to send calls back for dispatch
	 * @param linker TODO
	 */
	public void init(Properties properties, BlockingQueue<Message> loopQueue, Linker linker);

	/**
	 * disconnect notification
	 */
	public void disconnect( BlockingQueue<Message> outQueue);

}
