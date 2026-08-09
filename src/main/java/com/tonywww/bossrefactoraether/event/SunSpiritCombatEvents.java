package com.tonywww.bossrefactoraether.event;

import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.aetherteam.aether.entity.projectile.crystal.AbstractCrystal;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritCombatService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BossRefactorAether.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SunSpiritCombatEvents {
    private SunSpiritCombatEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof SunSpirit)
            && event.getSource().getDirectEntity() instanceof AbstractCrystal crystal
                && SunSpiritCombatService.isManagedProjectile(crystal)) {
            event.setAmount(SunSpiritCombatService.projectileDamage(
                    crystal, event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()
                || event.getBlockedDamage() <= 0.0F) {
            return;
        }
        if (event.getDamageSource().getDirectEntity() instanceof AbstractCrystal crystal
                && SunSpiritCombatService.isManagedProjectile(crystal)) {
            SunSpiritCombatService.blockManagedProjectile(player, crystal);
            return;
        }
        if (!(event.getDamageSource().getEntity() instanceof SunSpirit sunSpirit)
                || !SunSpiritCombatService.isCurrentAttackParryable(sunSpirit)) {
            return;
        }
        ItemStack shield = player.getUseItem();
        if (shield.canPerformAction(ToolActions.SHIELD_BLOCK)) {
            player.getCooldowns().addCooldown(
                    shield.getItem(),
                    BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                        .shieldCooldownTicks.get());
            player.stopUsingItem();
        }
    }
}