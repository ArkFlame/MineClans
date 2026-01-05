package com.arkflame.mineclans.modernlib.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.arkflame.mineclans.modernlib.utils.ChatColors;

import java.nio.file.Files;

public class ConfigWrapper {
    private final Plugin plugin;

    private ConfigurationSection config = null;
    private String path = null;
    private String fileName = null;

    // This will store all text that contains color
    private Map<String, String> colorTextMap = new HashMap<>();

    public ConfigWrapper(Plugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = config;
    }

    public ConfigWrapper(Plugin plugin, String fileName) {
        this.plugin = plugin;
        setFile(fileName);
    }

    public ConfigWrapper(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setFile(String fileName) {
        if (fileName != null) {
            this.path = new File(plugin.getDataFolder(), fileName).getPath();
        } else {
            this.path = null;
        }
        this.fileName = fileName;
    }

    public ConfigWrapper saveDefault() {
        if (path != null) {
            File configFile = new File(path);
            if (!configFile.exists()) {
                try (InputStream inputStream = plugin.getResource(fileName)) {
                        createParentFolder(configFile);
                    if (inputStream != null) {
                        Files.copy(inputStream, configFile.toPath());
                    } else {
                        configFile.createNewFile();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return this;
    }

    public ConfigWrapper load(String fileName) {
        setFile(fileName);
        return load();
    }

    public ConfigWrapper load() {
        if (path != null) {
            try {
                colorTextMap.clear();
                this.config = YamlConfiguration.loadConfiguration(new File(path));
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
        return this;
    }

    public void save() {
        if (path == null) {
            return;
        }
        if (config instanceof YamlConfiguration) {
            try {
                ((YamlConfiguration) config).save(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public boolean isLoaded() {
        return config != null;
    }

    public List<String> getStringList(String key) {
        if (!isLoaded())
            return Collections.emptyList();
        return config.getStringList(key);
    }

    public List<String> getTextList(String key) {
        List<String> textList = new ArrayList<>();
        List<String> stringList = getStringList(key);
        for (String text : stringList) {
            textList.add(ChatColors.color(text));
        }
        return textList;
    }

    public List<String> getTextList(String key, String... placeholders) {
        List<String> textList = new ArrayList<>();
        List<String> stringList = getStringList(key);
        for (String text : stringList) {
            String processedText = replacePlaceholders(text, placeholders);
            textList.add(ChatColors.color(processedText));
        }
        return textList;
    }

    public String getText(String key) {
        String text;
        if (colorTextMap.containsKey(key)) {
            text = colorTextMap.get(key);
        } else {
            text = getString(key);
            text = text.replace("%prefix%", getString("prefix"));
            text = ChatColors.color(text);
            colorTextMap.put(key, text);
        }
        return text;
    }

    public String getText(String key, Object... placeholders) {
        // Convert objects to strings
        String[] stringPlaceholders = new String[placeholders.length];
        for (int i = 0; i < placeholders.length; i++) {
            stringPlaceholders[i] = String.valueOf(placeholders[i]);
        }
        return getText(key, stringPlaceholders);
    }

    public String getText(String key, String... placeholders) {
        String text = getText(key);
    
        for (int i = 0; i < placeholders.length; i += 2) {
            text = replacePlaceholders(text, placeholders);
        }
    
        return text;
    }    

    private String replacePlaceholders(String text, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String replacement = i + 1 < placeholders.length ? placeholders[i + 1] : "";
            text = text.replace(placeholder, replacement);
        }
        return text;
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String def) {
        if (!isLoaded())
            return "undefined";
        return config.getString(key, def);
    }

    public int getInt(String key, int def) {
        if (!isLoaded())
            return def;
        return config.getInt(key);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        if (!isLoaded())
            return false;
        return config.getBoolean(key, def);
    }

    public ConfigWrapper getSection(String key) {
        if (!isLoaded())
            return null;
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null)
            return null;
        return new ConfigWrapper(plugin, section);
    }

    public Set<String> getKeys() {
        if (!isLoaded())
            return Collections.emptySet();
        return config.getKeys(false);
    }

    private static void createParentFolder(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public boolean isConfigurationSection(String path) {
        return config.isConfigurationSection(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public boolean contains(String string) {
        return config.contains(string);
    }

    public void set(String path, String value) {
        config.set(path, value);
    }

    public List<Double> getDoubleList(String string) {
        return config.getDoubleList(string);
    }
}
