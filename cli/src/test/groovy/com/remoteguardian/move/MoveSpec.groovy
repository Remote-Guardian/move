package com.remoteguardian.move

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

import static java.io.File.separator

class MoveSpec extends Specification {

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


    def setup() {
        Stream.of(originalDirectory, outputDirectory).forEach {createDirectoryIfNotExists(it)}
        localFiles.each {copyIfNotExists(
                Path.of(sourceDirectory.toString() + separator + it),
                Path.of(originalDirectory.toString() + separator + it))
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
}
