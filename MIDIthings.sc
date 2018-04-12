/*
MasterSequencer.instance = nil;
MasterSequencer();
MIDIthings.instance = nil;
MIDIthings();

Pattern Guide 01 Durchlesen !!!
*/

MIDIthings {
	classvar <>instance, <>majorSteps, <>minorSteps, <>chosenScale;
	var roman_1, roman_2, roman_3, roman_4, roman_5, roman_6, roman_7;
	var <>m;
	var <>chords;
	var <>window;

	*new {
		instance = super.new.init;
		^instance;
	}

    init {
		minorSteps= Array.with(2,1,2,2,1,2,2);
		majorSteps = Array.with(2,2,1,2,2,2,1);
		chosenScale = Array.new(52);

		chords = ();

		7.do({|i| chords[i] = List.new});

		this.makeWindow;
		this.makeScalePickWindow;
		^this;
    }

	makeScalePickWindow {
		var scale = "Ab";
		var mode = "minor";
		var scalePickWindow = QWindow.new("Pick a scale", Rect(50,800,180,110),false);

		QView.new(scalePickWindow, Rect(10,10,160,90)).background_(Color.fromHexString("D3D3D3"));
		scalePickWindow.background_(Color.fromHexString("808080"));

		QPopUpMenu(scalePickWindow, Rect(20,20,50,20))
	.items_(["A","A♯","B","C","C♯","D","D♯","E","F","F♯","G","G♯"]) 	//.items_(["A♭","A","A♯","B♭","B","C♭","C","C♯","D♭","D","D♯","E♭","E","E♯","F♭","F","F♯","G♭","G","G♯"]) // Remove Ab
		.action_({|pop| scale = pop.items[pop.value]; });

		QPopUpMenu(scalePickWindow, Rect(80,20,60,20))
		.items_(["minor","major"])
		.action_({|pop| mode = pop.items[pop.value]; });

		QButton(scalePickWindow, Rect(20, 50, 140 ,40))
		.states_([["Set Scale", Color.black, Color.white]])
		.action_({this.setScale(scale,mode);});

		scalePickWindow.front();
	}


	setScale { |scale_arg, mode_arg|
		var firstNote, steps;
		chosenScale = Array.new(52);
		7.do({|i| chords[i] = List.new});

		switch( scale_arg,
			"A",  {firstNote = 21},
			"A♯",{firstNote = 22},
			//"Bb",{firstNote = 22}, // can be removed
			"B",  {firstNote = 23},
			//"Cb",{firstNote = 23}, // can be removed
			"C",  {firstNote = 24},
			"C♯",{firstNote = 25},
			//"Db",{firstNote = 25}, // can be removed
			"D",  {firstNote = 26},
			"D♯",{firstNote = 27},
			//"Eb", {firstNote = 27}, // can be removed
			"E",   {firstNote = 28},
			//"E#", {firstNote = 29}, // can be removed
			//"Fb", {firstNote = 28}, // can be removed
			"F",   {firstNote = 29},
			"F♯", {firstNote = 30},
			//"Gb", {firstNote = 30}, // can be removed
			"G",   {firstNote = 31},
			"G♯", {firstNote = 32},
			//"Ab", {firstNote = 32}, // can be removed
		);

		if (mode_arg == "minor", {steps = minorSteps;}, {steps = majorSteps;} );

		" Scale: % \n Mode: % \n First note: %".format(scale_arg, mode_arg, firstNote).postln;

		52.do({arg j;
			chosenScale.add(firstNote);
			firstNote = firstNote + steps.at(j % 7);
		});
		chosenScale.postln;

		this.setChords(mode_arg);
	}


	setChords { |mode|
		var currentChordProg, currentChordMode, currentFirst, third, fifth;
		var majChordProg = Array.with("D","M","M","D","D","M","V");
		var minChordProg = Array.with("M","M","M","D","D","M","V");

		if (mode == "major",{currentChordProg = majChordProg}, {currentChordProg = minChordProg});

		7.do(
			{arg j;
				currentChordMode = currentChordProg.at(j);

				switch (currentChordMode,
					"D",{third = 4; fifth = 7},
					"M",{third = 3; fifth = 7},
					"V",{third = 3; fifth = 6}
				);

				currentFirst = chosenScale.at(j);
				chords[j].add(currentFirst);
				chords[j].add(currentFirst + third);
				chords[j].add(currentFirst + fifth);

				18.do({arg i; chords[j].add( chords[j].at(i) + 12 ); } );
				"Roman % chord notes: %".format(j+1, chords[j]).postln;
		})
	}

	get_ChordNotesMIDI { |roman, vari, oct, len|
		var noteOne, noteTwo, noteThree, index, notes;

		roman = roman - 1;
		index = (oct - 1) * 3;

		noteOne = chords[roman].at(index);
		noteTwo = chords[roman].at(index+1);
		noteThree = chords[roman].at(index+2);

		if (vari == "Var 1" ,
			{
				noteOne = chords[roman].at(index+3);
			}
		);
		if (vari == "Var 2" ,
			{
				noteOne = chords[roman].at(index+3);
				noteTwo = chords[roman].at(index+4);
			}
		);

		notes = [noteOne, noteTwo, noteThree];

		^notes;
	}

		makeWindow {
		// This method seems deprecated, I cant remember why it's there.
		// Window was never designed and isnt visible. Dead instance of a QWindow.
		window = QWindow.new("Scale Picker",Rect(100,100,800,600),false);
		//window.front();
		//this.addMIDISequencerElement();
	}

	addMIDISequencerElement {
		MIDISequencerElement.new(window);
	}
}


/*
MIDIthings();
MasterSequencer();
*/


ChordSequencerElement {

	classvar currentID, chosenScale, chosenMode, <>instances, <>mti, <>synthName = 0;

	var <>id;

	var  voice, chordSeq, chordPick, chordVari, noteLen, chord, vari, len, oct;

	var <>pseq, <>pseqStream, <>sequenceWithSilence, <>sequenceNoSilence, <>player, <>volume, <>synth, <>synthString;

	var <>view, <>scrollView, <>mainView, <>selectorView, <>addChordButton, <>closeButton, octavePick,<>chordFlowLayout, <>compositeView, <>barLines, <>bars, <>instance, <>steps, <>pseq, <>synthPickButton, <>playButton, <>allChordBlocks, <>stopButton, <>volSlider, <>scSynthPopUp;

	var <>scSynthWindow, <>scSynthTextBox;

	var <pianoSynthDefString, <defaultDefSynthString, <stringSynthDefString, <thingSynthDefString, <defaultSynthDefString, <simple_1SynthDefString;

	*new{ |parent, bars, midiThingsInstance |
		currentID = currentID + 1;
//		instance = super.new.init(parent);
//		^instance
		^super.new.init(parent, bars, midiThingsInstance);
	}

	*initClass {
		currentID = -1;
		instances = List.new();
	}

	// synthName wird eine Klassenvariable die inkrementiert mit jedem neuen Synth
	// der neu definierte synth wrd nach dieser variable (als symbol) benannt.
	//

	init { |parent, bars, midiThingsInstance|
		this.id = currentID;
		mti = midiThingsInstance;
		instances.add(this);
		allChordBlocks = List.new;
		this.steps = 101;
		this.bars = bars;
		chord = "I";
		len = "1/4";
		oct = "1";
		vari = "Standart";
		synth = \default; // just used for the initial synth declaration

		defaultSynthDefString = "// SynthDef(name, <- This can be left out in this window!\n\n{\n| out, freq, sustain = 1, amp = 0.1 |\n\t var s;\n\t s = SinOsc.ar(freq, 0, 0.2) * Line.kr(amp, 0, sustain, doneAction: 2);\n\t Out.ar(out, [s,s])\n}\n";
		pianoSynthDefString = "// SynthDef(name, <- This can be left out in this window!\n\n{\n |freq = 261.63, vol = 1, gate = 1| \n\tvar ampls = [0.7, 0.4, 0.2, 0.1, 0.95, 0.6, 0.5, 0.65, 0, 0.1, 0.2]; \n\tvar freqs = Array.fill(ampls.size, { |i| freq * (i + 1) }); \n\tvar waves = Array.fill(ampls.size, { |i| SinOsc.ar(freqs.at(i),mul: ampls.at(i))}); \n\tvar mixedwaves = Mix.ar(waves).range(vol * -1,vol); \n\tvar env = Env.perc(0.09,4,curve: -10); \n\tvar final = mixedwaves * EnvGen.ar(env, gate, doneAction: 2); \n\tOut.ar(0, [final,final]);\n} \n";

		stringSynthDefString = "{\n | out=0, freq=440, pan=0, sustain=0.5, amp=0.3 |\n\tvar pluck, period, string; \n\tpluck = PinkNoise.ar(Decay.kr(Impulse.kr(0.005), 0.05)); \n\tperiod = freq.reciprocal; \n\tstring = CombL.ar(pluck, period, period, sustain*6); \n\tstring = LeakDC.ar(LPF.ar(Pan2.ar(string, pan), 12000)) * amp; \n\tDetectSilence.ar(string, doneAction:2); \n\tOut.ar(out, string)\n}\n";

		simple_1SynthDefString = "{\n | t_trig=0, freq |\n\tOut.ar(0, SinOsc.ar(freq+[0,1], 0, Decay2.kr(t_trig, 0.005, 1.0)))\n;}";

		sequenceWithSilence = List.new;
		sequenceNoSilence = List.new;

		mainView = QView(MasterSequencer.instance.scroll, Point(820, 100));
		mainView.background_(Color.clear);
		mainView.acceptsMouseOver_(true);

		selectorView = QView(mainView, Rect(0, 0, 10, 100));   // LITTLE ORANGE SELECTOR BAR
		selectorView.background_(Color.clear); //Color.clear

		mainView.mouseEnterAction_({selectorView.background_(Color.fromHexString("FF7733"));});
		mainView.mouseLeaveAction_({selectorView.background_(Color.clear);});

		view = QView(mainView, Rect(10, 0, 800, 100));         // THE VIEW
		view.background_(Color.fromHexString("D3D3D3"));
		view.acceptsMouseOver_(true);

		chordPick = QPopUpMenu(view, Rect(20, 10, 60, 17));
		chordPick.items_(["I","II","III","IV","V","VI","VII","break"]);
		chordPick.action_({|pop| chord = pop.items[pop.value]; });

		chordVari = QPopUpMenu(view, Rect(20, 30, 60, 17));
		chordVari.items_(["Std", "Var 1", "Var 2"]);
		chordVari.action_({|pop| vari = pop.items[pop.value]; });

		noteLen = QPopUpMenu(view, Rect(20, 50, 60, 17));
		noteLen.items_(["1/4", "1/8", "1/2", "1/16", "1/32","1Bar","2Bars","1/4+","1/8+","1/16+","1/2+","1Bar+"]);
		noteLen.action_({|pop| len = pop.items[pop.value]; });

		octavePick = QPopUpMenu(view, Rect(20,70, 40, 17));
		octavePick.items_([1,2,3,4,5,6,7]);
		octavePick.action_({|pop| oct = pop.items[pop.value]; });

		addChordButton = QView(view, Rect(110,10,17,17));
		addChordButton.background_(Color.gray.alpha_(0.1));
		addChordButton.mouseEnterAction_({addChordButton.background_(Color.gray.alpha_(0.3))});
		addChordButton.mouseLeaveAction_({addChordButton.background_(Color.gray.alpha_(0.1))});
		addChordButton.mouseDownAction_({this.addNewChord(compositeView, chord, vari, len, oct);});
		QView(addChordButton, Rect(7,3,3,11)).background_(Color.black);
		QView(addChordButton, Rect(3,7,11,3)).background_(Color.black);

		playButton = QView(view, Rect(110,30,17,17));
		playButton.background_(Color.gray.alpha_(0.1));
		playButton.mouseEnterAction_({playButton.background_(Color.gray.alpha_(0.3))});
		playButton.mouseLeaveAction_({playButton.background_(Color.gray.alpha_(0.1))});
		playButton.mouseDownAction_({this.playSequence;});
		QStaticText(playButton, Rect(4,3,13,13)).string_("►");

		stopButton = QView(view, Rect(110,50,17,17));
		stopButton.background_(Color.gray.alpha_(0.1));
		stopButton.mouseEnterAction_({stopButton.background_(Color.gray.alpha_(0.3))});
		stopButton.mouseLeaveAction_({stopButton.background_(Color.gray.alpha_(0.1))});
		stopButton.mouseDownAction_({this.stopSequence;});
		QView(stopButton, Rect(4,4,9,9)).background_(Color.black);

		this.makeSynthPickWindow;

		QButton(scSynthWindow, Rect(0,600,600,20)).string_("Set").action_(
			{
				scSynthWindow.visible_(false);
				this.newSynth(this.scSynthTextBox.string)
			}
		);

		synthPickButton = QView(view, Rect(110,70,17,17));
		synthPickButton.background_(Color.gray.alpha_(0.1));
		synthPickButton.mouseEnterAction_({synthPickButton.background_(Color.gray.alpha_(0.3))});
		synthPickButton.mouseLeaveAction_({synthPickButton.background_(Color.gray.alpha_(0.1))});
		synthPickButton.mouseDownAction_({
			if(scSynthWindow.visible == nil, {this.makeSynthPickWindow;});
			scSynthWindow.front;
		});
		QStaticText(synthPickButton, Rect(4,3,13,13)).string_("S");

		volSlider = QSlider(view, Rect(85,10,20,80)).value_(0.8);
		volSlider.action_({|vol| volume = vol.value});

		scrollView = QScrollView(view, Rect(130,10,640, 90));
		scrollView.background_(Color.fromHexString("CCCCCC"));
		scrollView.hasHorizontalScroller_(true);
		scrollView.hasBorder_(false);

		compositeView = QView(scrollView,Rect(0, 0, 640*bars, 80));
		chordFlowLayout = compositeView.addFlowLayout(0@0, 0@0);

		barLines = Array.new(bars);
		bars.do({|i| barLines[i+1].add(
			QView(scrollView, Rect((640*(i+1))-1,0,1,80)).background_(Color.green);)
		});

		closeButton = QView(mainView, Rect(800,0,10,10));
		closeButton.background_(Color.red.alpha_(0.1));
		closeButton.mouseEnterAction_({closeButton.background_(Color.red)});
		closeButton.mouseLeaveAction_({closeButton.background_(Color.red.alpha_(0.1))});
		closeButton.mouseDownAction_({this.remove;});

		^this;
	}
// ---------------------------------------------
	makeSynthPickWindow {

		scSynthWindow = QWindow("SuperCollider Synth definition",
			Rect(230 + MasterSequencer.instance.window.bounds.left,200,600,620),true,true);
		scSynthTextBox = QTextView(scSynthWindow, Rect(0,20,600,580)).string_(defaultSynthDefString);
		scSynthPopUp = QPopUpMenu(scSynthWindow,Rect(0,0,600,20));
		scSynthPopUp.items_(["default","piano","string","thing", "simple 1"]);
		scSynthPopUp.action_({ |pop|
			switch(pop.items[pop.value],
				"default",{this.scSynthTextBox.string_(defaultSynthDefString)},
				"piano",{this.scSynthTextBox.string_(pianoSynthDefString)},
				"string",{this.scSynthTextBox.string_(stringSynthDefString)},
				"thing",{this.scSynthTextBox.string_(thingSynthDefString)},
				"simple 1",{this.scSynthTextBox.string_(simple_1SynthDefString)}
			)
		});

		QButton(scSynthWindow, Rect(0,600,600,20)).string_("Set").action_(
			{
				scSynthWindow.visible_(false);
				this.newSynth(this.scSynthTextBox.string)
			}
		);
	}
// ---------------------------------------------
	addNewChord { | scrollView, chord, vari, len, oct |
		var midiNotes, chordAsInt, lenAsInt, octAsInt;
		var c = ChordBlock.new(compositeView, chord, vari, len, oct, this);
		allChordBlocks.add(c);
		//chordFlowLayout.add(c);
		//sequence.add(c);
		octAsInt = oct.asInt;

		switch (chord,
			"I",{chordAsInt = 1},
			"II",{chordAsInt = 2},
			"III",{chordAsInt = 3},
			"IV",{chordAsInt = 4},
			"V",{chordAsInt = 5},
			"VI",{chordAsInt = 6},
			"VII",{chordAsInt = 7},
			"break",{chordAsInt = 8}
		);

		switch (len,
			"1/2",{lenAsInt = 2},
			"1/4",{lenAsInt = 1},
			"1/8",{lenAsInt = 0.5},
			"1/16",{lenAsInt = 0.25},
			"1/32",{lenAsInt = 0.125},
			"1/2+",{lenAsInt = 3},
			"1/4+",{lenAsInt = 1.5},
			"1/8+",{lenAsInt = 0.75},
			"1/16+",{lenAsInt = 0.375},
			"1Bar",{lenAsInt = 4},
			"2Bars",{lenAsInt = 8}
		);

		if (chordAsInt < 8,
			{
			midiNotes = mti.get_ChordNotesMIDI(chordAsInt, vari, octAsInt);
			},
			{
				midiNotes = \rest;
			}
		);

		if (synthName == 0, {synthName = \default;});
		sequenceNoSilence.add(
			(
				instrument: synthName.asSymbol,
				midinote: midiNotes,
				dur: lenAsInt,
				amp: 0.6;
			)
		);
		this.reNewSequence;
	}
// ---------------------------------------------
	reNewSequence {

		var usedTicks = 0;
		var emptyTicks;
		var maxAmountTicks = bars * 4;

		if (synthName == 0, {synthName = \default;});

// UsedTicks hochzählen
		allChordBlocks.do({| block |
			usedTicks = usedTicks + block.ticks;
		});


		emptyTicks = maxAmountTicks - usedTicks;

		sequenceWithSilence = sequenceNoSilence.copy();
		sequenceWithSilence.add ((
			intstrument: synthName.asSymbol,
			midinote: [0,0,0],
			dur: emptyTicks,
			amp: 0.0;
		));

		allChordBlocks.postln;
		sequenceWithSilence.postln;
	}
// ---------------------------------------------
	newSynth { |string|

		synthName = synthName + 1;
		SynthDef(synthName.asSymbol, string.interpret).add;

		// das beste wird es sein, jedem Chord-Track eine ID zu geben und der Name des Synths von der ID zu nehmen.
		// den Synth dann einfach überschreiben

		//var sy = SynthDef(this.id.asSymbol, synthString).add; // Saving one synthdef per instance and remove it and re-add when new synth must be initialized

		sequenceNoSilence.do({|block|
			block[\instrument] = synthName.asSymbol;
		});

		this.reNewSequence;
	}
// ---------------------------------------------
	removeChordBlock { |chord|

		var layoutIndex = allChordBlocks.indexOf(chord).postln;
		var restOfElements = allChordBlocks[layoutIndex..];
		var width = chord.view.bounds.width;

		sequenceNoSilence.removeAt(layoutIndex); // - hier entsteht der Fehler
		allChordBlocks.removeAt(layoutIndex); // - this is where the error happens

//		compositeView.layout.remove(this);

		compositeView.layout.remove(chord);

		restOfElements.do{ |element|
			var bounds = element.view.bounds;
			element.view.bounds = bounds.moveBy(0-width, 0);
		};

		chordFlowLayout.left = allChordBlocks.collect{|chord| chord.view.bounds.width}.sum;

		this.reNewSequence;
	}
// ---------------------------------------------
	remove { |ele|

		MasterSequencer.instance.removeSequencerElement(steps, this);
	}
// ---------------------------------------------
	playSequence {

		var startTime = TempoClock.default.beats;
		var stream = FuncStream(
			{
				var relativeTime = TempoClock.default.beats - startTime;
				var idx = 0;
				var relativeTimeInBar = relativeTime % (bars * 4); //
				var mySum = 0;
				var p,x;
				var event;

				while({mySum + sequenceWithSilence[idx][\dur] <= relativeTimeInBar},
					{
						mySum = mySum + sequenceWithSilence[idx][\dur];
						idx = idx + 1;
					}
				);

				event = sequenceWithSilence[idx].copy;
				event[\dur] = event[\dur] - (relativeTimeInBar - mySum);
				event;
			}
		);

		player = EventStreamPlayer(stream).play(TempoClock.default, quant: 0);
	}
// ---------------------------------------------
	stopSequence {
		player.stop;
		//player = nil;
	}
// ---------------------------------------------
	pauseSequence {
		player.pause;
	}
}
// ---------------------------------------------
/*
MasterSequencer();
MasterSequencer.instance = nil;
MIDIthings();
MIDIthings.instance = nil;
*/
// ---------------------------------------------
ChordBlock {

	var <>romanNum, <>chordLen, <>chordVari, <>pause;
	var <>view, closeButton,chordText, variText, noteLenText, octText, <>ticks;
	var <>width, marginLine;
	classvar currentID;
	var <>parentChordSequencer;

	*new{ |parentView, chord, vari, len, oct, parent |
		^super.new.init(parentView, chord, vari, len, oct, parent);
	}

	init { |parentView, chord, vari, len, oct, parent|
		parentChordSequencer = parent;
		switch(len,
			"1/4",{width = 160; ticks = 1;},
			"1/8",{width = 80; ticks = 0.5;},
			"1/2",{width= 320; ticks = 2;},
			"1/16",{width = 40; ticks = 0.25;},
			"1/32",{width = 20; ticks = 0125;},
			"1Bar",{width = 640; ticks = 4;},
			"2Bars",{width = 640*2; ticks = 8;},
			"1/4+",{width = 240; ticks = 1.5;},
			"1/8+",{width = 120; ticks = 0.75;},
			"1/2+",{width = 480; ticks = 3;},
			"1/16+",{width = 60; ticks = 0.375;},
			"1Bar+",{width = 960; ticks = 6;},);

		view = QView(parentView, Rect(0,0, width, 80));
		view.background_(Color.gray);

		closeButton = QView(view, Rect(width-12,0,10,10));
		closeButton.background_(Color.red.alpha_(0.1));
		closeButton.mouseEnterAction_({closeButton.background_(Color.red)});
		closeButton.mouseLeaveAction_({closeButton.background_(Color.red.alpha_(0.1))});
		closeButton.mouseDownAction_({parentChordSequencer.removeChordBlock(this);});

		marginLine = QView(view, Rect(width-2,0,2,80));
		marginLine.background_(Color.black);

		if (len == "1/32", {len = 32});

		switch(vari,
			"Standart",{vari="std"},
			"Var 1",{vari = 1},
			"Var 2", {vari = 2});

		chordText = StaticText(view, Rect(1,12,width-1,15));
		chordText.string_(chord);
		variText = StaticText(view, Rect(1,26,width-1,15));
		variText.string_(vari);
		noteLenText = QStaticText(view, Rect(1,40,width-1,15));
		noteLenText.string_(len);
		octText = QStaticText(view, Rect(1,55,width-1,15));
		octText.string_(oct);
	}
}


MIDISequencerElement {    // Rename to ScalePickWindow ?!
	classvar currentID;
	var id;
	var window, mainView, selectorView, view;
	var pop_1,pop_2,pop_3;

	*initClass {
		currentID = -1;
	}

	*new{ |parent|
		currentID = currentID + 1;
		^super.new.init(parent);
	}

	init{ |parent |
		mainView = QView(parent, Point(820, 150)).background_(Color.clear);
		selectorView = QView(mainView, Rect(0, 0, 10, 150));
		selectorView.background_(Color.clear);
		view = QView(mainView, Rect(10, 0, 800, 150)).background_(Color.fromHexString("D3D3D3"));
		mainView.mouseEnterAction_({selectorView.background_(Color.fromHexString("FF7733"));});
		mainView.mouseLeaveAction_({selectorView.background_(Color.clear);});
		pop_1 = QPopUpMenu(view, Rect(10,10,70,20));
		pop_1.items_(["1","2","3"]);
		pop_2 = QPopUpMenu(view, Rect(10,35,70,20));
		pop_2.items_(["1","2","3","4","5","6","7"]);
		pop_3 = QPopUpMenu(view, Rect(10,60,70,20));
		pop_3.items_(["BAR 1","BAR 2","BAR 3","BAR 4"]);
	}
}

// Bugs:
// 1. Löschen eines Chord-Block führt zum löschen des Blocks links davon: delete(chord[this-1]).
// Bug in  removechordblock()
// 2. Beim betätigen des Play-Button sollte ein Play-Mode aktiviert werden. Derzeit -> 10 Klicks = 10 Streams.


/*
MIDIthings();
MIDIthings.instance = nil;
MasterSequencer();
MasterSequencer.instance = nil;

midi2Note { |midiX|

		var theString = midiX % 12
		switch (theString.choose)
		{0} {"C"}
		{1} {"C#"}
		{2} {"D"}
		{3} {"D"}
		{4} {"E"}
		{5} {"F"}
		{6} {"F#"}
		{7} {"G"}
		{8} {"G#"}
		{9} {"A"}
		{10} {"A#"}
		{11} {"B"}

		var o = midiX / 12;
		switch (o.choose)
		{<2} {theString.toUpper; ",,," + theString}
		{2} {theString.toUpper;    ",," + theString}
		{3} {theString.toUpper;    ","+ theString}
		{4} {theString.toUpper}
		{5} {theString.toLower}
		{6} {theString.toLower; theString + " ' "}
		{7} {theString.toLower; theString + " '' "}
		{8} {theString.toLower; theString + " ''' "}
		{9} {theString.toLower; theString + " '''' "}
		theString.postln;

		^theString;
	}

*/

