package com.remoteguardian.move

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

@MicronautTest
class CliCommandSpec extends Specification {

    @Shared
    def localFiles = new String[]{
            "empty",
            "gradlew",
            "microfetch.exe",
            "viktor-bystrov-batman-unsplash(small)-compressed.zip",
            "viktor-bystrov-batman-unsplash(small).jpg",
            "viktor-bystrov-batmanunsplash(original).jpg"}

    void "test the hashFile method"() {
        given:
        CliCommand command = new CliCommand();
        Set<Path> fileSet = Set.of(Path.of("src/test/resources/" + file as String))

        when:
        Set<com.remoteguardian.File> hashedFiles = command.hashFiles(fileSet)

        then:
        noExceptionThrown()

        and:
        hashedFiles[0].filePath() == Path.of("src/test/resources/" + file as String)

        where:
        file << localFiles
    }
}

