package ptuxiaki.extraction;


import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import ptuxiaki.datastructures.Paragraph;
import ptuxiaki.datastructures.Sentence;
import ptuxiaki.datastructures.SentenceType;
import ptuxiaki.utils.SentenceUtils;

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
     * This methods extracts the sentences from a paragraph.
     * This method uses {@link BreakIterator#getSentenceInstance() sentenceInstance}
     * to scan through the text.
     * @param text the paragraphs text.
     */
    private List<String> getSentencesFromParagraph(String text) {
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
            while (steps-- > 0 && text.charAt(idx) != '.') {
                idx--;
            }
            // find the position of the first whitespace char before idx
            int wIdx = idx;
            while (wIdx > 0 && !Character.isWhitespace(text.charAt(wIdx))) {
                wIdx--;
            }

            // check if it is a small word like υπ. Δρ. κ. etc
            String substrt = text.substring(wIdx, idx).trim();
            if (substrt.length() <= 4 && !substrt.matches(".*\\d.") &&
                    !(substrt.contains("του") || substrt.contains("των") || substrt.contains("τις") )) {
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

    public List<Paragraph> extractParagraphs() {
        Pattern parSeparator = Pattern.compile("\\n");

        String content;
        try {
            content = extractFileContent().toString();
        } catch (SAXException | TikaException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        String[] paragraphsBlocks = parSeparator.split(content);
        int paragraphcount = (int) Arrays.stream(paragraphsBlocks).filter(s -> !s.isEmpty()).count();
        List<Paragraph> paragraphs = new ArrayList<>(paragraphcount);
        boolean titleNotFound = true;
        int sentPos = 0;
        int sentPosInPar = 0;
        int paragraphCount = 0;
        // For each paragraph block get the sentences and make a list of paragraph objects
        for (String par : paragraphsBlocks) {
            if (par.isEmpty()) continue;
            List<String> sents = getSentencesFromParagraph(par);
            final Paragraph p = new Paragraph();
            // check for title subtitles etc
            if (sents.size() == 1) {
                final String sent = SentenceUtils.removeSpecialChars(sents.get(0));
                // check if the sentence starts with a capital case letter
                // if does not end with a dot character and
                // if it has less than 5 words (debatable)
                if (Character.isUpperCase(sent.charAt(0))
                        && sent.charAt(sent.length() - 1) != '.'
                        && sent.split("\\s+").length <= 7
                        && titleNotFound) {
                    p.addSentence(new Sentence(sent, SentenceType.TITLE, sentPos, sentPosInPar));
                    titleNotFound = false;
                } else if (Character.isUpperCase(sent.charAt(0))  // subtitle
                        && sent.charAt(sent.length() - 1) != '.'
                        && sent.split("\\s+").length <= SECONDARY_TITLE_MIN_WORDS) {
                    p.addSentence(new Sentence(sent, SentenceType.SUBTITLE, sentPos, sentPosInPar));
                } else { // single sentence paragraph
                    p.addSentence(new Sentence(sent, SentenceType.SENTENCE, sentPos, sentPosInPar));
                }
                sentPos++;
            } else {
                // a list of sentences. Add each of them in a paragraph object and
                for (String sent : sents) {
                    p.addSentence(new Sentence(sent, SentenceType.SENTENCE, sentPos, sentPosInPar));
                    sentPos++;
                    sentPosInPar++;
                }
            }
            p.setPosition(paragraphCount++);
            paragraphs.add(p);
            sentPosInPar = 0;
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
