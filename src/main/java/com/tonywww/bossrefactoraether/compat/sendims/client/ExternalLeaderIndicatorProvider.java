package com.tonywww.bossrefactoraether.compat.sendims.client;

import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.ParryIndicatorStyle;
import com.tonywww.slashblade_sendims.api.leader.client.ClientLeaderIndicatorApi;
import net.minecraft.world.entity.LivingEntity;

import java.util.OptionalDouble;

public final class ExternalLeaderIndicatorProvider {
    private ExternalLeaderIndicatorProvider() {
    }

    public static void register() {
        ClientLeaderIndicatorApi.registerExternalWarningProvider(
                ExternalLeaderIndicatorProvider::warningProgress);
    }

    private static OptionalDouble warningProgress(LivingEntity entity) {
        if (!isSupportedBoss(entity)) {
            return OptionalDouble.empty();
        }
        if (!(entity instanceof AttackTelegraphAccess access)) {
            return OptionalDouble.of(1.0);
        }
        AttackTelegraph telegraph = access.bossRefactorAether$getAttackTelegraph();
        return ParryIndicatorStyle.isVisible(true, telegraph)
                ? OptionalDouble.of(telegraph.progress())
                : OptionalDouble.of(1.0);
    }

    private static boolean isSupportedBoss(LivingEntity entity) {
        return entity instanceof Slider
                || entity instanceof ValkyrieQueen
                || entity instanceof SunSpirit;
    }
}