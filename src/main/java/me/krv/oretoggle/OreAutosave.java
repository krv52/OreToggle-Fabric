package me.krv.oretoggle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

final class OreAutosave {
    private final ReplacedOreTracker tracker;
    private final OreStorage storage;
    private final int intervalTicks;
    private int ticksUntilSave;

    OreAutosave(ReplacedOreTracker tracker, OreStorage storage, OreToggleConfig config) {
        this.tracker = tracker;
        this.storage = storage;
        this.intervalTicks = config.autosaveSeconds() * 20;
        this.ticksUntilSave = intervalTicks;
    }

    void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    private void tick() {
        ticksUntilSave--;
        if (ticksUntilSave > 0) {
            return;
        }

        ticksUntilSave = intervalTicks;
        if (!tracker.isDirty()) {
            return;
        }

        storage.save(tracker.snapshot());
        tracker.markSaved();
    }
}
