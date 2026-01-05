package com.arkflame.mineclans.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Titles {
    private static final Map<String, Class<?>> NMS_CLASS_CACHE = new HashMap<>();
    private static final Map<Class<?>, Method> CHAT_COMPONENT_METHOD_CACHE = new HashMap<>();
    private static final Map<Class<?>, Constructor<?>> CHAT_PACKET_CONSTRUCTOR_CACHE = new HashMap<>();

    // Cached reflection elements
    private static Class<?> packetClass;
    private static Class<?> actionClass;
    private static Class<?> chatComponentClass;
    private static Method chatComponentMethod;
    private static Constructor<?> packetConstructor;

    // Cached enum values
    private static Object timesAction;
    private static Object titleAction;
    private static Object subtitleAction;

    // Cached reflection for sending packets
    private static Method getHandleMethod;
    private static Field playerConnectionField;
    private static Method sendPacketMethod;
    private static Class<?> craftPlayerClass;

    private static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        if (NMS_CLASS_CACHE.containsKey(name)) {
            Class<?> cachedClass = NMS_CLASS_CACHE.get(name);
            return cachedClass;
        } else {
            String fullName = "net.minecraft.server."
                    + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]
                    + "." + name;
            Class<?> clazz = Class.forName(fullName);
            NMS_CLASS_CACHE.put(name, clazz);
            return clazz;
        }
    }

    // Initialize all cached elements once
    static {
        try {
            packetClass = getNMSClass("PacketPlayOutTitle");
            actionClass = packetClass.getDeclaredClasses()[0]; // EnumTitleAction
            chatComponentClass = getNMSClass("IChatBaseComponent");

            // Cache methods and constructor
            chatComponentMethod = chatComponentClass.getDeclaredClasses()[0]
                    .getMethod("a", String.class);
            packetConstructor = packetClass.getConstructor(
                    actionClass, chatComponentClass, int.class, int.class, int.class);

            // Cache enum values
            timesAction = actionClass.getField("TIMES").get(null);
            titleAction = actionClass.getField("TITLE").get(null);
            subtitleAction = actionClass.getField("SUBTITLE").get(null);

            craftPlayerClass = Class
                    .forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");

            Class<?> entityPlayerClass = getNMSClass("EntityPlayer");
            playerConnectionField = entityPlayerClass.getField("playerConnection");

            Class<?> playerConnectionClass = getNMSClass("PlayerConnection");
            sendPacketMethod = playerConnectionClass.getMethod("sendPacket", getNMSClass("Packet"));
        } catch (Exception e) { /* Skip, not compatible */ }
    }

    private static void sendPacket(Player player, Object packet) {
        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            Object entityPlayer = getHandleMethod.invoke(craftPlayer);
            Object playerConnection = playerConnectionField.get(entityPlayer);
            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean sendTitlePacket(Player player, String title, String subtitle,
            int fadeIn, int stay, int fadeOut) {
        try {
            if (chatComponentMethod == null) {
                return false;
            }
            if (packetConstructor == null) {
                return false;
            }
            if (timesAction == null) {
                return false;
            }
            if (titleAction == null) {
                return false;
            }
            if (subtitleAction == null) {
                return false;
            }
            // Create chat components using cached method
            Object chatTitle = chatComponentMethod.invoke(null, "{\"text\":\"" + title + "\"}");
            Object chatSubtitle = subtitle != null
                    ? chatComponentMethod.invoke(null, "{\"text\":\"" + subtitle + "\"}")
                    : null;

            // 1. Send TIMES packet using cached constructor and enum
            Object timesPacket = packetConstructor.newInstance(
                    timesAction, null, fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            // 2. Send TITLE packet
            Object titlePacket = packetConstructor.newInstance(
                    titleAction, chatTitle, -1, -1, -1);
            sendPacket(player, titlePacket);

            // 3. Send SUBTITLE packet if exists
            if (subtitle != null) {
                Object subtitlePacket = packetConstructor.newInstance(
                        subtitleAction, chatSubtitle, -1, -1, -1);
                sendPacket(player, subtitlePacket);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || title == null) {
            return;
        }

        if ((title == null || title.isEmpty()) && (subtitle == null || subtitle.isEmpty())) {
            return;
        }

        if (sendTitlePacket(player, title, subtitle, fadeIn, stay, fadeOut)) {
            return;
        }

        try {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodError e) {
            // Failed
        }
    }

    public static void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    private static Method getChatComponentMethod(Class<?> clazz) throws NoSuchMethodException {
        Method method = CHAT_COMPONENT_METHOD_CACHE.get(clazz);
        if (method == null) {
            method = clazz.getDeclaredClasses()[0].getMethod("a", String.class);
            CHAT_COMPONENT_METHOD_CACHE.put(clazz, method);
        }
        return method;
    }

    private static Constructor<?> getChatPacketConstructor(Class<?> clazz)
            throws NoSuchMethodException, SecurityException, ClassNotFoundException {
        Constructor<?> constructor = CHAT_PACKET_CONSTRUCTOR_CACHE.get(clazz);
        if (constructor == null) {
            constructor = clazz.getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
            CHAT_PACKET_CONSTRUCTOR_CACHE.put(clazz, constructor);
        }
        return constructor;
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        try {
            Class<?> chatBaseComponentClass = getNMSClass("IChatBaseComponent");
            Method chatComponentMethod = getChatComponentMethod(chatBaseComponentClass);
            Object chatComponent = chatComponentMethod.invoke(null, "{\"text\":\"" + message + "\"}");

            Class<?> packetClass = getNMSClass("PacketPlayOutChat");
            Constructor<?> actionBarConstructor = getChatPacketConstructor(packetClass);
            Object actionBarPacket = actionBarConstructor.newInstance(chatComponent, (byte) 2);

            sendPacket(player, actionBarPacket);
        } catch (Exception e) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    public static void sendTitle(Collection<Player> players, String title, String subtitle, int fadeIn, int stay,
            int fadeOut) {
        for (Player player : players) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}
