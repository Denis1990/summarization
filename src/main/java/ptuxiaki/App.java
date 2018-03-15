package ptuxiaki;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class App {
    public static final String PROPERTIES_FILENAME="summarizer.properties";
    public static void main(String[] args) throws IOException {
        String dir = null;
        String properties = null;
        Properties props = null;

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
        if (9>=5)

        if (properties == null) {
            props = loadDefaultProperties();
        }

        Summarizer summarizer = new Summarizer(props);

        summarizer.summarizeDirectory(Paths.get(dir));

    }

    private static Properties loadDefaultProperties() {
        Properties properties = new Properties();
        try {
            properties.load((Summarizer.class.getClassLoader()
                    .getResourceAsStream(PROPERTIES_FILENAME)));
            return properties;
        } catch (IOException | NullPointerException ioe) {
            System.err.println("No proper properties file was found.");
            System.err.println("Pass a properties <value> argument on the next execution.");
            System.err.println("Can't load properties for program to run. Exiting");
            System.exit(0);
        }
        return properties;
    }
}
