package com.tonywww.bossrefactoraether.compat.sendims;

import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import com.tonywww.bossrefactoraether.slider.SliderParryBridge;
import com.tonywww.bossrefactoraether.slider.SliderParryIntegration;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenCombatService;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenParryBridge;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenParryIntegration;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderParryDecision;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAttemptEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

public final class SenDimsSliderCompat
    implements SliderParryBridge, ValkyrieQueenParryBridge {
    private SenDimsSliderCompat() {
    }

    public static void initialize() {
        SenDimsSliderCompat compat = new SenDimsSliderCompat();
        boolean registered = LeaderApi.registerLeaderType(
                AetherEntityTypes.SLIDER.get(), LeaderProfile.EXTERNAL);
        if (!registered) {
            BossRefactorAether.LOGGER.warn(
                    "A conflicting Leader profile is already registered for aether:slider");
        }
        boolean queenRegistered = LeaderApi.registerLeaderType(
            AetherEntityTypes.VALKYRIE_QUEEN.get(), LeaderProfile.EXTERNAL);
        if (!queenRegistered) {
            BossRefactorAether.LOGGER.warn(
                "A conflicting Leader profile is already registered for aether:valkyrie_queen");
        }
        SliderParryIntegration.install(compat);
        ValkyrieQueenParryIntegration.install(compat);
        MinecraftForge.EVENT_BUS.register(compat);
        BossRefactorAether.LOGGER.info(
            "Enabled SlashBlade SenDimS Slider and Valkyrie Queen integration");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean openWindow(Slider slider) {
        return LeaderApi.openParryWindow(slider);
    }

    @Override
    public boolean closeWindow(Slider slider) {
        return LeaderApi.closeParryWindow(slider);
    }

    @Override
    public boolean openWindow(ValkyrieQueen queen) {
        return LeaderApi.openParryWindow(queen);
    }

    @Override
    public boolean closeWindow(ValkyrieQueen queen) {
        return LeaderApi.closeParryWindow(queen);
    }

    @Override
    public void mirrorBarrierBreak(Slider slider, @Nullable LivingEntity actor,
                                   ResourceLocation sourceId) {
        ParryResult result = LeaderApi.enterParriedState(
                slider,
                actor,
                sourceId,
                Math.max(1, BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get()),
                Math.max(1, BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get())
        );
        if (result != ParryResult.SUCCESS) {
            BossRefactorAether.LOGGER.debug(
                    "Could not mirror Slider barrier break to SenDimS: {}", result);
        }
    }

    @SubscribeEvent
    public void onParryAttempt(LeaderParryAttemptEvent event) {
        if (event.getTarget() instanceof ValkyrieQueen queen
                && ValkyrieQueenCombatService.isCurrentAttackParryable(queen)) {
            ValkyrieQueenCombatService.acceptParry(queen);
                int recoveryTicks = Math.max(1, BossRefactorAetherConfig
                    .VALKYRIE_QUEEN_TIMING.parryRecoveryTicks.get());
                event.setParriedTicks(recoveryTicks);
                event.setStunTicks(recoveryTicks);
            return;
        }
        if (!(event.getTarget() instanceof Slider slider)
                || !SliderCombatService.isCurrentAttackParryable(slider)) {
            return;
        }

        int previousLayers = SliderCombatService.state(slider).getBarrierLayers();
        if (previousLayers <= 0) {
            SliderCombatService.acceptParryWithoutBarrier(slider);
            event.setDecision(LeaderParryDecision.ABSORB);
            return;
        }
        int remaining = SliderCombatService.consumeBarrierFromParryAttempt(
            slider, event.getActor());
        if (remaining > 0) {
            event.setDecision(LeaderParryDecision.ABSORB);
        } else {
                int stunTicks = Math.max(1,
                    BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get());
                event.setParriedTicks(stunTicks);
                event.setStunTicks(stunTicks);
        }
    }
}