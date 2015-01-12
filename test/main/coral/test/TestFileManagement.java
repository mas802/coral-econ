package coral.test;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestFileManagement {

	@Test
    public void testWaitForFile() throws InterruptedException, IOException {
	}
	/*
	@Test
    public void testWaitForFile() throws InterruptedException, IOException {
        
		File f = new File("testwait.txt");
    	
		if ( f.exists() ) {
			assertTrue(f.delete());
		}
		
		ExpServable testHandler = new ExpServable();

		Properties p = new Properties();
		
		// p.setProperty("coral.exp.stages", "test/testwait.csv");
		p.setProperty("exp.stagesfile", "test/testwait.csv");
		p.setProperty("coral.db.mode", "mem");
		
    	testHandler.init(p, null);
    	
    	ArrayBlockingQueue<Message> client1 = new ArrayBlockingQueue<Message>(1000);
    	ArrayBlockingQueue<Message> client2 = new ArrayBlockingQueue<Message>(1000);
    	ArrayBlockingQueue<Message> client3 = new ArrayBlockingQueue<Message>(1000);
    	
    	testHandler.process(new Message("/" + IExpService.START_KEY, new byte[] {}), client1);
    	testHandler.process(new Message("/" + IExpService.START_KEY, new byte[] {}), client2);
    	testHandler.process(new Message("/" + IExpService.START_KEY, new byte[] {}), client3);

    	testHandler.process(new Message("/?test=1", new byte[] {}), client1);
    	testHandler.process(new Message("/?test=1", new byte[] {}), client2);
    	testHandler.process(new Message("/?test=1", new byte[] {}), client3);
        
    	Thread.sleep(200);
    	System.out.println( "currentcap after 200ms:" + client1.remainingCapacity() );

    	// FIXME should this be 998?
    	// assertEquals(998, client1.remainingCapacity());
    	
    	f.createNewFile();
    	
    	Thread.sleep(200);
    	System.out.println( "currentcap after 200ms after File:" + client1.remainingCapacity() );
    	
    	// assertEquals(998, client1.remainingCapacity());

    	while ( client1.remainingCapacity() < 1000 ) {
    		Message m = client1.poll();
    		System.out.println();
    		System.out.println(m.getContent());
    		System.out.println(new String(m.getData()));
    	}
    	
		assertTrue(f.delete());

    }
    */
}
