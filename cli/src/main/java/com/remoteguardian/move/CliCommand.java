package com.remoteguardian.move;

import com.remoteguardian.AlgorithmEnum;
import com.remoteguardian.File;
import com.remoteguardian.RemoteGuardianException;
import com.remoteguardian.Utils;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.annotation.Value;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;


@Command(
        name = "move",
        description = "recursively moves files and directories",
        versionProvider = CliCommand.PropertiesVersionProvider.class,
        mixinStandardHelpOptions = true
)
public class CliCommand implements Runnable {

    private static final Logger console = LoggerFactory.getLogger(CliCommand.class);

    @Option(description = "File path(s) or directory path",
            names = {"-i", "--input", "-F"},
            split = ",",
            defaultValue = "current directory")
    private String[] input;

    @Option(description = "output directory",
            names = {"-o", "--output", "--destination"},
            required = true)
    private String outputDirectory;

    public static void main(String[] args) {
        PicocliRunner.run(CliCommand.class, args);
    }

    public void run() {
        Path output = Paths.get(outputDirectory);
        validateDirectory(output);

        List<Set<Path>> filePathsList = validateFilePaths(input);
        Set<Path> filePaths = filePathsList.get(0);
        filePaths.addAll(getFilesFromDirectories(filePathsList.get(1)));
        try { //remove this after handling files used by other processes is implemented
            moveFiles(hashFiles(filePaths), Path.of(outputDirectory));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateDirectory(Path output) {
        if (!Files.isDirectory(output, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.exists(output)) {
                console.error("Output directory does not exist: {}", outputDirectory);
            } else if (Files.isSymbolicLink(output)) {
                console.error("Output directory is a symbolic link: {}", outputDirectory);
            }
        }
        if (!Files.isWritable(output)) {
            console.error("Output directory is not writable: {}", outputDirectory);
        }
    }

    /**
     * Moves given files to the given output directory. Also validates the hash of the file immediately after moving it.
     * @param files the set of files to move and hash
     */
    void moveFiles(Set<File> files, Path outputDirectory) throws InterruptedException {
        validateDirectory(outputDirectory);
        for (File file : files) {
            int repeat = 0;
            while(repeat < 1) {
                try {
                    console.debug("Moving {} to {}", file.filePath(), outputDirectory);
                    Files.move(file.filePath(), Path.of(
                                    outputDirectory + java.io.File.separator + file.filePath().getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                    console.debug("Moved {} to {}", file.filePath(), outputDirectory);
                    repeat ++;
                } catch (IOException e) {
                    console.error("Error moving file {} to {} directory.", file.filePath(), outputDirectory, e);
                    wait(200); //todo remove this once behavior for handling files used by other processes is implemented
                    break;
                }
                File tempFile = new File(Path.of((outputDirectory + java.io.File.separator + file.filePath().getFileName())), file.fileHash());
                try {
                    console.debug("Hashing {} with the MD5 algorithm to compare against original hash", tempFile.filePath());
                    tempFile.validateHash(AlgorithmEnum.MD5);
                    console.debug("Hashes match. Original file {} is valid", tempFile.filePath());
                } catch (IOException | NoSuchAlgorithmException e) {
                    console.error("Could not hash {} with the MD5 algorithm: {}", tempFile.filePath(), e.getMessage());
                } catch (RemoteGuardianException e) {
                    console.error("Hashes do not match. {} is not identical to the original file", tempFile.filePath());
                }
            }
        }
        console.debug("Finished moving {} files to {} directory", files.size(), outputDirectory);
    }

    /**
     * Returns a set of file from given set of filePaths using the MD5 algorithm.
     *
     * <p>Will exit with System.exit(1) if it fails to hash the files with a NoSuchAlgorithmException.</p>
     *
     * @param files the set of files to hash
     * @return a set of File objects with their hash set
     */
    Set<File> hashFiles(Set<Path> files) {
        Set<File> hashedFiles = Set.of();
        try {
            return Utils.hash(AlgorithmEnum.MD5, files.toArray(Path[]::new));
        } catch (NoSuchAlgorithmException e) {
            console.error("Could not hash the input files with the MD5 algorithm: {}", e.getMessage());
        }
        return hashedFiles;
    }

    /**
     * Recursively gather all files in the given directories and subdirectories.
     *
     * @param directories the directories to whose contents are returned
     * @return a set of all files in the given directories and subdirectories
     */
    Set<Path> getFilesFromDirectories(Set<Path> directories) {
        Set<Path> filePaths = new CopyOnWriteArraySet<>();
        directories.forEach(directory -> {
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile)
                        .forEach(filePaths::add);
            } catch (IOException e) {
                console.error("Error traversing reading directory contents: {}", e.getMessage());
            }
        });
        return filePaths;
    }

    /**
     * Checks if all the given paths are valid and separates them into two arrays, the first containing valid files
     * and the second containing valid directories.
     *
     * <p>Any symbolic links are ignored. Each symbolic link is logged at the warn level.</p>
     *
     * @param inputs the paths to be checked
     * @return a list with two elements. The first element is an array of valid files and the second element is an
     * array of valid directories.
     * @throws RemoteGuardianException if any given paths are invalid
     */
    private List<Set<Path>> validateFilePaths(String... inputs) {
        Observable<String> pathsStrings = Observable.fromArray(inputs);
        Observable<String> invalidPaths = pathsStrings.filter(path -> !Files.exists(Paths.get(path)));

        // fail fast
        if (invalidPaths.count().blockingGet() > 0) {
            String invalidPathsStr = Arrays.toString(invalidPaths.blockingStream().toArray(String[]::new));
            console.error("Input parameter must be a valid file path. Input value: {} Please check the input " +
                    "path and make sure it exists. If it is a directory, " +
                    "it should be accessible and not empty.", invalidPathsStr);
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

    /**
     * {@link IVersionProvider} implementation that returns version from the application.yml file.
     */
    static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
        @Value("${micronaut.application.version}")
        String applicationVersion;

        @Value("${micronaut.application.name}")
        String applicationName;

        public String[] getVersion() {
            if (null == applicationVersion) {
                applicationVersion = System.getProperty("micronaut.application.version");
            }
            if (null == applicationName) {
                applicationName = System.getProperty("micronaut.application.name");
            }
            return new String[]{applicationName, applicationVersion};
        }
    }

}