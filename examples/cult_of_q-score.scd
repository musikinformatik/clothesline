
// for the members of the cult of q
// who don't want to ~tilde


//-- INIT
q = q ? ();

q.local = currentEnvironment;

q.local.pattern = Pbind(\note, Pbrown(0, { 17.rand + 1 } ! 12), \dur, 0.05, \amp, 0.01);


//-- PLAY

q.local.player = q.local.pattern.play;




//-- STOP

q.local.player.stop;

//-- FREE

q.local = nil;



