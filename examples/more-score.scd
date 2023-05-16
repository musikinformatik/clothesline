
// test


//-- INIT

SynthDef(\sinmore, { OffsetOut.ar(0, Env.perc(0.001, 0.1, \amp.kr(0.1)).ar(doneAction: 2) * SinOsc.ar(\freq.kr * [1, 1.1])) }).add;
s.sync;

//-- PLAY

(
~xmore.stop; // for live coding, stop any old one.
~xmore = Task {
	var dt = [1/8, 1/32, 1/16, 1/4, 1/64].choose * { rrand(1, 3) / rrand(3, 7) }.dup.sum;
	var chord = (0..11).scramble.keep(3);
	loop {
		(instrument: \sinmore, note: rrand(-3, 3) * chord, octave: 5, amp: 0.01).play;
		dt.wait;
	}
}.play
);




//-- STOP

~xmore.stop;

//-- FREE
