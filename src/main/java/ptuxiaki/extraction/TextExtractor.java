package ptuxiaki.extraction;


import org.apache.commons.lang3.tuple.Pair;
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
import java.util.*;
import java.util.regex.Matcher;
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

    private List<String> getSentencesFromHtml() {
        return Collections.emptyList();
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

    public Pair<Integer, Integer> newLinesStats() {
        iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(LANG_TAG));

        final String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Pair.of(-1,-1);
        }

        int pos = 0, state = 0;
        int doubleNewLines = 0, newLines = 0;
        while(pos < content.length()) {
            if (content.charAt(pos) == '\n' && state == 1) {
                doubleNewLines++;
                state = 0;
            } else if (content.charAt(pos) == '\n') {
                state = 1;
            } else if (content.charAt(pos) != '\n' && state == 1) {
                newLines++;
                state = 0;
            }
            pos++;
        }

        return Pair.of(newLines, doubleNewLines);
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

    public List<String> extractParagraphs() throws SAXException, TikaException, IOException {
        if (filePath.endsWith(".html")) return Collections.<String>emptyList();

        iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(LANG_TAG));


        AutoDetectParser parser = new AutoDetectParser();
        Metadata md = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();

        InputStream is = TikaInputStream.get(new File(filePath).toPath());
        parser.parse(is, handler, md);

        Pattern doubleNewLine = Pattern.compile("\\n\\n");

        // spaw to keimeno se grammes kai tis apothikeuw se mia lista
        ArrayList<String> lines = new ArrayList<>(
                Arrays.asList(doubleNewLine.split(handler.toString(), 0))
        );
        int to, from, pos, idx, i = 0;
        int len = lines.size();

        ArrayList<Integer> positions = new ArrayList<>();

        // break iterator gia protaseis
        BreakIterator brItr = BreakIterator.getSentenceInstance(Locale.forLanguageTag("el"));
        List<String> paragraphs = new ArrayList<>();
        // gia kathe grammi bres tis protaseis pou uparxoun se autin
        while (i < len) {
            final String line = lines.get(i++);

            // feedare tin grammi ston break iterator
            brItr.setText(line);

            // krata tis theseis sto string pou teleiwnei protasi
            while ((to = brItr.next()) != BreakIterator.DONE) {
                positions.add(to);
            }

            String sentence;
            pos = 0;
            from = 0;
            while (pos < positions.size()) {
                sentence = line.substring(from, positions.get(pos));
                idx = sentence.indexOf("\n");
                if (idx > 0 && idx + 1 < sentence.length()) {
                    if (Character.isLowerCase(sentence.charAt(idx-1))
                            && Character.isUpperCase(sentence.charAt(idx+1))) {
                        sentence = sentence.substring(idx+1);
                    } else {
                        sentence = sentence.replace('\n', ' ');
                    }
                }
                paragraphs.add(sentence);
                from = positions.get(pos);
                pos++;
            }
            positions.clear();
        }
        return paragraphs;
    }

    public List<String> extractTitles() throws SAXException, TikaException, IOException {
        if (filePath.endsWith(".html")) return Collections.emptyList();

        final String content = extractFileContent().toString();

        List<String> titles = new ArrayList<>();

        Matcher m = Pattern.compile("\\.\\n{2}[Α-Ω]*|\\n{2}[Α-Ω]*").matcher(content);
        List<Integer> positions = new ArrayList<>();
        while (m.find()) {
            positions.add(m.start());
            positions.add(m.end());
        }

        // in order to avoid IndexOutOfBoundException below when removing elements
        if (positions.size() % 2 == 0) {
            positions.add(positions.get(positions.size()-1));
        }

        int start = 0;
        int end = positions.remove(0);
        // add the first line of the document as a title
        titles.add(content.substring(start, end));
        while (!positions.isEmpty()) {
            start = positions.remove(0);
            end = positions.remove(0);
            if (content.substring(start-1, end).matches("(^\\d+\\.\\d+)|(^\\d+)")) {
                titles.add(content.substring(start-1, end));
            } else if (content.substring(start-1, end).length() < 7) {
                titles.add(content.substring(start, end));
            }
        }

        return titles.stream()
                .map(SentenceUtils::removeSpecialChars)
                .map(SentenceUtils::removeTonation)
                .map(SentenceUtils::replaceSigma)
                .map(SentenceUtils::removeWhiteSpaces)
                .map(SentenceUtils::stemSentence)
                .collect(Collectors.toList());
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
