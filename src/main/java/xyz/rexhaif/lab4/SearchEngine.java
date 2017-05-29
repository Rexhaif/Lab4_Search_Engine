package xyz.rexhaif.lab4;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rexhaif on 5/28/2017.
 */
public class SearchEngine {

    public static final int RESULTS_PER_QUERY = 10;
    public static JSONArray originalDocs = new JSONArray();

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("RX-Search engine v1.0");
        System.out.println("----------------------------------");
        System.out.println("Building Indices....");
        long p1 = System.nanoTime();
        Indexer indexer = new Indexer("book34_stemmed.json");
        long p2 = System.nanoTime();
        System.out.println("Indices builded in " + (p2 - p1)/1000000000.0d + " seconds");
        System.out.println("----------------------------------");
        System.out.println("Unique words: " + indexer.words.size());
        System.out.println("Total woords with coordinates: " + indexer.coordinates.size());
        System.out.println("Total words with tf coefficient: " + indexer.tfs.size());
        System.out.println("Total idfs coefficients: " + indexer.idfs.size());
        System.out.println("Total documents in corpus: " + indexer.docs.size());
        System.out.println("----------------------------------");

        JSONObject corpus = new JSONObject();
        try {
            corpus = new JSONObject(new String(
                    Files.readAllBytes(
                            Paths.get(
                                    new File(Formatter.OUTPUT_FILE_NAME)
                                            .toURI()
                            )
                    ), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        corpus.getJSONArray("corpus").forEach((doc) -> {
                            JSONObject obj = (JSONObject) doc;
                            List<String> words = new ArrayList<>();
                            words.addAll(Arrays.asList(obj.getString("text").split("[\\s+]")));
                            obj.remove("text");
                            obj.put("words", new JSONArray(words));
                            originalDocs.put(obj);
                }
        );

        while (true) {
            System.out.println("Enter search query or type exit to exit");
            System.out.print("/-> ");
            String query = in.nextLine();
            if (query.equalsIgnoreCase("exit")) break;
            System.out.println("Searching....");
            search(indexer, query).forEach(System.out::println);
        }
    }

    public static List<String> search(Indexer indexer, String query) {
        String[] queryWords = query.split("[\\s+]");
        String[] stemmedQuery = new String[queryWords.length];
        for (int i = 0; i < queryWords.length; ++i) {
            stemmedQuery[i] = Stemmer.stem(queryWords[i]);
            if (!indexer.words.contains(stemmedQuery[i])) {
                return Collections.singletonList("Some parts of query not indexed");
            }
        }
        List<List<Indexer.Coordinate>> queryCoordinates = new ArrayList<>();
        List<List<Double>> queryTFs = new ArrayList<>();
        List<Double> queryIDFs = new ArrayList<>();
        for (String queryWord : stemmedQuery) {
            queryCoordinates.add(indexer.coordinates.get(indexer.words.indexOf(queryWord)));
            queryTFs.add(indexer.tfs.get(indexer.words.indexOf(queryWord)));
            queryIDFs.add(indexer.idfs.get(indexer.words.indexOf(queryWord)));
        }
        Map<Indexer.DocId, List<Integer>> wordsPerDocument = new HashMap<>();
        Map<Indexer.DocId, List<Integer>> impactsPerDocument = new HashMap<>();
        queryCoordinates.forEach(coords -> coords.forEach(coordinate -> {
            Indexer.DocId docId = new Indexer.DocId(coordinate.book, coordinate.chapter, coordinate.part);
            if (impactsPerDocument.containsKey(docId)) {
                impactsPerDocument.get(docId).add(coordinate.word);
            } else {
                List<Integer> impacts = new ArrayList<>();
                impacts.add(coordinate.word);
                impactsPerDocument.put(docId, impacts);
            }
            if (wordsPerDocument.containsKey(docId)) {
                wordsPerDocument.get(docId).add(queryCoordinates.indexOf(coords));
            } else {
                List<Integer> words = new ArrayList<>();
                words.add(queryCoordinates.indexOf(coords));
                wordsPerDocument.put(docId, words);
            }
        }));

        Map<Indexer.DocId, Double> tf_idfPerDocument = new HashMap<>();
        wordsPerDocument.forEach((docId, wordIdxs) -> {
            int docIdx = indexer.ids.indexOf(docId);
            Double tf_idfSum = 0.0;
            for (int idx : wordIdxs) {
                double tf = queryTFs.get(idx).get(docIdx);
                double idf = queryIDFs.get(idx);
                tf_idfSum += (tf * idf);
            }
            tf_idfPerDocument.put(docId, tf_idfSum);
        });

        Map<Indexer.DocId, Double> tf_idf_COPY = new HashMap<>(tf_idfPerDocument);
        List<Indexer.DocId> maximums = new ArrayList<>();
        for (int i = 0; i < RESULTS_PER_QUERY; ++i) {
            final double[] maxCoef = {Double.MIN_VALUE};
            final Indexer.DocId[] maxCoefKey = {null};
            tf_idf_COPY.forEach((docId, coef) -> {
                if (coef > maxCoef[0]) {
                    maxCoef[0] = coef;
                    maxCoefKey[0] = docId;
                }
            });
            maximums.add(maxCoefKey[0]);
            tf_idf_COPY.remove(maxCoefKey[0], maxCoef[0]);
        }

        List<String> result = new ArrayList<>();
        maximums.forEach(docId -> {
            StringBuilder res = new StringBuilder();
            res.append("<------------------------------------>").append(System.lineSeparator());
            res.append("Result from book(")
                    .append(docId.book)
                    .append("), chapter(")
                    .append(docId.chapter)
                    .append("), part(")
                    .append(docId.part)
                    .append(")");
            res.append(System.lineSeparator());
            res.append("Summarized tf-idf of all query parts impacts in this result: ")
                    .append(tf_idfPerDocument.get(docId));
            res.append(System.lineSeparator());
            List<Integer> idxs = impactsPerDocument.get(docId);
            JSONArray words = originalDocs.getJSONObject(indexer.ids.indexOf(docId)).getJSONArray("words");
            Integer[] idxs_sorted = new Integer[idxs.size()];
            idxs_sorted = idxs.toArray(idxs_sorted);
            Arrays.sort(idxs_sorted);
            int min = idxs_sorted[0];
            int max = idxs_sorted[idxs_sorted.length - 1];
            if (min == max) {
                min = min - (min % 100);
                max = max + (((words.length()) - max) % 10);
            }
            for (int i = min; i < (max + 2); ++i) {
                res.append(words.getString(i)).append(" ");
            }
            res.append(System.lineSeparator());
            res.append("<---------------------------------->");
            res.append(System.lineSeparator());
            result.add(res.toString());
        });


        return result;
    }

}
