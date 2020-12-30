package com.github.ilooner.polygoncli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.github.ilooner.polygoncli.cmd.SourceCommand;
import com.github.ilooner.polygoncli.cmd.output.OutputCommand;
import com.github.ilooner.polygoncli.cmd.stocks.StocksCommand;
import com.github.ilooner.polygoncli.output.Outputter;
import lombok.ToString;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        final JCommander commander = createCommander();
        final Args rootArgs = ((Args) commander.getObjects().get(0));

        commander.parse(args);

        if (rootArgs.help) {
            commander.usage();
            return;
        }

        final var sourceName = commander.getParsedAlias();
        final var sourceCommander = commander.getCommands().get(sourceName);
        final var sourceCommand = (SourceCommand) sourceCommander.getObjects().get(0);
        final var outputName = sourceCommander.getParsedAlias();
        final var outputCommander = sourceCommander.getCommands().get(outputName);
        final var outputCommand = (OutputCommand) outputCommander.getObjects().get(0);

        final var schema = sourceCommand.getSchema();
        final Outputter<GenericRecord> outputter;

        try {
            outputter = outputCommand.getOutputter(schema);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return; // Make compiler happy
        }

        sourceCommand.run(outputter);
    }

    public static JCommander createCommander() {
        final JCommander root = new JCommander(new Args());

        for (SourceCommand sourceCommand: StocksCommand.SOURCE_COMMANDS) {
            final JCommander sourceCommander = addSourceCommand(root, sourceCommand);

            for (OutputCommand outputCommand: OutputCommand.COMMANDS) {
                sourceCommander.addCommand(outputCommand.getName(), outputCommand);
            }
        }

        return root;
    }

    public static JCommander addSourceCommand(final JCommander root,
                                              final SourceCommand sourceCommand) {
        JCommander current = root;

        for (int i = 0; i < (sourceCommand.getPath().size() - 1); i++) {
            final String pathComponent = sourceCommand.getPath().get(i);
            JCommander next = current.getCommands().get(pathComponent);

            if (next == null) {
                current.addCommand(pathComponent, new Object());
                next = current.getCommands().get(pathComponent);
            }

            current = next;
        }

        current.addCommand(sourceCommand.getName(), sourceCommand);
        return current.getCommands().get(sourceCommand.getName());
    }

    @ToString
    public static class Args {
        @Parameter(names = {"-h", "-help", "--help"}, description = "Get help.")
        private boolean help;
    }
}
