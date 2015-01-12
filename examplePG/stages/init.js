
// initialise values
var round = 0;
var endowment = 10;
var redistFactor = 1.6/4;

// initialise values that need to be set later to -99 to easily spot errors
var finalpayoff = -99;

// this selects a different random round 
// as payoff round for each participant
var payoffround = Math.floor(Math.random()*9)+1;
