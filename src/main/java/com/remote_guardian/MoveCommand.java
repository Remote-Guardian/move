package com.remote_guardian;

import io.micronaut.configuration.picocli.PicocliRunner;

import picocli.CommandLine.Command;

@Command(name = "move", description = "...",
        mixinStandardHelpOptions = true)
public class MoveCommand implements Runnable {

    public static void main(String[] args) {
        PicocliRunner.run(MoveCommand.class, args);
    }

    public void run() {
        // do stuff
    }
}
