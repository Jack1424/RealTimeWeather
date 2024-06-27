package io.github.jack1424.realtimeweather;

import io.github.jack1424.realtimeweather.requests.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.ConfigurationException;
import java.time.LocalTime;
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

		if (config.getSunriseSunset().equals("real"))
			debug("Syncing sunrise/sunset with " + config.getSunriseSunsetLatitude() + " " + config.getSunriseSunsetLongitude());

		if (config.getSunriseSunset().equals("custom"))
			debug("Using custom sunrise/sunset times. Sunrise: " + config.getSunriseCustomTime() + ", Sunset: " + config.getSunsetCustomTime());

		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL))
				world.setGameRuleValue("doDaylightCycle", "false");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			if (config.isTimeEnabled()) {
				Calendar cal = Calendar.getInstance(config.getTimeZone());
				for (World world : getServer().getWorlds())
					if (world.getEnvironment().equals(World.Environment.NORMAL))
						if (config.getSunriseSunset().equals("real")) {
							SunriseSunsetRequestObject sunriseSunset;
							try {
								sunriseSunset = new SunriseSunsetRequestObject(config.getTimeZone(), config.getWeatherLatitude(), config.getWeatherLongitude());
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
						} else if (config.getSunriseSunset().equals("custom"))
							world.setTime(calculateWorldTime(cal, config.getSunriseCustomTime(), config.getSunsetCustomTime()));
						else
							world.setTime(calculateWorldTime(cal, "5:02:27 AM", "6:36:36 PM"));
			}
		}, 0L, config.getTimeSyncInterval());
	}

	private void setupWeather() {
		try {
			new WeatherRequestObject(config.getAPIKey(), config.getWeatherLatitude(), config.getWeatherLongitude());
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
				WeatherRequestObject request = new WeatherRequestObject(config.getAPIKey(), config.getWeatherLatitude(), config.getWeatherLongitude());

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
		int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();

		if (currentMinutes >= sunriseMinutes && currentMinutes < sunsetMinutes) {
			return ((currentMinutes - sunriseMinutes) / (sunsetMinutes - sunriseMinutes) * 13569) + 23041;
		} else {
			if (currentMinutes < sunriseMinutes) {
				currentMinutes += 1440;
			}
			return ((currentMinutes - sunsetMinutes) / (1440 - sunsetMinutes + sunriseMinutes) * 13569) + 12610;
		}
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
