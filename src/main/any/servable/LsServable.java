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
package any.servable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import any.Linker;
import any.TblUtil;
import any.model.Message;
import any.model.Servable;


public class LsServable implements Servable {

	protected final Log logger = LogFactory.getLog(this.getClass());


	public void process(Message cmd, BlockingQueue<Message> outQueue) {
		System.out.println("LS SERVABLE");
		process(cmd.getScheme(), cmd.getFullContent(), outQueue);
	}

	// public List<Extras> run(BufferedReader in, PrintStream out) {
	//
	// StringBuilder sb = new StringBuilder();
	//
	// String line;
	//
	// try {
	// while ((line = in.readLine()) != null) {
	// System.out.println(line);
	// if (line.startsWith("END")) break;
	// sb.append(line);// + "\n");
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// System.out.println("GET PROJECT --" +sb.toString().trim()+"--");
	//
	// File root = new File(sb.toString());
	//
	// FilenameFilter filter = new FilenameFilter() {
	//
	// @Override
	// public boolean accept(File f, String name) {
	// return name.endsWith(".txt") || name.endsWith(".html")
	// || name.endsWith(".xml") || name.endsWith(".xhtml")
	// || name.endsWith(".java") || name.endsWith(".c")
	// || name.endsWith(".cpp") || name.endsWith(".c++")
	// || name.endsWith(".m") || name.endsWith(".bat")
	// || name.endsWith(".sh") || name.endsWith(".h")
	// || name.endsWith(".do") || name.endsWith(".tex");
	// }
	// };
	//
	// // get all files in path
	// List<File> filelist = new ArrayList<File>();
	//
	// recurseList(root, filelist, filter);
	//
	// // print each file in a newline
	// for( File f : filelist) {
	// System.out.println(f.getName()+","+f.getAbsolutePath());
	// out.println(f.getName()+","+f.getAbsolutePath()+","+f.length());
	// }
	//
	// // finished
	//
	// out.close();
	//
	// System.out.println("SENT PROJECT");
	// return null;
	//
	// }

	// private void recurseList(File dir, List<File> list, FilenameFilter
	// filter) {
	// System.out.println(dir.getAbsolutePath() + " : "+ dir.isDirectory());
	// for (File child : dir.listFiles()) {
	// if (child.isDirectory()) {
	// recurseList(child, list, filter);
	// } else {
	// if (filter.accept(dir, child.getName())) {
	// list.add(child);
	// }
	// }
	// }
	// }

	Map<String, String> resSet = new HashMap<String, String>();
	DateFormat df = DateFormat.getDateInstance();

	public void process(String cmd, String content,
			BlockingQueue<Message> outQueue) {
		logger.info("start LS SERVABLE");
		String filename = content.substring(0, content.length());

		File file = new File(filename);

		try {

			File[] list = file.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File arg0, String name) {
					return !(name.startsWith(".") && !(name.endsWith("~")));
				}
			});

			StringBuilder sb = new StringBuilder();

			
			// parent
			File fp = file.getParentFile();

			logger.debug("do file: " + file + " - list: " + list + " fp:" + fp);

			if (fp != null) {
				TblUtil tbl = new TblUtil();

				// DISPLAYTEXT
				tbl.set(TblUtil.DISPLAYTEXT, "[..] " + fp.getName());

				// DETAILTEXT
				tbl.set(TblUtil.DETAILTEXT, "UP");

//				// ICONRES
//				try { 
//					JFileChooser ch = new JFileChooser();
//					Icon icon = ch.getIcon(fp);
//
//					BufferedImage offscreen = new BufferedImage(
//							icon.getIconHeight(), icon.getIconWidth(),
//							BufferedImage.TYPE_4BYTE_ABGR);
//					icon.paintIcon(null, offscreen.getGraphics(), 0, 0);
//
//					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					ImageIO.write(offscreen, "png", baos);
//					baos.flush();
//					byte[] imageInByte = baos.toByteArray();
//					baos.close();
//
//					String strrep = new String(imageInByte);
//					if (!resSet.containsKey(strrep)) {
//
//						System.out.println(fp.getName() + ": "
//								+ icon.toString() + " " + icon.hashCode());
//
//						outQueue.put(new Message("res", icon.toString(),
//								"image/png", "NO", imageInByte));
//						resSet.put(strrep, icon.toString());
//						tbl.set(TblUtil.ICONRES, icon.toString());
//					} else {
//						tbl.set(TblUtil.ICONRES, resSet.get(strrep));
//					}
//				} catch (Error e) {
//					// TODO
//					System.err.println("Exception due to icon caught");
//					// e.printStackTrace();
//				}

				// TABCMD
				String tcmd = ((fp.isDirectory() ? "ls://host" : "get://host") + fp
						.getAbsolutePath());
				tbl.set(TblUtil.TABCMD, tcmd);

				// DETAILCMD
				tbl.set(TblUtil.DETAILCMD, "tmpl:" + tcmd);

				// DELETECMD

				sb.append(tbl.makeCell());

			}

			logger.debug("do list: " + list);

			if (list != null) {
				for (File f : list) {

					logger.debug("do file: " + f);

					TblUtil tbl = new TblUtil();

					// DISPLAYTEXT
					tbl.set(TblUtil.DISPLAYTEXT, f.getName());

					// DETAILTEXT
					tbl.set(TblUtil.DETAILTEXT, (f.isDirectory() ? " --      "
							: humanReadableByteCount(f.length(), true))
							+ " - "
							+ df.format(f.lastModified()));

//					// ICONRES
//					try {
//						JFileChooser ch = new JFileChooser();
//						Icon icon = ch.getIcon(f);
//
//						BufferedImage offscreen = new BufferedImage(
//								icon.getIconHeight(), icon.getIconWidth(),
//								BufferedImage.TYPE_4BYTE_ABGR);
//						icon.paintIcon(null, offscreen.getGraphics(), 0, 0);
//
//						ByteArrayOutputStream baos = new ByteArrayOutputStream();
//						ImageIO.write(offscreen, "png", baos);
//						baos.flush();
//						byte[] imageInByte = baos.toByteArray();
//						baos.close();
//
//						String strrep = new String(imageInByte);
//						if (!resSet.containsKey(strrep)) {
//
//							System.out.println(f.getName() + ": "
//									+ icon.toString() + " " + icon.hashCode());
//
//							outQueue.put(new Message("res", icon.toString(),
//									"image/png", "NO", imageInByte));
//							resSet.put(strrep, icon.toString());
//							tbl.set(TblUtil.ICONRES, icon.toString());
//						} else {
//							tbl.set(TblUtil.ICONRES, resSet.get(strrep));
//						}
//
//					} catch (Error e) {
//						// TODO
//						System.err.println("Exception due to icon caught");
//						// e.printStackTrace();
//					}

					
					String fullpath = f.getAbsolutePath();
					if ( !fullpath.startsWith("/") ) {
						fullpath = "/"+fullpath;
					}
					
					// TABCMD
					String tcmd = ((f.isDirectory() ? "ls://host"
							: "get://host") + fullpath);
					tbl.set(TblUtil.TABCMD, tcmd);

					// DETAILCMD
					tbl.set(TblUtil.DETAILCMD, "tmpl:" + tcmd);

					// DELETECMD

					sb.append(tbl.makeCell());
				}

				outQueue.put(new Message("vset", file.getName(), TblUtil.TYPE,
						"YES", "1", sb.toString().getBytes()));

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			outQueue.add(new Message("vset", "_error", "text/plain", "YES",
					result.toString().getBytes()));
//		} catch (IOException e) {
//			e.printStackTrace();
//			final Writer result = new StringWriter();
//			final PrintWriter printWriter = new PrintWriter(result);
//			e.printStackTrace(printWriter);
//			outQueue.add(new Message("vset", "_error", "text/plain", "YES",
//					result.toString().getBytes()));
		} catch (NullPointerException e) {
			e.printStackTrace();
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			outQueue.add(new Message("vset", "_error", "text/plain", "YES",
					result.toString().getBytes()));
		}
		
		logger.info("finished LS SERVABLE");
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		String r;
		if (bytes < unit) {
			r = bytes + " B ";
		} else {
			int exp = (int) (Math.log(bytes) / Math.log(unit));
			String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
					+ (si ? "" : "i");
			r = String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}
		int g = (8 - r.length());
		for (int i = 0; i <= g; i++) {
			r = r + " ";
		}
		return r;
	}
	
	@Override
	public void init(Properties initstr, BlockingQueue<Message> loopQueue, Linker linker) {
	}

	@Override
	public void disconnect(BlockingQueue<Message> outQueue) {
		// default, ignore 
	}

}
