ClotheslineGUI {

	var <clothesline, <>name, <timeStep;
	var <window, <bounds, <margin, <userView, <overlayView;

	var <selectedIndex = 0, <currentTimeOffset = 0, currentZoom = 1;

	var <>passiveModeName = \______passive;
	var <>backgroundColor, <>foregroundColor, <>activatedColor, <>deactivatedColor, <>emphasisFillColor, <>passiveModeColor;
	var <>font, <>zoomRatio = 1.1, <>scrollRatio = 0.125, <>minNumLines = 5, <>lineHeight;
	var <>keepFront = true;  // if GUI opens in score, keep to front
	var <>keepCursorCentered = true;

	var currentEventArray, updateCache, viewWidth, viewHeight, scoreDuration, updateFunc;

	*new { |clothesline, name = "Clothesline", bounds, timeStep = 0.1|
		^super.new.init(clothesline, name, bounds, timeStep)
	}

	init { |argClothesline, argName, argBounds, argTimeStep, argMargin|

		clothesline = argClothesline;
		name = argName;
		bounds = argBounds ?? { Rect(20, 30, 800, 300) };
		margin = argMargin ?? { 30 @ 30 };
		timeStep = argTimeStep;

		backgroundColor = Color.black;
		foregroundColor = Color(1, 0.8, 0.2);
		deactivatedColor = foregroundColor.copy.alpha_(0.6);
		emphasisFillColor =  foregroundColor.copy.alpha_(0.3);
		activatedColor = Color(0, 1, 0.3);
		passiveModeColor = Color.white;
		font = Font(Font.defaultSansFace, 10);

		currentEventArray = [];
		scoreDuration = clothesline.lastTime * 1.2;

		this.makeWindow;
		this.initUpdate;
		this.updateCache;

	}

	makeWindow {
		var viewBounds;

		window = Window(name, bounds);
		window.front;
		window.background = backgroundColor;

		viewBounds = bounds.moveTo(0, 0).insetBy(margin.x, margin.y);
		viewWidth = viewBounds.width;
		viewHeight = viewBounds.height;

		userView = UserView(window, viewBounds);
		userView.resize = 5;
		userView.drawFunc = { this.drawScore };

		overlayView = UserView(window, viewBounds);
		overlayView.resize = 5;
		overlayView.frameRate = 1 / timeStep;
		overlayView.animate = true;
		overlayView.focus;

		overlayView.drawFunc = {
			this.drawOverlay;
			this.updateWindowTitle;
		};

		overlayView.mouseDownAction = { |view, x, y, modifier| this.mouseDown(x, y, modifier) };
		overlayView.keyDownAction = { |view, char, mod, x, y, code| this.keyDown(char, mod, x, y, code) };

		window.front;
		window.onClose = { this.free };
	}

	free {
		clothesline.stop;
		clothesline.stopScore;
		clothesline.changedAction.removeFunc(updateFunc);
	}

	initUpdate {
		updateFunc = {
			defer {
				this.updateCache;
				userView.refresh;
			}
		};

		clothesline.changedAction = clothesline.changedAction.addFunc(updateFunc);

	}

	updateCache {
		if(overlayView.notNil) {
			currentEventArray = clothesline.getScoreAsSortedArray;
			currentEventArray = currentEventArray.select { |event|
				this.timeToHorizontalPosition(event[\endTime]) > 0 and:
				{
					this.timeToHorizontalPosition(event[\startTime]) < viewWidth
				}
			};
			scoreDuration = clothesline.lastTime * 1.2;
		}
	}

	mouseDown { |x, y, modifier|
		var time = this.horizontalPositionToTime(x);
		var events, index;
		var isPassive = clothesline.currentSolo == passiveModeName;

		if(modifier.isAlt or: { isPassive }) {
			clothesline.jumpTo(time);
		} {
			/*events = clothesline.eventsCurrentAt(time);
			if(events.notEmpty) {
			index = currentEventArray.indexOf(events.first);
			if(index.notNil) { selectedIndex = index };
			}*/
			selectedIndex = this.eventIndexFor(y);
		};

		overlayView.refresh;
		userView.refresh;
	}

	togglePassiveMode {
		clothesline.toggleSolo(passiveModeName);
		clothesline.stop;
		userView.refresh;
		overlayView.refresh;
	}

	keyDown { |char, mod, x, y, code|
		var name, time;


		if(char == $ ) { this.togglePlay };
		if(char == $0) { this.centerCursor };

		if(char == $p) { this.togglePassiveMode };


		if(char == $o) {
			clothesline.openDocument(this.getSelectedName)
		};

		if(char == $c) {
			clothesline.closeDocument(this.getSelectedName)
		};

		if(char == $m) {
			clothesline.toggleMute(this.getSelectedName)
		};

		if(char == $u) {
			clothesline.unmute
		};

		if(char == $s) {
			clothesline.toggleSolo(this.getSelectedName)
		};

		if(char == $r) {
			name = this.getSelectedName;
			if(name.notNil and: { clothesline.eventIsPlaying(clothesline.score[name]) }) {
				clothesline.restartScore(name)
			};
		};

		if(char == $R) {
			clothesline.restartScore(*clothesline.getPlayingEvents.collect { |x| x[\name] })
		};


		if(char == $+) { this.zoomIn };
		if(char == $-) { this.zoomOut };
		if(char == $=) { this.zoomFull };

		// right arrow
		if(code == 16777236) {
			if(mod.isAlt) {
				clothesline.jumpToNextBreakpoint
			} {
				if(mod.isShift) {
					this.scrollRight
				} {
					clothesline.jumpBy(timeStep)
				}
			};
			if(keepFront) { window.front };
		};

		// left arrow
		if(code == 16777234) {
			if(mod.isAlt) {
				clothesline.jumpToPreviousBreakpoint
			} {
				if(mod.isShift) {
					this.scrollLeft
				} {
					clothesline.jumpBy(timeStep.neg)
				}
			};
			if(keepFront) { window.front };
		};

		// up arrow
		if(code == 16777235) {
			if(mod.isShift) {
				this.zoomIn
			} {
				this.switchFocus(selectedIndex - 1, mod.isAlt)
			}

		};

		// down arrow
		if(code == 16777237) {
			if(mod.isShift) {
				this.zoomOut
			} {
				this.switchFocus(selectedIndex + 1, mod.isAlt)
			}
		};
	}

	switchFocus { |index, switchDoc|
		var prevIndex = selectedIndex;

		selectedIndex = index % currentEventArray.size;

		if(switchDoc) {
			clothesline.switchDocument(this.getSelectedName(selectedIndex), this.getSelectedName(prevIndex));
		};

		userView.refresh;
	}

	getScrollTimeStep {
		^clothesline.breakpoints.last * scrollRatio / currentZoom
	}

	getLineHeight {
		^lineHeight ?? { overlayView.bounds.height / max(currentEventArray.size, minNumLines) }
	}

	timeToHorizontalPosition { |sec, zoom|
		var relativeTime = (sec  - currentTimeOffset) / scoreDuration;
		^relativeTime * viewWidth * (zoom ? currentZoom)
	}

	horizontalPositionToTime { |x, zoom|
		var relativePosition = x / viewWidth;
		var totalTime = scoreDuration;
		^relativePosition * totalTime / (zoom ? currentZoom) + currentTimeOffset
	}

	eventIndexFor { |y|
		^(y / this.getLineHeight).asInteger
	}

	togglePlay {
		if(clothesline.isPlaying) {
			clothesline.stop
		} {
			clothesline.play(startTime: clothesline.currentTime, timeStep: timeStep)
		}
	}

	getSelectedName { |index|
		var currentEvent = currentEventArray.clipAt(index ? selectedIndex);
		^currentEvent !? { currentEvent[\name] }
	}



	// GUI

	centerCursor { |doUpdateCache = true|
		currentTimeOffset = clothesline.currentTime * (1 - (1/currentZoom));
		if(doUpdateCache, updateCache);
		userView.refresh
	}

	zoomIn {
		var prevZoom = currentZoom;
		var time = clothesline.currentTime;
		var timeInView = time - currentTimeOffset;
		currentZoom = currentZoom * zoomRatio;
		timeInView = timeInView * (prevZoom / currentZoom);
		currentTimeOffset = time - timeInView;
		this.updateCache; // we use height only for what is visible
		userView.refresh
	}

	zoomOut {
		var prevZoom = currentZoom;
		var time = clothesline.currentTime;
		var timeInView = time - currentTimeOffset;
		currentZoom = max(currentZoom / zoomRatio, 1);
		timeInView = timeInView * (prevZoom / currentZoom);
		currentTimeOffset = max(time - timeInView, 0);
		this.updateCache;  // we use height only for what is visible
		userView.refresh
	}

	zoomFull {
		currentZoom = 1;
		currentTimeOffset = 0;
		this.updateCache;
		userView.refresh
	}

	scrollLeft {
		currentTimeOffset = max(currentTimeOffset - this.getScrollTimeStep, 0);
		this.updateCache;
		userView.refresh
	}

	scrollRight {
		currentTimeOffset = currentTimeOffset + this.getScrollTimeStep;
		this.updateCache;
		userView.refresh
	}

	updateWindowTitle {
		var time = clothesline.currentTime;
		var prevBreakpoint = clothesline.getPreviousBreakpoint(time) ? time;
		var inactive = clothesline.isInSoloMode;
		window.name = "% % | \"%\" | global time: % | local time: %".format(
			name,
			if(inactive) { "(inactive)" } { "" },
			this.getSelectedName,
			time.asTimeString,
			(time - prevBreakpoint).asTimeString,
		)
	}

	drawMarkerLine { |hx, hy, color|
		Pen.moveTo(hx @ 0);
		Pen.lineTo(hx @ hy);
		Pen.width = 1;
		Pen.strokeColor = color;
		Pen.stroke;
	}

	drawOverlay {
		var timePos;
		viewWidth = overlayView.bounds.width;
		viewHeight = window.view.bounds.height;
		timePos = this.timeToHorizontalPosition(clothesline.currentTime);
		if(clothesline.isPlaying and: { keepCursorCentered } and: { timePos * 1.3 > viewWidth }) {
			this.centerCursor(doUpdateCache: false);
			timePos = this.timeToHorizontalPosition(clothesline.currentTime);
		};
		this.drawMarkerLine(
			timePos,
			viewHeight,
			if(clothesline.isPlaying) { activatedColor } { foregroundColor }
		);

	}

	drawGrid {
		clothesline.defaultBreakpoints.do { |time|
			this.drawMarkerLine(
				this.timeToHorizontalPosition(time),
				viewHeight,
				deactivatedColor
			)
		};
		Pen.strokeColor = deactivatedColor;
		Pen.width = 1;
		Pen.stroke;
	}

	drawScore {
		var frange, xCenter, relativeCursor;
		var markerLine, soloIsOn, isPlaying, isPassive;
		var stringOffsetY, height;
		var stringColor, penColor;

		soloIsOn = clothesline.isInSoloMode;
		isPassive = clothesline.currentSolo == passiveModeName;
		isPlaying = clothesline.isPlaying;

		height = this.getLineHeight;
		font.pixelSize = (height * 0.75).clip(8, 14);
		stringOffsetY = height * 0.25;

		this.drawGrid;
		penColor = deactivatedColor;

		currentEventArray.do { |event, i|
			var startTime = event[\startTime];
			var endTime = event[\endTime];
			var name = event[\name];
			var x = this.timeToHorizontalPosition(startTime);
			var y = i * height;
			var width = this.timeToHorizontalPosition(endTime) - x;
			var rect = Rect(x, y, width, height).insetBy(0, 2);
			var isSelected = selectedIndex == i;

			if(soloIsOn) {
				if(isPassive.not) {
					if(clothesline.currentSolo == name) {
						if(clothesline.eventIsPlaying(event)) {
							penColor = stringColor = activatedColor;
						} {
							penColor = stringColor = foregroundColor;
						}
					} {
						penColor = stringColor = deactivatedColor;
					}
				} {
					penColor = stringColor = passiveModeColor;
				}
			} {
				if(clothesline.eventIsMuted(event)) {
					penColor = stringColor = deactivatedColor;
				} {
					if(clothesline.eventIsPlaying(event)) {
						penColor = stringColor = activatedColor;
					} {
						penColor = stringColor = foregroundColor;
					}
				}
			};

			Pen.addRect(rect);
			Pen.stringAtPoint(
				name,
				(rect.right + 4) @ (y + stringOffsetY),
				font,
				stringColor
			);

			Pen.strokeColor = penColor;
			Pen.width = 1;

			if(isSelected) {
				Pen.fillColor = if(isPassive) { passiveModeColor } { emphasisFillColor };
				Pen.fillStroke;
			} {
				Pen.stroke
			};

		};
	}


}