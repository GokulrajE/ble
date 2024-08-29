package com.example.ble;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AverageByDateCalculator {

    public static Map<Date, Float[]> calculateAverages(String mainFolderPath) throws IOException, ParseException {
        Map<Date, Float[]> dateAverages = new HashMap<>();
        File mainFolder = new File(mainFolderPath);
        File[] dateFolders = mainFolder.listFiles(File::isDirectory);

        if (dateFolders != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // Adjust if needed

            for (File dateFolder : dateFolders) {
                File rightFile = new File(dateFolder, "right.csv");
                File leftFile = new File(dateFolder, "left.csv");

                if (rightFile.exists() || leftFile.exists()) {
                    Date date = sdf.parse(dateFolder.getName()); // Folder name as date
                    Float[] averages = dateAverages.computeIfAbsent(date, k -> new Float[]{0f, 0f, 0f, 0f}); // [sumRight, countRight, sumLeft, countLeft]

                    if (rightFile.exists()) {
                        averages[0] += calculateSum(rightFile, 1); // Sum for right values
                        averages[1] += countLines(rightFile); // Count for right values
                    }

                    if (leftFile.exists()) {
                        averages[2] += calculateSum(leftFile, 1); // Sum for left values
                        averages[3] += countLines(leftFile); // Count for left values
                    }
                }
            }

            // Compute averages
            for (Map.Entry<Date, Float[]> entry : dateAverages.entrySet()) {
                Float[] values = entry.getValue();
                if (values[1] > 0) values[0] /= values[1]; // Average for right
                if (values[3] > 0) values[2] /= values[3]; // Average for left
                entry.setValue(new Float[]{values[0], values[2]}); // [avgRight, avgLeft]
            }
        }

        return dateAverages;
    }

    private static float calculateSum(File file, int valueIndex) throws IOException {
        float sum = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > valueIndex) {
                    sum += Float.parseFloat(parts[valueIndex]);
                }
            }
        }
        return sum;
    }

    private static int countLines(File file) throws IOException {
        int count = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            while (bufferedReader.readLine() != null) {
                count++;
            }
        }
        return count;
    }
}

