/*

Arrange code on a timed clothesline
----,---;------,---------,---,-----

- score elements are uniquely identified by their file name
- each element has init/play/stop/free sections that are called as necessary
- code outside these sections is ignored but can be used to live code
- each element may maintain its own environment

*/

Clothesline {

	var <score, <breakpoints;
	var <currentTime, <player;
	var <>reader, <>fileNameEnding="-score";
	var <capturedCurrentEnvironment;
	var <currentSolo;
	var <>verbose = true, <>tolerant = true;
	var <defaultBreakpoints = #[0];
	var <>changedAction;

	var scheduleIndex;

	*new {
		^super.new.init
	}

	init {
		score = IdentityDictionary.new;
		currentTime = 0;
		scheduleIndex = {:x, x<-(0..)};
		reader = ClotheslineReader;
		// for reliability, we capture the environment here
		capturedCurrentEnvironment = currentEnvironment;
		capturedCurrentEnvironment[\clothesline] = this; // access self
	}

	// file reading

	addScore { |path, callInContext = true|
		var event = reader.parseScore(path);
		var name;
		if(event.notNil) {
			name = path.basename;
			name = name.replace(fileNameEnding ++ ".scd", "");
			name = name.asSymbol;
			if(callInContext) { this.addCallContextToEvent(event) };
			this.put(name, event);
		}
	}

	addAllScores { |path, callInContext = true|
		var paths;
		if(path.endsWith("/").not) { path = path ++ "/" };
		path =  "%*%.scd".format(path, fileNameEnding);
		paths = path.pathMatch;
		if(paths.isEmpty) { "No files found in this path: \n'%'".format(path.cs).warn };
		paths.do { |path| this.addScore(path, callInContext) };
	}

	// scheduling
	// aliases allow to add the same file multiple times.

	sched { |name, startTime, endTime, alias|
		var isRunning, shouldBeRunning, end;
		var event;

		if(tolerant) {
			if(startTime > endTime) {
				end = endTime;
				endTime = startTime;
				startTime = end;
			}
		};

		// later we can optimize this a little to schedule a bunch and then do the play update
		this.doWithPlayUpdate {
			event = if(alias.isNil) { this.getEvent(name) } { this.getAliasEvent(name, alias) };
			event[\startTime] = startTime;
			event[\endTime] = endTime;
			event[\indexInSchedule] = event[\indexInSchedule] ?? { scheduleIndex.next };
			this.updateBreakpoints;
		};

		changedAction.value(this, [event], \scheduled);

		^event

	}

	getEvent { |name|
		var event, err;
		err = "event with name '%' not found (maybe forgot to add '%'?)";
		event = score[name];
		if(event.isNil) { Error(err.format(name, fileNameEnding)).throw };
		^event
	}

	getAliasEvent { |name, alias|
		var event, err;
		err = "event with name '%' not found (maybe forgot to add '%'?)";
		event = score[alias];
		if(event.isNil) {
			event = score[name];
			if(event.isNil) { Error(err.format(name, fileNameEnding)).throw };
			event = event.copy;
			event[\sourceName] = name;
			event[\name] = alias;
			event[\indexInSchedule] = scheduleIndex.next;
			if(event[\callContext].notNil) { this.addCallContextToEvent(event) };
			this.put(alias, event);
		};
		^event
	}

	addCallContextToEvent { |event|
		event[\callContext] = Environment(parent: capturedCurrentEnvironment, know: true)
	}

	addMissingEndTimes { |arrays|
		var last = arrays.lastIndex;
		arrays.size.do { |i|
			var endTime = arrays[i][2];
			if(endTime.isNil) {
				endTime = if(i == last) { arrays[i][1] + 1.0 } { arrays[i+1][1] };
				arrays[i] = arrays[i].instill(2, endTime)
			}
		}
	}

	schedAllArrays { |arrays|
		this.addMissingEndTimes(arrays);
		arrays.do { |each|
			if(each.size < 3) { Error("scheduling data format is wrong: %".format(each)).throw };
			this.sched(*each)
		}
	}


	// access

	at { |name|
		^score[name]
	}

	put { |name, event|
		var prevEvent = score[name];
		var hotSwap = false;
		var doc;

		if(prevEvent.notNil) {
			hotSwap = this.eventIsPlaying(prevEvent, currentTime);
			if(hotSwap) { this.stopScore(name) };
			if(event[\startTime].isNil) { event[\startTime] = score[name][\startTime] };
			if(event[\endTime].isNil) { event[\endTime] = score[name][\endTime] };
		};
		event[\name] = name;
		score[name] = event;

		if(hotSwap) { this.playScore(name) };

		if(verbose) {
			if(prevEvent.notNil) {
				"replaced event '%' from path %".format(name, event[\path]).postln
			} {
				"added event '%' from path %".format(name, event[\path]).postln
			}
		};

		doc = Document.allDocuments.detect { |x| x.path == event[\path] };
		if(doc.notNil) { this.linkDocumentToEvent(doc, event) };
	}

	// TODO: to make playScore part of the public interface,
	// we need to know if an event is really playing and wasn't stopped by cmd-.

	playScore { |...names|
		var events = this.getEvents(names);
		events = events.reject { |event| this.eventIsPlaying(event, currentTime) };
		this.playEvents(events);
	}

	stopScore { |...names|
		var events = this.getEvents(names);
		events = events.select { |event| this.eventIsPlaying(event, currentTime) };
		this.stopEvents(events);
	}

	restartScore { |...names|
		var events = this.getEvents(names);
		this.stopEvents(events);
		this.playEvents(events);
	}

	playEvents { |events|
		events = this.sortEvents(events, \startTime);
		events.do { |event|
			event[\runningTask].stop;
			event[\runningTask] = fork {
				this.updateEventIfNeeded(event);
				this.callInContext({
					event[\INIT].value;
					event[\PLAY].value;
				}, event);
				event[\runningTask] = nil;
				changedAction.value(this, events, \started);
			}
		};

		if(events.notEmpty) {
			if(verbose) {
				"started: %".format(this.sortEvents(events, \name, \zzz) .collect { |e| e[\name] }).postln
			};
		};
	}

	stopEvents { |events|
		events = this.sortEvents(events, \endTime);
		events.do { |event|
			var task;
			event[\runningTask].stop;
			event[\runningTask] = fork {
				this.updateEventIfNeeded(event);
				this.callInContext({
					event[\STOP].value;
					event[\FREE].value;
				}, event);
				event[\runningTask] = nil;
				changedAction.value(this, events, \stopped);
			};
		};

		if(events.notEmpty) {
			if(verbose) {
				"stopped: %".format(this.sortEvents(events, \name, \zzz).collect { |e| e[\name] }).postln
			};
		};


	}


	// helpers

	callInContext { |func, event|
		var callContext = event[\callContext];
		if(callContext.notNil) {
			callContext.use(func)
		} {
			func.value
		}
	}

	needsUpdate { |event|
		^File.mtime(event[\path]) != event[\modificationTime]
	}

	updateEventIfNeeded { |event|
		var newEvent, path;
		if(this.needsUpdate(event)) {
			path = event[\path];
			newEvent = reader.parseScore(path);
			if(newEvent.notNil) {
				event.putAll(newEvent);
			} {
				"file update failed: %".format(path).throw
			}
		}
	}



	// schedule information

	firstEventScheduled {
		^this.scheduledEvents.minItem { |e| e[\startTime] }
	}

	lastEventScheduled {
		^this.scheduledEvents.maxItem { |e| e[\endTime] }
	}

	firstStartTime {
		var event = this.firstEventScheduled;
		^event !? { event[\startTime] }
	}

	lastEndTime {
		var event = this.lastEventScheduled;
		^event !? { event[\endTime] }
	}

	lastTime {
		 ^max(this.lastEndTime ? 0, defaultBreakpoints.last)
	}

	eventIsScheduled { |event|
		^event[\startTime].notNil
	}

	eventIsCurrent { |event, time|
		var start = event[\startTime];
		var end = event[\endTime];
		if(start.isNil or: { end.isNil }) { ^false };
		^this.timeIsInRange(time, start, end)
	}

	eventIsPlaying { |event, time|
		time = time ? currentTime;
		if(this.isInSoloMode) {
			^event[\name] == currentSolo and: { this.eventIsCurrent(event, time) }
		};
		if(event[\muted] == true) { ^false };
		^this.eventIsCurrent(event, time)
	}

	eventIsMuted { |event|
		^event[\muted] == true
	}

	timeIsInRange { |atTime, startTime, endTime|
		^startTime.notNil and: { atTime >= startTime }
		and: {
			endTime.isNil or: { atTime < endTime }
		}
	}

	scheduledEvents {
		^score.select(this.eventIsScheduled(_)).values
	}

	eventsThatPlayAt { |time|
		var events = this.scheduledEvents;
		^events.select { |e| this.eventIsPlaying(e, time) }
	}

	eventsCurrentAt { |time|
		var events = this.scheduledEvents;
		^events.select { |e| this.eventIsCurrent(e, time) }
	}

	sortEvents { |events, sortKey, default = inf|
		var f = { |e| e[sortKey] ? default };
		^events.sort { |a, b| f.(a) < f.(b) }
	}

	namesDo { |names, func|
		if(names.asArray.isEmpty) {
			score.do(func);
			^this
		};
		names.do { |name|
			var event = score[name];
			if(event.isNil) { "no event for this name: %".format(name).warn; ^this };
			func.value(event)
		}
	}

	getScoreAsSortedArray { |sortKey = \indexInSchedule|
		^this.sortEvents(this.scheduledEvents, sortKey)
	}

	getPlayingEvents {
		^this.eventsThatPlayAt(currentTime)
	}

	getEvents { |names|
		var events = Array.new;
		this.namesDo(names, { |event| events = events.add(event) });
		^events
	}


	// muting and solo


	mute { |...names|
		this.doWithPlayUpdate({
			this.namesDo(names, {|event| event[\muted] = true })
		})
	}

	unmute { |...names|
		this.doWithPlayUpdate({
			this.namesDo(names, { |event| event[\muted] = nil })
		})
	}

	toggleMute { |...names|
		this.doWithPlayUpdate({
			this.namesDo(names, { |event|
				event[\muted] = if(event[\muted] == true) { nil } { true }
			})
		})
	}

	solo { |name|
		this.doWithPlayUpdate({ currentSolo = name })
	}


	unsolo {
		this.doWithPlayUpdate({ currentSolo = nil })
	}

	toggleSolo { |name|
		if(this.isInSoloMode) { this.unsolo } { this.solo(name) }
	}

	isInSoloMode {
		^currentSolo.notNil
	}

	removeEventFromSchedule { |event|
		event[\startTime] = nil;
		event[\endTime] = nil;
	}

	clear {
		this.doWithPlayUpdate({
			score.do { |event|
				this.removeEventFromSchedule(event)
			};
			this.updateBreakpoints;
			changedAction.value(this, [], \cleared);
		})
	}

	doWithPlayUpdate { |func|
		var nowRunning, shouldBeRunning;
		nowRunning = this.getPlayingEvents;
		func.value;
		shouldBeRunning = this.getPlayingEvents;
		this.stopEvents(nowRunning difference: shouldBeRunning);
		this.playEvents(shouldBeRunning difference: nowRunning);
	}



	// navigation

	jumpTo { |time|
		var shouldRun, runningNow;

		if(time.isNil) { "nowhen to jump to (time is nil)".postln; ^this };

		runningNow = this.eventsThatPlayAt(currentTime);
		shouldRun = this.eventsThatPlayAt(time);

		this.stopEvents(runningNow difference: shouldRun);
		this.playEvents(shouldRun difference: runningNow);

		currentTime = time;

	}

	jumpBy { |delta|
		this.jumpTo(currentTime + delta)
	}

	moveTo { |time, previousTime|
		this.jumpTo(time)
	}


	// breakpoints

	defaultBreakpoints_ { |array|
		defaultBreakpoints = array.collect { |x| x.asFloat };
		this.updateBreakpoints;
	}

	jumpToNextBreakpoint {
		this.jumpTo(this.getNextBreakpoint(currentTime))
	}

	jumpToPreviousBreakpoint {
		this.jumpTo(this.getPreviousBreakpoint(currentTime) ? 0)
	}

	getNextBreakpoint { |time|
		var i;
		^breakpoints !? {
			i = breakpoints.indexOfGreaterThan(time);
			i !? { breakpoints[i] }
		}
	}

	getPreviousBreakpoint { |time|
		var i;
		^breakpoints !? {
			i = breakpoints.indexOfGreaterThan(time);
			^if(i.isNil) {
				if(time == breakpoints.last) {
					breakpoints.clipAt(breakpoints.lastIndex - 1)
				} {
					breakpoints.last
				}
			} {
				breakpoints.clipAt(i - 2)
			}
		}
	}

	updateBreakpoints {
		var b = defaultBreakpoints.as(IdentitySet);
		[\startTime, \endTime].do { |key|
			score.do { |event|
				var val = event[key];
				val !? { b.add(val.asFloat) };
			}
		};
		breakpoints = b.as(Array).sort
	}


	// playing

	isPlaying { ^player.isPlaying }
	stop { player.stop; player = nil; }
	pause {  if(player.isNil) { "not playing".warn } { player.pause }}
	resume { if(player.isNil) { "not playing".warn } { player.resume } }

	play { |startTime = 0, endTime = inf, clock, timeStep = 0.1|
		this.stop;
		player = Task {
			var next, dt;
			this.jumpTo(startTime);
			while {
				next = this.getNextBreakpoint(currentTime);
				currentTime < endTime
			} {
				dt = if(next.notNil) { min(next - currentTime, timeStep) } { timeStep };
				this.moveTo(currentTime + dt, currentTime);
				dt.wait;
			};

		}.play(clock);
		^player

	}


	// code access
	// sometimes this may open the wrong document, maybe due to an issue in SCIDE
	// the paths are correct, mostly it works ...

	openDocument { |name|
		var event = score[name];
		var path, doc;
		var updateFunc;

		if(event.notNil) {
			path = event[\path];
			if(path.notNil) {
				doc = Document.open(path);
				this.linkDocumentToEvent(doc, event);
				doc.front;
			} {
				"no path in event:\n%".format(event).warn;
			}
		} {
			"no score event for that name: '%'".format(name).warn
		};
		^doc
	}

	closeDocument { |name|
		var event = score[name];
		var doc;
		if(event.notNil) {
			doc = event[\document];
			if(doc.notNil) {
				doc.close;
				event[\document] = nil;
			};
		} {
			"no score event for that name: '%'".format(name).warn
		}
	}

	switchDocument { |name, prev|
		var event = score[name];
		var oldEvent = score[prev];
		// this check is needed because if they happen to be the same
		// there is some race condition in the IDE
		if(event[\path] != oldEvent[\path]) {
			this.closeDocument(prev);
		};
		this.openDocument(name)
	}

	// still missing, if you hand-open document after score is scheduled, this won't do
	// also: when clearing the schedule, the document functions need to be unlinked

	linkDocumentToEvent { |doc, event|
		event[\document] = doc;
		if(event[\callContext].notNil) {
			doc.toFrontAction = {
				currentEnvironment = event[\callContext];
			};
			doc.endFrontAction = {
				// this may override an environment that was hand-pushed
				// in the meantime. think about this later
				currentEnvironment = capturedCurrentEnvironment;
			};
			if(verbose) {
				"linked score event '%' envir to open document '%'".format(event[\name], doc.title).postln;
			};
		};
		doc.onClose_({ event[\document] = nil });
	}



	postTimeline {
		var lineLength = 80;
		"\n\n".postln;
		{ |i| (i+1).asString.padLeft(10, ".") }.dup(lineLength div: 10).join.postln;
		score.values.do { |e|
			e.use {
				var t0 = ~startTime * 10 - 1;
				var t1 = ~endTime - ~startTime * 10 - 1;
				"-> % (from: % to: % (dur: %)".format(~name, ~startTime, ~endTime, ~endTime - ~startTime).postln;
				t0.do { |i| " ".post }; "|".post;
				t1.do { |i| ".".post }; "|".post;
				"".postln;
			};
		};
		"\n\n".postln;
	}

	codeAsString {
		var stream = CollStream(String.new);
		breakpoints.do { |time|
			var startingEvent, endingEvent, makeParentheses, nothingToDo, hasStarts, hasEnds;
			startingEvent = this.scheduledEvents.select { |event| event[\startTime] == time };
			endingEvent = this.scheduledEvents.select { |event| event[\endTime] == time };

			hasStarts = startingEvent.notEmpty;
			hasEnds = endingEvent.notEmpty;


			if(hasStarts or: hasEnds) { stream << "// %\n".format(time.asTimeString) };
			if(hasStarts and: hasEnds) { stream << "(\n" };
			if(startingEvent.notEmpty) {
				stream << "Clothesline.default.playScore(%);\n".format(startingEvent.collect { |event| event[\name].asCompileString }.join(", "))
			};
			if(endingEvent.notEmpty) {
				stream << "Clothesline.default.stopScore(%);\n".format(endingEvent.collect { |event| event[\name].asCompileString }.join(", "))
			};
			if(hasStarts and: hasEnds) { stream << ")" };
			stream << "\n\n";
		};
		^stream.collection
	}

	codeDoc { |title = "clothesline score"|
		^Document.new(title, this.codeAsString, capturedCurrentEnvironment)
	}


}

ContinuousClothesline : Clothesline {

	var <timeProxy;

	*new { |server|
		^super.new.init(server)
	}

	init { |server|
		super.init;
		timeProxy = NodeProxy.control(server, 1);
		capturedCurrentEnvironment[\time] = timeProxy;
	}

	jumpTo { |time|

		if(time.isNil) { "nowhen to jump to (time is nil)".postln; ^this };

		timeProxy.fadeTime = 0;
		timeProxy.source = time;

		super.jumpTo(time)

	}

	moveTo { |time, previousTime|
		timeProxy.fadeTime = if(previousTime.notNil) { time - previousTime } { 0 };
		timeProxy.source = time;
		super.jumpTo(time)
	}


}



ClotheslineReader {

	var path, string;

	*parseScore { |path, delimiter = "//--"|
		^super.newCopyArgs(path).parseScore(delimiter)
	}

	parseScore { |delimiter = "//--"|
		var event;

		string = File.readAllString(path);

		event = ();

		string.findAll(delimiter).do { |from|
			this.parsePartToEvent(event, from, delimiter)
		};

		if(event.isEmpty) {
			"nothing could be read from the file: %".format(path).postln;
		};

		event[\path] = path;
		event[\modificationTime] = File.mtime(path);

		^event
	}

	parsePartToEvent { |event, from, delimiter|
		var part, to, eol, i, j, code;


		eol = string.find("\n", offset: from);
		to = string.find(delimiter, offset: from+1) ?? { string.size };

		if(eol.isNil) { this.parsePartErrorThrow("part '%', add a newline at least...".format(string[from..]), from, path) };
		part = string[from + delimiter.size..eol];
		i = part.detectIndex { |x| x.isAlpha };
		if(i.isNil) { this.parsePartErrorThrow("no part name: '%'".format(part), from, path) };
		part = part[i..];
		i = part.detectIndex { |x| x.isAlphaNum.not };
		if(i.isNil) { this.parsePartErrorThrow("part '%', add at least one newline".format(part), from, path) };
		part = part[..i-1];
		code = string[from..to-1];

		event[part.asSymbol] = code.compile;
	}

	parsePartErrorThrow {  |errorString, i|
		var lineNumber;
		errorString = "%\nin score file with the path:\n%".format(errorString, path);
		if(i.notNil) {
			lineNumber = string[..i].count { |x| x == Char.nl } + 1;
			errorString = errorString ++ "\nLine Number: " ++  lineNumber
		};
		Error(errorString).throw
	}

}

