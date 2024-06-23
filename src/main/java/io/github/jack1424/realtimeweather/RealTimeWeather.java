package io.github.jack1424.realtimeweather;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Calendar;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public final class RealTimeWeather extends JavaPlugin {
	private Logger logger;
	private Configurator config;

	@Override
	public void onEnable() {
		logger = getLogger();
		logger.info("Starting...");

		logger.info("Loading configuration...");
		saveDefaultConfig();
		config = new Configurator(this);
		config.refreshValues();

		debug("TimeSync: " + config.isTimeEnabled());
		if (config.isTimeEnabled())
			setupTime();

		debug("WeatherSync: " + config.isWeatherEnabled());
		if (config.isWeatherEnabled())
			setupWeather();

		getServer().getPluginManager().registerEvents(new EventHandler(this), this);

		debug("Enabling metrics...");
		Metrics metrics = new Metrics(this, 16709);
		metrics.addCustomChart(new SimplePie("weather_sync_enabled", () -> String.valueOf(config.isWeatherEnabled())));
		metrics.addCustomChart(new SimplePie("time_sync_enabled", () -> String.valueOf(config.isTimeEnabled())));

		logger.info("Started!");
	}

	@Override
	public void onDisable() {
		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL)) {
				debug("Re-enabling normal daylight and weather cycles...");

				if (config.isTimeEnabled())
					world.setGameRuleValue("doDaylightCycle", "true");
				if (config.isWeatherEnabled())
					world.setGameRuleValue("doWeatherCycle", "true");
			}

		logger.info("Stopping...");
	}

	private void setupTime() {
		debug("Enabling time zone sync...");
		debug("Syncing time with " + config.getTimeZone().getDisplayName());

		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL))
				world.setGameRuleValue("doDaylightCycle", "false");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			if (config.isTimeEnabled()) {
				Calendar cal = Calendar.getInstance(config.getTimeZone());
				for (World world : getServer().getWorlds())
					if (world.getEnvironment().equals(World.Environment.NORMAL))
						world.setTime((1000 * cal.get(Calendar.HOUR_OF_DAY)) + (16 * (cal.get(Calendar.MINUTE) + 1)) - 6000);
			}
		}, 0L, config.getTimeSyncInterval());
	}

	private void setupWeather() {
		try {
			new RequestObject(config.getAPIKey(), config.getLat(), config.getLon());
		} catch (Exception e) {
			logger.severe(e.getMessage());
			logger.severe("Disabling weather sync...");

			config.setWeatherEnabled(false);
			return;
		}
		
		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL))
				world.setGameRuleValue("doWeatherCycle", "false");

		debug("Enabling weather sync...");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			debug("Syncing weather...");

			try {
				RequestObject request = new RequestObject(config.getAPIKey(), config.getLat(), config.getLon());

				debug("Setting weather (Rain: " + request.isRaining() + ", Thunder: " + request.isThundering() + ")...");
				for (World world : getServer().getWorlds())
					if (world.getEnvironment().equals(World.Environment.NORMAL)) {
						world.setStorm(request.isRaining());
						world.setThundering(request.isThundering());
					}
			} catch (Exception e) {
				logger.severe("There was an error when attempting to get weather information");
				debug(e.getMessage());
			}
		}, 0L, config.getWeatherSyncInterval());

		debug("Weather sync enabled");
	}

	public Configurator getConfigurator() {
		return config;
	}

	public void debug(String message) {
		if (config.debugEnabled()) {
			logger.info("[DEBUG] " + message);
		}
	}
}
