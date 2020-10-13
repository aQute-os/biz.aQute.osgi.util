/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.nextencia.rrdiagram.grammar.rrdiagram.RRChoice;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;

/**
 * @author Christopher Deckers
 */
public class Choice extends Expression {

	private Expression[] expressions;

	public Choice(Expression... expressions) {
		this.expressions = expressions;
	}

	public Expression[] getExpressions() {
		return expressions;
	}

	@Override
	protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
		RRElement[] rrElements = new RRElement[expressions.length];
		for (int i = 0; i < rrElements.length; i++) {
			rrElements[i] = expressions[i].toRRElement(grammarToRRDiagram);
		}
		return new RRChoice(rrElements);
	}

	@Override
	protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
		List<Expression> expressionList = new ArrayList<>();
		boolean hasNoop = false;
		for (Expression expression : expressions) {
			if (expression instanceof Sequence && ((Sequence) expression).getExpressions().length == 0) {
				hasNoop = true;
			} else {
				expressionList.add(expression);
			}
		}
		if (expressionList.isEmpty()) {
			sb.append("( )");
		} else if (hasNoop && expressionList.size() == 1) {
			boolean isUsingMultiplicationTokens = grammarToBNF.isUsingMultiplicationTokens();
			if (!isUsingMultiplicationTokens) {
				sb.append("[ ");
			}
			expressionList.get(0)
				.toBNF(grammarToBNF, sb, isUsingMultiplicationTokens);
			if (!isUsingMultiplicationTokens) {
				sb.append(" ]");
			}
		} else {
			boolean isUsingMultiplicationTokens = grammarToBNF.isUsingMultiplicationTokens();
			if (hasNoop && !isUsingMultiplicationTokens) {
				sb.append("[ ");
			} else if (hasNoop || isNested && expressionList.size() > 1) {
				sb.append("( ");
			}
			int count = expressionList.size();
			for (int i = 0; i < count; i++) {
				if (i > 0) {
					sb.append(" | ");
				}
				expressionList.get(i)
					.toBNF(grammarToBNF, sb, false);
			}
			if (hasNoop && !isUsingMultiplicationTokens) {
				sb.append(" ]");
			} else if (hasNoop || isNested && expressionList.size() > 1) {
				sb.append(" )");
				if (hasNoop) {
					sb.append("?");
				}
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(expressions);
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
		Choice other = (Choice) obj;
		if (!Arrays.equals(expressions, other.expressions))
			return false;
		return true;
	}

}
