/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText.Type;

/**
 * @author Christopher Deckers
 */
public class SpecialSequence extends Expression {

	private String text;

	public SpecialSequence(String text) {
		this.text = text;
	}

	@Override
	protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
		return new RRText(Type.SPECIAL_SEQUENCE, text, null);
	}

	@Override
	protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
		sb.append("(? ");
		sb.append(text);
		sb.append(" ?)");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpecialSequence other = (SpecialSequence) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

}
