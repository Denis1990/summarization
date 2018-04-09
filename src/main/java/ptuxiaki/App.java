package ptuxiaki;


import ptuxiaki.datastructures.Conf;

import java.io.IOException;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) throws IOException {
        String dir = null;
        String properties = null;

        for (int i = 0; i < args.length; i++) {
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
            Conf.instance();
        } else {
            Conf.instance(properties);
        }

        Summarizer summarizer = new Summarizer();

        summarizer.summarizeDirectory(Paths.get(dir));

    }
}
