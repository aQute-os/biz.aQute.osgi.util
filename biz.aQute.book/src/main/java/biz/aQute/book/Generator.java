package biz.aQute.book;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.data.MutableDataSet;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.lib.env.Env;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.tag.Tag;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.sed.ReplacerAdapter;
import biz.aQute.book.ext.railroad.RailroadDiagramExtension;
import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;

public class Generator extends Env {
	final static Pattern	UNICODEREF	= Pattern.compile("(?<!\\\\)\\\\u(?<nr>[\\dabcdefABCDEF]{4,4})");
	final static Pattern	HEADER		= Pattern.compile("---\r?\n(?<body>(.*\r?\n)*)---\r?\n", Pattern.MULTILINE);
	final static String[]	TABS		= new String[] {
		"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        "
	};
	final List<Doc>			docs		= new ArrayList<>();
	Document				document;
	Parser					parser;
	HtmlRenderer			renderer;
	boolean					expansion;
	ReplacerAdapter			replacer;
	int						tabwidth	= 4;

	class Doc extends Env {
		final String	content;
		final String	id;
		Document		doc;

		Doc(int n, File f) throws IOException {
			super(Generator.this);
			setProperty("seq", n + "");
			setProperty("path", f.getAbsolutePath());
			setProperty("name", f.getName());
			setProperty("dir", f.getParentFile()
				.getAbsoluteFile()
				.getAbsolutePath());
			String basename = Strings.first(f.getName(), '.')[0];
			this.id = getProperty("id", basename);
			String s = IO.collect(f);
			Matcher matcher = HEADER.matcher(s);
			if (matcher.lookingAt()) {
				String props = matcher.group("body");
				UTF8Properties p = new UTF8Properties();
				StringReader sr = new StringReader(props);
				p.load(sr);
				getProperties().putAll(p);
				Generator.this.getProperties()
					.putAll(p);
				content = tab(s.substring(matcher.end()));
			} else {
				content = tab(s);
			}
		}

		Document parse() {
			if (doc == null) {
				String s = replacer.process(content);
				Generator.this.getInfo(replacer);
				if (expansion) {
					System.out.println("Expand " + id);
					System.out.println(s);
				}
				doc = adjust(parser.parse(s));
			}
			return doc;
		}
	}

	public Generator(Env env) throws IOException {
		super(env);
		setBase(env.getBase());
		this.replacer = new ReplacerAdapter(this);
		this.tabwidth = Integer.parseInt(getProperty("tab", "4"));
	}

	public String generate() throws IOException {

		String template;
		String tpath = getProperty("-template");
		if (tpath == null) {
			error("-template not set");
			return null;
		} else {
			File f = getFile(tpath);
			if (!f.isFile()) {
				error("No template file %s", f);
				return null;
			}
			template = IO.collect(f);
		}

		int n = 0;
		Parameters content = new Parameters(getProperty("-chapters"));
		for (Entry<String, Attrs> e : content.entrySet()) {
			File f = getFile(e.getKey());
			if (!f.isFile()) {
				error("No such chapter file %s", f);
				continue;
			}
			docs.add(new Doc(n++, f));
		}

		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList( //
			AttributesExtension.create(), //
			TablesExtension.create(), //
			TaskListExtension.create(), StrikethroughExtension.create(), RailroadDiagramExtension.create()));

		options.set(Parser.REFERENCES_KEEP, KeepType.LAST);

		// Set GFM table parsing options
		options.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.MIN_HEADER_ROWS, 1)
			.set(TablesExtension.MAX_HEADER_ROWS, 1)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.WITH_CAPTION, false)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

		// Setup List Options for GitHub profile which is kramdown for documents
		options.setFrom(ParserEmulationProfile.GITHUB_DOC);

		parser = Parser.builder(options)
			.build();
		renderer = HtmlRenderer.builder(options)
			.build();

		String s = replacer.process(template);
		getInfo(replacer);
		return s;
	}

	protected Document adjust(Document document) {
		return document;
	}

	public String _FRONT(String args[]) {
		return "<!-- front -->\n";
	}

	public String _TOC(String args[]) {
		return "<!-- toc -->\n";
	}

	public String _CONTENT(String args[]) {
		StringBuffer sw = new StringBuffer(6_000_000);

		for (Doc doc : docs) {
			sw.append("\n<section class=doc id='" + Tag.escape(doc.id) + "'>\n");
			Document d = doc.parse();

			String rendered = renderer.render(d);
			Matcher m = UNICODEREF.matcher(rendered);
			while (m.find()) {
				m.appendReplacement(sw, "");
				int nr = Integer.parseInt(m.group("nr"), 16);
				switch (nr) {
					case '$' :
						sw.append('\\'); // needs escaping since bnd will
											// process it

						// FALL THROUGH

					default :
						sw.append((char) nr);
						break;
				}
			}
			m.appendTail(sw);
			sw.append("\n</section>\n");
		}
		return sw.toString();
	}

	public String _INDEX(String args[]) {
		return "<!-- index -->";
	}

	public String _STYLES(String args[]) {
		StringWriter sw = new StringWriter();

		sw.append("<!-- scripts -->\n");
		Parameters scripts = new Parameters(getProperty("-styles"));
		for (Entry<String, Attrs> e : scripts.entrySet()) {
			File f = getFile(e.getKey());
			if (!f.isFile()) {
				error("No such script file %s", f);
				sw.append("<!-- missing ")
					.append(f.getAbsolutePath())
					.append(" -->\n");
				continue;
			}
			String name = f.getName()
				.toLowerCase();
			if (name.endsWith(".css")) {
				sw.append("<link rel='stylesheet' href='")
					.append(relative(f))
					.append("' type='text/css'/>\n");
			} else if (name.endsWith(".scss")) {
				URI inputFile = f.toURI();
				URI outputFile = new File(getBase(), "somefile.html").toURI();

				Compiler compiler = new Compiler();
				Options options = new Options();

				try {
					Output output = compiler.compileFile(inputFile, outputFile, options);
					sw.append("<style>\n");
					sw.append(output.getCss());
					sw.append("\n</style>\n");
				} catch (CompilationException ex) {
					error("SASS Compile failed %s", ex.getErrorText());
				}

			}
		}
		return sw.toString();
	}

	private String relative(File f) {
		URI to = f.getAbsoluteFile()
			.toURI()
			.normalize();
		URI base = getBase().getAbsoluteFile()
			.toURI()
			.normalize();
		return base.relativize(to)
			.toString();
	}

	public String _SCRIPTS(String args[]) throws FileNotFoundException, IOException {
		StringWriter sw = new StringWriter();
		sw.append("<!-- scripts -->\n");
		Parameters scripts = new Parameters(getProperty("-scripts"));
		for (Entry<String, Attrs> e : scripts.entrySet()) {
			File f = getFile(e.getKey());
			if (!f.isFile()) {
				error("No such script file %s", f);
				sw.append("<!-- missing ")
					.append(f.getAbsolutePath())
					.append(" -->\n");
				continue;
			}
			sw.append("<script  src='")
				.append(relative(f))
				.append("' type='application/javascript' defer ></script>\n");
		}

		return sw.toString();
	}

	public void xpansion() {
		this.expansion = true;
	}

	private String tab(String s) {
		StringBuilder sb = new StringBuilder(s);
		int x = 0;
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			switch (c) {
				case '\n' :
				case '\r' :
					x = 0;
					break;

				case '\t' :
					int r = tabwidth - (x % tabwidth);
					sb.replace(i, i + 1, TABS[r]);
					i += r;
					break;
				default :
					x++;
					break;
			}

		}

		return sb.toString();
	}
}
