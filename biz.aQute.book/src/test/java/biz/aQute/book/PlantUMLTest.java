package biz.aQute.book;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Ignore;
import org.junit.Test;

import aQute.lib.io.IO;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class PlantUMLTest {
	@Ignore
	@Test
	public void testSimple() throws IOException {
		String source = "@startuml\n";
		source += "Bob -> Alice : hello\n";
		source += "@enduml\n";

		SourceStringReader reader = new SourceStringReader(source);
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			reader.outputImage(os, new FileFormatOption(FileFormat.SVG));

			String svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
			System.out.println(svg);
		}
	}

	@Ignore
	@Test
	public void testStyled() throws IOException, FontFormatException {
		File file = IO.getFile("/Users/aqute/Desktop/Dropbox/alloy-book/fonts/PRODOBookcItalic.ttf");
		Font font = Font.createFont(Font.TRUETYPE_FONT, file);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		ge.registerFont(font);

		String source = IO.collect("test/plantuml/fonts.uml");

		SourceStringReader reader = new SourceStringReader(source);
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			reader.outputImage(os, new FileFormatOption(FileFormat.SVG));

			String svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
			System.out.println(svg);
		}
	}
}
