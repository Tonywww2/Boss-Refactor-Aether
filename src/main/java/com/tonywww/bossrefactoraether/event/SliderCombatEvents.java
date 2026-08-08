package com.tonywww.bossrefactoraether.event;

import com.aetherteam.aether.block.AetherBlocks;
import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.slider.SliderBarrierBreakCause;
import com.tonywww.bossrefactoraether.slider.SliderCombatService;
import com.tonywww.bossrefactoraether.slider.SliderMechanics;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BossRefactorAether.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SliderCombatEvents {
    private SliderCombatEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof Slider slider)
                || player.level().isClientSide()
                || !isPickaxe(player.getMainHandItem())
                || !SliderMechanics.isFullyChargedAttack(
                    player.getAttackStrengthScale(0.5F),
                    BossRefactorAetherConfig.SLIDER_COMBAT
                        .fullPickaxeAttackStrength.get())) {
            return;
        }
        SliderCombatService.recordChargedPickaxeAttack(slider, player);
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Slider
                && BossRefactorAetherConfig.SLIDER_COMBAT.immuneToNegativeEffects.get()
                && event.getEffectInstance().getEffect().getCategory()
                        == MobEffectCategory.HARMFUL) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Slider slider)
                || slider.level().isClientSide()
                || isPickaxeDamage(event.getSource())) {
            return;
        }
        event.setAmount(event.getAmount() * SliderCombatService.barrierDamageMultiplier(slider));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Slider slider)
                || slider.level().isClientSide()
                || event.getAmount() <= 0.0F
                || !(event.getSource().getDirectEntity() instanceof Player player)
                || !isPickaxe(player.getMainHandItem())
                || !SliderCombatService.consumeChargedPickaxeAttack(slider, player)) {
            return;
        }
        SliderCombatService.consumeBarrier(
                slider, SliderBarrierBreakCause.PICKAXE, player, true);
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()
                || event.getBlockedDamage() <= 0.0F) {
            return;
        }
        SliderCombatService.acceptShieldBlock(player, event.getDamageSource(), false);
    }

    private static boolean isPickaxeDamage(DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)) {
            return false;
        }
        ItemStack weapon = attacker.getMainHandItem();
        return isPickaxe(weapon);
    }

    private static boolean isPickaxe(ItemStack weapon) {
        return weapon.canPerformAction(ToolActions.PICKAXE_DIG)
                || weapon.isCorrectToolForDrops(
                        AetherBlocks.CARVED_STONE.get().defaultBlockState());
    }
}