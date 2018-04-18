package ptuxiaki.extraction;


import org.apache.commons.lang3.tuple.Triple;
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
    private static final String LANG_TAG = "el-GR";
    private BreakIterator iterator;
    private String filePath;

    private static final int SECONDARY_TITLE_MIN_WORDS = 9;
    private ContentHandler extractFileContent() throws SAXException, TikaException, IOException  {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata md = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();

        InputStream is = TikaInputStream.get(new File(filePath).toPath());
        parser.parse(is, handler, md);
        return handler;
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
    private int findTitlePos(String text) {
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

    /**
     * This methods extracts the sentences from a paragraph.
     * Similarly to {@#extractSentences} this method uses {@link BreakIterator#getSentenceInstance() sentenceInstance}
     * to scan through the text.
     * @implNote Scan each portion of text returned from the iterator object and look for small words like υπ. Δρ. etc
     * @param text the paragraphs text.
     */
    private List<String> getSentencesFromText(String text) {
        iterator = BreakIterator.getSentenceInstance(Locale.forLanguageTag(LANG_TAG));
        List<String> sents = new ArrayList<>();
        iterator.setText(text);
        int end = iterator.next();
        int start = 0;
        int steps = 4;
        while(end != text.length() && end != BreakIterator.DONE) {

            // go back at most 4 steps
            // find the position of dot char
            int idx = end;
            while (steps-- > 0 && text.charAt(idx--) != '.')
                ;
            // find the position of the first whitespace char before idx
            int wIdx = idx;
            while (wIdx > 0 && !Character.isWhitespace(text.charAt(wIdx--)))
                ;

            // check if it is a small word like υπ. Δρ. κ. etc
            if (idx - wIdx <= 4) {
                end = iterator.next();
                steps = 4;
                continue;
            }
            sents.add(text.substring(start, end));
            start = end;
            steps = 4;
            end = iterator.next();
        }

        sents.add(text.substring(start, end));
        return sents.stream().filter(s->!s.isEmpty()).collect(Collectors.toList());
    }

    /**
     * <p>Extract the sentences from a document.</p>
     * The document should be passed with the {@link #setFile(String)} method.
     * @implNote <p>We use a {@link BreakIterator#getSentenceInstance() sentenceInstance} to scan through
     *           the content of the document.</p>
     *
     * @return a list of strings each representing a sentence.
     */
    public List<String> extractSentences() {
        final String content;
        try {
            content = extractFileContent().toString().trim();
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
        boolean titleFound = false;
        int len = content.length();
        int steps = 4; // steps to backtrack or lookahead
        while (current != BreakIterator.DONE && current < len) {
            // in most cases the first sentence of a document contains it's title.
            // BreakIterator can't recognize the title automatically since it does
            // not end with any of it's stop characters. In order to get the title
            // we need to rescan that first sentence using lineBreakIterator.
            // This will create an additional boundary we need to account for
            // as we do in the if statement.
            // If the position is different (smaller) than the current position
            // returned from iterator, it means that the first sentence contains
            // the title as well as the rest of the text which constitutes a sentence
            // by itself and needs to be added in the list.
            if (!titleFound) {
                final int titlePos = findTitlePos(content.substring(start, current));
                // titles are uppercase
                sentences.add(content.substring(start, titlePos).toUpperCase().trim());
                start = titlePos+1;
                titleFound = true;
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
            if (idx - wIdx <= 4) {
                current = iterator.next();
                steps = 4;
                continue;
            }

            String str = content.substring(start, current);
            int strLen = str.length();
            int cur = 0;
            ArrayList<Integer> positions = new ArrayList<>();
            int pos;
            // find all the newline characters in the str segment
            while ((pos = str.indexOf('\n', cur)) != -1 ) {
                // add one after pos except when you reach the end of string
                if (pos + 1 == strLen) {
                    positions.add(pos);
                } else {
                    positions.add(pos+1);
                }
                cur = cur + pos + 1;
            }
            cur = 0;
            while (positions.size() > 0) {
                int p = positions.remove(0);
                // check if the character after newLine is capital
                // and add it as a separate sentence or secondary title
                // if it has less than SECONDARY_TITLE_MIN_WORDS
                if (Character.isUpperCase(str.charAt(p))) {
                    if (str.substring(cur, p).split("\\s+").length < SECONDARY_TITLE_MIN_WORDS) {
                        sentences.add(str.substring(cur, p).trim().toUpperCase());
                        cur = p;
                    } else {
                        sentences.add(str.substring(cur, p).trim());
                        cur = p;
                    }
                }
            }

            sentences.add(str.substring(cur).trim());
            start = current;
            current = iterator.next();
            steps = 4;
        }
        // add the last sentence in the list
        sentences.add(content.substring(start).trim());
        return sentences.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    public List<Paragraph> extractParagraphs(int sentSize) {
        if (filePath.endsWith(".html")) return Collections.emptyList();
        Pattern parSeparator;
        if (filePath.endsWith(".txt") || filePath.endsWith(".odt") || filePath.endsWith(".docx")) {
            parSeparator = Pattern.compile("\\n");
        } else {
            parSeparator = Pattern.compile("\\n\\n");
        }

        String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        String[] paragraphsBlocks = parSeparator.split(content);
        int paragraphcount = (int) Arrays.stream(paragraphsBlocks).filter(s -> !s.isEmpty() && s.length() > 1).count();
        List<Paragraph> paragraphs = new ArrayList<>(paragraphsBlocks.length);

        // 10% of the sentences of the document
        int delta = Math.round(sentSize * 0.1f);

        // if number_of_sentences - number_of_paragraphs < delta then assume the document has no paragraphs
        if ((sentSize - paragraphcount) < delta) {
            System.out.println(filePath + " does not have paragraphs");
            return Collections.emptyList();
        }

        int parPos = 0; // position of paragraph in document
        int sentGlPos= 0; // sentence global position. The position of the sentence inside the document
        for (String p : paragraphsBlocks) {
            if (p.trim().isEmpty()) continue;
            int sentPos = 1; // the position of the sentence inside the paragraph
            Paragraph par = new Paragraph(parPos++);
            for (String s : getSentencesFromText(p.trim())) {
                par.addSentence(Triple.of(s.trim(), sentPos, sentGlPos));
                sentPos++;
                sentGlPos++;
            }
            paragraphs.add(par);
        }
        return paragraphs;
    }

    /**
     * Set the name of the file to extract sentences from.
     * @param filePath the path to the file.
     */
    public void setFile(final String filePath) {
        this.filePath = filePath;
    }
}
