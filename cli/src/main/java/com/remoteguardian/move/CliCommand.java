package com.remoteguardian.move;

import com.remoteguardian.RemoteGuardianException;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Command(name = "move", description = "recursively moves files and directories",
        mixinStandardHelpOptions = true)
public class CliCommand implements Runnable {

    private static final Logger console = LoggerFactory.getLogger(CliCommand.class);

    @Option(description = "File path(s) or directory path",
            names = {"-i", "--input", "-F"},
            defaultValue = "current directory")
    private String[] input = new String[]{System.getProperty("user.dir")};

    @Option(description = "output directory",
            names = {"-o", "--output", "-O", "--destination"},
            required = true)
    private String outputDirectory;

    public static void main(String[] args) {
        PicocliRunner.run(CliCommand.class, args);
    }

    public void run() {
        if (!Files.exists(Paths.get(outputDirectory))) {
            console.error("Output directory does not exist: {}", outputDirectory);
        }
        if (!Files.isDirectory(Paths.get(outputDirectory))) {
            console.error("Output directory is not a directory: {}", outputDirectory);
        }
        List<Set<Path>> filePaths = validateFilePaths(input);

    }

    /**
     * Checks if all the given paths are valid and separates them into two arrays, the first containing valid files
     * and the second containing valid directories.
     *
     * <p>Any symbolic links are ignored. Each symbolic link is logged at the warn level.</p>
     *
     * @param inputs the paths to be checked
     * @return a list with two elements. The first element is an array of valid files and the second element is an array of valid directories.
     * @throws RemoteGuardianException if any given paths are invalid
     */
    private List<Set<Path>> validateFilePaths(String... inputs) {
        Observable<String> pathsStrings = Observable.fromArray(inputs);
        Observable<String> invalidPaths = pathsStrings.filter(path -> !Files.exists(Paths.get(path)));

        // fail fast
        if (invalidPaths.count().blockingGet() > 0) {
            String invalidPathsStr = Arrays.toString(invalidPaths.blockingStream().toArray(String[]::new));
            console.error("Error: Input parameter must be a valid file path. Input value: {}", invalidPathsStr);
            console.error("Please check the input path and make sure it exists. If it is a directory, " +
                    "it should be accessible and not empty.");
            throw new RemoteGuardianException("Input parameter must be a valid file path");
        }

        // provide warning on any found symbolic links
        Arrays.stream(inputs).forEach(thisInput -> {
            Path path = Paths.get(thisInput);
            if (Files.isSymbolicLink(path)) {
                console.warn("Ignoring symbolic link: {}", thisInput);
            }
        });

        // parse out files and directories
        Set<Path> fileSet = new CopyOnWriteArraySet<>();
        Set<Path> directorySet = new CopyOnWriteArraySet<>();
        Observable<Path> filePathsObservable = pathsStrings
                .map(Paths::get)
                .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS));
        Observable<Path> directoryPathsObservable = pathsStrings
                .map(Paths::get)
                .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS));
        Disposable fileSubscription = filePathsObservable.subscribe(fileSet::add);
        Disposable directorySubscription = directoryPathsObservable.subscribe(directorySet::add);
        fileSubscription.dispose();
        directorySubscription.dispose();
        return List.of(fileSet, directorySet);
    }

}