package com.tonywww.bossrefactoraether.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageBlockAccessor {
    @Invoker("isDamageSourceBlocked")
    boolean bossRefactorAether$isDamageSourceBlocked(DamageSource source);
}