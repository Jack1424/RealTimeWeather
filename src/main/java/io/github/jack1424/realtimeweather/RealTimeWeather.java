package io.github.jack1424.realtimeweather;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public final class RealTimeWeather extends JavaPlugin implements Listener {
	private Logger logger;
	private ZoneId timezone;
	private boolean timeEnabled, weatherEnabled, debug, blockTimeSetCommand, blockWeatherCommand;

	@Override
	public void onEnable() {
		logger = getLogger();
		logger.info("Starting...");

		saveDefaultConfig();

		debug = getConfig().getBoolean("Debug");

		timeEnabled = getConfig().getBoolean("SyncTime");
		if (timeEnabled)
			setupTime();

		weatherEnabled = getConfig().getBoolean("SyncWeather");
		if (weatherEnabled)
			setupWeather();

		getServer().getPluginManager().registerEvents(this, this);

		debug("Enabling metrics...");
		Metrics metrics = new Metrics(this, 16709);
		metrics.addCustomChart(new SimplePie("weather_sync_enabled", () -> String.valueOf(weatherEnabled)));
		metrics.addCustomChart(new SimplePie("time_sync_enabled", () -> String.valueOf(timeEnabled)));

		logger.info("Started!");
	}

	@Override
	public void onDisable() {
		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL)) {
				debug("Re-enabling normal daylight and weather cycles...");

				if (timeEnabled)
					world.setGameRuleValue("doDaylightCycle", "true");
				if (weatherEnabled)
					world.setGameRuleValue("doWeatherCycle", "true");
			}

		logger.info("Stopping...");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onOperatorSet(PlayerCommandPreprocessEvent event) {
		if ((blockTimeSetCommand && timeEnabled && event.getMessage().contains("time set")) || (blockWeatherCommand && weatherEnabled && event.getMessage().contains("weather"))) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onOperatorSetConsole(ServerCommandEvent event) {
		if ((blockTimeSetCommand && timeEnabled && event.getCommand().contains("time set")) || (blockWeatherCommand && weatherEnabled && event.getCommand().contains("weather"))) {
			event.setCancelled(true);
			event.getSender().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}

	private void setupTime() {
		long timeSyncInterval;

		try {
			timezone = ZoneId.of(Objects.requireNonNull(getConfig().getString("Timezone")));
			timeSyncInterval = getConfig().getLong("TimeSyncInterval");
			blockTimeSetCommand = getConfig().getBoolean("BlockTimeSetCommand");
		} catch (NullPointerException|ZoneRulesException e) {
			logger.severe("Error loading timezone. Check that the values in your configuration file are valid.");
			debug(e.getMessage());
			logger.severe("Disabling time sync...");

			timeEnabled = false;
			return;
		}

		debug("Enabling time zone sync...");
		debug("Syncing time with " + timezone.toString());

		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL))
				world.setGameRuleValue("doDaylightCycle", "false");

		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			if (timeEnabled) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timezone));
				for (World world : getServer().getWorlds())
					if (world.getEnvironment().equals(World.Environment.NORMAL))
						world.setTime((1000 * cal.get(Calendar.HOUR_OF_DAY)) + (16 * (cal.get(Calendar.MINUTE) + 1)) - 6000);
			}
		}, 0L, timeSyncInterval);
	}

	private void setupWeather() {
		long weatherSyncInterval;

		String apiKey = getConfig().getString("APIKey");
		String lat = getConfig().getString("Latitude"), lon = getConfig().getString("Longitude");

		try {
			weatherSyncInterval = getConfig().getLong("WeatherSyncInterval");
			blockWeatherCommand = getConfig().getBoolean("blockWeatherCommand");

			RequestObject request = new RequestObject(apiKey, lat, lon);

			int response = request.getResponseCode();
			if (response > 499) {
				logger.severe("There was a server error when requesting weather information. Please try again later");
				throw new Exception("Server/client error");
			}
			else if (response > 399) {
				String message = "Error when getting weather information: ";

				if (response == 401)
					logger.severe(message + "API key incorrect");
				else if (response == 404)
					logger.severe(message + "Zip/Country code incorrect");
				else
					logger.severe("Unknown error");

				logger.severe("Please check that the values set in the config file are correct");

				throw new Exception("Configuration error");
			}
		} catch (Exception e) {
			debug(e.getMessage());
			logger.severe("Disabling weather sync...");

			weatherEnabled = false;
			return;
		}
		
		for (World world : getServer().getWorlds())
			if (world.getEnvironment().equals(World.Environment.NORMAL))
				world.setGameRuleValue("doWeatherCycle", "false");
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			debug("Syncing weather...");

			try {
				RequestObject request = new RequestObject(apiKey, lat, lon);

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
		}, 0L, weatherSyncInterval);
	}

	private void debug(String message) {
		if (debug) {
			logger.info("[DEBUG] " + message);
		}
	}
}
