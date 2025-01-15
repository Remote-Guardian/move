package com.remoteguardian.move;

import com.remoteguardian.AlgorithmEnum;
import com.remoteguardian.File;
import com.remoteguardian.RemoteGuardianException;
import com.remoteguardian.Utils;
import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.annotation.Value;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.fusesource.jansi.AnsiConsole;

import static java.lang.System.out; // ignore sonarqube whining (we actually are printing to console despite what sonarqube thinks)
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(CliCommand.class);

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
        AnsiConsole.systemInstall();
        Path output = Paths.get(outputDirectory);
        validateDirectory(output);

        List<Set<Path>> filePathsList = validateFilePaths(input);
        Set<Path> filePaths = filePathsList.get(0);
        filePaths.addAll(getFilesFromDirectories(filePathsList.get(1)));
        moveFiles(hashFiles(filePaths), Path.of(outputDirectory));
    }

    private void validateDirectory(Path output) {
        if (!Files.isDirectory(output, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.exists(output)) {
                err("Output directory does not exist: %s", outputDirectory);
            } else if (Files.isSymbolicLink(output)) {
                err("Output directory is a symbolic link: %s", outputDirectory);
            }
        }
        if (!Files.isWritable(output)) {
            err("Output directory is not writable: %s", outputDirectory);
        }
    }

    /**
     * Moves given files to the given output directory. Also validates the hash of the file immediately after moving it.
     * @param files the set of files to move and hash
     */
    void moveFiles(Set<File> files, Path outputDirectory) {
        validateDirectory(outputDirectory);
        for (File file : files) {
            try {
                debug("Moving %s to %s", file.filePath(), outputDirectory);
                Files.move(file.filePath(), Path.of(outputDirectory + java.io.File.separator + file.filePath().getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                debug("Moved %s to %s", file.filePath(), outputDirectory);
            } catch (IOException e) {
                err("Error moving file %s to %s directory. %s", file.filePath(), outputDirectory, e);
            }
            File tempFile = new File(Path.of((outputDirectory + java.io.File.separator + file.filePath().getFileName())), file.fileHash());
            try {
                debug("Hashing %s with the MD5 algorithm to compare against original hash", tempFile.filePath());
                tempFile.validateHash(AlgorithmEnum.MD5);
                debug("Hashes match. %s is identical to the original file", tempFile.filePath());
            } catch (IOException | NoSuchAlgorithmException e) {
                err("Could not hash %s with the MD5 algorithm: %s", tempFile.filePath(), e.getMessage());
            } catch (RemoteGuardianException e) {
                err("Hashes do not match. %s is not identical to the original file", tempFile.filePath());
            }
        }
        debug("Finished moving %s files to %s directory", files.size(), outputDirectory);
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
            err("Could not hash the input files with the MD5 algorithm: %s", e.getMessage());
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
                err("Error traversing reading directory contents: %s", e.getMessage());
            }
        });
        return filePaths;
    }


    /**
     * Logs a message at the debug level and prints it to the console.
     *
     * @param message the message to be logged and printed
     */
    private void debug(String message) {
        out.println(ansi().fg(BLUE).a(message).reset());
        logger.debug(message);
    }

    /**
     * Logs a message at the error level and prints it to the console.
     *
     * @param message the message to be logged and printed
     */
    private void err(String message) {
        out.println(ansi().fg(RED).a(message).reset());
        logger.error(message);
    }

    /**
     * Logs a message at the debug level and prints it to the console.
     *
     * @param format the format of the message. This parameter is passed to the {@link String#format(String, Object...)}
     *               method to generate the message string.
     * @param args   the arguments to be formatted
     */
    private void debug(String format, Object... args) {
        debug(String.format(format, args));
    }

    /**
     * Logs a message at the error level and prints it to the console.
     *
     * @param format the format of the message. This parameter is passed to the {@link String#format(String, Object...)}
     *               method to generate the message string.
     * @param args   the arguments to be formatted
     */
    private void err(String format, Object... args) {
        err(String.format(format, args));
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
            logger.error("Input parameter must be a valid file path. Input value: {} Please check the input " +
                    "path and make sure it exists. If it is a directory, " +
                    "it should be accessible and not empty.", invalidPathsStr);
        }

        // provide warning on any found symbolic links
        Arrays.stream(inputs).forEach(thisInput -> {
            Path path = Paths.get(thisInput);
            if (Files.isSymbolicLink(path)) {
                logger.warn("Ignoring symbolic link: {}", thisInput);
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
    static class PropertiesVersionProvider implements IVersionProvider {
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