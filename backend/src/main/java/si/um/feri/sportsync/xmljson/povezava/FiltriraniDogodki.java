package si.um.feri.sportsync.xmljson.povezava;

import java.util.List;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Korenski element za {@code filtrirano.xml}: {@code <filtriraniDogodki><dogodek .../>...}. */
@JacksonXmlRootElement(localName = "filtriraniDogodki")
public record FiltriraniDogodki(
		@JacksonXmlElementWrapper(useWrapping = false)
		@JacksonXmlProperty(localName = "dogodek")
		List<PovezanDogodek> dogodki) {

	public FiltriraniDogodki {
		dogodki = List.copyOf(dogodki);
	}
}
