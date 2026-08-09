package com.tonywww.bossrefactoraether.valkyriequeen;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;

public interface ValkyrieQueenParryBridge {
    boolean openWindow(ValkyrieQueen queen);

    boolean closeWindow(ValkyrieQueen queen);

}