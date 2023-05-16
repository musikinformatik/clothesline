
// put up a new clothesline and schedule some items
(
~player = Clothesline.new;
~player.addAllScores("".resolveRelative);

~player.schedAllArrays([
	[\this, 1.0, 4],
	[\more, 2.5, 5.0],
	[\more, 7.5, 8.0, \more2], // alias
	[\that, 3.0, 4.5],
	[\those, 0.5, 3.0],
	[\more, 3.5, 8.0, \more3], // alias
	[\cult_of_q, 0.5, 2.0] + 4 // addition moves the item
] * 3); // multiplication stretches the line
)


// now we can jump around in it
~player.jumpTo(2.0);
~player.jumpTo(4.2);
~player.jumpTo(1.0);

// or play it from the beginning
~player.play(0.0);

// visualize it
ClotheslineGUI(~player, "Wäscheleine");





// put up a new clothesline and schedule some items
(
"../Classes/init-GUI.scd".resolveRelative.load;

~player = Clothesline.new;
~player.addAllScores("".resolveRelative);

~player.schedAllArrays([
	[\this, 1.0, 4],
	[\more, 2.5, 5.0] + 4,
	[\more, 7.5, 8.0, \more2], // alias
	[\that, 3.0, 4.5],
	[\those, 0.5, 3.0],
	[\more, 3.5, 8.0, \more3], // alias
	[\cult_of_q, 0.5, 2.0] + 4

] * 3);

~player.defaultBreakpoints = (0,2..24);

ClotheslineGUI(~player, "Wäscheleine");
);



// reschedule while open
(
~player.schedAllArrays([
	[\this, 2.0, 4] + 3,
	[\more, 0.5, 5.0] + 4.0.rand,
	[\that, 3.0, 4.5],
] * rrand(0.5, 1.5));
)

// moving in the timeline

(
Tdef(\x, {
	var endTime = ~player.lastEndTime;
	inf.do {
		~player.jumpTo(endTime.rand);
		2.0.rand.wait;
	}
}).play
)

// clear
~player.clear;

// dynamic score
(
Tdef(\x, {
	~player.jumpTo(5);
	inf.do {
		var randomScore = [[\this, 1.0, 4],
			[\more, 2.5, 5.0] + 4,
			[\more, 7.5, 8.0, \more2], // alias
			[\that, 3.0, 4.5],
			[\those, 0.5, 3.0],
			[\more, 3.5, 8.0, \more3], // alias

		].scramble.keep(4) * rrand(0.4, 1.2) + 4.0.rand;
		~player.schedAllArrays(randomScore);
		if(0.1.coin) { ~player.jumpTo(~player.lastEndTime.rand) };
		0.2.rand.wait
	}
}).play;
)

Tdef(\x).stop;
~player.jumpTo(0);
~player.clear;


// breakpoints
~player.defaultBreakpoints = { [1, 2, 3].choose }.dup(8).integrate.sort;


// with this we can test a bit if scheduling works under load.
// try with key "p" to stop and play this again and again
~player.schedAllArrays([[\heavy, 0.3, 3]]);


// stress test
(
var n = 90;
"../Classes/init-GUI.scd".resolveRelative.load;

~player = Clothesline.new;
~player.addAllScores("".resolveRelative);

~player.schedAllArrays({ |i|
	var start = rrand(1, 60);
	[\more, start, start + rrand(1, 60), i.asSymbol] } ! n
);
//~player.defaultBreakpoints = [0, 30, 60];
ClotheslineGUI(~player, "Wäscheleine", bounds: Rect(30, 30, 400, 800), lineHeight: nil, timeStep: 0.1);

)



// reschedule test
(
~player.schedAllArrays({ |i|
	[\that, rrand(1, 60), rrand(1, 60), i.asSymbol] } ! n
);
)

~player.breakpoints.keep(8)

~player.playScore(*(0..29).keep(8).collect { |x| x.asSymbol });




// old version




// put up a new clothesline and schedule some items
(
"../Classes/init-GUI.scd".resolveRelative.load;

~player = Clothesline.new;
~player.addAllScores("".resolveRelative);

~player.schedAllArrays([
	[\this, 1.0, 4],
	[\more, 2.5, 5.0] + 4,
	[\more, 7.5, 8.0, \more2], // alias
	[\that, 3.0, 4.5],
	[\more_more, 0.5, 3.0]
]);

~player.defaultBreakpoints = [0, 2, 4, 6, 8, 10];

~makeClotheslineGUI.(~player, "Wäscheleine");
);
