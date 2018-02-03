package ptuxiaki;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;
import ptuxiaki.extraction.TextExtractor;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

public class ExtractorDemo {

    public static void main(String[] args) throws IOException, TikaException, SAXException {
        TextExtractor extractor = new TextExtractor();
        for (String arg : args) {
            System.out.println("=====" + arg + "======");
            List<String> s = extractor.extractSentencesFromFile(arg);
            int j = 0;
            for (String i : s) {
                System.out.println(String.format("%d sentence: %s", ++j, i.replaceAll("\\s", " ")));
            }
        }
    }
}
