package com.remoteguardian.move

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Shared
import java.nio.file.Files
import java.nio.file.Path

import static java.io.File.separator

@MicronautTest
class CliCommandSpec extends MoveSpec {

    @Shared
    @Value('${micronaut.application.version}')
    String version = "0.0.1"

    @Shared
    @Value('${micronaut.application.version}')
    String name = "move"


    @Shared
    final PrintStream originalOut = System.out
    @Shared
    final PrintStream originalErr = System.err

    @Shared
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    ByteArrayOutputStream errStream = new ByteArrayOutputStream()


    def setup() {
        outputStream.reset()
        errStream.reset()
        System.setOut(new PrintStream(outputStream))
        System.setErr(new PrintStream(errStream))
    }

    def cleanup() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }


    void "test the help text"() {
        when:
        String[] args = ["-h"]
        def (stdout, stderr) = executeCommand(args)

        then:
        String[] expectedOutput = new String[]{
                "Usage: move [-hV] -o=<outputDirectory> [-i=<input>[,<input>...]]...",
                "recursively moves files and directories",
                "  -h, --help      Show this help message and exit.",
                "  -i, -F, --input=<input>[,<input>...]",
                "                  File path(s) or directory path",
                "  -o, --output, --destination=<outputDirectory>",
                "                  output directory",
                "  -V, --version   Print version information and exit."
        }
        stdout.toString().split(System.lineSeparator()) == expectedOutput
        stderr.size() == 0
    }

    void "test the version text"() {
        when:
        String[] args = [input]
        def (stdout, stderr) = executeCommand(args)

        then:
        stdout.toString().trim().replace(System.lineSeparator() as String, "") == "$name$version"
        stderr.size() == 0

        where:
        input << ["-V", "--version"]
    }

    void "test moving one file at a time"() {
        when:
        String[] args = ["-i", originalDirectory.toString() + separator + input, "-o", outputDirectory]
        def (stdout, stderr) = executeCommand(args)

        then:
        stdout.size() == 0
        stderr.size() == 0

        and: "File exists in the new directory"
        Files.exists(Path.of(outputDirectory.toString() + separator + input))

        and: "File does not exist in the original directory"
        !Files.exists(Path.of(originalDirectory.toString() + separator + input))

        where:
        input << localFiles
    }

    void "test moving multiple files at once using the '#inputFlag' and the '#outputFlag'"() {
        when:
        String input = localFiles.collect { originalDirectory.toString() + separator + it }.join(",")
        String[] args = [inputFlag, input, outputFlag, outputDirectory]
        def (stdout, stderr) = executeCommand(args)

        then:
        stdout.size() == 0
        stderr.size() == 0

        and: "Files exists in the new directory"
        input.each { file -> Files.exists(Path.of(outputDirectory.toString() + separator + file)) }

        and: "File does not exist in the original directory"
        originalDirectory.toFile().list().length == 0

        where:
        entry << GroovyCollections.combinations(["-o", "--output", "--destination"], ["-i", "-F", "--input"])
        outputFlag = (entry as List)[0] as String
        inputFlag = (entry as List)[1] as String
    }

    /**
     * Execute a command with the given arguments and return a pair of streams as stdout and stderr.
     *
     * This method captures the stdout and stderr, runs the command using the PicocliRunner,
     * and then returns the output streams.
     *
     * @param args the arguments to pass to the command
     * @return an array containing the output stream and error stream
     */
    String[] executeCommand(String... args) {
        OutputStream out = new ByteArrayOutputStream()
        OutputStream err = new ByteArrayOutputStream()
        System.setOut(new PrintStream(out))
        System.setErr(new PrintStream(err))
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            PicocliRunner.run(CliCommand.class, ctx, args)
        }
        return new String[]{out, err}
    }
}



