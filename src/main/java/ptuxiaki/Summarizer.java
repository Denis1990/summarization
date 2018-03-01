package ptuxiaki;


import com.google.common.primitives.Ints;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Paragraph;
import ptuxiaki.datastructures.SentenceType;
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
import static java.lang.Math.round;
import static ptuxiaki.utils.MathUtils.log2;
import static ptuxiaki.utils.MathUtils.log3;
import static ptuxiaki.utils.PropertyKey.*;
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

    /**
     * <p>Calculates the importance of the sentence based on how many title words it has.</p>
     * @param sentence the stemmed sentence.
     * @param titleWords a set of {@link Pair<String,SentenceType>} object. Each pair consists
     *                   of the actual word and an enum denoting if the word was found on a title
     *                   or subtitle of the document.
     * @param tw the size of the title glossary
     * @param mtw the size of the medially titles glossary
     * @return a double
     */
    private double titleKeywords(String sentence, Set<Pair<String, SentenceType>> titleWords, int tw, int mtw) {
        double a = Double.valueOf(properties.getProperty("a", "0.6"));
        double b = Double.valueOf(properties.getProperty("b", "0.4"));
        int tt = 0, mtt = 0;
        for (Pair<String, SentenceType> word : titleWords) {
            if (stemSentence(sentence).contains(word.getKey())) {
                if (word.getValue().equals(SentenceType.TITLE)) {
                    tt++;
                } else if (word.getValue().equals(SentenceType.SUBTITLE)) {
                    mtt++;
                }
            }
        }
        return (a * (log2(tt)/log2(tw))) + (b * (log3(mtt) / log3(mtw)));
    }

    private void summarizeFile(final String filePath, int docId) throws IOException {

        /***********************************Load properties values************************************************/
        int minWords = Integer.parseInt(properties.getProperty(MINIMUN_WORDS));
        double wsl = Double.parseDouble(properties.getProperty(WSL)); // weight sentence location
        double wst = Double.parseDouble(properties.getProperty(WST)); // weight sentence terms
        double wtt = Double.parseDouble(properties.getProperty(WTT)); // weight title terms
        String sw = properties.getProperty(SW).toLowerCase(); // sentence weight function
        double compress = (double) Integer.parseInt(properties.getProperty(COMPRESS)) / 100;

        extractor.setFile(filePath);
        List<String> sentences = extractor.extractSentences();
        List<String> titles = sentences.stream().filter(s -> s.equals(s.toUpperCase())).collect(Collectors.toList());

        // construct the global title dictionary.
        // Each word has an enum description denoting if it is a primary of medially title
        Set<Pair<String, SentenceType>> titleWords = new HashSet<>();
        int j  = 0;
        while (j < titles.size()) {
            for (String word : stemSentence(titles.get(j)).split("\\s+")) {
                titleWords.add(
                        Pair.of(word, (j == 0) ? SentenceType.TITLE : SentenceType.SUBTITLE)
                );
            }
            j++;
        }

        // keep the sentences that have more than minWords words
        sentences = sentences.stream().filter(s -> s.split("\\s+").length > minWords).collect(Collectors.toList());

        // remove sometime
        LOG.debug(String.format("%n===============Sentences found in %s==========================%n", filePath));
        sentences.forEach(s -> LOG.debug(SentenceUtils.removeWhiteSpaces(s)));

        HashMap<String, Integer> termsOccurrences = new HashMap<>();
        if (sw.equals(ISF)) {
            // Compute data for ISF
            Set<String> terms = new HashSet<>();
            for (String s : sentences) {
                terms.addAll(Arrays.asList(stemSentence(s).split("\\s+")));
            }
            terms.forEach(s -> termsOccurrences.put(s, 1));
            for (String t : terms) {
                for (String s : sentences) {
                    if (stemSentence(s).contains(t)) {
                        termsOccurrences.replace(t, termsOccurrences.get(t), termsOccurrences.get(t) + 1);
                    }
                }
            }
        }

        List<Paragraph> paragraphs = extractor.extractParagraphs(sentences.size());

        int size = sentences.size();
        double tt[] = new double[size];
        double sentWeight[] = new double[size];
        double sl[] = new double[size];
        int titleTerms = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.TITLE)).count();
        int mTitleTerms = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.SUBTITLE)).count();
        // in order to avoid calculating log(0)
        if (mTitleTerms == 0) {
            mTitleTerms = 1;
        }

        List<Pair<Double, Integer>> weights = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // use log functions to determine importance
            // see paper B47
            tt[i] = titleKeywords(sentences.get(i), titleWords, titleTerms, mTitleTerms);

            if (sw.equals(IDF)) {
                // tfIdf sentence weight
                sentWeight[i] = indexer.computeSentenceWeight(stemSentence(sentences.get(i)), docId);
            } else if (sw.equals(ISF)) {
                // ISF sentence weight
                for (String word : stemSentence(sentences.get(i)).split("\\s+")) {
                    sentWeight[i] += indexer.tf(word, docId) * log10((double)size / termsOccurrences.getOrDefault(word, 1));
                }
            }
        }

        int sp = paragraphs.size();
        j = 0;
        for (Paragraph par : paragraphs) {
            final int p = par.getPositionInDocument();
            final int sip = par.numberOfSentences();
            for (int i = 0; i < par.numberOfSentences(); i++) {
                final int spip = i;
                sl[j++] = (double)((sp - p + 1) / sp) * ((sip - spip + 1) / sip);
            }

        }

        for (int i = 0; i < size; i++) {
            weights.add(Pair.of(wtt * tt[i] + (wst * sentWeight[i]), i));
        }

        int summarySents = Ints.saturatedCast(size - (round(size * compress)));
        // If the document has too few sentences by default
        // write the 3 most important
        if (summarySents < 3) {
            summarySents = 3;
        }

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
