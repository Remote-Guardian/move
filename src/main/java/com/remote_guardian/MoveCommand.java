package com.remote_guardian;

import io.micronaut.configuration.picocli.PicocliRunner;

import picocli.CommandLine.Command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

@Command(name = "move", description = "...",
        mixinStandardHelpOptions = true)
public class MoveCommand implements Runnable {

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(MoveCommand.class, args);
    }

    public void run() {

    }

    void HashFile(MessageDigest messageDigest, String... filePath) {
        Arrays.stream(filePath).forEach(
            path -> {
                try (InputStream inputStream = Files.newInputStream(Paths.get(path))) {

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );

    }
}
