/*
MasterSequencer.instance = nil;
MasterSequencer();
*/

SequencerElement {

	var <>view, <>selectorView, <>mainView, <>preButton, <>popUp, <>slider1, <>slider2, <>matrix, <>height, <>section, <>closeButton;
	var <>sample, <>steps, <>bars, <>samplePath, <>sequenceDuration;
	var <>specAmp, <>specRate, <>pan, <>sustain, <>range, <>start, <>end;
	var <>inspectorString;
	var <>id, <>original;
	classvar <>currentID;

	var <>volumeSlider, <>panKnob, <>rateKnob;

	*initClass {
		currentID = -1;
	}

	*new{ |parent, steps, bars, section|
		currentID = currentID + 1;
		^super.new.init(parent, steps, bars, section);
	}

	init{ |parent, steps, bars, section |


///// SETTING THE INSTANCE VARIABLES /////

		var dir = this.class.filenameSymbol.asString.dirname;
		var defaultSamp;
		var ms = MasterSequencer.instance;

		this.steps = steps;
		this.bars = bars;
		this.id = currentID;
		this.section = section;
		this.start = 0;
		this.end = 1;
		slider1 = 1;
		slider2 = 1;

		SampleManager.loadSamples(
			(dir +/+ "samples/%/*".format(section)).pathMatch,
			section
		);

		specAmp = ControlSpec(0.0001, 2, \db, 0, 1, "dB");
		specRate = ControlSpec(0.25, 4, \exponential, 0, 1, "speed");

		switch(bars)
		{4} {height = 90}
		{8} {height = 136}
		{16} {height = 232};

///// THE VIEWS AND BUTTONS AND SLIDERS //////////////////////////////////////////////////////////////

		mainView = QView(MasterSequencer.instance.scroll, Point(820, height)).background_(Color.clear);

		selectorView = QView(mainView, Rect(0, 0, 10, height))   // LITTLE ORANGE SELECTOR BAR
		.background_(Color.clear);

		view = QView(mainView, Rect(10, 0, 800, height))         // THE VIEW
		.background_(Color.fromHexString("D3D3D3")); ///99CCBB

		mainView.mouseEnterAction_({
			this.selectorView.background_(Color.fromHexString("FF7733"));
			MasterSequencer.instance.currentElement = this;
			this.refresh;
		})
		.mouseLeaveAction_({
			this.selectorView.background_(Color.clear);
			MasterSequencer.instance.currentElement = nil;
		});

		popUp = QPopUpMenu(mainView, Rect(40,20,140,50));
		popUp.items_(SampleManager.getSection(section).keys.postln.asArray.sort);
		popUp.action_({|pop|
			var aSample = SampleManager.getSample(pop.items[pop.value].asSymbol, section);
			"setting sample for Element % % %".format(section, pop.items[pop.value], aSample).postln;
			sample = aSample;
			this.samplePath = sample.path;
			this.refresh;
		});

		defaultSamp = SampleManager.getSection(section).keys.asArray.sort.first;
		sample = SampleManager.getSample(defaultSamp.asSymbol, section);  // LOAD 1st sample per default

		volumeSlider = QSlider(mainView, Rect(190, 20, 170, 20));
		volumeSlider.action_{ |v|            // VOL SLIDER (1)
			slider1 = specAmp.map(v.value);
			this.refresh;
		};
		volumeSlider.valueAction_(specAmp.unmap(1));


		panKnob = QKnob(mainView, Rect(368, 20, 20, 20))                            // PAN KNOB
		.action_({|v,x,y,m| pan =\pan.asSpec.map(v.value);
		this.refresh;
		})
		.value_(pan = \pan.asSpec.unmap(0))
		.centered_(true);

		rateKnob = QKnob(mainView, Rect(368, 50, 20, 20)).action_{ |v|               // SPEED KNOB (slider 2)
			slider2 = specRate.map(v.value);
			this.refresh;
		}.valueAction_(specRate.unmap(1));

		range = QRangeSlider(mainView,Rect(190, 50, 170,20)).action_{ |v|          // RANGE SLIDER
			var string = "Low: \t% ; High: \t% ".format(v.lo,v.hi);
			string.postln;
			this.start = v.lo;
			this.end = v.hi;
			this.refresh;
		};

		QButton.new(mainView, Rect(30,20,10,50)).action_({                // SAMPLE LISTEN BUTTON
			sample.play;});
/*
		QButton.new(mainView, Rect(795,((height / 2)-7),9,10))            // DELETE SEQUENCER ELEMENT BUTTON
		.states_([["x", Color.white, (Color.clear).alpha_(0.3)]])
		.action_({this.remove();})
		.mouseEnterAction_({MasterSequencer.instance.inspectorTextPrint("\nRemove this Element")})
		.mouseLeaveAction_({this.refresh});
		*/
		closeButton = QView(mainView, Rect(800,0,10,10));
		closeButton.background_(Color.red.alpha_(0.1));
		closeButton.mouseEnterAction_({closeButton.background_(Color.red)});
		closeButton.mouseLeaveAction_({closeButton.background_(Color.red.alpha_(0.1))});
		closeButton.mouseDownAction_({this.remove;});

		QButton.new(mainView, Rect(785,((height / 2)+10),15,13))          // Bounce to disk Button
		.states_([["B", Color.white, (Color.clear).alpha_(0.1)]])
		.action_({
			var binarySequence = this.matrix.sequence.collect(_.binaryValue);
			this.bounceSequenceDialogue(binarySequence);
			})
		.mouseEnterAction_({MasterSequencer.instance.inspectorTextPrint("\nBounce to Disk")})
		.mouseLeaveAction_({this.refresh});

		QButton.new(mainView, Rect(785,((height / 2)+30),15,13))          // DUPLICATE BUTTON
		.states_([["D", Color.white, (Color.clear).alpha_(0.1)]])
		.action_({
			var ms = MasterSequencer.instance;
			ms.addNewSequencerElement(ms.duplicateSequencerElement(ms.currentElement));
		})
		.mouseEnterAction_({MasterSequencer.instance.inspectorTextPrint("\nDuplicate this Element")})
		.mouseLeaveAction_({this.refresh});

		matrix = SequencerMatrix(view, steps, bars);

		^this;
	}

	randomize {  // RANDOMIZE THE CELLS OF THE ACTUAL SEQUENCER ELEMENT
		"randomizing".postln;
		100.do{
			matrix.setCell( bars.rand, steps.rand, 2.rand );};
	}

	refresh {    // REFRESHING THE INSPECTOR TEXT FOR THE ACTUAL SEQUENCER ELEMENT
		var inspectorString =
		"*** INSPECTOR ***\n
		Sample: % \n
		Volume: \t% dB\n
		Speed: \t%\n
		\n
		Start: \t% \n
		End: \t% \n
		Pan \t% \n
		".format(
			popUp.items[popUp.value],
			slider1.ampdb.round(0.01),
			slider2.round(0.01),
			this.start.round(0.0001),
			this.end.round(0.0001),
			this.pan
		);
		MasterSequencer.instance.inspectorTextPrint(inspectorString);
	}

	remove {
		"Removing local sequencer element".postln;
		MasterSequencer.instance.removeSequencerElement(steps, this);
	}

	bounceSequenceDialogue{ |binarySequence, sampleFormat = "int16"|

		var p;

		[\BPM, MasterSequencer.instance.globalBPM].postln;
		[\BARS, this.bars].postln;

		// this.sequenceDuration = (this.steps.switch(16,{1/8},32,{1/16})) / (60 / MasterSequencer.instance.globalBPM) * (this.bars * this.steps);

		// this.sequenceDuration = ((60 / MasterSequencer.instance.globalBPM) * 4) * this.bars;

		this.sequenceDuration = (this.bars * 4) / (MasterSequencer.instance.globalBPM / 60);
		//this.sequenceDuration = 14;

		[\duration, this.sequenceDuration].postln;

		SynthDef(\sampler, { |out, buffer, rate=1, amp|
			var snd = PlayBuf.ar(2, buffer, BufRateScale.kr(buffer)*rate, doneAction:2);
			Out.ar(0, snd * amp)
		}).store;

		p = Pbind(
			\instrument,\sampler,
			\rate, this.slider2.value,
			\buffer, this.id,
			\dur, (1 / this.steps) * 4,
			\amp, Pseq(binarySequence) * this.slider1.value,
		).asScore(this.sequenceDuration);

		p = p.score.insert(1, [0, ["/b_allocRead", this.id, this.samplePath, 0, -1]]);
		p.postln;

		Dialog.savePanel({ |path,score|
			var header = path.basename.split($.);
			if(header.size == 1){
				header = "WAV";
				path = path ++ ".wav";
			}{
				header = header.last;
			};
			if(header == "aif"){ header = "AIFF" };
			if(header == "aiff"){ header = "AIFF" };
			if(header == "wav"){ header = "WAV" };

			Score.recordNRT(
				p,
				path.dirname +/+ path.basename ++ ".osc", path,
				headerFormat:header,
				duration: this.sequenceDuration
			);
		},
			{}
		);
	}

	setSetting { |popVal, vol, pan, rate|
		this.popUp.value = popVal;
		this.pan = pan;
		this.rate = rate;
	}
}

SCSynthElement {

	var <>mainView,<>selectorView,<>view,<>scCodeBox_syn,<>scCodeBox_pat;
	var <>slider1, <>slider2, <>specAmp, <>pan;
	var <>id;
	var <>melo;
	var <>steps;
	classvar currentID;

	*initClass {
		currentID = 0;
	}

	*new{|parent|
		currentID = currentID + 1;
		^super.new.init(parent);
	}

	init{|parent|

		this.id = currentID;
		this.steps = 100;
		specAmp = ControlSpec(0.0001, 2, \db, 0, 1, "dB");

		mainView = QView(parent, Point(820, 232)).background_(Color.clear);

		selectorView = QView(mainView, Rect(0, 0, 10, 232))   // LITTLE ORANGE SELECTOR BAR
		.background_(Color.clear);

		view = QView(mainView, Rect(10, 0, 800, 232))         // THE VIEW
		.background_(Color.fromHexString("D3D3D3")); ///99CCBB

		mainView.mouseEnterAction_({
			this.selectorView.background_(Color.fromHexString("FF7733"));
			MasterSequencer.instance.currentElement = this;
			this.refresh;
		})
		.mouseLeaveAction_({
			this.selectorView.background_(Color.clear);
			MasterSequencer.instance.currentElement = nil;
		});
/*
		QSlider(mainView, Rect(368, 50, 20, 162)).action_{ |v|  // VOL SLIDER (1)
			slider1 = specAmp.map(v.value);
			//this.refresh;
		}.valueAction_(specAmp.unmap(1));

		QKnob(mainView, Rect(368, 20, 20, 20))                  // PAN KNOB
		.action_({|v,x,y,m| pan =\pan.asSpec.map(v.value);})
		.value_(pan = \pan.asSpec.unmap(0))
		.centered_(true);
*/
		QButton.new(mainView, Rect(795,((232 / 2)-7),9,10))  // DELETE SEQUENCER ELEMENT BUTTON
		.states_([["x", Color.white, (Color.clear).alpha_(0.3)]])
		.action_({
			this.remove();
		};);

		scCodeBox_syn = QTextView(
			mainView,Rect(30,20,330,192))
		.background_(Color.fromHexString("DCDCDC"));

//		scCodeBox_syn.string_("{ | asdf | Out.ar(0,EnvGen.kr(Env.perc(0.01,0.1, doneAction: 2)* SinOsc.ar) }");

		scCodeBox_syn.string_("{\n | out, freq, sustain = 1, amp = 0.1 |\n\tvar s;\n\ts = SinOsc.ar(freq, 0, 0.2) * \n\tLine.kr(amp, 0, sustain, doneAction: 2);\n\tOut.ar(out, [s,s])\n}");

		scCodeBox_pat = QTextView(mainView, Rect(397,20,386,192)).background_(Color.fromHexString("DCDCDC"));
		scCodeBox_pat.string_("Pseq([0,1,2,0], inf)");

		^this;
	}

	remove {
		"Removing local scSynth element".postln;
		MasterSequencer.instance.removeSequencerElement(steps, this);
	}

	buildSynth {
		var s = (this.scCodeBox_syn.string).interpret;
		SynthDef(this.id.asSymbol, s).add;

		this.melo =
		("[instrument:\\"++
			this.id.asString ++",degree: "++
			this.scCodeBox_pat.string ++
			",amp: 0.1," ++ "]"
		).interpret;
		this.melo.dump;
	}

	refresh {    // REFRESHING THE INSPECTOR TEXT FOR THE ACTUAL scSynthelement
		var inspectorString =
		"*** INSPECTOR ***\n\n A scSynthElement";
		MasterSequencer.instance.inspectorTextPrint(inspectorString);
	}
}

/*
MasterSequencer.instance = nil;
MasterSequencer();
*/