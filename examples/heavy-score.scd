



//-- INIT
(
SynthDef(\heavy, { |out|
	var n = 1200;
	var sig = {
		var freqs = { |i|  (0..i).nthPrime.scramble.keep(8) }.dup(n).flat.as(Set).as(Array);
		var amps = { 0.4.rand }.dup(freqs.size);
		var dts = { 2.4.rand }.dup(freqs.size);
		freqs = freqs.normalize(200, 10000) * rrand(0.5, 1.5);
		Klank.ar(`[freqs, amps, dts], Dust.ar([4, 14, 40], [1, 0.4, 0.1] * 0.002))
	}.dup(2);
	Out.ar(out, sig)
}).add
);

s.sync;

//-- PLAY

~synth = Synth(\heavy);



//-- STOP

~synth.free;

//-- FREE
