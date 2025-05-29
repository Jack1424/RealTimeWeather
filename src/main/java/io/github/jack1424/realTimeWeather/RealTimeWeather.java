package io.github.jack1424.realTimeWeather;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealTimeWeather extends JavaPlugin {

	@Override
	public void onEnable() {
		// TODO: Revamp metrics?
		Metrics metrics = new Metrics(this, 16709);
		/*
		metrics.addCustomChart(new SimplePie("weather_sync_enabled", () -> String.valueOf(config.isWeatherEnabled())));
		metrics.addCustomChart(new SimplePie("sunrise_sunset_source", () -> String.valueOf(config.getSunriseSunset())));
		metrics.addCustomChart(new SimplePie("time_sync_enabled", () -> String.valueOf(config.isTimeEnabled())));
		*/
	}

	@Override
	public void onDisable() {

	}
}
