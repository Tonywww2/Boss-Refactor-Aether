package com.tonywww.bossrefactoraether.mixin;

import com.aetherteam.aether.entity.monster.dungeon.boss.goal.AvoidObstaclesGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.BackOffAfterAttackGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.CrushGoal;
import com.aetherteam.aether.entity.monster.dungeon.boss.goal.SetPathUpOrDownGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
    CrushGoal.class,
    AvoidObstaclesGoal.class,
    BackOffAfterAttackGoal.class,
    SetPathUpOrDownGoal.class
})
public abstract class SliderPathGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void bossRefactorAether$suppressPathing(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}