package me.krv.oretoggle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

final class OreAutosave {
    private static final int DEFAULT_INTERVAL_SECONDS = 60;

    private final ReplacedOreTracker tracker;
    private final OreStorage storage;
    private final int intervalTicks;
    private int ticksUntilSave;

    OreAutosave(ReplacedOreTracker tracker, OreStorage storage) {
        this.tracker = tracker;
        this.storage = storage;
        this.intervalTicks = DEFAULT_INTERVAL_SECONDS * 20;
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
