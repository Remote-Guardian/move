package com.remoteguardian;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Represents a file with its hash.
 * <p>
 * This class is used to store the hash of a file in the file system and to validate it against a given hash.
 */
public record File(Path filePath, String fileHash) {

    /**
     * Calculates the hash of the given file and compares it against the hash stored in this object's metadata.
     *
     * @throws IOException              if the file could not be read
     * @throws NoSuchAlgorithmException if the algorithm specified in the {@link AlgorithmEnum} is invalid.
     */
    public void validateHash(AlgorithmEnum algorithm) throws IOException, NoSuchAlgorithmException {
        byte[] expectedBytes = Files.readAllBytes(filePath);
        MessageDigest digest = MessageDigest.getInstance(String.valueOf(algorithm));
        if (!fileHash.equals(HexFormat.of().formatHex(digest.digest(expectedBytes)))) {
            throw new RemoteGuardianException("Hash does not match");
        }
    }
}
