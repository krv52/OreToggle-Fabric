package me.krv.oretoggle;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class OreToggleFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		OreDefinitions definitions = new OreDefinitions();
		OreStateManager stateManager = new OreStateManager();
		ReplacedOreTracker replacedOreTracker = new ReplacedOreTracker();
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve("oretoggle-config.json");
		Path storagePath = FabricLoader.getInstance().getConfigDir().resolve("oretoggle-replaced-blocks.json");
		OreToggleConfig config = OreToggleConfig.load(configPath);
		OreStorage storage = new JsonOreStorage(storagePath);

		replacedOreTracker.load(storage.load());
		new OreToggleCommands(definitions, stateManager, replacedOreTracker, storage).register();
		new OreScanPrototype(definitions, stateManager, replacedOreTracker, config).register();
		new OreAutosave(replacedOreTracker, storage, config).register();
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			storage.save(replacedOreTracker.snapshot());
			replacedOreTracker.markSaved();
		});
		System.out.println("OreToggle Fabric loaded");
	}
}
