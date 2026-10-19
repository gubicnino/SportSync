package si.um.feri.sportsync.xmljson.povezava;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import si.um.feri.sportsync.xmljson.model.Ekipa;
import si.um.feri.sportsync.xmljson.model.EventStatus;
import si.um.feri.sportsync.xmljson.model.EventType;
import si.um.feri.sportsync.xmljson.model.Sport;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Dogodek, v katerem sta ID-ja ekip zamenjana s celima ekipama. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PovezanDogodek(
		@JacksonXmlProperty(isAttribute = true) String id,
		Sport sport,
		EventType tip,
		String tekmovanje,
		String naslov,
		LocalDate datum,
		@JsonFormat(pattern = "HH:mm") LocalTime ura,
		String prizorisce,
		EventStatus status,
		Ekipa domaci,
		Ekipa gostje,
		Integer rezultatDomaci,
		Integer rezultatGostje,
		String zmagovalec) {

	public boolean nastopaEkipa(String ekipaId) {
		return (domaci != null && domaci.id().equals(ekipaId))
				|| (gostje != null && gostje.id().equals(ekipaId));
	}

	/** Rezultat za izpis: {@code "2 : 1"}, pri F1 {@code "1. Lando Norris"}, brez rezultata {@code "-"}. */
	public String prikazRezultata() {
		if (zmagovalec != null) {
			return "1. " + zmagovalec;
		}
		if (rezultatDomaci == null || rezultatGostje == null) {
			return "-";
		}
		return rezultatDomaci + " : " + rezultatGostje;
	}
}
