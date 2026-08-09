package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.SliderMoveGoal;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SliderMoveGoal.class)
public abstract class SliderMoveGoalMixin {
    @Shadow(remap = false)
    @Final
    private Slider slider;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$suppressMove(CallbackInfoReturnable<Boolean> callback) {
        if (SliderCombatService.shouldOverrideOriginalMovement(slider)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$stopMove(CallbackInfoReturnable<Boolean> callback) {
        if (SliderCombatService.shouldOverrideOriginalMovement(slider)) {
            callback.setReturnValue(false);
        }
    }

}