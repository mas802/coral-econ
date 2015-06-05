package coral;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import any.IConnection;
import any.Linker;

@Deprecated
public class CoralRobot extends Linker {

	public CoralRobot(Properties prop) {
		super(prop);
	}

	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException,
			FileNotFoundException, IOException {

		Display display = Display.getDefault();
		Shell shell = new Shell( display ); // , SWT.NO_TRIM | SWT.ON_TOP);

		// Shell shell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
//		gridLayout.makeColumnsEqualWidth = true;

		shell.setLayout(gridLayout);		
		shell.setBounds(Display.getDefault().getPrimaryMonitor().getBounds());

		for ( int i = 0; i < 9; i++ ) {

			System.out.println("get " + i);
			
			Properties p1 = new Properties();
	
			p1.put("shell", shell);
			p1.put("coral.polyp.res", "res" + i + "/");//+ (int)(Math.random() * 99999)+ "/");
			p1.put("coral.polyp.robot", "robot.js");

			
			for( String a : args ) {
				String[] sp = a.split("=");
				p1.put(sp[0], sp[1]);
			}
			
			// Start the main thread
			final CoralRobot s = new CoralRobot(p1);

 			PolypVsetServable vset = new PolypVsetServable();
			vset.init(p1, null, null);
			s.getServableMap().put("vset", vset);

			s.connect("host", "localhost", 43802);
			
			new Thread() {
				public void run() {
					IConnection loop = s.getNamedCon().get("loop");
					s.getServableMap().get("vset").process(null, loop.getOutQueue());
				}
			}.start();
		} 


		// shell.setBounds(20,20,1044,788);
		shell.open();
		
		// FIXME swt legacy
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

	}

}
