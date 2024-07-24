package io.github.jack1424.realTimeWeather.requests;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URISyntaxException;

public class WeatherRequestObject {
	private boolean rain = false, thunder = false;

	public WeatherRequestObject(String apiKey, String lat, String lon) throws IOException, ParseException, ConfigurationException, URISyntaxException {
		JSONArray conditions;
		try {
			conditions = (JSONArray) ((JSONObject) RequestFunctions.makeRequest(String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s", lat, lon, apiKey))).get("weather");
		} catch (RequestFunctions.HTTPResponseException e) {
			int responseCode = Integer.parseInt(e.getMessage());
			if (responseCode > 499) {
				throw new ProtocolException("Server/client error (HTTP error " + responseCode + ")");
			} else if (responseCode > 399) {
				String message = "Error when getting weather information: ";

				if (responseCode == 401)
					throw new ConfigurationException(message + "API key invalid. Check the Wiki for troubleshooting steps.");
				else
					throw new ProtocolException(message + "Unknown error");
			} else {
				throw new IOException("Server/client error (HTTP error " + e.getMessage() + ")");
			}
		}

		for (Object rawCondition : conditions) {
			int id = Integer.parseInt(String.valueOf(((JSONObject) rawCondition).get("id")));
			while (id >= 10)
				id /= 10;

			rain = id == 2 || id == 3 || id == 5 || id == 6;
			thunder = id == 2;
		}
	}

	public boolean isRaining() {
		return rain;
	}

	public boolean isThundering() {
		return thunder;
	}
}
