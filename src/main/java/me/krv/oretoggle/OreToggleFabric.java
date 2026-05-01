package me.krv.oretoggle;

import net.fabricmc.api.ModInitializer;

public class OreToggleFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		OreDefinitions definitions = new OreDefinitions();
		OreStateManager stateManager = new OreStateManager();
		ReplacedOreTracker replacedOreTracker = new ReplacedOreTracker();

		new OreToggleCommands(definitions, stateManager, replacedOreTracker).register();
		new OreScanPrototype(definitions, stateManager, replacedOreTracker).register();
		System.out.println("OreToggle Fabric loaded");
	}
}
