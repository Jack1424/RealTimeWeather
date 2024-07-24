package io.github.jack1424.realTimeWeather;

import io.github.jack1424.realTimeWeather.requests.WeatherRequestObject;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.parser.ParseException;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.HashSet;
import java.util.Objects;
import java.util.TimeZone;

public class ConfigManager {
	private final RealTimeWeather rtw;
	private final FileConfiguration configFile;
	private TimeZone timeZone;
	private boolean debug, timeEnabled, weatherEnabled, timeSyncAllWorlds, weatherSyncAllWorlds, blockTimeSetCommand, blockWeatherCommand, disableBedsAtNight, disableBedsDuringThunder;
	private long updateCheckInterval, timeSyncInterval, weatherSyncInterval;
	private String sunriseSunset, sunriseSunsetLatitude, sunriseSunsetLongitude, apiKey, weatherLatitude, weatherLongitude, disableBedsAtNightMessage, disableBedsDuringThunderMessage, sunriseCustomTime, sunsetCustomTime;
	private HashSet<World> timeSyncWorlds, weatherSyncWorlds;

	public ConfigManager(RealTimeWeather rtw) {
		this.rtw = rtw;
		configFile = rtw.getConfig();
	}

	public void refreshValues() {
		setDebugEnabled(configFile.getBoolean("Debug"));

		setTimeEnabled(configFile.getBoolean("SyncTime"));
		if (isTimeEnabled())
			try {
				timeSyncWorlds = new HashSet<>();
				setTimeSyncAllWorlds(configFile.getBoolean("TimeSyncAllWorlds"));
				if (getTimeSyncAllWorlds()) {
					for (World world : rtw.getServer().getWorlds())
						if (world.getEnvironment() == World.Environment.NORMAL)
							addTimeSyncWorld(world.getName());
				} else {
					for (String worldName : configFile.getStringList("TimeSyncWorlds"))
						addTimeSyncWorld(worldName);
				}
				setBlockTimeSetCommand(configFile.getBoolean("BlockTimeSetCommand"));
				setDisableBedsAtNight(configFile.getBoolean("DisableBedsAtNight"));
				setDisableBedsAtNightMessage(configFile.getString("DisableBedsAtNightMessage"));
				setTimeSyncInterval(configFile.getLong("TimeSyncInterval"));
				setTimeZone(configFile.getString("Timezone"));
				setSunriseSunset(configFile.getString("SunriseSunset"));
				if (getSunriseSunset().equals("real")) {
					setSunriseSunsetLatitude(configFile.getString("SunriseSunsetLatitude"));
					setSunriseSunsetLongitude(configFile.getString("SunriseSunsetLongitude"));
				} else if (getSunriseSunset().equals("custom")) {
					setSunriseCustomTime(configFile.getString("SunriseCustomTime"));
					setSunsetCustomTime(configFile.getString("SunsetCustomTime"));
				}
			} catch (ConfigurationException e) {
				rtw.getLogger().severe((e.getMessage()));
				rtw.getLogger().severe("Error loading time configuration. Check that the values in your configuration file are valid.");
				rtw.getLogger().severe("Disabling time sync...");

				setTimeEnabled(false);
			}

		setWeatherEnabled(configFile.getBoolean("SyncWeather"));
		if (isWeatherEnabled())
			try {
				weatherSyncWorlds = new HashSet<>();
				setWeatherSyncAllWorlds(configFile.getBoolean("WeatherSyncAllWorlds"));
				if (getWeatherSyncAllWorlds()) {
					for (World world : rtw.getServer().getWorlds())
						if (world.getEnvironment() == World.Environment.NORMAL)
							addWeatherSyncWorld(world.getName());
				} else {
					for (String worldName : configFile.getStringList("WeatherSyncWorlds"))
						addWeatherSyncWorld(worldName);
				}
				setBlockWeatherCommand(configFile.getBoolean("BlockWeatherCommand"));
				setDisableBedsDuringThunder(configFile.getBoolean("DisableBedsDuringThunder"));
				setDisableBedsDuringThunderMessage(configFile.getString("DisableBedsDuringThunderMessage"));
				setWeatherSyncInterval(configFile.getLong("WeatherSyncInterval"));
				setAPIKey(configFile.getString("APIKey"));
				setWeatherLatitude(configFile.getString("WeatherLatitude"));
				setWeatherLongitude(configFile.getString("WeatherLongitude"));
			} catch (ConfigurationException e) {
				rtw.getLogger().severe(e.getMessage());
				rtw.getLogger().severe("Error loading weather configuration. Check that the values in your configuration file are valid.");
				rtw.getLogger().severe("Disabling weather sync...");

				setWeatherEnabled(false);
			}

		setUpdateCheckInterval(configFile.getLong("UpdateCheckInterval"));
	}

	public long getUpdateCheckInterval() {
		return updateCheckInterval;
	}

	public void setUpdateCheckInterval(long value) {
		updateCheckInterval = value;
		rtw.debug("updateCheckInterval set to " + value);
	}

	public boolean debugEnabled() {
		return debug;
	}

	public void setDebugEnabled(boolean value) {
		debug = value;
		rtw.getLogger().warning("Debug set to " + value);
	}

	public boolean isTimeEnabled() {
		return timeEnabled;
	}

	public void setTimeEnabled(boolean value) {
		timeEnabled = value;
		rtw.debug("SyncTime set to " + value);
	}

	public boolean getTimeSyncAllWorlds() {
		return timeSyncAllWorlds;
	}

	public void setTimeSyncAllWorlds(boolean value) {
		timeSyncAllWorlds = value;
		rtw.debug("TimeSyncAllWorlds set to " + value);
	}

	public HashSet<World> getTimeSyncWorlds() {
		return timeSyncWorlds;
	}

	public void addTimeSyncWorld(String worldName) throws ConfigurationException {
		World world = rtw.getServer().getWorld(worldName);

		if (world == null)
			throw new ConfigurationException("World \"" + worldName + "\" cannot be found");

		timeSyncWorlds.add(world);
		rtw.debug("World \"" + worldName + "\" added to TimeSyncWorlds");
	}

	public boolean getBlockTimeSetCommand() {
		return blockTimeSetCommand;
	}

	public void setBlockTimeSetCommand(boolean value) {
		blockTimeSetCommand = value;
		rtw.debug("BlockTimeSetCommand set to " + value);
	}

	public boolean getDisableBedsAtNight() {
		return disableBedsAtNight;
	}

	public void setDisableBedsAtNight(boolean value) {
		disableBedsAtNight = value;
		rtw.debug("DisableBedsAtNight set to " + value);
	}

	public String getDisableBedsAtNightMessage() {
		return disableBedsAtNightMessage;
	}

	public void setDisableBedsAtNightMessage(String value) {
		disableBedsAtNightMessage = value;
		rtw.debug("NightDisabledBedMessage set to " + value);
	}

	public long getTimeSyncInterval() {
		return timeSyncInterval;
	}

	public void setTimeSyncInterval(long value) throws ConfigurationException {
		if (value < 0)
			throw new ConfigurationException("Time sync interval cannot be less than 0");

		timeSyncInterval = value;
		rtw.debug("TimeSyncInterval set to " + value);
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String value) throws ConfigurationException {
		try {
			timeZone = TimeZone.getTimeZone(ZoneId.of(Objects.requireNonNull(value)));
		} catch (ZoneRulesException | NullPointerException e) {
			throw new ConfigurationException("Timezone not valid");
		}

		rtw.debug("TimeZone set to " + value);
	}

	public String getSunriseSunset() {
		return sunriseSunset;
	}

	public void setSunriseSunset(String value) throws ConfigurationException {
		value = value.toLowerCase();
		if (value.equals("default") || value.equals("real") || value.equals("custom")) {
			sunriseSunset = value;
			rtw.debug("SunriseSunset set to " + value);
		} else {
			throw new ConfigurationException("SunriseSunset value invalid (must be default or real or custom)");
		}
	}

	public String getSunriseSunsetLatitude() {
		return sunriseSunsetLatitude;
	}

	public void setSunriseSunsetLatitude(String value) {
		sunriseSunsetLatitude = value;
		rtw.debug("SunriseSunsetLatitude set to " + value);
	}

	public String getSunriseSunsetLongitude() {
		return sunriseSunsetLongitude;
	}

	public void setSunriseSunsetLongitude(String value) {
		sunriseSunsetLongitude = value;
		rtw.debug("SunriseSunsetLongitude set to " + value);
	}

	public String getSunriseCustomTime() {
		return sunriseCustomTime;
	}

	public void setSunriseCustomTime(String value) throws ConfigurationException {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
			sunriseCustomTime = LocalTime.parse(value, formatter).format(formatter);
			rtw.debug("SunriseCustomTime set to " + value);
		} catch (DateTimeParseException e) {
			throw new ConfigurationException("SunriseCustomTime value invalid (check format)");
		}
	}

	public String getSunsetCustomTime() {
		return sunsetCustomTime;
	}

	public void setSunsetCustomTime(String value) throws ConfigurationException {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss a");
			sunsetCustomTime = LocalTime.parse(value, formatter).format(formatter);
			rtw.debug("SunsetCustomTime set to " + value);
		} catch (DateTimeParseException e) {
			throw new ConfigurationException("SunsetCustomTime value invalid (check format)");
		}
	}

	public boolean isWeatherEnabled() {
		return weatherEnabled;
	}

	public void setWeatherEnabled(boolean value) {
		weatherEnabled = value;
		rtw.debug("SyncWeather set to " + value);
	}

	public boolean getWeatherSyncAllWorlds() {
		return weatherSyncAllWorlds;
	}

	public void setWeatherSyncAllWorlds(boolean value) {
		weatherSyncAllWorlds = value;
		rtw.debug("WeatherSyncAllWorlds set to " + value);
	}

	public HashSet<World> getWeatherSyncWorlds() {
		return weatherSyncWorlds;
	}

	public void addWeatherSyncWorld(String worldName) throws ConfigurationException {
		World world = rtw.getServer().getWorld(worldName);

		if (world == null)
			throw new ConfigurationException("World \"" + worldName + "\" cannot be found");

		weatherSyncWorlds.add(world);
		rtw.debug("World \"" + worldName + "\" added to WeatherSyncWorlds");
	}

	public boolean getBlockWeatherCommand() {
		return blockWeatherCommand;
	}

	public void setBlockWeatherCommand(boolean value) {
		blockWeatherCommand = value;
		rtw.debug("BlockWeatherCommand set to " + value);
	}

	public boolean getDisableBedsDuringThunder() {
		return disableBedsDuringThunder;
	}

	public void setDisableBedsDuringThunder(boolean value) {
		disableBedsDuringThunder = value;
		rtw.debug("DisableBedsDuringThunder set to " + value);
	}

	public String getDisableBedsDuringThunderMessage() {
		return disableBedsDuringThunderMessage;
	}

	public void setDisableBedsDuringThunderMessage(String value) {
		disableBedsDuringThunderMessage = value;
		rtw.debug("ThunderDisabledBedMessage set to " + value);
	}

	public long getWeatherSyncInterval() {
		return weatherSyncInterval;
	}

	public void setWeatherSyncInterval(long value) throws ConfigurationException {
		if (value < 0)
			throw new ConfigurationException("WeatherSyncInterval cannot be less than 0");

		weatherSyncInterval = value;
		rtw.debug("WeatherSyncInterval set to " + value);
	}

	public String getAPIKey() {
		return apiKey;
	}

	public void setAPIKey(String value) throws ConfigurationException {
		try {
			new WeatherRequestObject(Objects.requireNonNull(value), "0", "0");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The APIKey cannot be blank");
		}
		catch (IOException | ParseException | URISyntaxException e) {
			rtw.getLogger().severe(e.getMessage());
			throw new ConfigurationException("There was an error when validating this APIKey (this does not mean that the API key is invalid)");
		}

		apiKey = value;
		rtw.debug("APIKey set to " + value);
	}

	public String getWeatherLatitude() {
		return weatherLatitude;
	}

	public void setWeatherLatitude(String value) throws ConfigurationException {
		try {
			double doubleValue = Double.parseDouble(Objects.requireNonNull(value));
			if (doubleValue < -90 || doubleValue > 90)
				throw new ConfigurationException("The entered latitude cannot be less than -90 or greater than 90");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The latitude cannot be blank");
		} catch (NumberFormatException e) {
			throw new ConfigurationException("The entered latitude might not be a number (or is too long)");
		}

		weatherLatitude = value;
		rtw.debug("Latitude set to " + value);
	}

	public String getWeatherLongitude() {
		return weatherLongitude;
	}

	public void setWeatherLongitude(String value) throws ConfigurationException {
		try {
			double doubleValue = Double.parseDouble(Objects.requireNonNull(value));
			if (doubleValue < -180 || doubleValue > 180)
				throw new ConfigurationException("The entered longitude cannot be less than -180 or greater than 180");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The longitude cannot be blank");
		} catch (NumberFormatException e) {
			throw new ConfigurationException("The entered longitude might not be a number (or is too long)");
		}

		weatherLongitude = value;
		rtw.debug("Longitude set to " + value);
	}
}
