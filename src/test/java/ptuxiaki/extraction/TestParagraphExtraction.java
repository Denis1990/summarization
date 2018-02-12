package ptuxiaki.extraction;

import org.junit.Before;
import org.junit.Test;
import ptuxiaki.datastructures.Paragraph;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestParagraphExtraction {

    private TextExtractor extractor;
    private String [] files;


    @Before
    public void init() {
        this.files = new String[] {
                "athina.pdf", "dias.txt", "dionusos.docx"
        };
        this.extractor = new TextExtractor();
    }

    @Test
    public void testParagraphsInAthina() {
        extractor.setFile(this.getClass().getClassLoader().getResource("ptuxiaki/extraction/athina.pdf").getFile());
        // 9 sentences in the document
        List<Paragraph> paragraphList = extractor.extractParagraphs(9);
        assertEquals(paragraphList.size(), 3);
        assertEquals(paragraphList.get(0).numberOfSentences(), 4);
        assertEquals(
                paragraphList.get(0).getIthSentence(0),
                "Η Αθηνά, κατά την Ελληνική μυθολογία, ήταν η θεά της σοφίας, της στρατηγικής και του πολέμου."
        );
    }

}
