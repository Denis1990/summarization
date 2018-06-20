package ptuxiaki;


import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Conf;
import ptuxiaki.datastructures.Paragraph;
import ptuxiaki.datastructures.Sentence;
import ptuxiaki.datastructures.SentenceType;
import ptuxiaki.extraction.TextExtractor;
import ptuxiaki.indexing.Indexer;

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
import static ptuxiaki.utils.MathUtils.log2p;
import static ptuxiaki.utils.MathUtils.log3;
import static ptuxiaki.utils.PropertyKey.*;


public class Summarizer {
    public static final Path SUMMARY_DIR = Paths.get("summaries");

    private static Logger LOG = LoggerFactory.getLogger(Summarizer.class);
    private TextExtractor extractor;
    private Indexer indexer;

    public Summarizer() {
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
    private double titleKeywords(Sentence sentence, Set<Pair<String, SentenceType>> titleWords, int tw, int mtw) {
        double a = Double.valueOf(Conf.getOrDefault("a", "0.6"));
        double b = Double.valueOf(Conf.getOrDefault("b", "0.4"));
        int tt = 0, mtt = 0;
        for (Pair<String, SentenceType> word : titleWords) {
            if (sentence.getStemmedTermsAsList().contains(word.getKey())) {
                if (word.getValue().equals(SentenceType.TITLE)) {
                    tt++;
                } else if (word.getValue().equals(SentenceType.SUBTITLE)) {
                    mtt++;
                }
            }
        }
        if (tt == 0 && mtt == 0) return 0;
        sentence.setTitleTerm((a * (log2p(tt)/log2p(tw))) + (b * (log3(mtt) / log3(mtw))));
        return (a * (log2p(tt)/log2p(tw))) + (b * (log3(mtt) / log3(mtw)));
    }

    private void summarizeFile(final String filePath) throws IOException {
        /***********************************Load properties values************************************************/
        int minWords = Conf.minimumWords();
        double wsl = Conf.sentenceLocationWeight(); // weight sentence location
        double wst = Conf.sentenceTermsWeight(); // weight sentence terms
        double wtt = Conf.titleTermsWeight(); // weight title terms
        String sw = Conf.sentenceWeight(); // sentence weight function
        String pw = Conf.paragraphWeight(); // sentence location weight function
        double compress = Conf.compressRation() / 100.0;

        int begin = filePath.lastIndexOf(File.separatorChar) + 1;
        String fileName = filePath.substring(begin);
        extractor.setFile(filePath);

        List<Paragraph> paragraphs = extractor.extractParagraphs();

        int size = paragraphs.stream().map(Paragraph::getAllSentences).mapToInt(Collection::size).sum();

        // remove sentences that have less than minWords
        for (Paragraph p : paragraphs) {
            p.removeSentencesWithLessThan(minWords);
        }

        List<Sentence> sentences = new ArrayList<>();
        for (Paragraph p : paragraphs) {
            sentences.addAll(p.getAllSentences());
        }

        List<Sentence> titles = sentences.stream().filter(s -> s.isSubTitle() || s.isTitle()).collect(Collectors.toList());

        for (Paragraph p : paragraphs) {
            LOG.info(p.toString());
        }

        // Construct the global title dictionary.
        // Constructing a Set of Pair objec. Each Pair object  is a <word, enum> denoting if the word
        // is from a title or from a medially title. The word is stored as a stem of the original
        Set<Pair<String, SentenceType>> titleWords = new HashSet<>();
        for (Sentence s : titles) {
            for (String str : s.getStemmedTerms()) {
                titleWords.add(Pair.of(str, s.isTitle() ? SentenceType.TITLE : SentenceType.SUBTITLE));
            }
        }

        // if isf algorithm is picked for sentence weight, then we need to count
        // how many times a term is present in each sentence.
        HashMap<String, Integer> termsOccurrences = new HashMap<>();
        if (sw.equals(ISF)) {
            // Compute data for ISF
            Set<String> terms = new HashSet<>();
            sentences.forEach(s -> terms.addAll(s.getStemmedTermsAsList()));

            // set initial occurrences to 1
            terms.forEach(s -> termsOccurrences.put(s, 1));
            for (String t : terms) {
                for (Sentence s : sentences) {
                    if (s.getStemmedTermsAsList().contains(t)) {
                        termsOccurrences.replace(t, termsOccurrences.get(t), termsOccurrences.get(t) + 1);
                    }
                }
            }
        }

        double tt[] = new double[size];
        double sentWeight[] = new double[size];
        double sl[] = new double[size];


        int titleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.TITLE)).count();
        int mTitleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.SUBTITLE)).count();
        // in order to avoid calculating log(0)
        if (mTitleTermsCount == 0) {
            mTitleTermsCount = 1;
        }

        LOG.info(String.format("========%s========", fileName));

        // the list that holds the weight of each sentence.
        // The double value is the the weight while and the int value
        // is the index in the list of sentences
        List<Pair<Double, Integer>> weights = new ArrayList<>();

        for (Sentence s : sentences) {
            final int i = s.getPosition();
            /** Calculate Title Term weight */
            // use log functions to determine importance see paper B47
            tt[i] = titleKeywords(s, titleWords, titleTermsCount, mTitleTermsCount);

            /**Calculate sentence weight based on IDF or ISF */
            if (sw.equals(IDF)) {
                // tfIdf sentence weight
                sentWeight[i] = indexer.assignSentenceWeight(s, fileName);
                LOG.info(String.format("sentence: %s tt: %f", s.getStemmedTermsAsList(), tt[i]));
            } else if (sw.equals(ISF)) {
                // ISF sentence weight
                for (String word : sentences.get(i).getStemmedTermsAsList()) {
                    final double tfVal = indexer.tf(word, fileName);
                    final double isfVal = log10((double)size / termsOccurrences.getOrDefault(word, 1));
                    LOG.info(String.format("\tword: %s tf: %f isf: %f", word, tfVal, isfVal));
                    sentWeight[i] += tfVal * isfVal;
                }
                s.setWeight(sentWeight[i]);
                LOG.info(String.format("sentence: %s tfIsf: %f tt: %f", sentences.get(i), sentWeight[i], tt[i]));
            }
        }

        /** Calculate sentence weight based on paragraphs */
        if (pw.equals(BAX)) {
            // baxendales algorithm
            for (Paragraph p : paragraphs) {
                // get the first sentence
                final Sentence s = p.getFirstSentence();
                if (s.isTitle() || s.isSubTitle()) continue;
                final int idx = s.getPosition();
                sentWeight[idx] += sentWeight[idx] * 0.85;
                s.setSentLocationWeight(sentWeight[idx] * 0.85);
            }
        } else if (pw.equals(NAR)) {
            // news article algorithm
            int sp = paragraphs.size();
            int j = 0;
            for (Paragraph par : paragraphs) {
                final int p = par.getPositionInDocument();
                final int sip = par.numberOfSentences();
                for (int k = 0;  k < sip && k < size; k++) {
                    final int spip = k + 1; // we don't want 0 based indexing for sentence location in paragraph
                    if (j < size) {
                        sl[j++] = ((double) (sp - p + 1) / sp) * ((double) (sip - spip + 1) / sip);
                        sentences.get(j-1).setSentLocationWeight(sl[j-1]);
                    }
                }
            }
        }

        System.out.println("File: " + fileName);
        for (Sentence s : sentences) {
            assert tt[s.getPosition()] == s.getTitleTerm() : "tt Difference in " + s.getPosition();
            assert sentWeight[s.getPosition()] == s.getWeight() : "Diference in " + s.getPosition();
            if (pw.equals(NAR)) {
                assert sl[s.getPosition()] == s.getSentLocationWeight() : "sl difference in " + s.getPosition();
            }
        }

        /** Calculate combined weights value */
        // a * tt + b * st + c * sl
        for (int i = 0; i < size; i++) {
            //TODO: Add sl into the equation
            double w = 0.0;
            if (pw.equals(NAR)) {
                w = (wtt * tt[i]) + (wst * sentWeight[i]) + (wsl * sl[i]);
            } else {
                w = (wtt * tt[i]) + (wst * sentWeight[i]);
            }
            weights.add(Pair.of(w, i));
        }

        /** Calcuate the number of sentences we will keep based on compress ratio */
        int summarySents = (int)(size - (round(size * compress)));
        // If the document has too few sentences by default
        // write the 3 most important
        if (summarySents < 3 && size > 3) {
            summarySents = 3;
        }


        // sort the list based on the weight of each sentence
        weights.sort(Comparator.reverseOrder());
        // keep the indices of the most relevant sentences and then sort them
        List<Integer> selSentIdx = weights.stream().map(Pair::getValue).collect(Collectors.toList()).subList(0, summarySents);
        selSentIdx.sort(Comparator.naturalOrder());
        String summaryFileName = fileName.concat("_summary");
        try(FileOutputStream fos = new FileOutputStream(SUMMARY_DIR.toString() + File.separatorChar + summaryFileName)) {
            for (Integer i : selSentIdx) {
                fos.write(sentences.get(i)
                        .getText()
                        .trim()
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
            for (File f : dir.toFile().listFiles()) {
                try {
                    summarizeFile(f.toString());
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
