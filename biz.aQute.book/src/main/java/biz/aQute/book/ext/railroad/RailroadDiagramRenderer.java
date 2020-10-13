package biz.aQute.book.ext.railroad;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import net.nextencia.rrdiagram.grammar.model.BNFToGrammar;
import net.nextencia.rrdiagram.grammar.model.Grammar;
import net.nextencia.rrdiagram.grammar.model.GrammarToRRDiagram;
import net.nextencia.rrdiagram.grammar.model.Rule;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagramToSVG;

class RailroadDiagramRenderer implements NodeRenderer {
	final static Pattern RAILROAD = Pattern.compile("(\\s*\r?\n)*```railroad\r?\n(?<code>(.*\r?\n)*)```");

	public RailroadDiagramRenderer(DataHolder options) {}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
		return new HashSet<>(Arrays.asList(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render)));
	}

	private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
		try {
			String s = node.getChars()
				.toString();
			Matcher m = RAILROAD.matcher(s);
			if (m.lookingAt()) {
				html.line();
				String svg = toSVG(m.group("code"));
				html.append(svg);
			} else {
				context.delegateRender();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String toSVG(String group) throws IOException {
		StringWriter sw = new StringWriter();
		sw.append("<div class='railroad'>\n");
		BNFToGrammar bnfToGrammar = new BNFToGrammar();
		Grammar grammar = bnfToGrammar.convert(new StringReader(group));

		GrammarToRRDiagram grammarToRRDiagram = new GrammarToRRDiagram();
		for (Rule rule : grammar.getRules()) {
			RRDiagram rrDiagram = grammarToRRDiagram.convert(rule);
			RRDiagramToSVG rrDiagramToSVG = new RRDiagramToSVG();
			String svg = rrDiagramToSVG.convert(rrDiagram);
			sw.append("    ")
				.append(svg)
				.append("\n");
			if (rule.getName() != null) {
				sw.append("<figcaption>")
					.append(rule.getName())
					.append("<figccaption>");
			}
		}
		sw.append("</div>\n");
		return sw.toString();
	}

	public static class Factory implements NodeRendererFactory {

		@Override
		public NodeRenderer apply(DataHolder options) {
			return new RailroadDiagramRenderer(options);
		}
	}

}
