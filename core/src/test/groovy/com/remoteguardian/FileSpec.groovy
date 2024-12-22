package com.remoteguardian

import io.micronaut.core.io.ResourceLoader
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@MicronautTest
class FileSpec extends Specification {

    @Shared
    def localFiles = new String[]{
            "empty",
            "gradlew",
            "microfetch.exe",
            "viktor-bystrov-batman-unsplash(small)-compressed.zip",
            "viktor-bystrov-batman-unsplash(small).jpg",
            "viktor-bystrov-batmanunsplash(original).jpg"}

    @Inject @Shared
    ResourceLoader resourceLoader

    def "test ValidateHash with '#localFile' file and '#algorithm'"() {
        when:"read in the file as a byte array"
        Optional<URL> resource = resourceLoader.getResource(localFile as String)
        if (!resource.isPresent()) { throw new IllegalStateException("test file $localFile not found") }
        def filepath = Path.of(resource.get().toURI())
        byte[] expectedArray = Files.readAllBytes(filepath)

        and:"calculate the hash and create a new File object"
        MessageDigest digest = MessageDigest.getInstance(algorithm.toString())
        String myCalculatedHash = HexFormat.of().formatHex(digest.digest(expectedArray))
        File thisFile = new File(filepath, myCalculatedHash)

        then:"no exception is thrown thus far in the test"
        noExceptionThrown()

        and:"Validate the hash using the new File object"
        thisFile.validateHash(algorithm as AlgorithmEnum)

        then: "Create a file with a hash not formatted in hex"
        File thisBadFile = new File(filepath, new String(digest.digest(expectedArray), StandardCharsets.UTF_8))

        and: "Attempting to validate the hash of this bad file will throw an exception"
        try {
            thisBadFile.validateHash(algorithm as AlgorithmEnum)
        } catch (RemoteGuardianException e) {
            assert e.getMessage() == "Hash does not match"
        }

        where:"all combinations of localFile and algorithm are tested"
        entry << GroovyCollections.combinations([localFiles, AlgorithmEnum.values()])
        localFile = (entry as List)[0]
        algorithm = (entry as List)[1]
    }
}
