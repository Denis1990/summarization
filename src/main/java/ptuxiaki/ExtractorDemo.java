package ptuxiaki;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;
import ptuxiaki.extraction.TextExtractor;

import java.io.IOException;
import java.util.List;

public class ExtractorDemo {

    public static void main(String[] args) throws IOException, TikaException, SAXException {
        //FileOutputStream fos = new FileOutputStream(new File("sentences_found.txt"));
        TextExtractor extractor = new TextExtractor();
        List<String> s = extractor.extractSentencesFromFile(args[0]);
        int j = 0;
        for (String i : s) {
            System.out.println(String.format("%d sentence: %s", ++j, i.replaceAll("\\s", " ")));
        }
    }
}
