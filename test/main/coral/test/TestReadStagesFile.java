package coral.test;

import java.io.File;
import java.util.List;

import org.junit.Assert;

import org.junit.Test;

import coral.model.ExpStage;
import coral.utils.CoralUtils;

public class TestReadStagesFile {

    
    @Test
    public void testReadFlatStagesFile() {
	
	List<ExpStage> stages = CoralUtils.readStages(new File("test/main/stagesflat.csv"), null);
	
	Assert.assertEquals( 5, stages.size());
	
	// TODO further tests here
    }
        
    @Test
    public void testReadIncludeStagesFile() {
	
	List<ExpStage> stages = CoralUtils.readStages(new File("test/main/stagesinclude.csv"), null);
	
	Assert.assertEquals( 5, stages.size());
	
	System.out.println(stages.get(0).getTemplate());
	System.out.println(stages.get(1).getTemplate());
	System.out.println(stages.get(2).getTemplate());
	System.out.println(stages.get(3).getTemplate());
	System.out.println(stages.get(4).getTemplate());
	
	// TODO further tests here
    }    
}
