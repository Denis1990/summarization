package ptuxiaki;


import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Conf;
import ptuxiaki.datastructures.Paragraph;
import ptuxiaki.datastructures.Sentence;
import ptuxiaki.datastructures.SentenceType;
import ptuxiaki.extraction.TextExtractor;
import ptuxiaki.indexing.Indexer;
import ptuxiaki.utils.PropertyKey;

import java.io.File;
import java.io.FileFilter;
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
    public Conf conf;

    private static Logger LOG = LoggerFactory.getLogger(Summarizer.class);
    private TextExtractor extractor;
    private Indexer indexer;

    public Summarizer() {
        this.conf = Conf.instance();
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
        double a = Double.valueOf(conf.getOrDefault("a", "0.6"));
        double b = Double.valueOf(conf.getOrDefault("b", "0.4"));
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
        return (a * (log2p(tt)/log2p(tw))) + (b * (log3(mtt) / log3(mtw)));
    }

    private void summarizeFile(final String filePath) throws IOException {
        /***********************************Load properties values************************************************/
        int minWords = conf.minimumWords();
        double wsl = conf.sentenceLocationWeight(); // weight sentence location coefficient
        double wst = conf.sentenceTermsWeight(); // weight sentence terms coefficient
        double wtt = conf.titleTermsWeight(); // weight title terms coefficient
        String sw = conf.sentenceWeight(); // sentence weight function
        String pw = conf.paragraphWeight(); // sentence location weight function
        double compress = conf.compressRation() / 100.0;

        int begin = filePath.lastIndexOf(File.separatorChar) + 1;
        String fileName = filePath.substring(begin);
        extractor.setFile(filePath);

        List<Paragraph> paragraphs = extractor.extractParagraphs();

        int size = paragraphs.stream().map(Paragraph::getAllSentences).mapToInt(Collection::size).sum();

        List<Sentence> sentences = new ArrayList<>();
        for (Paragraph p : paragraphs) {
            sentences.addAll(p.getAllSentences());
        }

        // Construct the global title dictionary.
        // Constructing a Set of Pair objects. Each Pair object  is a <word, enum> denoting if the word
        // is from a title or from a medially title. The word is stored as a stem of the original
        Set<Pair<String, SentenceType>> titleWords = new HashSet<>();
        sentences.stream().filter(s -> s.isSubTitle() || s.isTitle()).collect(Collectors.toList()).forEach(s -> {
            for (String str : s.getStemmedTerms()) {
                titleWords.add(Pair.of(str, s.isTitle() ? SentenceType.TITLE : SentenceType.SUBTITLE));
            }
        });

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

        int titleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.TITLE)).count();
        int mTitleTermsCount = (int)titleWords.stream().filter(p -> p.getValue().equals(SentenceType.SUBTITLE)).count();
        // in order to avoid calculating log(0)
        if (mTitleTermsCount == 0) {
            mTitleTermsCount = 1;
        }

        if (titleTermsCount == 0) {
            titleTermsCount = 1;
        }

        LOG.info(String.format("========%s========", fileName));

        for (Sentence s : sentences) {
            if (s.isSubTitle() || s.isTitle()) continue;
            if (s.hasLessThanNWords(minWords)) continue; // ignore sentence with less than minWords
            final int i = s.getPosition();
            /** Calculate Title Term weight */
            // use log functions to determine importance see paper B47
            s.setTitleTermWeight(
                    titleKeywords(s, titleWords, titleTermsCount, mTitleTermsCount)
            );

            /** Calculate sentence weight based on IDF or ISF */
            if (sw.equals(IDF)) {
                // tfIdf sentence weight
                indexer.assignSentenceWeight(s, fileName);
                LOG.info(String.format("sentence: %s tt: %f", s.getStemmedTermsAsList(), s.getTitleTermWeight()));
            } else if (sw.equals(ISF)) {
                double sum = 0;
                // ISF sentence weight
                for (String word : sentences.get(i).getStemmedTermsAsList()) {
                    final double tfVal = indexer.tf(word, fileName);
                    final double isfVal = log10((double)size / termsOccurrences.getOrDefault(word, 1));
                    LOG.info(String.format("\tword: %s tf: %f isf: %f", word, tfVal, isfVal));
                    sum += tfVal * isfVal;
                }
                s.setTermsWeight(sum);
                LOG.info(String.format("sentence: %s tfIsf: %f tt: %f", sentences.get(i), s.getTermsWeight(), s.getTitleTermWeight()));
            }
        }

        /** Calculate sentence weight based on location inside the paragraph */
        if (pw.equals(BAX)) {
            // baxendale algorithm
            for (Sentence s : sentences) {
                if (s.hasLessThanNWords(minWords)) continue;
                if (s.isFirstInParagraph() && !(s.isTitle() || s.isSubTitle())) {
                    s.updateTermsWeight(wsl);
                }
            }
        } else if (pw.equals(NAR)) {
            // news article algorithm
            int sp = paragraphs.size();
            int j = 0;
            for (Paragraph par : paragraphs) {
                final int p = par.getPositionInDocument();
                final int sip = par.numberOfSentences();
                for (int spip = 0; spip < sip; spip++) {
                    if(par.getFirstSentence().isSubTitle() || par.getFirstSentence().isTitle()) {
                        // update j though in order to not loose the global order of the sentences list
                        j++;
                        continue;
                    }

                    if (sentences.get(j).hasLessThanNWords(minWords)) {
                        j++;
                        continue;
                    }
                    sentences.get(j).setSLWeight(((double) (sp - p) / sp) * ((double) (sip - spip) / sip));
                    j++;
                }
            }
        }

        /** Calculate combined weights value */
        sentences.forEach(s -> s.compositeWeight(wtt, wst, wsl));

        System.out.println(String.format("\t======================%s======================%n", fileName));
        System.out.println("Extracted sentences: \n");
        paragraphs.forEach(System.out::println);

        /** Sort based on that */
        sentences.sort(Comparator.reverseOrder());

        /** Calculate the number of sentences we will keep based on compress ratio */
        int summarySents = (int)(size - (round(size * compress)));
        // If the document has too few sentences by default
        // write the 3 most important
        if (summarySents < 3 && size > 3) {
            summarySents = 3;
        }

        // take a sublist of the sentences list and sort it by sentence position. this way we have the most
        // relevant sentences and we can show them in the order they appear in the original document
        List<Sentence> selectedSentences = sentences.subList(0, summarySents);
        selectedSentences.sort(Comparator.comparingInt(Sentence::getPosition));

        boolean showTitles = Boolean.valueOf(conf.getOrDefault(PropertyKey.SHOWTITLES, "true"));
        if (showTitles) {
            // merge selectedSentences with titles collection
            selectedSentences = merge(selectedSentences, sentences);
        }
        String summaryFileName = fileName.concat("_summary");
        try(FileOutputStream fos = new FileOutputStream(SUMMARY_DIR.toString() + File.separatorChar + summaryFileName)) {
            for (Sentence s : selectedSentences) {
                fos.write(s.getText()
                        .trim()
                        .concat(System.lineSeparator())
                        .getBytes(Charset.forName("UTF-8"))
                );
            }
        }
        System.out.println("New summary saved to: " + summaryFileName);
        System.out.println();
    }

    /**
     * Merge the two titles and sentences the Sentence position property the lower positions come first.
     * @param sentences
     * @param titles
     * @return
     */
    private List<Sentence> merge(List<Sentence> sentences, List<Sentence> titles) {
        List<Sentence> merged = new ArrayList<>();
        merged.addAll(sentences);
        merged.addAll(titles);
        merged.sort(Comparator.comparingInt(Sentence::getPosition));
        return merged;
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
                    if (!f.isHidden()) continue;
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
