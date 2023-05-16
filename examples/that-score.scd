
// test


//-- INIT

SynthDef(\thatblip, {
	OffsetOut.ar(0,
		Env.perc(0.001, 0.1, \amp.kr(0.1)).ar(doneAction: 2)
		*
		Blip.ar(\freq.kr, 20)
	)
}).add;

s.sync;

//-- PLAY

(
Tdef(\xthat, {
	loop {
		(instrument: \thatblip, note: rrand(-3, 10), octave: [5, 6], amp: 0.01).play;
		0.02.wait;
	}
}).play
);






//-- STOP

Tdef(\xthat).stop;

//-- FREE

Tdef(\xthat).clear;
//SynthDef.removeAt(\sinmore); // if we do this, we get SynthDef not founds at the very end.