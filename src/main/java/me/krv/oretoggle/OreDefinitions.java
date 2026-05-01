package me.krv.oretoggle;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OreDefinitions {
    private final Map<String, OreDefinition> definitions = new LinkedHashMap<>();

    OreDefinitions() {
        add("coal", "Coal ore", Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        add("iron", "Iron ore", Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        add("gold", "Gold ore", Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        add("redstone", "Redstone ore", Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        add("lapis", "Lapis ore", Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        add("diamond", "Diamond ore", Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        add("emerald", "Emerald ore", Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        add("copper", "Copper ore", Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        add("quartz", "Quartz ore", Blocks.NETHER_QUARTZ_ORE);
        add("nether_gold", "Nether gold ore", Blocks.NETHER_GOLD_ORE);
        add("debris", "Ancient debris", Blocks.ANCIENT_DEBRIS);
    }

    OreDefinition get(String key) {
        return definitions.get(key);
    }

    Set<String> keys() {
        return definitions.keySet();
    }

    Collection<OreDefinition> all() {
        return definitions.values();
    }

    private void add(String key, String displayName, Block... blocks) {
        definitions.put(key, new OreDefinition(key, displayName, Set.copyOf(List.of(blocks))));
    }
}
