
// test


//-- INIT


//-- PLAY

(
Ndef(\xxx, {
	~time.kr.poll(label: "continuous time ");
});
);

(
Tdef(\xtest, {
	loop {
		"~currentTime: %".format(~currentTime).postln;
		0.1.wait;
	}
}).play
);




//-- STOP

Tdef(\xtest).stop;
Ndef(\xxx).stop(2);

//-- FREE

Ndef(\xxx).clear;
Tdef(\xtest).clear;

