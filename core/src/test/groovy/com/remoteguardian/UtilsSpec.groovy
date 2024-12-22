package com.remoteguardian

import io.micronaut.core.io.ResourceLoader
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

@MicronautTest
class UtilsSpec extends Specification {

    @Shared
    def localFiles = new String[]{
            "empty",
            "gradlew",
            "microfetch.exe",
            "viktor-bystrov-batman-unsplash(small)-compressed.zip",
            "viktor-bystrov-batman-unsplash(small).jpg",
            "viktor-bystrov-batmanunsplash(original).jpg"}

    @Inject
    @Shared
    ResourceLoader resourceLoader

    void "Instantiating Utils class throws RemoteGuardianException"() {
        when:
        //noinspection GroovyAccessibility
        new Utils()

        then:
        thrown(IllegalStateException)
    }

    void "test hash method produces single file array from '#localFile' file and #algorithm"() {
        when: "read in the file as a byte array"
        Optional<URL> resource = resourceLoader.getResource(localFile as String)
        if (!resource.isPresent()) {
            throw new IllegalStateException("test file $localFile not found")
        }
        def filepath = Path.of(resource.get().toURI())
        byte[] expectedArray = Files.readAllBytes(filepath)

        and:
        File[] files = Utils.hash(algorithm as AlgorithmEnum, filepath)

        then:
        files.length == 1
        files[0].validateHash(algorithm as AlgorithmEnum)

        and:
        MessageDigest digest = MessageDigest.getInstance(algorithm.toString())
        new File[]{new File(filepath, HexFormat.of()
                .formatHex(digest.digest(expectedArray))
        )}[0].fileHash() == files[0].fileHash()

        where: "all combinations of localFile and algorithm"
        entry << GroovyCollections.combinations([localFiles, AlgorithmEnum.values()])
        localFile = (entry as List)[0]
        algorithm = (entry as List)[1]
    }

    void "test hash method called on an invalid file throws IOException wrapped by a RemoteGuardianException"() {
        when:
        final RandomAccessFile file = new RandomAccessFile(localFile as java.io.File, "rw")
        file.getChannel().lock()
        def error = null
        try {
            Utils.hash(algorithm as AlgorithmEnum, Paths.get(localFile as String))
        }catch (RemoteGuardianException e) {
            error = e
        }
        file.close()

        then:"validate the error message reports the invalid file"
        error.getMessage() == "failed to read from file \"$localFile\""
        error.cause.class == IOException

        and:"clean up errant files"
        try{
            (localFile as java.io.File).delete()
        }catch (Exception ignored){}


        where:
        entry << GroovyCollections.combinations([localFiles, AlgorithmEnum.values()])
        localFile = (entry as List)[0]
        algorithm = (entry as List)[1]
    }
}
