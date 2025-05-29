package io.github.jack1424.realtimeweather;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class RealTimeWeather extends JavaPlugin {
	private final Logger logger = getLogger();

	@Override
	public void onEnable() {
		logger.info("Starting...");

		// TODO: Revamp metrics?
		Metrics metrics = new Metrics(this, 16709);
		/*
		metrics.addCustomChart(new SimplePie("weather_sync_enabled", () -> String.valueOf(config.isWeatherEnabled())));
		metrics.addCustomChart(new SimplePie("sunrise_sunset_source", () -> String.valueOf(config.getSunriseSunset())));
		metrics.addCustomChart(new SimplePie("time_sync_enabled", () -> String.valueOf(config.isTimeEnabled())));
		*/

		logger.info("Started!");
	}

	@Override
	public void onDisable() {
		logger.info("Stopping...");
	}
}
