package io.github.jack1424.realTimeWeather.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class TextUtils {
	private TextUtils() {
	}

	public static void sendActionbar(Player player, String message) {
		if (player == null)
			return;

		String safeMessage = message == null ? "" : message;
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(safeMessage));
	}

	public static void sendPlayerAlert(Player player, String message, String delivery) {
		if (player == null)
			return;

		String safeMessage = message == null ? "" : message;
		if ("actionbar".equalsIgnoreCase(delivery)) {
			sendActionbar(player, safeMessage);
			return;
		}

		player.sendMessage(safeMessage);
	}
}
