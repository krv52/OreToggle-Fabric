package me.krv.oretoggle;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

import java.util.Locale;

final class OreToggleCommands {
    private final OreDefinitions definitions;
    private final OreStateManager stateManager;

    OreToggleCommands(OreDefinitions definitions, OreStateManager stateManager) {
        this.definitions = definitions;
        this.stateManager = stateManager;
    }

    void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("toggleore")
                        .then(CommandManager.argument("ore", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    definitions.keys().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("on");
                                            builder.suggest("off");
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String oreKey = StringArgumentType.getString(context, "ore").toLowerCase(Locale.ROOT);
                                            String mode = StringArgumentType.getString(context, "mode").toLowerCase(Locale.ROOT);
                                            OreDefinition definition = definitions.get(oreKey);

                                            if (definition == null) {
                                                context.getSource().sendFeedback(
                                                        () -> Text.literal("Unknown ore. Available: " + String.join(", ", definitions.keys())),
                                                        false
                                                );
                                                return 0;
                                            }

                                            if (!mode.equals("on") && !mode.equals("off")) {
                                                context.getSource().sendFeedback(
                                                        () -> Text.literal("Second argument must be on or off."),
                                                        false
                                                );
                                                return 0;
                                            }

                                            boolean disabled = mode.equals("off");
                                            stateManager.setDisabled(definition.key(), disabled);
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal(messageFor(definition, disabled)),
                                                    true
                                            );
                                            return 1;
                                        }))))
        );
    }

    private String messageFor(OreDefinition definition, boolean disabled) {
        if (disabled) {
            return definition.displayName() + " is now disabled in memory. Block scanning is not migrated yet.";
        }
        return definition.displayName() + " is now enabled in memory.";
    }
}
