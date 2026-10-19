package si.um.feri.sportsync.xmljson.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import si.um.feri.sportsync.xmljson.model.EventStatus;
import si.um.feri.sportsync.xmljson.model.Sport;
import si.um.feri.sportsync.xmljson.povezava.PovezaniPodatki;

/**
 * Vprasa za vsak filter posebej (Enter = brez omejitve). Ob koncu vhoda (EOF) vrne prazen
 * {@link Optional}, da zagon brez terminala (npr. testi) ne obvisi.
 */
public class InteraktivniMeni {

	private final BufferedReader vhod;
	private final PrintStream izhod;

	public InteraktivniMeni(BufferedReader vhod, PrintStream izhod) {
		this.vhod = vhod;
		this.izhod = izhod;
	}

	public Optional<FilterKriteriji> vprasaj(PovezaniPodatki podatki) {
		izhod.println();
		izhod.println("=== Izbira filtra (Enter = brez omejitve) ===");
		try {
			Sport sport = vprasajZa("Sport " + Arrays.toString(Sport.values()), FilterParser::parseSport);

			izhod.println("Ekipe: " + podatki.ekipe().values().stream()
					.map(e -> e.id() + " " + e.ime())
					.collect(Collectors.joining(", ")));
			String ekipa = vprasajZa("ID ekipe", id -> obstaja(id, podatki.ekipe().keySet(), "Ekipa"));

			String tekmovanje = vprasajZa("Tekmovanje (npr. Premier League, NBA)", Function.identity());
			EventStatus status = vprasajZa("Status " + Arrays.toString(EventStatus.values()),
					FilterParser::parseStatus);
			LocalDate od = vprasajZa("Datum od (YYYY-MM-DD)", v -> FilterParser.parseDatum(v, "od"));
			LocalDate doDatuma = vprasajZa("Datum do (YYYY-MM-DD)", v -> datumDo(v, od));

			izhod.println("Uporabniki: " + podatki.uporabniki().values().stream()
					.map(u -> u.id() + " " + u.prikaznoIme())
					.collect(Collectors.joining(", ")));
			String uporabnik = vprasajZa("ID uporabnika",
					id -> obstaja(id, podatki.uporabniki().keySet(), "Uporabnik"));

			return Optional.of(new FilterKriteriji(sport, ekipa, tekmovanje, status, od, doDatuma, uporabnik));
		} catch (KonecVhoda e) {
			return Optional.empty();
		}
	}

	/** Ponavlja vprasanje, dokler vnos ni veljaven; prazen vnos vrne {@code null}. */
	private <T> T vprasajZa(String poziv, Function<String, T> pretvori) {
		while (true) {
			izhod.print(poziv + ": ");
			izhod.flush();
			String vrstica = preberiVrstico();
			if (vrstica == null) {
				throw new KonecVhoda();
			}
			if (vrstica.isBlank()) {
				return null;
			}
			try {
				return pretvori.apply(vrstica.trim());
			} catch (IllegalArgumentException e) {
				izhod.println("  " + e.getMessage());
			}
		}
	}

	private String preberiVrstico() {
		try {
			return vhod.readLine();
		} catch (IOException e) {
			throw new UncheckedIOException("Napaka pri branju vhoda.", e);
		}
	}

	private static String obstaja(String id, Collection<String> znani, String vrsta) {
		if (!znani.contains(id)) {
			throw new IllegalArgumentException(vrsta + " z ID '" + id + "' ne obstaja.");
		}
		return id;
	}

	private static LocalDate datumDo(String vrednost, LocalDate od) {
		LocalDate doDatuma = FilterParser.parseDatum(vrednost, "do");
		if (od != null && doDatuma.isBefore(od)) {
			throw new IllegalArgumentException("Datum do mora biti enak ali za datumom od (" + od + ").");
		}
		return doDatuma;
	}

	private static final class KonecVhoda extends RuntimeException {
		KonecVhoda() {
			super(null, null, false, false);
		}
	}
}
