package ptuxiaki.datastructures;

import ptuxiaki.utils.PropertyKey;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Global configuration class holding properties
 * that are essential for the execution of the program.
 * The instance of this class is singleton and can be called
 * from any class that wants a particular property.
 */
public class Conf {
    private static final String PROPERTIES_FILENAME="summarizer.properties";
    private static Properties props;
    private static Conf conf;

    private Conf() {
        try {
            props = new Properties();
            props.load(Conf.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));
        } catch (NullPointerException | IOException e) {
            System.err.println("No proper properties file was found.");
            System.err.println("Pass a properties <value> argument on the next execution.");
            System.err.println("Can't load properties for program to run. Exiting");
            System.exit(0);
        }
    }

    private Conf(final String propsFile) {
        try {
            props = new Properties();
            props.load(new FileInputStream(propsFile));
        } catch (NullPointerException | IOException e) {
            System.err.println("No proper properties file was found.");
            System.err.println("Pass a properties <value> argument on the next execution.");
            System.err.println("Can't load properties for program to run. Exiting");
            System.exit(0);
        }
    }

    public static Conf instance() {
        if (conf == null) {
            conf = new Conf();
        }
        return conf;
    }

    public static Conf instance(String propsFile) {
        if (conf == null) {
            conf = new Conf(propsFile);
        }
        return conf;
    }

    /*==========================================================*/

    public int minimumWords() {
        return Integer.parseInt(props.getProperty(PropertyKey.MINIMUN_WORDS));
    }

    public int compressRation() {
        return Integer.parseInt(props.getProperty(PropertyKey.COMPRESS));
    }

    /**
     * Coefficient for wsl
     * @return
     */
    public double sentenceLocationWeight() {
        return Double.parseDouble(props.getProperty(PropertyKey.WSL));
    }

    /**
     * Coefficient for wst
     * @return
     */
    public double sentenceTermsWeight() {
        return Double.parseDouble(props.getProperty(PropertyKey.WST));
    }

    /**
     * Coefficient for tt
     * @return
     */
    public double titleTermsWeight() {
        return Double.parseDouble(props.getProperty(PropertyKey.WTT));
    }

    /**
     * <p>Algorithm to compute sentence weight.</p>
     * Possible values <strong>idf</strong> <strong>isf</strong>
     * @return
     */
    public String sentenceWeight() {
        return props.getProperty(PropertyKey.SW).toLowerCase();
    }

    /**
     * <p>Algorithm to compute paragraph weight.</p>
     * Possible values <strong>nar</strong> <strong>bax</strong>
     * @return
     */
    public String paragraphWeight() {
        return props.getProperty(PropertyKey.PW).toLowerCase();
    }


    public String stemmerClass() {
        return props.getProperty(PropertyKey.STEMMER).toLowerCase();
    }

    public String getOrDefault(final String key, String defaultValue) {
        String prop = props.getProperty(key);
        return prop == null ? defaultValue : prop;
    }

    @Override
    public String toString() {
        if (conf == null) {
            return "{}";
        } else {
            return props.toString();
        }
    }
}
