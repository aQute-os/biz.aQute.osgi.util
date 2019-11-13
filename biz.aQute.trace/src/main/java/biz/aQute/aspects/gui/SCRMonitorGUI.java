package biz.aQute.aspects.gui;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Formatter;
import java.util.List;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.annotations.Component;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import biz.aQute.trace.activate.ActivationTracer;
import biz.aQute.trace.activate.ActivationTracer.Event;

@Component(property = {
	"osgi.command.scope=aspect", "osgi.command.function=clear", //
	"osgi.command.function=traces", //
	InventoryPrinter.NAME + "=SCR Monitor", //
	InventoryPrinter.TITLE + "=SCR Monitor", //
	InventoryPrinter.FORMAT + "=TEXT", //
	InventoryPrinter.FORMAT + "=HTML", //
	InventoryPrinter.FORMAT + "=JSON"
})
public class SCRMonitorGUI implements InventoryPrinter {

	@Descriptor("Clear the monitor event queue")
	public void clear() {
		ActivationTracer.clear();
	}

	@Descriptor("Show the monitor queue")
	public List<Event> traces() {
		return ActivationTracer.list();
	}

	public String traces(@Parameter(names = "t", absentValue = "text") String type) throws Exception {
		switch (type) {
			case "json" :
				return toJson();

			default :
				return toText();
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
			int n = sb.indexOf("$$$");
			sb.replace(n, n + 3, json);
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
						e.next,
					e.methodName);
			});

			return f.toString();
		}
	}

}
