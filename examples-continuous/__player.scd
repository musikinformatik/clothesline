
// put up a new clothesline and schedule some items
(
~player = ContinuousClothesline.new;
~player.addAllScores("".resolveRelative);

~player.schedAllArrays([
	[\a, 1.0, 4],
]);
ClotheslineGUI(~player, "Wäscheleine");

)

