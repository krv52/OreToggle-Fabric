package me.krv.oretoggle;

import net.minecraft.block.Block;

import java.util.Set;

record OreDefinition(String key, String displayName, Set<Block> blocks) {
    boolean matches(Block block) {
        return blocks.contains(block);
    }
}
