package com.arkflame.mineclans.listeners;

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
        if (!(damager instanceof Player) || !(entity instanceof Player)) {
            return;
        }

        final Player attacker = (Player) damager;
        final Player defender = (Player) entity;
        final EntityDamageByEntityEvent e = event;
        
        MineClans plugin = MineClans.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction attackerFaction = plugin.getAPI().getFaction(((Player) damager).getUniqueId());
            Faction entityFaction = plugin.getAPI().getFaction(((Player) entity).getUniqueId());
            RelationType relation = MineClans.getInstance().getFactionManager().getEffectiveRelation(attackerFaction, entityFaction);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if ((relation == RelationType.ALLY || relation == RelationType.SAME_FACTION) && !attackerFaction.isFriendlyFire()) {
                    e.setCancelled(true);
                    damager.sendMessage(MineClans.getInstance().getMessages().getText("factions.friendly_fire.cannot_attack"));
                }
            });
        });
    }
}




