package si.um.feri.sportsync.xmljson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import si.um.feri.sportsync.xmljson.model.Dogodek;
import si.um.feri.sportsync.xmljson.model.Ekipa;
import si.um.feri.sportsync.xmljson.model.Uporabnik;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlReadFeature;

/** Prebere {@code ekipe.xml}, {@code dogodki.xml} in {@code uporabniki.xml} v Java objekte. */
public class XmlLoader {

	public record XmlPodatki(List<Ekipa> ekipe, List<Dogodek> dogodki, List<Uporabnik> uporabniki) {
	}

	private final XmlMapper mapper = XmlMapper.builder()
			.enable(XmlReadFeature.EMPTY_ELEMENT_AS_NULL)
			.build();

	public XmlPodatki nalozi(Path mapa) {
		return new XmlPodatki(
				beri(mapa.resolve("ekipe.xml"), Ekipa.class),
				beri(mapa.resolve("dogodki.xml"), Dogodek.class),
				beri(mapa.resolve("uporabniki.xml"), Uporabnik.class));
	}

	private <T> List<T> beri(Path datoteka, Class<T> tip) {
		if (!Files.isRegularFile(datoteka)) {
			throw new IllegalArgumentException("Manjka datoteka " + datoteka.toAbsolutePath().normalize());
		}
		JavaType seznam = mapper.getTypeFactory().constructCollectionType(List.class, tip);
		try {
			List<T> zapisi = mapper.readValue(datoteka.toFile(), seznam);
			return zapisi == null ? List.of() : List.copyOf(zapisi);
		} catch (JacksonException e) {
			throw new IllegalArgumentException(
					"Napaka pri branju " + datoteka.getFileName() + ": " + e.getOriginalMessage(), e);
		}
	}
}
