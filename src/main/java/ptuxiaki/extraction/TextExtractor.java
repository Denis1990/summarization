package ptuxiaki.extraction;


import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import ptuxiaki.utils.SentenceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TextExtractor {
    private static final String LANG_TAG = "el_GR";
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

    public List<String> extractSentencesFromText(String text) {
        iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(LANG_TAG));
        iterator.setText(text);
        int done;
        //start signifies the index of a piece of text that may be a sentence
        int start = iterator.first();
        // current index in the text body
        int current = iterator.next();
        boolean isSentence = true;
        List<String> sentences = new ArrayList<>();
        int len = text.length();
        int dotIdx = -1;
        while (current != BreakIterator.DONE && current < len) {

            // find sentences that end with a dot
            char c = text.charAt(current);
            if (c == '.') {
                //check for one char sentences like: υπ. κ.
                if (current - start < 3) {
                    isSentence = false;
                    current = iterator.next();
                    continue;
                }
                sentences.add(text.substring(start, current).trim());
                start = current + 1;
                dotIdx = current;
            } else if (Character.isUpperCase(text.charAt(current + 1)) && isSentence && current != dotIdx) {
                // find sentences that doesn't end with a dot
                // we consider a capital first character word
                // after a white space or after anything other
                // than dot character to constitute a new sentence
                sentences.add(text.substring(start, current).trim());
                start = current + 1;
            } else if (Character.isSpaceChar(c))
            isSentence = true;
            current = iterator.next();
        }
        // if no sentences have been found until the end
        // of the text consider the entire string a single sentence.
        if (sentences.isEmpty()) {
            sentences.add(text.trim());
        } else if (current - start != 0) {
            // add any remaining portion of the text
            sentences.add(text.substring(start, current).trim());
        }
        return sentences;
    }

    public List<String> extractSentencesFromFile(final String filePath) throws TikaException, SAXException, IOException {
        this.filePath = filePath;
        return extractSentencesFromText(extractFileContent().toString());
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
