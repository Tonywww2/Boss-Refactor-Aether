package com.tonywww.bossrefactoraether.valkyriequeen;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;

import java.util.Objects;

public final class ValkyrieQueenParryIntegration {
    private static final ValkyrieQueenParryBridge NOOP = new ValkyrieQueenParryBridge() {
        @Override
        public boolean openWindow(ValkyrieQueen queen) {
            return false;
        }

        @Override
        public boolean closeWindow(ValkyrieQueen queen) {
            return false;
        }
    };

    private static ValkyrieQueenParryBridge bridge = NOOP;

    private ValkyrieQueenParryIntegration() {
    }

    public static ValkyrieQueenParryBridge bridge() {
        return bridge;
    }

    public static void install(ValkyrieQueenParryBridge newBridge) {
        bridge = Objects.requireNonNull(newBridge, "newBridge");
    }
}