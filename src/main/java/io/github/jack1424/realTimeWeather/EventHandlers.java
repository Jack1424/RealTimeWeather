package io.github.jack1424.realTimeWeather;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class EventHandlers implements Listener {
	private final ConfigManager config;

	public EventHandlers(RealTimeWeather rtw) {
		config = rtw.getConfigManager();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if ((config.getBlockTimeSetCommand() && config.isTimeEnabled() && event.getMessage().toLowerCase().contains("time set "))
				|| (config.getBlockWeatherCommand() && config.isWeatherEnabled() && event.getMessage().toLowerCase().contains("weather "))) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("Command disabled by RealTimeWeather");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onConsoleCommand(ServerCommandEvent event) {
		if ((config.getBlockTimeSetCommand() && config.isTimeEnabled() && event.getCommand().toLowerCase().contains("time set "))
				|| (config.getBlockWeatherCommand() && config.isWeatherEnabled() && event.getCommand().toLowerCase().contains("weather "))) {
			event.setCancelled(true);
			event.getSender().sendMessage("Command disabled by RealTimeWeather");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		World playerWorld = player.getWorld();
		long worldTime = playerWorld.getTime();

		if (config.isTimeEnabled() && config.getDisableBedsAtNight() && ((!playerWorld.hasStorm() && worldTime >= 12542 && worldTime <= 23459)
			|| (playerWorld.hasStorm() && worldTime >= 12010 && worldTime <= 23991))) {
			event.setCancelled(true);
			player.sendMessage(config.getDisableBedsAtNightMessage());
		}

		if (config.isWeatherEnabled() && config.getDisableBedsDuringThunder() && playerWorld.isThundering()) {
			event.setCancelled(true);
			player.sendMessage(config.getDisableBedsDuringThunderMessage());
		}
	}
}
