package com.arkflame.mineclans.providers.daos.mysql;

import com.arkflame.mineclans.models.Faction;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;
import com.arkflame.mineclans.utils.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChestDAO {
    private static final String TABLE_NAME = "mineclans_chests";
    
    protected String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
            + "faction_id CHAR(36) NOT NULL PRIMARY KEY, "
            + "chest_contents TEXT"
            + ")";
    protected String INSERT_CHEST_QUERY = "INSERT INTO " + TABLE_NAME + " (faction_id, chest_contents) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE chest_contents = VALUES(chest_contents)";
    protected String SELECT_BY_FACTION_ID_QUERY = "SELECT chest_contents FROM " + TABLE_NAME + " WHERE faction_id = ?";
    protected String DELECT_CHEST_QUERY = "DELETE FROM " + TABLE_NAME + " WHERE faction_id = ?";
    private MySQLProvider mySQLProvider;

    public ChestDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLE_QUERY);
    }

    public void saveFactionChest(UUID id, Inventory chestInventory) {
        String data = InventoryUtil.itemStackArrayToBase64(chestInventory.getContents());
        mySQLProvider.executeUpdateQuery(INSERT_CHEST_QUERY, id, data);
    }

    public Inventory loadFactionChest(Faction faction) {
        Inventory inventory = Bukkit.createInventory(faction, 27, "Faction Chest");
        mySQLProvider.executeSelectQuery(SELECT_BY_FACTION_ID_QUERY,
                new ResultSetProcessor() {
                    @Override
                    public void run(ResultSet resultSet) throws SQLException {
                        if (resultSet != null && resultSet.next()) {
                            String data = resultSet.getString("chest_contents");
                            if (data != null && !data.isEmpty()) {
                                try {
                                    ItemStack[] items = InventoryUtil.itemStackArrayFromBase64(data);
                                    inventory.setContents(items);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }, faction.getId());
        return inventory;
    }

    public void deleteFactionChest(UUID id) {
        mySQLProvider.executeUpdateQuery(DELECT_CHEST_QUERY, id);
    }
}
