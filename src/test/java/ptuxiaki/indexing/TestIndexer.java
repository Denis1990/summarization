package ptuxiaki.indexing;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestIndexer {
    private Indexer indexer;
    private String [] fileNames = {"test_file1.txt", "test_file2.txt", "test_file3.txt"};

    @Before
    public void indexDocuments() {
        try {
            indexer = new Indexer(System.getProperty("user.home") + File.separator + "test_index");
            if (indexer.indexExists()) return;
            String path = TestIndexer.class.getClassLoader().getResource("ptuxiaki/indexing").getPath();
            indexer.indexDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.print("\n[ERROR] No documents were indexed.\n");
        }
    }

    @Test
    public void testTfAndIdf() {
        double [] tfs = new double[fileNames.length];
        // word βολτ exists in one documents, 1 time
        // doc1 has 10 terms so the tf should be 0,1
        for (int i = 0; i < fileNames.length; i++) {
            tfs[i] = indexer.tf("κακοσ", fileNames[i]);
        }
        Assert.assertArrayEquals(tfs, new double [] { 0.1, 0, 0 }, 0.01);
        // word βολτ exists in two documents, 1 time in each
        // doc1 has 10 terms and doc2 has 7 terms
        // so the tf should be 0,1 0,14 respectively
        for (int i = 0; i < fileNames.length; i++) {
            tfs[i] = indexer.tf("βολτ", fileNames[i]);
        }
        Assert.assertArrayEquals(tfs, new double [] { 0.1, 0.14, 0 }, 0.01);

        double [] idfs = new double[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            idfs[i] = indexer.idf("βολτ");
        }
    }
}
