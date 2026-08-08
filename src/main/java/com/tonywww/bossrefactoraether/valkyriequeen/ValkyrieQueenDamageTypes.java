package com.tonywww.bossrefactoraether.valkyriequeen;

import com.tonywww.bossrefactoraether.BossRefactorAether;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ValkyrieQueenDamageTypes {
    public static final ResourceKey<DamageType> LIGHTNING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    BossRefactorAether.MOD_ID, "valkyrie_queen_lightning"));

    private ValkyrieQueenDamageTypes() {
    }
}