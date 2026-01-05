package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;
import com.arkflame.mineclans.utils.LocationData;
import com.arkflame.mineclans.utils.LocationUtil;

public class FactionDAO {
    private final static String TABLE_NAME = "mineclans_factions";
    
    protected String CREATE_FACTIONS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "faction_id CHAR(36) PRIMARY KEY," +
            "owner_id CHAR(36) NOT NULL," +
            "display_name VARCHAR(64) NOT NULL," +
            "home VARCHAR(255)," +
            "name VARCHAR(16) UNIQUE," +
            "balance DOUBLE," +
            "kills INT," +
            "events_won INT," +
            "friendly_fire BOOLEAN," +
            "open BOOLEAN," +
            "creation_date TIMESTAMP," +
            "announcement TEXT," +
            "discord VARCHAR(255))";

    protected String DELETE_FACTION_BY_NAME_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE name = ?";

    protected String UPSERT_FACTION_QUERY = "INSERT INTO " + TABLE_NAME + " (" +
            "faction_id, owner_id, display_name, home, name, balance, kills, events_won, friendly_fire, open, creation_date, announcement, discord) "
            +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "owner_id = VALUES(owner_id), " +
            "display_name = VALUES(display_name), " +
            "home = VALUES(home), " +
            "name = VALUES(name), " +
            "balance = VALUES(balance), " +
            "kills = VALUES(kills), " +
            "events_won = VALUES(events_won), " +
            "friendly_fire = VALUES(friendly_fire), " +
            "open = VALUES(open), " +
            "creation_date = VALUES(creation_date), " +
            "announcement = VALUES(announcement), " +
            "discord = VALUES(discord)";

    protected String DELETE_FACTION_BY_ID_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE faction_id = ?";

    protected String SELECT_FACTION_BY_ID_QUERY = "SELECT faction_id, name, owner_id, display_name, home, balance, kills, events_won, friendly_fire, open, creation_date, announcement, discord "
            +
            "FROM " + TABLE_NAME + " WHERE faction_id = ?";

    protected String SELECT_FACTION_BY_NAME_QUERY = "SELECT faction_id, name, owner_id, display_name, home, balance, kills, events_won, friendly_fire, open, creation_date, announcement, discord "
            +
            "FROM " + TABLE_NAME + " WHERE name = ?";

    private MySQLProvider mySQLProvider;

    public FactionDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_FACTIONS_TABLE_QUERY);
    }

    public void removeFactionByName(String name) {
        mySQLProvider.executeUpdateQuery(DELETE_FACTION_BY_NAME_QUERY, name);
    }

    public void insertOrUpdateFaction(Faction faction) {
        mySQLProvider.executeUpdateQuery(UPSERT_FACTION_QUERY,
                faction.getId(),
                faction.getOwner(),
                faction.getDisplayName(),
                faction.getHomeString(),
                faction.getName(),
                faction.getBalance(),
                faction.getKills(),
                faction.getEventsWon(),
                faction.isFriendlyFire(),
                faction.isOpen(),
                faction.getCreationDate(),
                faction.getAnnouncement(),
                faction.getDiscord());
    }

    public void removeFaction(UUID factionId) {
        mySQLProvider.executeUpdateQuery(DELETE_FACTION_BY_ID_QUERY, factionId);
    }

    public void disbandFaction(Faction faction) {
        mySQLProvider.getMemberDAO().removeMembers(faction.getId());
        mySQLProvider.getInvitedDAO().removeInvitedMembers(faction.getId());
        mySQLProvider.getRelationsDAO().removeRelationsById(faction.getId());
        removeFaction(faction.getId());
    }

    private boolean extractFactionFromResultSet(ResultSet resultSet, Faction faction) throws SQLException {
        if (resultSet.next()) {
            UUID id = UUID.fromString(resultSet.getString("faction_id"));
            UUID ownerId = UUID.fromString(resultSet.getString("owner_id"));
            String displayName = resultSet.getString("display_name");
            // Fetch additional faction properties
            LocationData home = LocationUtil.parseLocationData(resultSet.getString("home"));
            double balance = resultSet.getDouble("balance");
            int kills = resultSet.getInt("kills");
            boolean friendlyFire = resultSet.getBoolean("friendly_fire");
            // Name
            String name = resultSet.getString("name");
            // Events Won
            int eventsWon = resultSet.getInt("events_won");

            boolean open = resultSet.getBoolean("open");
            Timestamp creationDate = resultSet.getTimestamp("creation_date");
            String announcement = resultSet.getString("announcement");
            String discord = resultSet.getString("discord");

            // Create a Faction object and set additional properties
            faction.setup(id, ownerId, name, displayName);
            faction.setHome(home);
            faction.setBalance(balance);
            faction.setFriendlyFire(friendlyFire);
            faction.setOpen(open);
            faction.setCreationDate(creationDate);
            faction.setAnnouncement(announcement);
            faction.setDiscord(discord);

            // Load other faction stuff
            faction.setMembers(mySQLProvider.getMemberDAO().getMembers(id));
            faction.setInvited(mySQLProvider.getInvitedDAO().getInvitedMembers(id));
            faction.setRelations(mySQLProvider.getRelationsDAO().getRelationsByFactionId(id));
            faction.setRanks(mySQLProvider.getRanksDAO().getAllRanks());

            // Load Chest
            faction.setChest(mySQLProvider.getChestDAO().loadFactionChest(faction));

            // Load Kills
            faction.setKills(kills);

            // Load Events Won
            faction.setEventsWon(eventsWon);

            // Load Power
            faction.updatePower();
            return true;
        }
        return false;
    }

    public boolean getFactionById(UUID factionId, Faction faction) {
        AtomicBoolean extracted = new AtomicBoolean(false);
        mySQLProvider.executeSelectQuery(SELECT_FACTION_BY_ID_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                extracted.set(extractFactionFromResultSet(resultSet, faction));
            };
        }, factionId.toString());
        return extracted.get();
    }

    public boolean getFactionByName(String name, Faction faction) {
        AtomicBoolean extracted = new AtomicBoolean(false);
        mySQLProvider.executeSelectQuery(SELECT_FACTION_BY_NAME_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                extracted.set(extractFactionFromResultSet(resultSet, faction));
            };
        }, name);
        return extracted.get();
    }
}
