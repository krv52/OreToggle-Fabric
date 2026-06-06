package me.krv.oretoggle;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;

final class OreToggleCommands {
    private final OreDefinitions definitions;
    private final OreStateManager stateManager;
    private final ReplacedOreTracker replacedOreTracker;
    private final OreStorage storage;
    private final JsonDisabledOreStorage disabledOreStorage;

    OreToggleCommands(
            OreDefinitions definitions,
            OreStateManager stateManager,
            ReplacedOreTracker replacedOreTracker,
            OreStorage storage,
            JsonDisabledOreStorage disabledOreStorage
    ) {
        this.definitions = definitions;
        this.stateManager = stateManager;
        this.replacedOreTracker = replacedOreTracker;
        this.storage = storage;
        this.disabledOreStorage = disabledOreStorage;
    }

    void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
                                            if (!hasOreTogglePermission(context.getSource())) {
                                                return 0;
                                            }

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
                                            if (stateManager.setDisabled(definition.key(), disabled)) {
                                                disabledOreStorage.save(stateManager.disabledOreKeys());
                                            }
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal(messageFor(definition, disabled)),
                                                    true
                                            );
                                            return 1;
                                        })))
            );

            dispatcher.register(CommandManager.literal("restoreore")
                    .then(CommandManager.argument("ore", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                definitions.keys().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                if (!hasOreTogglePermission(context.getSource())) {
                                    return 0;
                                }

                                String oreKey = StringArgumentType.getString(context, "ore").toLowerCase(Locale.ROOT);
                                OreDefinition definition = definitions.get(oreKey);

                                if (definition == null) {
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("Unknown ore. Available: " + String.join(", ", definitions.keys())),
                                            false
                                    );
                                    return 0;
                                }

                                RestoreResult result = replacedOreTracker.restore(definition.key(), context.getSource().getServer());
                                context.getSource().sendFeedback(
                                        () -> Text.literal("Restore completed for " + definition.displayName() + ":\n"
                                                + "- Restored: " + result.restored() + "\n"
                                                + "- Skipped: " + result.skipped() + "\n"
                                                + "  - Unloaded chunks: " + result.skippedUnloaded() + "\n"
                                                + "  - Changed blocks: " + result.skippedChanged()),
                                        true
                                );
                                return result.restored();
                            }))
            );

            dispatcher.register(CommandManager.literal("oretoggle")
                    .then(CommandManager.literal("save")
                            .executes(context -> {
                                if (!hasOreTogglePermission(context.getSource())) {
                                    return 0;
                                }

                                storage.save(replacedOreTracker.snapshot());
                                replacedOreTracker.markSaved();
                                context.getSource().sendFeedback(
                                        () -> Text.literal("OreToggle replaced block data saved."),
                                        true
                                );
                                return 1;
                            }))
                    .then(CommandManager.literal("status")
                            .executes(context -> {
                                if (!hasOreTogglePermission(context.getSource())) {
                                    return 0;
                                }

                                context.getSource().sendFeedback(
                                        () -> Text.literal(disabledOresMessage()),
                                        false
                                );
                                context.getSource().sendFeedback(
                                        () -> Text.literal("Tracked blocks: " + replacedOreTracker.size()),
                                        false
                                );
                                return 1;
                            }))
            );
        });
    }

    private boolean hasOreTogglePermission(ServerCommandSource source) {
        if (CommandManager.GAMEMASTERS_CHECK.allows(source.getPermissions())) {
            return true;
        }
        source.sendFeedback(
                () -> Text.literal("You do not have permission to use OreToggle commands."),
                false
        );
        return false;
    }

    private String disabledOresMessage() {
        if (!stateManager.hasDisabledOres()) {
            return "Disabled ores: none";
        }
        return "Disabled ores: " + String.join(", ", stateManager.disabledOreKeys());
    }

    private String messageFor(OreDefinition definition, boolean disabled) {
        if (disabled) {
            return definition.displayName() + " is now disabled. Nearby matching ores will be replaced.";
        }
        return definition.displayName() + " is now enabled. Existing replaced blocks can be restored with /restoreore " + definition.key() + ".";
    }
}
