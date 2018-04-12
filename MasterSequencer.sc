/*
MasterSequencer.instance = nil;
MasterSequencer();
*/

MasterSequencer{

	classvar <>instance;

	var <>window, <>inspectorView, <>playerView, <>playerSelectorView, <>inspectorTextView, <>constructor,
	<>ampFader,<>bpmFader, <>playButton, <>bpmNumberBox,
	<>globalAmp, <>globalBPM, <>sequences, <>sequencerElementsLayout, <>scSynthElements,
	<>chosenSoundType, <>bars, <>noteLength, <>sampleArray, <>scroll, <>flowLayout,
	<>drawing, <>toggling, <>erasing, <>tempoSpec, <>volumeSpec, <>midiThingsInstance;

	var <>currentElement;

	*new {
		if( instance.isNil ) {
			instance = super.new.init;
		};
		^instance;
	}

	createNewSequencerElement { |steps, bars, type|
		^SequencerElement(scroll, steps, bars, type)
	}
	createNewChordSequencerElement {
		^ChordSequencerElement(scroll);
	}


	duplicateSequencerElement { |ele|
		var element = SequencerElement(scroll, ele.steps, ele.bars, ele.section);

		element.matrix.sequence = ele.matrix.sequence;
		element.slider1 = ele.slider1;
		element.slider2 = ele.slider2;
		element.popUp.value = ele.popUp.value;
		element.start = ele.start;
		element.end = ele.end;
		element.range.lo_(ele.start);
		element.range.hi_(ele.end);
		element.sample = ele.sample;
		element.matrix.refreshGuiMatrix();
		element.volumeSlider.value = ele.volumeSlider.value;
		element.panKnob.value = ele.panKnob.value;
		element.rateKnob.value = ele.rateKnob.value;

		^element
	}

	addNewChordSequencerElement {|ele|
		sequencerElementsLayout.add(ele);
		sequences[ele.steps].add(ele);
		flowLayout.nextLine;
	}

	addNewSequencerElement { |ele| // noteLength, bars
		sequencerElementsLayout.add(ele);
		sequences[ele.steps].add(ele);
		flowLayout.nextLine;
	}

	init {
		tempoSpec = ControlSpec(30.0, 240.0);
		globalBPM = 120;
		sequencerElementsLayout = List.new;
		volumeSpec = [Server.default.volume.min, Server.default.volume.max, \db].asSpec;
		midiThingsInstance = MIDIthings();

		this.loadSynths;
		this.loadTask;
		this.makeWindow;
		this.makeInspector;
		this.makePlayer;
		this.resetModes;
		TempoClock.tempo_(globalBPM/60);

		sequences = ();
		scSynthElements = ();
		sequences[16] = List.new;
		sequences[32] = List.new;
		sequences[64] = List.new;
		scSynthElements[100] = List.new;

		sequences[100] = List.new; // Fake List for scSynthElements to use them like sequencerElements.
		sequences[101] = List.new; // Fake List for ChordSequencerElement to use them like ...
	}

	makeWindow {

		chosenSoundType = "Effects";
		bars = 4;
		noteLength = 16;
		sampleArray = (MasterSequencer.class.filenameSymbol.asString.dirname +/+ "samples/*")
		.pathMatch.collect{ |path| path.basename.asSymbol };

		window = QWindow.new("App", Rect(300,300,1030,720),true)
		.background_(Color.fromHexString("808080"));

		window.acceptsMouseOver_(true);
		scroll = QScrollView.new(window, Rect(210,50, 820, 600))
		.background_(Color.fromHexString("808080")).hasBorder_(false);
		scroll.hasVerticalScroller_(true);
		scroll.acceptsMouseOver_(true);
		flowLayout = scroll.addFlowLayout(0@0, 0@10);

		constructor = QView(window, Rect(220, 10, 800,30))
		.background_(Color.fromHexString("D3D3D3"));

		QStaticText(constructor, Rect(432,1,30,30)).string_("Bars");  // Bars string

		QPopUpMenu(constructor, Rect(390,5,40,20)) // Bars popup-list
		.items_([4,8,16])
		.action_({|pop| bars = pop.items[pop.value]; });

		QStaticText(constructor, Rect(523,1,80,30)).string_("Notelength");

		QPopUpMenu(constructor, Rect(470,5,50,20)) // Notelength popup-list
		.items_([16,32,64])
		.action_({|pop| noteLength = pop.items[pop.value]; });

		QPopUpMenu(constructor, Rect(25,5,140,20)) // Sample Type popup-list
		.items_(sampleArray)
		.action_({|pop|
			chosenSoundType = pop.items[pop.value];
		});

		QButton(constructor, Rect(210,5,60,20)) // Add Chord Sequencer Element
		.states_([["Chords", Color.black, Color.white]])
		.action_({
			this.addNewChordSequencerElement(ChordSequencerElement(scroll, bars, midiThingsInstance));
		});

		QView(constructor, Rect(690,0,110,30)).background_(Color.fromHexString("D3D3D3"));
		QView(constructor, Rect(685,0,5,30)).background_(Color.grey);
		QButton(constructor, Rect(695,5,100,20)) // New SCSynth-Button
		.states_([["SCSynth", Color.black, Color.white]])
		.action_({
			var s = SCSynthElement(scroll);
			this.addNewSequencerElement(s);
		});

		QButton(constructor, Rect(5,5,20,20))    // New SequencerElement-Button
		.states_([["+", Color.black, Color.white]])
		.action_({
			var dir = this.class.filenameSymbol.asString.dirname.postln;
			var paths = ((dir +/+ "samples/%/*".format(chosenSoundType))).pathMatch;
			"Number of files in section: %".format(paths.size).postln;

			if (paths.size > 1) {
				this.addNewSequencerElement(this.createNewSequencerElement(noteLength, bars, chosenSoundType));
			} {
				"There is no file in section. Creation discarded.".postln;
				MasterSequencer.instance.inspectorTextPrint(
					"\n There is no file in section %. \nCreation discarded.".format(chosenSoundType).postln);
			}
		});

		///// MAPPING CERTAIN KEYDOWNS TO MATRIX EDITING MODES

		window.view.keyDownAction_{ |view,char|
			if((char==$t) or:{ char==$T }){
				toggling = toggling.not;
				drawing = false;
				erasing = false;
				"toggling mode".postln;
			};
			if((char==$d) or:{ char==$D }){
				drawing = drawing.not;
				toggling = false;
				erasing = false;
				"drawing".postln;
			};
			if((char==$e) or:{ char==$E }){
				erasing = erasing.not;
				toggling = false;
				drawing = false;
				"erasing".postln;
			};
			if((char==$r) or:{ char==$R }){
				"randomize".postln;
				if( currentElement.isNil.not){
					currentElement.randomize;
				};
			};
			if((char==$c) or:{ char==$C }){
				drawing = false;
				toggling = false;
				erasing = false;
				"Editing Modes off".postln;
			};
		};

		window.front();

	}


	removeSequencerElement { |noteLength, obj|
		// var index = sequences[noteLength].indexOf(obj);
		var layoutIndex = sequencerElementsLayout.indexOf(obj).postln;
		var layoutElement = sequencerElementsLayout.removeAt(layoutIndex);
		var restOfElements = sequencerElementsLayout[layoutIndex..];
		var height = layoutElement.mainView.bounds.height + 10;
		"Removing Sequencer element from Main Sequencer".postln;
		sequences[noteLength].removeAt(sequences[noteLength].indexOf(obj));
		[\current, layoutElement].postln;
		[\after, restOfElements].postln;
		layoutElement.mainView.remove;
		restOfElements.do{ |element|
			var bounds = element.mainView.bounds;
			element.mainView.bounds = bounds.moveBy(0, 0-height);
		};

		flowLayout.top = sequencerElementsLayout.collect{ |element| element.mainView.bounds.height + 10}.sum;
		flowLayout.left = 0;
	}

	resetModes {
		drawing = false;
		toggling = false;
		erasing = false;
	}

	makeInspector {

		inspectorView = QView(window, Rect(10,10,200,640)) //  Rect(10,10,200,590)
		.background_(Color.fromHexString("D3D3D3"));

		inspectorTextView = QTextView(inspectorView, Rect(5,5,190,630))
		.background_(Color.clear).editable_(true)
		.string_("App started") ;
	}

	inspectorTextPrint { |text|
		this.inspectorTextView.string_(text);
	}

	makePlayer {

		playerSelectorView = QView.new(window, Rect(10,650,200,10)) // old: Rect(10,600,200,10)
		.background_(Color.clear);

		playerView = QView.new(window, Rect(10,660,1010,50)) // old: Rect(10,610,1010,100)
		.background_(Color.fromHexString("D3D3D3"))
		.mouseEnterAction_({playerSelectorView.background_(Color.fromHexString("FF7733"))})
		.mouseLeaveAction_({playerSelectorView.background_(Color.clear)});

		playButton = QButton.new(playerView, Rect(5,5,90,40))
		.states_([
			["►", Color.black, Color.white],
			["∞", Color.white, Color.black],
		])
		.action_({|val|
			if (val.value == 1) {
				Tdef(\mainSequencerPlayer).quant = 0;
				Tdef(\mainSequencerPlayer).play;

				sequences[100].do{|syn|
					syn.buildSynth;
					Pbind(*syn.melo).play
				};

		sequences[101].do{|seq| seq.playSequence;};


			} {
				Tdef(\mainSequencerPlayer).stop;

				sequences[101].do{|syn| syn.stopSequence;};

				sequences[100].do{|syn|
					Pbind(*syn.melo).stop;
				};

				sequences[101].do{|seq| seq.stopSequence;};

				sequences[16].do{|seq|seq.matrix.refreshGuiMatrix;};
				sequences[32].do{|seq|seq.matrix.refreshGuiMatrix;};
				sequences[64].do{|seq|seq.matrix.refreshGuiMatrix;};

		}});

		bpmFader = QSlider.new(playerView, Rect(200,9,200,32))
		.action_({|val|
			this.changeTempo(val.value);
			bpmNumberBox.value_(globalBPM);
		})
		.value_(tempoSpec.unmap(globalBPM))
		.background_(Color.gray);

		bpmNumberBox = QNumberBox.new(playerView, Rect(145,10,50,30))
		.action_({|val|
			globalBPM = val.value;
			TempoClock.tempo_(globalBPM/60);
			bpmFader.value_(tempoSpec.unmap(globalBPM));
		})
		.value_(globalBPM)
		.background_(Color.gray);

		QStaticText(playerView, Rect(105,10,30,30)).string_("BPM");

		ampFader = QKnob.new(playerView, Rect(975,10,30,30)) // old: Rect(975,10,25,80)
		.action_({|val| Server.default.volume_(volumeSpec.map(val.value));})
		.value_(volumeSpec.unmap(Server.default.volume.volume))
		.background_(Color.gray);
	}

	changeTempo { |new_tempo|
		globalBPM = tempoSpec.map(new_tempo);
		[\newTempo, globalBPM].postln;
		TempoClock.tempo_(globalBPM/60);
	}


	loadTask {

		Tdef(\mainSequencerPlayer, {
			inf.do{ |i|
				var current16 = (i/4).floor;
				var current32 = (i/2).floor;
				var current64 = i;


				////// 16 ///////// 16 ////////// 16 /////////// 16 ////////////
				sequences[16].do{ |seq|        // LED
					var bars = seq.matrix.sequence.size / 16;
					defer{
						seq.matrix.guiMatrix.flatten.wrapAt(current16).ledLightOn;
						seq.matrix.guiMatrix.flatten.wrapAt(current16-1).ledLightOff;
					};
				};

				if( i % 4 == 0 ) {        // Trigger Sample
					sequences[16].do{ |seq|

/*						var arguments = [
							\amp, seq.slider1,
							\rate, seq.slider2,
							\buffer, seq.sample,
							\pan, seq.pan.value,
							\start, seq.start,
							\end, seq.end,
						];
						var synthName = if(seq.sample.numChannels > 1)
						{ \samplePlayerStereo }{ \samplePlayerMono };

						if( seq.matrix.sequence.wrapAt(current16) ) {
							Synth(synthName, arguments);
						};
*/

//// as Event -BEGIN
						var synthName = if(seq.sample.numChannels > 1) {\samplePlayerStereo}{\samplePlayerMono};
						if( seq.matrix.sequence.wrapAt(current16) ) {
							(
							\instrument: synthName,
							\amp: seq.slider1,
							\rate: seq.slider2,
							\buffer: seq.sample,
							\pan: seq.pan.value,
							\start: seq.start,
							\end: seq.end,
							).play
						};
//// as Event - END

					};
				};

				////// 32 ///////// 32 //////////32 /////////// 32 ////////////
				sequences[32].do{ |seq| // LED
					defer{
						seq.matrix.guiMatrix.flatten.wrapAt(current32).ledLightOn;
						seq.matrix.guiMatrix.flatten.wrapAt(current32-1).ledLightOff;
					};
				};

				if( i % 2 == 0 ) {     // Trigger Sample

					sequences[32].do{ |seq|

						var bars = seq.matrix.sequence.size / 16;
						var actualBar = 0;
						var arguments = [
							\amp, seq.slider1,
							\rate, seq.slider2,
							\buffer, seq.sample,
							\pan, seq.pan.value,
							\start, seq.start,
							\end, seq.end,
						];

						var synthName = if(seq.sample.numChannels > 1)
						{ \samplePlayerStereo }{ \samplePlayerMono };

						if( seq.matrix.sequence.wrapAt(current32) ) {
							Synth(synthName, arguments);
							//Synth(\samplePlayerStereo,
							//[\amp, seq.slider1, \rate, seq.slider2, \buffer, seq.sample]);
						};
					};
				};

				////// 64 ///////// 64 ////////// 64 /////////// 64 ////////////
				sequences[64].do{ |seq|   // LED
					defer{
						seq.matrix.guiMatrix.flatten.wrapAt(current64).ledLightOn;
						seq.matrix.guiMatrix.flatten.wrapAt(current64-1).ledLightOff;
					};
				};

				sequences[64].do{ |seq|  // Trigger Sample

					var bars = seq.matrix.sequence.size / 16;
					var actualBar = 0;
					var arguments = [
						\amp, seq.slider1,
						\rate, seq.slider2,
						\buffer, seq.sample,
						\pan, seq.pan.value,
						\start, seq.start,
						\end, seq.end,
					];

					var synthName = if(seq.sample.numChannels > 1)
					{\samplePlayerStereo }{ \samplePlayerMono };

					if( seq.matrix.sequence.wrapAt(current64) ) {
						Synth(synthName, arguments);
						//Synth(\samplePlayerStereo,
						//[\amp, seq.slider1, \rate, seq.slider2, \buffer, seq.sample]);
					};
				};
				///////////////////////////////////////////////////////////////////
				(1/16).wait;
			};
		};
	);
}


	loadSynths {

		Server.default.waitForBoot({

			SynthDef(\samplePlayerMono, { |out, buffer, amp=1, rate=1, pan, start, end|
				var snd = PlayBuf.ar(1, buffer, BufRateScale.kr(buffer) * rate, 1, start * BufFrames.kr(buffer));
				snd = snd * EnvGen.ar(Env.linen(0, BufDur.kr(buffer) * (end-start), 0.005), doneAction:2);
				snd = Pan2.ar(snd, pan);
				Out.ar(out, Pan2.ar(snd, 0) * amp);
			}).add;

			SynthDef(\samplePlayerStereo, { |out, buffer, amp=1, rate=1, pan, start, end|
				var snd = PlayBuf.ar(2, buffer, BufRateScale.kr(buffer) * rate, 1, start * BufFrames.kr(buffer));
				snd = snd * EnvGen.ar(Env.linen(0, BufDur.kr(buffer) * (end-start), 0.005), doneAction:2);
				snd = Balance2.ar(snd[0], snd[1], pan);
				Out.ar(out, snd * amp);
			}).add;
		})
	}

	play {
		Tdef(\mainSequencerPlayer).play;
		this.inspectorTextPrint("PLAY");
	}

	stop {
		Tdef(\mainSequencerPlayer).stop;
	}

	pause {
		Tdef(\mainSequencerPlayer).pause;
	}

	*clearMasterSequencer {
		instance = nil;
	}

}



/*
MasterSequencer.instance = nil
MasterSequencer();
test
*/