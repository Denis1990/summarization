package ptuxiaki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptuxiaki.datastructures.Conf;

import java.io.IOException;
import java.nio.file.Paths;

public class App {
    private static Logger LOG = LoggerFactory.getLogger(App.class);
    private static Conf conf;
    public static void main(String[] args) throws IOException {
        String dir = null;
        String properties = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    usage();
                    break;
                case "-wsl":
                    break;
                case "-wst":
                    break;
                case "-wtt":
                    break;
                case "-minWords":
                    break;
                case "-compress":
                    break;
                case "-sw":
                    break;
                case "-pw":
                    break;
                case "-stemmer":
                    break;
                case "-showTitles":
                    break;
            }
            if (args[i].equals("directory") || args[i].equals("dir")) {
                dir = args[i+1];
            } else if (args[i].equals("properties") || args[i].equals("props")) {
                properties = args[i+1];
            }
        }

        if (dir == null) {
            System.out.println("You must pass a directory with the files you need to summarize");
            System.out.println("use: directory /path/to/dir");
            System.exit(0);
        }

        if (properties == null) {
            conf = Conf.instance();
        } else {
            conf = Conf.instance(properties);
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
        System.out.println("\t-wsl=[0..n] sentence location weight.");
        System.out.println("\t-wst=[0..n] sentence term weight");
        System.out.println("\t-wtt=[0..n] title term weight");
        System.out.println("\t-minWords=[0..n]  minimum words a sentence must have in order to be included in the summary");
        System.out.println("\t-compress=[0..n]%   compress ratio of the summary. How small we want the summary to be");
        System.out.println("\t-sw=['idf', 'isf']  sentence weight function");
        System.out.println("\t-pw=['nar', 'bax']  paragraph weight function");
        System.out.println("\t-stemmer=['lucene', 'nnk'] stemmer to use");
        System.out.println("\t-showTitles=[true, false]  whether to show the titles in the summary or not");
        System.out.println();
    }
}
