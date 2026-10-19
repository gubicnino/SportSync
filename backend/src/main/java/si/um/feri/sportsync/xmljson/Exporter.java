package si.um.feri.sportsync.xmljson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import si.um.feri.sportsync.xmljson.povezava.FiltriraniDogodki;
import si.um.feri.sportsync.xmljson.povezava.PovezanDogodek;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;

/** Zapise filtrirane dogodke v {@code filtrirano.json} in {@code filtrirano.xml}. */
public class Exporter {

	private final JsonMapper jsonMapper = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.build();

	private final XmlMapper xmlMapper = XmlMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.enable(XmlWriteFeature.WRITE_XML_DECLARATION)
			.build();

	/** Vrne poti zapisanih datotek. */
	public List<Path> izvozi(List<PovezanDogodek> dogodki, Path mapa) {
		try {
			Files.createDirectories(mapa);
		} catch (IOException e) {
			throw new UncheckedIOException("Mape " + mapa.toAbsolutePath() + " ni mogoce ustvariti.", e);
		}
		Path json = mapa.resolve("filtrirano.json");
		Path xml = mapa.resolve("filtrirano.xml");
		jsonMapper.writeValue(json.toFile(), dogodki);
		xmlMapper.writeValue(xml.toFile(), new FiltriraniDogodki(dogodki));
		return List.of(json, xml);
	}
}
