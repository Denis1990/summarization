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

    private int findSecondaryTitle(String text) {
        BreakIterator iterator = BreakIterator.getLineInstance(Locale.forLanguageTag(LANG_TAG));
        iterator.setText(text);
        int c;
        int s = 0;
        // iterator returns values from [0, text.length()] so we need to
        // guard for NullPointerException in the inner if clause
        while ((c = iterator.next()) != BreakIterator.DONE && c < text.length())  {
                /*npe here is c == text.length */
                /*                          v  */
            if (Character.isUpperCase(text.charAt(c)) && text.charAt(c-1) == '\n') {
                if (text.substring(s, c-1).trim().split(" ").length < 7) {
                    break;
                }
                s = c;
            }
        }
        return c == -1 ? text.length() : c;
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

    public List<String> extractSentences() {
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
        int start = 0;
        // current index in the text body
        int current = iterator.next();
        List<String> sentences = new ArrayList<>();
        ArrayList<Integer> boundaries = new ArrayList<>();
        boolean titleFound = false;
        int len = content.length();
        int steps = 4; // steps to backtrack or lookahead
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
                current = iterator.next();
                continue;
            }

            // go back at most 4 steps
            // find the position of dot char
            int idx = current;
            while (steps-- > 0 && content.charAt(idx--) != '.')
                ;
            // find the position of the first whitespace char before idx
            int wIdx = idx;
            while (!Character.isWhitespace(content.charAt(wIdx--)))
                ;

            // check if it is a small word like υπ. Δρ. κ. etc
            if (idx - wIdx <= 3) {
                current = iterator.next();
                steps = 4;
                continue;
            }

            // check if the sentence contains a secondary title
            int pos = findSecondaryTitle(content.substring(start, current)) + start;
            if (pos < current) {
                sentences.add(content.substring(start, pos));
                sentences.add(content.substring(pos, current));
            } else {
                sentences.add(content.substring(start, current).trim());
            }
            start = current;
            current = iterator.next();
            steps = 4;
        }
        // add the last sentence in the list
        sentences.add(content.substring(start).trim());
        return sentences;
    }

    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
