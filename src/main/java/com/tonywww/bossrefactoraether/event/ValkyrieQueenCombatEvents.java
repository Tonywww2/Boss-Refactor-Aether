package com.tonywww.bossrefactoraether.event;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.aetherteam.aether.entity.projectile.crystal.ThunderCrystal;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenCombatService;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BossRefactorAether.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ValkyrieQueenCombatEvents {
    private ValkyrieQueenCombatEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ValkyrieQueen queen)
                || queen.level().isClientSide()
                || event.getAmount() <= 0.0F) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living
                ? living
                : null;
        ValkyrieQueenCombatService.onDamaged(queen, attacker);
    }

    @SubscribeEvent
    public static void onThunderCrystalDamage(LivingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ThunderCrystal crystal)
                || !(crystal.getOwner() instanceof ValkyrieQueen queen)
                || queen.level().isClientSide()) {
            return;
        }
        double attackDamage = queen.getAttribute(Attributes.ATTACK_DAMAGE) != null
                ? queen.getAttributeValue(Attributes.ATTACK_DAMAGE)
                : 13.5;
        BossRefactorAetherConfig.DamageFormula formula =
                BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.thunderCrystal;
        event.setAmount((float) (formula.baseDamage.get()
                + attackDamage * formula.attackDamageMultiplier.get()));
    }
}