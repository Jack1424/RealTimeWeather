package io.github.jack1424.realTimeWeather;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class RealTimeWeatherCommand implements CommandExecutor, TabCompleter {
	private final RealTimeWeather plugin;

	public RealTimeWeatherCommand(RealTimeWeather plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("realtimeweather.admin")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
			return true;
		}

		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("reload")) {
				sender.sendMessage(ChatColor.GREEN + "Reloading RealTimeWeather configuration...");
				try {
					plugin.reloadPlugin();
					sender.sendMessage(ChatColor.GREEN + "RealTimeWeather successfully reloaded!");
				} catch (Exception e) {
					sender.sendMessage(ChatColor.RED + "Failed to reload plugin. Check the console for errors.");
					plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
					e.printStackTrace();
				}
				return true;
			}

			if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
				boolean enable = args[0].equalsIgnoreCase("enable");
				if (args.length > 1) {
					if (args[1].equalsIgnoreCase("time")) {
						plugin.toggleTimeSync(enable);
						sender.sendMessage(ChatColor.GREEN + "Time synchronization " + (enable ? "enabled" : "disabled") + ".");
						return true;
					} else if (args[1].equalsIgnoreCase("weather")) {
						plugin.toggleWeatherSync(enable);
						sender.sendMessage(ChatColor.GREEN + "Weather synchronization " + (enable ? "enabled" : "disabled") + ".");
						return true;
					}
				}
				sender.sendMessage(ChatColor.RED + "Usage: /rtw " + args[0].toLowerCase() + " <time|weather>");
				return true;
			}
		}

		sender.sendMessage(ChatColor.GOLD + "=== RealTimeWeather ===");
		sender.sendMessage(ChatColor.YELLOW + "/rtw reload " + ChatColor.GRAY + "- Reloads the configuration.");
		sender.sendMessage(ChatColor.YELLOW + "/rtw enable <time|weather> " + ChatColor.GRAY + "- Enables time or weather sync.");
		sender.sendMessage(ChatColor.YELLOW + "/rtw disable <time|weather> " + ChatColor.GRAY + "- Disables time or weather sync.");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();
		if (!sender.hasPermission("realtimeweather.admin")) {
			return completions;
		}

		if (args.length == 1) {
			String input = args[0].toLowerCase();
			if ("reload".startsWith(input)) completions.add("reload");
			if ("enable".startsWith(input)) completions.add("enable");
			if ("disable".startsWith(input)) completions.add("disable");
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
				String input = args[1].toLowerCase();
				if ("time".startsWith(input)) completions.add("time");
				if ("weather".startsWith(input)) completions.add("weather");
			}
		}
		return completions;
	}
}
