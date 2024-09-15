package com.rainchat.rlib.commands.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class TypeParserService {

    private final Map<Class<?>, Parser> parsers = new HashMap<>();

    public TypeParserService() {
        registerParser(Boolean.TYPE, Boolean::parseBoolean);
        registerParser(Byte.TYPE, Byte::parseByte);
        registerParser(Short.TYPE, Short::parseShort);
        registerParser(Integer.TYPE, Integer::parseInt);
        registerParser(Float.TYPE, Float::parseFloat);
        registerParser(Long.TYPE, Long::parseLong);
        registerParser(Double.TYPE, Double::parseDouble);
        registerParser(String.class, (string) -> string);
        registerParser(Player.class, Bukkit::getPlayer);
    }

    private static Object parseEnum(Class<?> enumType, String arg) {
        for (Object constant : enumType.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(arg)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(arg + " is not a valid value for " + enumType.getCanonicalName() + ".");
    }

    public void registerParser(Class<?> type, Parser objParser) {
        parsers.put(type, objParser);
    }

    public Object parseObject(Class<?> type, String parse) {
        Parser objParser = parsers.get(type);
        try {
            if (objParser != null) {
                return objParser.parseObject(parse);
            }
            if (type.isEnum()) {
                return parseEnum(type, parse);
            }
        } catch (Exception e) {
            return null;
        }
        throw new IllegalArgumentException("No registered parser for " + type.getCanonicalName() + ".");
    }

    public boolean parserExistsFor(Class<?> type) {
        return parsers.containsKey(type) || type.isEnum();
    }

    public interface Parser {
        Object parseObject(String string);
    }
}
