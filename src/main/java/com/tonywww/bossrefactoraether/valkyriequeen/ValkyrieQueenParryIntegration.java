package com.tonywww.bossrefactoraether.valkyriequeen;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.compat.OptionalCompatBootstrap;

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
        bridge = new GuardedBridge(Objects.requireNonNull(newBridge, "newBridge"));
    }

    // 联动出错（特供模组版本不兼容、API 变化）时整体关闭联动，Boss 照常按本模组的规则战斗。
    public static void reset() {
        bridge = NOOP;
    }

    private static final class GuardedBridge implements ValkyrieQueenParryBridge {
        private static final String INCOMPATIBLE_API = "incompatible SenDimS Leader API";

        private final ValkyrieQueenParryBridge delegate;

        private GuardedBridge(ValkyrieQueenParryBridge delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean openWindow(ValkyrieQueen queen) {
            try {
                return delegate.openWindow(queen);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean closeWindow(ValkyrieQueen queen) {
            try {
                return delegate.closeWindow(queen);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean isParryable(ValkyrieQueen queen) {
            try {
                return delegate.isParryable(queen);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }
    }
}