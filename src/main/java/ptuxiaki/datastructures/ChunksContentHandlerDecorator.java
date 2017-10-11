package ptuxiaki.datastructures;

import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.SAXException;

import java.util.ArrayList;

public class ChunksContentHandlerDecorator extends ContentHandlerDecorator {

    private static final int MAXIMUM_TEXT_CHUNK_SIZE = 1024;

    private ArrayList<String> chunks = new ArrayList<>();

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String lastChunk;

        if (chunks.isEmpty()) {
            chunks.add("");
        }

        lastChunk = chunks.get(chunks.size() - 1);


        String thisStr = new String(ch, start, length);
        if (lastChunk.length() + length > MAXIMUM_TEXT_CHUNK_SIZE) {
            chunks.add(thisStr);
        } else {
            chunks.set(chunks.size() - 1, lastChunk + thisStr);
        }
    }

    @Override
    public String toString() {
        return String.join(" ", chunks);
    }
}
