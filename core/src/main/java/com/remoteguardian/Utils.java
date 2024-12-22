package com.remoteguardian;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility functions that are not part of any particular class.
 * <p>
 * Since this class is only a collection of static methods, it should be final and have a private constructor.
 */
public final class Utils {

    /**
     * private constructor solely to prevent instantiation
     */
    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Computes hash for each file in filePath object as a hexadecimal string. Returns an array of {@link File} objects.
     *
     * @param algorithm the algorithm to use for computing the hash
     * @param filePath the files to compute the hash of
     * @return a set of files with their hash set
     * @throws NoSuchAlgorithmException if the algorithm is not supported
     *
     * @see AlgorithmEnum#values()
     *
     * <p>Example:
     * <pre>
     * File[] files = Utils.hash(AlgorithmEnum.SHA256, Paths.get("foo.txt"), Paths.get("bar.txt"));
     * for (File file : files) {
     *     System.out.println(file.getHash());
     * }
     * </pre>
     */
    public static Set<File> hash(AlgorithmEnum algorithm, Path... filePath) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm.toString());
        return Arrays.stream(filePath).map(path -> {
            String hash;
            try {
                hash = HexFormat.of().formatHex(digest.digest(Files.readAllBytes((path))));
            } catch (IOException e) {
                throw new RemoteGuardianException(String.format("failed to read from file \"%s\"", path.getFileName()), e);
            }
            return new File(path, hash);
        }).collect(Collectors.toSet());
    }
}
