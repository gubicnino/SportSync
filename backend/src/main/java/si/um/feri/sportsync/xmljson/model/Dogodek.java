package si.um.feri.sportsync.xmljson.model;

import java.time.LocalDate;
import java.time.LocalTime;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Dogodek iz {@code data/dogodki.xml}. {@code domaciId} in {@code gostjeId} se sklicujeta na
 * {@link Ekipa#id()}; pri F1 sta izpuscena, rezultat pa manjka pri neodigranih dogodkih.
 */
public record Dogodek(
		@JacksonXmlProperty(isAttribute = true) String id,
		Sport sport,
		EventType tip,
		String tekmovanje,
		String naslov,
		LocalDate datum,
		LocalTime ura,
		String prizorisce,
		EventStatus status,
		String domaciId,
		String gostjeId,
		Integer rezultatDomaci,
		Integer rezultatGostje,
		String zmagovalec) {
}
