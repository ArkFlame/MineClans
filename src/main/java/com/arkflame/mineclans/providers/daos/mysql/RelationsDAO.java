package com.arkflame.mineclans.providers.daos.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arkflame.mineclans.models.Relation;
import com.arkflame.mineclans.providers.MySQLProvider;
import com.arkflame.mineclans.providers.processors.ResultSetProcessor;

public class RelationsDAO {
    protected String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS mineclans_relations (" +
            "faction_id CHAR(36) NOT NULL," +
            "target_faction_id CHAR(36) NOT NULL," +
            "relation_type VARCHAR(64) NOT NULL," +
            "PRIMARY KEY (faction_id, target_faction_id))";

    protected String INSERT_RELATION_QUERY = "INSERT INTO mineclans_relations (faction_id, target_faction_id, relation_type) VALUES (?, ?, ?)";

    protected String INSERT_OR_UPDATE_RELATION_QUERY = "INSERT INTO mineclans_relations (faction_id, target_faction_id, relation_type) " +
            "VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE relation_type = VALUES(relation_type)";

    protected String DELETE_RELATION_BY_ID_QUERY = "DELETE FROM mineclans_relations WHERE faction_id = ? AND target_faction_id = ?";

    protected String SELECT_RELATIONS_BY_FACTION_ID_QUERY = "SELECT target_faction_id, relation_type FROM mineclans_relations WHERE faction_id = ?";

    protected String DELETE_RELATIONS_BY_FACTION_ID_QUERY = "DELETE FROM mineclans_relations WHERE faction_id = ?";

    protected String DELETE_RELATIONS_BY_TARGET_ID_QUERY = "DELETE FROM mineclans_relations WHERE target_faction_id = ?";

    private MySQLProvider mySQLProvider;

    public RelationsDAO(MySQLProvider mySQLProvider) {
        this.mySQLProvider = mySQLProvider;
    }

    public void createTable() {
        mySQLProvider.executeUpdateQuery(CREATE_TABLE_QUERY);
    }

    public void insertRelation(UUID factionId, UUID targetFactionId, String relationType) {
        mySQLProvider.executeUpdateQuery(INSERT_RELATION_QUERY,
                factionId, targetFactionId, relationType);
    }

    public void insertOrUpdateRelation(UUID factionId, UUID targetFactionId, String relationType) {
        mySQLProvider.executeUpdateQuery(INSERT_OR_UPDATE_RELATION_QUERY, factionId, targetFactionId, relationType);
    }

    public void removeRelationById(UUID factionId, UUID targetFactionId) {
        mySQLProvider.executeUpdateQuery(DELETE_RELATION_BY_ID_QUERY,
                factionId, targetFactionId);
    }

    public Collection<Relation> getRelationsByFactionId(UUID factionId) {
        Collection<Relation> relations = ConcurrentHashMap.newKeySet();
        mySQLProvider.executeSelectQuery(SELECT_RELATIONS_BY_FACTION_ID_QUERY, new ResultSetProcessor() {
            public void run(ResultSet resultSet) throws SQLException {
                if (resultSet != null) {
                    while (resultSet.next()) {
                        UUID targetFactionId = UUID.fromString(resultSet.getString("target_faction_id"));
                        String relationType = resultSet.getString("relation_type");
                        Relation relation = new Relation(factionId, targetFactionId, relationType);
                        relations.add(relation);
                    }
                }
            };
        }, factionId);
        return relations;
    }

    public void removeRelationsById(UUID id) {
        mySQLProvider.executeUpdateQuery(DELETE_RELATIONS_BY_FACTION_ID_QUERY, id);
        mySQLProvider.executeUpdateQuery(DELETE_RELATIONS_BY_TARGET_ID_QUERY, id);
    }

    public void removeRelationsByTargetId(UUID targetId) {
        mySQLProvider.executeUpdateQuery(DELETE_RELATIONS_BY_TARGET_ID_QUERY, targetId);
    }

}