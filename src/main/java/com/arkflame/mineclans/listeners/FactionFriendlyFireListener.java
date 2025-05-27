package com.arkflame.mineclans.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

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
        Faction attackerFaction = MineClans.getInstance().getAPI().getFaction(((Player) damager).getUniqueId());
        Faction entityFaction = MineClans.getInstance().getAPI().getFaction(((Player) entity).getUniqueId());
        RelationType relation = MineClans.getInstance().getFactionManager().getEffectiveRelation(attackerFaction, entityFaction);
        if ((relation == RelationType.ALLY || relation == RelationType.SAME_FACTION) && !attackerFaction.isFriendlyFire()) {
            event.setCancelled(true);
            damager.sendMessage(MineClans.getInstance().getMessages().getText("factions.friendly_fire.cannot_attack"));
        }
    }
}
