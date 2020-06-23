package net.snortum.readability;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import net.snortum.inputer.Inputer;

public class Readability {
    private static final String VOWEL = "[aeiouyAEIOUY]";
    private static final String ENDS_WITH_E = ".*[eE]";
    private static final String SENTENCE_SPLITTER = "[.?!]\\s*";
    private static final String WORD_SPLITTER = "[,.!?]?\\s+";
    private static final String LETTERS_AND_NUMBERS = "[\\S]";
    private static final String CHARACTER_SPLITTER = "";

    private int sentences, words, chars, syllables, polysyllables;
    private final List<Integer> ages = new ArrayList<>();
    private String fileName = null;

    public static void main(String[] args) {
        new Readability().run(args);
    }

    private void run(String[] args) {
        boolean moreToDo = true;

        do {
            if (args.length == 0) {
                System.out.println();
                String prompt = "Enter the file name to analyse, or \"quit\"";
                fileName = Inputer.getString(prompt, null, fileName);

                if ("quit".equalsIgnoreCase(fileName)) {
                    break;
                }
            } else {
                fileName = args[0];
                moreToDo = false;
            }

            String text = getTextFromFile(fileName);
            parseDataFromText(text);
            displayComponents();
            displayScores();
            displayAverage();
        } while (moreToDo);
    }

    private int sumSyllables(List<String> wordList) {
        return syllableList(wordList)
                .stream()
                .mapToInt(Integer::valueOf)
                .sum();
    }

    private int countPolysyllables(List<String> wordList) {
        return (int) syllableList(wordList)
                .stream()
                .filter(i -> i > 2)
                .count();
    }

    private List<Integer> syllableList(List<String> wordList) {
        List<Integer> syllableList = new ArrayList<>();

        for (String word : wordList) {
            boolean prevLetterWasVowel = false;
            int count = 0;

            for (String letter : word.split("")) {
                if (letter.matches(VOWEL)) {
                    if (!prevLetterWasVowel) {
                        count++;
                    }

                    prevLetterWasVowel = true;
                } else {
                    prevLetterWasVowel = false;
                }
            }

            if (word.matches(ENDS_WITH_E)) {
                count--;
            }

            if (count <= 0) {
                count = 1;
            }

            syllableList.add(count);
        }

        return syllableList;
    }

    private void parseDataFromText (String text) {
        sentences = text.split(SENTENCE_SPLITTER).length;
        List<String> wordList = Arrays.stream(text.split(WORD_SPLITTER))
                .collect(Collectors.toList());
        words = wordList.size();
        chars = (int) Arrays.stream(text.split(CHARACTER_SPLITTER))
                .filter(s -> s.matches(LETTERS_AND_NUMBERS))
                .count();
        syllables = sumSyllables(wordList);
        polysyllables = countPolysyllables(wordList);
    }

    private void displayComponents() {
        System.out.printf("Words: %d%n", words);
        System.out.printf("Sentences: %d%n", sentences);
        System.out.printf("Characters: %d%n", chars);
        System.out.printf("Syllables: %d%n", syllables);
        System.out.printf("Polysyllables: %d%n", polysyllables);
    }

    private void displayScores() {
        boolean moreToDo = true;

        do {
            System.out.println();
            System.out.println("ARI - Automated Readability Index");
            System.out.println("FK - Flesch–Kincaid readability tests");
            System.out.println("SMOG - Simple Measure of Gobbledygook");
            System.out.println("CL - Coleman–Liau index");
            System.out.println("all - All of the above");
            System.out.println("quit - Quit");
            String which = Inputer.getString("Enter the score you want to calculate",
                    Inputer.oneOfThese("ARI", "FK", "SMOG", "CL", "all", "quit"),
                    "all");
            System.out.println();

            switch (which) {
                case "ARI":
                    displayARI();
                    break;
                case "FK":
                    displayFK();
                    break;
                case "SMOG":
                    displaySMOG();
                    break;
                case "CL":
                    displayCL();
                    break;
                case "all":
                    displayARI();
                    displayFK();
                    displaySMOG();
                    displayCL();
                    moreToDo = false;
                    break;
                case "quit":
                    moreToDo = false;
                    break;
                default:
                    System.out.println("Bad input");
            }
        } while (moreToDo);
    }

    private void displayARI() {
        double score = ariScore();
        int age = scoreToAge(score);
        ages.add(age);
        System.out.printf("Automated Readability Index: %.2f (about %d year olds).%n", score, age);
    }

    private void displayFK() {
        double score = fkScore();
        int age = scoreToAge(score);
        ages.add(age);
        System.out.printf("Flesch–Kincaid readability tests: %.2f (about %d year olds).%n", score, age);
    }

    private void displaySMOG() {
        double score = smogScore();
        int age = scoreToAge(score);
        ages.add(age);
        System.out.printf("Simple Measure of Gobbledygook: %.2f (about %d year olds).%n", score, age);
    }

    private void displayCL() {
        double score = clScore();
        int age = scoreToAge(score);
        ages.add(age);
        System.out.printf("Coleman–Liau index: %.2f (about %d year olds).%n", score, age);
    }

    private void displayAverage() {
        OptionalDouble average = ages
                .stream()
                .mapToDouble(Double::valueOf)
                .average();
        average.ifPresent(ave ->
                System.out.printf("This text should be understood in average by %.2f year olds.%n", ave));
    }

    private double ariScore() {
        return 4.71 * ((double) chars / words) + 0.5 * ((double) words / sentences) - 21.43;
    }

    private double fkScore() {
        return 0.39 * ((double) words / sentences) + 11.8 * ((double) syllables / words) - 15.59;
    }

    private double smogScore() {
        return 1.043 * Math.sqrt(polysyllables * (30.0 / sentences)) + 3.1291;
    }

    private double clScore() {
        return 0.0588 * calculateL() - 0.296 * calculateS() - 15.8;
    }

    private double calculateL() {
        return (chars / (double) (words)) * 100;
    }

    private double calculateS() {
        return (sentences / (double) (words)) * 100;
    }

    private int scoreToAge(double score) {
        int ageGroup = 0;
        int intScore = (int) Math.round(score); // wiki says ceiling

        if (intScore == 1) {
            ageGroup = 6;
        } else if (intScore == 2) {
            ageGroup = 7;
        } else if (intScore == 3) {
            ageGroup = 9;
        } else if (intScore == 4) {
            ageGroup = 10;
        } else if (intScore == 5) {
            ageGroup = 11;
        } else if (intScore == 6) {
            ageGroup = 12;
        } else if (intScore == 7) {
            ageGroup = 13;
        } else if (intScore == 8) {
            ageGroup = 14;
        } else if (intScore == 9) {
            ageGroup = 15;
        } else if (intScore == 10) {
            ageGroup = 16;
        } else if (intScore == 11) {
            ageGroup = 17;
        } else if (intScore == 12) {
            ageGroup = 18;
        } else if (intScore == 13) {
            ageGroup = 24;
        } else if (intScore == 14){
            ageGroup = 25; // 24+, what's the upper bound?
        }

        return ageGroup;
    }
    private String getTextFromFile(String fileName) {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.printf("Could not find filename \"%s\"%n", fileName);
            return "";
        }

        Path path = Path.of(fileName);
        List<String> lines;

        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            System.out.printf("Could not read filename \"%s\"%n", fileName);
            e.printStackTrace();
            return "";
        }

        return String.join(" ", lines);
    }
}
