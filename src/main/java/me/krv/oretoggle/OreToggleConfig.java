package me.krv.oretoggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

final class OreToggleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int blocksPerTick = 512;
    private int horizontalScanRadius = 64;
    private int verticalScanRadius = 32;
    private int autosaveSeconds = 60;
    private int maxTrackedBlocks = 0;
    private Integer scanRadiusChunks;

    static OreToggleConfig load(Path path) {
        if (!Files.exists(path)) {
            OreToggleConfig config = new OreToggleConfig();
            config.save(path);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            OreToggleConfig config = GSON.fromJson(reader, OreToggleConfig.class);
            if (config == null) {
                config = new OreToggleConfig();
            }
            config.clampValues();
            config.save(path);
            return config;
        } catch (IOException exception) {
            System.out.println("[OreToggle] Failed to load config, using defaults: " + exception.getMessage());
            return new OreToggleConfig();
        }
    }

    int blocksPerTick() {
        return blocksPerTick;
    }

    int horizontalScanRadius() {
        return horizontalScanRadius;
    }

    int verticalScanRadius() {
        return verticalScanRadius;
    }

    int autosaveSeconds() {
        return autosaveSeconds;
    }

    int maxTrackedBlocks() {
        return maxTrackedBlocks;
    }

    private void save(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            System.out.println("[OreToggle] Failed to create config: " + exception.getMessage());
        }
    }

    private void clampValues() {
        if (horizontalScanRadius <= 0 && scanRadiusChunks != null) {
            horizontalScanRadius = Math.max(0, scanRadiusChunks) * 16;
        }
        if (horizontalScanRadius <= 0) {
            horizontalScanRadius = 48;
        }

        blocksPerTick = Math.max(1, blocksPerTick);
        if (verticalScanRadius <= 0) {
            verticalScanRadius = 64;
        }
        autosaveSeconds = Math.max(1, autosaveSeconds);
        maxTrackedBlocks = Math.max(0, maxTrackedBlocks);
        scanRadiusChunks = null;
    }
}
