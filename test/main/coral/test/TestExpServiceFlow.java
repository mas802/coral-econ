package coral.test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import coral.data.DataService;
import coral.model.ExpData;
import coral.model.ExpStage;
import coral.service.ExpHandler;
import coral.service.ExpServiceImpl;
import coral.service.IExpService;
import coral.utils.CoralUtils;

@RunWith(JUnit4.class)
public class TestExpServiceFlow {

	@Test
    public void testSkipbackWithCondition() throws InterruptedException, IOException {
        
        ExpServiceImpl service = new ExpServiceImpl(new TestHandler(), new Properties(), new TestDataService());

        List<ExpStage> stages = new ArrayList<ExpStage>();
        stages.add(new ExpStage("__START", 0, 0));
        stages.add(new ExpStage("test1", 0, 0));
        stages.add(new ExpStage("test2", 0, 0, new String[] {"testfalse"}, new String[] {}, "" ));
        stages.add(new ExpStage("test3", 0, 0));
	    
        service.initWithStages(stages);

        service.addClient(0);
        
        service.process(0, CoralUtils.START_KEY);
        Assert.assertEquals( "test1", service.getData(0).get("template"));
        service.process(0, "?");
        Assert.assertEquals( "test3", service.getData(0).get("template"));
        service.process(0, "&skipback=true");
        Assert.assertEquals( "test1", service.getData(0).get("template"));
	
	}
	
	class TestDataService implements DataService {

        @Override
        public void saveOETData(String collection, ExpData stage) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Map<String, String> getMap(String collection, long id)
                throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String[]> getAllData() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ExpData retriveState(String collection, long id) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean retriveData(String collection, ExpData data) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public List<String[]> stageInfos() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getNewId(int min) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object[][] stageInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getAllVariableNames() throws SQLException {
            // TODO Auto-generated method stub
            return null;
        }
	    
	}
	
	class TestHandler implements ExpHandler {

        @Override
        public void broadcast(Integer id, String msg) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void wait(Integer id, String mode, int stage, int loop) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public IExpService getService() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<Integer> getServiceIds() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ArrayList<Map<String, Object>> getClientInfoMapList() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void sendRes(Integer id, String... res) {
            // TODO Auto-generated method stub
            
        }
	    
	}
}
