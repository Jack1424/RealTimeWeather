package io.github.jack1424.realtimeweather;

import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class EventHandler implements Listener {
	private final Configurator config;

	public EventHandler(RealTimeWeather rtw) {
		config = rtw.getConfigurator();
	}

	@org.bukkit.event.EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onOperatorSet(PlayerCommandPreprocessEvent event) {
		if ((config.getBlockTimeSetCommand() && config.isTimeEnabled() && event.getMessage().contains("time set"))
				|| (config.getBlockWeatherCommand() && config.isWeatherEnabled() && event.getMessage().contains("weather"))) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}

	@org.bukkit.event.EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onOperatorSetConsole(ServerCommandEvent event) {
		if ((config.getBlockTimeSetCommand() && config.isTimeEnabled() && event.getCommand().contains("time set"))
				|| (config.getBlockWeatherCommand() && config.isWeatherEnabled() && event.getCommand().contains("weather"))) {
			event.setCancelled(true);
			event.getSender().sendMessage("Command cancelled (RealTimeWeather is controlling this)");
		}
	}
}
