package com.rainchat.rlib.commands.services;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public final class MessageService {

    private final Map<String, MessageResolver> messages = new HashMap<>();

    public MessageService() {
        register("command.error.permission", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cOops! You don't have the required permissions to execute this command!")));
        register("command.error.no-args", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cNot enough arguments provided! &7Please check the command usage and try again.")));
        register("command.error.exists", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cThe command you're trying to use doesn't seem to exist!")));
        register("command.error.no-message", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cAn error occurred while executing the command. &7Please contact the administrator!")));
        register("command.error.wrong-usage", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cIncorrect command usage! &7Please review the syntax and try again.")));
        register("command.error.console-only", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cThis command can only be executed from the console!")));
        register("command.error.player-only", (sender, args) -> sender.sendMessage(color("&e⚠ &7| &cThis command can only be executed from the console!")));
    }

    public static String color(final String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void register(final String messageId, final MessageResolver messageResolver) {
        messages.put(messageId, messageResolver);
    }

    public void sendMessage(final String messageId, final CommandSender sender, String[] args) {
        MessageResolver messageResolver = messages.get(messageId);
        if (messageResolver == null) messageResolver = messages.get("command.error.no-message");
        if (messageResolver != null) messageResolver.resolve(sender, args);
    }

    public interface MessageResolver {
        void resolve(CommandSender sender, String[] args);
    }

}
