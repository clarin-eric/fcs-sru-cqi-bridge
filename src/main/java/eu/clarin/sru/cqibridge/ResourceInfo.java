package eu.clarin.sru.cqibridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author akislev
 */
public class ResourceInfo {

    private static final String FCS_RESOURCE_INFO_NS = "http://clarin.eu/fcs/1.0/resource-info";
    private final String corpusId;
    private final int resourceCount;
    private final boolean hasSubResources;
    private final Map<String, String> title;
    private final Map<String, String> description;
    private final List<String> languages;
    private final Map<String, String> indexes;

    public ResourceInfo(String corpusId, int resourceCount,
            boolean hasSubResources, List<String> title,
            List<String> description, List<String> languages,
            List<String> indexes) {
        this.corpusId = corpusId;
        this.resourceCount = (resourceCount > 0) ? resourceCount : -1;
        this.hasSubResources = hasSubResources;
        this.title = listToMap(title, true);
        this.description = listToMap(description, false);
        this.languages = languages;
        this.indexes = listToMap(indexes, false);
    }

    public String getCorpusId() {
        return corpusId;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void writeResourceInfo(XMLStreamWriter writer, String prefix)
            throws XMLStreamException {
        final boolean defaultNS = (prefix == null || prefix.isEmpty());
        if (defaultNS) {
            writer.setDefaultNamespace(FCS_RESOURCE_INFO_NS);
        } else {
            writer.setPrefix(prefix, FCS_RESOURCE_INFO_NS);
        }
        writer.writeStartElement(FCS_RESOURCE_INFO_NS, "resource-info");
        if (defaultNS) {
            writer.writeDefaultNamespace(FCS_RESOURCE_INFO_NS);
        } else {
            writer.writeNamespace(prefix, FCS_RESOURCE_INFO_NS);
        }
        if (hasSubResources) {
            writer.writeAttribute("has-sub-resources", "true");
        }
        for (Map.Entry<String, String> i : title.entrySet()) {
            writer.writeStartElement(FCS_RESOURCE_INFO_NS, "title");
            writer.writeAttribute(XMLConstants.XML_NS_URI,
                    "lang", i.getKey());
            writer.writeCharacters(i.getValue());
            writer.writeEndElement(); // "title" element
        }

        if (description != null) {
            for (Map.Entry<String, String> i : description.entrySet()) {
                writer.writeStartElement(FCS_RESOURCE_INFO_NS,
                        "description");
                writer.writeAttribute(XMLConstants.XML_NS_URI, "lang",
                        i.getKey());
                writer.writeCharacters(i.getValue());
                writer.writeEndElement(); // "description" element
            }
        }

        writer.writeStartElement(FCS_RESOURCE_INFO_NS, "languages");
        for (String i : languages) {
            writer.writeStartElement(FCS_RESOURCE_INFO_NS, "language");
            writer.writeCharacters(i);
            writer.writeEndElement(); // "language" element

        }
        writer.writeEndElement(); // "languages" element

        if (indexes != null) {
            writer.writeStartElement(FCS_RESOURCE_INFO_NS, "indexes");
            for (Map.Entry<String, String> i : indexes.entrySet()) {
                writer.writeStartElement(FCS_RESOURCE_INFO_NS,
                        "index");
                writer.writeAttribute("category", i.getKey());
                writer.writeCharacters(i.getValue());
                writer.writeEndElement(); // "index" element
            }
            writer.writeEndElement(); // "indexes" element
        }
        writer.writeEndElement(); // "resource-info" element
    }

    private static Map<String, String> listToMap(List<String> list,
            boolean mandatory) {
        if (list != null) {
            if (list.isEmpty() || ((list.size() % 2) != 0)) {
                throw new IllegalArgumentException(
                        "list must contain an even number of elements and must not be empty");
            }
            Map<String, String> result = new HashMap<String, String>();
            for (int i = 0; i < list.size(); i += 2) {
                String key = list.get(i);
                String val = list.get(i + 1);
                if ((key == null) || (val == null)) {
                    throw new NullPointerException(
                            "key == null || val == null");
                }
                result.put(list.get(i), list.get(i + 1));
            }
            return result;
        } else {
            if (mandatory) {
                throw new NullPointerException("list == null");
            }
            return null;
        }
    }
}
