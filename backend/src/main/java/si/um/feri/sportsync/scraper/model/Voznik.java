package si.um.feri.sportsync.scraper.model;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Voznik s strani {@code formula1.com/en/drivers/{id}}. Statistiki sta preslikavi
 * oznaka → vrednost (npr. {@code "Podiums" → 137}), kot jih prikaze stran.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Voznik(
		String id,
		String ime,
		String priimek,
		Integer stevilka,
		String drzava,
		String mostvo,
		LocalDate datumRojstva,
		String krajRojstva,
		String slikaUrl,
		String url,
		Map<String, Object> statistikaSezone,
		Map<String, Object> statistikaKariere) {
}
