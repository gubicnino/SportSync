package si.um.feri.sportsync.scraper;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import si.um.feri.sportsync.scraper.model.Mostvo;
import si.um.feri.sportsync.scraper.model.Proga;
import si.um.feri.sportsync.scraper.model.Voznik;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Naloga 2 (Parser): s formula1.com prebere voznike, mostva in proge sezone ter jih zapise v
 * {@code drivers.json}, {@code teams.json} in {@code circuits.json}. Vklopi se s profilom
 * {@code scraper} (glej {@code application-scraper.properties}).
 */
@Component
@ConditionalOnProperty(name = "scraper.enabled", havingValue = "true")
public class ScraperRunner implements ApplicationRunner {

	private final Path mapaIzhoda;
	private final int sezona;
	private final long zamikMs;

	private final JsonMapper jsonMapper = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.build();

	public ScraperRunner(
			@Value("${scraper.output-dir:output/f1}") String mapaIzhoda,
			@Value("${scraper.season:0}") int sezona,
			@Value("${scraper.delay-ms:1000}") long zamikMs) {
		this.mapaIzhoda = Path.of(mapaIzhoda);
		this.sezona = sezona > 0 ? sezona : Year.now().getValue();
		this.zamikMs = zamikMs;
	}

	@Override
	public void run(ApplicationArguments args) {
		PrintStream izhod = System.out;
		try {
			F1Scraper scraper = new F1Scraper(zamikMs, izhod);
			Files.createDirectories(mapaIzhoda);

			izhod.println("Vozniki (" + F1Scraper.OSNOVA + "/en/drivers):");
			List<Voznik> vozniki = scraper.vozniki();
			zapisi("drivers.json", vozniki, izhod);

			izhod.println("Mostva (" + F1Scraper.OSNOVA + "/en/teams):");
			List<Mostvo> mostva = scraper.mostva();
			zapisi("teams.json", mostva, izhod);

			izhod.println("Proge sezone " + sezona + " (" + F1Scraper.OSNOVA + "/en/racing/" + sezona + "):");
			List<Proga> proge = scraper.proge(sezona);
			zapisi("circuits.json", proge, izhod);

			izhod.printf("%nSkupaj: %d voznikov, %d mostev, %d prog%n", vozniki.size(), mostva.size(), proge.size());
		} catch (IOException e) {
			System.err.println("Napaka pri delu z datotekami: " + e.getMessage());
		} catch (UncheckedIOException e) {
			// seznam (npr. /en/drivers) ni dosegljiv – brez njega ni kaj prebrati
			System.err.println("Napaka: " + e.getMessage());
		} catch (JacksonException e) {
			System.err.println("Napaka pri zapisu JSON: " + e.getMessage());
		}
	}

	private void zapisi(String ime, List<?> podatki, PrintStream izhod) {
		Path datoteka = mapaIzhoda.resolve(ime);
		jsonMapper.writeValue(datoteka.toFile(), podatki);
		izhod.printf("Zapisano: %s (%d zapisov)%n", datoteka.toAbsolutePath().normalize(), podatki.size());
	}
}
