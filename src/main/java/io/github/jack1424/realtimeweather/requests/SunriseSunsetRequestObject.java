package io.github.jack1424.realtimeweather.requests;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.naming.ConfigurationException;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SunriseSunsetRequestObject {
	private String sunriseTime, sunsetTime;

	public SunriseSunsetRequestObject(TimeZone timeZone, String lat, String lon) throws IOException, ParseException, ConfigurationException {
		URL url = new URL(String.format("https://api.sunrisesunset.io/json?lat=%s&lng=%s&timezone=UTC", lat, lon));

		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.connect();
		int responseCode = con.getResponseCode();
		if (responseCode > 399)
			throw new ProtocolException("Server/client error (HTTP error " + responseCode + ")");

		Scanner scanner = new Scanner(url.openStream());
		StringBuilder data = new StringBuilder();
		while (scanner.hasNext()) {
			data.append(scanner.nextLine());
		}
		scanner.close();

		JSONObject response = (JSONObject) ((JSONObject) new JSONParser().parse(data.toString())).get("results");
		sunriseTime = response.get("sunrise").toString();
		sunsetTime = response.get("sunset").toString();

		if (sunriseTime.equalsIgnoreCase("null") || sunsetTime.equalsIgnoreCase("null"))
			throw new ConfigurationException("Time(s) returned null. Check the sunrise/sunset longitude and latitude.");

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a");
		LocalDate currentDate = LocalDate.now(ZoneId.of("UTC"));
		sunriseTime = ZonedDateTime.of(currentDate, LocalTime.parse(sunriseTime, timeFormatter), ZoneId.of("UTC")).withZoneSameInstant(timeZone.toZoneId()).format(timeFormatter);
		sunsetTime = ZonedDateTime.of(currentDate, LocalTime.parse(sunsetTime, timeFormatter), ZoneId.of("UTC")).withZoneSameInstant(timeZone.toZoneId()).format(timeFormatter);
	}

	public String getSunriseTime() {
		return sunriseTime;
	}

	public String getSunsetTime() {
		return sunsetTime;
	}
}
