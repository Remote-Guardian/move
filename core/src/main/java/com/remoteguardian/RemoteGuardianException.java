package com.remoteguardian;

/**
 * A generic exception class for Remote Guardian.
 * <p>
 * This exception should be used sparingly and only for exceptional conditions
 * that are not otherwise covered by the API. It is not intended to be used
 * as a general-purpose exception class, and should be used only as a last resort.
 * <p>
 * It is recommended that subclasses of this exception be immutable, as
 * they will be serialized and deserialized when transmitted over the wire.
 * <p>
 * When throwing this exception, it is recommended to include a message that
 * fully describes the error, including any relevant details such as any
 * related {@link Throwable} instances.
 */
public class RemoteGuardianException extends RuntimeException {

    /**
     * Creates a new RemoteGuardianException with the given message.
     * <p>
     * The message should be a concise description of the error, including any
     * relevant details. It is recommended to include a message that fully
     * describes the error, including any related {@link Throwable} instances.
     * <p>
     * It is recommended to avoid using this constructor unless you are
     * absolutely sure that the message is complete and accurate. Otherwise,
     * you should use the
     * {@link #RemoteGuardianException(String, Throwable)} constructor.
     *
     * @param message the error message
     */
    public RemoteGuardianException(String message) {
        super(message);
    }


    /**
     * Creates a new RemoteGuardianException with the given message and cause.
     * <p>
     * The message should be a concise description of the error, including any
     * relevant details. It is recommended to include a message that fully
     * describes the error, including any related {@link Throwable} instances.
     * <p>
     * When throwing this exception, it is recommended to include a message that
     * fully describes the error, including any relevant details such as any
     * related {@link Throwable} instances. It is also recommended to set the
     * cause of the exception to the related {@link Throwable} instance.
     * <p>
     * This constructor is preferred over the
     * {@link #RemoteGuardianException(String) RemoteGuardianException(String)}
     * constructor, as it provides more information about the error.
     *
     * @param message the error message
     * @param cause   the related {@link Throwable} instance
     */
    public RemoteGuardianException(String message, Throwable cause) {
        super(message, cause);
    }
}
