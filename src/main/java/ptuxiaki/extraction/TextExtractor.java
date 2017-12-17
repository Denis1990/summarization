package ptuxiaki.extraction;


import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import ptuxiaki.datastructures.Paragraph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextExtractor {
    private static final String LANG_TAG = "el";
    private BreakIterator iterator;
    private String filePath;

    private ContentHandler extractFileContent() throws SAXException, TikaException, IOException  {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata md = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();

        InputStream is = TikaInputStream.get(new File(filePath).toPath());
        parser.parse(is, handler, md);
        return handler;
    }

    private List<String> getSentencesFromDocx() {
        List<String> sentences = new ArrayList<>();
        final String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        iterator.setText(content);
        int start = iterator.first();
        int end = iterator.next();
        int prev = -1;
        int consecutiveNewLines = 0;
        int temp;
        boolean isPossibleSentence = false;
        while (end != BreakIterator.DONE) {
            char c = content.charAt(end - 1);
            if (c == '.' || c == ':') {
                temp = end-2;
                // otan exoume suntomografies (υπ., π.χ., ν., αρ.)
                while (temp > 0 && !Character.isWhitespace(content.charAt(temp)) && content.charAt(temp) != '.') {
                    temp--;
                }
                if (end - temp > 4) {
                    sentences.add(content.substring(start, end).trim());
                    start = end;
                    consecutiveNewLines = 0;
                }
            } else if (c == '\n') {
                isPossibleSentence = true;
                consecutiveNewLines++;
                prev = end;
            } else if (consecutiveNewLines >= 2) {
                isPossibleSentence = false;
                consecutiveNewLines = 0;
            } else if (isPossibleSentence) {
                if (Character.isUpperCase(content.charAt(prev))) {
                    sentences.add(content.substring(start, prev).trim());
                    start = prev;
                    consecutiveNewLines = 0;
                }
                isPossibleSentence = false;
            }
            end = iterator.next();
        }
        // add the last sentence of the document if it did not end with a dot char
        sentences.add(content.substring(start));
        return sentences.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> getSentencesFromPdf() {
        List<String> sentences = new ArrayList<>();
        final String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        iterator.setText(content);
        int start = iterator.first();
        int end = iterator.next();
        int prev = -1;
        boolean isPossibleSentence = false;
        int temp;
        while (end != BreakIterator.DONE) {
            char c = content.charAt(end - 1);
            if (c == '.' || c == ':') {
                temp = end-2;
                // otan exoume suntomografies (υπ., π.χ., ν., αρ.)
                while (temp > 0 && !Character.isWhitespace(content.charAt(temp)) && content.charAt(temp) != '.') {
                    temp--;
                }
                if (end - temp > 4) {
                    sentences.add(content.substring(start, end).trim());
                    start = end;
                }
            } else if (c == '\n') {
                isPossibleSentence = true;
                prev = end;
            } else if (isPossibleSentence) {
                if (Character.isUpperCase(content.charAt(prev))) {
                    sentences.add(content.substring(start, prev).trim());
                    start = prev;
                }
                isPossibleSentence = false;
            }
            end = iterator.next();
        }
        sentences.add(content.substring(start));
        return sentences.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> getSentencesFromTxt() {
        List<String> sentences = new ArrayList<>();
        final String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        iterator.setText(content);
        int start = iterator.first();
        int end = iterator.next();
        int prev = -1;
        int consecutiveNewLines = 0;
        boolean isPossibleSentence = false;
        int temp;
        while (end != BreakIterator.DONE) {
            char c = content.charAt(end - 1);
            if (c == '.' || c == ':') {
                temp = end-2;
                // otan exoume suntomografies (υπ., π.χ., ν., αρ.)
                while (temp > 0 && !Character.isWhitespace(content.charAt(temp)) && content.charAt(temp) != '.') {
                    temp--;
                }
                if (end - temp > 4) {
                    sentences.add(content.substring(start, end).trim());
                    start = end;
                    consecutiveNewLines = 0;
                }
            } else if (content.charAt(end - 1) == '\n') {
                isPossibleSentence = true;
                consecutiveNewLines++;
                prev = end;
            } else if (consecutiveNewLines >= 2) {
                isPossibleSentence = false;
                consecutiveNewLines = 0;
            } else if (isPossibleSentence) {
                if (Character.isUpperCase(content.charAt(prev))) {
                    sentences.add(content.substring(start, end).trim());
                    consecutiveNewLines = 0;
                }
                isPossibleSentence = false;
            }
            end = iterator.next();
        }
        sentences.add(content.substring(start).trim());
        return sentences.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> getSentencesFromText(String text) {

        iterator = BreakIterator.getSentenceInstance(new Locale("el", "gr"));
        List<Integer> boundaries = new ArrayList<>();
        iterator.setText(text);
        int bound;
        while((bound = iterator.next()) != BreakIterator.DONE) {
            int i = bound - 2;
            while (i > 0 && !Character.isWhitespace(text.charAt(i)))
                i--;
            if ((bound - 2) - i < 3)
                continue;

            boundaries.add(bound);
        }

        int start = 0;
        List<String> sentences = new ArrayList<>();

        // if we found no sentence assume the entire text is one sentence.
        if (boundaries.isEmpty()) {
            sentences.add(text);
            return sentences;
        }

        while (!boundaries.isEmpty()) {
            int next = boundaries.remove(0);
            sentences.add(text.substring(start, next));
            start = next;
        }
        return sentences;
    }

    public List<String> extractSentences() {
        if (filePath.endsWith("html")) return Collections.emptyList();

        // TODO: make language configurable?
        iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(LANG_TAG));

        if (filePath.endsWith("pdf")) {
            return getSentencesFromPdf();
        } else if (filePath.endsWith("doc") || filePath.endsWith("docx") || filePath.endsWith("odt")) {
            return getSentencesFromDocx();
        } else if (filePath.endsWith("html")) {
            return Collections.emptyList();
        } else if (filePath.endsWith("txt")) {
            return getSentencesFromTxt();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     *
     * @param sentSize we need it in order to estimate if the document has any paragraphs
     * @return
     */
    public List<Paragraph> extractParagraphs(int sentSize) {
        if (filePath.endsWith(".html")) return Collections.emptyList();

        Pattern parSeparator = Pattern.compile("\\n");


        String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        String [] paragraphsBlocks = parSeparator.split(content);
        int paragraphcount = (int) Arrays.stream(paragraphsBlocks).filter(s -> !s.isEmpty()).count();
        List<Paragraph> paragraphs = new ArrayList<>(paragraphsBlocks.length);

        // 10% of the sentences of the document
        int delta = Math.round(sentSize * 0.1f);

        // if number_of_sentences - number_of_paragraphs < delta then assume the document has no paragraphs
        if ((sentSize - paragraphcount) < delta) {
            System.out.println(filePath + " does not have paragraphs");
            return Collections.emptyList();
        }

        int i = 0;
        for(String p : paragraphsBlocks) {
            if (p.isEmpty()) continue;
            int position = 1; // the position of the sentence inside the paragraph
            Paragraph par = new Paragraph(i++);

            for (String s : getSentencesFromText(p)) {
                par.addSentence(new Paragraph.Sentence(position++, s.hashCode()));
            }
            paragraphs.add(par);
        }

        return paragraphs;
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
