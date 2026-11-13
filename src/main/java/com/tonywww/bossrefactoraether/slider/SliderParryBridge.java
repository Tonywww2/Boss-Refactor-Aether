package com.tonywww.bossrefactoraether.slider;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public interface SliderParryBridge {
    boolean isAvailable();

    boolean openWindow(Slider slider);

    boolean closeWindow(Slider slider);

    default boolean isParryable(Slider slider) {
        return false;
    }

    void mirrorBarrierBreak(Slider slider, @Nullable LivingEntity actor, ResourceLocation sourceId);
}