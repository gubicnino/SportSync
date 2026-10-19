package si.um.feri.sportsync.xmljson.povezava;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import si.um.feri.sportsync.xmljson.model.Ekipa;
import si.um.feri.sportsync.xmljson.model.Uporabnik;

/** Rezultat povezovanja vseh treh XML datotek. Mape ohranijo vrstni red iz XML. */
public record PovezaniPodatki(
		List<PovezanDogodek> dogodki,
		Map<String, Ekipa> ekipe,
		Map<String, Uporabnik> uporabniki,
		List<String> opozorila) {

	public PovezaniPodatki {
		dogodki = List.copyOf(dogodki);
		ekipe = Collections.unmodifiableMap(new LinkedHashMap<>(ekipe));
		uporabniki = Collections.unmodifiableMap(new LinkedHashMap<>(uporabniki));
		opozorila = List.copyOf(opozorila);
	}
}
