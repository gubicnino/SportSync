package si.um.feri.sportsync.scraper.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Mostvo s strani {@code formula1.com/en/teams/{id}}; {@code vozniki} so ID-ji iz {@link Voznik#id()}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Mostvo(
		String id,
		String ime,
		String polnoIme,
		String sedez,
		String sefEkipe,
		String tehnicniSef,
		String sasija,
		String pogonskaEnota,
		String rezervniVozniki,
		Integer prviNastop,
		List<String> vozniki,
		String logoUrl,
		String dirkalnikUrl,
		String url,
		Map<String, Object> statistikaSezone,
		Map<String, Object> statistikaKariere) {
}
