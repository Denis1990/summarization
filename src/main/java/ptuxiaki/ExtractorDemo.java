package ptuxiaki;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;
import ptuxiaki.extraction.TextExtractor;
import ptuxiaki.utils.SentenceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractorDemo {

    public static void main(String[] args) throws IOException, TikaException, SAXException {
        TextExtractor extractor = new TextExtractor();
        for (String arg : args) {
            System.out.println("=====" + arg + "======");
            extractor.setFile(arg);
            List<String> s = new ArrayList<>(); //extractor.extractSentences();
            int j = 0;
            for (String i : s) {
                System.out.println(String.format("%d sentence: %s", ++j, SentenceUtils.stemSentence(i)));
            }
        }
    }
}
