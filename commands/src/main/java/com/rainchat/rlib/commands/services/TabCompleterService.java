package com.rainchat.rlib.commands.services;

import com.rainchat.rlib.commands.annotation.Completion;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TabCompleterService {

    private final Map<String, TriFunction<CommandSender, Class<?>, String, List<String>>> completers = new HashMap<>();

    public TabCompleterService() {
        // Инициализация с лямбда-выражениями
        addCompleter("@player", (sender, args, extra) -> Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList()));
        addCompleter("@world", (sender, args, extra) -> Bukkit.getServer().getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList()));
        addCompleter("@material", (sender, args, extra) -> Arrays.stream(Material.values())
                .filter(Material::isItem)
                .map(material -> material.name().toLowerCase())
                .collect(Collectors.toList()));
        addCompleter("@entity", (sender, args, extra) -> Arrays.stream(EntityType.values())
                .filter(EntityType::isSpawnable)
                .map(EntityType::name)
                .collect(Collectors.toList()));
        addCompleter("@biome", (sender, args, extra) -> Arrays.stream(Biome.values())
                .map(Biome::name)
                .collect(Collectors.toList()));
        addCompleter("@chatcolor", (sender, args, extra) -> Arrays.stream(ChatColor.values())
                .map(ChatColor::name)
                .collect(Collectors.toList()));
        addCompleter("@sound", (sender, args, extra) -> Arrays.stream(Sound.values())
                .map(Sound::name)
                .collect(Collectors.toList()));
        addCompleter("@enum", (sender, args, extra) -> {
            if (args != null && args.isEnum()) {
                return Arrays.stream(((Class<? extends Enum<?>>) args).getEnumConstants())
                        .map(Enum::name)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        });
        addCompleter("@range", (sender, args, extra) -> {
            String[] parts = extra.split("-");
            int min, max;
            try {
                min = Integer.parseInt(parts[0]);
                max = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                return new ArrayList<>();
            }
            List<String> range = new ArrayList<>();
            for (int i = min; i <= max; i++) {
                range.add(String.valueOf(i));
            }
            return range;
        });
    }

    public void addCompleter(String parameter, TriFunction<CommandSender, Class<?>, String, List<String>> completer) {
        completers.put(parameter, completer);
    }

    public List<String> getCompletions(String parameter, CommandSender sender, Class<?> args) {
        String[] parts = parameter.split(":", 2);
        String mainParam = parts[0];
        String extraParam = (parts.length > 1) ? parts[1] : null;

        TriFunction<CommandSender, Class<?>, String, List<String>> completer = completers.get(mainParam);
        if (completer != null) {
            try {
                List<String> list = completer.apply(sender, args, extraParam);
                if (list.isEmpty()) return new ArrayList<>();
                return list;
            } catch (ClassCastException e) {
                return new ArrayList<>();
            }
        }

        // Если символ @ отсутствует или не совпадает ни с одним из списка
        if (!parameter.startsWith("@")) {
            String[] split = parameter.split("\\|");
            if (split.length > 1) {
                return Arrays.asList(split);
            }
            return Collections.singletonList(parameter);
        }

        // Если параметр не совпадает ни с одним из списка
        return Collections.singletonList(parameter);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
