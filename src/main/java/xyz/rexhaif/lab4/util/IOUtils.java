package xyz.rexhaif.lab4.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class IOUtils {

    public static File getFromResources(String name) {
        return new File(IOUtils.class.getClassLoader().getResource(name).getFile());
    }

    public static void saveToFile(String filename, String data) {
        Path path = Paths.get(getFromResources(filename).toURI());

    }

}
