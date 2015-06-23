/*
MasterSequencer.instance = nil
MasterSequencer();
*/
SampleManager {

	classvar <>samples;

	*initClass{
		samples = Dictionary.new;
	}

	*loadSample { |path, section|
		var key = path.basename.drop(-4).asSymbol;
		if( Server.default.serverRunning ) {
			if( section.notNil ){
				// if there is not yet such a section, create one
				if( samples[section].isNil ) { samples[section] = Dictionary.new };

				// check if that file was already loaded on that section
				samples[section][key] = samples[section][key] ?? {
					//("Loading sample % in section %".format(key, section)).postln;
					Buffer.read(Server.default, path);
				};
			} {
				// check if that file was already loaded
				samples[key] = samples[key] ?? {
					//("Loading sample % at root level".format(key)).postln;
					Buffer.read(Server.default, path)
				};
			}
		} {
			warn("Boot the server first!");
		};
	}

	*loadSamples { |paths, section|
		paths.do{ |path|
			SampleManager.loadSample(path, section);
		}
	}

	*getSection{ |section|
		^samples[section]
	}

	*getSample{ |key, section|
		//key.postln;
		//key.class.postln;
		if( section.isNil ) {
			//"retreiving sample %".format(key).postln;
			^samples[key]
		} {
			//"retreiving sample % from %".format(key, section).postln;
			^samples[section][key]
		}
	}

	*getSamples{ |keys, section|
		^keys.collect{ |key| SampleManager.getSample(key, section) }
	}

}


/*

SampleManager.loadSamokes(this.class.filenameSymbol.asString.dirname +/+ "samples/kicks/*").pathMatch, 'kicks');

// get section (dictionary with keys/samples)
SampleManager.getSection('kicks').keys

// get sample (get a single sample form a section or root level)
SampleManager.getSample('EL_Kick001', 'kicks')

// get an array of samples from a section (or root level)
SampleManager.getSamples(['EL_Kick001', 'EL_Kick002', 'EL_Kick003'], 'kicks')

// load the same files, won't reload the whole buffers, just ignores...
SampleManager.loadFiles("~/Music/samples/kinski/*.aif".pathMatch, 'Kinski');

*/

