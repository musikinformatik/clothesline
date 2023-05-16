
// test


//-- INIT


//-- PLAY

(
Ndef(\xxx, { |freq = 200|
	BPF.ar(
		WhiteNoise.ar(0.3 ! 2) + GrayNoise.ar(0.2),
		XLine.kr(0.5, 1, 4) * freq.lagud([1, 0.2, 0.5], [0.2, 0.6, 0.3]) * [[1, 1.2, 1.5]],
		0.01
	).flop.sum
}).play;
);

(
Tdef(\xtest, {
	loop {
		Ndef(\xxx).set(\freq, 600 + 50.0.rand2);
		0.2.rand.wait;
	}
}).play
);




//-- STOP

Tdef(\xtest).stop;
Ndef(\xxx).stop(2);

//-- FREE

Ndef(\xxx).clear;
Tdef(\xtest).clear;

