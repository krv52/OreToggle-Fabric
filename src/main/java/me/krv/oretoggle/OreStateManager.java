package me.krv.oretoggle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class OreStateManager {
    private final Set<String> disabledOreKeys = new HashSet<>();
    private int version;

    boolean isDisabled(String oreKey) {
        return disabledOreKeys.contains(oreKey);
    }

    boolean setDisabled(String oreKey, boolean disabled) {
        boolean changed;
        if (disabled) {
            changed = disabledOreKeys.add(oreKey);
        } else {
            changed = disabledOreKeys.remove(oreKey);
        }

        if (changed) {
            version++;
        }
        return changed;
    }

    boolean hasDisabledOres() {
        return !disabledOreKeys.isEmpty();
    }

    List<String> disabledOreKeys() {
        return disabledOreKeys.stream().sorted().toList();
    }

    int version() {
        return version;
    }
}
