package com.remoteguardian.move

import com.remoteguardian.AlgorithmEnum
import com.remoteguardian.File
import com.remoteguardian.Utils
import io.micronaut.test.extensions.spock.annotation.MicronautTest

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

import static java.io.File.separator

@MicronautTest
class CliCommandMethodsSpec extends MoveSpec {

    def setupSpec() {
        command = new CliCommand()
    }

    void "test the hashFile method with all the files in localFiles"() {
        given:
        Set<Path> fileSet = localFiles.collect { Path.of(originalDirectory.toString() + separator + it) } as Set

        when:
        Set<File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles.stream().map { it -> it.filePath() }.allMatch { localFiles.contains(it.fileName.toString()) }

    }

    void "test the hashFile method with single files"() {
        given:
        Set<Path> fileSet = Set.of Path.of(originalDirectory.toString() + separator + (file as String))

        when:
        Set<File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles[0].filePath() == Path.of(originalDirectory.toString() + separator + file as String)

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
        def fileSet = Utils.hash(AlgorithmEnum.MD5, Stream.of(localFiles)
                .map(it -> Path.of(originalDirectory.toString() + separator + it))
                .collect().toArray(Path[]::new) as Path[])

        when:
        command.moveFiles(fileSet, outputDirectory)

        then:
        noExceptionThrown()

        and: "Files exists in the new directory"
        localFiles.each { file -> Files.exists(Path.of(outputDirectory.toString() + separator + file)) }

        and: "File does not exist in the original directory"
        originalDirectory.toFile().list().length == 0
    }

    void "test the moveFiles method moving one file at a time"() {
        given:
        Set<File> fileSet = command.hashFiles Set.of(Path.of(originalDirectory.toString() + separator + file))

        when:
        command.moveFiles(fileSet, outputDirectory)

        then:
        noExceptionThrown()

        and: "File exists in the new directory"
        Files.exists(Path.of(outputDirectory.toString() + separator + file))

        and: "File does not exist in the original directory"
        !Files.exists(Path.of(originalDirectory.toString() + separator + file))

        where:
        file << localFiles
    }

    void "test the validateFilePaths method"() {
        when:
        String[] localFilesString = localFiles.collect { originalDirectory.toString() + separator + it }
        Set<Path> fileSet = command.validateFilePaths(localFilesString)

        then:
        noExceptionThrown()

        and:
        null != fileSet[0]
        fileSet[0].size() == localFiles.size()
        fileSet[0].forEach {it -> localFiles.contains(it.fileName.toString())}

        and:
        null != fileSet[1]
        fileSet[1].size() == 0
    }


}

