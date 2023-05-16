

(
~makeClotheslineGUI = { |clothesline, name = "leuchtstoffraum", bounds, margin, lineHeight, timeStep = 0.1, keepFront = true, zoomRatio = 1.1|

	var currentEventArray = [], updateCache;
	var timeToHorizontalPosition, horizontalPositionToTime, eventIndexFor, getSelectedName, getLineHeight, getScrollTimeStep;
	var drawMarkerLine, updateWindowTitle;
	var togglePlay, openDocument, switchFocus, zoomIn, zoomOut, zoomFull, scrollLeft, scrollRight, centerCursor;
	var window, xmargin, ymargin, viewWidth, viewHeight;
	var selectedIndex = 0, currentTimeOffset = 0, currentZoom = 1;
	var userView, overlayView;
	var updateFunc;
	var passiveModeName = \______passive;


	// parameters
	var backgroundColor = Color.black;
	var foregroundColor = Color(1, 0.8, 0.2);
	var deactivatedColor = foregroundColor.copy.alpha_(0.6);
	var emphasisFillColor =  foregroundColor.copy.alpha_(0.3);
	var activeColor = Color(0, 1, 0.3);
	var scoreDuration = clothesline.lastEndTime ? 0 * 1.2;
	var minNumLines = 5;
	var scrollRatio = 1/8;
	var keepCursorCentered = true;

	var font = Font(Font.defaultSansFace, 10);



	margin = margin ?? { 30 @ 30 };
	xmargin = margin.x;
	ymargin = margin.y;
	bounds = bounds ?? { Rect(20, 30, 800, 300) };


	window = Window(name, bounds);
	window.front;
	window.background = backgroundColor;

	userView = UserView(window, Rect(0,0,window.bounds.width, window.bounds.height).insetBy(xmargin, ymargin).postcs);
	userView.resize = 5;

	updateFunc = {
		defer {
			updateCache.value;
			userView.refresh;
		}
	};
	clothesline.changedAction = clothesline.changedAction.addFunc(updateFunc);

	overlayView = UserView(window, Rect(0,0,window.bounds.width, window.bounds.height).insetBy(xmargin, ymargin));
	overlayView.resize = 5;
	overlayView.frameRate = 1 / timeStep;
	overlayView.focus;
	viewWidth = overlayView.bounds.width;
	viewHeight = overlayView.bounds.height;


	overlayView.mouseDownAction = { |view, x, y, modifier|
		var time = horizontalPositionToTime.(x);
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
			selectedIndex = eventIndexFor.(y);
		};

		overlayView.refresh;
		userView.refresh;
	};

	overlayView.keyDownAction = { |view, char, mod, x, y, code|
		var name, time;


		if(char == $ ) { togglePlay.value };
		if(char == $0) { centerCursor.value };

		if(char == $p) {
			clothesline.toggleSolo(passiveModeName);
			userView.refresh;
			overlayView.refresh;
		};

		if(char == $o) {
			clothesline.openDocument(getSelectedName.value)
		};

		if(char == $c) {
			clothesline.closeDocument(getSelectedName.value)
		};

		if(char == $m) {
			clothesline.toggleMute(getSelectedName.value)
		};

		if(char == $u) {
			clothesline.unmute
		};

		if(char == $s) {
			clothesline.toggleSolo(getSelectedName.value)
		};

		if(char == $r) {
			name = getSelectedName.value;
			if(name.notNil and: { clothesline.eventIsPlaying(clothesline.score[name]) }) {
				clothesline.restartScore(name)
			};
		};

		if(char == $R) {
			clothesline.restartScore(*clothesline.getPlayingEvents.collect { |x| x[\name] })
		};


		if(char == $+) { zoomIn.value };
		if(char == $-) { zoomOut.value };
		if(char == $=) { zoomFull.value };

		// right arrow
		if(code == 16777236) {
			if(mod.isAlt) {
				clothesline.jumpToNextBreakpoint
			} {
				if(mod.isShift) {
					scrollRight.value
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
					scrollLeft.value
				} {
					clothesline.jumpBy(timeStep.neg)
				}
			};
			if(keepFront) { window.front }; // if GUI opens in score, keep to front
		};

		// up arrow
		if(code == 16777235) {
			if(mod.isShift) {
				zoomIn.value
			} {
				switchFocus.(selectedIndex - 1, mod.isAlt)
			}

		};

		// down arrow
		if(code == 16777237) {
			if(mod.isShift) {
				zoomOut.value
			} {
				switchFocus.(selectedIndex + 1, mod.isAlt)
			}
		};


	};


	switchFocus = { |index, switchDoc|

		if(switchDoc) {
			clothesline.closeDocument(getSelectedName.value);
		};

		selectedIndex = index % currentEventArray.size;

		if(switchDoc) {
			clothesline.openDocument(getSelectedName.value);
		};

		userView.refresh;
	};

	getScrollTimeStep = {
		clothesline.breakpoints.last * scrollRatio / currentZoom
	};

	getLineHeight = {
		lineHeight ?? { overlayView.bounds.height / max(currentEventArray.size, minNumLines) };
	};

	timeToHorizontalPosition = { |sec, zoom|
		var relativeTime = sec  - currentTimeOffset / scoreDuration;
		relativeTime * viewWidth * (zoom ? currentZoom)
	};

	horizontalPositionToTime = { |x, zoom|
		var relativePosition = x / viewWidth;
		var totalTime = scoreDuration;
		relativePosition * totalTime / (zoom ? currentZoom) + currentTimeOffset
	};

	eventIndexFor = { |y|
		(y / getLineHeight.value).asInteger
	};

	togglePlay = {
		if(clothesline.isPlaying) {
			clothesline.stop
		} {
			clothesline.play(startTime: clothesline.currentTime, timeStep: timeStep)
		};
	};

	getSelectedName = {
		var currentEvent = currentEventArray.clipAt(selectedIndex);
		currentEvent[\name]
	};

	updateCache = {
		if(overlayView.notNil) {
			currentEventArray = clothesline.getScoreAsSortedArray;
			currentEventArray = currentEventArray.select { |event|
				timeToHorizontalPosition.(event[\endTime]) > 0 and:
				{
					timeToHorizontalPosition.(event[\startTime]) < viewWidth
				}
			};
		}
	};

	updateCache.value;

	// GUI

	centerCursor = { |doUpdateCache = true|
		currentTimeOffset = clothesline.currentTime * (1 - (1/currentZoom));
		if(doUpdateCache, updateCache);
		userView.refresh
	};

	zoomIn = {
		var prevZoom = currentZoom;
		var time = clothesline.currentTime;
		var timeInView = time - currentTimeOffset;
		currentZoom = currentZoom * zoomRatio;
		timeInView = timeInView * (prevZoom / currentZoom);
		currentTimeOffset = time - timeInView;
		updateCache.value; // we use height only for what is visible
		userView.refresh
	};

	zoomOut = {
		var prevZoom = currentZoom;
		var time = clothesline.currentTime;
		var timeInView = time - currentTimeOffset;
		currentZoom = max(currentZoom / zoomRatio, 1);
		timeInView = timeInView * (prevZoom / currentZoom);
		currentTimeOffset = max(time - timeInView, 0);
		updateCache.value;  // we use height only for what is visible
		userView.refresh
	};

	zoomFull = {
		currentZoom = 1;
		currentTimeOffset = 0;
		updateCache.value;
		userView.refresh
	};

	scrollLeft = {
		currentTimeOffset = max(currentTimeOffset - getScrollTimeStep.value, 0);
		updateCache.value;
		userView.refresh
	};

	scrollRight = {
		currentTimeOffset = currentTimeOffset + getScrollTimeStep.value;
		updateCache.value;
		userView.refresh
	};

	updateWindowTitle = {
		var time = clothesline.currentTime;
		var prevBreakpoint = clothesline.getPreviousBreakpoint(time);
		var inactive = clothesline.isInSoloMode;
		window.name = "% % | \"%\" | global time: % | local time: %".format(
			name,
			if(inactive) { "(inactive)" } { "" },
			getSelectedName.value,
			time.asTimeString,
			(time - prevBreakpoint).asTimeString,
		)
	};

	drawMarkerLine = { |hx, hy, color|
		Pen.moveTo(hx @ 0);
		Pen.lineTo(hx @ hy);
		Pen.width = 1;
		Pen.strokeColor = color;
		Pen.stroke;
	};

	overlayView.animate = true;

	overlayView.drawFunc = {
		var timePos;
		viewWidth = overlayView.bounds.width;
		viewHeight = window.view.bounds.height;
		timePos = timeToHorizontalPosition.(clothesline.currentTime);
		if(clothesline.isPlaying and: { keepCursorCentered } and: { timePos * 1.3 > viewWidth }) {
			centerCursor.value(doUpdateCache: false);
			timePos = timeToHorizontalPosition.(clothesline.currentTime);
		};
		drawMarkerLine.(
			timePos,
			viewHeight,
			if(clothesline.isPlaying) { activeColor } { foregroundColor }
		);
		updateWindowTitle.value;
	};

	userView.drawFunc = {
		var frange, xCenter, relativeCursor;
		var markerLine, soloIsOn, isPlaying, isPassive;
		var stringOffsetY, height;

		soloIsOn = clothesline.isInSoloMode;
		isPassive = clothesline.currentSolo == passiveModeName;
		isPlaying = clothesline.isPlaying;

		updateWindowTitle.(25 @ 10);
		height = getLineHeight.value;
		font.pixelSize = (height * 0.75).clip(8, 14);
		stringOffsetY = height * 0.25;

		clothesline.defaultBreakpoints.do { |time|
			drawMarkerLine.(
				timeToHorizontalPosition.(time),
				viewHeight,
				deactivatedColor
			)
		};


		currentEventArray.do { |event, i|
			var startTime = event[\startTime];
			var endTime = event[\endTime];
			var name = event[\name];
			var x = timeToHorizontalPosition.(startTime);
			var y = i * height;
			var width = timeToHorizontalPosition.(endTime) - x;
			var rect = Rect(x, y, width, height).insetBy(0, 2);
			var isSelected = selectedIndex == i;
			var stringColor, penColor;

			if(soloIsOn) {
				if(isPassive.not) {
					if(clothesline.currentSolo == name) {
						if(clothesline.eventIsPlaying(event)) {
							stringColor = activeColor;
							penColor = activeColor;
						} {
							stringColor = foregroundColor;
							penColor = foregroundColor;
						}
					} {
						penColor = stringColor = deactivatedColor;
					}
				} {
					penColor = stringColor = Color.white;
				}
			} {
				if(event[\muted] == true) {
					penColor = stringColor = deactivatedColor;
				} {
					if(clothesline.eventIsPlaying(event)) {
						stringColor = activeColor;
						penColor = activeColor;
					} {
						stringColor = foregroundColor;
						penColor = foregroundColor;
					}
				}
			};

			Pen.addRect(rect);
			Pen.stringAtPoint(
				name,
				(rect.right + 4) @ (y + stringOffsetY),
				font, // .pixelSize_(lineHeight),
				stringColor
			);

			// this fails to draw all strings:
			// Pen.stringInRect(name, rect, font, stringColor);


			Pen.strokeColor = penColor;
			Pen.fillColor = if(isSelected) {
				if(isPassive) { Color.white } { emphasisFillColor }
			} {
				backgroundColor
			};
			Pen.width = 1;

			Pen.fillStroke;

		};


	};

	window.onClose = {
		clothesline.stop;
		clothesline.stopScore;
		clothesline.changedAction.removeFunc(updateFunc);
	};



};
);
