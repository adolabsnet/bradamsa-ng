package com.github.nscuro.bradamsang.radamsa;

import com.github.nscuro.bradamsang.io.CommandExecutor;
import com.github.nscuro.bradamsang.io.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public final class Radamsa {

    private static final Logger LOGGER = LoggerFactory.getLogger(Radamsa.class);

    private final CommandExecutor commandExecutor;

    private final String radamsaCommand;

    public Radamsa(final CommandExecutor commandExecutor, final String radamsaCommand) {
        this.commandExecutor = commandExecutor;
        this.radamsaCommand = radamsaCommand;
    }

    public void fuzz(final Parameters parameters) throws RadamsaException {
        if (!isValidRadamsaCommand(radamsaCommand)) {
            throw new RadamsaException(format("\"%s\" is not a valid radamsa command", radamsaCommand));
        } else if (parameters.getBaseValue() == null) {
            throw new RadamsaException("No baseValue provided");
        } else if (parameters.getOutputDirectoryPath() == null) {
            throw new RadamsaException("No output directory path provided");
        }

        final List<String> commandLine = new ArrayList<>(CommandExecutor.parseCommand(radamsaCommand));

        Optional
                .ofNullable(parameters.getCount())
                .filter(count -> count > 0)
                .ifPresent(count -> {
                    commandLine.add("-n");
                    commandLine.add(String.valueOf(count));
                });

        Optional
                .ofNullable(parameters.getSeed())
                .ifPresent(seed -> {
                    commandLine.add("-s");
                    commandLine.add(String.valueOf(seed));
                });

        final String outputPattern = parameters
                .getOutputDirectoryPath()
                .resolve("radamsa_%n.out")
                .toString()
                .replace("\\", "/");

        commandLine.add("-o");
        commandLine.add(outputPattern);

        try {
            commandExecutor.execute(commandLine, parameters.getBaseValue());
        } catch (IOException e) {
            throw new RadamsaException("Failed to execute radamsa", e);
        }
    }

    boolean isValidRadamsaCommand(final String command) throws RadamsaException {
        if (command == null || command.trim().isEmpty()) {
            return false;
        } else if (!command.matches("^.*radamsa(\\.[a-zA-Z]+)?$")) {
            return false;
        }

        final List<String> versionCommand = CommandExecutor.parseCommand(command);
        versionCommand.add("-V");

        final Optional<String> radamsaVersion;
        try {
            final ExecutionResult executionResult = commandExecutor.execute(versionCommand);

            if (executionResult.getExitCode() != 0) {
                return false;
            }

            radamsaVersion = executionResult.getOutput()
                    .filter(output -> output.toLowerCase().startsWith("radamsa"))
                    .map(output -> output.split(" ", 3))
                    .map(outputParts -> outputParts[1]);
        } catch (IOException e) {
            throw new RadamsaException(e);
        }

        if (radamsaVersion.isPresent()) {
            LOGGER.debug("Detected radamsa v{}", radamsaVersion.get());

            return true;
        } else {
            LOGGER.warn("Unable to determine version of radamsa");

            return false;
        }
    }

}
