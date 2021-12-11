package aQute.struct;

import aQute.lib.regex.PatternConstants;

/**
 * Never use capturing groups so they can be used as building blocks.
 */
public interface Patterns {
	String	SIMPLE_NAME_S	= "[\\p{javaJavaIdentifierPart}]+";
	String	HEX_S			= "(?:[0-9a-fA-F][0-9a-fA-Z])+";
	String	SHA_1_S			= "(?:" + HEX_S + "){20,20}";
	String	SLASHED_PATH_S	= SIMPLE_NAME_S + "(?:/" + SIMPLE_NAME_S + ")*";
	String	NUMMERIC_S		= "[0-9]+";
	String	VERSION_S		= "\\d{1,9}(:?\\.\\d{1,9}(:?\\.\\d{1,9}(:?\\." + PatternConstants.TOKEN + ")?)?)?";
	String	VERSION_RANGE_S	= "(?:(:?\\(|\\[)" + VERSION_S + "," + VERSION_S + "(\\]|\\)))|" + VERSION_S;

}
