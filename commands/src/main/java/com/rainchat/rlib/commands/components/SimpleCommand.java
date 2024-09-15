package com.rainchat.rlib.commands.components;

import com.rainchat.rlib.commands.annotation.Completion;
import com.rainchat.rlib.commands.annotation.Option;
import com.rainchat.rlib.commands.annotation.Permission;
import com.rainchat.rlib.commands.services.MessageService;
import com.rainchat.rlib.commands.services.TabCompleterService;
import com.rainchat.rlib.commands.services.TypeParserService;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class SimpleCommand implements CommandExecutor, TabCompleter {

    private final Map<String, Method> commandMethods = new HashMap<>();
    private final Map<String, String[]> completionMethods = new HashMap<>();
    private final Map<String, String> permissionMethods = new HashMap<>();
    private final Map<String, Method> tabCompletionMethods = new HashMap<>();
    private final Map<Method, Object> methodInstances = new HashMap<>();
    private final TypeParserService typeParserService;
    private final TabCompleterService tabCompleterService;
    private final MessageService message;

    public SimpleCommand(TypeParserService typeParserService, TabCompleterService tabCompleterService, MessageService message) {
        this.typeParserService = typeParserService;
        this.tabCompleterService = tabCompleterService;
        this.message = message;
    }

    public void addCommand(Object instance, Method method, String commandName) {
        commandMethods.put(commandName, method);
        methodInstances.put(method, instance);

        if (method.isAnnotationPresent(Completion.class)) {
            Completion completion = method.getAnnotation(Completion.class);
            completionMethods.put(commandName, completion.value());
        }

        if (method.isAnnotationPresent(Permission.class)) {
            Permission completionFor = method.getAnnotation(Permission.class);
            permissionMethods.put(commandName, completionFor.value());
        }
    }

    public void addCompletion(Object instance, Method method, String commandName) {
        tabCompletionMethods.put(commandName, method);
        methodInstances.put(method, instance);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String fullCommand = command.getName() + " " + String.join(" ", args);
        Map.Entry<String, Method> bestMatch = findBestMatchEntry(fullCommand, commandMethods);

        if (bestMatch != null) {
            String permission = permissionMethods.get(bestMatch.getKey());
            if (permission != null && !sender.hasPermission(permission)) {
                message.sendMessage("command.error.permission", sender, args);
                return false;
            }
            try {
                String[] commandParts = bestMatch.getKey().split(" ");
                String[] remainingArgs = Arrays.copyOfRange(fullCommand.split(" "), commandParts.length, fullCommand.split(" ").length);

                Parameter[] parameters = bestMatch.getValue().getParameters();
                Object[] parsedArgs = new Object[parameters.length];
                int argIndex = 0;

                for (int i = 0; i < parameters.length; i++) {
                    if (CommandSender.class.isAssignableFrom(parameters[i].getType())) {
                        parsedArgs[i] = sender;

                        if (ConsoleCommandSender.class.isAssignableFrom(parameters[i].getType()) && !(sender instanceof ConsoleCommandSender)) {
                            message.sendMessage("command.error.player-only", sender, args);
                            return false;
                        }

                        if (Player.class.isAssignableFrom(parameters[i].getType()) && !(sender instanceof Player)) {
                            message.sendMessage("command.error.console-only", sender, args);
                            return false;
                        }

                    } else if (String[].class.isAssignableFrom(parameters[i].getType())) {
                        parsedArgs[i] = remainingArgs;
                    } else if (argIndex < remainingArgs.length) {
                        parsedArgs[i] = typeParserService.parseObject(parameters[i].getType(), remainingArgs[argIndex]);
                        argIndex++;
                    } else {
                        Option option = parameters[i].getAnnotation(Option.class);
                        if (option != null) {
                            parsedArgs[i] = typeParserService.parseObject(parameters[i].getType(), option.value());
                        } else {
                            message.sendMessage("command.error.no-args", sender, args);
                            return false;
                        }
                    }
                }

                bestMatch.getValue().invoke(methodInstances.get(bestMatch.getValue()), parsedArgs);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                message.sendMessage("command.error.wrong-usage", sender, args);
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String fullCommand = command.getName() + " " + String.join(" ", Arrays.copyOf(args, args.length - 1));

        List<String> subcommands = getSubcommands(command.getName(), args, sender);
        if (!subcommands.isEmpty()) {
            return filterSuggestions(subcommands, args.length > 0 ? args[args.length - 1] : "");
        }

        Map.Entry<String[], Integer> bestCompletionsEntry = findBestCompletions(fullCommand, completionMethods);
        String[] bestCompletions = bestCompletionsEntry.getKey();
        int bestMatchLength = bestCompletionsEntry.getValue();

        if (bestCompletions != null) {
            String[] fullCommandParts = fullCommand.split(" ");

            int completionIndex = fullCommandParts.length - bestMatchLength; // Учитываем длину подкоманды

            if (completionIndex < bestCompletions.length) {
                String completion = bestCompletions[completionIndex];
                List<String> suggestions = tabCompleterService.getCompletions(completion, sender, String.class);
                return filterSuggestions(suggestions, args.length > 0 ? args[args.length - 1] : "");
            }
        }

        Map.Entry<String, Method> bestMatchEntry = findBestMatchEntry(fullCommand, tabCompletionMethods);
        if (bestMatchEntry != null) {
            try {
                String[] commandParts = bestMatchEntry.getKey().split(" ");
                String[] remainingArgs = Arrays.copyOfRange(fullCommand.split(" "), commandParts.length, fullCommand.split(" ").length + 1);
                List<String> suggestions = (List<String>) bestMatchEntry.getValue().invoke(methodInstances.get(bestMatchEntry.getValue()), sender, command, command.getName(), remainingArgs);
                return filterSuggestions(suggestions, args.length > 0 ? args[args.length - 1] : "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    private Map.Entry<String, Method> findBestMatchEntry(String fullCommand, Map<String, Method> methods) {
        Map.Entry<String, Method> bestMatchEntry = null;
        int bestMatchLength = 0;

        for (Map.Entry<String, Method> entry : methods.entrySet()) {
            String commandPattern = entry.getKey();
            String[] commandParts = commandPattern.split(" ");

            if (fullCommand.startsWith(commandPattern) && commandParts.length > bestMatchLength) {
                bestMatchEntry = entry;
                bestMatchLength = commandParts.length;
            }
        }

        return bestMatchEntry;
    }

    private Map.Entry<String[], Integer> findBestCompletions(String fullCommand, Map<String, String[]> completions) {
        String[] bestCompletions = null;
        int bestMatchLength = 0;

        for (Map.Entry<String, String[]> entry : completions.entrySet()) {
            String commandPattern = entry.getKey();
            String[] commandParts = commandPattern.split(" ");

            if (fullCommand.startsWith(commandPattern) && commandParts.length > bestMatchLength) {
                bestCompletions = entry.getValue();
                bestMatchLength = commandParts.length;
            }
        }

        return new AbstractMap.SimpleEntry<>(bestCompletions, bestMatchLength);
    }


    private List<String> getSubcommands(String alias, String[] args, CommandSender sender) {
        List<String> subcommands = new ArrayList<>();
        String fullCommand = alias + " " + String.join(" ", Arrays.copyOf(args, args.length - 1));
        for (String commandPattern : commandMethods.keySet()) {
            String permission = permissionMethods.get(commandPattern);
            if (permission != null && !sender.hasPermission(permission)) {
                continue;
            }

            String[] commandParts = commandPattern.split(" ");
            String[] fullCommandParts = fullCommand.split(" ");
            if (commandParts.length > fullCommandParts.length) {
                boolean match = true;
                for (int i = 0; i < fullCommandParts.length; i++) {
                    if (!commandParts[i].equalsIgnoreCase(fullCommandParts[i])) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    subcommands.add(commandParts[fullCommandParts.length]);
                }
            }
        }
        return subcommands;
    }

    private List<String> filterSuggestions(List<String> suggestions, String arg) {
        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(arg.toLowerCase())) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }
}
