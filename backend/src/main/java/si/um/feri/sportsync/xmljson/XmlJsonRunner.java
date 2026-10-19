package si.um.feri.sportsync.xmljson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import si.um.feri.sportsync.xmljson.filter.EventFilter;
import si.um.feri.sportsync.xmljson.filter.FilterKriteriji;
import si.um.feri.sportsync.xmljson.filter.FilterParser;
import si.um.feri.sportsync.xmljson.filter.InteraktivniMeni;
import si.um.feri.sportsync.xmljson.model.Ekipa;
import si.um.feri.sportsync.xmljson.model.Narocnina;
import si.um.feri.sportsync.xmljson.model.Uporabnik;
import si.um.feri.sportsync.xmljson.povezava.PovezanDogodek;
import si.um.feri.sportsync.xmljson.povezava.PovezaniPodatki;
import tools.jackson.core.JacksonException;

/**
 * Naloga 1 (XML in JSON): ob zagonu aplikacije prebere XML, poveze podatke, filtrira, izpise
 * tabelo ter izvozi {@code filtrirano.json} in {@code filtrirano.xml}. Izklopi se z
 * {@code xmljson.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "xmljson.enabled", havingValue = "true", matchIfMissing = true)
public class XmlJsonRunner implements ApplicationRunner {

	private final Path mapaPodatkov;
	private final Path mapaIzhoda;

	public XmlJsonRunner(
			@Value("${xmljson.data-dir:data}") String mapaPodatkov,
			@Value("${xmljson.output-dir:output}") String mapaIzhoda) {
		this.mapaPodatkov = Path.of(mapaPodatkov);
		this.mapaIzhoda = Path.of(mapaIzhoda);
	}

	@Override
	public void run(ApplicationArguments args) {
		PrintStream izhod = System.out;
		PrintStream napake = System.err;
		try {
			PovezaniPodatki podatki = new DataLinker().povezi(new XmlLoader().nalozi(mapaPodatkov));
			izhod.printf("Nalozeno iz %s: %d ekip, %d dogodkov, %d uporabnikov%n",
					mapaPodatkov.toAbsolutePath().normalize(), podatki.ekipe().size(),
					podatki.dogodki().size(), podatki.uporabniki().size());
			podatki.opozorila().forEach(o -> izhod.println("Opozorilo: " + o));

			Optional<FilterKriteriji> kriteriji = FilterParser.imaFiltre(args)
					? Optional.of(FilterParser.izArgumentov(args))
					: interaktivniMeni(izhod).vprasaj(podatki);
			if (kriteriji.isEmpty()) {
				izhod.println("Ni vhoda – filtriranje preskoceno, nic ni izvozeno.");
				return;
			}
			izpisiInIzvozi(podatki, kriteriji.get(), izhod);
		} catch (IllegalArgumentException e) {
			napake.println("Napaka: " + e.getMessage());
		} catch (UncheckedIOException | JacksonException e) {
			napake.println("Napaka pri delu z datotekami: " + e.getMessage());
		}
	}

	private void izpisiInIzvozi(PovezaniPodatki podatki, FilterKriteriji kriteriji, PrintStream izhod) {
		List<PovezanDogodek> rezultat = EventFilter.filtriraj(podatki, kriteriji);

		izhod.println();
		izhod.println("Filter: " + kriteriji.opis());
		if (kriteriji.uporabnikId() != null) {
			izhod.println("Uporabnik: " + opisUporabnika(podatki, kriteriji.uporabnikId()));
		}
		ConsoleTable.izpisi(rezultat, izhod);

		for (Path datoteka : new Exporter().izvozi(rezultat, mapaIzhoda)) {
			izhod.println("Zapisano: " + datoteka.toAbsolutePath().normalize());
		}
	}

	/** Npr. {@code Ana (Arsenal, Denver Nuggets, F1)} – imena ekip poisce prek narocnin. */
	private static String opisUporabnika(PovezaniPodatki podatki, String uporabnikId) {
		Uporabnik uporabnik = podatki.uporabniki().get(uporabnikId);
		List<String> imena = new ArrayList<>();
		for (Narocnina narocnina : uporabnik.seznamNarocnin()) {
			Ekipa ekipa = podatki.ekipe().get(narocnina.teamId());
			imena.add(ekipa != null ? ekipa.ime() : narocnina.teamId());
		}
		if (uporabnik.f1()) {
			imena.add("F1");
		}
		return uporabnik.prikaznoIme() + " (" + (imena.isEmpty() ? "brez narocnin" : String.join(", ", imena)) + ")";
	}

	private static InteraktivniMeni interaktivniMeni(PrintStream izhod) {
		Charset kodiranje = Charset.forName(System.getProperty("stdin.encoding", Charset.defaultCharset().name()));
		return new InteraktivniMeni(new BufferedReader(new InputStreamReader(System.in, kodiranje)), izhod);
	}
}
