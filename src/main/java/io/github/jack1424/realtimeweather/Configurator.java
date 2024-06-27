package io.github.jack1424.realtimeweather;

import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.parser.ParseException;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Objects;
import java.util.TimeZone;

public class Configurator {
	private final RealTimeWeather rtw;
	private final FileConfiguration configFile;
	private TimeZone timeZone;
	private boolean debug, timeEnabled, weatherEnabled, blockTimeSetCommand, blockWeatherCommand, disableBedsAtNight, disableBedsDuringThunder;
	private long timeSyncInterval, weatherSyncInterval;
	private String apiKey, lat, lon, disableBedsAtNightMessage, disableBedsDuringThunderMessage;

	public Configurator(RealTimeWeather rtw) {
		this.rtw = rtw;
		configFile = rtw.getConfig();
	}

	public void refreshValues() {
		setDebugEnabled(configFile.getBoolean("Debug"));

		setTimeEnabled(configFile.getBoolean("SyncTime"));
		if (isTimeEnabled())
			try {
				setBlockTimeSetCommand(configFile.getBoolean("BlockTimeSetCommand"));
				setDisableBedsAtNight(configFile.getBoolean("DisableBedsAtNight"));
				setDisableBedsAtNightMessage(configFile.getString("DisableBedsAtNightMessage"));
				setTimeSyncInterval(configFile.getLong("TimeSyncInterval"));
				setTimeZone(configFile.getString("Timezone"));
			} catch (ConfigurationException e) {
				rtw.getLogger().severe((e.getMessage()));
				rtw.getLogger().severe("Error loading time configuration. Check that the values in your configuration file are valid.");
				rtw.getLogger().severe("Disabling time sync...");

				setTimeEnabled(false);
			}

		setWeatherEnabled(configFile.getBoolean("SyncWeather"));
		if (isWeatherEnabled())
			try {
				setBlockWeatherCommand(configFile.getBoolean("BlockWeatherCommand"));
				setDisableBedsDuringThunder(configFile.getBoolean("DisableBedsDuringThunder"));
				setDisableBedsDuringThunderMessage(configFile.getString("DisableBedsDuringThunderMessage"));
				setWeatherSyncInterval(configFile.getLong("WeatherSyncInterval"));
				setAPIKey(configFile.getString("APIKey"));
				setLat(configFile.getString("Latitude"));
				setLon(configFile.getString("Longitude"));
			} catch (ConfigurationException e) {
				rtw.getLogger().severe(e.getMessage());
				rtw.getLogger().severe("Error loading weather configuration. Check that the values in your configuration file are valid.");
				rtw.getLogger().severe("Disabling weather sync...");

				setWeatherEnabled(false);
			}
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

	public boolean isWeatherEnabled() {
		return weatherEnabled;
	}

	public void setWeatherEnabled(boolean value) {
		weatherEnabled = value;
		rtw.debug("SyncWeather set to " + value);
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
			new RequestObject(Objects.requireNonNull(value), "0", "0");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The APIKey cannot be blank");
		}
		catch (IOException | ParseException e) {
			rtw.getLogger().severe(e.getMessage());
			throw new ConfigurationException("There was an error when validating this APIKey (this does not mean that the API key is invalid)");
		}

		apiKey = value;
		rtw.debug("APIKey set to " + value);
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String value) throws ConfigurationException {
		try {
			double doubleValue = Double.parseDouble(Objects.requireNonNull(value));
			if (doubleValue < -90 || doubleValue > 90)
				throw new ConfigurationException("The entered latitude cannot be less than -90 or greater than 90");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The latitude cannot be blank");
		} catch (NumberFormatException e) {
			throw new ConfigurationException("The entered latitude might not be a number (or is too long)");
		}

		lat = value;
		rtw.debug("Latitude set to " + value);
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String value) throws ConfigurationException {
		try {
			double doubleValue = Double.parseDouble(Objects.requireNonNull(value));
			if (doubleValue < -180 || doubleValue > 180)
				throw new ConfigurationException("The entered longitude cannot be less than -180 or greater than 180");
		} catch (NullPointerException e) {
			throw new ConfigurationException("The longitude cannot be blank");
		} catch (NumberFormatException e) {
			throw new ConfigurationException("The entered longitude might not be a number (or is too long)");
		}

		lon = value;
		rtw.debug("Longitude set to " + value);
	}
}
