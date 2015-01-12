
// increase the round counter
round = round + 1;

// reset input values
contribution = '';

/*
 * matching (with a parameter list)
 * 
 * will select the appropriate matching for 1, 4, or 8 players
 * alternating between all available options 
 */ 
var matchings = new Array(8);

//for eight players
matchings[8] = [ 
        		[[1,2,3],[0,2,3],[0,1,3],[0,1,2],
        		 [5,6,7],[4,6,7],[4,5,7],[4,5,6]], 
	       ];
			   
// for four players
matchings[4] = [[ 
		[1,2,3],[0,2,3],[0,1,3],[0,1,2]
	       ]];

//for one player (plays with himself for testing)
matchings[1] = [[ 
		[ 0,0,0 ]
       ]];

selectmatching = (round-1) % matchings[agents.length].length; 

p1 = matchings[agents.length][selectmatching][agent][0];
p2 = matchings[agents.length][selectmatching][agent][1];
p3 = matchings[agents.length][selectmatching][agent][2];
