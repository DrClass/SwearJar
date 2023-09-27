package io.github.drclass.swearjar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvManager {
	private static final String CSV_PATH = "jars.csv";
	
	public static void writeJarssToCsv(List<Jar> jars) {
        try (FileWriter writer = new FileWriter(CSV_PATH)) {
            for (Jar jar : jars) {
                writer.append(jar.toString());
                writer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Jar> readJarsFromCsv() {
    	if (!Files.exists(Paths.get(CSV_PATH))) {
            return null;
        }
        List<Jar> jars = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Jar jar = Jar.fromCsv(line);
                jars.add(jar);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jars;
    }
}
