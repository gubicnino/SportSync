package si.um.feri.sportsync.xmljson.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import si.um.feri.sportsync.xmljson.model.Sport;
import si.um.feri.sportsync.xmljson.model.Uporabnik;
import si.um.feri.sportsync.xmljson.povezava.PovezanDogodek;
import si.um.feri.sportsync.xmljson.povezava.PovezaniPodatki;

/** Filtrira povezane dogodke po {@link FilterKriteriji}; rezultat je urejen po datumu in uri. */
public final class EventFilter {

	private static final Comparator<PovezanDogodek> PO_CASU = Comparator
			.comparing(PovezanDogodek::datum, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(PovezanDogodek::ura, Comparator.nullsLast(Comparator.naturalOrder()));

	private EventFilter() {
	}

	public static List<PovezanDogodek> filtriraj(PovezaniPodatki podatki, FilterKriteriji kriteriji) {
		Predicate<PovezanDogodek> pogoj = sestaviPogoj(podatki, kriteriji);
		return podatki.dogodki().stream()
				.filter(pogoj)
				.sorted(PO_CASU)
				.toList();
	}

	private static Predicate<PovezanDogodek> sestaviPogoj(PovezaniPodatki podatki, FilterKriteriji k) {
		List<Predicate<PovezanDogodek>> pogoji = new ArrayList<>();
		if (k.sport() != null) {
			pogoji.add(d -> d.sport() == k.sport());
		}
		if (k.ekipaId() != null) {
			if (!podatki.ekipe().containsKey(k.ekipaId())) {
				throw new IllegalArgumentException("Ekipa z ID '" + k.ekipaId() + "' ne obstaja.");
			}
			pogoji.add(d -> d.nastopaEkipa(k.ekipaId()));
		}
		if (k.tekmovanje() != null) {
			pogoji.add(d -> k.tekmovanje().equalsIgnoreCase(d.tekmovanje()));
		}
		if (k.status() != null) {
			pogoji.add(d -> d.status() == k.status());
		}
		if (k.odDatuma() != null) {
			pogoji.add(d -> d.datum() != null && !d.datum().isBefore(k.odDatuma()));
		}
		if (k.doDatuma() != null) {
			pogoji.add(d -> d.datum() != null && !d.datum().isAfter(k.doDatuma()));
		}
		if (k.uporabnikId() != null) {
			Uporabnik uporabnik = podatki.uporabniki().get(k.uporabnikId());
			if (uporabnik == null) {
				throw new IllegalArgumentException("Uporabnik z ID '" + k.uporabnikId() + "' ne obstaja.");
			}
			pogoji.add(d -> spremlja(uporabnik, d));
		}
		return pogoji.stream().reduce(d -> true, Predicate::and);
	}

	/** F1 dogodke spremlja, ce ima {@code f1=true}, ostale, ce je narocen na eno od ekip. */
	private static boolean spremlja(Uporabnik uporabnik, PovezanDogodek dogodek) {
		if (dogodek.sport() == Sport.F1) {
			return uporabnik.f1();
		}
		return uporabnik.seznamNarocnin().stream().anyMatch(n -> dogodek.nastopaEkipa(n.teamId()));
	}
}
