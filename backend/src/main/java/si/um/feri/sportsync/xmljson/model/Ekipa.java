package si.um.feri.sportsync.xmljson.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Ekipa iz {@code data/ekipe.xml}; pri izvozu se prazna polja (npr. {@code grbUrl}) izpustijo. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Ekipa(
		@JacksonXmlProperty(isAttribute = true) String id,
		Sport sport,
		String ime,
		String kratkoIme,
		String tekmovanje,
		String mesto,
		Integer ustanovljena,
		String grbUrl) {
}
