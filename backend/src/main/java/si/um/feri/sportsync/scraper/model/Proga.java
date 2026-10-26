package si.um.feri.sportsync.scraper.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Proga (prizorisce dirke) s strani {@code formula1.com/en/racing/{leto}/{id}}. Dolzine so v km,
 * rekord kroga je cas (npr. {@code 1:20.901}) z voznikom in letom.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Proga(
		String id,
		Integer krog,
		String termin,
		String uradnoIme,
		String kraj,
		String drzava,
		Double dolzinaKm,
		Integer steviloKrogov,
		Double dolzinaDirkeKm,
		Integer prvaDirka,
		String rekordKroga,
		String rekordVoznik,
		Integer rekordLeto,
		String zemljevidUrl,
		String url) {
}
