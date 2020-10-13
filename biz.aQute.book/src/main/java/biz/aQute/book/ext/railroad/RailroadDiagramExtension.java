package biz.aQute.book.ext.railroad;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.MutableDataHolder;

public class RailroadDiagramExtension implements HtmlRenderer.HtmlRendererExtension {
	@Override
	public void rendererOptions(MutableDataHolder options) {}

	@Override
	public void extend(HtmlRenderer.Builder htmlRendererBuilder, String rendererType) {
		htmlRendererBuilder.nodeRendererFactory(new RailroadDiagramRenderer.Factory());
	}

	public static RailroadDiagramExtension create() {
		return new RailroadDiagramExtension();
	}
}
