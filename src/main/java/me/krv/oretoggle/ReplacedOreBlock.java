package me.krv.oretoggle;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

record ReplacedOreBlock(
        String oreKey,
        String worldKey,
        BlockPos pos,
        BlockState originalState,
        BlockState replacementState
) {
}
