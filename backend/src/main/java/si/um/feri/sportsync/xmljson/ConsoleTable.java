package si.um.feri.sportsync.xmljson;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

import si.um.feri.sportsync.xmljson.povezava.PovezanDogodek;

/** Izpise dogodke v konzolo kot tabelo s poravnanimi stolpci. */
public final class ConsoleTable {

	private static final String[] GLAVA = { "Datum", "Ura", "Sport", "Tekmovanje", "Dogodek", "Rezultat", "Status" };
	private static final String PRAZNO = "-";

	private ConsoleTable() {
	}

	public static void izpisi(List<PovezanDogodek> dogodki, PrintStream izhod) {
		if (dogodki.isEmpty()) {
			izhod.println("Ni dogodkov, ki ustrezajo filtru.");
			return;
		}
		List<String[]> vrstice = dogodki.stream().map(ConsoleTable::vrstica).toList();
		int[] sirine = sirine(vrstice);
		String locilo = locilo(sirine);

		izhod.println(locilo);
		izhod.println(oblikuj(GLAVA, sirine));
		izhod.println(locilo);
		vrstice.forEach(v -> izhod.println(oblikuj(v, sirine)));
		izhod.println(locilo);
		izhod.println("Skupaj dogodkov: " + dogodki.size());
	}

	private static String[] vrstica(PovezanDogodek d) {
		return new String[] {
				Objects.toString(d.datum(), PRAZNO),
				Objects.toString(d.ura(), PRAZNO),
				Objects.toString(d.sport(), PRAZNO),
				Objects.toString(d.tekmovanje(), PRAZNO),
				Objects.toString(d.naslov(), PRAZNO),
				d.prikazRezultata(),
				Objects.toString(d.status(), PRAZNO) };
	}

	private static int[] sirine(List<String[]> vrstice) {
		int[] sirine = new int[GLAVA.length];
		for (int i = 0; i < GLAVA.length; i++) {
			sirine[i] = GLAVA[i].length();
			for (String[] vrstica : vrstice) {
				sirine[i] = Math.max(sirine[i], vrstica[i].length());
			}
		}
		return sirine;
	}

	private static String oblikuj(String[] celice, int[] sirine) {
		StringBuilder sb = new StringBuilder("|");
		for (int i = 0; i < celice.length; i++) {
			sb.append(' ').append(String.format("%-" + sirine[i] + "s", celice[i])).append(" |");
		}
		return sb.toString();
	}

	private static String locilo(int[] sirine) {
		StringBuilder sb = new StringBuilder("+");
		for (int sirina : sirine) {
			sb.append("-".repeat(sirina + 2)).append('+');
		}
		return sb.toString();
	}
}
