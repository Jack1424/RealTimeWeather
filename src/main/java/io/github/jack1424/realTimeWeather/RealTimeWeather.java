package io.github.jack1424.realTimeWeather;

import io.github.jack1424.realTimeWeather.requests.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.ConfigurationException;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.logging.Logger;

public final class RealTimeWeather extends JavaPlugin {
	private Logger logger;
	private ConfigManager config;

	@Override
	public void onEnable() {
		logger = getLogger();
		logger.info("Starting...");

		logger.info("Loading configuration...");
		saveDefaultConfig();
		config = new ConfigManager(this);
		config.refreshValues();

		debug("TimeSync: " + config.isTimeEnabled());
		if (config.isTimeEnabled())
			setupTime();

		debug("WeatherSync: " + config.isWeatherEnabled());
		if (config.isWeatherEnabled())
			setupWeather();

		setupPlaceholderAPI();

		getServer().getPluginManager().registerEvents(new EventHandlers(this), this);

		RealTimeWeatherCommand rtwCommand = new RealTimeWeatherCommand(this);
		this.getCommand("realtimeweather").setExecutor(rtwCommand);
		this.getCommand("realtimeweather").setTabCompleter(rtwCommand);

		debug("Enabling metrics...");
		Metrics metrics = new Metrics(this, 16709);
		metrics.addCustomChart(new SimplePie("weather_sync_enabled", () -> String.valueOf(config.isWeatherEnabled())));
		metrics.addCustomChart(new SimplePie("sunrise_sunset_source", () -> String.valueOf(config.getSunriseSunset())));
		metrics.addCustomChart(new SimplePie("time_sync_enabled", () -> String.valueOf(config.isTimeEnabled())));

		logger.info("Started!");

		logger.info("Checking for updates...");
		logger.info(getUpdateCheck());

		long updateCheckInterval = config.getUpdateCheckInterval();
		if (config.getUpdateCheckInterval() > 0)
			getServer().getScheduler().scheduleSyncRepeatingTask(this,  () -> logger.info(getUpdateCheck()), updateCheckInterval, updateCheckInterval);
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

		if (config.getSunriseSunset().equals("real"))
			debug("Syncing sunrise/sunset with " + config.getSunriseSunsetLatitude() + " " + config.getSunriseSunsetLongitude());

		if (config.getSunriseSunset().equals("custom"))
			debug("Using custom sunrise/sunset times. Sunrise: " + config.getSunriseCustomTime() + ", Sunset: " + config.getSunsetCustomTime());

		for (World world : config.getTimeSyncWorlds())
			world.setGameRuleValue("doDaylightCycle", "false");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			if (config.isTimeEnabled()) {
				Calendar cal = Calendar.getInstance(config.getTimeZone());
				for (World world : config.getTimeSyncWorlds())
					if (config.getSunriseSunset().equals("real")) {
						SunriseSunsetRequestObject sunriseSunset;
						try {
							sunriseSunset = new SunriseSunsetRequestObject(config.getTimeZone(), config.getSunriseSunsetLatitude(), config.getSunriseSunsetLongitude());
							world.setTime(calculateWorldTime(cal, sunriseSunset.getSunriseTime(), sunriseSunset.getSunsetTime()));
						} catch (Exception e) {
							logger.severe(e.getMessage());
							logger.severe("Error getting sunrise/sunset times, using default sunrise/sunset times");

							try {
								config.setSunriseSunset("default");
							} catch (ConfigurationException ex) {
								throw new RuntimeException(ex);
							}

							world.setTime(calculateWorldTime(cal, "5:02:27 AM", "6:36:36 PM"));
							return;
						}
					} else if (config.getSunriseSunset().equals("custom")) {
						world.setTime(calculateWorldTime(cal, config.getSunriseCustomTime(), config.getSunsetCustomTime()));
					} else
						world.setTime(calculateWorldTime(cal, "5:02:27 AM", "6:36:36 PM"));
			}
		}, 0L, config.getTimeSyncInterval());

		debug("Weather sync enabled");
	}

	private void setupPlaceholderAPI() {
		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new io.github.jack1424.realTimeWeather.placeholders.RealTimeWeatherExpansion(this).register();
			debug("PlaceholderAPI detected; placeholders enabled");
		} else {
			debug("PlaceholderAPI not found; placeholders disabled");
		}
	}

	private void setupWeather() {
		debug("Enabling weather sync...");

		try {
			new WeatherRequestObject(config.getAPIKey(), config.getWeatherLatitude(), config.getWeatherLongitude());
		} catch (Exception e) {
			logger.severe(e.getMessage());
			logger.severe("Disabling weather sync...");

			config.setWeatherEnabled(false);
			return;
		}

		for (World world : config.getWeatherSyncWorlds())
			world.setGameRuleValue("doWeatherCycle", "false");

		getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
			debug("Syncing weather...");

			try {
				WeatherRequestObject request = new WeatherRequestObject(config.getAPIKey(), config.getWeatherLatitude(), config.getWeatherLongitude());
				boolean rain = request.isRaining();
				boolean thunder = request.isThundering();

				getServer().getScheduler().runTask(this, () -> {
					debug("Setting weather (Rain: " + rain + ", Thunder: " + thunder + ")...");
					for (World world : config.getWeatherSyncWorlds()) {
						world.setStorm(rain);
						world.setThundering(thunder);
					}
				});
			} catch (Exception e) {
				logger.severe("There was an error when attempting to get weather information");
				debug(e.getMessage());
			}
		}, 0L, config.getWeatherSyncInterval());

		debug("Weather sync enabled");
	}

	private long calculateWorldTime(Calendar cal, String sunriseTime, String sunsetTime) {
		String[] sunriseTimeSplit = sunriseTime.split(":");
		String[] sunsetTimeSplit = sunsetTime.split(":");

		long sunriseMinutes = Long.parseLong(sunriseTimeSplit[0]) * 60 + Long.parseLong(sunriseTimeSplit[1]) + Long.parseLong(sunriseTimeSplit[2].substring(0, 2)) / 60;
		long sunsetMinutes = Long.parseLong(sunsetTimeSplit[0]) * 60 + Long.parseLong(sunsetTimeSplit[1]) + Long.parseLong(sunsetTimeSplit[2].substring(0, 2)) / 60;

		if (sunriseTimeSplit[2].substring(3).equalsIgnoreCase("PM"))
			sunriseMinutes += 720;
		if (sunsetTimeSplit[2].substring(3).equalsIgnoreCase("PM"))
			sunsetMinutes += 720;

		LocalTime currentTime = LocalTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
		double currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();

		if (currentMinutes >= sunriseMinutes && currentMinutes < sunsetMinutes) {
			return (long) (((currentMinutes - sunriseMinutes) / (sunsetMinutes - sunriseMinutes)) * 13569) + 23041;
		} else {
			if (currentMinutes < sunriseMinutes)
				currentMinutes += 1440;
			
			return (long) (((currentMinutes - sunsetMinutes) / (1440 - sunsetMinutes + sunriseMinutes)) * 13569) + 12610;
		}
	}

	public String getUpdateCheck() {
		String currentVersion = this.getDescription().getVersion();
		String latestVersion;
		try {
			debug("Getting latest version...");
			latestVersion = RequestFunctions.getLatestVersion();
		} catch (Exception exception) {
			debug(exception.getMessage());
			return "There was an error getting the latest version";
		}

		if (currentVersion.equals(latestVersion)) {
			return String.format("RealTimeWeather (v%s) is up to date!", currentVersion);
		} else
			return String.format("RealTimeWeather (v%s) is outdated! v%s is the latest version.", currentVersion, latestVersion);
	}

	public void reloadPlugin() {
		debug("Reloading plugin...");
		
		// Reset gamerules for currently managed worlds before reloading config
		for (World world : getServer().getWorlds()) {
			if (world.getEnvironment().equals(World.Environment.NORMAL)) {
				if (config.isTimeEnabled() && config.getTimeSyncWorlds() != null && config.getTimeSyncWorlds().contains(world)) {
					world.setGameRuleValue("doDaylightCycle", "true");
				}
				if (config.isWeatherEnabled() && config.getWeatherSyncWorlds() != null && config.getWeatherSyncWorlds().contains(world)) {
					world.setGameRuleValue("doWeatherCycle", "true");
				}
			}
		}

		// Reload configuration file
		reloadConfig();
		config.refreshValues();

		// Rebuild all tasks
		rebuildTasks();
		
		logger.info("Plugin successfully reloaded.");
	}

	public void toggleTimeSync(boolean enable) {
		if (!enable) {
			for (World world : getServer().getWorlds()) {
				if (world.getEnvironment().equals(World.Environment.NORMAL)) {
					if (config.isTimeEnabled() && config.getTimeSyncWorlds() != null && config.getTimeSyncWorlds().contains(world)) {
						world.setGameRuleValue("doDaylightCycle", "true");
					}
				}
			}
		}

		config.setTimeEnabled(enable);
		getConfig().set("SyncTime", enable);
		saveConfig();

		rebuildTasks();
	}

	public void toggleWeatherSync(boolean enable) {
		if (!enable) {
			for (World world : getServer().getWorlds()) {
				if (world.getEnvironment().equals(World.Environment.NORMAL)) {
					if (config.isWeatherEnabled() && config.getWeatherSyncWorlds() != null && config.getWeatherSyncWorlds().contains(world)) {
						world.setGameRuleValue("doWeatherCycle", "true");
					}
				}
			}
		}

		config.setWeatherEnabled(enable);
		getConfig().set("SyncWeather", enable);
		saveConfig();

		rebuildTasks();
	}

	private void rebuildTasks() {
		getServer().getScheduler().cancelTasks(this);

		debug("TimeSync: " + config.isTimeEnabled());
		if (config.isTimeEnabled()) {
			setupTime();
		}

		debug("WeatherSync: " + config.isWeatherEnabled());
		if (config.isWeatherEnabled()) {
			setupWeather();
		}

		long updateCheckInterval = config.getUpdateCheckInterval();
		if (updateCheckInterval > 0) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> logger.info(getUpdateCheck()), updateCheckInterval, updateCheckInterval);
		}
	}


	public ConfigManager getConfigManager() {
		return config;
	}

	public void debug(String message) {
		if (config.debugEnabled()) {
			logger.info("[DEBUG] " + message);
		}
	}
}
