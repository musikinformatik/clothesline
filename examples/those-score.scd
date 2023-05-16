
// test


//-- INIT
(
SynthDef(\sinmore2, {
	OffsetOut.ar(0,
		Env.sine(0.1, \amp.kr(0.1)).ar(doneAction: 2) * SinOsc.ar(\freq.kr * [1, 1.2]))
}).add
);
s.sync;

//-- PLAY

(
Tdef(\xmoremore, {
	var dt = 0.04 * { rrand(1, 3) / rrand(3, 7) }.dup.sum;
	var mul = rrand(2, 5);
	loop {
		(instrument: \sinmore2, note: rrand(-3, 10) * mul, octave: 5, amp: 0.01).play;
		dt.wait;
	}
}).play
);




//-- STOP

Tdef(\xmoremore).stop;

//-- FREE

Tdef(\xmoremore).clear;
//SynthDef.removeAt(\sinmore); // if we do this, we get SynthDef not founds at the very end.