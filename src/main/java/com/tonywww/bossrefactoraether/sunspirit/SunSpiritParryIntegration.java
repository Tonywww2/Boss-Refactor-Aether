package com.tonywww.bossrefactoraether.sunspirit;

import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;

import java.util.Objects;

public final class SunSpiritParryIntegration {
    private static final SunSpiritParryBridge NOOP = new SunSpiritParryBridge() {
        @Override
        public boolean openWindow(SunSpirit sunSpirit) {
            return false;
        }

        @Override
        public boolean closeWindow(SunSpirit sunSpirit) {
            return false;
        }

    };

    private static SunSpiritParryBridge bridge = NOOP;

    private SunSpiritParryIntegration() {
    }

    public static SunSpiritParryBridge bridge() {
        return bridge;
    }

    public static void install(SunSpiritParryBridge newBridge) {
        bridge = Objects.requireNonNull(newBridge, "newBridge");
    }
}