package ca.phon.dict.cmn;

import ca.phon.app.log.LogUtil;
import ca.phon.app.session.editor.view.ipaDictionary.IPALookupPostProcessor;
import ca.phon.ipa.IPATranscript;
import ca.phon.ipa.IPATranscriptBuilder;
import ca.phon.ipadictionary.IPADictionary;
import ca.phon.phonex.PhonexMatcher;
import ca.phon.phonex.PhonexPattern;
import ca.phon.plugin.IPluginExtensionFactory;
import ca.phon.plugin.IPluginExtensionPoint;
import ca.phon.syllable.SyllabificationInfo;

import java.text.ParseException;

/**
 * This class will perform the following transformations:
 *
 * <ul>
 *   <li>σ²¹⁴ sandhi
 *     <ul>
 *       <li>Any σ²¹⁴ followed by another σ²¹⁴ has tone changed to ³⁵
 *         <ul>
 *           <li>Phonex: <code>(σ:tn("214"))(?>σ:tn("214"))</code></li>
 *           <li>Replace: <code>\1³⁵</code></li>
 *         </ul>
 *       </li>
 *
 *       <li>Any σ²¹⁴ followed by a σ with tone *not* 214 has tone changed to ²¹
 *         <ul>
 *           <li>Phonex: <code>(σ:tn("214"))(?>σ:tn("not 214"))</code></li>
 *           <li>Replace: <code>\1²¹</code></li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *
 *   <li>i⁵⁵ sandhi
 *     <ul>
 *       <li>i⁵⁵ followed by any σ⁵¹ has tone changed to ³⁵
 *         <ul>
 *           <li>Phonex: <code>(\Si:tn("55"))(?>σ:tn("51"))</code></li>
 *           <li>Replace: <code>\1³⁵</code></li>
 *         </ul>
 *       </li>
 *
 *       <li>i⁵⁵ followed by anything has tone changed to ⁵¹
 *         <ul>
 *           <li>Phonex: <code>(\Si:tn("55"))(?>\S.+)</code></li>
 *           <li>Replace: <code>\1⁵¹</code></li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 *
 *   <li>pu⁵¹ sandhi
 *     <ul>
 *       <li>pu⁵¹ followed by any σ⁵¹ has tone changed to ³⁵
 *         <ul>
 *           <li>Phonex: <code>(\Spu:tn("51"))(?>σ:tn("51"))</code></li>
 *           <li>Replace: <code>\1³⁵</code></li>
 *         </ul>
 *       </li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class ToneFixes implements IPALookupPostProcessor, IPluginExtensionPoint<IPALookupPostProcessor> {

	private final static String CMN_LANG = "cmn";

	private final static String[] TONE_214_PHONEX = {
			"(σ:tn(\"214\"))(?>σ:tn(\"214\"))", "(σ:tn(\"214\"))\\b(?>σ:tn(\"214\"))", "(σ:tn(\"214\"))(?>.+)"

	};
	private final static String[] TONE_214_REPLACE = {
			"\\1³⁵", "\\1³⁵ ", "\\1²¹"
	};

	private final static String[] I_55_PHONEX = {
			"(\\Si:tn(\"55\"))(?>σ:tn(\"51\"))", "(\\Si:tn(\"55\"))(?>\\S.+)"
	};
	private final static String[] I_55_REPLACE = {
			"\\1³⁵", "\\1⁵¹"
	};

	private final static String[] PU_51_PHONEX = {
			"(\\Spu:tn(\"51\"))(?>σ:tn(\"51\"))"
	};
	private final static String[] PU_51_REPLACE = {
			"\\1³⁵"
	};

	@Override
	public IPATranscript postProcess(IPADictionary dictionary, String orthography, IPATranscript transcript) {
		if (CMN_LANG.equals(dictionary.getLanguage().getPrimaryLanguage().getId())) {
			IPATranscript retVal = transcript;
			retVal = tone214Sandhi(retVal);
			retVal = i55Sandhi(orthography, retVal);
			retVal = pu51Sandhi(orthography, retVal);
			return retVal;
		} else {
			return transcript;
		}
	}

	/**
	 * Process a set of phonex expressions with given replace expressions
	 *
	 * @param ipa
	 * @param phonexExprs
	 * @param replaceExprs
	 * @return
	 * @throws IllegalArgumentException if replaceExprs.length != phonexExprs.length
	 * @throws ParseException           if a phonex parse error occurs
	 */
	private IPATranscript process(IPATranscript ipa, String[] phonexExprs, String[] replaceExprs) throws ParseException {
		IPATranscript retVal = ipa;

		if (phonexExprs.length != replaceExprs.length)
			throw new IllegalArgumentException("phonexExprs.length must match replaceExprs.length");

		for (int i = 0; i < phonexExprs.length; i++) {
			String phonex = phonexExprs[i];
			String repaceExpr = replaceExprs[i];


			IPATranscript replace = IPATranscript.parseIPATranscript(repaceExpr);
			if(repaceExpr.endsWith(" ")) {
				replace = (new IPATranscriptBuilder()).append(replace).appendWordBoundary().toIPATranscript();
			}

			PhonexPattern pattern = PhonexPattern.compile(phonex);
			PhonexMatcher matcher = pattern.matcher(retVal);

			IPATranscriptBuilder builder = new IPATranscriptBuilder();
			while (matcher.find()) {
				matcher.appendReplacement(builder, replace);
			}
			matcher.appendTail(builder);
			retVal = builder.toIPATranscript();
		}

		return retVal;
	}

	/**
	 * σ²¹⁴ sandhi<ul>
	 * <li>Any σ²¹⁴ followed by another σ²¹⁴ has tone changed to ³⁵<ul>
	 * <li>Phonex: <code>(σ:tn("214"))(?>σ:tn("214"))</code></li>
	 * <li>Replace: <code>\1³⁵</code></li></ul>
	 * </li>
	 * <li>Any σ²¹⁴ followed by a σ with tone *not* 214 has tone changed to ²¹<ul>
	 * <li>Phonex: <code>(σ:tn("214"))(?>σ:tn("not 214"))</code></li>
	 * <li>Replace: <code>\1²¹</code></li></ul>
	 * </li></ul>
	 *
	 * @param ipa
	 * @return
	 */
	public IPATranscript tone214Sandhi(IPATranscript ipa) {
		IPATranscript retVal = ipa;
		try {
			retVal = process(ipa, TONE_214_PHONEX, TONE_214_REPLACE);
		} catch (ParseException e) {
			LogUtil.warning(e);
		}
		return retVal;
	}

	/**
	 * '一' sandhi* <ul>
	 * <li>i⁵⁵ followed by any σ⁵¹ has tone changed to ³⁵
	 *         <ul>
	 *           <li>Phonex: <code>(\Si:tn("55"))(?>σ:tn("51"))</code></li>
	 *           <li>Replace: <code>\1³⁵</code></li>
	 *         </ul>
	 *       </li>
	 *
	 *       <li>i⁵⁵ followed by anything has tone changed to ⁵¹
	 *         <ul>
	 *           <li>Phonex: <code>(\Si:tn("55"))(?>\S.+)</code></li>
	 *           <li>Replace: <code>\1⁵¹</code></li>
	 *         </ul>
	 *       </li>
	 *     </ul>
	 */
	public IPATranscript i55Sandhi(String fullOrtho, IPATranscript fullIpa) {
		IPATranscript retVal = fullIpa;
		if(fullOrtho.contains("一")) {
			// split into words
			String[] orthoWords = fullOrtho.split("\\p{Space}");
			IPATranscript[] ipaWords = fullIpa.words().toArray(new IPATranscript[0]);

			if(orthoWords.length == ipaWords.length) {
				IPATranscriptBuilder builder = new IPATranscriptBuilder();
				for(int wIdx = 0; wIdx < orthoWords.length; wIdx++) {
					if(builder.size() > 0) builder.appendWordBoundary();

					String orthography = orthoWords[wIdx];
					IPATranscript ipa = ipaWords[wIdx];

					if (orthography.length() == ipa.syllables().size()) {
						int idx = 0;
						int syllIdx = 0;

						while ((idx = orthography.indexOf('一', idx)) >= 0) {
							while (syllIdx < idx) builder.append(ipa.syllables().get(syllIdx++));
							idx++;

							IPATranscript iSyll = ipa.syllables().get(syllIdx++);

							// sanity check
							if (!"i⁵⁵".equals(iSyll.toString())) {
								--syllIdx;
								continue;
							}

							// don't change if last syllable
							if (syllIdx >= ipa.syllables().size()) {
								--syllIdx;
								continue;
							}
							IPATranscript nextSyll = ipa.syllables().get(syllIdx);

							SyllabificationInfo scInfo = nextSyll.elementAt(nextSyll.length() - 1).getExtension(SyllabificationInfo.class);
							if (scInfo.getToneNumber().equals("51"))
								builder.append("i³⁵:N");
							else
								builder.append("i⁵¹:N");
						}
						while (syllIdx < ipa.syllables().size()) builder.append(ipa.syllables().get(syllIdx++));
					} else {
						builder.append(ipa);
					}
				}
				retVal = builder.toIPATranscript();
			}
		}
		return retVal;
	}

	/**
	 * '不' sandhi
	 * <ul>
	 *   <li>pu⁵¹ followed by any σ⁵¹ has tone changed to ³⁵
	 *     <ul>
	 *       <li>Phonex: <code>(\Spu:tn("51"))(?>σ:tn("51"))</code></li>
	 *       <li>Replace: <code>\1³⁵</code></li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 *
	 * @param orthography
	 * @param ipa
	 * @return
	 */
	public IPATranscript pu51Sandhi(String fullOrtho, IPATranscript fullIpa) {
		IPATranscript retVal = fullIpa;
		if(fullOrtho.contains("不")) {
			// split into words
			String[] orthoWords = fullOrtho.split("\\p{Space}");
			IPATranscript[] ipaWords = fullIpa.words().toArray(new IPATranscript[0]);

			if (orthoWords.length == ipaWords.length) {
				IPATranscriptBuilder builder = new IPATranscriptBuilder();
				for (int wIdx = 0; wIdx < orthoWords.length; wIdx++) {
					if (builder.size() > 0) builder.appendWordBoundary();

					String orthography = orthoWords[wIdx];
					IPATranscript ipa = ipaWords[wIdx];

					if (orthography.length() == ipa.syllables().size()) {
						int idx = 0;
						int syllIdx = 0;

						while ((idx = orthography.indexOf('不', idx)) >= 0) {
							while (syllIdx < idx) builder.append(ipa.syllables().get(syllIdx++));
							idx++;

							IPATranscript iSyll = ipa.syllables().get(syllIdx++);

							// sanity check
							if (!"pu⁵¹".equals(iSyll.toString())) {
								--syllIdx;
								continue;
							}

							// don't change if last syllable
							if (syllIdx >= ipa.syllables().size()) {
								--syllIdx;
								continue;
							}

							IPATranscript nextSyll = ipa.syllables().get(syllIdx);

							SyllabificationInfo scInfo = nextSyll.elementAt(nextSyll.length() - 1).getExtension(SyllabificationInfo.class);
							if (scInfo.getToneNumber().equals("51"))
								builder.append("p:Ou³⁵:N");
							else
								builder.append(iSyll);
						}
						while(syllIdx < ipa.syllables().size()) builder.append(ipa.syllables().get(syllIdx++));


					} else {
						builder.append(ipa);
					}
				}
				retVal = builder.toIPATranscript();
			}
		}
		return retVal;
	}

	@Override
	public Class<?> getExtensionType() {
		return IPALookupPostProcessor.class;
	}

	@Override
	public IPluginExtensionFactory<IPALookupPostProcessor> getFactory() {
		return (args) -> this;
	}

}
