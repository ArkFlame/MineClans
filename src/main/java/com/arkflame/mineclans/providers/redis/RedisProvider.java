package com.arkflame.mineclans.providers.redis;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.enums.Rank;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.managers.FactionManager;
import com.arkflame.mineclans.managers.FactionPlayerManager;
import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.modernlib.config.ConfigWrapper;
import com.arkflame.mineclans.utils.LocationData;
import com.arkflame.mineclans.utils.LocationUtil;

import org.bukkit.plugin.Plugin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RedisProvider {
    private Plugin plugin = MineClans.getInstance();
    private FactionManager factionManager;
    private FactionPlayerManager factionPlayerManager;
    private final Logger logger;
    private final ConfigWrapper config;
    private final String channelName;
    private final String instanceId;
    private boolean shutdown = false;
    private JedisPool jedisPool;

    public RedisProvider(FactionManager factionManager, FactionPlayerManager factionPlayerManager, ConfigWrapper config,
            Logger logger) {
        this.factionManager = factionManager;
        this.factionPlayerManager = factionPlayerManager;
        this.config = config;
        this.logger = logger;
        this.channelName = config.getString("redis.channel", "mineclansUpdates");
        this.instanceId = UUID.randomUUID().toString();

        if (!config.getBoolean("redis.enabled")) {
            logger.info("Redis is disabled in the configuration file. Buffs and locations wont synchronize.");
            return;
        }

        validateInputs(factionManager, config);
        new Thread(this::subscribeToFactionUpdates).start();
    }

    private void validateInputs(FactionManager factionManager, ConfigWrapper config) {
        if (factionManager == null || config == null) {
            throw new IllegalArgumentException("FactionManager and Configuration cannot be null");
        }
    }

    public boolean isClosed() {
        return jedisPool == null || jedisPool.isClosed();
    }

    public void initializeRedis() {
        if (isClosed()) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
            poolConfig.setMaxIdle(64);
            poolConfig.setMinIdle(16);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);

            String host = config.getString("redis.host");
            int port = config.getInt("redis.port");
            boolean ssl = config.getBoolean("redis.ssl", false);
            String scheme = ssl ? "rediss" : "redis";

            String user = "";
            String password = "";
            if (config.getBoolean("redis.auth.enabled")) {
                user = config.getString("redis.auth.username", "");
                password = config.getString("redis.auth.password", "");
            }

            String uriStr;
            if (user.isEmpty()) {
                uriStr = scheme + "://" + host + ":" + port;
            } else {
                uriStr = scheme + "://" + user + ":" + password + "@" + host + ":" + port;
            }
            URI uri = URI.create(uriStr);

            jedisPool = new JedisPool(poolConfig, uri, 2000, 2000);
        }
    }

    public Jedis getResource() {
        return jedisPool.getResource();
    }

    private void subscribeToFactionUpdates() {
        while (!shutdown && plugin.isEnabled()) {
            try {
                try {
                    shutdown();
                    shutdown = false;
                    initializeRedis();

                    try (Jedis sub = getResource()) {
                        sub.subscribe(new FactionMessageSub(factionManager, factionPlayerManager, channelName, instanceId, logger), channelName);
                    }
                } catch (Exception e) {
                    if (!shutdown) {
                        logger.log(Level.SEVERE, "Cannot connect to redis. Please check your configuration files.", e);
                    }
                }
                Thread.sleep(1000L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        shutdown();
    }

    public void publishUpdate(String actionType, UUID id, String action, String... params) {
        if (isClosed()) return;
        try (Jedis pub = getResource()) {
            if (id == null || action == null)
                return;
            StringBuilder message = new StringBuilder(instanceId).append(":").append(actionType).append(":")
                    .append(action).append(":").append(id);
            for (String param : params)
                message.append(":").append(param);
            pub.publish(channelName, message.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        this.shutdown = true;
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public void setAnnouncement(UUID factionId, String announcement) {
        publishUpdate("faction", factionId, "announcement", announcement != null ? announcement : "");
    }

    public void focus(UUID factionId, UUID factionFocus) {
        publishUpdate("faction", factionId, "focus", factionFocus.toString());
    }

    public void deposit(UUID factionId, double amount) {
        publishUpdate("faction", factionId, "deposit", String.valueOf(amount));
    }

    public void withdraw(UUID factionId, double amount) {
        publishUpdate("faction", factionId, "withdraw", String.valueOf(amount));
    }

    public void updateHome(UUID factionId, LocationData home) {
        publishUpdate("faction", factionId, "updateHome", LocationUtil.locationDataToString(home));
    }

    public void updateFriendlyFire(UUID factionId, boolean friendlyFire) {
        publishUpdate("faction", factionId, "updateFriendlyFire", String.valueOf(friendlyFire));
    }

    public void invite(UUID factionId, UUID playerId) {
        publishUpdate("faction", factionId, "invite", playerId.toString());
    }

    public void uninvite(UUID factionId, UUID playerId) {
        publishUpdate("faction", factionId, "uninvite", playerId.toString());
    }

    public void removePlayer(UUID factionId, UUID playerId) {
        publishUpdate("faction", factionId, "removePlayer", playerId.toString());
    }

    public void addPlayer(UUID factionId, UUID playerId) {
        publishUpdate("faction", factionId, "addPlayer", playerId.toString());
    }

    public void startChestUpdate(Faction faction) {
        publishUpdate("faction", faction.getId(), "startChestUpdate");
    }

    public void endChestUpdate(Faction faction, boolean updateChestContent) {
        publishUpdate("faction", faction.getId(), "endChestUpdate", String.valueOf(updateChestContent));
    }

    public void updateRelation(UUID factionId, UUID otherFactionId, RelationType relationType) {
        publishUpdate("faction", factionId, "updateRelation", String.valueOf(otherFactionId), relationType.name());
    }

    public void updateDisplayName(UUID factionId, String displayName) {
        publishUpdate("faction", factionId, "updateDisplayName", displayName);
    }

    public void updateName(UUID factionId, String name) {
        publishUpdate("faction", factionId, "updateName", name);
    }

    public void updateFactionOwner(UUID factionId, UUID newOwnerId) {
        publishUpdate("faction", factionId, "updateFactionOwner", newOwnerId.toString());
    }

    public void removeFaction(UUID factionId) {
        publishUpdate("faction", factionId, "removeFaction");
    }

    public void createFaction(UUID factionId, UUID playerId, String factionName) {
        publishUpdate("faction", factionId, "createFaction", playerId.toString(), factionName);
    }

    public void sendFactionMessage(UUID factionId, String message) {
        publishUpdate("faction", factionId, "sendFactionMessage", message);
    }

    public void sendAllianceMessage(UUID factionId, String message) {
        publishUpdate("faction", factionId, "sendAllianceMessage", message);
    }

    public void activateBuff(UUID factionId, String playerName, String buffName) {
        publishUpdate("faction", factionId, "activateBuff", playerName, buffName);
    }

    public void updateFaction(UUID playerId, String factionName) {
        publishUpdate("player", playerId, "updateFaction", factionName);
    }

    public void updateRank(UUID playerId, Rank rank) {
        publishUpdate("player", playerId, "updateRank", rank.name());
    }

    public void requestHome(UUID playerId) {
        publishUpdate("player", playerId, "requestHome");
    }

    public void updateChunk(UUID factionId, int x, int z, String worldName, String serverName, boolean remove) {
        publishUpdate("faction", factionId, "updateChunk", String.valueOf(x), String.valueOf(z), worldName, serverName, String.valueOf(remove));
    }

    public void updatePower(UUID uniqueId, double amount) {
        publishUpdate("player", uniqueId, "updatePower", String.valueOf(amount));
    }
}
