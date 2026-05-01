package me.krv.oretoggle;

import java.util.List;

interface OreStorage {
    List<ReplacedOreBlock> load();

    void save(List<ReplacedOreBlock> replacedBlocks);
}
