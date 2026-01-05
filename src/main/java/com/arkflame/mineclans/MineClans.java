package com.arkflame.mineclans;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
import com.arkflame.mineclans.claims.ClaimedChunks;
import com.arkflame.mineclans.commands.FactionsCommand;
import com.arkflame.mineclans.events.ClanEventManager;
import com.arkflame.mineclans.events.ClanEventScheduler;
import com.arkflame.mineclans.hooks.DynmapIntegration;
import com.arkflame.mineclans.hooks.FactionsPlaceholder;
import com.arkflame.mineclans.hooks.MineClansPlaceholder;
import com.arkflame.mineclans.hooks.ProtocolLibHook;
import com.arkflame.mineclans.hooks.WorldGuardReflectionUtil;
import com.arkflame.mineclans.listeners.ChatListener;
import com.arkflame.mineclans.listeners.ChunkProtectionListener;
import com.arkflame.mineclans.listeners.ClanEventListener;
import com.arkflame.mineclans.listeners.FactionBenefitsListener;
import com.arkflame.mineclans.listeners.FactionFriendlyFireListener;
import com.arkflame.mineclans.listeners.FactionsClaimsMenuListener;
import com.arkflame.mineclans.listeners.InventoryClickListener;
import com.arkflame.mineclans.listeners.PlayerJoinListener;
import com.arkflame.mineclans.listeners.PlayerKillListener;
import com.arkflame.mineclans.listeners.PlayerMoveListener;
import com.arkflame.mineclans.listeners.PlayerQuitListener;
import com.arkflame.mineclans.listeners.PlayerTeleportListener;
import com.arkflame.mineclans.managers.FactionBenefitsManager;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.managers.LeaderboardManager;
import com.arkflame.mineclans.managers.ScoreManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.modernlib.menus.listeners.MenuListener;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.redis.RedisProvider;
import com.arkflame.mineclans.tasks.BuffExpireTask;
import com.arkflame.mineclans.tasks.ClaimedChunksParticleTask;
import com.arkflame.mineclans.tasks.FactionBenefitsTask;
import com.arkflame.mineclans.tasks.PowerTask;
import com.arkflame.mineclans.tasks.TeleportScheduler;
import com.arkflame.mineclans.utils.BungeeUtil;

import net.milkbowl.vault.economy.Economy;

public class MineClans extends JavaPlugin {
    private static MineClans instance;
    private static String serverId;

    public static void setInstance(MineClans instance) {
        MineClans.instance = instance;
    }

    public static MineClans getInstance() {
        return MineClans.instance;
    }

    public static String getServerId() {
        return MineClans.serverId;
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(MineClans.getInstance(), runnable);
    }

    public static void runAsync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(MineClans.getInstance(), runnable, delay);
    }

    public static void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(MineClans.getInstance(), runnable);
    }

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

    // Score Manager
    private ScoreManager scoreManager;

    // Buff Manager
    private BuffManager buffManager;

    // Redis Provider
    private RedisProvider redisProvider = null;

    // Bungee Util
    private BungeeUtil bungeeUtil;

    // Teleport Scheduler
    private TeleportScheduler teleportScheduler;

    // Claimed Chunks
    private ClaimedChunks claimedChunks;
    private FactionsClaimsMenuListener claimsMenuListener;

    private DynmapIntegration dynmapIntegration;
    private WorldGuardReflectionUtil worldGuardReflectionUil;

    private FactionBenefitsManager benefitsManager;
    private FactionBenefitsTask benefitsTask;

    private ProtocolLibHook protocolLibHook;

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

    public ScoreManager getScoreManager() {
        return scoreManager;
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

    public ClaimedChunks getClaimedChunks() {
        return claimedChunks;
    }

    public DynmapIntegration getDynmapIntegration() {
        return dynmapIntegration;
    }

    public WorldGuardReflectionUtil getWorldGuardIntegration() {
        return worldGuardReflectionUil;
    }
    public FactionsClaimsMenuListener getClaimsMenuListener() {
        return claimsMenuListener;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        Server server = getServer();
        // Set static instance
        setInstance(this);
        protocolLibHook = new ProtocolLibHook(this);
        runAsync(() -> {
            // Save default config
            config = new ConfigWrapper(this, "config.yml").saveDefault().load();
            messages = new ConfigWrapper(this, "messages.yml").saveDefault().load();

            config.set("serverId", MineClans.serverId = config.getString("serverId", UUID.randomUUID().toString()));
            config.save();

            // Hooks
            dynmapIntegration = new DynmapIntegration(this);
            worldGuardReflectionUil = new WorldGuardReflectionUtil();

            // Basic
            mySQLProvider = new MySQLProvider(
                    config.getBoolean("mysql.enabled"),
                    config.getString("mysql.url"),
                    config.getString("mysql.username"),
                    config.getString("mysql.password"));
            factionManager = new FactionManager();
            factionPlayerManager = new FactionPlayerManager();
            redisProvider = new RedisProvider(factionManager, factionPlayerManager, getConfig(), logger);

            // API
            api = new MineClansAPI(factionManager, factionPlayerManager, mySQLProvider, redisProvider);

            // Advanced
            clanEventManager = new ClanEventManager(this);
            clanEventScheduler = new ClanEventScheduler(config.getInt("events.interval"),
                    config.getInt("events.time-limit"));
            leaderboardManager = new LeaderboardManager(mySQLProvider.getScoreDAO());
            scoreManager = new ScoreManager(mySQLProvider.getScoreDAO(), leaderboardManager);
            buffManager = new BuffManager(config);
            bungeeUtil = new BungeeUtil(this);
            teleportScheduler = new TeleportScheduler(this);
            claimedChunks = new ClaimedChunks(mySQLProvider.getClaimedChunksDAO());
            benefitsManager = new FactionBenefitsManager();
            benefitsTask = new FactionBenefitsTask();
            benefitsTask.register();

            // Register Listeners
            PluginManager pluginManager = server.getPluginManager();
            pluginManager.registerEvents(new ChatListener(), this);
            pluginManager.registerEvents(new ChunkProtectionListener(this), this);
            pluginManager.registerEvents(new ClanEventListener(), this);
            pluginManager.registerEvents(new FactionFriendlyFireListener(), this);
            pluginManager.registerEvents(new InventoryClickListener(), this);
            pluginManager.registerEvents(new PlayerJoinListener(factionPlayerManager), this);
            pluginManager.registerEvents(new PlayerKillListener(), this);
            pluginManager.registerEvents(new PlayerMoveListener(), this);
            pluginManager.registerEvents(new PlayerQuitListener(factionPlayerManager), this);
            pluginManager.registerEvents(new PlayerTeleportListener(), this);
            pluginManager.registerEvents(new MenuListener(), this);
            pluginManager.registerEvents(dynmapIntegration, this);
            pluginManager.registerEvents(new FactionBenefitsListener(), this);
        
            // Initialize the claims menu listener
            claimsMenuListener = new FactionsClaimsMenuListener(this);

            // Register Commands
            factionsCommand = new FactionsCommand();
            factionsCommand.register(this);

            // Register the placeholder
            if (pluginManager.getPlugin("PlaceholderAPI") != null) {
                runSync(() -> {
                    new FactionsPlaceholder(this).register();
                    new MineClansPlaceholder(this).register();
                });
            }

            // Register tasks
            BuffExpireTask buffExpireTask = new BuffExpireTask();
            buffExpireTask.register();
            ClaimedChunksParticleTask.start(20L);
            PowerTask.start();

            // Attempt to hook Vault
            if (server.getPluginManager().getPlugin("Vault") != null) {
                if (!setupEconomy()) {
                    logger.severe("Vault economy setup failed, using fallback.");
                }
            } else {
                logger.info("Vault not found, using fallback economy.");
            }
        });
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (dynmapIntegration != null) {
            dynmapIntegration.cleanup();
        }

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
        
        protocolLibHook.cleanup();

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

    public FactionBenefitsManager getFactionBenefitsManager() {
        return benefitsManager;
    }

    public FactionBenefitsTask getFactionBenefitsTask() {
        return benefitsTask;
    }

    public double getPowerMultiplier(Player player) {
        List<Double> multipliers = config.getDoubleList("power_multipliers");
        if (multipliers == null || multipliers.isEmpty()) {
            return 1.0;
        }
        Collections.sort(multipliers, Collections.reverseOrder());
        for (double multiplier : multipliers) {
            if (player.hasPermission("mineclans.power-multiplier." + multiplier)) {
                return multiplier;
            }
        }
        return 1.0;
    }
}