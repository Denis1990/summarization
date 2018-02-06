package ptuxiaki;


import com.google.common.primitives.Ints;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.extraction.TextExtractor;
import ptuxiaki.indexing.Indexer;
import ptuxiaki.utils.SentenceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.log10;
import static ptuxiaki.utils.PropertyKey.*;
import static ptuxiaki.utils.SentenceUtils.keywords;
import static ptuxiaki.utils.SentenceUtils.stemSentence;


public class Summarizer {
    public static final Path SUMMARY_DIR = Paths.get("summaries");

    private static Logger LOG = LoggerFactory.getLogger(Summarizer.class);
    private TextExtractor extractor;
    private Indexer indexer;
    private final Properties properties;

    public Summarizer(final Properties properties) {
        this.properties = properties;
        this.extractor = new TextExtractor();
        if (!Files.exists(SUMMARY_DIR)) {
            try {
                Files.createDirectory(SUMMARY_DIR);
            } catch (IOException e) {
                System.out.println("Cannot create summarization directory. Exiting");
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private void summarizeFile(final String filePath, int docId) throws IOException {
        // get the titles and construct the titles dictionary.
        extractor.setFile(filePath);
        int minWords = Integer.parseInt(properties.getProperty(MINIMUN_WORDS));
        //double wsl = Double.parseDouble(properties.getProperty(WSL)); // weight sentence location
        double wst = Double.parseDouble(properties.getProperty(WST)); // weight sentence terms
        double wtt = Double.parseDouble(properties.getProperty(WTT)); // weight title terms
        String sw = properties.getProperty(SW).toLowerCase(); // sentence weight function
        List<String> sentences = extractor.extractSentences();
        List<String> titles = sentences.stream().filter(s -> s.equals(s.toUpperCase())).collect(Collectors.toList());

        sentences = sentences.stream().filter(s -> s.split(" ").length > minWords).collect(Collectors.toList());

        LOG.debug(String.format("%n===============Sentences found in %s==========================%n", filePath));
        sentences.forEach(s -> LOG.debug(SentenceUtils.removeWhiteSpaces(s)));

        HashMap<String, Integer> termsOccurrences = new HashMap<>();
        if (sw.equals(ISF)) {
            // Compute data for ISF
            String[] terms = Arrays.stream(
                                sentences.stream()
                                .map(SentenceUtils::stemSentence)
                                .collect(Collectors.joining(" "))
                                .split(" ")
                             ).filter(s -> s.length() > 3)
                              .distinct()
                              .collect(Collectors.joining(" "))
                              .split(" ");
            Arrays.stream(terms).forEach(s -> termsOccurrences.put(s, 1));
            for (String t : terms) {
                for (String s : sentences) {
                    s = stemSentence(s);
                    if (s.contains(t)) {
                        termsOccurrences.replace(t, termsOccurrences.get(t), termsOccurrences.get(t) + 1);
                    }
                }
            }
        }

        // the first sentence in the list is the title of the document
        // stem the sentence first and then split it to words with String#split
        Set<String> titleWords = new HashSet<>(Arrays.asList(stemSentence(sentences.get(0)).split(" ")));

        //List<Paragraph> paragraphs = extractor.extractParagraphs(sentences.size());

        int size = sentences.size();
        long tt[] = new long[size];
        double sentWeight[] = new double[size];
        //double sl[] = new double[size];

        List<Pair<Double, Integer>> weights = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // use log functions to determine importance
            // see paper B47
            // TODO: impement keyword for each title and subtitle.
            tt[i] = keywords(sentences.get(i), titleWords);
            // this is a problem. 0 indicates the number of the document, in the order it was indexed
            // what is that order i don't know. Maybe alphabetical, maybe by type or size.
            // The point is i need a way to link the indexed document with an integer because the tf() method needs one
            // to reference the document.
            // FIXME: critical it is
            if (sw.equals(IDF)) {
                // tfIdf sentence weight
                sentWeight[i] = indexer.computeSentenceWeight(stemSentence(sentences.get(i)), docId);
            } else if (sw.equals(ISF)) {
                // ISF sentence weight
                for (String word : stemSentence(sentences.get(i)).split(" ")) {
                    sentWeight[i] += indexer.tf(word, docId) * log10((double)size / termsOccurrences.getOrDefault(word, 1));
                }
            }
        }

//        int sp = paragraphs.size();
//        int j = 0;
//        for (Paragraph par : paragraphs) {
//            final int p = par.getSerialNo();
//            final int sip = par.numberOfSentences();
//            for (Paragraph.Sentence s : par.getSentences()) {
//                final int spip = s.position;
//                sl[j++] = ((sp - p + 1) / sp) * ((sip - spip + 1) / sip);
//            }
//
//        }

        for (int i = 0; i < size; i++) {
            weights.add(Pair.of((wtt * tt[i]) + (wst * sentWeight[i]), i));
        }

        // this should have been loaded above
        double compress = (double) Integer.parseInt(properties.getProperty(COMPRESS)) / 100;
        int summarySents = Ints.saturatedCast(size - (Math.round(size * compress)));

        weights.sort(Comparator.reverseOrder());
        int begin = filePath.lastIndexOf(File.separatorChar);
        int end = filePath.lastIndexOf(".");
        String summaryFileName = filePath.substring(++begin, end).concat("_summary");
        try(FileOutputStream fos = new FileOutputStream(SUMMARY_DIR.toString() + File.separatorChar + summaryFileName)) {
            for (int i = 0; i < summarySents; i++) {
                fos.write(sentences.get(weights.get(i).getValue())
                        .concat(System.lineSeparator())
                        .getBytes(Charset.forName("UTF-8"))
                );
            }
        }
        System.out.println("New summary saved to: " + summaryFileName);
    }

    public void summarizeDirectory(final Path dir) throws IOException {
        this.indexer = new Indexer();

        // there is a problem here. If we want to summarize another directory we can't index those new files
        // because of the way the indexer was implemented. We need to delete the index directory gather all
        // the files we want to summarize in a directory pass that directory to indexer and then do the summary.

        if (!indexer.indexExists()) {
            indexer.indexDirectory(dir.toString());
        }

        try {
            int docId = 0;
            for (File f : dir.toFile().listFiles()) {
                try {
                    summarizeFile(f.toString(), docId++);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException npe) {
            System.out.println("Empty directory " + dir);
            npe.printStackTrace();
        }
    }
}
