package com.tonywww.bossrefactoraether.sunspirit;

import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;

public interface SunSpiritParryBridge {
    boolean openWindow(SunSpirit sunSpirit);

    boolean closeWindow(SunSpirit sunSpirit);

}