package com.remote_guardian;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.graalvm.collections.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a file with its hash.
 * <p>
 * This class is used to store the hash of a file in the file system and to validate it against a given hash.
 */
@Data
@AllArgsConstructor
public final class File {
    private final AlgorithmEnum algorithm;
    private final Pair<Path, byte[]> metadata;

    /**
     * Calculates the hash of the given file and compares it against the hash stored in this object's metadata.
     *
     * @throws IOException if the file could not be read
     * @throws NoSuchAlgorithmException if the algorithm specified in the {@link AlgorithmEnum} is invalid.
     */
    public void validateHash() throws IOException, NoSuchAlgorithmException {
        byte[] expectedBytes = Files.readAllBytes(metadata.getLeft());

        MessageDigest digest = MessageDigest.getInstance(String.valueOf(algorithm));
        if (!java.util.Arrays.equals(metadata.getRight(), digest.digest(expectedBytes))) {
            throw new RemoteGuardianException("Hash does not match");
        }
    }
}
