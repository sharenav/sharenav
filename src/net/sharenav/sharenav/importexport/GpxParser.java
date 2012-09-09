package net.sharenav.sharenav.importexport;

import java.io.InputStream;

public interface GpxParser {
	public boolean parse(InputStream in, XmlParserContentHandler contentH);

}
