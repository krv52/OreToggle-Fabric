package me.krv.oretoggle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class OreScanPrototype {
    private static final int BLOCKS_PER_TICK = 2048;
    private static final int SCAN_RADIUS_CHUNKS = 2;
    private static final List<ChunkOffset> CHUNK_OFFSETS = createChunkOffsets();

    private final OreDefinitions definitions;
    private final OreStateManager stateManager;
    private final ReplacedOreTracker replacedOreTracker;
    private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
    private int chunkOffsetIndex;
    private int columnIndex;
    private int yIndex;
    private int lastSeenStateVersion = -1;
    private String lastWorldKey = "";
    private int lastPlayerChunkX;
    private int lastPlayerChunkZ;

    OreScanPrototype(OreDefinitions definitions, OreStateManager stateManager, ReplacedOreTracker replacedOreTracker) {
        this.definitions = definitions;
        this.stateManager = stateManager;
        this.replacedOreTracker = replacedOreTracker;
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
        ChunkPos chunkPos = player.getChunkPos();
        if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return;
        }

        resetCursorIfScanOriginChanged(world, chunkPos);
        scanSomeBlocks(world, player, chunkPos);
    }

    private ServerPlayerEntity firstPlayer(MinecraftServer server) {
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return null;
        }
        return server.getPlayerManager().getPlayerList().getFirst();
    }

    private void resetCursorIfScanOriginChanged(ServerWorld world, ChunkPos playerChunkPos) {
        String worldKey = world.getRegistryKey().getValue().toString();
        boolean stateChanged = lastSeenStateVersion != stateManager.version();
        boolean worldChanged = !lastWorldKey.equals(worldKey);
        boolean chunkChanged = lastPlayerChunkX != playerChunkPos.x || lastPlayerChunkZ != playerChunkPos.z;

        if (stateChanged || worldChanged || chunkChanged) {
            resetCursor();
            lastSeenStateVersion = stateManager.version();
            lastWorldKey = worldKey;
            lastPlayerChunkX = playerChunkPos.x;
            lastPlayerChunkZ = playerChunkPos.z;
        }
    }

    private void resetCursor() {
        chunkOffsetIndex = 0;
        columnIndex = 0;
        yIndex = 0;
    }

    private void scanSomeBlocks(ServerWorld world, ServerPlayerEntity player, ChunkPos playerChunkPos) {
        int minY = world.getBottomY();
        int height = world.getHeight();
        int topY = minY + height - 1;
        List<ColumnOffset> columns = createColumnsClosestToPlayer(player);
        List<Integer> yLevels = createYLevelsClosestToPlayer(player, minY, topY);

        for (int checked = 0; checked < BLOCKS_PER_TICK; checked++) {
            ChunkOffset chunkOffset = CHUNK_OFFSETS.get(chunkOffsetIndex);
            int chunkX = playerChunkPos.x + chunkOffset.x();
            int chunkZ = playerChunkPos.z + chunkOffset.z();

            // Chunk order starts with the player's current chunk, then nearby chunks by distance.
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                moveCursor(columns.size(), yLevels.size());
                continue;
            }

            ColumnOffset column = columns.get(columnIndex);
            int blockX = (chunkX << 4) + column.x();
            int blockY = yLevels.get(yIndex);
            int blockZ = (chunkZ << 4) + column.z();
            scanPos.set(blockX, blockY, blockZ);

            BlockState state = world.getBlockState(scanPos);
            replaceIfDisabledOreFound(world, state, blockX, blockY, blockZ);
            moveCursor(columns.size(), yLevels.size());
        }
    }

    private static List<ChunkOffset> createChunkOffsets() {
        List<ChunkOffset> offsets = new ArrayList<>();
        for (int x = -SCAN_RADIUS_CHUNKS; x <= SCAN_RADIUS_CHUNKS; x++) {
            for (int z = -SCAN_RADIUS_CHUNKS; z <= SCAN_RADIUS_CHUNKS; z++) {
                offsets.add(new ChunkOffset(x, z));
            }
        }

        offsets.sort(Comparator.comparingInt(offset -> square(offset.x()) + square(offset.z())));
        return List.copyOf(offsets);
    }

    private List<ColumnOffset> createColumnsClosestToPlayer(ServerPlayerEntity player) {
        int playerLocalX = Math.floorMod(player.getBlockX(), 16);
        int playerLocalZ = Math.floorMod(player.getBlockZ(), 16);
        List<ColumnOffset> columns = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int distanceSquared = square(x - playerLocalX) + square(z - playerLocalZ);
                columns.add(new ColumnOffset(x, z, distanceSquared));
            }
        }

        // Inside each chunk, scan the horizontal columns closest to the player first.
        columns.sort(Comparator.comparingInt(ColumnOffset::distanceSquared));
        return columns;
    }

    private List<Integer> createYLevelsClosestToPlayer(ServerPlayerEntity player, int minY, int topY) {
        int playerY = Math.clamp(player.getBlockY(), minY, topY);
        List<Integer> yLevels = new ArrayList<>();

        for (int y = minY; y <= topY; y++) {
            yLevels.add(y);
        }

        // Inside each column, scan Y levels closest to the player's feet first.
        yLevels.sort(Comparator.comparingInt(y -> Math.abs(y - playerY)));
        return yLevels;
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
            if (replacedOreTracker.remember(definition.key(), worldKey, pos, state, replacementState)) {
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

    private void moveCursor(int columnCount, int yLevelCount) {
        yIndex++;
        if (yIndex < yLevelCount) {
            return;
        }

        yIndex = 0;
        columnIndex++;
        if (columnIndex < columnCount) {
            return;
        }

        columnIndex = 0;
        chunkOffsetIndex++;
        if (chunkOffsetIndex < CHUNK_OFFSETS.size()) {
            return;
        }

        chunkOffsetIndex = 0;
    }

    private static int square(int value) {
        return value * value;
    }

    private record ChunkOffset(int x, int z) {
    }

    private record ColumnOffset(int x, int z, int distanceSquared) {
    }
}
