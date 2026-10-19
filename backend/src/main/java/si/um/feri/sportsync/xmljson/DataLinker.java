package si.um.feri.sportsync.xmljson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import si.um.feri.sportsync.xmljson.XmlLoader.XmlPodatki;
import si.um.feri.sportsync.xmljson.model.Dogodek;
import si.um.feri.sportsync.xmljson.model.Ekipa;
import si.um.feri.sportsync.xmljson.model.Narocnina;
import si.um.feri.sportsync.xmljson.model.Uporabnik;
import si.um.feri.sportsync.xmljson.povezava.PovezanDogodek;
import si.um.feri.sportsync.xmljson.povezava.PovezaniPodatki;

/**
 * Poveze prebrane zapise prek ID-jev: dogodek dobi celi domaco in gostujoco ekipo, narocnine
 * uporabnikov se preverijo proti ekipam. Neznani ali podvojeni ID-ji ne ustavijo programa, ampak
 * se zabelezijo kot opozorila.
 */
public class DataLinker {

	public PovezaniPodatki povezi(XmlPodatki xml) {
		List<String> opozorila = new ArrayList<>();

		Map<String, Ekipa> ekipe = new LinkedHashMap<>();
		for (Ekipa ekipa : xml.ekipe()) {
			if (ekipe.putIfAbsent(ekipa.id(), ekipa) != null) {
				opozorila.add("Podvojen ID ekipe '" + ekipa.id() + "' – upostevana je prva.");
			}
		}

		List<PovezanDogodek> dogodki = new ArrayList<>();
		for (Dogodek dogodek : xml.dogodki()) {
			dogodki.add(poveziDogodek(dogodek, ekipe, opozorila));
		}

		Map<String, Uporabnik> uporabniki = new LinkedHashMap<>();
		for (Uporabnik uporabnik : xml.uporabniki()) {
			for (Narocnina narocnina : uporabnik.seznamNarocnin()) {
				poisciEkipo(narocnina.teamId(), ekipe, "Uporabnik " + uporabnik.id(), opozorila);
			}
			if (uporabniki.putIfAbsent(uporabnik.id(), uporabnik) != null) {
				opozorila.add("Podvojen ID uporabnika '" + uporabnik.id() + "' – upostevan je prvi.");
			}
		}

		return new PovezaniPodatki(dogodki, ekipe, uporabniki, opozorila);
	}

	private PovezanDogodek poveziDogodek(Dogodek d, Map<String, Ekipa> ekipe, List<String> opozorila) {
		String kontekst = "Dogodek " + d.id();
		Ekipa domaci = poisciEkipo(d.domaciId(), ekipe, kontekst, opozorila);
		Ekipa gostje = poisciEkipo(d.gostjeId(), ekipe, kontekst, opozorila);
		String naslov = d.naslov() != null
				? d.naslov()
				: imeAliId(domaci, d.domaciId()) + " vs " + imeAliId(gostje, d.gostjeId());
		return new PovezanDogodek(d.id(), d.sport(), d.tip(), d.tekmovanje(), naslov, d.datum(), d.ura(),
				d.prizorisce(), d.status(), domaci, gostje, d.rezultatDomaci(), d.rezultatGostje(), d.zmagovalec());
	}

	private static Ekipa poisciEkipo(String id, Map<String, Ekipa> ekipe, String kontekst, List<String> opozorila) {
		if (id == null) {
			return null;
		}
		Ekipa ekipa = ekipe.get(id);
		if (ekipa == null) {
			opozorila.add(kontekst + ": ekipa z ID '" + id + "' ne obstaja.");
		}
		return ekipa;
	}

	private static String imeAliId(Ekipa ekipa, String id) {
		return ekipa != null ? ekipa.ime() : String.valueOf(id);
	}
}
