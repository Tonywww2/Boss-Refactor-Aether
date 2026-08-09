package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.projectile.crystal.AbstractCrystal;
import com.aetherteam.aether.entity.projectile.crystal.IceCrystal;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritCombatService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceCrystal.class)
public abstract class IceCrystalMixin {
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$replaceManagedHit(
            EntityHitResult hitResult, CallbackInfo callback) {
        AbstractCrystal crystal = (AbstractCrystal) (Object) this;
        if (SunSpiritCombatService.isManagedProjectile(crystal)) {
            SunSpiritCombatService.handleManagedProjectileHit(crystal, hitResult);
            callback.cancel();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$handleDefense(
            DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback) {
        AbstractCrystal crystal = (AbstractCrystal) (Object) this;
        if (SunSpiritCombatService.isManagedProjectile(crystal)) {
            callback.setReturnValue(
                    SunSpiritCombatService.deflectManagedProjectile(crystal, source));
        }
    }
}