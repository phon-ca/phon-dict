package ca.phon.ipadictionary

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ca.phon.ipa.IPATranscript;
import ca.phon.ipadictionary.impl.TransliterationDictionary;
import ca.phon.ipadictionary.spi.OrthoKeyIterator;

@RunWith(JUnit4.class)
class TestStaticTranscriptions {
	
	private final static java.util.logging.Logger LOGGER = 
		Logger.getLogger(TestStaticTranscriptions.class.getName());

	@Test
	public void testDicts() {
		// test all dictionaries with the OrthoKeyIterator extension
		final IPADictionaryLibrary library = IPADictionaryLibrary.getInstance();
		
		// keep track of some statistics
		
		final Iterator<IPADictionary> itr = library.availableDictionaries();
		
		itr.each { dictionary ->
			final OrthoKeyIterator orthoItr = dictionary.getExtension(OrthoKeyIterator.class);
			if(orthoItr != null) {
				int numberOfTranscriptions = 0;
				int numberOfValidTranscriptions = 0;
				LOGGER.info("Checking transcriptions in dictionary: " + dictionary.getLanguage());
				orthoItr.each { orthography ->
					dictionary.lookup(orthography).each { transcription ->
						++numberOfTranscriptions;
						try {
							final IPATranscript ipa = IPATranscript.parseIPATranscript(transcription);
							++numberOfValidTranscriptions;
						} catch (pe) {
							LOGGER.warning("Failed: " + transcription + ", " + pe.getMessage());
						}
					}
				}
				
				if(numberOfValidTranscriptions != numberOfTranscriptions)
					LOGGER.warning("$dictionary.language $numberOfTranscriptions - $numberOfValidTranscriptions");
				//Assert.assertEquals(numberOfTranscriptions, numberOfValidTranscriptions);
			}
		}
	}
	
}