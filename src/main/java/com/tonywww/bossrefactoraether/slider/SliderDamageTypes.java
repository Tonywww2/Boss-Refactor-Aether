package com.tonywww.bossrefactoraether.slider;

import com.aetherteam.aether.data.resources.registries.AetherDamageTypes;
import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class SliderDamageTypes {
    public static final ResourceKey<DamageType> UNBLOCKABLE_CHAIN_DASH = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    BossRefactorAether.MOD_ID, "unblockable_slider_dash")
    );

    private SliderDamageTypes() {
    }

        public static DamageSource collision(Slider slider) {
                return AetherDamageTypes.entityDamageSource(
                                slider.level(), AetherDamageTypes.CRUSH, slider);
        }

    public static DamageSource chainDash(Slider slider, boolean unblockable) {
        if (!unblockable) {
                        return collision(slider);
        }
        return new DamageSource(
                slider.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(UNBLOCKABLE_CHAIN_DASH),
                slider
        );
    }
}