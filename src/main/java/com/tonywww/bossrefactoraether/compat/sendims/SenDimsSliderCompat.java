package com.tonywww.bossrefactoraether.compat.sendims;

import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.compat.OptionalCompatBootstrap;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import com.tonywww.bossrefactoraether.slider.SliderParryBridge;
import com.tonywww.bossrefactoraether.slider.SliderParryIntegration;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritCombatService;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritParryBridge;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritParryIntegration;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;

public final class SenDimsSliderCompat
    implements SliderParryBridge, ValkyrieQueenParryBridge, SunSpiritParryBridge {
    private static final String CLIENT_INDICATOR_PROVIDER =
            "com.tonywww.bossrefactoraether.compat.sendims.client.ExternalLeaderIndicatorProvider";

    private static final int MINIMUM_PARRIED_TICKS = 30;
    private static final int MAXIMUM_PARRIED_TICKS = 100;

    private SenDimsSliderCompat() {
    }

    public static void initialize() {
        SenDimsSliderCompat compat = new SenDimsSliderCompat();
        // 先注册事件：事件类缺失时在这里失败，后面的 Leader 注册和桥接都还没做。
        MinecraftForge.EVENT_BUS.register(compat);
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
        boolean sunSpiritRegistered = LeaderApi.registerLeaderType(
            AetherEntityTypes.SUN_SPIRIT.get(), LeaderProfile.EXTERNAL);
        if (!sunSpiritRegistered) {
            BossRefactorAether.LOGGER.warn(
                "A conflicting Leader profile is already registered for aether:sun_spirit");
        }
        SliderParryIntegration.install(compat);
        ValkyrieQueenParryIntegration.install(compat);
        SunSpiritParryIntegration.install(compat);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientIndicatorProvider();
        }
        BossRefactorAether.LOGGER.info(
            "Enabled SlashBlade SenDimS Slider, Valkyrie Queen, and Sun Spirit integration");
    }

    private static void registerClientIndicatorProvider() {
        try {
            Class<?> provider = Class.forName(CLIENT_INDICATOR_PROVIDER);
            provider.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            BossRefactorAether.LOGGER.warn(
                    "Unable to register SenDimS external Leader indicator provider",
                    exception);
        }
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
    public boolean isParryable(Slider slider) {
        return LeaderApi.isParryable(slider);
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
    public boolean isParryable(ValkyrieQueen queen) {
        return LeaderApi.isParryable(queen);
    }

    @Override
    public boolean openWindow(SunSpirit sunSpirit) {
        return LeaderApi.openParryWindow(sunSpirit);
    }

    @Override
    public boolean closeWindow(SunSpirit sunSpirit) {
        return LeaderApi.closeParryWindow(sunSpirit);
    }

    @Override
    public boolean isParryable(SunSpirit sunSpirit) {
        return LeaderApi.isParryable(sunSpirit);
    }

    @Override
    public void mirrorBarrierBreak(Slider slider, @Nullable LivingEntity actor,
                                   ResourceLocation sourceId) {
        int ticks = parriedTicks(BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get());
        ParryResult result = LeaderApi.enterParriedState(
                slider,
                actor,
                sourceId,
                ticks,
                ticks
        );
        if (result != ParryResult.SUCCESS) {
            BossRefactorAether.LOGGER.debug(
                    "Could not mirror Slider barrier break to SenDimS: {}", result);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onParryAttempt(LeaderParryAttemptEvent event) {
        if (!SliderParryIntegration.bridge().isAvailable()) {
            return;
        }
        try {
            handleParryAttempt(event);
        } catch (LinkageError error) {
            OptionalCompatBootstrap.disableSenDims(
                    "incompatible SenDimS parry attempt event", error);
        }
    }

    private static void handleParryAttempt(LeaderParryAttemptEvent event) {
        if (event.getTarget() instanceof SunSpirit sunSpirit
                && SunSpiritCombatService.isCurrentAttackParryable(sunSpirit)) {
            SunSpiritCombatService.acceptParry(sunSpirit);
            completeParry(event, BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.parryRecoveryTicks.get());
            return;
        }
        if (event.getTarget() instanceof ValkyrieQueen queen
                && ValkyrieQueenCombatService.isCurrentAttackParryable(queen)) {
            ValkyrieQueenCombatService.acceptParry(queen);
            completeParry(event, BossRefactorAetherConfig
                .VALKYRIE_QUEEN_TIMING.parryRecoveryTicks.get());
            return;
        }
        if (!(event.getTarget() instanceof Slider slider)
                || !SliderCombatService.isCurrentAttackParryable(slider)) {
            return;
        }

        int parryRecoveryTicks = BossRefactorAetherConfig
            .SLIDER_COMBAT.parryRecoveryTicks.get();
        int previousLayers = SliderCombatService.state(slider).getBarrierLayers();
        if (previousLayers <= 0) {
            SliderCombatService.acceptParryWithoutBarrier(slider);
            completeParry(event, parryRecoveryTicks);
            return;
        }
        int remaining = SliderCombatService.consumeBarrierFromParryAttempt(
            slider, event.getActor());
        completeParry(event, remaining > 0
            ? parryRecoveryTicks
            : Math.max(parryRecoveryTicks,
                BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get()));
    }

    private static int parriedTicks(int ticks) {
        return Math.min(MAXIMUM_PARRIED_TICKS, Math.max(MINIMUM_PARRIED_TICKS, ticks));
    }

    private static void completeParry(LeaderParryAttemptEvent event, int ticks) {
        int feedbackTicks = parriedTicks(ticks);
        event.setDecision(LeaderParryDecision.PARRY);
        event.setParriedTicks(feedbackTicks);
        event.setStunTicks(feedbackTicks);
    }
}