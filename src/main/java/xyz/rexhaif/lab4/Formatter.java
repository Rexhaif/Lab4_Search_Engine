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
import java.util.List;

public class Formatter {

    public static final String INPUT_FILE_NAME = "book34.txt";
    public static final String OUTPUT_FILE_NAME = "book34.json";

    public static void main(String[] args) {
        List<JSONObject> chapters = new ArrayList<>();
        final StringBuilder[] tmpBuilder = {new StringBuilder()};
        final Integer[] currentBook = {-1};
        final Integer[] currentPart = {-1};
        final Integer[] currentChapter = {-1};
        try {
            Files.lines(Paths.get(IOUtils.getFromResources(INPUT_FILE_NAME).toURI()))
                    .filter(t -> !t.equalsIgnoreCase(""))
                    .forEach(t -> {
                        if (t.startsWith("/")) {
                            if (currentBook[0] != -1) {
                                JSONObject previousChapter = new JSONObject();
                                previousChapter
                                        .put("book", currentBook[0])
                                        .put("part", currentPart[0])
                                        .put("chapter", currentChapter[0])
                                        .put("text", tmpBuilder[0].toString());
                                chapters.add(previousChapter);
                                tmpBuilder[0] = new StringBuilder();
                            }
                            String[] numeration = t.replace('/', ' ').split("\\s+");
                            currentBook[0] = Integer.parseInt(numeration[1]);
                            currentPart[0] = Integer.parseInt(numeration[2]);
                            currentChapter[0] = Integer.parseInt(numeration[3]);
                        } else {
                            tmpBuilder[0].append(
                                    t
                                            .replace("\u2026", "")
                                            .replace("\u2013", "")
                                            .replace(".", " ")
                                            .replace(",", "")
                                            .replace(":", "")
                                            .replace(";", "")
                                            .replace("[", "")
                                            .replace("]", "")
                                            .replace("(", "")
                                            .replace(")", "")
                                            .replace("«", "")
                                            .replace("»", "")
                                            .replace("  ", " ")
                            );
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray arr = chapters.stream().collect(
                JSONArray::new,
                JSONArray::put,
                (a1, a2) -> {}
        );
        JSONObject object = new JSONObject();
        object.put("corpus", arr);
        try {
            Files.write(
                    Paths.get(new File(OUTPUT_FILE_NAME).toURI()),
                    object.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
