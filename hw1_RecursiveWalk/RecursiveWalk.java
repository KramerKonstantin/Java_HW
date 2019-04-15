package ru.ifmo.rain.kramer.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    private static Path inputPath;
    private static Path outputPath;

    private static void walk() {
        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                String directory;
                FileVisitor visitor = new FileVisitor(writer);
                try {
                    while ((directory = reader.readLine()) != null) {
                        try {
                            Path path = Paths.get(directory);
                            Files.walkFileTree(path, visitor);
                        } catch (InvalidPathException e) {
                            writer.write(String.format("%08x %s", 0, directory));
                            writer.newLine();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error while count hashes: " + e.getMessage());
                }
            } catch (FileNotFoundException e) {
                System.err.println("Output file not found" + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Security exception while opening output file." + e.getMessage());
            } catch (IOException e) {
                System.err.println("An exception has occurred when writing file: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found" + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Security exception while opening input file." + e.getMessage());
        } catch (IOException e) {
            System.err.println("An exception has occurred when reading file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Expected 2 not-null arguments: <input file> and <output file>");
        } else {
            try {
                inputPath = Paths.get(args[0]);
            } catch (InvalidPathException e) {
                System.err.println("Incorrect path to input file: " + e.getMessage());
            }
            try {
                outputPath = Paths.get(args[1]);
            } catch (InvalidPathException e) {
                System.err.println("Incorrect path to output file: " + e.getMessage());
            }
            if (outputPath.getParent() != null) {
                try {
                    Files.createDirectories(outputPath.getParent());
                } catch (IOException e) {
                    System.err.println("Unable to create folders for output file: " + e.getMessage());
                }
            }
            walk();
        }
    }
}