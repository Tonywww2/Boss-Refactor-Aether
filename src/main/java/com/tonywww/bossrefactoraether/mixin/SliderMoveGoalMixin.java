package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.goal.SliderMoveGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SliderMoveGoal.class)
public abstract class SliderMoveGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$suppressMove(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$stopMove(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}