package ptuxiaki.indexing;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.Summarizer;
import ptuxiaki.datastructures.Conf;
import ptuxiaki.datastructures.Sentence;
import ptuxiaki.utils.LuceneConstant;
import ptuxiaki.utils.PropertyKey;
import stemmer.MyGreekAnalyzer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Math.log10;


/**
 * <p>The class responsible for indexing a collection of documents, using Apache Lucene library.
 * <p>The class takes each file from the directory of document to index and it constructs a Lucene
 * {@link Document} object consisting of four fields
 * <ul>
 *     <li>UUID</li>
 *     <li>FILE_NAME</li>
 *     <li>FILE_PATH</li>
 *     <li>CONTENT</li>
 * </ul>
 * and then adds that object to index. The content extraction is achieved using apache Tika library.
 *
 * <p>Besides indexing the documents it also provides method to acquire the values stored in the
 * index, such as tf and idf weights.
 *
 * <p>The index files generated is saved to a predefined directory which is $HOME/index, unless specified
 * otherwise.
 */
public class Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(Summarizer.class);

    private static final String DEFAULT_INDEX_DIR = System.getProperty("user.home") + File.separator + "index";
    private static final String TERM_FREQ_DOC_TFD = "termFreqDoc.tfd";

    // configuration for lucene index
    private final FieldType INDEX_STORED_ANALYZED = new FieldType();

    private String indexDirectory;
    private boolean indexExists;

    // total number of documents in index
    private int docNum = 0;

    // for indexing
    /**
     * Configuration for indexWriter
     */
    private IndexWriterConfig iwc;
    /**
     * The actual 'index' object where the information are stored
     */
    private IndexWriter index;
    /**
     * Used to read and retrieve information from a lucene index
     */
    private IndexReader reader;

    /**
     * For each document hold the sum of all its term frequency
     */
    private long [] totalTermFreqDoc;

    /**
     *
     */
    private boolean totalTermFreqFileExists;

    // for pdf content extraction
    private Metadata metadata;
    private Tika tika;


    /**
     * This method is used to initialize and configure the indexWriter object.
     * @param analyzer {@link GreekAnalyzer} if we are using lucene stemmer {@link MyGreekAnalyzer} otherwise
     * @throws IOException
     */
    private void setUp(Analyzer analyzer) throws IOException {
        INDEX_STORED_ANALYZED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        INDEX_STORED_ANALYZED.setStored(true);
        INDEX_STORED_ANALYZED.setTokenized(true);
        INDEX_STORED_ANALYZED.setStoreTermVectors(true);
        // lock the configuration
        INDEX_STORED_ANALYZED.freeze();

        this.iwc = new IndexWriterConfig(analyzer);
        if (!Files.exists(Paths.get(indexDirectory), LinkOption.NOFOLLOW_LINKS)) {
            this.iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            this.indexExists = false;
        } else {
            this.iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            this.indexExists = true;
        }

        this.index = new IndexWriter(FSDirectory.open(Paths.get(indexDirectory)), iwc);

        this.metadata = new Metadata();
        this.tika = new Tika();
    }

    /**
     * Saves the {@code totalTermFreq} array to disk using java serialization API.
     * @throws IOException
     */
    private void saveToFile() throws IOException {
        ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream(indexDirectory + File.separator + TERM_FREQ_DOC_TFD));
        objStream.writeObject(totalTermFreqDoc);
        objStream.close();
        totalTermFreqFileExists = true;
    }

    /**
     * Restore the {@code totalTermFreq} from the disk.
     * This method is called only when the index directory exists.
     * @throws IOException
     */
    private void restoreFromFile() throws IOException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(indexDirectory + File.separator + TERM_FREQ_DOC_TFD))) {
            totalTermFreqDoc = (long []) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        totalTermFreqFileExists = true;
    }

    /**
     * Due to how nnkstemmer works it also does some word filtering.
     * It works as an filter as well.
     * This doesn't get along with Lucene which has the two concepts separate.
     * First is the filtering through the use of stopword.txt files
     * and then the stemming of the remaining words which is done through
     * language specific stemmers.
     * As a result printStatistics reports a total term frequency lower than
     * what Lucene is using internally.
     *
     */
    private void computeSumTermFreqByDoc() {
        this.totalTermFreqDoc = new long[docNum];
        try {
            openReader();
            BytesRef txt;
            for (int i = 0; i < docNum; i++) {
                Terms termVector = reader.getTermVector(i, LuceneConstant.CONTENTS);
                TermsEnum itr = termVector.iterator();
                PostingsEnum postings = null;
                while ((txt = itr.next()) != null) {
                    if (txt.length == 0) continue;
                    postings = itr.postings(postings, PostingsEnum.FREQS);
                    postings.nextDoc();
                    totalTermFreqDoc[i] += postings.freq();
                }
            }
            closeReader();
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Construct a {@link Document} object from the file content, name and path add it to index
     * @param file A file in natural language
     */
    private void addDocument(final File file) {
        Document doc = new Document();
        try (InputStream stream = new FileInputStream(file)) {
            doc.add(new StringField(LuceneConstant.DOC_ID, UUID.randomUUID().toString(), Field.Store.NO));
            doc.add(new Field(LuceneConstant.CONTENTS, tika.parseToString(stream, metadata), INDEX_STORED_ANALYZED));
            doc.add(new Field(LuceneConstant.FILE_PATH, file.getPath(), INDEX_STORED_ANALYZED));
            doc.add(new Field(LuceneConstant.FILE_NAME, file.getName(), INDEX_STORED_ANALYZED));
            if (iwc.getOpenMode().equals(IndexWriterConfig.OpenMode.CREATE)) {
                index.addDocument(doc);
                System.out.printf("\tIndexing file %s%n", file.getName());
            } else {
                index.updateDocument(new Term("uuid"), doc);
                System.out.printf("\tUpdating file %s%n", file.getName());
            }
        } catch (IOException | TikaException e) {
            e.printStackTrace();
        }
    }

    private boolean openReader() throws IOException {
        if (reader == null || reader.getRefCount() <= 0) {
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectory)));
        }
        return reader != null;
    }

    /**
     * Default constructor. Use the default index directory to save index files
     * @throws IOException
     */
    public Indexer() throws IOException {
        this(DEFAULT_INDEX_DIR);
    }

    /**
     * Set the directory where the index files will be saved. Initialize and
     * the indexer based on the stemmer we are using
     * @param directory
     * @throws IOException
     */
    public Indexer(final String directory) throws IOException {
        this.indexDirectory = directory;
        if (Conf.instance().stemmerClass().equals(PropertyKey.NNKSTEMER)) {
            setUp(new MyGreekAnalyzer());
        } else {
            setUp(new GreekAnalyzer());
        }
        if (indexExists()) {
            restoreFromFile();
            docNum = index.numDocs();
        }
        totalTermFreqFileExists = false;
    }

    /**
     * <p>Do the actual indexing.
     * <p>Traverse the specified directory and for each file call {@link Indexer#addDocument(File)}.
     * Avoid indexing hidden files or any other non text files such as .lock and .class files
     *
     * <p>After this method is called and returns successfully all the documents that were
     * indexed and their information are stored inside the index files and can be retrieved by
     * the program.
     * @param directory where the document for indexing reside
     * @return true if the indexing of the directory was successful.
     * @throws IOException
     */
    public boolean indexDirectory(String directory) throws IOException {
        File dir = new File(directory);
        System.out.printf("Indexing directory %s%n", dir.getName());
        try {
            // dir.listFiles may throw NullPointerException
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    indexDirectory(f.getPath());
                    continue;
                }

                // don't index hidden files for example .directory files on kde dolphin
                // or files that are not pdf or txt
                if (f.isHidden() || f.getName().endsWith("lock") || f.getName().endsWith("class")) {
                    continue;
                }
                addDocument(f);
            }
            index.commit();
            this.indexExists = true;
            System.out.printf("Indexing of %s directory completed!%n", dir.getName());
        } catch (NullPointerException | IOException e) {
            // the directory we opened has no files for indexing
            e.printStackTrace();
            this.indexExists = false;
            return false;
        }
        docNum = index.numDocs();
        index.close();
        computeSumTermFreqByDoc();
        return true;
    }

    public boolean indexExists() {
        return indexExists;
    }

    /**
     * Compute the frequency of a term in the given document.
     * The calculation is performed according to equation (3)
     * presented <a href="file:///home/denis/Documents/bachelor_thesis/papers/B33.pdf">here</a>
     * @param term the stemmed word
     * @param docId the document to search in
     * @return
     */
    public double tf(final String term, final int docId) {
        int freq = 0;
        try {
            openReader();
            Terms termVector = reader.getTermVector(docId, LuceneConstant.CONTENTS);
            TermsEnum itr = termVector.iterator();
            PostingsEnum postings = null;
            BytesRef termText = itr.next();
            while (termText != null) {
                if (term.equals(termText.utf8ToString())) {
                    postings = itr.postings(postings, PostingsEnum.FREQS);
                    postings.nextDoc();
                    freq = postings.freq();
                }
                termText = itr.next();
            }
            closeReader();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (double) freq / totalTermFreqDoc[docId];
    }

    /**
     * Compute the frequency of a term in the given document.
     * The calculation is performed according to equation (3)
     * presented <a href="file:///home/denis/Documents/bachelor_thesis/papers/B33.pdf">here</a>
     *
     * @param term the stemmed word
     * @param fileName the document file name to search in
     * @return
     */
    public double tf(final String term, final String fileName) {
        long freq = 0;
        int termDoc = 0;
        try {
            openReader();
            boolean done = false;
            // the docId the term was found on
            termDoc = -1;
            for (int doc = 0; doc < reader.numDocs() && !done; doc++) {
                final String docName = reader.document(doc).get(LuceneConstant.FILE_NAME);
                if (fileName.equals(docName)) {
                    Terms terms = reader.getTermVector(doc, LuceneConstant.CONTENTS);
                    TermsEnum itr = terms.iterator();
                    BytesRef text = null;
                    while ((text = itr.next()) != null) {
                        if (text.utf8ToString().equals(term)) {
                            // the term frequency of the term in the document
                            freq = itr.totalTermFreq();
                            done = true;
                            termDoc = doc;
                            break;
                        }
                    }
                }
            }
            closeReader();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (termDoc == -1) {
            return (double) freq;
        }
        return (double) freq / totalTermFreqDoc[termDoc];
    }

    /**
     * Compute the inverse documentfrequency of a term.
     * The calculation is performed according to equation (5)
     * presented <a href="file:///home/denis/Documents/bachelor_thesis/papers/B33.pdf">here</a>
     * @param term
     * @return
     */
    public double idf(final String term) {
        int docFreq = 0;
        try {
            openReader();
            docFreq = reader.docFreq(new Term(LuceneConstant.CONTENTS, term));
            closeReader();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return log10((double) docNum / (docFreq + 1));
    }

    /**
     * Calculate the tfIdf value for a sentence object.
     * @param sentence the sentence to update
     * @param file the document where the sentence exists
     * @return
     */
    public double assignSentenceWeight(final Sentence sentence, String file)  {
        double tfIdf = 0;
        LOG.info(String.format("sentence: %s tfIdf: %f", sentence, tfIdf));
        for (String w : sentence.getStemmedTermsAsList()) {
            final double tfVal = tf(w, file);
            final double idfVal = idf(w);
            tfIdf += tfVal * idfVal;
            LOG.info(String.format("\tword: %s tf: %f idf: %f", w, tfVal, idfVal));
        }
        sentence.setTermsWeight(tfIdf);
        return tfIdf;
    }

    public void closeReader() throws IOException {
        reader.close();
    }

    /**
     * Print statistics about the index.
     * For each document show each stemmed word contained in it
     * and the document frequency it has as well as the term frequency.
     * <p>If no indexing has taken place this method won't show something
     */
    public void printStatistics() throws IOException {
        if (!indexExists) {
            return;
        }

        openReader();
        List<LeafReaderContext> leaves = reader.leaves();
        int docs[] = new int[reader.numDocs()];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = i;
        }
        // term --> totalDocFreq, totalTf
        HashMap<String, Pair<Long, Long>> termsTfs = new HashMap<>();
        System.out.println("============ STATISTICS PER DOCUMENT ===========");
        for (int doc : docs) {
            System.out.print("\n\nDocument: ");
            for (LeafReaderContext ctx : leaves) {
                LeafReader atmReader = ctx.reader();
                FieldInfo info = atmReader.getFieldInfos().fieldInfo(LuceneConstant.CONTENTS);
                if (info == null || !info.hasVectors()) continue;

                Document storedData = atmReader.document(doc, new HashSet<>(Arrays.asList(LuceneConstant.CONTENTS, LuceneConstant.FILE_NAME)));
                Terms terms = atmReader.getTermVector(doc, LuceneConstant.CONTENTS);


                if (storedData == null) continue;

                String fileName = storedData.get(LuceneConstant.FILE_NAME);

                TermsEnum tenums = terms.iterator();

                BytesRef text = null;
                restoreFromFile();
                System.out.println(fileName);
                System.out.println(String.format("\t%-16s %-12s %-5s", "Stemmed", "DocFreq", "DocTf"));
                while ((text = tenums.next()) != null) {
                    if (text.length <= 0) continue;
                    long docFreq = ctx.reader().docFreq(new Term(LuceneConstant.CONTENTS, text));
                    long tf = tenums.totalTermFreq();
                    System.out.println(String.format("\t%-17s%-13d%-23d", text.utf8ToString(), docFreq, tf));
                    if (termsTfs.containsKey(text.utf8ToString())) {
                        termsTfs.put(text.utf8ToString(),
                                     Pair.of(termsTfs.get(text.utf8ToString()).getLeft(),termsTfs.get(text.utf8ToString()).getRight() + tf)
                        );
                    } else {
                        termsTfs.put(text.utf8ToString(), Pair.of(docFreq, tf));
                    }
                }
            }
        }
        System.out.println("=====================================\n\n\n");
        System.out.println("=========AGGREGATE STATISTICS===========\n");
        System.out.println(String.format("%-16s %-12s %-5s", "Stem", "Documents", "Total term freq"));
        for (Map.Entry<String, Pair<Long, Long>> e : termsTfs.entrySet()) {
            System.out.println(String.format("%-17s%-13d%-23d", e.getKey(), e.getValue().getLeft(), e.getValue().getRight()));
        }
        System.out.println("=====================================");
        closeReader();
    }
}
