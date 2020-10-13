/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;

/**
 * @author Christopher Deckers
 */
public class GrammarToRRDiagram {

	public interface RuleLinkProvider {
		String getLink(String ruleName);
	}

	private RuleLinkProvider ruleLinkProvider = ruleName -> "#" + ruleName;

	public void setRuleLinkProvider(RuleLinkProvider ruleLinkProvider) {
		this.ruleLinkProvider = ruleLinkProvider;
	}

	public RuleLinkProvider getRuleLinkProvider() {
		return ruleLinkProvider;
	}

	private String ruleConsideredAsLineBreak;

	public void setRuleConsideredAsLineBreak(String ruleConsideredAsLineBreak) {
		this.ruleConsideredAsLineBreak = ruleConsideredAsLineBreak;
	}

	public String getRuleConsideredAsLineBreak() {
		return ruleConsideredAsLineBreak;
	}

	public RRDiagram convert(Rule rule) {
		return rule.toRRDiagram(this);
	}

}
