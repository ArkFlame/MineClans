package com.arkflame.mineclans.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.Bukkit;

import com.arkflame.mineclans.MineClans;
import com.arkflame.mineclans.enums.RelationType;
import com.arkflame.mineclans.models.Faction;

public class FactionFriendlyFireListener implements Listener {
    private static long FRIENDLY_FIRE_MESSAGE_COOLDOWN = 10000;
    private Map<UUID, Long> lastMessageCooldowns = new HashMap<>();

    public boolean updateLastMessageCooldown(Entity entity) {
        if (lastMessageCooldowns.containsKey(entity.getUniqueId())) {
            return System.currentTimeMillis() - lastMessageCooldowns.get(entity.getUniqueId()) < FRIENDLY_FIRE_MESSAGE_COOLDOWN;
        }
        lastMessageCooldowns.put(entity.getUniqueId(), System.currentTimeMillis());
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity) {
                damager = (Entity) shooter;
            }
        }
        Entity entity = event.getEntity();
        if (damager == entity) {
            return;
        }
        if (!(damager instanceof Player) || !(entity instanceof Player)) {
            return;
        }
        Faction attackerFaction = MineClans.getInstance().getAPI().getFactionByPlayer(((Player) damager).getUniqueId());
        if (attackerFaction == null) {
            return;
        }
        Faction entityFaction = MineClans.getInstance().getAPI().getFactionByPlayer(((Player) entity).getUniqueId());
        if (entityFaction == null) {
            return;
        }
        RelationType relation = MineClans.getInstance().getFactionManager().getEffectiveRelation(attackerFaction, entityFaction);
        if ((relation == RelationType.ALLY || relation == RelationType.SAME_FACTION) && !attackerFaction.isFriendlyFire()) {
            event.setCancelled(true);
            if (!updateLastMessageCooldown(entity)) {
                damager.sendMessage(MineClans.getInstance().getMessages().getText("factions.friendly_fire.cannot_attack"));
            }
        }
    }
}





