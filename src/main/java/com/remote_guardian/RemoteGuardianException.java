package com.remote_guardian;

public class RemoteGuardianException extends RuntimeException {

    public RemoteGuardianException(String message) {
        super(message);
    }

    public RemoteGuardianException(String message, Throwable cause) {
        super(message, cause);
    }
}
