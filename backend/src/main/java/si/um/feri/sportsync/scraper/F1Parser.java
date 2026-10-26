package si.um.feri.sportsync.scraper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import si.um.feri.sportsync.scraper.model.Mostvo;
import si.um.feri.sportsync.scraper.model.Proga;
import si.um.feri.sportsync.scraper.model.Voznik;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Razclenjevalnik strani formula1.com (brez omrezja – dobi ze nalozen {@link Document}).
 * Stran je streznisko izrisana (Next.js), zato so vsi podatki ze v HTML-ju: statistika v parih
 * {@code <dt>}/{@code <dd>}, kraj dirke pa v JSON-LD ({@code schema.org/SportsEvent}).
 */
public final class F1Parser {

	/** Dirka s strani sezone; karta dirke ima npr. {@code ROUND 13}, testiranja ga nimajo. */
	public record KrogDirke(String id, int krog, String termin, String drzava) {
	}

	/** Statistika na strani voznika/mostva: pred {@code dl.rows} je sezona, v njem kariera, za njim profil. */
	record Statistika(Map<String, String> sezona, Map<String, String> kariera, Map<String, String> profil) {
	}

	private static final Pattern KROG = Pattern.compile("^ROUND\\s+(\\d+)$");
	/** {@code 04 - 06 Sep}, {@code 30 Oct - 01 Nov}; naslednja dirka ima mesec z velikimi crkami ({@code 23 - 25 OCT}). */
	private static final Pattern TERMIN = Pattern.compile("^\\d{1,2}(\\s+[a-z]{3})?\\s*-\\s*\\d{1,2}\\s+[a-z]{3}$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern VOZNIK_IN_LETO = Pattern.compile("^(.+?)\\s*\\((\\d{4})\\)$");
	private static final Pattern CELO_STEVILO = Pattern.compile("^-?\\d+$");
	private static final Pattern DECIMALNO = Pattern.compile("^-?\\d+\\.\\d+$");
	private static final DateTimeFormatter DATUM_ROJSTVA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private F1Parser() {
	}

	/** ID-ji (prvi del poti za predpono) vseh povezav, npr. {@code /en/drivers/} → {@code max-verstappen}. */
	public static List<String> povezave(Document doc, String predpona) {
		Set<String> idji = new LinkedHashSet<>();
		for (Element a : doc.select("a[href^=" + predpona + "]")) {
			String id = a.attr("href").substring(predpona.length()).split("[/?#]", 2)[0];
			if (!id.isBlank()) {
				idji.add(id);
			}
		}
		return List.copyOf(idji);
	}

	/** Dirke sezone v vrstnem redu krogov; testiranja (brez {@code ROUND n}) so izpuscena. */
	public static List<KrogDirke> koledar(Document doc, int leto) {
		String predpona = "/en/racing/" + leto + "/";
		Map<String, KrogDirke> dirke = new LinkedHashMap<>();
		for (Element karta : doc.select("a[href^=" + predpona + "]")) {
			String id = karta.attr("href").substring(predpona.length()).split("[/?#]", 2)[0];
			// "ROUND 13" in "04 - 06 Sep" sta sosednja <span> brez presledka, zato ne karta.text()
			List<String> besedila = karta.select("span").stream().map(s -> s.ownText().trim()).toList();
			Integer krog = besedila.stream()
					.map(KROG::matcher).filter(Matcher::matches)
					.map(m -> Integer.valueOf(m.group(1)))
					.findFirst().orElse(null);
			if (id.isBlank() || dirke.containsKey(id) || krog == null) {
				continue;
			}
			String termin = besedila.stream().filter(t -> TERMIN.matcher(t).matches()).findFirst().orElse(null);
			Element drzava = karta.selectFirst("p");
			dirke.put(id, new KrogDirke(id, krog, termin, drzava != null ? drzava.text() : null));
		}
		return dirke.values().stream()
				.sorted(Comparator.comparingInt(KrogDirke::krog))
				.toList();
	}

	public static Voznik voznik(Document doc, String id, String url) {
		Element h1 = doc.selectFirst("h1");
		Elements imeDeli = h1 != null ? h1.select("> span") : new Elements();
		// pod imenom so trije <p>: drzava | mostvo | stevilka
		Element podNaslovom = h1 != null ? h1.nextElementSibling() : null;
		List<String> glava = podNaslovom != null ? podNaslovom.select("p").eachText() : List.of();
		Statistika stat = statistika(doc);

		return new Voznik(
				id,
				imeDeli.isEmpty() ? null : imeDeli.first().text(),
				imeDeli.size() > 1 ? imeDeli.last().text() : null,
				celoStevilo(element(glava, 2)),
				element(glava, 0),
				element(glava, 1),
				datum(stat.profil().get("Date of Birth")),
				stat.profil().get("Place of Birth"),
				slika(doc, "img[src~=/common/f1/\\d{4}/.+right\\.webp]"),
				url,
				pretvori(stat.sezona()),
				pretvori(stat.kariera()));
	}

	public static Mostvo mostvo(Document doc, String id, String url) {
		Element h1 = doc.selectFirst("h1");
		Statistika stat = statistika(doc);
		Map<String, String> profil = stat.profil();

		return new Mostvo(
				id,
				h1 != null ? h1.text() : null,
				profil.get("Full Team Name"),
				profil.get("Base"),
				profil.get("Team Chief"),
				profil.get("Technical Chief"),
				profil.get("Chassis"),
				profil.get("Power Unit"),
				profil.get("Reserve Driver"),
				celoStevilo(profil.get("First Team Entry")),
				povezave(doc, "/en/drivers/"),
				slika(doc, "img[src~=/common/f1/\\d{4}/.+logo]"),
				slika(doc, "img[src~=/common/f1/\\d{4}/.+carright]"),
				url,
				pretvori(stat.sezona()),
				pretvori(stat.kariera()));
	}

	public static Proga proga(Document doc, KrogDirke dirka, String url) {
		Map<String, String> podatki = new LinkedHashMap<>();
		String rekordOpomba = null;
		for (Element dt : doc.select("dt")) {
			Element dd = dt.nextElementSibling();
			if (dd == null || !dd.is("dd")) {
				continue;
			}
			podatki.putIfAbsent(dt.text(), dd.text());
			// za rekordom kroga je v <span> se "Voznik (leto)"
			Element opomba = dd.nextElementSibling();
			if (dt.text().equalsIgnoreCase("Fastest lap time") && opomba != null && !opomba.text().isBlank()) {
				rekordOpomba = opomba.text();
			}
		}
		Matcher rekord = rekordOpomba != null ? VOZNIK_IN_LETO.matcher(rekordOpomba) : null;
		boolean imaLeto = rekord != null && rekord.matches();
		JsonNode dogodek = jsonLdDogodek(doc);

		return new Proga(
				dirka.id(),
				dirka.krog(),
				dirka.termin(),
				besedilo(dogodek, "name"),
				dogodek != null ? besedilo(dogodek.path("location"), "name") : null,
				dirka.drzava(),
				kilometri(podatki.get("Circuit Length")),
				celoStevilo(podatki.get("Number of Laps")),
				kilometri(podatki.get("Race Distance")),
				celoStevilo(podatki.get("First Grand Prix")),
				podatki.get("Fastest lap time"),
				imaLeto ? rekord.group(1) : rekordOpomba,
				imaLeto ? Integer.valueOf(rekord.group(2)) : null,
				slika(doc, "img[src*=/track/]"),
				url);
	}

	static Statistika statistika(Document doc) {
		Map<String, String> sezona = new LinkedHashMap<>();
		Map<String, String> kariera = new LinkedHashMap<>();
		Map<String, String> profil = new LinkedHashMap<>();
		boolean poKarieri = false;
		for (Element dl : doc.select("dl")) {
			boolean jeKariera = dl.className().contains("DataGrid-module_rows");
			Map<String, String> cilj = jeKariera ? kariera : (poKarieri ? profil : sezona);
			poKarieri |= jeKariera;
			for (Element dt : dl.select("dt")) {
				Element dd = dt.nextElementSibling();
				if (dd != null && dd.is("dd")) {
					cilj.putIfAbsent(dt.text(), dd.text());
				}
			}
		}
		return new Statistika(sezona, kariera, profil);
	}

	/** {@code "236"} → 236, {@code "3680.5"} → 3680.5; ostalo (npr. {@code "1 (x73)"}, {@code "4th"}) ostane besedilo. */
	static Map<String, Object> pretvori(Map<String, String> surovo) {
		Map<String, Object> rezultat = new LinkedHashMap<>();
		surovo.forEach((oznaka, vrednost) -> {
			if (CELO_STEVILO.matcher(vrednost).matches()) {
				rezultat.put(oznaka, Long.valueOf(vrednost));
			} else if (DECIMALNO.matcher(vrednost).matches()) {
				rezultat.put(oznaka, Double.valueOf(vrednost));
			} else {
				rezultat.put(oznaka, vrednost);
			}
		});
		return rezultat;
	}

	private static JsonNode jsonLdDogodek(Document doc) {
		for (Element skripta : doc.select("script[type=application/ld+json]")) {
			try {
				JsonNode vozlisce = JSON.readTree(skripta.data());
				if ("SportsEvent".equals(vozlisce.path("@type").asString(""))) {
					return vozlisce;
				}
			} catch (JacksonException e) {
				// neveljaven JSON-LD blok preskocimo; podatki o progi so tudi v HTML-ju
			}
		}
		return null;
	}

	private static String slika(Document doc, String selektor) {
		Element img = doc.selectFirst(selektor);
		return img != null ? img.attr("abs:src") : null;
	}

	private static String besedilo(JsonNode vozlisce, String polje) {
		if (vozlisce == null) {
			return null;
		}
		JsonNode vrednost = vozlisce.path(polje);
		return vrednost.isMissingNode() || vrednost.isNull() ? null : vrednost.asString();
	}

	private static String element(List<String> seznam, int indeks) {
		return indeks < seznam.size() ? seznam.get(indeks) : null;
	}

	private static Integer celoStevilo(String vrednost) {
		return vrednost != null && CELO_STEVILO.matcher(vrednost.trim()).matches()
				? Integer.valueOf(vrednost.trim())
				: null;
	}

	/** {@code "5.793km"} → 5.793 */
	private static Double kilometri(String vrednost) {
		if (vrednost == null) {
			return null;
		}
		String stevilo = vrednost.replaceAll("(?i)\\s*km$", "").trim();
		return DECIMALNO.matcher(stevilo).matches() || CELO_STEVILO.matcher(stevilo).matches()
				? Double.valueOf(stevilo)
				: null;
	}

	/** {@code "30/09/1997"} → 1997-09-30 */
	private static LocalDate datum(String vrednost) {
		if (vrednost == null) {
			return null;
		}
		try {
			return LocalDate.parse(vrednost.trim(), DATUM_ROJSTVA);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
