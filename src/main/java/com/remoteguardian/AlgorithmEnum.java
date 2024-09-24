package com.remoteguardian;
/**
 * Valid values for the {@code --algorithm} command line option.
 * <p>
 * Used as a means of data validation algorithm types when using {@link java.security.MessageDigest}.
 */
public enum AlgorithmEnum {
    MD5("MD5"),
    SHA256("SHA-256"),
    SHA512("SHA-512");
    private final String algorithm;


    AlgorithmEnum(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return algorithm;
    }
}
