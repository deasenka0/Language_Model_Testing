import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LanguageModel {
    private static final int DEFAULT_N = 2;

    private int n;
    private Map<String, Map<String, Integer>> languageModels;

    public LanguageModel(int n) {
        this.n = n;
        languageModels = new HashMap<>();
    }

    public void constructLanguageModel(File directory) throws IOException {
        // Get a list of all language subfolders
        File[] languageFolders = directory.listFiles(File::isDirectory);

        // Process each language subfolder and update the language models
        Arrays.stream(languageFolders).forEach(languageFolder -> {
            // Create a histogram for the language subfolder
            Map<String, Integer> histogram = new HashMap<>();

            // Get a list of all text files in the language subfolder
            File[] textFiles = languageFolder.listFiles(file -> file.getName().endsWith(".txt"));

            // Process each text file and update the histogram
            Arrays.stream(textFiles).forEach(textFile -> processTextFile(textFile, histogram));

            // Update the language models with the histogram
            String language = languageFolder.getName();
            languageModels.put(language, histogram);
        });
    }

    private void processTextFile(File textFile, Map<String, Integer> histogram) {
        // Read the text file and filter out punctuation, standardize to lower case, and tokenize into words
        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            reader.lines()
                    .flatMap(line -> Stream.of(line.split("\\s+")))
                    .map(word -> word.replaceAll("[^a-zA-Z]", ""))
                    .map(String::toLowerCase)
                    .forEach(word -> {
                        // Process the word in its n-gram sequence and update the histogram
                        IntStream.range(0, word.length() - n + 1)
                                .mapToObj(i -> word.substring(i, i + n))
                                .forEach(ngram -> histogram.merge(ngram, 1, Integer::sum));
                    });
        } catch (IOException e) {
            // Handle the exception as needed
        }
    }


    public String runClassification(File textFile) throws IOException {
        // Read the text file and filter out punctuation, standardize to lower case, and tokenize into words
        List<String> words = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            words = reader.lines().flatMap(line -> Stream.of(line.split("\\s+")))
                    .map(word -> word.replaceAll("[^a-zA-Z]", ""))
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }

        // Process the words in their n-gram sequence and create a histogram for the text
        Map<String, Integer> histogram = new HashMap<>();

        words.forEach(word -> IntStream.range(0, word.length() - n + 1)
                .mapToObj(i -> word.substring(i, i + n))
                .forEach(ngram -> histogram.merge(ngram, 1, Integer::sum)));

        // Calculate the cosine similarity between the text histogram and each language model
        Map<String, Double> similarities = new HashMap<>();
        languageModels.entrySet().stream()
                .forEach(entry -> {
                    String language = entry.getKey();
                    Map<String, Integer> model = entry.getValue();
                    double similarity = CalculateSimilarity(histogram, model);
                    similarities.put(language, similarity);
                });

        // Select the language with the highest similarity
        return similarities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private double CalculateSimilarity
            (Map<String, Integer> histogram1, Map<String, Integer> histogram2) {
        double dotProduct = histogram1.entrySet().stream()
                .mapToDouble(entry -> {
                    String ngram = entry.getKey();
                    int frequency = entry.getValue();
                    return frequency * histogram2.getOrDefault(ngram, 0);
                }).sum();

        double magnitude1 = histogram1.values().stream().mapToDouble(frequency -> frequency * frequency).sum();
        double magnitude2 = histogram2.values().stream().mapToDouble(frequency -> frequency * frequency).sum();
        return dotProduct / Math.sqrt(magnitude1 * magnitude2);
    }


    public static void main(String[] args) throws IOException {
        // Parse command line arguments
        File directory = new File(args[0]);
        int n = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_N;

        // Construct the language models
        LanguageModel model = new LanguageModel(n);
        model.constructLanguageModel(directory);

        // Classify the mystery text
        File mysteryText = new File(directory, "mysteryGr.txt");
        String language = model.runClassification(mysteryText);
        System.out.println("The mystery text is written in " + language.substring(0, language.length() - 3) + ".");
    }
}
