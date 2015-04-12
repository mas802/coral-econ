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
package coral;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import any.Linker;
import any.model.Message;
import any.servable.VsetServable;
import coral.utils.CoralUtils;

/**
 * A subclass of PolypVsetServable that adds a sidebar and prepares for server
 * duty
 * 
 * TODO could also add a CLI (?)
 * 
 * @author mas
 * 
 */
public class CoralHeadServable extends VsetServable {

    protected final Log logger = LogFactory.getLog(this.getClass());

    private Browser sidebrowser;

    @Override
    public void init(Properties properties, BlockingQueue<Message> loopQueue,
            Linker linker) {

        shell = (Shell) properties.get("shell");

        if (!properties.containsKey("coral.head.res")) {
            try {
                res = File.createTempFile("coral", "server");
                res.delete();
                res.mkdirs();
            } catch (IOException e1) {
                throw new RuntimeException(
                        "problem creating temporary directory for polyp, please specify with coral.polyp.res",
                        e1);
            }
        } else {
            res = new File(properties.getProperty("coral.head.res"),
                    "server_res/");
            res.mkdirs();
        }

        mainfilename = properties.getProperty("coral.head.main", "main.html");

        String sidebarfilename = properties.getProperty("coral.head.sidebar",
                "servervm/sidebar.html");

        File dir = new File(properties.getProperty("exp.basepath", "./"));
        File sidebarfile = new File(dir, sidebarfilename);

        String sidebartext = "<html><a href='" + CoralUtils.getHostStr()
                + CoralUtils.SERVER_KEY + "/info.vm'>SERVER</a></html>";
        if (sidebarfile.exists()) {
            try {
                sidebartext = new Scanner(sidebarfile).useDelimiter("\\Z")
                        .next();
            } catch (FileNotFoundException e) {
                logger.warn("Could not read sidebar file " + sidebarfilename, e);
            }
        }

        try {
            sidebrowser = new Browser(shell, SWT.NONE);
        } catch (SWTError e) {
            logger.warn("Could not instantiate sidebar Browser: ", e);
            throw new RuntimeException("Could not instantiate Browser", e);
        }

        super.setup();

        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        sidebrowser.setText(sidebartext);
        sidebrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true,
                1, 1));

        sidebrowser.addLocationListener(getLocationListener());

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 5;
        gridLayout.makeColumnsEqualWidth = true;
        shell.setLayout(gridLayout);

        // Shell shell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);
        // shell.setLayout(new FillLayout());
        // shell.setBounds(Display.getDefault().getPrimaryMonitor().getBounds());

        // shell.setBounds(20,20,1044,788);
        // shell.open();

        // res.mkdirs();

        logger.debug("ready");
    }

}