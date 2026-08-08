package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.CollideGoal;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CollideGoal.class)
public abstract class CollideGoalMixin {
    @Shadow(remap = false)
    @Final
    private Slider slider;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$suppressCollision(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$stopCollision(CallbackInfo callback) {
        callback.cancel();
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean bossRefactorAether$scaleCollisionDamage(
            Entity entity, DamageSource source, float amount) {
        if (entity instanceof Player player
                && SliderCombatService.tryShieldBlock(player, slider, source)) {
            return false;
        }
        boolean damaged = entity.hurt(
            source, SliderCombatService.normalCollisionDamage(slider, amount));
        if (damaged && entity instanceof Player) {
            SliderCombatService.markNormalMoveHit(slider);
        }
        return damaged;
    }
}