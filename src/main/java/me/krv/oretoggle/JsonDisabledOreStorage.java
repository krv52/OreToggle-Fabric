package me.krv.oretoggle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class JsonDisabledOreStorage {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    JsonDisabledOreStorage(Path path) {
        this.path = path;
    }

    List<String> load() {
        if (!Files.exists(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            String[] disabledOreKeys = gson.fromJson(reader, String[].class);
            if (disabledOreKeys == null) {
                return List.of();
            }
            return Arrays.stream(disabledOreKeys)
                    .filter(key -> key != null && !key.isBlank())
                    .map(key -> key.toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();
        } catch (IOException | JsonSyntaxException exception) {
            System.out.println("[OreToggle] Failed to load " + path + ": " + exception.getMessage());
            return List.of();
        }
    }

    void save(List<String> disabledOreKeys) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(disabledOreKeys, writer);
            }
            System.out.println("[OreToggle] Saved " + disabledOreKeys.size() + " disabled ore keys to " + path + ".");
        } catch (IOException exception) {
            System.out.println("[OreToggle] Failed to save " + path + ": " + exception.getMessage());
        }
    }
}
