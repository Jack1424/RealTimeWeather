package io.github.jack1424.realtimeweather.requests;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class RequestFunctions {
	public static Object makeRequest(String URLString) throws IOException, HTTPResponseException, ParseException {
		int responseCode = getResponseCode(URLString);
		if (responseCode > 399)
			throw new HTTPResponseException(responseCode);

		Scanner scanner = new Scanner(new URL(URLString).openStream());
		StringBuilder response = new StringBuilder();
		while (scanner.hasNextLine())
			response.append(scanner.nextLine());
		scanner.close();

		return new JSONParser().parse(response.toString());
	}

	public static int getResponseCode(String URLString) throws IOException {
		URL url = new URL(URLString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.connect();
		return con.getResponseCode();
	}

	public static String getLatestVersion() throws Exception {
		return ((JSONObject) ((JSONArray) makeRequest("https://api.modrinth.com/v2/project/WRA6ODcm/version")).get(0)).get("version_number").toString();
	}

	public static class HTTPResponseException extends Exception {
		public HTTPResponseException(int responseCode) {
			super(String.valueOf(responseCode));
		}
	}
}
