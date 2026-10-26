package si.um.feri.sportsync.scraper;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import si.um.feri.sportsync.scraper.F1Parser.KrogDirke;
import si.um.feri.sportsync.scraper.model.Mostvo;
import si.um.feri.sportsync.scraper.model.Proga;
import si.um.feri.sportsync.scraper.model.Voznik;

/**
 * Prenasa strani s formula1.com in jih preda {@link F1Parser}. Med zahtevki caka {@code zamikMs},
 * da strezniku ne posljemo prevec zahtevkov naenkrat ({@code robots.txt} scraping dovoljuje).
 * Ce posamezna stran ne uspe, se izpise opozorilo in nadaljuje z naslednjo.
 */
public class F1Scraper {

	public static final String OSNOVA = "https://www.formula1.com";
	private static final String USER_AGENT =
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";
	private static final int TIMEOUT_MS = 20_000;
	private static final int POSKUSOV = 3;

	private final long zamikMs;
	private final PrintStream izhod;
	private boolean prviZahtevek = true;

	public F1Scraper(long zamikMs, PrintStream izhod) {
		this.zamikMs = zamikMs;
		this.izhod = izhod;
	}

	public List<Voznik> vozniki() {
		List<String> idji = F1Parser.povezave(prenesi(OSNOVA + "/en/drivers"), "/en/drivers/");
		return preberiVse("voznik", idji, id -> {
			String url = OSNOVA + "/en/drivers/" + id;
			return F1Parser.voznik(prenesi(url), id, url);
		});
	}

	public List<Mostvo> mostva() {
		List<String> idji = F1Parser.povezave(prenesi(OSNOVA + "/en/teams"), "/en/teams/");
		return preberiVse("mostvo", idji, id -> {
			String url = OSNOVA + "/en/teams/" + id;
			return F1Parser.mostvo(prenesi(url), id, url);
		});
	}

	public List<Proga> proge(int leto) {
		List<KrogDirke> koledar = F1Parser.koledar(prenesi(OSNOVA + "/en/racing/" + leto), leto);
		return preberiVse("proga", koledar, dirka -> {
			String url = OSNOVA + "/en/racing/" + leto + "/" + dirka.id();
			return F1Parser.proga(prenesi(url), dirka, url);
		});
	}

	private <V, R> List<R> preberiVse(String vrsta, List<V> vhodi, Function<V, R> preberi) {
		List<R> rezultat = new ArrayList<>();
		for (int i = 0; i < vhodi.size(); i++) {
			V vhod = vhodi.get(i);
			String opis = vhod instanceof KrogDirke dirka ? dirka.id() : String.valueOf(vhod);
			try {
				rezultat.add(preberi.apply(vhod));
				izhod.printf("  [%d/%d] %s %s%n", i + 1, vhodi.size(), vrsta, opis);
			} catch (UncheckedIOException e) {
				izhod.printf("  [%d/%d] Opozorilo: %s %s preskocen (%s)%n",
						i + 1, vhodi.size(), vrsta, opis, e.getCause().getMessage());
			}
		}
		return List.copyOf(rezultat);
	}

	/** Ob napaki (npr. obcasni HTTP 500) poskusi se {@code POSKUSOV - 1}-krat z vse daljsim zamikom. */
	private Document prenesi(String url) {
		pocakaj(zamikMs);
		for (int poskus = 1;; poskus++) {
			try {
				return Jsoup.connect(url)
						.userAgent(USER_AGENT)
						.timeout(TIMEOUT_MS)
						.get();
			} catch (IOException e) {
				if (poskus == POSKUSOV) {
					throw new UncheckedIOException("Strani " + url + " ni mogoce prenesti: " + e.getMessage(), e);
				}
				pocakaj(zamikMs * 2 * poskus);
			}
		}
	}

	private void pocakaj(long ms) {
		if (prviZahtevek) {
			prviZahtevek = false;
			return;
		}
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Scraping prekinjen.", e);
		}
	}
}
