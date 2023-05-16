

TestClothesline : UnitTest {

	classvar <>current;
	var  <>environment;

	scorepath {
		^this.class.filenameSymbol.asString.dirname +/+ "test-clothesline/"
	}

	setUp {
		environment = Environment.new;
		environment.use {
			~outerState = \outerState;
			~test = this;
			current = Clothesline.new;
			current.verbose = false;
		};
	}

	tearDown {
		current = nil;
		environment = nil;
	}

	test_load {
		environment.use {
			current.addAllScores(this.scorepath, callInContext: false);
			this.assert(current.score['test-01'].notNil, "test-01-score.scd should have loaded");
			this.assert(current.score['test-02'].notNil, "test-02-score.scd should have loaded");
		}
	}


	test_jump_play {

		environment.use {

			current.addAllScores(this.scorepath, callInContext: false);

			current.sched('test-01', 1, 3);
			current.sched('test-02', 2, 4);

			current.jumpTo(0);
			0.01.wait;
			current.jumpTo(1.5);
			0.01.wait;

			this.assert(~test1_initialized == true, "after having passed INIT, the init code should have been called");
			this.assert(~test2_initialized != true, "before having passed INIT, the init code should not have been called");
			this.assert(~test1_playing == true, "after having passed PLAY, the play code should have been called");


			current.jumpTo(3.5);
			0.01.wait;

			this.assert(~test1_playing != true, "after having passed STOP, the stop code should have been called");
			this.assert(~test2_playing == true, "after having passed PLAY, the play code should have been called");
		};

	}

	test_jump_forward {
		environment.use {
			current.addAllScores(this.scorepath, callInContext: false);

			current.sched('test-01', 1, 3);
			current.sched('test-02', 2, 4);

			current.jumpTo(0);
			0.01.wait;
			current.jumpTo(1.5);
			0.01.wait;
			current.jumpTo(3.5);
			0.01.wait;

			this.assert(~test1_playing != true, "when jumping after the end time, STOP should have been called");
			this.assert(~test2_playing == true, "after having passed PLAY, the play code should have been called");
		}
	}

	test_jump_backward {

		environment.use {
			current.addAllScores(this.scorepath, callInContext: false);

			current.sched('test-01', 1, 3);
			current.sched('test-02', 2, 4);

			current.jumpTo(0);
			0.01.wait;
			current.jumpTo(3);
			0.01.wait;
			current.jumpTo(0);

			0.01.wait;
			this.assert(~test1_playing != true, "when jumping back before play time, STOP should have been called");
			this.assert(~test2_playing != true, "when jumping back before play time, STOP should have been called");
		}


	}

	test_alias {

		environment.use {

			current.addAllScores(this.scorepath, callInContext: false);

			current.sched('test-01', 1, 3);
			current.sched('test-01', 4, 8, 'test_alias');

			current.jumpTo(5);

			0.01.wait;
			this.assert(~test1_playing == true, "alias should add a duplicate");
			this.assertEquals(current.breakpoints, [0, 1, 3, 4, 8], "alias should add extra breakpoints");
		};
	}

	test_breakpoints {

		environment.use {
			var b;

			current.addAllScores(this.scorepath, callInContext: false);
			current.defaultBreakpoints = [0, 10];

			current.sched('test-01', 1.12, 3);
			current.sched('test-02', 100, 400);
			current.sched('test-02', 1.12, 4);
			b = Routine {
				var t = 0;
				0.yield;
				while {
					t = current.getNextBreakpoint(t);
					t.notNil
				} {
					t.yield;
				}
			}.all;
			this.assertEquals(b, [0, 1.12, 3, 4, 10]);
		};
	}

	test_breakpoint_scheduling {
		environment.use {
			current.addAllScores(this.scorepath, callInContext: false);
			current.defaultBreakpoints = [0, 10];

			this.assertEquals(current.breakpoints, current.defaultBreakpoints,
				"before anything is scheduled, the breakpoiunts should be the default breakpoints");
			current.sched('test-01', 0, 1);
			current.sched('test-02', 0.5, 1.2);
			this.assertEquals(current.breakpoints, [0, 0.5, 1, 1.2, 10],
				"after something is scheduled, the breakpoiunts should be updated");
			current.sched('test-01', 0.1, 3.5);
			this.assertEquals(current.breakpoints, [0, 0.1, 0.5, 1.2, 3.5, 10],
				"after an existing score is rescheduled, the breakpoiunts should be updated");
		};

	}

	test_environment {

		environment.use {
			this.assertEquals(~test, this,
				"self test: test instance should be in current environment");
			this.assertEquals(current.capturedCurrentEnvironment, environment,
				"self test: test envir should be clothesline's captured envir");
			this.assertEquals(current.capturedCurrentEnvironment[\test], this,
				"self test: test instance should be in captured environment");

			this.assertEquals(~outerState, \outerState, "self test: outerState variable should have been set correctly");

			current.addAllScores(this.scorepath, callInContext: true);

			current.sched('test-envir', 1, 3);

			// tests are called from score
			current.jumpTo(0);
			0.01.wait;
			current.jumpTo(1.5);
			0.01.wait;
			current.jumpTo(3.0);

		};


	}

	test_environment_alias {
		// aliases should not share the same environment.
		environment.use {

			current.addAllScores(this.scorepath, callInContext: true);

			current.sched('test-envir', 1, 3, 'alias1');
			current.sched('test-envir', 2, 4, 'alias2');

			// tests are called from score
			current.jumpTo(1.5);
			0.01.wait;
			current.jumpTo(2.5);
			0.01.wait;
			current.jumpTo(4.0);


		}
	}

	// methods called from score

	callEnvirTest_OuterReachable {
		this.assertEquals(~outerState, \outerState, "outer environment should be reachable");
	}

	callEnvirTest_OuterOverridden {
		this.assertEquals(~outerState, \overridden, "outer environment should be overridden by local environment");
	}

	callEnvirTest_OuterReachableAgain {
		this.assertEquals(~outerState, \outerState, "outer environment should be reachable again");
	}

	callEnvirTest_DoesNotSpillOver {
		this.assert(~internalState.isNil, "state should not spill over between environments when set locally.");
	}





}