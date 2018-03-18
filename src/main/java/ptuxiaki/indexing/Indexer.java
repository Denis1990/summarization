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
import ptuxiaki.utils.LuceneConstant;
import stemmer.MyGreekAnalyzer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Math.log10;


public class Indexer {
    public static final String DEFAULT_INDEX_DIR = System.getProperty("user.home") + File.separator + "index";
    public static final String TERM_FREQ_DOC_TFD = "termFreqDoc.tfd";

    // configuration for lucene index
    public final FieldType INDEX_STORED_ANALYZED = new FieldType();

    private String indexDirectory;
    private boolean indexExists;

    // total number of documents in index
    private int docNum = 0;

    // for indexing
    private IndexWriterConfig iwc;
    private IndexWriter index;
    private IndexReader reader;

    private long [] totalTermFreqDoc;
    private boolean totalTermFreqFileExists;

    // for pdf content extraction
    private Metadata metadata;
    private Tika tika;


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
        // TODO maybe this needs to be set dynamically instead of this fixed value
        this.tika.setMaxStringLength(10 * 1024 * 1024);
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

    private void computeSumTermFreqByDoc() {
        this.totalTermFreqDoc = new long[docNum];
        try {
            openReader();
            for (int i = 0; i < docNum; i++) {
                Terms termVector = reader.getTermVector(i, LuceneConstant.CONTENTS);
                TermsEnum itr = termVector.iterator();
                PostingsEnum postings = null;
                while (itr.next() != null) {
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

    private void addDocument(final File file) throws IOException {
        InputStream stream = null;
        try {
            Document doc = new Document();
            stream = new FileInputStream(file);
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
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public Indexer() throws IOException {
        this(DEFAULT_INDEX_DIR, new MyGreekAnalyzer());
    }

    public Indexer(String directory) throws IOException {
        this(directory, new MyGreekAnalyzer());
    }

    public Indexer(final String directory, final Analyzer analyzer) throws IOException {
        this.indexDirectory = directory;
        setUp(analyzer);
        if (indexExists()) {
            restoreFromFile();
            docNum = index.numDocs();
        }
        totalTermFreqFileExists = false;
    }

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

    public double computeSentenceWeight(final String sentence, int docNum) throws IOException {
        double tfIdf = 0;
        for (String w : sentence.split("\\s+")) {
            tfIdf += tf(w, docNum) * idf(w);
        }
        return tfIdf;
    }

    public boolean openReader() throws IOException {
        if (reader == null || reader.getRefCount() <= 0) {
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectory)));
        }
        return reader != null;
    }

    /**
     * @see org.apache.lucene.index.IndexWriter#commit()
     */
    public void commit() throws IOException {
        index.commit();
        docNum = index.numDocs();
        index.close();
        this.indexExists = true;
        if (!totalTermFreqFileExists) {
            computeSumTermFreqByDoc();
        }
    }

    public void closeReader() throws IOException {
        reader.close();
    }

    public int numOfDocs() {
        return this.docNum;
    }

    /**
     * For the demo.
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
                    long docFreq = ctx.reader().docFreq(new Term(LuceneConstant.CONTENTS, text));
                    long tf = tenums.totalTermFreq();
                    System.out.println(String.format("\t%-17s%-13d%-23d", text.utf8ToString(), docFreq, tf));
                }
            }
        }
        closeReader();
    }
}
