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
package coral.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A utility class that facilitates to provide custom matchings.
 * 
 * @author Markus Schaffner
 * 
 */
public class Matching {

    protected final static Log logger = LogFactory.getLog(Matching.class);

    Integer[][] groups;
    Integer[][] roles;

    Integer[] mygroup;
    Integer[] myrole;

    // make sure nobody can instantiate this on its own
    private Matching() {
    }

    /**
     * Provide a matching for a N participants into groups of groupsize. The
     * seeds will iterate over different matchings and the algorithm produces
     * stranger matchings for subsequent itseeds WHEN possible (with a boundary
     * at seed%groupsize. The initialisation will fail if N%groupsize != 0, i.e.
     * N needs to be a multiple of groupsize.
     * 
     * @param N
     * @param groupsize
     * @param itseed
     *            Note that the seed needs to be the same for all participants
     *            for a given matchings
     * @return
     */
    public static Matching make(int N, int groupsize, int popseed,
            int roleseed, int itseed) {
        Matching m = new Matching();

        int noRoles = groupsize;
        int noGroups = N / groupsize;

        // indexer that maps integers to participants
        Integer[] populationIndexer = new Integer[N];
        // indexers that maps integers to roles
        Integer[] roleIndexer = new Integer[noRoles];
        // indexer that maps indexes to
        Integer[][] roleposIndexer = new Integer[noRoles][noGroups];

        for (int i = 0; i < N; i++) {
            populationIndexer[i] = i;
        }

        for (int i = 0; i < noRoles; i++) {
            roleIndexer[i] = i;
        }

        for (int i = 0; i < noRoles; i++) {
            for (int j = 0; j < noGroups; j++) {
                roleposIndexer[i][j] = j;
            }
        }

        // SHUFFLE
        // change order in population according to seed
        if (popseed > 0)
            populationIndexer = shuffle(populationIndexer, popseed);
        if (roleseed > 0)
            roleIndexer = shuffle(roleIndexer, roleseed);
        if ( itseed > noGroups ) {
            // make sure the seed only changes after all stranger iterations
            int sitseed = itseed / noGroups;
            for (int i = 0; i < noRoles; i++) {
                roleposIndexer[i] = shuffle(roleposIndexer[i], sitseed
                        * noRoles + i);
            }
        }

        // System.out.println(itseed);
        // System.out.println( Arrays.deepToString(populationIndexer));
        // System.out.println( Arrays.deepToString(roleIndexer));
        // System.out.println( Arrays.deepToString(roleposIndexer));
         

        //
        // compute the matching with the given shuffled indexers
        //
        m.mygroup = new Integer[N];
        m.myrole = new Integer[N];
        m.roles = new Integer[noRoles][noGroups];
        m.groups = new Integer[noGroups][noRoles];

        //
        // give a role to an agent and
        // map correctly in m.roles
        //
        for (int i = 0; i < N; i++) {
            int agent = populationIndexer[i];
            int role = roleIndexer[i / noGroups];

            int rolepos = roleposIndexer[role][i % noGroups];

            m.roles[role][rolepos] = agent;
            m.myrole[agent] = role;
        }

        // System.out.println( "roles: " + Arrays.deepToString(m.roles));

        // change order in role depending on itseed
        int iteration = itseed;
        for (int g = 0; g < noGroups; g++) {
            for (int p = 0; p < noRoles; p++) {
                Integer[] activerole = m.roles[p];

                int index = (g + p * iteration) % noGroups;
                m.groups[g][p] = activerole[index];
                m.mygroup[activerole[index]] = g;
            }
        }

        // System.out.println( "groups: " + Arrays.deepToString(m.groups));


        /*
         * System.out.println( Arrays.deepToString(m.population));
         * System.out.println( i + " - " + noGroups + " - " + groupsize + " - "
         * + (i / noGroups) +" - "+ i % noGroups);
         */
        return m;
    }

    // returns the group for a given agent
    public int group(int agent) {
        return mygroup[agent];
    }

    // returns the role of a given agent
    public int role(int agent) {
        return myrole[agent];
    }

    // returns a matrix of rolemembers by rolenumber
    public Integer[][] roles() {
        return roles;
    }

    // returns a matrix of groupsmembers by groupnumber
    public Integer[][] groups() {
        return groups;
    }

    // returns a group members by groupid
    public Integer[] members(int group) {
        return groups[group];
    }

    // returns group members by agents
    public Integer[] partners(int agent) {
        int g = mygroup[agent];
        Integer[] members = members(g);
        Integer[] result = new Integer[members.length - 1];
        int counter = 0;
        for (int i = 0; i < members.length; i++) {
            if (members[i] != agent) {
                result[counter++] = members[i];
            }
        }
        return result;
    }

    // UTIL Methods

    /**
     * Shuffle the array for N==2 will produce alternating behaviour
     * 
     * @param in
     * @param seed
     * @return
     */
    public static Integer[] shuffle(Integer[] in, long seed) {
        int n = in.length;

        Integer[] out;

            List<Integer> l = Arrays.asList(in);
            Collections.shuffle(l, new Random((seed*48271)%2147483647));
            out = l.toArray(new Integer[n]);
        

        return out;
    }

    // http://stackoverflow.com/questions/7918806/finding-n-th-permutation-without-computing-others
    public static Integer[] ithPermutation(Integer[] in, long permutations) {

        int n = in.length;

        Integer[] out = new Integer[n];
        int k = 0;
        long[] fact = new long[in.length];
        long[] perm = new long[in.length];

        // compute factorial numbers
        fact[k] = 1;
        while (++k < n) {
            fact[k] = fact[k - 1] * k;
        }

        // compute factorial code
        for (k = 0; k < n; ++k) {
            if (fact[n - 1 - k] == 0) {
                System.out.println("now");
            }
            perm[k] = permutations / fact[n - 1 - k];
            permutations = permutations % fact[n - 1 - k];
        }

        // readjust values to obtain the permutation
        // start from the end and check if preceding values are lower
        for (k = n - 1; k > 0; --k) {
            for (int j = k - 1; j >= 0; --j) {
                if (perm[j] <= perm[k]) {
                    perm[k]++;
                }
            }
        }

        // print permutation
        for (k = 0; k < n; ++k) {
            int i = (int) perm[k];
            out[k] = in[i];
        }

        return out;
    }
}
