package com.remoteguardian.move

import com.remoteguardian.File
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

@MicronautTest
class CliCommandSpec extends Specification {

    @Shared
    Path sourceDirectory = Path.of("src/test/resources/")

    @Shared
    Path originalDirectory = Path.of("src/test/resources/first-directory/")

    @Shared
    Path outputDirectory = Path.of("src/test/resources/second-directory/")

    @Shared
    def command

    @Shared
    String[] localFiles = new String[]{
            "empty",
            "gradlew",
            "microfetch.exe",
            "viktor-bystrov-batman-unsplash(small)-compressed.zip",
            "viktor-bystrov-batman-unsplash(small).jpg",
            "viktor-bystrov-batmanunsplash(original).jpg"}

    def setupSpec() {
        command = new CliCommand()
    }

    def setup() {
        Stream.of(originalDirectory, outputDirectory).forEach {createDirectoryIfNotExists(it)}
        localFiles.each {copyIfNotExists(
                Path.of(sourceDirectory.toString() + "/" + it),
                Path.of(originalDirectory.toString() + "/" + it))
        }
    }

    def cleanup() {
        outputDirectory.toFile().deleteDir()
    }

    def cleanupSpec() {
        originalDirectory.toFile().deleteDir()
    }

    @Shared
    def copyIfNotExists = { Path source, Path target ->
        if (!Files.exists(target)) {
            Files.copy(source, target)
        }
    }

    @Shared
    def createDirectoryIfNotExists = { Path target ->
        if (!Files.exists(target)) {
            Files.createDirectory(target)
        }
    }

    void "test the hashFile method with all the files in localFiles"() {
        given:
        Set<Path> fileSet = localFiles.collect { Path.of(originalDirectory.toString() + "/" + it) } as Set

        when:
        Set<File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles.stream().map { it -> it.filePath() }.allMatch { localFiles.contains(it.fileName.toString()) }

    }

    void "test the hashFile method with single files"() {
        given:
        Set<Path> fileSet = Set.of Path.of(originalDirectory.toString() + "/" +  (file as String))

        when:
        Set<File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles[0].filePath() == Path.of(originalDirectory.toString() + "/" + file as String)

        where:
        file << localFiles
    }

    void "test the getFilesFromDirectories method"() {
        given:
        def directory = Set.of(Path.of("src/test/resources/"))

        when:
        Set<Path> hashedFiles = command.getFilesFromDirectories(directory)

        then:
        noExceptionThrown()

        and: "hashedFiles contain all the expected files"
        hashedFiles.stream().map { it -> it.fileName }.allMatch { localFiles.contains(it.toString()) }
    }

    void "test the moveFiles method using several files"() {
        given:
        Set<Path> fileSet = localFiles.collect { Path.of(originalDirectory.toString() + "/" + it) } as Set

        when:
        command.moveFiles(fileSet, outputDirectory)

        then:
        noExceptionThrown()

        and: "Files exists in the new directory"
        localFiles.each { file -> Files.exists(Path.of(outputDirectory.toString() + "/" + file)) }

        and: "File does not exist in the original directory"
        originalDirectory.toFile().list().length == 0
    }

    void "test the moveFiles method moving one file at a time"() {
        given:
        Set<File> fileSet = command.hashFiles Set.of(Path.of(originalDirectory.toString() + "/" + file))

        when:
        command.moveFiles(fileSet, outputDirectory)

        then:
        noExceptionThrown()

        and: "File exists in the new directory"
        Files.exists(Path.of(outputDirectory.toString() + "/" + file))

        and: "File does not exist in the original directory"
        !Files.exists(Path.of(originalDirectory.toString() + "/" + file))

        where:
        file << localFiles
    }

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

