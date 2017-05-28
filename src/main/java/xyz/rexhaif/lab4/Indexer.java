package xyz.rexhaif.lab4;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Rexhaif on 5/28/2017.
 */
public class Indexer {

    public List<String> words;
    public List<List<Coordinate>> coordinates;
    public List<List<Double>> tfs;
    public List<Double> idfs;
    public List<JSONObject> docs;
    public List<DocId> ids;
    public static class DocId {
        public int book, chapter, part;

        public DocId(int book, int chapter, int part) {
            this.book = book;
            this.chapter = chapter;
            this.part = part;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DocId docId = (DocId) o;

            if (book != docId.book) return false;
            if (chapter != docId.chapter) return false;
            return part == docId.part;
        }

        @Override
        public int hashCode() {
            int result = book;
            result = 31 * result + chapter;
            result = 31 * result + part;
            return result;
        }
    }
    public static class Coordinate {
        public int book, chapter, part, word;

        public Coordinate(int book, int chapter, int part, int word) {
            this.book = book;
            this.chapter = chapter;
            this.part = part;
            this.word = word;
        }
    }

    private static boolean isFromDoc(DocId docId, Coordinate coord) {
        return new DocId(coord.book, coord.chapter, coord.part).equals(docId);
    }

    public Indexer(String filename) {
        JSONObject corpus = new JSONObject();
        try {
            corpus = new JSONObject(new String(
                    Files.readAllBytes(
                            Paths.get(
                                    new File(filename)
                                            .toURI()
                            )
                    ), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        words = new ArrayList<>();
        coordinates = new ArrayList<>();
        tfs = new ArrayList<>();
        idfs = new ArrayList<>();
        docs = new ArrayList<>();
        ids = new ArrayList<>();
        final int[] cBook = {-1};
        final int[] cChapter = {-1};
        final int[] cPart = {-1};
        corpus.getJSONArray("corpus").forEach(
                doc -> {
                    JSONObject obj = (JSONObject) doc;
                    docs.add(obj);
                    cBook[0] = obj.getInt("book");
                    cChapter[0] = obj.getInt("chapter");
                    cPart[0] = obj.getInt("part");
                    ids.add(new DocId(cBook[0], cChapter[0], cPart[0]));
                    final int[] index = {0};
                    obj.getJSONArray("words").forEach(
                            str -> {
                                String word = (String) str;
                                if (words.contains(word)) {
                                    coordinates
                                            .get(
                                                    words.indexOf(str)
                                            )
                                            .add(
                                                    new Coordinate(cBook[0], cChapter[0], cPart[0], index[0])
                                            );
                                } else {
                                    words.add(word);
                                    List<Coordinate> coordinates1 = new ArrayList<>();
                                    coordinates1.add(new Coordinate(cBook[0], cChapter[0], cPart[0], index[0]));
                                    coordinates.add(coordinates1);
                                }
                                index[0]++;
                            }
                    );
                }
        );
        for (int i = 0; i < coordinates.size(); i++) {
            List<Coordinate> coords = coordinates.get(i);
            String word = words.get(i);
            List<Double> cTf = new ArrayList<>();
            for (int j = 0; j < ids.size(); j++) {
                final int[] n_i = {0};
                int finalJ = j;
                coords.forEach(coordinate -> {
                    if (isFromDoc(ids.get(finalJ), coordinate)){
                        n_i[0]++;
                    }
                });
                int numWords = docs.get(j).getJSONArray("words").length();
                cTf.add(((double) n_i[0])/((double) numWords));
            }
            tfs.add(cTf);
        }
        for (int i = 0; i < tfs.size(); i++) {
            List<Double> cTf = tfs.get(i);
            int numOfDocs = docs.size();
            final int[] numOfContainmentDocs = {0};
            cTf.forEach(tf -> {
                if (tf != 0.0) {
                    numOfContainmentDocs[0]++;
                }
            });
            idfs.add(Math.log(
                    (((double) numOfDocs)/((double) numOfContainmentDocs[0]))
            ));
        }
    }

}
