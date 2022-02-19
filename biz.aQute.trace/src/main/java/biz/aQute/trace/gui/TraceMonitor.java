package biz.aQute.trace.gui;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Component;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.justif.Justif;
import biz.aQute.trace.activate.ActivationTracer;
import biz.aQute.trace.activate.ActivationTracer.Event;

@GogoCommand(scope = "trace", function = {
	"trace", "clear", "traces", "dump", "trace", "untrace", "debug", "man"
})
@Component(property = {
	InventoryPrinter.NAME + "=biz-aQute-trace", //
	InventoryPrinter.TITLE + "=Tracer", //
	InventoryPrinter.FORMAT + "=TEXT", //
	InventoryPrinter.FORMAT + "=HTML", //
	InventoryPrinter.FORMAT + "=JSON"
})
public class TraceMonitor implements InventoryPrinter {

	@Descriptor("Clear the monitor event queue")
	public void clear() {
		ActivationTracer.clear();
	}

	@Descriptor("Show the monitor queue")
	public List<Event> traces() {
		return ActivationTracer.list();
	}

	@Descriptor("Show the monitor queue in specific format: json html or text (default)")
	public String dump(String type) throws Exception {
		switch (type) {
			case "json" :
				return toJson();

			case "html" :
				return toHtml();

			case "text" :
				return toText();

			default :
				System.out.println("No such format " + type);
				return null;
		}
	}

	public String json() throws Exception {
		return toJson();
	}

	@Override
	public void print(PrintWriter printWriter, Format format, boolean isZip) {

		switch (format.toString()) {
			case "TEXT" :
				printWriter.println(toText());
				break;
			case "HTML" :
				printWriter.println(toHtml());
				break;
			case "JSON" :
				try {
					printWriter.println(toJson());
				} catch (Exception e) {
					Exceptions.duck(e);
				}
				break;

			default :
				throw new IllegalArgumentException("Unsupported format " + format);
		}
	}

	private String toHtml() {
		try {
			URL url = getClass().getResource("/traces.html");
			String html = IO.collect(url);
			String json = toJson();

			StringBuilder sb = new StringBuilder(html);
			int n = sb.indexOf("\"$$$\"");
			sb.replace(n, n + 5, json);
			return sb.toString();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private String toJson() throws Exception {
		return new JSONCodec().enc()
			.put(ActivationTracer.list())
			.toString();
	}

	private String toText() {
		try (Formatter f = new Formatter()) {
			f.format("Id;Start(ns);Duration(ns);Bundle;ThreadName;Parent;Children;Method\n");

			ActivationTracer.list()
				.forEach(e -> {
					f.format("%s;%s;%s;%s;%s;%s;%s;%s\n", e.id, e.begin, e.end - e.begin, e.bundle, e.thread, e.prevId,
						e.next, e.methodName);
				});

			return f.toString();
		}
	}

	@Descriptor("Trace a method in a class")
	public void trace(@Descriptor("<class-fqn>:<method>:<action>") String spec) {
		ActivationTracer.trace(spec);
	}

	public Collection<String[]> trace() {
		return ActivationTracer.extra.values();
	}

	@Descriptor("Trace a method in a class, format: trace <fqn>:<method>:<action> or trace <fqn>")
	public void untrace(@Descriptor("<class-fqn>:<method>:<action> or ") String spec) {
		ActivationTracer.untrace(spec);
	}

	public boolean debug() {
		return ActivationTracer.debug = !ActivationTracer.debug;
	}

	public String man() throws IOException {
		String s= IO.collect(TraceMonitor.class.getResource("/readme.md"));
		Justif j = new Justif(120);
		j.formatter().format("%s", s);
		return j.wrap();
	}
}
