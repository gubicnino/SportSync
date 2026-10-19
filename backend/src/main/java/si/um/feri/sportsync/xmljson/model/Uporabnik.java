package si.um.feri.sportsync.xmljson.model;

import java.time.LocalDate;
import java.util.List;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Uporabnik iz {@code data/uporabniki.xml}; {@code telegramChatId} je lahko prazen. */
public record Uporabnik(
		@JacksonXmlProperty(isAttribute = true) String id,
		String email,
		String prikaznoIme,
		LocalDate registriran,
		Long telegramChatId,
		boolean tedenskiPregled,
		boolean f1,
		Narocnine narocnine) {

	/**
	 * Element {@code <narocnine>} s ponovljenimi {@code <narocnina>}. Locen record, ker Jackson pri
	 * recordih ne zna preimenovati ovitega seznama ({@code @JacksonXmlElementWrapper} + drugo ime).
	 */
	public record Narocnine(@JacksonXmlElementWrapper(useWrapping = false) List<Narocnina> narocnina) {

		public Narocnine {
			narocnina = narocnina == null ? List.of() : List.copyOf(narocnina);
		}
	}

	/** Narocnine kot seznam; prazen seznam, ce element manjka ali je prazen. */
	public List<Narocnina> seznamNarocnin() {
		return narocnine == null ? List.of() : narocnine.narocnina();
	}
}
