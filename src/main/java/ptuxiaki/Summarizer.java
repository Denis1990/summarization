package ptuxiaki;


import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Conf;
import ptuxiaki.datastructures.Paragraph;
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
import static ptuxiaki.utils.SentenceUtils.stemSentence;


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
    private double titleKeywords(String sentence, Set<Pair<String, SentenceType>> titleWords, int tw, int mtw) {
        double a = Double.valueOf(Conf.getOrDefault("a", "0.6"));
        double b = Double.valueOf(Conf.getOrDefault("b", "0.4"));
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
        if (tt == 0 && mtt == 0) return 0;
        return (a * (log2p(tt)/log2p(tw))) + (b * (log3(mtt) / log3(mtw)));
    }

    private void summarizeFile(final String filePath, int docId) throws IOException {
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
        List<String> sentences = extractor.extractSentences();
        List<String> titles = sentences.stream().filter(s -> s.equals(s.toUpperCase())).collect(Collectors.toList());
        List<Paragraph> pars = extractor.extractParagraphs();
//
//        // construct the global title dictionary.
//        // Each sentence has an enum description denoting if it is a primary of medially title
//        Set<Pair<String, SentenceType>> titleWords = new HashSet<>();
//        int j  = 0;
//        while (j < titles.size()) {
//            for (String word : stemSentence(titles.get(j)).split("\\s+")) {
//                titleWords.add(
//                        Pair.of(word, (j == 0) ? SentenceType.TITLE : SentenceType.SUBTITLE)
//                );
//            }
//            j++;
//        }
//
//        // keep the sentences that have more than minWords words
//        sentences = sentences.stream().filter(s -> s.split("\\s+").length > minWords).collect(Collectors.toList());
//
//        HashMap<String, Integer> termsOccurrences = new HashMap<>();
//        if (sw.equals(ISF)) {
//            // Compute data for ISF
//            Set<String> terms = new HashSet<>();
//            for (String s : sentences) {
//                terms.addAll(Arrays.asList(stemSentence(s).split("\\s+")));
//            }
//            // set initial occurrences to 1
//            terms.forEach(s -> termsOccurrences.put(s, 1));
//            for (String t : terms) {
//                for (String s : sentences) {
//                    if (stemSentence(s).contains(t)) {
//                        termsOccurrences.replace(t, termsOccurrences.get(t), termsOccurrences.get(t) + 1);
//                    }
//                }
//            }
//        }
//
//        int size = sentences.size();
//        double tt[] = new double[size];
//        double sentWeight[] = new double[size];
//        double sl[] = new double[size];
//
//        int titleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.TITLE)).count();
//        int mTitleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.SUBTITLE)).count();
//        // in order to avoid calculating log(0)
//        if (mTitleTermsCount == 0) {
//            mTitleTermsCount = 1;
//        }
//
//        List<Paragraph> paragraphs = extractor.extractParagraphs(size);
//
//        long sentencesFromPar = paragraphs.stream().mapToInt(Paragraph::numberOfSentences).sum();
//
//        assert sentencesFromPar == size : String.format("%s does not have identical number of sentences", fileName);
//
//        // the list that holds the weight of each sentence.
//        // The double value is the the weight while the int value
//        // is the index in the list of sentences
//        List<Pair<Double, Integer>> weights = new ArrayList<>();
//        LOG.info(String.format("========%s========", fileName));
//        for (int i = 0; i < size; i++) {
//            /** Calculate Title Term weight */
//            // use log functions to determine importance see paper B47
//            tt[i] = titleKeywords(sentences.get(i), titleWords, titleTermsCount, mTitleTermsCount);
//
//            /**Calculate sentence weight based on IDF or ISF */
//            if (sw.equals(IDF)) {
//                // tfIdf sentence weight
//                sentWeight[i] = indexer.computeSentenceWeight(stemSentence(sentences.get(i)), fileName);
//                LOG.info(String.format("sentence: %s tt: %f", stemSentence(sentences.get(i)), tt[i]));
//            } else if (sw.equals(ISF)) {
//                // ISF sentence weight
//                for (String word : stemSentence(sentences.get(i)).split("\\s+")) {
//                    final double tfVal = indexer.tf(word, fileName);
//                    final double isfVal = log10((double)size / termsOccurrences.getOrDefault(word, 1));
//                    LOG.info(String.format("\tword: %s tf: %f isf: %f", word, tfVal, isfVal));
//                    sentWeight[i] += tfVal * isfVal;
//                }
//                LOG.info(String.format("sentence: %s tfIsf: %f tt: %f", stemSentence(sentences.get(i)), sentWeight[i], tt[i]));
//            }
//        }
//
//        /** Calculate sentence weight based on paragraphs */
//        if (pw.equals(BAX)) {
//            // baxendales algorithm
//            for (Paragraph p : paragraphs) {
//                // get the first sentence
//                Triple<String, Integer, Integer> s = p.getSentenceTriplet(0);
//                int idx = s.getRight();
//                sentWeight[idx] += sentWeight[idx] * 0.85;
//            }
//        } else if (pw.equals(NAR)) {
//            // news article algorithm
//            int totalNumOfSentences = paragraphs.stream().mapToInt(Paragraph::numberOfSentences).sum();
//            int sp = paragraphs.size();
//            j = 0;
//            for (Paragraph par : paragraphs) {
//                final int p = par.getPositionInDocument();
//                final int sip = par.numberOfSentences();
//                for (int k = 0;  k < sip && k < size; k++) {
//                    final int spip = k + 1; // we don't want 0 based indexing for sentence location in paragraph
//                    if (j < totalNumOfSentences) {
//                        sl[j++] = ((double) (sp - p + 1) / sp) * ((double) (sip - spip + 1) / sip);
//                    }
//                }
//            }
//        }
//
//        /** Calculate combined weights value */
//        // a * tt + b * st + c * sl
//        for (int i = 0; i < size; i++) {
//            //TODO: Add sl into the equation
//            double w = 0.0;
//            if (pw.equals(NAR)) {
//                w = (wtt * tt[i]) + (wst * sentWeight[i]) + (wsl * sl[i]);
//            } else {
//                w = (wtt * tt[i]) + (wst * sentWeight[i]);
//            }
//            weights.add(Pair.of(w, i));
//        }
//
//        /** Calcuate the number of sentences we will keep based on compress ratio */
//        int summarySents = (int)(size - (round(size * compress)));
//        // If the document has too few sentences by default
//        // write the 3 most important
//        if (summarySents < 3 && size > 3) {
//            summarySents = 3;
//        }
//
//        weights.sort(Comparator.reverseOrder());
//        // keep the indices of the most relevant sentences and then sort them
//        List<Integer> selSentIdx = weights.stream().map(Pair::getValue).collect(Collectors.toList()).subList(0, summarySents);
//        selSentIdx.sort(Comparator.naturalOrder());
//        String summaryFileName = fileName.concat("_summary");
//        try(FileOutputStream fos = new FileOutputStream(SUMMARY_DIR.toString() + File.separatorChar + summaryFileName)) {
//            for (int i = 0; i < summarySents; i++) {
//                fos.write(sentences.get(selSentIdx.get(i))
//                        .trim()
//                        .concat(System.lineSeparator())
//                        .getBytes(Charset.forName("UTF-8"))
//                );
//            }
//        }
//        System.out.println("New summary saved to: " + summaryFileName);
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
