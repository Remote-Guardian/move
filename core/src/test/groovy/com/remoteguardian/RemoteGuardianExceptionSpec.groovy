package com.remoteguardian

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

@MicronautTest
class RemoteGuardianExceptionSpec extends Specification {

    void "test RemoteGuardianException"() {
        when:
        try {
            new java.io.File(null as String)
        } catch (Exception e) {
            throw new RemoteGuardianException("this is a test", e)
        }

        then:
        def error = thrown(RemoteGuardianException)
        error.message == "this is a test"
        error.cause.class == NullPointerException.class
    }

    void "test RemoteGuardianException with message"() {
        when:
        try {
            new java.io.File(null as String)
        } catch (Exception ignored) {
            throw new RemoteGuardianException("this is a test")
        }

        then:
        def error = thrown(RemoteGuardianException)
        error.message == "this is a test"
        null == error.cause

    }
}
