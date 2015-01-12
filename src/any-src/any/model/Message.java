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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import any.Linker;

public class Message {

	String cmd;
	byte[] content;

	private Map<String, String> query = null;

	public Message(String cmd) {
		this( cmd, new byte[] {} );
	}

	public Message(String cmd, byte[] data) {
		this.cmd = cmd;
		this.content = data;
	}

	public String getScheme() {
		int i = cmd.indexOf(':');
		return cmd.substring(0, i);
	}

	public String getFullContent() {
		String host = getHost();

		int i = cmd.indexOf(':');

		if (host != null) {
			i = cmd.indexOf('/', i + 3);
		}
		return cmd.substring(i + 1);
	}

	public String getContent() {
		String a = getFullContent();
		int i = a.indexOf('?');
		return (i > 0) ? a.substring(0, i) : a;
	}

	public Map<String, String> getQuery() {
		if (query == null) {
			String a = getFullContent();
			int i = a.indexOf('?');
			query = new HashMap<String, String>();
			if (i >= 0) {
				String q = a.substring(i+1);
				String[] terms = q.split("&");
				for (String term : terms) {
					String[] args = term.split("=");
					if (args.length == 2) query.put(args[0], args[1]);
				}
			}
		}
		return query;
	}

	public byte[] getData() {
		// TODO Auto-generated method stub
		return this.content;
	}

	public Message(String method, String viewID, String type, String show,
			byte[] content) {
		this(method + ":/" + viewID + "?type=" + type + "&show=" + show,
				content);
	}

	public Message(String method, String viewID, String type, String show,
			String target, byte[] content) {
		this(method + ":/" + viewID + "?type=" + type + "&show=" + show
				+ "&target=" + target, content);
	}

	public byte[] toSent() {
		
		if (content == null)
			content = new byte[] {};
		ByteBuffer bb = ByteBuffer.allocate(65000 + content.length);
		byte[] str = content;

		String s = null;
		try {
			s = URLEncoder.encode(cmd, "utf-8");
			s = cmd; // FIXME
			bb.put(s.getBytes());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		bb.put((byte) Linker.SEPCHAR);
		long len = str.length;

		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			b[7 - i] = (byte) (len >>> (i * 8));
		}

		bb.put(b);
		bb.put((byte) Linker.SEPCHAR);
		bb.put(str);

		byte[] sbytes = Arrays.copyOfRange(bb.array(), 0, bb.position());
		// logger.info("send out:"+str.length+"\n" ); //+ out.toString());
		return sbytes;
	}

	public String getHost() {
		int st = cmd.indexOf("://") + 3;
		if (st > 3) {
			int end = cmd.indexOf("/", st);
			end = (end > 0) ? end : cmd.length();
			return cmd.substring(st, end);
		} else {
			return null;
		}
	}

}
