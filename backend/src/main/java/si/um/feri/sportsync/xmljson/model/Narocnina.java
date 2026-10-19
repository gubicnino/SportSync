package si.um.feri.sportsync.xmljson.model;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Narocnina uporabnika na ekipo; {@code teamId} se sklicuje na {@link Ekipa#id()}. */
public record Narocnina(@JacksonXmlProperty(isAttribute = true) String teamId) {
}
