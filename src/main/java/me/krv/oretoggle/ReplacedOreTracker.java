package me.krv.oretoggle;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ReplacedOreTracker {
    private final Map<String, ReplacedOreBlock> replacedBlocks = new HashMap<>();
    private boolean dirty;

    boolean remember(String oreKey, String worldKey, BlockPos pos, BlockState originalState, BlockState replacementState) {
        String key = key(worldKey, pos);
        replacedBlocks.put(key, new ReplacedOreBlock(oreKey, worldKey, pos, originalState, replacementState));
        dirty = true;
        return true;
    }

    void load(List<ReplacedOreBlock> blocks) {
        replacedBlocks.clear();
        for (ReplacedOreBlock block : blocks) {
            replacedBlocks.put(key(block.worldKey(), block.pos()), block);
        }
        dirty = false;
    }

    List<ReplacedOreBlock> snapshot() {
        return List.copyOf(replacedBlocks.values());
    }

    int size() {
        return replacedBlocks.size();
    }

    boolean isDirty() {
        return dirty;
    }

    void markSaved() {
        dirty = false;
    }

    RestoreResult restore(String oreKey, MinecraftServer server) {
        int restored = 0;
        int skipped = 0;
        List<String> entriesToRemove = new ArrayList<>();

        for (Map.Entry<String, ReplacedOreBlock> entry : replacedBlocks.entrySet()) {
            ReplacedOreBlock replacedBlock = entry.getValue();
            if (!replacedBlock.oreKey().equals(oreKey)) {
                continue;
            }

            ServerWorld world = worldFor(server, replacedBlock.worldKey());
            if (world == null || !world.isChunkLoaded(replacedBlock.pos().getX() >> 4, replacedBlock.pos().getZ() >> 4)) {
                skipped++;
                continue;
            }

            BlockState currentState = world.getBlockState(replacedBlock.pos());
            if (!currentState.equals(replacedBlock.replacementState())) {
                entriesToRemove.add(entry.getKey());
                skipped++;
                continue;
            }

            world.setBlockState(replacedBlock.pos(), replacedBlock.originalState());
            entriesToRemove.add(entry.getKey());
            restored++;
            System.out.println("[OreToggle] Restored " + oreKey + " at "
                    + replacedBlock.pos().getX() + " "
                    + replacedBlock.pos().getY() + " "
                    + replacedBlock.pos().getZ() + " in " + replacedBlock.worldKey() + ".");
        }

        if (!entriesToRemove.isEmpty()) {
            for (String key : entriesToRemove) {
                replacedBlocks.remove(key);
            }
            dirty = true;
        }

        return new RestoreResult(restored, skipped);
    }

    private ServerWorld worldFor(MinecraftServer server, String worldKey) {
        Identifier id = Identifier.of(worldKey);
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        return server.getWorld(key);
    }

    private String key(String worldKey, BlockPos pos) {
        return worldKey + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
