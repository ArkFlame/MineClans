package com.arkflame.mineclans;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.arkflame.mineclans.api.MineClansAPI;
import com.arkflame.mineclans.buff.BuffManager;
import com.arkflame.mineclans.commands.FactionsCommand;
import com.arkflame.mineclans.events.ClanEventManager;
import com.arkflame.mineclans.events.ClanEventScheduler;
import com.arkflame.mineclans.listeners.ChatListener;
import com.arkflame.mineclans.listeners.ClanEventListener;
import com.arkflame.mineclans.listeners.FactionFriendlyFireListener;
import com.arkflame.mineclans.listeners.InventoryClickListener;
import com.arkflame.mineclans.listeners.PlayerJoinListener;
import com.arkflame.mineclans.listeners.PlayerKillListener;
import com.arkflame.mineclans.listeners.PlayerMoveListener;
import com.arkflame.mineclans.listeners.PlayerQuitListener;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.managers.LeaderboardManager;
import com.arkflame.mineclans.managers.PowerManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.modernlib.menus.listeners.MenuListener;
import com.arkflame.mineclans.placeholders.FactionsPlaceholder;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.RedisProvider;
import com.arkflame.mineclans.tasks.BuffExpireTask;
import com.arkflame.mineclans.tasks.TeleportScheduler;
import com.arkflame.mineclans.utils.BungeeUtil;

import net.milkbowl.vault.economy.Economy;

public class MineClans extends JavaPlugin {
    private ConfigWrapper config;
    private ConfigWrapper messages;

    // Providers
    private MySQLProvider mySQLProvider = null;

    // Managers
    private FactionManager factionManager;
    private FactionPlayerManager factionPlayerManager;

    // API
    private MineClansAPI api;

    private FactionsCommand factionsCommand;

    // Vault Economy
    private Economy economy;

    // Events
    private ClanEventManager clanEventManager;
    private ClanEventScheduler clanEventScheduler;

    // Leaderboard Manager
    private LeaderboardManager leaderboardManager;

    // Power Manager
    private PowerManager powerManager;

    // Buff Manager
    private BuffManager buffManager;

    // Redis Provider
    private RedisProvider redisProvider = null;

    // Bungee Util
    private BungeeUtil bungeeUtil;

    // Teleport Scheduler
    private TeleportScheduler teleportScheduler;

    public ConfigWrapper getCfg() {
        return config;
    }

    public ConfigWrapper getMessages() {
        return messages;
    }

    public MySQLProvider getMySQLProvider() {
        return mySQLProvider;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public FactionPlayerManager getFactionPlayerManager() {
        return factionPlayerManager;
    }

    public MineClansAPI getAPI() {
        return api;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isVaultHooked() {
        return economy != null;
    }

    public Economy getVaultEconomy() {
        return economy;
    }

    public ClanEventManager getClanEventManager() {
        return clanEventManager;
    }

    public ClanEventScheduler getClanEventScheduler() {
        return clanEventScheduler;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public BuffManager getBuffManager() {
        return buffManager;
    }

    public RedisProvider getRedisProvider() {
        return redisProvider;
    }

    public BungeeUtil getBungeeUtil() {
        return bungeeUtil;
    }

    public TeleportScheduler getTeleportScheduler() {
        return teleportScheduler;
    }

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        Server server = getServer();
        // Set static instance
        setInstance(this);
        // Save default config
        config = new ConfigWrapper(this, "config.yml").saveDefault().load();
        messages = new ConfigWrapper(this, "messages.yml").saveDefault().load();

        try {
            mySQLProvider = new MySQLProvider(
                    config.getBoolean("mysql.enabled"),
                    config.getString("mysql.url"),
                    config.getString("mysql.username"),
                    config.getString("mysql.password"));
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("An error occurred while connecting to the database.");
        } finally {
            if (!mySQLProvider.isConnected()) {
                logger.severe("=============== DATABASE CONNECTION ERROR ================");
                logger.severe("MineClans is unable to connect to the database.");
                logger.severe("To fix this, please configure the database settings in the 'config.yml' file.");
                logger.severe("You need a MySQL database for the plugin to work properly.");
                logger.severe("Make sure you have the following settings in the 'config.yml':");
                logger.severe("mysql:");
                logger.severe("  enabled: true");
                logger.severe(
                        "  url: jdbc:mysql://localhost:3306/database  # Replace 'database' with your database name");
                logger.severe("  username: root  # Change if your username is different");
                logger.severe("  password: password  # Use your actual database password");
                logger.severe("After making these changes, save the file and restart your server.");
                logger.severe("=============== DATABASE CONNECTION ERROR ================");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        // Managers
        factionManager = new FactionManager();
        factionPlayerManager = new FactionPlayerManager();
        clanEventManager = new ClanEventManager(this);
        clanEventScheduler = new ClanEventScheduler(config.getInt("events.interval"),
                config.getInt("events.time-limit"));
        leaderboardManager = new LeaderboardManager(mySQLProvider.getPowerDAO());
        powerManager = new PowerManager(mySQLProvider.getPowerDAO(), leaderboardManager);
        buffManager = new BuffManager(config);
        if (config.getConfig().getBoolean("redis.enabled", false)) {
            redisProvider = new RedisProvider(factionManager, factionPlayerManager, getConfig(), logger);
        } else {
            logger.info("Redis is disabled in the configuration fille. Buffs and locations wont synchronize.");
        }
        bungeeUtil = new BungeeUtil(this);
        teleportScheduler = new TeleportScheduler(this);

        // Initialize API
        api = new MineClansAPI(factionManager, factionPlayerManager, mySQLProvider, redisProvider);

        // Register Listeners
        PluginManager pluginManager = server.getPluginManager();
        pluginManager.registerEvents(new ChatListener(), this);
        pluginManager.registerEvents(new ClanEventListener(), this);
        pluginManager.registerEvents(new FactionFriendlyFireListener(), this);
        pluginManager.registerEvents(new InventoryClickListener(), this);
        pluginManager.registerEvents(new PlayerJoinListener(factionPlayerManager), this);
        pluginManager.registerEvents(new PlayerKillListener(), this);
        pluginManager.registerEvents(new PlayerMoveListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(factionPlayerManager), this);
        pluginManager.registerEvents(new MenuListener(), this);

        // Register Commands
        factionsCommand = new FactionsCommand();
        factionsCommand.register(this);

        // Register the placeholder
        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            new FactionsPlaceholder(this).register();
        }

        // Register tasks
        BuffExpireTask buffExpireTask = new BuffExpireTask();
        buffExpireTask.register();

        // Attempt to hook Vault
        if (server.getPluginManager().getPlugin("Vault") != null) {
            if (!setupEconomy()) {
                logger.severe("Vault economy setup failed, using fallback.");
            }
        } else {
            logger.info("Vault not found, using fallback economy.");
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (factionsCommand != null) {
            factionsCommand.unregisterBukkitCommand();
        }

        if (mySQLProvider != null) {
            mySQLProvider.close();
        }

        if (redisProvider != null) {
            redisProvider.shutdown();
        }

        if (bungeeUtil != null) {
            bungeeUtil.shutdown();
        }

        for (Player player : getServer().getOnlinePlayers()) {
            InventoryView view = player.getOpenInventory();

            if (view != null) {
                Inventory inventory = view.getTopInventory();

                if (inventory != null) {
                    InventoryHolder inventoryHolder = inventory.getHolder();

                    if (inventoryHolder instanceof Faction) {
                        player.closeInventory();
                    }
                }
            }
        }

        getServer().getScheduler().cancelTasks(this);
    }

    private static MineClans instance;

    public static void setInstance(MineClans instance) {
        MineClans.instance = instance;
    }

    public static MineClans getInstance() {
        return MineClans.instance;
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(MineClans.getInstance(), runnable);
    }

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(MineClans.getInstance(), runnable);
    }
}