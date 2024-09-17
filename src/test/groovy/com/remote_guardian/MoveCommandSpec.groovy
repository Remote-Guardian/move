package com.remote_guardian

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MoveCommandSpec extends Specification {

    @Shared @AutoCleanup ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)

    void "test move with command line option"() {
        given:
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        System.setOut(new PrintStream(baos))

        String[] args = ['-v'] as String[]
        PicocliRunner.run(MoveCommand, ctx, args)

        expect:
        baos.toString().contains('Hi!')
    }
}

