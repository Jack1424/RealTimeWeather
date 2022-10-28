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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.logging.Logger;

public final class RealTimeWeather extends JavaPlugin implements Listener {
	private Logger logger;
	private ZoneId timezone;
	private boolean timeEnabled, weatherEnabled, debug;

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
		if ((timeEnabled && event.getMessage().contains("time set")) || (weatherEnabled && event.getMessage().contains("weather"))) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onOperatorSetConsole(ServerCommandEvent event) {
		if ((timeEnabled && event.getCommand().contains("time set")) || (weatherEnabled && event.getCommand().contains("weather"))) {
			event.setCancelled(true);
			event.getSender().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}

	private void setupTime() {
		long timeSyncInterval;

		try {
			timezone = ZoneId.of(Objects.requireNonNull(getConfig().getString("Timezone")));
			timeSyncInterval = getConfig().getLong("TimeSyncInterval");
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
						world.setTime((1000 * cal.get(Calendar.HOUR_OF_DAY)) + (16 * cal.get(Calendar.MINUTE)) - 6000); // TODO: Time is one minute behind
			}
		}, 0L, timeSyncInterval);
	}

	private void setupWeather() {
		long weatherSyncInterval;

		String apiKey = getConfig().getString("APIKey");
		String zipCode = getConfig().getString("ZipCode");
		String countryCode = getConfig().getString("CountryCode");

		String lat, lon;

		try {
			weatherSyncInterval = getConfig().getLong("WeatherSyncInterval");

			HttpsURLConnection con = (HttpsURLConnection) new URL(String.format("https://api.openweathermap.org/geo/1.0/zip?zip=%s,%s&appid=%s", zipCode, countryCode, apiKey)).openConnection();
			con.setRequestMethod("GET");
			con.connect();

			int response = con.getResponseCode();
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

			JSONObject obj = makeWeatherRequest(con.getURL());
			lat = String.valueOf(obj.get("lat"));
			lon = String.valueOf(obj.get("lon"));
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

			boolean rain = false, thunder = false;
			try {
				JSONObject obj = makeWeatherRequest(new URL(String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s", lat, lon, apiKey)));
				JSONArray conditions = (JSONArray) obj.get("weather");

				for (Object rawCondition : conditions) {
					JSONObject condition = (JSONObject) rawCondition;
					int id = Integer.parseInt(String.valueOf(condition.get("id")));
					debug("Weather ID: " + id);

					while (id >= 10)
						id /= 10;

					if (!rain)
						rain = id == 2 || id == 3 || id == 5 || id == 6;
					if (!thunder)
						thunder = id == 2;
				}
			} catch (Exception e) {
				logger.severe("There was an error when attempting to get weather information");
				debug(e.getMessage());
			}

			debug("Setting weather (Rain: " + rain + ", Thunder: " + thunder + ")...");
			for (World world : getServer().getWorlds())
				if (world.getEnvironment().equals(World.Environment.NORMAL)) {
					world.setStorm(rain);
					world.setThundering(thunder);
				}
		}, 0L, weatherSyncInterval);
	}

	private JSONObject makeWeatherRequest(URL url) throws IOException, ParseException {
		Scanner scanner = new Scanner(url.openStream());
		StringBuilder data = new StringBuilder();
		while (scanner.hasNext()) {
			data.append(scanner.nextLine());
		}
		scanner.close();

		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(data.toString());
	}

	private void debug(String message) {
		if (debug) {
			logger.info("[DEBUG] " + message);
		}
	}
}
