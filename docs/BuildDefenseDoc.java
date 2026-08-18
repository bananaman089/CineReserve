import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Генерира .docx по методическите указания (A4, Times New Roman, 1.5, полета).
 */
public class BuildDefenseDoc {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    public static void main(String[] args) throws Exception {
        Path base = Path.of(args.length > 0 ? args[0] : ".");
        Path content = base.resolve("zastita-sydyrzhanie.txt");
        Path out = base.resolve("Pismena_zastita_CineReserve.docx");
        String xml = buildDocument(Files.readAllLines(content, StandardCharsets.UTF_8));
        writeDocx(out, xml);
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    private static void writeDocx(Path out, String documentXml) throws IOException {
        Files.createDirectories(out.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", relsRoot());
            put(zip, "word/_rels/document.xml.rels", relsDocument());
            put(zip, "word/document.xml", documentXml);
            put(zip, "word/styles.xml", styles());
            put(zip, "word/numbering.xml", numbering());
            put(zip, "word/settings.xml", settings());
            put(zip, "word/fontTable.xml", fontTable());
            put(zip, "word/footer1.xml", footer());
            put(zip, "docProps/core.xml", core());
            put(zip, "docProps/app.xml", app());
        }
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String buildDocument(List<String> lines) {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        b.append("<w:document xmlns:w=\"").append(W).append("\" xmlns:r=\"").append(R).append("\">");
        b.append("<w:body>");

        List<String> tableRows = null;
        List<String> codeLines = null;

        for (String raw : lines) {
            if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
            String line = raw;
            if (line.isBlank()) continue;

            if (codeLines != null) {
                if (line.equals("CE|")) {
                    b.append(codeBlock(codeLines));
                    codeLines = null;
                } else if (line.startsWith("C|")) {
                    codeLines.add(line.substring(2));
                }
                continue;
            }
            if (tableRows != null) {
                if (line.equals("TE|")) {
                    b.append(table(tableRows));
                    tableRows = null;
                } else if (line.startsWith("TR|")) {
                    tableRows.add(line.substring(3));
                }
                continue;
            }

            if (line.equals("NP|")) {
                b.append(pageBreak());
            } else if (line.startsWith("H1|")) {
                b.append(heading(1, line.substring(3)));
            } else if (line.startsWith("H2|")) {
                b.append(heading(2, line.substring(3)));
            } else if (line.startsWith("H3|")) {
                b.append(heading(3, line.substring(3)));
            } else if (line.startsWith("P|")) {
                b.append(paraJustify(line.substring(2)));
            } else if (line.startsWith("C|") && line.equals("C|BEGIN")) {
                codeLines = new ArrayList<>();
            } else if (line.equals("CB|")) {
                codeLines = new ArrayList<>();
            } else if (line.startsWith("TC|")) {
                b.append(caption(line.substring(3), true));
            } else if (line.startsWith("F|")) {
                b.append(caption(line.substring(2), false));
            } else if (line.equals("TB|")) {
                tableRows = new ArrayList<>();
            } else if (line.startsWith("CENTER|")) {
                b.append(centered(line.substring(7), false));
            } else if (line.startsWith("CENTERB|")) {
                b.append(centered(line.substring(8), true));
            } else if (line.equals("TOC|")) {
                b.append(staticToc());
            }
        }

        b.append(sectPr(true));
        b.append("</w:body></w:document>");
        return b.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String run(String text, String extra) {
        return "<w:r>" + extra + "<w:t xml:space=\"preserve\">" + esc(text) + "</w:t></w:r>";
    }

    private static String paraJustify(String text) {
        StringBuilder p = new StringBuilder("<w:p><w:pPr>");
        p.append("<w:spacing w:line=\"360\" w:lineRule=\"auto\" w:after=\"200\"/>");
        p.append("<w:jc w:val=\"both\"/>");
        p.append("<w:ind w:firstLine=\"709\"/>");
        p.append("</w:pPr>");
        p.append(run(text, rPr("24", false, false)));
        p.append("</w:p>");
        return p.toString();
    }

    private static String centered(String text, boolean bold) {
        StringBuilder p = new StringBuilder("<w:p><w:pPr>");
        p.append("<w:spacing w:line=\"360\" w:lineRule=\"auto\" w:after=\"120\"/>");
        p.append("<w:jc w:val=\"center\"/>");
        p.append("</w:pPr>");
        p.append(run(text, rPr("24", bold, false)));
        p.append("</w:p>");
        return p.toString();
    }

    private static String heading(int level, String text) {
        String sz = level == 1 ? "32" : (level == 2 ? "28" : "26");
        boolean italic = level == 3;
        StringBuilder p = new StringBuilder("<w:p><w:pPr>");
        if (level == 1) p.append("<w:pageBreakBefore/>");
        p.append("<w:pStyle w:val=\"Heading").append(level).append("\"/>");
        p.append("<w:spacing w:before=\"360\" w:after=\"240\" w:line=\"360\" w:lineRule=\"auto\"/>");
        p.append("<w:jc w:val=\"").append(level == 1 ? "center" : "left").append("\"/>");
        p.append("<w:outlineLvl w:val=\"").append(level - 1).append("\"/>");
        p.append("</w:pPr>");
        p.append(run(text, rPr(sz, true, italic)));
        p.append("</w:p>");
        return p.toString();
    }

    private static String rPr(String sz, boolean bold, boolean italic) {
        StringBuilder r = new StringBuilder("<w:rPr>");
        r.append("<w:rFonts w:ascii=\"Times New Roman\" w:hAnsi=\"Times New Roman\" w:cs=\"Times New Roman\"/>");
        if (bold) r.append("<w:b/><w:bCs/>");
        if (italic) r.append("<w:i/><w:iCs/>");
        r.append("<w:sz w:val=\"").append(sz).append("\"/><w:szCs w:val=\"").append(sz).append("\"/>");
        r.append("</w:rPr>");
        return r.toString();
    }

    private static String caption(String text, boolean table) {
        StringBuilder p = new StringBuilder("<w:p><w:pPr>");
        p.append("<w:spacing w:before=\"120\" w:after=\"200\" w:line=\"360\" w:lineRule=\"auto\"/>");
        p.append("<w:jc w:val=\"").append(table ? "center" : "center").append("\"/>");
        p.append("</w:pPr>");
        p.append(run(text, rPr("22", true, true)));
        p.append("</w:p>");
        return p.toString();
    }

    private static String pageBreak() {
        return "<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>";
    }

    private static String codeBlock(List<String> lines) {
        StringBuilder b = new StringBuilder();
        for (String line : lines) {
            b.append("<w:p><w:pPr>");
            b.append("<w:shd w:val=\"clear\" w:fill=\"F2F2F2\"/>");
            b.append("<w:spacing w:line=\"276\" w:lineRule=\"auto\" w:after=\"0\"/>");
            b.append("<w:ind w:left=\"200\"/>");
            b.append("</w:pPr>");
            b.append(run(line, "<w:rPr><w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:cs=\"Courier New\"/><w:sz w:val=\"20\"/><w:szCs w:val=\"20\"/></w:rPr>"));
            b.append("</w:p>");
        }
        return b.toString();
    }

    private static String table(List<String> rows) {
        if (rows.isEmpty()) return "";
        int cols = rows.get(0).split("\\|", -1).length;
        int width = 9026 / cols;
        StringBuilder t = new StringBuilder("<w:tbl>");
        t.append("<w:tblPr><w:tblW w:w=\"9026\" w:type=\"dxa\"/>");
        t.append("<w:tblBorders>");
        for (String edge : List.of("top", "left", "bottom", "right", "insideH", "insideV")) {
            t.append("<w:").append(edge).append(" w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"000000\"/>");
        }
        t.append("</w:tblBorders></w:tblPr>");
        t.append("<w:tblGrid>");
        for (int i = 0; i < cols; i++) t.append("<w:gridCol w:w=\"").append(width).append("\"/>");
        t.append("</w:tblGrid>");
        boolean header = true;
        for (String row : rows) {
            String[] cells = row.split("\\|", -1);
            t.append("<w:tr>");
            for (int i = 0; i < cols; i++) {
                String cell = i < cells.length ? cells[i] : "";
                t.append("<w:tc><w:tcPr><w:tcW w:w=\"").append(width).append("\" w:type=\"dxa\"/>");
                if (header) t.append("<w:shd w:val=\"clear\" w:fill=\"E8E8E8\"/>");
                t.append("</w:tcPr><w:p><w:pPr><w:spacing w:after=\"40\" w:line=\"276\" w:lineRule=\"auto\"/></w:pPr>");
                t.append(run(cell, rPr(header ? "22" : "22", header, false)));
                t.append("</w:p></w:tc>");
            }
            t.append("</w:tr>");
            header = false;
        }
        t.append("</w:tbl>");
        t.append("<w:p><w:pPr><w:spacing w:after=\"200\"/></w:pPr></w:p>");
        return t.toString();
    }

    private static String staticToc() {
        return heading(2, "Съдържание")
                + paraJustify("Списъкът по-долу съответства на задължителната структура от методическите указания. При отваряне в Microsoft Word полето за съдържание може да се обнови автоматично чрез десен бутон върху съдържанието и „Update Field“.")
                + tocLine("1. Увод")
                + tocLine("2. Теоретична част")
                + tocLine("2.1. Конфигурация и сигурност")
                + tocLine("2.2. Същност на предметната област")
                + tocLine("2.3. Сходни решения")
                + tocLine("2.4. Технологичен стек")
                + tocLine("2.5. Обосновка на технологиите")
                + tocLine("3. Анализ и проектиране на системата")
                + tocLine("3.1. Функционални изисквания")
                + tocLine("3.2. Нефункционални изисквания")
                + tocLine("3.3. Роли и права")
                + tocLine("3.4. Архитектура")
                + tocLine("3.5. Модел на базата данни")
                + tocLine("3.6. Основни сценарии на употреба")
                + tocLine("4. Реализация")
                + tocLine("4.1. Среда за разработка")
                + tocLine("4.2. Модули на системата")
                + tocLine("4.3. Ключова бизнес логика")
                + tocLine("4.4. Нефункционални решения в кода")
                + tocLine("5. Тестване, резултати и демонстрация")
                + tocLine("6. Заключение")
                + tocLine("Използвана литература")
                + tocLine("Приложения");
    }

    private static String tocLine(String text) {
        StringBuilder p = new StringBuilder("<w:p><w:pPr>");
        p.append("<w:tabs><w:tab w:val=\"right\" w:leader=\"dot\" w:pos=\"9026\"/></w:tabs>");
        p.append("<w:ind w:left=\"0\"/>");
        p.append("<w:spacing w:line=\"360\" w:lineRule=\"auto\" w:after=\"80\"/>");
        p.append("</w:pPr>");
        p.append(run(text, rPr("24", false, false)));
        p.append("</w:p>");
        return p.toString();
    }

    private static String sectPr(boolean withFooter) {
        StringBuilder s = new StringBuilder("<w:sectPr>");
        if (withFooter) {
            s.append("<w:footerReference w:type=\"default\" r:id=\"rId8\"/>");
        }
        s.append("<w:pgSz w:w=\"11906\" w:h=\"16838\" w:orient=\"portrait\"/>");
        s.append("<w:pgMar w:top=\"1418\" w:right=\"1134\" w:bottom=\"1418\" w:left=\"1701\" w:header=\"708\" w:footer=\"708\"/>");
        s.append("<w:pgNumType w:start=\"1\"/>");
        s.append("</w:sectPr>");
        return s.toString();
    }

    private static String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
                  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
                  <Override PartName="/word/fontTable.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml"/>
                  <Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>
                  <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
                  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                </Types>
                """;
    }

    private static String relsRoot() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                </Relationships>
                """;
    }

    private static String relsDocument() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
                  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>
                  <Relationship Id="rId8" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
                </Relationships>
                """;
    }

    private static String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:docDefaults>
                    <w:rPrDefault><w:rPr>
                      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
                      <w:sz w:val="24"/><w:szCs w:val="24"/>
                    </w:rPr></w:rPrDefault>
                    <w:pPrDefault><w:pPr>
                      <w:spacing w:line="360" w:lineRule="auto"/>
                    </w:pPr></w:pPrDefault>
                  </w:docDefaults>
                  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                    <w:name w:val="Normal"/>
                    <w:qFormat/>
                  </w:style>
                  <w:style w:type="paragraph" w:styleId="Heading1">
                    <w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:uiPriority w:val="9"/><w:qFormat/>
                    <w:pPr><w:keepNext/><w:outlineLvl w:val="0"/></w:pPr>
                  </w:style>
                  <w:style w:type="paragraph" w:styleId="Heading2">
                    <w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:uiPriority w:val="9"/><w:qFormat/>
                    <w:pPr><w:keepNext/><w:outlineLvl w:val="1"/></w:pPr>
                  </w:style>
                  <w:style w:type="paragraph" w:styleId="Heading3">
                    <w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:uiPriority w:val="9"/><w:qFormat/>
                    <w:pPr><w:keepNext/><w:outlineLvl w:val="2"/></w:pPr>
                  </w:style>
                </w:styles>
                """;
    }

    private static String numbering() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>
                """;
    }

    private static String settings() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:zoom w:percent="100"/>
                </w:settings>
                """;
    }

    private static String fontTable() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:font w:name="Times New Roman"><w:family w:val="roman"/><w:pitch w:val="variable"/></w:font>
                  <w:font w:name="Courier New"><w:family w:val="modern"/><w:pitch w:val="fixed"/></w:font>
                </w:fonts>
                """;
    }

    private static String footer() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:p>
                    <w:pPr><w:jc w:val="right"/></w:pPr>
                    <w:r>
                      <w:rPr>
                        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/>
                        <w:sz w:val="24"/>
                      </w:rPr>
                      <w:fldChar w:fldCharType="begin"/>
                    </w:r>
                    <w:r>
                      <w:rPr>
                        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/>
                        <w:sz w:val="24"/>
                      </w:rPr>
                      <w:instrText xml:space="preserve"> PAGE </w:instrText>
                    </w:r>
                    <w:r>
                      <w:rPr>
                        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/>
                        <w:sz w:val="24"/>
                      </w:rPr>
                      <w:fldChar w:fldCharType="end"/>
                    </w:r>
                  </w:p>
                </w:ftr>
                """;
    }

    private static String core() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                  xmlns:dc="http://purl.org/dc/elements/1.1/"
                  xmlns:dcterms="http://purl.org/dc/terms/"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <dc:title>Теоретична писмена защита — CineReserve</dc:title>
                  <dc:subject>Enterprise система за резервация на билети за кино</dc:subject>
                  <dc:creator>Ученик</dc:creator>
                </cp:coreProperties>
                """;
    }

    private static String app() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
                  <Application>CineReserve Defense Builder</Application>
                </Properties>
                """;
    }
}
