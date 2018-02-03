package ptuxiaki.extraction;


import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

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
    private static final String LANG_TAG = "el-GR";
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
        sentences.add(content.substring(start).trim());
        return sentences.stream()
                .filter(s -> !s.isEmpty())
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

    public List<String> extractSentencesFromTextSentenceIterator() {
        final String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        iterator = BreakIterator.getSentenceInstance(Locale.forLanguageTag(LANG_TAG));
        iterator.setText(content);
        //start signifies the index of a piece of text that may be a sentence
        int start = iterator.first();
        // current index in the text body
        int current = iterator.next();
        List<String> sentences = new ArrayList<>();
        ArrayList<Integer> boundaries = new ArrayList<>();
        boolean titleFound = false;
        int len = content.length();
        int temp;
        while (current != BreakIterator.DONE && current < len) {
            boundaries.add(current);
            // in most cases the first sentence of a document contains it's title.
            // BreakIterator can't recognize the title automatically since it does
            // not end with any of it's stop characters. In order to get the title
            // we need to rescan that first sentence using lineBreakIterator.
            // This will create another additional boundary we need to account for
            // as we do in the if statement.
            // If the position is different (smaller) than the current position
            // returned from iterator, it means that the first sentence contains
            // the title as well as the rest of the text which constitutes a sentence
            // by itself and need to be added in the list.
            if (!titleFound) {
                final int titlePos = findTitle(content.substring(start, current));
                sentences.add(content.substring(start, titlePos));
                if (titlePos != current) {
                    sentences.add(content.substring(titlePos + 1, current));
                }
                start = current;
                titleFound = true;
                continue;
            }

            // backtrack
            // find the position of dot char
            int idx = current;
            while (content.charAt(idx--) != '.')
                ;
            // find the position of the whitespace char before idx
            int wIdx = idx;
            while (!Character.isWhitespace(content.charAt(wIdx--)))
                ;

            // check if it is a small word like υπ. Δρ. κ. etc
            if (idx - wIdx <= 3) {
                current = iterator.next();
                continue;
            }

            sentences.add(content.substring(start, current).trim());
            start = current;
            current = iterator.next();
        }
        // add the last sentence in the list
        sentences.add(content.substring(start).trim());
        return sentences;
    }

    /**
     * <p>This method is used to find the main title of a document.</p>
     * <p>It should only be called once for each document.</p>
     *
     * @implSpec <p>As the majority of document begins with a title that doesn't
     *           end with a dot character (.), we need to rely on other patterns
     *           to discover the title of a document.</p>
     *           One such pattern is \n followed by a capital case letter
     *           immediately after.
     *
     * @implNote Scan the text using
     *           {@link BreakIterator#getLineInstance(Locale) lineInstanceIterator}
     *           and compare the current and the previous position returned by
     *           the iterator. If the character in the current position is
     *           capital case the character in the previous position is \n
     *           return that position.
     *
     * @param text The string to search in.
     *             <p>Usually the first sentence of a document
     *             as returned from {@link BreakIterator#getSentenceInstance() sentenceInstanceIterator} iterator</p>
     * @return the position found by the iterator
     */
    private int findTitle(String text) {
        BreakIterator iterator = BreakIterator.getLineInstance(Locale.forLanguageTag(LANG_TAG));
        iterator.setText(text);
        int c;
        // iterator returns values from [0, text.length()] so we need to
        // guard for NullPointerException in the inner if clause
        while ((c = iterator.next()) != BreakIterator.DONE && c < text.length())  {
                /*npe here is c == text.length */
                /*                          v  */
            if (Character.isUpperCase(text.charAt(c)) && text.charAt(c-1) == '\n') {
                break;
            }
        }
        return c == -1 ? text.length() : c-1;
    }


    public List<String> extractSentencesFromFile(final String filePath) throws TikaException, SAXException, IOException {
        this.filePath = filePath;
        return extractSentencesFromTextSentenceIterator();
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
