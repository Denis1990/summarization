package ptuxiaki.extraction;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestTextExtraction {

    private String [] sentences;
    private TextExtractor extractor;

    @Before
    public void sampleSentences() {
        sentences = new String[] {
                "Σε καθεστωσ ειδικησ διαχειρισησ ο ΔΟΛ",
                "Διασφαλιζεται η λειτουργια των εντυπων και των ιστοσελιδων του Οργανισμου",
                "Τη λειτουργια του Δημοσιογραφικου Οργανισμου Λαμπρακη ΔΟΛ και την εκδοση των ιστορικων εντυπων «ΤΑ ΝΕΑ» και «ΤΟ ΒΗΜΑ» διασφαλιζει με την αποφαση του το Πρωτοδικειο τησ Αθηνασ Τμημα Εκουσιασ Δικαιοδοσιασ που θετει τον Οργανισμο σε «ειδικη διαχειριση εν λειτουργια».",
                "Με τη δικαστικη αποφαση οριζεται ωσ ειδικοσ διαχειριστησ τησ ανωνυμησ εταιρειασ με την επωνυμια «Δημοσιογραφικοσ Οργανισμοσ Λαμπρακη» ΔΟΛ η εταιρεια ορκωτων ελεγκτων και συμβουλων επιχειρησεων Grant Thorton και η δικηγορικη εταιρεια Ανδρεασ Αγγελιδησ.",
                "Συμφωνα με πληροφοριεσ στο σκεπτικο τησ πολυσελιδησ αποφασησ τησ η δικαστησ Νεκταρια Ξελυσσακτη η οποια εκδικασε την αιτηση των τραπεζων για υπαγωγη του ΔΟΛ σε καθεστωσ ειδικησ διαχειρισησ αποτιμωντασ ολα τα στοιχεια του φακελου εκρινε οτι",
                "Η διατηρηση σε λειτουργια τησ επιχειρησησ του ΔΟΛ μετα την υπαγωγη τησ στη διαδικασια τησ ειδικησ διαχειρισησ δεν αποτελει διακριτικη ευχερεια αλλα υποχρεωση του ειδικου διαχειριστη",
                "Η χρηματοδοτηση των εξοδων υπαγωγησ του ΔΟΛ στην διαδικασια τησ διαχειρισησ εν λειτουργια αποτελει εργο του διαχειριστη μετα το διορισμο του ενω σε περιπτωση που υπαρχει αδυναμια ανευρεσησ των αναγκαιων κεφαλαιων για τη διατηρηση τησ Επιχειρησησ σε λειτουργια και για την καλυψη των λοιπων εξοδων συντρεχει περιπτωση ανακλησησ τησ αποφασησ που διατασσει την υπαγωγη τησ σε καθεστωσ ειδικησ διαχειρισησ εν λειτουργια",
                "Αναλυτικα η αποφαση",
                "Η υπ. αριθμον 963/2017 Αποφαση του Μονομελουσ Πρωτοδικειου Αθηνων του Τμηματοσ Εκουσιασ Διαδικασιασ προεδροσ κα. Νεκταρια Ξελυσσακτη αποδεχεται το αιτημα διορισμου ειδικου διαχειριστη του ν. 4307/2014 στον ΔΟΛ.",
                "Οριζει ωσ ειδικο διαχειριστη για τον ΔΟΛ την Grant Thornton σε συμπραξη με τη Δικηγορικη Εταιρεια Ανδρεα Αγγελιδη και Συνεργατων",
                "Η Αποφαση ομωσ στο αιτιολογικο τησ αναφερει πολλα και πολυ πιο ενδιαφεροντα απο την απλη αποδοχη του αιτηματοσ διορισμου ειδικου διαχειριστη",
                "Η Αποφαση με πληθοσ παραπομπων στη νομικη θεωρια αναλυει το ευροσ των καθηκοντων και των εξουσιων του ειδικου διαχειριστη",
                "Συμφωνα με την Αποφαση –και αυτο ειναι το σημαντικοτερο στοιχειο τησ - ο ειδικοσ διαχειριστησ δεν εχει απλωσ την ευχερεια να διατηρησει την επιχειρηση σε λειτουργια μεχρι να ολοκληρωσει τασ καθηκοντα του υποχρεουται να την διατηρησει σε λειτουργια",
                "Ο τροποσ με τον οποιο θα καταφερει ο ειδικοσ διαχειριστησ να επιτυχει τη συνεχιση τησ λειτουργιασ δεν μπορει να προκαθοριστει απο το Δικαστηριο και εναποκειται στην επιχειρηματικη κριση του ειδικου διαχειριστη",
                "Αν ομωσ ο ειδικοσ διαχειριστησ διαπιστωσει οτι δεν μπορει να επιτυχει τη συνεχιση τησ λειτουργιασ τησ επιχειρησησ τοτε δεν μπορει πλεον να συνεχισει τα καθηκοντα του και θα πρεπει να υποβληθει αιτημα ανακλησησ του",
                "Αυτο που περιγραφει σαφωσ η Αποφαση ειναι οτι οσο ο ειδικοσ διαχειριστησ καταφερνει να εξασφαλισει τη συνεχιση τησ λειτουργιασ παραμενει στη θεση του ενω οταν η συνεχιση τησ λειτουργιασ δεν μπορει να εξασφαλιστει ο διαχειριστησ ανακαλειται και απομενει μονο η διαδικασια τησ πτωχευσησ ωσ εναλλακτικη για το μελλον",
                "Η Αποφαση αναφερει οτι δεν μπορει να διαταξει τον ειδικο διαχειριστη να χρησιμοποιει συγκεκριμενα ποσα απο συγκεκριμενεσ πηγεσ εσοδων τησ επιχειρησησ για να επιτυχει τη συνεχιση τησ λειτουργιασ τησ",
                "Ειναι ελευθεροσ ο διαχειριστησ να επιλεξει τον πλεον προσφορο τροπο για να το επιτυχει",
                "Πιθανωσ μεσω χρησησ των εσοδων τησ επιχειρησησ μεσω παροχησ προσθετησ χρηματοδοτησησ η με οποιον αλλο τροπο κρινει επωφελεστερο αρκει να διατηρηθει η επιχειρηση σε λειτουργια.",
                "Μεσω ομωσ του αιτιολογικου τησ Αποφασησ ανοιγεται σαφησ και ξεκαθαρη η νομικη οδοσ που θα μπορει να ακολουθησει ο ειδικοσ διαχειριστησ για τη συνεχιση τησ λειτουργια του ΔΟΛ μεσω τησ χρησιμοποιησησ των εσοδων απο διαφημισεισ και απο τισ εισπραξεισ απο την πωληση των εντυπων και στο μελλον",
                "Τα υφισταμενα νομικα εμποδια για τη χρηση αυτων των ποσων ηταν δυο:",
                "Αφενοσ μεν οτι ο τραπεζικοσ λογαριασμοσ στον οποιο κατατιθεντο οι εισπραξεισ απο τισ πωλησεισ των εφημεριων και τισ διαφημισεισ ειχε συμφωνηθει να παραμενει ‘δεσμευμενοσ’ για οσο διαστημα οι τραπεζεσ ειχαν ληξιπροθεσμεσ απαιτησεισ κατα του ΔΟΛ",
                "Η συμφωνια περι δεσμευμενου λογαριασμου αναφεροταν στισ συμβασεισ ομολογιακων δανειων του ΔΟΛ ωσ ενοχικη υποχρεωση δεν ειχε ομωσ υπογραφει ειδικη εμπραγματη συμφωνια περι ενεχυρασεωσ του τραπεζικου λογαριασμου",
                "Το δευτερο εμποδιο ηταν οτι ολεσ οι υφισταμενεσ και μελλοντικεσ απαιτησεισ του ΔΟΛ απο τισ πωλησεισ των εντυπων του απαιτησεισ του ΔΟΛ κατα τησ εταιρειασ ΑΡΓΟΣ Α.Ε.που αναλαμβανει τη διανομη των εντυπων και τισ σχετικεσ εισπραξεισ ολεσ οι απαιτησεισ αυτεσ ειχαν προεκχωρηθει υπερ των τραπεζων για οσο διαστημα υφιστανται εκκρεμεισ οφειλεσ του ΔΟΛ κατα των τραπεζων",
                "Η Αποφαση απαντα εμμεσωσ πλην σαφωσ επ’ αυτων και φαινεται να λυνει τα σχετικα εμποδια.",
                "Η Αποφαση αναφερει ρητα οτι ο διορισμοσ του ειδικου διαχειριστη επιφερει την αναστολη των ατομικων διωξεων ολων των πιστωτων τησ επιχειρησησ",
                "Κατα την Αποφαση η αναστολη αυτη των ατομικων διωξεων δεν ταυτιζεται με την αντιστοιχη εννοια του γενικου Πτωχευτικου Δικαιου ουτε υποκειται στισ αιρεσεισ και περιορισμουσ που εχει η αντιστοιχη εννοια στον Πτωχευτικο Κωδικα",
                "Η αναστολη των ατομικων διωξεων του ν. 4307/2014 αφορα ολουσ τουσ πιστωτεσ του ΔΟΛ ειτε αυτοι εχουν εμπραγματη εξασφαλιση ειτε οχι",
                "Έτσι η Αποφαση οριζει οτι αναστελλεται η δυνατοτητα ληψεωσ μετρων αναγκαστικησ εκτελεσησ τοσο απο πιστωτεσ του ΔΟΛ που δεν εχουν εμπραγματεσ εξασφαλισεισ οσο και απο τισ πιστωτριεσ τραπεζεσ που διαθετουν εμπραγματεσ εξασφαλισεισ",
                "Επισησ συναγεται οτι η αναστολη των ατομικων διωξεων επιφερει και την αναστολη εκτελεσησ των οποιων εννομων συνεπειων εχει η συμφωνια περι τηρησησ ενοσ τραπεζικου λογαριασμου ωσ ‘δεσμευμενου’ ανεξαρτητωσ απο το αν αυτη η συμφωνια ειχε μονον ενοχικο η ακομα και εμπραγματο χαρακτηρα",
                "Σε αλλο σημειο τησ Αποφασησ οριζεται οτι ο διορισμοσ του ειδικου διαχειριστη εχει ωσ συνεπεια την απωλεια τησ εξουσιασ διαθεσησ οποιουδηποτε περιουσιακου στοιχειου τησ επιχειρησησ απο την τωρινη τησ διοικηση και αρα εχει τισ συνεπειεσ τησ πτωχευτικησ απαλλοτριωσησ",
                "Στο πλαισιο τησ ‘κλασσικησ’ πτωχευσησ γινεται δεκτο οτι λογω τησ πτωχευτικησ απαλλοτριωσησ απωλεια τησ εξουσιασ διαθεσησ του οφειλετη παυουν να εχουν ενεργεια ολεσ οι συναφθεισεσ εκχωρησεισ μελλοντικων απαιτησεων",
                "Καθε νεα απαιτηση που γενναται και εισπραττεται μετα την κηρυξη τησ πτωχευσησ εδω μετα το διορισμο του ειδικου διαχειριστη δεν ανηκει στισ πιστωτριεσ τραπεζεσ αλλα ανηκει στην πτωχευτικη περιουσια",
                "Σε εφαρμογη τησ ιδιασ αυτησ αρχησ με τον διορισμο του ειδικου διαχειριστη ολεσ οι μελλοντικεσ απαιτησεισ του ΔΟΛ κατα τησ εταιρειασ ΑΡΓΟΣ που διανεμει τα εντυπα του ΔΟΛ και εισπραττει το σχετικο τιμημα εντασσονται στην περιουσια που διοικει ο ειδικοσ διαχειριστησ και οχι στην περιουσια των τραπεζων που ειχαν συστησει ενεχυρο των απαιτησεων αυτων",
                "Απο το διορισμο του ειδικου διαχειριστη και εξησ οι απαιτησεισ του ΔΟΛ κατα τησ ΑΡΓΟΣ δεν δεσμευονταν απο το βαροσ του ενεχυρου που ειχε συμφωνηθει προ του διορισμου του ειδικου διαχειριστη",
                "Και αυτο οχι μονο λογω τησ αναστολησ των ατομικων διωξεων αλλα και λογω τησ πτωχευτικησ απαλλοτριωσησ",
                "Επομενωσ η Αποφαση παρεχει στο ειδικο διαχειριστη πληρεσ νομικο οπλοστασιο προκειμενου ο τελευταιοσ να επιδιωξει την εισπραξη καθε ποσου που κατατιθεται στουσ τραπεζικουσ λογαριασμουσ του ΔΟΛ και την χρηση των ποσων αυτων για τη συνεχιση τησ λειτουργιασ τησ επιχειρησησ",
                "Αν δεν το κανει η δεν βρει αλλο τροπο να χρηματοδοτηθει η λειτουργια τησ επιχειρησησ λ.χ. νεοσ δανεισμοσ τοτε δεν μπορει να συνεχισει τακαθηκοντα του και θα πρεπει να ζητηθει η ανακληση του διορισμου του"
        };
        extractor = new TextExtractor();
    }


    private void testNumberOfSentencesExtracted() {
        Assert.assertEquals(extractor.extractSentences().size(), sentences.length);
    }

    private void compareLists(List<String> list1, List<String> list2) {
        int index = 0;
        // compare element by element
        testNumberOfSentencesExtracted();
        for (int i = 0; i < list1.size(); i++) {
            Assert.assertEquals(list1.get(i), list2.get(i));
        }
    }

    @Test
    public void testSentencesAreTheSame() {
        extractor.setFile("/home/denis/Documents/bachelor_thesis/alpha_testing/KathestwsDOL.pdf");
        compareLists(extractor.extractSentences(), Arrays.asList(sentences));
    }


}
