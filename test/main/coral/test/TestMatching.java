package coral.test;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import coral.utils.Matching;

public class TestMatching {

    @Test
    public void testMatchingIteration() {

        int repetition = 100;
        
        for (int N = 10; N <= 100; N++) {
            for (int G = 3; G < 20; G++) {
                if (N % G == 0 && N > G) {
                    int R = N/G;
                    int[][] myroles = new int[N][G];
                    int[][] mygroups = new int[N][R];
                    int[][] mypartners = new int[N][N];
                    
                    System.out.println("number of participants: " + N
                            + " groupsize: " + G + " groups: " + (N / G));
                    for (int seed = 0; seed < repetition; seed++) {
                        Matching m;
                        m = Matching.make(N, G, 0, 0, seed);
                        // System.out.println("groups ("+N+"): " + Arrays.deepToString(m.groups()));
                        
                        for ( int agent = 0; agent<N; agent++ ) {
                            // check group
                            Integer g = m.group(agent);
                            //System.out.println("a:"+agent+" members:" + Arrays.deepToString(m.members(g)));

                            // check partners
                            // System.out.println("a:"+agent+" partners:" + Arrays.deepToString(m.partners(agent)));
                            
                            Integer r = m.role(agent);
                            Integer[] partners = m.partners(agent);
                            for ( int i = 0; i<partners.length; i++) {
                                int partner = partners[i];
                                assertTrue( "others (" +agent+ "," +partner+ ") need differnt roles " + Arrays.deepToString(partners), r != m.role(partner) );
                                assertTrue("others need to be in the same group", g == m.group(partner) );
                                mypartners[agent][partner]++;
                            }
                            
                            // stats 
                            myroles[agent][r]++;
                            mygroups[agent][g]++;
                        }
                    }
                    
                    // check stats
                    for ( int agent = 0; agent<N; agent++ ) {
                        /*
                        */
                        boolean show = false;
                        System.out.println("agent: " + agent);
                        int[] mr = myroles[agent];
                        System.out.println("roles: " + Arrays.toString(mr));
                        System.out.println("groups: " + Arrays.toString(mygroups[agent]));
                        System.out.println("partners: " + Arrays.toString(mypartners[agent]));                        

                        
                        for ( int i = 0; i<G; i++ ) {
                            assertTrue("roles need to be 0 or repetition ", (myroles[agent][i] == 0 || myroles[agent][i] == repetition) );
                        }
                        
                        for ( int i = 0; i<N; i++ ) {
                            boolean test = (mypartners[agent][i] == 0 || Math.abs(mypartners[agent][i]) < repetition);
                            
                            if ( !test ) {
                                System.out.println("partners: " + Arrays.deepToString(mypartners));                        
                                System.out.println("groups: " + Arrays.deepToString(mygroups));                        
                                System.out.println("roles: " + Arrays.deepToString(myroles));                     
                            }
                            assertTrue("partners need to be 0 or rep/R " +(repetition/R)+" - " + mypartners[agent][i], test );
                        }
                        
                        for ( int i = 0; i<R; i++ ) {
                            if (( Math.abs(mygroups[agent][i]-(repetition/R)) > (12*repetition)/100) ) {
                                show = true;
                                // System.out.println( "bad a: " + agent + " g:" + i + " ist: " + mygroups[agent][i]+" soll: "+(repetition/R) + " diff: " + Math.abs(mygroups[agent][i]-(repetition/R)) );
                             } else {
                                 // System.out.println( "good a: " + agent + " g:" + i );

                             }
                             assertTrue("groups need to be 0 or rep/G " +(repetition/R)+" - " + mygroups[agent][i], (mygroups[agent][i] == 0 || Math.abs(mygroups[agent][i]) < repetition ) );

                        }
                        if ( show ) {
                            //System.err.println("look at this");
                        }
                    }
                }
            }
        }
    }

    
    @Test
    public void testMatchingAbsolutStranger() {

        int N = 12;
        int G = 3;
        int seed = 0;

        System.out.println("number of participants: " + N + " groupsize: " + G
                + " groups: " + (N / G));

        Matching m;
        m = Matching.make(N, G, 0, 0, seed);
        System.out.println(Arrays.deepToString(m.groups()));
    }


    @Test
    public void testShuffle() {
        
        int count = 0;
        int ccount = 0;
        for ( int i = 0; i<2000; i++) {
        Integer[] out = Matching.shuffle(new Integer[] {0,1}, i);
        
        // System.out.println(Arrays.toString(out));
        if ( out[0] == 1) count++;
        if ( out[1] == 1) ccount++;
        }
        
        System.out.println(count);
        System.out.println(ccount);
        assertTrue("need at least one shuffle " + count, count>0);
        assertTrue("need at least one shuffle " + ccount, ccount>0);

    }

    @Test
    public void testPerm() {
        Integer[] group = new Integer[10];
        for (int i = 0; i < 10; i++) {
            group[i] = i;
        }

        Integer[] result;
        result = Matching.ithPermutation(group, 3628799);

        System.out.println(Arrays.deepToString(result));
    }

}
