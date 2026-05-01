package me.krv.oretoggle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class OreScanPrototype {
    private final OreDefinitions definitions;
    private final OreStateManager stateManager;
    private final ReplacedOreTracker replacedOreTracker;
    private final OreToggleConfig config;
    private final List<RelativeBlockOffset> scanOffsets;
    private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
    private int offsetIndex;
    private int lastSeenStateVersion = -1;
    private String lastWorldKey = "";
    private int lastPlayerX;
    private int lastPlayerY;
    private int lastPlayerZ;

    OreScanPrototype(
            OreDefinitions definitions,
            OreStateManager stateManager,
            ReplacedOreTracker replacedOreTracker,
            OreToggleConfig config
    ) {
        this.definitions = definitions;
        this.stateManager = stateManager;
        this.replacedOreTracker = replacedOreTracker;
        this.config = config;
        this.scanOffsets = createScanOffsets(config.scanRadiusChunks());
    }

    void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::scanNearOnePlayer);
    }

    private void scanNearOnePlayer(MinecraftServer server) {
        if (!stateManager.hasDisabledOres()) {
            return;
        }

        ServerPlayerEntity player = firstPlayer(server);
        if (player == null) {
            return;
        }

        ServerWorld world = player.getEntityWorld();
        resetCursorIfScanOriginChanged(world, player);
        scanSomeBlocks(world, player);
    }

    private ServerPlayerEntity firstPlayer(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return null;
        }
        return server.getPlayerManager().getPlayerList().getFirst();
    }

    private void resetCursorIfScanOriginChanged(ServerWorld world, ServerPlayerEntity player) {
        String worldKey = world.getRegistryKey().getValue().toString();
        boolean stateChanged = lastSeenStateVersion != stateManager.version();
        boolean worldChanged = !lastWorldKey.equals(worldKey);
        boolean playerMoved = lastPlayerX != player.getBlockX()
                || lastPlayerY != player.getBlockY()
                || lastPlayerZ != player.getBlockZ();

        if (stateChanged || worldChanged || playerMoved) {
            offsetIndex = 0;
            lastSeenStateVersion = stateManager.version();
            lastWorldKey = worldKey;
            lastPlayerX = player.getBlockX();
            lastPlayerY = player.getBlockY();
            lastPlayerZ = player.getBlockZ();
        }
    }

    private void scanSomeBlocks(ServerWorld world, ServerPlayerEntity player) {
        int minY = world.getBottomY();
        int topY = minY + world.getHeight() - 1;
        int playerY = Math.clamp(player.getBlockY(), minY, topY);

        for (int checked = 0; checked < config.blocksPerTick(); checked++) {
            RelativeBlockOffset offset = scanOffsets.get(offsetIndex);
            int blockX = player.getBlockX() + offset.x();
            int blockY = Math.clamp(playerY + offset.y(), minY, topY);
            int blockZ = player.getBlockZ() + offset.z();

            offsetIndex++;
            if (offsetIndex >= scanOffsets.size()) {
                offsetIndex = 0;
            }

            if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                continue;
            }

            scanPos.set(blockX, blockY, blockZ);
            BlockState state = world.getBlockState(scanPos);
            replaceIfDisabledOreFound(world, state, blockX, blockY, blockZ);
        }
    }

    private static List<RelativeBlockOffset> createScanOffsets(int scanRadiusChunks) {
        int radius = scanRadiusChunks * 16;
        List<RelativeBlockOffset> offsets = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    offsets.add(new RelativeBlockOffset(x, y, z, square(x) + square(y) + square(z)));
                }
            }
        }

        offsets.sort(Comparator.comparingInt(RelativeBlockOffset::distanceSquared));
        return List.copyOf(offsets);
    }

    private void replaceIfDisabledOreFound(ServerWorld world, BlockState state, int x, int y, int z) {
        Block block = state.getBlock();

        for (OreDefinition definition : definitions.all()) {
            if (!stateManager.isDisabled(definition.key()) || !definition.matches(block)) {
                continue;
            }

            String worldKey = world.getRegistryKey().getValue().toString();
            BlockPos pos = new BlockPos(x, y, z);
            Block replacement = replacementFor(block);
            BlockState replacementState = replacement.getDefaultState();
            if (replacedOreTracker.remember(definition.key(), worldKey, pos, state, replacementState, config.maxTrackedBlocks())) {
                world.setBlockState(pos, replacementState);
                System.out.println("[OreToggle] Replaced disabled " + definition.displayName()
                        + " at " + x + " " + y + " " + z + " in " + worldKey
                        + " with " + replacement.getName().getString() + ".");
            }
        }
    }

    private Block replacementFor(Block oreBlock) {
        if (oreBlock == Blocks.DEEPSLATE_COAL_ORE
                || oreBlock == Blocks.DEEPSLATE_IRON_ORE
                || oreBlock == Blocks.DEEPSLATE_GOLD_ORE
                || oreBlock == Blocks.DEEPSLATE_REDSTONE_ORE
                || oreBlock == Blocks.DEEPSLATE_LAPIS_ORE
                || oreBlock == Blocks.DEEPSLATE_DIAMOND_ORE
                || oreBlock == Blocks.DEEPSLATE_EMERALD_ORE
                || oreBlock == Blocks.DEEPSLATE_COPPER_ORE) {
            return Blocks.DEEPSLATE;
        }

        if (oreBlock == Blocks.NETHER_QUARTZ_ORE
                || oreBlock == Blocks.NETHER_GOLD_ORE
                || oreBlock == Blocks.ANCIENT_DEBRIS) {
            return Blocks.NETHERRACK;
        }

        return Blocks.STONE;
    }

    private static int square(int value) {
        return value * value;
    }

    private record RelativeBlockOffset(int x, int y, int z, int distanceSquared) {
    }
}
