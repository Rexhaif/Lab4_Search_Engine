package xyz.rexhaif.lab4;


import org.json.JSONArray;
import org.json.JSONObject;
import xyz.rexhaif.lab4.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StemmedBuilder {

    public static void main(String[] args) {

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
        JSONArray docs = new JSONArray();
        corpus
                .getJSONArray("corpus")
                .forEach((doc) -> {
                    JSONObject obj = (JSONObject) doc;
                    List<String> words = new ArrayList<>();
                    Arrays.asList(obj.getString("text").split("[\\s+]")).forEach(
                            str -> {
                                words.add(Stemmer.stem(str));
                            }
                    );
                    obj.remove("text");
                    obj.put("words", new JSONArray(words));
                    docs.put(obj);
                }
        );
        try {
            Files.write(
                    Paths.get(new File("book34_stemmed.json").toURI()),
                    new JSONObject().put("corpus", docs).toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
