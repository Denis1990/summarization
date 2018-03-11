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
                "ptuxiaki/extraction/athina.pdf", "ptuxiaki/extraction/dias.txt", "ptuxiaki/extraction/dionusos.docx"
        };
        this.extractor = new TextExtractor();
    }

    @Test
    public void testParagraphsInAthina() {
        extractor.setFile(this.getClass().getClassLoader().getResource(this.files[0]).getFile());
        // 9 sentences in the document
        List<Paragraph> paragraphList = extractor.extractParagraphs(9);
        assertEquals(paragraphList.size(), 2);
        assertEquals(paragraphList.get(0).numberOfSentences(), 4);
        assertEquals(
                paragraphList.get(0).getIthSentence(0),
                "Η Αθηνά, κατά την Ελληνική μυθολογία, ήταν η θεά της σοφίας, της στρατηγικής και του πολέμου."
        );
    }
    @Test
    public void testParagraphsInDias() {
        extractor.setFile(this.getClass().getClassLoader().getResource(this.files[1]).getFile());
        // 12 sentences in the document
        List<Paragraph> paragraphList = extractor.extractParagraphs(12);
        assertEquals(paragraphList.size(), 1);
        assertEquals(paragraphList.get(0).numberOfSentences(), 12);
        assertEquals(
                paragraphList.get(0).getIthSentence(0),
                "Ο Δίας ή Ζεύς σύμφωνα με την αρχαία ελληνική θεογονία είναι ο «Πατέρας των θεών και των ανθρώπων», που κυβερνά τους Θεούς του Ολύμπου."
        );
    }

    @Test
    public void testParagraphInDionusos() {
        extractor.setFile(this.getClass().getClassLoader().getResource(this.files[2]).getFile());
        List<Paragraph> paragraphs = extractor.extractParagraphs(10);
        assertEquals(paragraphs.size(), 3);
        assertEquals(paragraphs.get(0).numberOfSentences(), 5);
        assertEquals(paragraphs.get(0).getFirstSentence(),
                "Ο Διόνυσος, επίσης Διώνυσος, γιος του θεού Δία, ανήκει στις ελάσσονες πλην όμως σημαντικές θεότητες του αρχαιοελληνικού πανθέου, καθώς η λατρεία του επηρέασε σημαντικά τα θρησκευτικά δρώμενα της ελλαδικής επικράτειας.");
    }
}
