package com.rainchat.rlib.commands;

import com.rainchat.rlib.commands.annotation.CommandNode;
import com.rainchat.rlib.commands.annotation.TabComplete;
import com.rainchat.rlib.commands.components.SimpleCommand;
import com.rainchat.rlib.commands.services.MessageService;
import com.rainchat.rlib.commands.services.TabCompleterService;
import com.rainchat.rlib.commands.services.TypeParserService;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class CommandController {

    private final Plugin plugin;
    private final Map<String, SimpleCommand> commandExecutors = new HashMap<>();
    private final CommandMap commandMap;
    private final TypeParserService typeParserService = new TypeParserService();
    private final TabCompleterService tabCompleterService = new TabCompleterService();
    private final MessageService message = new MessageService();

    public CommandController(final Plugin plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
    }


    private static PluginCommand getCommand(String name, Plugin plugin) {
        PluginCommand command = null;

        try {
            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);

            command = c.newInstance(name, plugin);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return command;
    }

    public void registerCommands(Object commandInstance) {
        Class<?> clazz = commandInstance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CommandNode.class)) {
                CommandNode commandNode = method.getAnnotation(CommandNode.class);
                String commandName = commandNode.value().split(" ")[0];
                String[] commandAliases = commandNode.aliases();

                registerCommand(commandInstance, method, commandName, commandNode.value());

                for (String alias : commandAliases) {
                    String main = alias.split(" ")[0];
                    registerCommand(commandInstance, method, main, alias);
                }
            }

            if (method.isAnnotationPresent(TabComplete.class)) {
                TabComplete tabComplete = method.getAnnotation(TabComplete.class);
                String[] commandNames = tabComplete.value();

                for (String commandNameWithArgs : commandNames) {
                    String commandName = commandNameWithArgs.split(" ")[0];

                    SimpleCommand executor = commandExecutors.computeIfAbsent(commandName, k -> {
                        PluginCommand command = getCommand(commandName, plugin);
                        if (command != null) {
                            SimpleCommand newExecutor = new SimpleCommand(typeParserService, tabCompleterService, message);
                            command.setExecutor(newExecutor);
                            command.setTabCompleter(newExecutor);
                            commandMap.register(plugin.getDescription().getName(), command);
                            return newExecutor;
                        }
                        return null;
                    });

                    if (executor != null) {
                        executor.addCompletion(commandInstance, method, commandNameWithArgs);
                    }
                }
            }

        }
    }

    private void registerCommand(Object commandInstance, Method method, String commandName, String fullCommand) {
        SimpleCommand executor = commandExecutors.computeIfAbsent(commandName, k -> {
            PluginCommand command = getCommand(commandName, plugin);
            if (command != null) {
                SimpleCommand newExecutor = new SimpleCommand(typeParserService, tabCompleterService, message);
                command.setExecutor(newExecutor);
                command.setTabCompleter(newExecutor);
                commandMap.register(plugin.getDescription().getName(), command);
                return newExecutor;
            }
            return null;
        });

        if (executor != null) {
            executor.addCommand(commandInstance, method, fullCommand);
        }
    }

    private CommandMap getCommandMap() {
        CommandMap commandMap = null;
        try {
            final Server server = Bukkit.getServer();
            final Method getCommandMap = server.getClass().getDeclaredMethod("getCommandMap");
            getCommandMap.setAccessible(true);

            commandMap = (CommandMap) getCommandMap.invoke(server);
            final Field bukkitCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            bukkitCommands.setAccessible(true);
        } catch (final Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get Command Map, Commands won't be registered!");
        }

        return commandMap;
    }

    public TypeParserService getTypeParserService() {
        return typeParserService;
    }

    public TabCompleterService getTabCompleterService() {
        return tabCompleterService;
    }

    public MessageService getMessage() {
        return message;
    }

}

