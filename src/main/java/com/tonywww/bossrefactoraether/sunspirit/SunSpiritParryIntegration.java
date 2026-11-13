package com.tonywww.bossrefactoraether.sunspirit;

import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.tonywww.bossrefactoraether.compat.OptionalCompatBootstrap;

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
        bridge = new GuardedBridge(Objects.requireNonNull(newBridge, "newBridge"));
    }

    // 联动出错（特供模组版本不兼容、API 变化）时整体关闭联动，Boss 照常按本模组的规则战斗。
    public static void reset() {
        bridge = NOOP;
    }

    private static final class GuardedBridge implements SunSpiritParryBridge {
        private static final String INCOMPATIBLE_API = "incompatible SenDimS Leader API";

        private final SunSpiritParryBridge delegate;

        private GuardedBridge(SunSpiritParryBridge delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean openWindow(SunSpirit sunSpirit) {
            try {
                return delegate.openWindow(sunSpirit);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean closeWindow(SunSpirit sunSpirit) {
            try {
                return delegate.closeWindow(sunSpirit);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean isParryable(SunSpirit sunSpirit) {
            try {
                return delegate.isParryable(sunSpirit);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }
    }
}