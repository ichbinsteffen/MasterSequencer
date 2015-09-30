/*
MasterSequencer.instance = nil;
MasterSequencer();
*/

SequencerButton : QView {
	var <>value;

	*new { |parent, bounds|
		^super.new(parent, bounds).init
	}

	init {
		value = 0;
	}

	turnOn {
		value = 1;
		this.background_(Color.red);
	}
	turnOff {
		value = 0;
		this.background_(Color.black);
	}
	switchValue {
		if(value == 0){this.turnOn} {this.turnOff};
	}
	ledLightOn {
		this.background_(Color.green);
	}
	ledLightOff {
		if (this.value == 1) {this.background_(Color.red)} {this.background_(Color.black)}
	}

}

SequencerMatrix {

	var <>guiMatrix, <>sequence, <>steps, <>bars, <>view, <>size, <>height, <>bounds;
	var <>score;

	*new{ |parent, steps, bars, existingSequence|
		^super.new.init(parent, steps, bars, existingSequence);
	}

	init { |parent, steps, bars, existingSequence|
		this.steps = steps;
		this.bars = bars;
		this.sequence = existingSequence;
		existingSequence.postln;

		size = steps.switch(
			16, { 24 },
			32, { 12 },
			64, { 6 },
		);
		height = bars.switch(
			4, { 50 },
			8, { 100 },
			16,{ 192 },
		);

		view = QView(parent, Rect(390, 20, 400, height))
		.background_(Color.clear);

		QView.new(view, Rect(0,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		QView.new(view, Rect(4*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		QView.new(view, Rect(8*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		QView.new(view, Rect(12*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		if (steps == 32) {
			QView.new(view, Rect(16*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(20*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(24*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(28*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		};
		 if (steps == 64) {
			QView.new(view, Rect(16*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(20*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(24*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(28*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));

			QView.new(view, Rect(32*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(36*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(40*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(44*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(48*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(52*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(56*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
			QView.new(view, Rect(60*size,0,size-2,height-12)).background_(Color.fromHexString("00BFFF"));
		};

		// 2D array for buttons
		guiMatrix = {0}!steps!bars;

		// 1D array for values

		//if (existingSequence = nil){
			sequence = {false}.dup(steps*bars); // set all values to false
		//};

		bars.do{ |i|
			steps.do{ |j|

				guiMatrix[i][j] = SequencerButton(view, Rect((j*size),(i*12), size-2, 10)).background_(Color.black);

				guiMatrix[i][j].mouseDownAction_({
					guiMatrix[i][j].switchValue;
					sequence[(i*steps)+j] = guiMatrix[i][j].value.asBoolean;
				});

				guiMatrix[i][j].mouseEnterAction_({
					if(MasterSequencer.instance.toggling) {
						this.setCell(i, j, 2);
					};
					if(MasterSequencer.instance.drawing) {
						this.setCell(i, j, 1);
					};
					if(MasterSequencer.instance.erasing) {
						this.setCell(i, j, 0);
					};
				});

			};
		};
		this.refreshGuiMatrix;
	}

	setCell { |i, j, value|
		if (value == 0) {
			guiMatrix[i][j].turnOff;
		};
		if (value == 1) {
			guiMatrix[i][j].turnOn;
		};
		if (value == 2) {
			guiMatrix[i][j].switchValue;
		};
		sequence[(i*steps)+j] = guiMatrix[i][j].value.asBoolean;
	}

	refreshGuiMatrix {

		bars.do {|i|
			steps.do{|j|
				if(sequence[(i*steps)+j] == true){
					guiMatrix[i][j].turnOn;
				}
			}
		}
	}
}

/*
MasterSequencer();
MasterSequencer.instance = nil;
*/ 