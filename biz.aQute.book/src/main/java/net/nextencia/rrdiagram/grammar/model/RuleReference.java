/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.grammar.model.GrammarToRRDiagram.RuleLinkProvider;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRBreak;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText.Type;

/**
 * @author Christopher Deckers
 */
public class RuleReference extends Expression {

	private String ruleName;

	public RuleReference(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getRuleName() {
		return ruleName;
	}

	@Override
	protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
		String ruleConsideredAsLineBreak = grammarToRRDiagram.getRuleConsideredAsLineBreak();
		if (ruleConsideredAsLineBreak != null && ruleConsideredAsLineBreak.equals(ruleName)) {
			return new RRBreak();
		}
		RuleLinkProvider ruleLinkProvider = grammarToRRDiagram.getRuleLinkProvider();
		return new RRText(Type.RULE, ruleName, ruleLinkProvider == null ? null : ruleLinkProvider.getLink(ruleName));
	}

	@Override
	protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
		sb.append(ruleName);
		String ruleConsideredAsLineBreak = grammarToBNF.getRuleConsideredAsLineBreak();
		if (ruleConsideredAsLineBreak != null && ruleConsideredAsLineBreak.equals(ruleName)) {
			sb.append("\n");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ruleName == null) ? 0 : ruleName.hashCode());
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
		RuleReference other = (RuleReference) obj;
		if (ruleName == null) {
			if (other.ruleName != null)
				return false;
		} else if (!ruleName.equals(other.ruleName))
			return false;
		return true;
	}

}
