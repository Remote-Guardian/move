package com.remoteguardian.move

import com.remoteguardian.File
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

@MicronautTest
class CliCommandSpec extends Specification {

    @Shared
    String[] localFiles = new String[]{
            "empty",
            "gradlew",
            "microfetch.exe",
            "viktor-bystrov-batman-unsplash(small)-compressed.zip",
            "viktor-bystrov-batman-unsplash(small).jpg",
            "viktor-bystrov-batmanunsplash(original).jpg"}

    void "test the hashFile method"() {
        given:
        CliCommand command = new CliCommand();
        Set<Path> fileSet = Set.of Path.of("src/test/resources/" + (file as String))

        when:
        Set<File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles[0].filePath() == Path.of("src/test/resources/" + file as String)

        where:
        file << localFiles
    }

    void "test the getFilesFromDirectories method"() {
        given:
        CliCommand command = new CliCommand();
        def directory = Set.of(Path.of("src/test/resources/"))

        when:
        Set<Path> hashedFiles = command.getFilesFromDirectories(directory)

        then:
        noExceptionThrown()

        and:
        hashedFiles.stream().map { it -> it.fileName }.allMatch { localFiles.contains(it.toString()) }
    }

    void "test the moveFiles method moving 1 file at a time"() {
        given:
        CliCommand command = new CliCommand();
        Set<File> fileSet = command.hashFiles(Set.of(Path.of("src/test/resources/" + file as String)));
        def outputDirectory = Path.of("src/test/resources/new-directory/")
        try {
            Files.createDirectory(outputDirectory)
        } catch (FileAlreadyExistsException ignored) {
        }


        when:
        command.moveFiles(fileSet, outputDirectory)

        then:
        noExceptionThrown()

        and: "File exists in the new directory"
        Files.exists(Path.of("src/test/resources/new-directory/" + file as String))

        and: "File does not exist in the original directory"
        !Files.exists(Path.of("src/test/resources/" + file as String))

        where:
        file << localFiles
    }

    // TODO(Jonathan) test the moveFiles method moving multiple files at once (and thus move all the files back to the original directory)

    /**
     * Execute a command with the given arguments and return a pair of streams as stdout and stderr.
     *
     * This method captures the stdout and stderr, runs the command using the PicocliRunner,
     * and then returns the output streams.
     *
     * @param args the arguments to pass to the command
     * @return an array containing the output stream and error stream
     */
    String[] executeCommand(String... args) {
        OutputStream out = new ByteArrayOutputStream()
        OutputStream err = new ByteArrayOutputStream()
        System.setOut(new PrintStream(out))
        System.setErr(new PrintStream(out))
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            PicocliRunner.run(CliCommand, ctx, args)
        }
        return new String[]{out, err}
    }
}

