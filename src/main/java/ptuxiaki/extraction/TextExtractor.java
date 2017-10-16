package ptuxiaki.extraction;


import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import ptuxiaki.datastructures.Paragraph;
import ptuxiaki.utils.SentenceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
                while (!Character.isWhitespace(content.charAt(temp)) && content.charAt(temp) != '.') {
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
        return sentences.stream().filter(s -> !s.isEmpty())
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
                while (!Character.isWhitespace(content.charAt(temp)) && content.charAt(temp) != '.') {
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
        return sentences;
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
        while (end != BreakIterator.DONE) {
            if (content.charAt(end - 1) == '.') {
                sentences.add(content.substring(start, end).trim());
                start = end;
                consecutiveNewLines = 0;
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

        return sentences.stream().filter(s -> !s.isEmpty())
                .map(SentenceUtils::removeSpecialChars)
                .map(SentenceUtils::removeTonation)
                .map(SentenceUtils::replaceSigma)
                .map(SentenceUtils::removeWhiteSpaces)
                .collect(Collectors.toList());
    }

    public List<String> extractSentences() {
        if (filePath.endsWith(".html")) return Collections.emptyList();

        // TODO: make language configurable?
        iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(LANG_TAG));

        if (filePath.endsWith(".pdf")) {
            return getSentencesFromPdf();
        } else if (filePath.endsWith(".doc") || filePath.endsWith(".docx")) {
            return getSentencesFromDocx();
        } else if (filePath.endsWith(".html")) {
            return Collections.emptyList();
        } else if (filePath.endsWith(".txt")) {
            return getSentencesFromTxt();
        } else {
            return Collections.emptyList();
        }
    }

    public List<Paragraph> extractParagraphs() {
        if (filePath.endsWith(".html")) return Collections.emptyList();

        Pattern parSeparator;

        if (filePath.endsWith(".txt")) {
            parSeparator = Pattern.compile("\\n\\n");
        } else {
            parSeparator = Pattern.compile("\\n");
        }

        String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        String [] paragraphsBlocks = parSeparator.split(content);
        List<Paragraph> paragraphs = new ArrayList<>(paragraphsBlocks.length);

        int paragraphCount = 0;
        for(String p : paragraphsBlocks) {
            int location = 1; // at least one sentence
            Paragraph par = new Paragraph(paragraphCount++);
            for (String s : p.split("\\.")) {
                par.addSentence(new Paragraph.Sentence(location++, s.hashCode()));
            }
            paragraphs.add(par);
        }

        return paragraphs;
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
