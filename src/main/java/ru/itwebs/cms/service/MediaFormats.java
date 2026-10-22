package ru.itwebs.cms.service;

import ru.itwebs.cms.entity.MediaAsset;
import org.w3c.dom.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Validates content before it reaches object storage. SVG uses a deliberately small static subset. */
public final class MediaFormats {
    private MediaFormats() { }
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Map<String, String> SUFFIXES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/gif", ".gif",
            "image/avif", ".avif", "image/svg+xml", ".svg", "application/pdf", ".pdf", XLSX, ".xlsx");
    private static final Set<String> SVG_ELEMENTS = Set.of("svg", "g", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
            "defs", "linearGradient", "radialGradient", "stop", "clipPath", "mask", "title", "desc", "text", "tspan", "use", "symbol", "image", "filter", "feColorMatrix", "feBlend", "feFlood", "feOffset", "feGaussianBlur");
    private static final Set<String> SVG_ATTRIBUTES = Set.of("id", "viewBox", "width", "height", "x", "y", "x1", "x2", "y1", "y2",
            "cx", "cy", "r", "rx", "ry", "d", "points", "transform", "fill", "fill-opacity", "fill-rule", "opacity", "stroke",
            "stroke-width", "stroke-opacity", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset",
            "clip-path", "clip-rule", "mask", "offset", "stop-color", "stop-opacity", "gradientUnits", "gradientTransform", "spreadMethod",
            "fx", "fy", "preserveAspectRatio", "font-family", "font-size", "font-weight", "text-anchor", "dominant-baseline", "dx", "dy", "href", "maskUnits", "maskContentUnits", "overflow", "filter", "filterUnits", "color-interpolation-filters",
            "flood-opacity", "flood-color", "stdDeviation", "result", "in", "in2", "type", "values", "mode", "style");

    public static String suffix(String type) {
        String suffix = SUFFIXES.get(type == null ? "" : type);
        if (suffix == null) throw new IllegalArgumentException("Разрешены JPEG, PNG, WebP, GIF, AVIF, безопасный SVG, PDF и XLSX");
        return suffix;
    }
    public static MediaAsset requireImage(MediaAsset asset) {
        if (asset.getContentType() == null || !asset.getContentType().startsWith("image/"))
            throw new IllegalArgumentException("В поле изображения нельзя выбрать документ");
        return asset;
    }
    public static void validate(String type, byte[] data) {
        suffix(type);
        if (data.length == 0 || data.length > 15 * 1024 * 1024) throw new IllegalArgumentException("Файл должен быть непустым и не больше 15 МБ");
        boolean valid = switch (type) {
            case "image/jpeg" -> starts(data, 0, new byte[]{(byte)0xff, (byte)0xd8, (byte)0xff})
                    && data.length > 4 && data[data.length - 2] == (byte)0xff && data[data.length - 1] == (byte)0xd9;
            case "image/png" -> starts(data, 0, new byte[]{(byte)137,80,78,71,13,10,26,10}) && starts(data, 12, "IHDR");
            case "image/gif" -> starts(data, 0, "GIF87a") || starts(data, 0, "GIF89a");
            case "image/webp" -> webp(data);
            case "image/avif" -> avif(data);
            case "image/svg+xml" -> svg(data);
            case "application/pdf" -> starts(data, 0, "%PDF-") && new String(data, Math.max(0, data.length - 1024), Math.min(1024, data.length), StandardCharsets.ISO_8859_1).contains("%%EOF");
            case XLSX -> xlsx(data);
            default -> false;
        };
        if (valid && Set.of("image/png", "image/jpeg", "image/gif").contains(type)) valid = decodableRaster(data);
        if (!valid) throw new IllegalArgumentException("Содержимое файла не соответствует заявленному формату или содержит неподдерживаемые элементы");
    }
    private static boolean starts(byte[] bytes, int offset, String expected) { return starts(bytes, offset, expected.getBytes(StandardCharsets.US_ASCII)); }
    private static boolean starts(byte[] bytes, int offset, byte[] expected) {
        return offset >= 0 && bytes.length >= offset + expected.length && Arrays.equals(bytes, offset, offset + expected.length, expected, 0, expected.length);
    }
    private static boolean decodableRaster(byte[] data) {
        try (var input = javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 40_000_000) return false;
                return reader.read(0) != null;
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException e) { return false; }
    }
    private static boolean webp(byte[] data) {
        if (!starts(data, 0, "RIFF") || !starts(data, 8, "WEBP") || data.length < 20) return false;
        var buffer = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        if (Integer.toUnsignedLong(buffer.getInt(4)) != data.length - 8L) return false;
        boolean image = false;
        int position = 12;
        while (position + 8L <= data.length) {
            long size = Integer.toUnsignedLong(buffer.getInt(position + 4));
            long next = position + 8L + size + (size & 1);
            if (next > data.length) return false;
            if (starts(data, position, "VP8 ")) {
                if (size < 10 || !starts(data, position + 11, new byte[]{(byte)0x9d, 1, 0x2a})) return false;
                image = true;
            } else if (starts(data, position, "VP8L")) {
                if (size < 5 || data[position + 8] != 0x2f) return false;
                image = true;
            } else if (starts(data, position, "VP8X")) {
                if (size != 10) return false;
            } else if (starts(data, position, "ANMF")) {
                if (size < 24) return false;
                image = true;
            }
            position = (int) next;
        }
        return image && position == data.length;
    }
    private static boolean avif(byte[] data) {
        if (!starts(data, 4, "ftyp") || data.length < 16) return false;
        var buffer = java.nio.ByteBuffer.wrap(data);
        long firstSize = Integer.toUnsignedLong(buffer.getInt());
        if (firstSize < 16 || firstSize > data.length || firstSize > 4096) return false;
        boolean brand = starts(data, 8, "avif") || starts(data, 8, "avis");
        for (int i = 16; i + 4 <= firstSize; i += 4) brand |= starts(data, i, "avif") || starts(data, i, "avis");
        if (!brand) return false;
        boolean metadata = false, pixels = false;
        int position = 0;
        while (position + 8L <= data.length) {
            long size = Integer.toUnsignedLong(buffer.getInt(position));
            int header = 8;
            if (size == 1) {
                if (position + 16L > data.length) return false;
                size = buffer.getLong(position + 8); header = 16;
            } else if (size == 0) size = data.length - (long) position;
            if (size < header || size > data.length - (long) position) return false;
            if (starts(data, position + 4, "meta") || starts(data, position + 4, "moov")) metadata |= size > header;
            if (starts(data, position + 4, "mdat")) pixels |= size > header;
            position += (int) size;
        }
        return metadata && pixels && position == data.length;
    }
    private static Document xml(byte[] data) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
        });
        return builder.parse(new ByteArrayInputStream(data));
    }
    private static boolean svg(byte[] data) {
        try {
            var doc = xml(data);
            if (!"svg".equals(doc.getDocumentElement().getLocalName())) return false;
            return staticSvg(doc, 0);
        } catch (Exception e) { return false; }
    }
    private static boolean staticSvg(Node node, int depth) {
        if (depth > 100 || node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) return false;
        if (node instanceof Element element) {
            if (!"http://www.w3.org/2000/svg".equals(element.getNamespaceURI()) || !SVG_ELEMENTS.contains(element.getLocalName())) return false;
            var attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) continue;
                if (attr.getNamespaceURI() != null && !("http://www.w3.org/1999/xlink".equals(attr.getNamespaceURI()) && "href".equals(attr.getLocalName()))) return false;
                String name = attr.getLocalName(), value = attr.getNodeValue().trim();
                if (!SVG_ATTRIBUTES.contains(name)) return false;
                if (name.equals("style") && !value.matches("mask-type\\s*:\\s*(alpha|luminance)\\s*;?")) return false;
                if (name.equals("href") && !value.matches("#[A-Za-z_][A-Za-z0-9_.:-]*")) {
                    if (!element.getLocalName().equals("image") || !embeddedImage(value)) return false;
                }
                if (Set.of("fill", "stroke", "clip-path", "mask", "filter").contains(name)
                        && !value.matches("(?:none|currentColor|transparent|[A-Za-z]+|#[A-Fa-f0-9]{3,8}|(?:rgb|rgba|hsl|hsla)\\([0-9.,% +\\-]+\\)|url\\(#[A-Za-z_][A-Za-z0-9_.:-]*\\))")) return false;
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) if (!staticSvg(child, depth + 1)) return false;
        return true;
    }
    private static boolean embeddedImage(String value) {
        for (String type : List.of("image/png", "image/jpeg")) {
            String prefix = "data:" + type + ";base64,";
            if (!value.startsWith(prefix)) continue;
            try { validate(type, Base64.getDecoder().decode(value.substring(prefix.length()).replaceAll("\\s", ""))); return true; }
            catch (IllegalArgumentException e) { return false; }
        }
        return false;
    }
    private static boolean xlsx(byte[] data) {
        // Bound inflated bytes and entries; do not extract anything to disk.
        try (var zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            Set<String> names = new HashSet<>();
            long total = 0;
            boolean workbook = false, types = false;
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                String name = entry.getName();
                if (names.size() >= 2000 || !names.add(name) || name.startsWith("/") || name.contains("..") || name.contains("\\")) return false;
                var bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    total += count;
                    if (total > 50 * 1024 * 1024 || bytes.size() + count > 20 * 1024 * 1024) return false;
                    bytes.write(buffer, 0, count);
                }
                if (entry.isDirectory()) {
                    if (bytes.size() != 0) return false;
                    continue;
                }
                if (name.startsWith("xl/media/") && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
                    validate(name.endsWith(".png") ? "image/png" : "image/jpeg", bytes.toByteArray());
                    continue;
                }
                if (!name.endsWith(".xml") && !name.endsWith(".rels")) return false;
                var doc = xml(bytes.toByteArray());
                if (name.equals("xl/workbook.xml")) workbook = "workbook".equals(doc.getDocumentElement().getLocalName())
                        && "http://schemas.openxmlformats.org/spreadsheetml/2006/main".equals(doc.getDocumentElement().getNamespaceURI());
                if (name.equals("[Content_Types].xml")) {
                    var overrides = doc.getElementsByTagNameNS("http://schemas.openxmlformats.org/package/2006/content-types", "Override");
                    for (int i = 0; i < overrides.getLength(); i++) {
                        var item = (Element) overrides.item(i);
                        if (item.getAttribute("ContentType").toLowerCase(Locale.ROOT).contains("macro")) return false;
                        if (item.getAttribute("PartName").equals("/xl/workbook.xml")) types = item.getAttribute("ContentType").equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
                    }
                }
            }
            return workbook && types && names.contains("_rels/.rels");
        } catch (Exception e) { return false; }
    }
}
