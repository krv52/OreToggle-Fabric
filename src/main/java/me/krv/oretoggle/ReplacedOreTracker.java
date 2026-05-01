package me.krv.oretoggle;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

final class ReplacedOreTracker {
    private final Map<String, ReplacedOreBlock> replacedBlocks = new HashMap<>();

    boolean remember(String oreKey, String worldKey, BlockPos pos, BlockState originalState) {
        String key = key(worldKey, pos);
        replacedBlocks.put(key, new ReplacedOreBlock(oreKey, worldKey, pos, originalState));
        return true;
    }

    private String key(String worldKey, BlockPos pos) {
        return worldKey + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
