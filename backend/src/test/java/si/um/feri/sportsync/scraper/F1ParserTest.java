package si.um.feri.sportsync.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import si.um.feri.sportsync.scraper.F1Parser.KrogDirke;
import si.um.feri.sportsync.scraper.model.Mostvo;
import si.um.feri.sportsync.scraper.model.Proga;
import si.um.feri.sportsync.scraper.model.Voznik;

/** HTML je skrajsan posnetek strukture formula1.com (oktober 2026). */
class F1ParserTest {

	private static final String OSNOVA = "https://www.formula1.com";

	private static Document html(String telo) {
		return Jsoup.parse("<html><body>" + telo + "</body></html>", OSNOVA);
	}

	@Test
	void povezave_vrneUnikatneIdjeVVrstnemRedu() {
		Document doc = html("""
				<a href="/en/drivers/max-verstappen">Max</a>
				<a href="/en/drivers/lando-norris?x=1">Lando</a>
				<a href="/en/drivers/max-verstappen">Max znova</a>
				<a href="/en/teams/mclaren">McLaren</a>
				""");

		assertThat(F1Parser.povezave(doc, "/en/drivers/")).containsExactly("max-verstappen", "lando-norris");
	}

	@Test
	void koledar_izpustiTestiranjaInUrediPoKrogih() {
		Document doc = html("""
				<a href="/en/racing/2026/pre-season-testing-1"><span>TESTING</span><p>Bahrain</p></a>
				<a href="/en/racing/2026/italy"><span><span>ROUND 13</span><span>04 - 06 Sep</span></span><p>Italy</p></a>
				<a href="/en/racing/2026/mexico"><span><span>ROUND 20</span><span>30 OCT - 01 NOV</span></span><p>Mexico</p></a>
				<a href="/en/racing/2026/australia"><span><span>ROUND 1</span><span>06 - 08 Mar</span></span><p>Australia</p></a>
				""");

		List<KrogDirke> koledar = F1Parser.koledar(doc, 2026);

		assertThat(koledar).extracting(KrogDirke::id).containsExactly("australia", "italy", "mexico");
		assertThat(koledar.get(1)).isEqualTo(new KrogDirke("italy", 13, "04 - 06 Sep", "Italy"));
		assertThat(koledar.get(2).termin()).isEqualTo("30 OCT - 01 NOV");
	}

	@Test
	void voznik_preberePodatkeInRazdeliStatistiko() {
		Document doc = html("""
				<h1><span><span>Max</span></span><span>Verstappen</span></h1>
				<div><div><p>Netherlands</p></div><p>Red Bull Racing</p><p>3</p></div>
				<img src="https://media.formula1.com/image/upload/common/f1/2026/redbullracing/maxver01/2026redbullracingmaxver01right.webp">
				<dl class="DataGrid-module_dataGrid__x DataGrid-module_two-columns__y">
				  <div><dt>Season Position</dt><dd>4th</dd></div>
				  <div><dt>Season Points</dt><dd>236</dd></div>
				</dl>
				<dl class="DataGrid-module_dataGrid__x DataGrid-module_rows__z">
				  <div><dt>Career Points</dt><dd>3680.5</dd></div>
				  <div><dt>Highest Race Finish</dt><dd>1 (x73)</dd></div>
				  <div><dt>World Championships</dt><dd>4</dd></div>
				</dl>
				<dl class="DataGrid-module_dataGrid__x DataGrid-module_two-columns__y">
				  <div><dt>Date of Birth</dt><dd>30/09/1997</dd></div>
				  <div><dt>Place of Birth</dt><dd>Hasselt, Belgium</dd></div>
				</dl>
				""");

		Voznik voznik = F1Parser.voznik(doc, "max-verstappen", OSNOVA + "/en/drivers/max-verstappen");

		assertThat(voznik.ime()).isEqualTo("Max");
		assertThat(voznik.priimek()).isEqualTo("Verstappen");
		assertThat(voznik.drzava()).isEqualTo("Netherlands");
		assertThat(voznik.mostvo()).isEqualTo("Red Bull Racing");
		assertThat(voznik.stevilka()).isEqualTo(3);
		assertThat(voznik.datumRojstva()).isEqualTo(LocalDate.of(1997, 9, 30));
		assertThat(voznik.krajRojstva()).isEqualTo("Hasselt, Belgium");
		assertThat(voznik.slikaUrl()).endsWith("maxver01right.webp");
		assertThat(voznik.statistikaSezone()).containsEntry("Season Position", "4th").containsEntry("Season Points", 236L);
		assertThat(voznik.statistikaKariere())
				.containsEntry("Career Points", 3680.5)
				.containsEntry("Highest Race Finish", "1 (x73)")
				.containsEntry("World Championships", 4L)
				.doesNotContainKey("Date of Birth");
	}

	@Test
	void mostvo_preberProfilVoznikeInSlike() {
		Document doc = html("""
				<h1>McLaren</h1>
				<a href="/en/drivers/lando-norris">Lando Norris</a>
				<a href="/en/drivers/oscar-piastri">Oscar Piastri</a>
				<img src="https://media.formula1.com/image/upload/common/f1/logo/pirelli/pirelli.webp">
				<img src="https://media.formula1.com/image/upload/common/f1/2025/mclaren/2025mclarenlogowhite.webp">
				<img src="https://media.formula1.com/image/upload/common/f1/2026/mclaren/2026mclarencarright.webp">
				<dl class="DataGrid-module_two-columns__y"><div><dt>Season Points</dt><dd>355</dd></div></dl>
				<dl class="DataGrid-module_rows__z"><div><dt>World Championships</dt><dd>10</dd></div></dl>
				<dl class="DataGrid-module_two-columns__y">
				  <div><dt>Full Team Name</dt><dd>McLaren Mastercard F1 Team</dd></div>
				  <div><dt>Base</dt><dd>Woking, United Kingdom</dd></div>
				  <div><dt>Team Chief</dt><dd>Andrea Stella</dd></div>
				  <div><dt>Chassis</dt><dd>MCL40</dd></div>
				  <div><dt>Power Unit</dt><dd>Mercedes</dd></div>
				  <div><dt>First Team Entry</dt><dd>1966</dd></div>
				</dl>
				""");

		Mostvo mostvo = F1Parser.mostvo(doc, "mclaren", OSNOVA + "/en/teams/mclaren");

		assertThat(mostvo.ime()).isEqualTo("McLaren");
		assertThat(mostvo.polnoIme()).isEqualTo("McLaren Mastercard F1 Team");
		assertThat(mostvo.sedez()).isEqualTo("Woking, United Kingdom");
		assertThat(mostvo.sefEkipe()).isEqualTo("Andrea Stella");
		assertThat(mostvo.sasija()).isEqualTo("MCL40");
		assertThat(mostvo.pogonskaEnota()).isEqualTo("Mercedes");
		assertThat(mostvo.prviNastop()).isEqualTo(1966);
		assertThat(mostvo.vozniki()).containsExactly("lando-norris", "oscar-piastri");
		assertThat(mostvo.logoUrl()).endsWith("2025mclarenlogowhite.webp");
		assertThat(mostvo.dirkalnikUrl()).endsWith("2026mclarencarright.webp");
		assertThat(mostvo.statistikaSezone()).containsEntry("Season Points", 355L);
		assertThat(mostvo.statistikaKariere()).containsEntry("World Championships", 10L);
	}

	@Test
	void proga_preberePodatkeRekordInJsonLd() {
		Document doc = html("""
				<script type="application/ld+json">
				{"@type":"SportsEvent","name":"FORMULA 1 PIRELLI GRAN PREMIO D'ITALIA 2026",
				 "location":{"@type":"City","name":"Monza","address":"Monza, Italy"}}
				</script>
				<img src="https://media.formula1.com/image/upload/common/f1/2026/track/2026trackmonzadetailed.webp">
				<dl>
				  <div><dt>Circuit Length</dt><dd>5.793km</dd><span></span></div>
				  <div><dt>First Grand Prix</dt><dd>1950</dd><span></span></div>
				  <div><dt>Number of Laps</dt><dd>53</dd><span></span></div>
				  <div><dt>Fastest lap time</dt><dd>1:20.901</dd><span>Lando Norris (2025)</span></div>
				  <div><dt>Race Distance</dt><dd>306.72km</dd><span></span></div>
				</dl>
				""");
		KrogDirke dirka = new KrogDirke("italy", 13, "04 - 06 Sep", "Italy");

		Proga proga = F1Parser.proga(doc, dirka, OSNOVA + "/en/racing/2026/italy");

		assertThat(proga.krog()).isEqualTo(13);
		assertThat(proga.uradnoIme()).isEqualTo("FORMULA 1 PIRELLI GRAN PREMIO D'ITALIA 2026");
		assertThat(proga.kraj()).isEqualTo("Monza");
		assertThat(proga.drzava()).isEqualTo("Italy");
		assertThat(proga.dolzinaKm()).isEqualTo(5.793);
		assertThat(proga.steviloKrogov()).isEqualTo(53);
		assertThat(proga.dolzinaDirkeKm()).isEqualTo(306.72);
		assertThat(proga.prvaDirka()).isEqualTo(1950);
		assertThat(proga.rekordKroga()).isEqualTo("1:20.901");
		assertThat(proga.rekordVoznik()).isEqualTo("Lando Norris");
		assertThat(proga.rekordLeto()).isEqualTo(2025);
		assertThat(proga.zemljevidUrl()).endsWith("2026trackmonzadetailed.webp");
	}

	@Test
	void proga_brezPodatkovVrneNullNamestoNapake() {
		Proga proga = F1Parser.proga(html("<p>prazno</p>"), new KrogDirke("x", 1, null, null), OSNOVA);

		assertThat(proga.dolzinaKm()).isNull();
		assertThat(proga.kraj()).isNull();
		assertThat(proga.rekordVoznik()).isNull();
	}
}
