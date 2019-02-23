package ru.ifmo.rain.kramer.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class FileVisitor extends SimpleFileVisitor<Path> {
    private final static int INIT_HASH = 0x811c9dc5;
    private final static int FNV_32_PRIME = 0x01000193;
    private final static int STEP = 0xff;
    private final static int BUFF_SIZE = 1024;
    private byte[] bytes = new byte[BUFF_SIZE];
    private final BufferedWriter writer;

    FileVisitor(BufferedWriter writer) {
        this.writer = writer;
    }

    private FileVisitResult writeData(int hash, Path file) {
        try {
            writer.write(String.format("%08x %s", hash, file.toString()));
            writer.newLine();
            return CONTINUE;
        } catch (IOException e) {
            System.err.println("Error while writing in walking" + e.getMessage());
            return TERMINATE;
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        int hash = INIT_HASH;
        try (InputStream reader = Files.newInputStream(file)) {
            int count;
            while ((count = reader.read(bytes)) >= 0) {
                for (int i = 0; i < count; i++) {
                    hash = (hash * FNV_32_PRIME) ^ (bytes[i] & STEP);
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return writeData(hash, file);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return writeData(0, file);
    }

}