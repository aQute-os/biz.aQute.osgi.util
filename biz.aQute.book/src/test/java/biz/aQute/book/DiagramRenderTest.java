package biz.aQute.book;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Ignore;
import org.junit.Test;

import net.nextencia.rrdiagram.grammar.model.BNFToGrammar;
import net.nextencia.rrdiagram.grammar.model.Grammar;
import net.nextencia.rrdiagram.grammar.model.GrammarToRRDiagram;
import net.nextencia.rrdiagram.grammar.model.Rule;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagramToSVG;

public class DiagramRenderTest {

	@Ignore
	@Test
	public void testRender() throws IOException {
		BNFToGrammar bnfToGrammar = new BNFToGrammar();
		Grammar grammar = bnfToGrammar.convert(new StringReader("H2_SELECT = \n" + "'SELECT' [ 'TOP' term ] "));

		GrammarToRRDiagram grammarToRRDiagram = new GrammarToRRDiagram();
		for (Rule rule : grammar.getRules()) {
			RRDiagram rrDiagram = grammarToRRDiagram.convert(rule);
			RRDiagramToSVG rrDiagramToSVG = new RRDiagramToSVG();
			String svg = rrDiagramToSVG.convert(rrDiagram);
			System.out.println(svg);
		}

	}
}
