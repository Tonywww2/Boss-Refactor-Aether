package com.tonywww.bossrefactoraether.slider;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.tonywww.bossrefactoraether.compat.OptionalCompatBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Objects;

public final class SliderParryIntegration {
    private static final SliderParryBridge NOOP = new SliderParryBridge() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean openWindow(com.aetherteam.aether.entity.monster.dungeon.boss.Slider slider) {
            return false;
        }

        @Override
        public boolean closeWindow(com.aetherteam.aether.entity.monster.dungeon.boss.Slider slider) {
            return false;
        }

        @Override
        public void mirrorBarrierBreak(
                com.aetherteam.aether.entity.monster.dungeon.boss.Slider slider,
                net.minecraft.world.entity.LivingEntity actor,
                net.minecraft.resources.ResourceLocation sourceId) {
        }
    };

    private static SliderParryBridge bridge = NOOP;

    private SliderParryIntegration() {
    }

    public static SliderParryBridge bridge() {
        return bridge;
    }

    public static void install(SliderParryBridge newBridge) {
        bridge = new GuardedBridge(Objects.requireNonNull(newBridge, "newBridge"));
    }

    // 联动出错（特供模组版本不兼容、API 变化）时整体关闭联动，Boss 照常按本模组的规则战斗。
    public static void reset() {
        bridge = NOOP;
    }

    private static final class GuardedBridge implements SliderParryBridge {
        private static final String INCOMPATIBLE_API = "incompatible SenDimS Leader API";

        private final SliderParryBridge delegate;

        private GuardedBridge(SliderParryBridge delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isAvailable() {
            try {
                return delegate.isAvailable();
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean openWindow(Slider slider) {
            try {
                return delegate.openWindow(slider);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean closeWindow(Slider slider) {
            try {
                return delegate.closeWindow(slider);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public boolean isParryable(Slider slider) {
            try {
                return delegate.isParryable(slider);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
                return false;
            }
        }

        @Override
        public void mirrorBarrierBreak(Slider slider, @Nullable LivingEntity actor,
                                       ResourceLocation sourceId) {
            try {
                delegate.mirrorBarrierBreak(slider, actor, sourceId);
            } catch (LinkageError error) {
                OptionalCompatBootstrap.disableSenDims(INCOMPATIBLE_API, error);
            }
        }
    }
}