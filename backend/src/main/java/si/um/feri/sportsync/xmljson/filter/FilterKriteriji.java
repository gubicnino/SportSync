package si.um.feri.sportsync.xmljson.filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import si.um.feri.sportsync.xmljson.model.EventStatus;
import si.um.feri.sportsync.xmljson.model.Sport;

/** Izbrani filtri; {@code null} pomeni brez omejitve. Vsi podani pogoji veljajo hkrati (AND). */
public record FilterKriteriji(
		Sport sport,
		String ekipaId,
		String tekmovanje,
		EventStatus status,
		LocalDate odDatuma,
		LocalDate doDatuma,
		String uporabnikId) {

	public FilterKriteriji {
		if (odDatuma != null && doDatuma != null && odDatuma.isAfter(doDatuma)) {
			throw new IllegalArgumentException(
					"Datum od (" + odDatuma + ") je za datumom do (" + doDatuma + ").");
		}
	}

	public String opis() {
		List<String> deli = new ArrayList<>();
		dodaj(deli, "sport", sport);
		dodaj(deli, "ekipa", ekipaId);
		dodaj(deli, "tekmovanje", tekmovanje);
		dodaj(deli, "status", status);
		dodaj(deli, "od", odDatuma);
		dodaj(deli, "do", doDatuma);
		dodaj(deli, "uporabnik", uporabnikId);
		return deli.isEmpty() ? "brez filtrov (vsi dogodki)" : String.join(", ", deli);
	}

	private static void dodaj(List<String> deli, String ime, Object vrednost) {
		if (vrednost != null) {
			deli.add(ime + "=" + vrednost);
		}
	}
}
