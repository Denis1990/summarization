package ptuxiaki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Conf;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class App {
    private static Logger LOG = LoggerFactory.getLogger(App.class);
    private static Conf conf;

    public static void main(String[] args) throws IOException {
        String dir = null, props = null;
        // Initialize program's properties to default values.
        double wsl = 1.0, wst = 1.0, wtt = 1.0;
        int minWords = 4, compress = 80;
        String sw = "idf", pw = "nar", stemmer = "lucene";
        boolean showTitles = true;

        // Parse cli args. Take care to validate the values as well as the options passed to the program.
        // Terminate with exit code 2 for wrong option (i.e '-wtl 0.4' instead of '-wtt 0.4' )
        // Terminate with exit code 3 for invalid option value (i.e '-stemmer muahaha' instead of '-stemmer nnk')

        int i = 0;
        while (i < args.length) {
            try {
                switch (args[i]) {
                    case "-h":
                        usage();
                        return;
                    case "-wsl":
                        wsl = Double.parseDouble(args[++i]);
                        break;
                    case "-wst":
                        wst = Double.parseDouble(args[++i]);
                        break;
                    case "-wtt":
                        wtt = Double.parseDouble(args[++i]);
                        break;
                    case "-minWords":
                        minWords = Integer.parseInt(args[++i]);
                        break;
                    case "-compress":
                        compress = Integer.parseInt(args[++i]);
                        break;
                    case "-sw":
                        sw = args[++i];
                        break;
                    case "-pw":
                        pw = args[++i];
                        break;
                    case "-stemmer":
                        stemmer = args[++i];
                        break;
                    case "-showTitles":
                        showTitles = Boolean.parseBoolean(args[++i]);
                        break;
                    case "-dir":
                        dir = args[++i];
                        break;
                    case "-props":
                        props = args[++i];
                        break;
                    default:
                        System.out.println("Invalid option " + args[i]);
                        System.out.println("Try 'summarizer -h' for more information");
                        System.exit(3);
                        break;
                }
                i++;
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid value " + args[i-2] + " for option " + args[i--]);
                System.exit(2);
            }
        }

        if (!sw.equals("idf") && !sw.equals("isf")) {
            System.out.println("Invalid value " + sw + " for option -sw");
            System.out.println("Try 'summarizer -h' for more information");
            System.exit(2);
        }

        if (!pw.equals("bax") && !pw.equals("nar")) {
            System.out.println("Invalid value " + pw + " for option -pw");
            System.out.println("Try 'summarizer -h' for more information");
            System.exit(2);
        }

        if (!stemmer.equals("lucene") && !stemmer.equals("nnk")) {
            System.out.println("Invalid value " + stemmer + " for option -stemmer");
            System.out.println("Try 'summarizer -h' for more information");
            System.exit(2);
        }

        // End args parsing section

        if (dir == null) {
            System.out.println("You must pass a directory with the files you need to summarize");
            System.out.println("use: directory /path/to/dir");
            System.exit(0);
        }

        if (props == null) {
            Properties p = new Properties();
            p.put("wsl", String.valueOf(wsl));
            p.put("wst", String.valueOf(wst));
            p.put("wtt", String.valueOf(wtt));
            p.put("minimumWords", String.valueOf(minWords));
            p.put("compress", String.valueOf(compress));
            p.put("sw", sw);
            p.put("pw", pw);
            p.put("stemmer", stemmer);
            p.put("showTitles", String.valueOf(showTitles));
            conf = Conf.instance(p);
        } else {
            conf = Conf.instance(props);
        }

        Summarizer summarizer = new Summarizer();

        LOG.info(String.format("Running with properties: %s", conf));
        LOG.debug(String.format("Running with properties: %s", conf));

        summarizer.summarizeDirectory(Paths.get(dir));

        LOG.info("-------------------------------------End of run------------------------------------------%n");
        LOG.debug("-------------------------------------End of run------------------------------------------%n");
    }


    public static void usage() {
        System.out.println("Usage: summarizer [<flags>] <directory> [<property-file>]");
        System.out.println();
        System.out.println("\tPerform summarization of documents in the specified directory");
        System.out.println("\tThe generated summaries are stored in a folder names summaries in your working directory.");
        System.out.println();
        System.out.println("Flags: ");
        System.out.println("\t-wsl [0..n] sentence location weight.");
        System.out.println("\t-wst [0..n] sentence term weight");
        System.out.println("\t-wtt [0..n] title term weight");
        System.out.println("\t-minWords [0..n]  minimum words a sentence must have in order to be included in the summary");
        System.out.println("\t-compress [0..n]%   compress ratio of the summary. How small we want the summary to be");
        System.out.println("\t-sw ['idf', 'isf']  sentence weight function");
        System.out.println("\t-pw ['nar', 'bax']  paragraph weight function");
        System.out.println("\t-stemmer ['lucene', 'nnk'] stemmer to use");
        System.out.println("\t-showTitles [true, false]  whether to show the titles in the summary or not");
        System.out.println();
    }
}
