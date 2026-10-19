package si.um.feri.sportsync.xmljson.filter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;

import si.um.feri.sportsync.xmljson.model.EventStatus;
import si.um.feri.sportsync.xmljson.model.Sport;

/** Pretvori argumente ukazne vrstice ({@code --sport=F1 --od=2026-10-01 ...}) v {@link FilterKriteriji}. */
public final class FilterParser {

	public static final List<String> MOZNOSTI =
			List.of("sport", "ekipa", "tekmovanje", "status", "od", "do", "uporabnik");

	/** Argumenti, ki jih uporablja Spring sam in niso filtri. */
	private static final List<String> SISTEMSKE_PREDPONE = List.of("spring.", "logging.", "xmljson.", "debug", "trace");

	private FilterParser() {
	}

	/** Ali je podan vsaj en (ne-sistemski) argument; sicer se uporabi interaktivni meni. */
	public static boolean imaFiltre(ApplicationArguments args) {
		return args.getOptionNames().stream().anyMatch(ime -> !jeSistemski(ime));
	}

	public static FilterKriteriji izArgumentov(ApplicationArguments args) {
		for (String ime : args.getOptionNames()) {
			if (!jeSistemski(ime) && !MOZNOSTI.contains(ime)) {
				throw new IllegalArgumentException("Neznan argument --" + ime + ". Dovoljeni: --"
						+ String.join(", --", MOZNOSTI) + ".");
			}
		}
		return new FilterKriteriji(
				vrednost(args, "sport").map(FilterParser::parseSport).orElse(null),
				vrednost(args, "ekipa").orElse(null),
				vrednost(args, "tekmovanje").orElse(null),
				vrednost(args, "status").map(FilterParser::parseStatus).orElse(null),
				vrednost(args, "od").map(v -> parseDatum(v, "od")).orElse(null),
				vrednost(args, "do").map(v -> parseDatum(v, "do")).orElse(null),
				vrednost(args, "uporabnik").orElse(null));
	}

	public static Sport parseSport(String vrednost) {
		return parseEnum(Sport.class, vrednost, "sport");
	}

	public static EventStatus parseStatus(String vrednost) {
		return parseEnum(EventStatus.class, vrednost, "status");
	}

	public static LocalDate parseDatum(String vrednost, String ime) {
		try {
			return LocalDate.parse(vrednost.trim());
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Neveljaven datum '" + vrednost + "' za " + ime
					+ " (pricakovana oblika YYYY-MM-DD).", e);
		}
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> tip, String vrednost, String ime) {
		try {
			return Enum.valueOf(tip, vrednost.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Neveljaven " + ime + " '" + vrednost + "'. Mozne vrednosti: "
					+ Arrays.toString(tip.getEnumConstants()) + ".", e);
		}
	}

	private static Optional<String> vrednost(ApplicationArguments args, String ime) {
		List<String> vrednosti = args.getOptionValues(ime);
		if (vrednosti == null) {
			return Optional.empty();
		}
		String zadnja = vrednosti.isEmpty() ? "" : vrednosti.get(vrednosti.size() - 1);
		if (zadnja.isBlank()) {
			throw new IllegalArgumentException("Argument --" + ime + " nima vrednosti (uporabi --" + ime + "=...).");
		}
		return Optional.of(zadnja.trim());
	}

	private static boolean jeSistemski(String ime) {
		return SISTEMSKE_PREDPONE.stream().anyMatch(ime::startsWith);
	}
}
