package com.tonywww.bossrefactoraether.slider;

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
        bridge = Objects.requireNonNull(newBridge, "newBridge");
    }
}