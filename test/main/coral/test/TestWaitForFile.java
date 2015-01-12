package coral.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestWaitForFile {

	@Test
    public void testWaitForFile() throws InterruptedException, IOException {
        
		File f = new File("test/test.vm");
    	
		// Desktop.getDesktop().edit(f);
    }
}
