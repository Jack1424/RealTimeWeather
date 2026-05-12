package io.github.jack1424.realTimeWeather.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;

public final class TextUtils {
	private TextUtils() {
	}

    public static void sendActionbar(Player player, String message) {
        if (player == null) return;

        String safeMessage = message == null ? "" : message;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Object component = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + safeMessage + "\"}");

            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> baseComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Constructor<?> packetConstructor = packetClass.getConstructor(baseComponentClass, byte.class);
            Object packet = packetConstructor.newInstance(component, (byte) 2);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"))
                    .invoke(connection, packet);

        } catch (Exception e) {
            player.sendMessage(safeMessage);
        }
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
