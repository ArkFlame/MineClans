package com.arkflame.mineclans.hooks;

import com.arkflame.mineclans.MineClans;

public class FactionsPlaceholder extends MineClansPlaceholder {
    public FactionsPlaceholder(MineClans plugin) {
        super(plugin);
    }

    @Override
    public String getIdentifier() {
        return "mineclans";
    }
}
