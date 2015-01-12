
contribution1 = agents[p1].getLong("contribution");
contribution2 = agents[p2].getLong("contribution");
contribution3 = agents[p3].getLong("contribution");

pot = contribution + contribution1 + contribution2 + contribution3;

payoff = endowment - contribution + (pot * redistFactor);

if ( round == payoffround ) {
	finalpayoff = payoff;
}