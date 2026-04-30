package io.github.jack1424.realTimeWeather;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class RealTimeWeather extends JavaPlugin {
	private Logger logger;

	@Override
	public void onEnable() {
		logger = getLogger();
		logger.info("Starting...");
	}

	@Override
	public void onDisable() {
		logger.info("Stopping...");
	}
}
