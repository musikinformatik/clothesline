

//-- INIT

~test.callEnvirTest_DoesNotSpillOver;


//-- PLAY
~test.callEnvirTest_OuterReachable;
~outerState = \overridden;
~test.callEnvirTest_OuterOverridden;
~internalState = 1970;

//-- STOP

currentEnvironment[\outerState] = nil; // can't currently use ~outerState = nil; because of bug in sclang
~test.callEnvirTest_OuterReachableAgain;


//-- FREE


