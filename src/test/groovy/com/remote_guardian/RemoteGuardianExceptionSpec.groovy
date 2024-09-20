package com.remote_guardian

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

@MicronautTest
class RemoteGuardianExceptionSpec extends Specification {

    void "test RemoteGuardianException"() {
        when:
        try {
            Math.divideExact(1,0)
        } catch (Exception e) {
            throw new RemoteGuardianException("this is a test", e)
        }

        then:
        def error = thrown(RemoteGuardianException)
        error.message == "this is a test"
        error.cause.class == ArithmeticException.class
    }

    void "test RemoteGuardianException with message"() {
        when:
        try {
            Math.divideExact(1,0)
        } catch (Exception ignored) {
            throw new RemoteGuardianException("this is a test")
        }

        then:
        def error = thrown(RemoteGuardianException)
        error.message == "this is a test"
        null == error.cause

    }
}
