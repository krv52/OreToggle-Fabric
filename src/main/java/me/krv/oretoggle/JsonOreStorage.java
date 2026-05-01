package me.krv.oretoggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class JsonOreStorage implements OreStorage {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    JsonOreStorage(Path path) {
        this.path = path;
    }

    @Override
    public List<ReplacedOreBlock> load() {
        if (!Files.exists(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            StoredBlock[] storedBlocks = gson.fromJson(reader, StoredBlock[].class);
            if (storedBlocks == null) {
                return List.of();
            }

            List<ReplacedOreBlock> loaded = new ArrayList<>();
            for (StoredBlock storedBlock : storedBlocks) {
                ReplacedOreBlock replacedBlock = storedBlock.toReplacedOreBlock();
                if (replacedBlock != null) {
                    loaded.add(replacedBlock);
                }
            }
            System.out.println("[OreToggle] Loaded " + loaded.size() + " replaced ore blocks from " + path + ".");
            return loaded;
        } catch (IOException exception) {
            System.out.println("[OreToggle] Failed to load " + path + ": " + exception.getMessage());
            return List.of();
        }
    }

    @Override
    public void save(List<ReplacedOreBlock> replacedBlocks) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StoredBlock[] storedBlocks = replacedBlocks.stream()
                    .map(StoredBlock::from)
                    .toArray(StoredBlock[]::new);

            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(Arrays.asList(storedBlocks), writer);
            }
            System.out.println("[OreToggle] Saved " + replacedBlocks.size() + " replaced ore blocks to " + path + ".");
        } catch (IOException exception) {
            System.out.println("[OreToggle] Failed to save " + path + ": " + exception.getMessage());
        }
    }

    private static Block blockFromId(String blockId) {
        Identifier id = Identifier.of(blockId);
        return Registries.BLOCK.get(id);
    }

    private static String blockId(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }

    private record StoredBlock(
            String oreKey,
            String worldKey,
            int x,
            int y,
            int z,
            String originalBlockId,
            String replacementBlockId
    ) {
        static StoredBlock from(ReplacedOreBlock block) {
            return new StoredBlock(
                    block.oreKey(),
                    block.worldKey(),
                    block.pos().getX(),
                    block.pos().getY(),
                    block.pos().getZ(),
                    blockId(block.originalState().getBlock()),
                    blockId(block.replacementState().getBlock())
            );
        }

        ReplacedOreBlock toReplacedOreBlock() {
            if (oreKey == null || worldKey == null || originalBlockId == null || replacementBlockId == null) {
                return null;
            }

            Block originalBlock = blockFromId(originalBlockId);
            Block replacementBlock = blockFromId(replacementBlockId);
            return new ReplacedOreBlock(
                    oreKey,
                    worldKey,
                    new BlockPos(x, y, z),
                    originalBlock.getDefaultState(),
                    replacementBlock.getDefaultState()
            );
        }
    }
}
