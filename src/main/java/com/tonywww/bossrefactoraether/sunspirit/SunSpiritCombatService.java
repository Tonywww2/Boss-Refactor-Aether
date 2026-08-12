package com.tonywww.bossrefactoraether.sunspirit;

import com.tonywww.bossrefactoraether.BossRecovery;
import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether.entity.monster.dungeon.FireMinion;
import com.aetherteam.aether.entity.monster.dungeon.boss.SunSpirit;
import com.aetherteam.aether.entity.projectile.crystal.AbstractCrystal;
import com.aetherteam.aether.entity.projectile.crystal.FireCrystal;
import com.aetherteam.aether.entity.projectile.crystal.IceCrystal;
import com.aetherteam.aether.client.AetherSoundEvents;
import com.aetherteam.aether.event.AetherEventDispatch;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.mixin.LivingEntityDamageBlockAccessor;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.core.particles.ParticleTypes;
import com.aetherteam.aether.data.resources.registries.AetherDamageTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class SunSpiritCombatService {
    private static final String MANAGED_PROJECTILE_TAG =
            BossRefactorAether.MOD_ID + ".sun_spirit_projectile";
    private static final String REFLECTED_ICE_TAG =
            BossRefactorAether.MOD_ID + ".reflected_ice";
    private static final String BOSS_UUID_KEY =
            BossRefactorAether.MOD_ID + ".sun_spirit_uuid";
    private static final String PROJECTILE_DAMAGE_KEY =
            BossRefactorAether.MOD_ID + ".sun_spirit_damage";
    private static final String MINION_OWNER_PREFIX =
            BossRefactorAether.MOD_ID + ".sun_spirit_minion.";

    private SunSpiritCombatService() {
    }

    public static SunSpiritCombatState state(SunSpirit sunSpirit) {
        return ((SunSpiritStateAccess) sunSpirit)
                .bossRefactorAether$getSunSpiritCombatState();
    }

    public static void tick(SunSpirit sunSpirit) {
        if (sunSpirit.level().isClientSide()) {
            return;
        }
        SunSpiritCombatState state = state(sunSpirit);
        holdPosition(sunSpirit, state);
        applyKnockbackResistance(sunSpirit,
            BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                .knockbackResistance.get());
        sunSpirit.setFrozen(false);
        sunSpirit.setFrozenDuration(0);

        if (sunSpirit.isDeadOrDying() || !sunSpirit.isBossFight()) {
            state.outOfCombatHealingTicks = BossRecovery.tick(
                    sunSpirit, true, state.outOfCombatHealingTicks);
            if (sunSpirit.isDeadOrDying()) {
                cleanupOwnedEntities(sunSpirit);
            }
            cancelAttack(sunSpirit, state);
            state.flameSigils.clear();
            return;
        }
        applyOwnedMinionKnockbackResistance(sunSpirit);
        tickFlameSigils(sunSpirit, state);

        long gameTime = sunSpirit.level().getGameTime();
        initializeCooldowns(state, gameTime);
        processHealthThresholds(sunSpirit, state);
        updatePhase(sunSpirit, state, gameTime);
        emitPhaseTwoAura(sunSpirit, state);

        LivingEntity target = validTarget(sunSpirit);
        state.outOfCombatHealingTicks = BossRecovery.tick(
            sunSpirit, target == null, state.outOfCombatHealingTicks);
        if (target == null) {
            cancelAttack(sunSpirit, state);
            return;
        }
        faceTarget(sunSpirit, target);
        tickPhaseTwoSigil(sunSpirit, target, state, gameTime);

        switch (state.attackPhase) {
            case IDLE -> tickIdle(sunSpirit, target, state, gameTime);
            case PROJECTILE_WINDUP -> tickProjectileWindup(sunSpirit, target, state);
            case RISING_FLAME_WINDUP -> tickRisingFlameWindup(sunSpirit, target, state);
            case TITAN_FIST_WINDUP -> tickTitanFistWindup(sunSpirit, target, state);
            case SUMMON_WINDUP -> tickSummonWindup(sunSpirit, target, state, gameTime);
            case RECOVERY -> tickRecovery(sunSpirit, state);
        }
        synchronizeParryWindow(sunSpirit, state);
    }

    public static void reset(SunSpirit sunSpirit) {
        SunSpiritCombatState state = state(sunSpirit);
        clearTelegraph(sunSpirit);
        closeParryWindow(sunSpirit, state);
        cleanupOwnedEntities(sunSpirit);
        state.phaseTwo = false;
        state.healthThresholdsSummoned = 0;
        state.attackIndex = 0;
        state.summonReadyAt = 0L;
        state.phaseSigilReadyAt = 0L;
        state.outOfCombatHealingTicks = 0;
        state.resetTransient();
        holdPosition(sunSpirit, state);
    }

    public static void onLoaded(SunSpirit sunSpirit) {
        SunSpiritCombatState state = state(sunSpirit);
        state.resetTransient();
        if (!sunSpirit.level().isClientSide()) {
            clearTelegraph(sunSpirit);
            applyKnockbackResistance(sunSpirit,
                    BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                        .knockbackResistance.get());
            sunSpirit.setFrozen(false);
            sunSpirit.setFrozenDuration(0);
        }
    }

    public static float adjustedIncomingDamage(SunSpirit sunSpirit,
                                               DamageSource source,
                                               float amount) {
        if (isReflectedIceFor(source.getDirectEntity(), sunSpirit)) {
            return SunSpiritMechanics.reflectedIceDamage(
                    sunSpirit.getMaxHealth(),
                    BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                        .reflectedIceHealthRatio.get());
        }
        return SunSpiritMechanics.minionProtectedDamage(
            amount,
            hasOwnedMinions(sunSpirit),
            BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                .minionDamageMultiplier.get());
    }

    public static void onDamaged(SunSpirit sunSpirit, DamageSource source,
                                 boolean hurt) {
        if (sunSpirit.level().isClientSide()) {
            return;
        }
        if (source.getEntity() instanceof Player player
                && SunSpiritMechanics.shouldStartFightFromPlayerAttack(
                    hurt, sunSpirit.isAlive(), sunSpirit.isBossFight())) {
            startFightFromPlayerAttack(sunSpirit, player);
        }
        if (!hurt) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (isReflectedIceFor(direct, sunSpirit)) {
            if (direct != null) {
                direct.discard();
            }
            SunSpiritCombatState state = state(sunSpirit);
            if (state.attackPhase == SunSpiritAttackPhase.SUMMON_WINDUP) {
                state.summonReadyAt = sunSpirit.level().getGameTime()
                        + BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                            .summonCooldownTicks.get();
                enterRecovery(sunSpirit, state, BossRefactorAetherConfig
                        .SUN_SPIRIT_TIMING.summonRecoveryTicks.get());
            }
        }
    }

    private static void startFightFromPlayerAttack(SunSpirit sunSpirit,
                                                   Player player) {
        sunSpirit.setTarget(player);
        sunSpirit.setBossFight(true);
        if (sunSpirit.getDungeon() != null) {
            sunSpirit.closeRoom();
        }
        sunSpirit.level().playSound(null, sunSpirit.blockPosition(),
                AetherSoundEvents.ENTITY_SUN_SPIRIT_ACTIVATE.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        AetherEventDispatch.onBossFightStart(sunSpirit, sunSpirit.getDungeon());
    }

    public static boolean isCurrentAttackParryable(SunSpirit sunSpirit) {
        return state(sunSpirit).attackPhase.isParryBreak();
    }

    public static void acceptParry(SunSpirit sunSpirit) {
        if (!isCurrentAttackParryable(sunSpirit)) {
            return;
        }
        enterRecovery(sunSpirit, state(sunSpirit), BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.parryRecoveryTicks.get());
    }

    public static boolean isManagedProjectile(@Nullable Entity entity) {
        return entity instanceof AbstractCrystal
                && entity.getTags().contains(MANAGED_PROJECTILE_TAG);
    }

    public static boolean deflectManagedProjectile(AbstractCrystal crystal,
                                                   DamageSource source) {
        if (!isManagedProjectile(crystal) || isSlashBladeDamage(source)) {
            return false;
        }
        if (crystal.level().isClientSide()) {
            return true;
        }
        if (crystal instanceof IceCrystal) {
            reflectIceCrystal(crystal, source.getEntity() != null
                    ? source.getEntity() : source.getDirectEntity());
        } else {
            crystal.discard();
        }
        return true;
    }

    public static void blockManagedProjectile(Player player,
                                              AbstractCrystal crystal) {
        if (crystal instanceof IceCrystal) {
            reflectIceCrystal(crystal, player);
        } else {
            crystal.discard();
        }
    }

    public static float projectileDamage(AbstractCrystal crystal,
                                         float fallback) {
        return crystal.getPersistentData().contains(PROJECTILE_DAMAGE_KEY)
                ? crystal.getPersistentData().getFloat(PROJECTILE_DAMAGE_KEY)
                : fallback;
    }

    public static void handleManagedProjectileHit(
            AbstractCrystal crystal, EntityHitResult hitResult) {
        if (!isManagedProjectile(crystal) || crystal.level().isClientSide()) {
            return;
        }
        Entity target = hitResult.getEntity();
        if (target == crystal.getOwner() || !(target instanceof LivingEntity living)) {
            return;
        }
        DamageSource source = AetherDamageTypes.indirectEntityDamageSource(
                crystal.level(),
                crystal instanceof IceCrystal
                    ? AetherDamageTypes.ICE_CRYSTAL
                    : AetherDamageTypes.FIRE_CRYSTAL,
                crystal,
                crystal.getOwner());
            if (living instanceof Player player
                && ((LivingEntityDamageBlockAccessor) player)
                    .bossRefactorAether$isDamageSourceBlocked(source)) {
                blockManagedProjectile(player, crystal);
                return;
            }
        living.hurt(source, projectileDamage(crystal, 0.0F));
            if (crystal.isAlive()) {
            crystal.discard();
        }
    }

    public static boolean isSlashBladeDamage(DamageSource source) {
        if (hasSlashBladeIdentity(source.getDirectEntity())
                || hasSlashBladeIdentity(source.getEntity())) {
            return true;
        }
        return source.typeHolder().unwrapKey()
            .map(key -> SunSpiritMechanics.isSlashBladeIdentifier(
                key.location().getNamespace()))
                .orElse(false);
    }

    private static void tickIdle(SunSpirit sunSpirit, LivingEntity target,
                                 SunSpiritCombatState state, long gameTime) {
        if (!hasOwnedMinions(sunSpirit) && gameTime >= state.summonReadyAt) {
            startSummon(sunSpirit, target, state);
            return;
        }
        switch (SunSpiritMechanics.attackForIndex(state.attackIndex++)) {
            case PROJECTILE -> startProjectile(sunSpirit, target, state);
            case RISING_FLAME -> startRisingFlame(sunSpirit, target, state);
            case TITAN_FIST -> startTitanFist(sunSpirit, target, state);
        }
    }

    private static void startProjectile(SunSpirit sunSpirit, LivingEntity target,
                                        SunSpiritCombatState state) {
        state.attackPhase = SunSpiritAttackPhase.PROJECTILE_WINDUP;
        state.phaseTicks = 0;
        state.projectileIsIce = sunSpirit.getRandom().nextDouble()
                < BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                    .iceProjectileChance.get();
        lockTargetedAttack(sunSpirit, target, state);
        setTelegraph(sunSpirit, AttackTelegraphShape.CORRIDOR,
            state.attackOrigin,
                state.attackDirection,
            state.attackLength,
                0.65, 0.0, 0.0F);
    }

    private static void tickProjectileWindup(SunSpirit sunSpirit,
                                             LivingEntity target,
                                             SunSpiritCombatState state) {
        faceTarget(sunSpirit, target);
        int windup = BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                .projectileWindupTicks.get();
                                            updateTelegraphProgress(sunSpirit,
                                                AttackTelegraph.windupProgress(state.phaseTicks, windup));
        emitChargeParticles(sunSpirit, state.projectileIsIce);
        if (++state.phaseTicks >= windup) {
                                                launchProjectile(sunSpirit, state, state.projectileIsIce);
            enterRecovery(sunSpirit, state, BossRefactorAetherConfig
                    .SUN_SPIRIT_TIMING.attackRecoveryTicks.get());
        }
    }

    private static void startRisingFlame(SunSpirit sunSpirit, LivingEntity target,
                                         SunSpiritCombatState state) {
        state.attackPhase = SunSpiritAttackPhase.RISING_FLAME_WINDUP;
        state.phaseTicks = 0;
        double groundY = target.getBoundingBox().minY;
        state.attackOrigin = SunSpiritMechanics.groundTelegraphOrigin(
            sunSpirit.position(), groundY);
        state.attackTarget = new Vec3(target.getX(), groundY, target.getZ());
        openParryWindow(sunSpirit, state);
        setTelegraph(sunSpirit, AttackTelegraphShape.CIRCLE,
                state.attackOrigin,
                horizontalLook(sunSpirit), 0.0, 0.0,
                BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                    .risingFlameRadius.get(), 0.0F);
    }

    private static void tickRisingFlameWindup(SunSpirit sunSpirit,
                                              LivingEntity target,
                                              SunSpiritCombatState state) {
        int windup = BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                .risingFlameWindupTicks.get();
        updateTelegraphProgress(sunSpirit,
            AttackTelegraph.windupProgress(state.phaseTicks, windup));
        emitChargeParticles(sunSpirit, false);
        if (++state.phaseTicks >= windup) {
            damageCircle(sunSpirit, state.attackOrigin,
                    BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                        .risingFlameRadius.get(),
                    BossRefactorAetherConfig.SUN_SPIRIT_DAMAGE.risingFlame,
                    false);
            createFlameSigil(state, groundPosition(target));
            emitRisingFlame(sunSpirit, state.attackOrigin);
            closeParryWindow(sunSpirit, state);
            enterRecovery(sunSpirit, state, BossRefactorAetherConfig
                    .SUN_SPIRIT_TIMING.attackRecoveryTicks.get());
        }
    }

    private static void startTitanFist(SunSpirit sunSpirit, LivingEntity target,
                                       SunSpiritCombatState state) {
        state.attackPhase = SunSpiritAttackPhase.TITAN_FIST_WINDUP;
        state.phaseTicks = 0;
        state.extraTitanFistPending = state.phaseTwo;
        lockTargetedAttack(sunSpirit, target, state);
        openParryWindow(sunSpirit, state);
        setTitanFistTelegraph(sunSpirit, state, 0.0F);
    }

    private static void tickTitanFistWindup(SunSpirit sunSpirit,
                                            LivingEntity target,
                                            SunSpiritCombatState state) {
        faceTarget(sunSpirit, target);
        int windup = BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                .titanFistWindupTicks.get();
                                            updateTelegraphProgress(sunSpirit,
                                                AttackTelegraph.windupProgress(state.phaseTicks, windup));
        emitChargeParticles(sunSpirit, false);
        if (++state.phaseTicks < windup) {
            return;
        }

        damageTitanFist(sunSpirit, state.attackOrigin, state.attackDirection);
        emitTitanFist(sunSpirit, state.attackOrigin, state.attackDirection);
        if (state.extraTitanFistPending) {
            state.extraTitanFistPending = false;
            state.phaseTicks = 0;
            lockTargetedAttack(sunSpirit, target, state);
            setTitanFistTelegraph(sunSpirit, state, 0.0F);
            return;
        }
        closeParryWindow(sunSpirit, state);
        enterRecovery(sunSpirit, state, BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.attackRecoveryTicks.get());
    }

    private static void startSummon(SunSpirit sunSpirit, LivingEntity target,
                                    SunSpiritCombatState state) {
        state.attackPhase = SunSpiritAttackPhase.SUMMON_WINDUP;
        state.phaseTicks = 0;
        state.attackOrigin = SunSpiritMechanics.groundTelegraphOrigin(
                sunSpirit.position(), target.getBoundingBox().minY);
        setTelegraph(sunSpirit, AttackTelegraphShape.CIRCLE,
                state.attackOrigin,
                horizontalLook(sunSpirit), 0.0, 0.0,
                BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                    .summonTelegraphRadius.get(), 0.0F);
    }

    private static void tickSummonWindup(SunSpirit sunSpirit,
                                         LivingEntity target,
                                         SunSpiritCombatState state,
                                         long gameTime) {
        int windup = BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                .summonWindupTicks.get();
        updateTelegraphProgress(sunSpirit,
            AttackTelegraph.windupProgress(state.phaseTicks, windup));
        emitSummonParticles(sunSpirit, state.attackOrigin);
        if (++state.phaseTicks < windup) {
            return;
        }
        if (!hasOwnedMinions(sunSpirit)) {
            spawnMinions(sunSpirit, target, state.attackOrigin.y,
                BossRefactorAetherConfig
                    .SUN_SPIRIT_COMBAT.emptyFieldSummonCount.get());
        }
        state.summonReadyAt = gameTime + BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.summonCooldownTicks.get();
        enterRecovery(sunSpirit, state, BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.summonRecoveryTicks.get());
    }

    private static void tickRecovery(SunSpirit sunSpirit,
                                     SunSpiritCombatState state) {
        if (++state.phaseTicks >= state.recoveryTicks) {
            state.attackPhase = SunSpiritAttackPhase.IDLE;
            state.phaseTicks = 0;
            state.recoveryTicks = 0;
        }
    }

    private static void enterRecovery(SunSpirit sunSpirit,
                                      SunSpiritCombatState state, int ticks) {
        closeParryWindow(sunSpirit, state);
        clearTelegraph(sunSpirit);
        state.attackPhase = SunSpiritAttackPhase.RECOVERY;
        state.phaseTicks = 0;
        state.recoveryTicks = Math.max(0, ticks);
        state.extraTitanFistPending = false;
        clearAttackSnapshot(state);
    }

    private static void initializeCooldowns(SunSpiritCombatState state,
                                            long gameTime) {
        if (state.summonReadyAt == 0L) {
            state.summonReadyAt = gameTime + BossRefactorAetherConfig
                    .SUN_SPIRIT_TIMING.initialSummonDelayTicks.get();
        }
    }

    private static void processHealthThresholds(SunSpirit sunSpirit,
                                                SunSpiritCombatState state) {
        int crossed = SunSpiritMechanics.crossedHealthThresholds(
                sunSpirit.getHealth(), sunSpirit.getMaxHealth(),
                BossRefactorAetherConfig.SUN_SPIRIT_COMBAT.healthPerMinion.get());
        if (crossed <= state.healthThresholdsSummoned) {
            return;
        }
        int thresholdCount = crossed - state.healthThresholdsSummoned;
        state.healthThresholdsSummoned = crossed;
        LivingEntity target = validTarget(sunSpirit);
        double spawnY = target != null
            ? target.getBoundingBox().minY
            : sunSpirit.getY();
        spawnMinions(sunSpirit, target, spawnY,
            thresholdCount * BossRefactorAetherConfig
                .SUN_SPIRIT_COMBAT.minionsPerHealthThreshold.get());
    }

    private static void updatePhase(SunSpirit sunSpirit,
                                    SunSpiritCombatState state,
                                    long gameTime) {
        if (!state.phaseTwo && SunSpiritMechanics.isPhaseTwo(
                sunSpirit.getHealth(), sunSpirit.getMaxHealth(),
                BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                    .phaseTwoHealthRatio.get())) {
            state.phaseTwo = true;
            state.phaseSigilReadyAt = gameTime + BossRefactorAetherConfig
                    .SUN_SPIRIT_TIMING.initialPhaseTwoSigilDelayTicks.get();
            if (sunSpirit.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.FLASH,
                    sunSpirit.getX(), sunSpirit.getY() + 1.0, sunSpirit.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    sunSpirit.getX(), sunSpirit.getY() + 1.0, sunSpirit.getZ(),
                    100, 1.8, 1.5, 1.8, 0.06);
                level.sendParticles(ParticleTypes.FLAME,
                    sunSpirit.getX(), sunSpirit.getY() + 1.0, sunSpirit.getZ(),
                    80, 1.5, 1.5, 1.5, 0.08);
                level.sendParticles(ParticleTypes.LAVA,
                    sunSpirit.getX(), sunSpirit.getY() + 0.75, sunSpirit.getZ(),
                    24, 1.2, 0.8, 1.2, 0.03);
                level.playSound(null, sunSpirit.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE,
                    2.0F, 0.55F);
            }
        }
    }

    private static void tickPhaseTwoSigil(SunSpirit sunSpirit,
                                          LivingEntity target,
                                          SunSpiritCombatState state,
                                          long gameTime) {
        if (!state.phaseTwo || gameTime < state.phaseSigilReadyAt) {
            return;
        }
        createFlameSigil(state, groundPosition(target));
        state.phaseSigilReadyAt = gameTime + BossRefactorAetherConfig
                .SUN_SPIRIT_TIMING.phaseTwoSigilIntervalTicks.get();
    }

    private static void createFlameSigil(SunSpiritCombatState state,
                                         Vec3 position) {
        state.flameSigils.add(new SunSpiritFlameSigil(
                position,
                BossRefactorAetherConfig.SUN_SPIRIT_TIMING
                    .flameSigilDelayTicks.get()));
    }

    private static void tickFlameSigils(SunSpirit sunSpirit,
                                        SunSpiritCombatState state) {
        if (!(sunSpirit.level() instanceof ServerLevel level)) {
            return;
        }
        Iterator<SunSpiritFlameSigil> iterator = state.flameSigils.iterator();
        while (iterator.hasNext()) {
            SunSpiritFlameSigil sigil = iterator.next();
            if (!sigil.advanceAndShouldErupt()) {
                emitSigilTelegraph(level, sigil);
                continue;
            }
            damageCircle(sunSpirit, sigil.position,
                    BossRefactorAetherConfig.SUN_SPIRIT_RANGE.flameSigilRadius.get(),
                    BossRefactorAetherConfig.SUN_SPIRIT_DAMAGE.flameSigil,
                    true);
            emitSigilEruption(level, sigil.position);
            iterator.remove();
        }
    }

    private static void launchProjectile(SunSpirit sunSpirit,
                                         SunSpiritCombatState state,
                                         boolean ice) {
        ServerLevel level = (ServerLevel) sunSpirit.level();
        AbstractCrystal crystal = ice
                ? new IceCrystal(level, sunSpirit)
                : new FireCrystal(level, sunSpirit);
        Vec3 origin = sunSpirit.getEyePosition();
        Vec3 direction = state.projectileAim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-8) {
            direction = horizontalLook(sunSpirit);
        } else {
            direction = direction.normalize();
        }
        crystal.setPos(origin.x, origin.y, origin.z);
        crystal.setDeltaMovement(direction.scale(BossRefactorAetherConfig
                .SUN_SPIRIT_RANGE.projectileSpeed.get()));
        crystal.addTag(MANAGED_PROJECTILE_TAG);
        crystal.getPersistentData().putUUID(BOSS_UUID_KEY, sunSpirit.getUUID());
        crystal.getPersistentData().putFloat(PROJECTILE_DAMAGE_KEY,
                configuredDamage(sunSpirit,
                    BossRefactorAetherConfig.SUN_SPIRIT_DAMAGE.projectile));
        level.addFreshEntity(crystal);
        level.playSound(null, sunSpirit.blockPosition(),
                ice ? SoundEvents.GLASS_BREAK : SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 1.5F, ice ? 1.25F : 0.85F);
    }

    private static void reflectIceCrystal(AbstractCrystal crystal,
                                          @Nullable Entity reflector) {
        SunSpirit sunSpirit = findOwningSunSpirit(crystal);
        if (sunSpirit == null) {
            crystal.discard();
            return;
        }
        Vec3 direction = sunSpirit.getEyePosition().subtract(crystal.position());
        if (direction.lengthSqr() < 1.0E-8) {
            direction = new Vec3(0.0, 0.0, 1.0);
        }
        crystal.setDeltaMovement(direction.normalize().scale(BossRefactorAetherConfig
                .SUN_SPIRIT_RANGE.projectileSpeed.get()));
        crystal.setOwner(reflector);
        crystal.addTag(REFLECTED_ICE_TAG);
        crystal.level().playSound(null, crystal.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.35F);
    }

    private static void spawnMinions(SunSpirit sunSpirit,
                                     @Nullable LivingEntity target,
                                     double spawnY, int count) {
        if (!(sunSpirit.level() instanceof ServerLevel level) || count <= 0) {
            return;
        }
        double radius = BossRefactorAetherConfig.SUN_SPIRIT_RANGE.summonRadius.get();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0 * index / Math.max(1, count)
                    + sunSpirit.getRandom().nextDouble() * 0.35;
            FireMinion minion = new FireMinion(
                    AetherEntityTypes.FIRE_MINION.get(), level);
            minion.setPos(
                    sunSpirit.getX() + Math.cos(angle) * radius,
                    spawnY,
                    sunSpirit.getZ() + Math.sin(angle) * radius);
            minion.addTag(minionOwnerTag(sunSpirit));
            minion.setTarget(target);
            applyKnockbackResistance(minion,
                    BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                        .minionKnockbackResistance.get());
            level.addFreshEntity(minion);
        }
        level.sendParticles(ParticleTypes.FLAME,
                sunSpirit.getX(), sunSpirit.getY() + 0.5, sunSpirit.getZ(),
                40, radius, 0.8, radius, 0.04);
        level.playSound(null, sunSpirit.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.65F);
    }

    private static boolean hasOwnedMinions(SunSpirit sunSpirit) {
        if (!(sunSpirit.level() instanceof ServerLevel level)) {
            return false;
        }
        double range = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                .minionSearchRange.get();
        String ownerTag = minionOwnerTag(sunSpirit);
        return !level.getEntitiesOfClass(
                FireMinion.class,
                sunSpirit.getBoundingBox().inflate(range),
                minion -> minion.isAlive() && minion.getTags().contains(ownerTag))
            .isEmpty();
    }

    private static void applyOwnedMinionKnockbackResistance(SunSpirit sunSpirit) {
        if (!(sunSpirit.level() instanceof ServerLevel level)) {
            return;
        }
        double range = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                .minionSearchRange.get();
        String ownerTag = minionOwnerTag(sunSpirit);
        for (FireMinion minion : level.getEntitiesOfClass(
                FireMinion.class,
                sunSpirit.getBoundingBox().inflate(range),
                entity -> entity.isAlive() && entity.getTags().contains(ownerTag))) {
            applyKnockbackResistance(minion,
                    BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                        .minionKnockbackResistance.get());
        }
    }

    private static void damageTitanFist(SunSpirit sunSpirit, Vec3 origin,
                                        Vec3 direction) {
        double length = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .titanFistLength.get();
        double halfWidth = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .titanFistHalfWidth.get();
        double vertical = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .verticalHitRange.get();
        AABB bounds = new AABB(origin, origin)
            .expandTowards(direction.scale(length))
                .inflate(halfWidth, vertical, halfWidth);
        for (Player player : eligiblePlayers(sunSpirit, bounds)) {
            if (Math.abs(player.getY() - origin.y) <= vertical
                    && SunSpiritMechanics.isInsideForwardRectangle(
                origin, direction, player.position(),
                        length, halfWidth)) {
                dealDamage(sunSpirit, player,
                        BossRefactorAetherConfig.SUN_SPIRIT_DAMAGE.titanFist,
                origin, false);
            }
        }
    }

    private static void damageCircle(SunSpirit sunSpirit, Vec3 center,
                                     double radius,
                                     BossRefactorAetherConfig.DamageFormula formula,
                                     boolean ignite) {
        double vertical = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .verticalHitRange.get();
        AABB bounds = new AABB(center, center).inflate(radius, vertical, radius);
        for (Player player : eligiblePlayers(sunSpirit, bounds)) {
            if (Math.abs(player.getY() - center.y) <= vertical
                    && SunSpiritMechanics.isInsideHorizontalCircle(
                        center, player.position(), radius)) {
                dealDamage(sunSpirit, player, formula, center, ignite);
            }
        }
    }

    private static void dealDamage(SunSpirit sunSpirit, Player player,
                                   BossRefactorAetherConfig.DamageFormula formula,
                                   Vec3 sourcePosition, boolean ignite) {
        DamageSource source = new DamageSource(
                sunSpirit.damageSources().mobAttack(sunSpirit).typeHolder(),
                sunSpirit, sunSpirit, sourcePosition);
        if (player.hurt(source, configuredDamage(sunSpirit, formula)) && ignite) {
            player.setSecondsOnFire(BossRefactorAetherConfig
                    .SUN_SPIRIT_TIMING.flameSeconds.get());
        }
    }

    private static float configuredDamage(
            SunSpirit sunSpirit, BossRefactorAetherConfig.DamageFormula formula) {
        AttributeInstance attribute = sunSpirit.getAttribute(Attributes.ATTACK_DAMAGE);
        double attackDamage = attribute != null
                ? attribute.getValue() : SunSpiritMechanics.DEFAULT_ATTACK_DAMAGE;
        double base = formula.baseDamage.get()
                + attackDamage * formula.attackDamageMultiplier.get();
        return (float) (base * SunSpiritMechanics.phaseDamageMultiplier(
                state(sunSpirit).phaseTwo,
                BossRefactorAetherConfig.SUN_SPIRIT_COMBAT
                    .phaseTwoDamageMultiplier.get()));
    }

    private static void applyKnockbackResistance(LivingEntity entity,
                                                 double resistance) {
        AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            attribute.setBaseValue(Math.max(0.0, Math.min(1.0, resistance)));
        }
    }

    @Nullable
    private static LivingEntity validTarget(SunSpirit sunSpirit) {
        LivingEntity target = sunSpirit.getTarget();
        if (isEligibleTarget(target)) {
            return target;
        }
        double range = BossRefactorAetherConfig.SUN_SPIRIT_COMBAT.pursuitRange.get();
        Player nearest = null;
        double nearestDistance = range * range;
        for (Player player : eligiblePlayers(
                sunSpirit, sunSpirit.getBoundingBox().inflate(range))) {
            double distance = sunSpirit.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        sunSpirit.setTarget(nearest);
        return nearest;
    }

    private static boolean isEligibleTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive()
                && (!(target instanceof Player player)
                    || (!player.isCreative() && !player.isSpectator()));
    }

    private static List<Player> eligiblePlayers(SunSpirit sunSpirit, AABB bounds) {
        return sunSpirit.level().getEntitiesOfClass(Player.class, bounds,
                player -> player.isAlive() && !player.isCreative()
                        && !player.isSpectator());
    }

    private static void holdPosition(SunSpirit sunSpirit,
                                     SunSpiritCombatState state) {
        sunSpirit.getNavigation().stop();
        sunSpirit.setDeltaMovement(Vec3.ZERO);
    }

    private static void faceTarget(SunSpirit sunSpirit, LivingEntity target) {
        sunSpirit.getLookControl().setLookAt(target, 40.0F, 40.0F);
    }

    private static void setTitanFistTelegraph(SunSpirit sunSpirit,
                                              SunSpiritCombatState state,
                                              float progress) {
        setTelegraph(sunSpirit, AttackTelegraphShape.CORRIDOR,
            state.attackOrigin,
                state.attackDirection,
                BossRefactorAetherConfig.SUN_SPIRIT_RANGE.titanFistLength.get(),
                BossRefactorAetherConfig.SUN_SPIRIT_RANGE.titanFistHalfWidth.get(),
                0.0, progress);
    }

    private static void setTelegraph(SunSpirit sunSpirit,
                                     AttackTelegraphShape shape,
                                     Vec3 origin, Vec3 direction, double length,
                                     double width, double radius, float progress) {
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-8) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        } else {
            horizontal = horizontal.normalize();
        }
        if (sunSpirit instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(new AttackTelegraph(
                    shape,
                    (float) origin.x,
                    (float) origin.y,
                    (float) origin.z,
                    (float) horizontal.x,
                    (float) horizontal.z,
                    (float) Math.max(0.0, length),
                    (float) Math.max(0.0, width),
                    (float) Math.max(0.0, radius),
                    progress));
        }
    }

    private static void updateTelegraphProgress(SunSpirit sunSpirit, float progress) {
        if (sunSpirit instanceof AttackTelegraphAccess access) {
            AttackTelegraph telegraph = access.bossRefactorAether$getAttackTelegraph();
            if (telegraph.shape() != AttackTelegraphShape.NONE) {
                access.bossRefactorAether$setAttackTelegraph(
                        telegraph.withProgress(progress));
            }
        }
    }

    private static void lockTargetedAttack(SunSpirit sunSpirit,
                                           LivingEntity target,
                                           SunSpiritCombatState state) {
        Vec3 targetPosition = target.position();
        double groundY = target.getBoundingBox().minY;
        state.attackOrigin = SunSpiritMechanics.groundTelegraphOrigin(
                sunSpirit.position(), groundY);
        state.attackTarget = new Vec3(targetPosition.x, groundY, targetPosition.z);
        state.projectileAim = target.getEyePosition();
        state.attackDirection = horizontalDirection(
                state.attackOrigin, state.attackTarget, sunSpirit);
        state.attackLength = horizontalDistance(
                state.attackOrigin, state.attackTarget);
    }

    private static void clearAttackSnapshot(SunSpiritCombatState state) {
        state.attackOrigin = Vec3.ZERO;
        state.attackTarget = Vec3.ZERO;
        state.projectileAim = Vec3.ZERO;
        state.attackDirection = new Vec3(0.0, 0.0, 1.0);
        state.attackLength = 0.0;
    }

    private static void clearTelegraph(SunSpirit sunSpirit) {
        if (sunSpirit instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(AttackTelegraph.NONE);
        }
    }

    private static void cancelAttack(SunSpirit sunSpirit,
                                     SunSpiritCombatState state) {
        if (state.attackPhase == SunSpiritAttackPhase.IDLE
                && state.flameSigils.isEmpty()) {
            clearTelegraph(sunSpirit);
            return;
        }
        closeParryWindow(sunSpirit, state);
        clearTelegraph(sunSpirit);
        state.attackPhase = SunSpiritAttackPhase.IDLE;
        state.phaseTicks = 0;
        state.recoveryTicks = 0;
        state.extraTitanFistPending = false;
        clearAttackSnapshot(state);
    }

    private static void openParryWindow(SunSpirit sunSpirit,
                                        SunSpiritCombatState state) {
        if (!state.parryWindowOpen
                && SunSpiritParryIntegration.bridge().openWindow(sunSpirit)) {
            state.parryWindowOpen = true;
        }
    }

    private static void closeParryWindow(SunSpirit sunSpirit,
                                         SunSpiritCombatState state) {
        if (state.parryWindowOpen) {
            SunSpiritParryIntegration.bridge().closeWindow(sunSpirit);
            state.parryWindowOpen = false;
        }
    }

    private static void synchronizeParryWindow(SunSpirit sunSpirit,
                                                SunSpiritCombatState state) {
        if (isCurrentAttackParryable(sunSpirit)) {
            openParryWindow(sunSpirit, state);
        } else {
            closeParryWindow(sunSpirit, state);
        }
    }

    private static boolean isReflectedIceFor(@Nullable Entity entity,
                                             SunSpirit sunSpirit) {
        if (!(entity instanceof IceCrystal)
                || !entity.getTags().contains(MANAGED_PROJECTILE_TAG)
                || !entity.getTags().contains(REFLECTED_ICE_TAG)) {
            return false;
        }
        return entity.getPersistentData().hasUUID(BOSS_UUID_KEY)
                && entity.getPersistentData().getUUID(BOSS_UUID_KEY)
                    .equals(sunSpirit.getUUID());
    }

    @Nullable
    private static SunSpirit findOwningSunSpirit(Entity projectile) {
        if (!(projectile.level() instanceof ServerLevel level)
                || !projectile.getPersistentData().hasUUID(BOSS_UUID_KEY)) {
            return null;
        }
        UUID id = projectile.getPersistentData().getUUID(BOSS_UUID_KEY);
        Entity entity = level.getEntity(id);
        return entity instanceof SunSpirit sunSpirit ? sunSpirit : null;
    }

    private static boolean hasSlashBladeIdentity(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (SunSpiritMechanics.isSlashBladeIdentifier(entity.getClass().getName())) {
            return true;
        }
        if (entity instanceof LivingEntity living) {
            ItemStack item = living.getMainHandItem();
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item.getItem());
            return key != null
                    && SunSpiritMechanics.isSlashBladeIdentifier(key.getNamespace());
        }
        return false;
    }

    private static String minionOwnerTag(SunSpirit sunSpirit) {
        return MINION_OWNER_PREFIX + sunSpirit.getUUID();
    }

    private static void cleanupOwnedEntities(SunSpirit sunSpirit) {
        if (!(sunSpirit.level() instanceof ServerLevel level)) {
            return;
        }
        double range = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                .minionSearchRange.get();
        String ownerTag = minionOwnerTag(sunSpirit);
        for (FireMinion minion : level.getEntitiesOfClass(
                FireMinion.class, sunSpirit.getBoundingBox().inflate(range),
                entity -> entity.getTags().contains(ownerTag))) {
            minion.discard();
        }
        for (AbstractCrystal crystal : level.getEntitiesOfClass(
                AbstractCrystal.class, sunSpirit.getBoundingBox().inflate(range),
                entity -> entity.getPersistentData().hasUUID(BOSS_UUID_KEY)
                        && entity.getPersistentData().getUUID(BOSS_UUID_KEY)
                            .equals(sunSpirit.getUUID()))) {
            crystal.discard();
        }
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to,
                                            SunSpirit sunSpirit) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return direction.lengthSqr() < 1.0E-8
                ? horizontalLook(sunSpirit) : direction.normalize();
    }

    private static Vec3 horizontalLook(SunSpirit sunSpirit) {
        Vec3 look = sunSpirit.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-8) {
            double radians = Math.toRadians(sunSpirit.getYRot());
            return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        }
        return horizontal.normalize();
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double deltaX = second.x - first.x;
        double deltaZ = second.z - first.z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private static Vec3 groundPosition(LivingEntity entity) {
        return new Vec3(entity.getX(), entity.getBoundingBox().minY, entity.getZ());
    }

    private static void emitPhaseTwoAura(SunSpirit sunSpirit,
                                         SunSpiritCombatState state) {
        if (!state.phaseTwo
                || !(sunSpirit.level() instanceof ServerLevel level)
                || sunSpirit.tickCount % 2 != 0) {
            return;
        }
        double height = Math.max(1.0, sunSpirit.getBbHeight());
        double radius = Math.max(1.1, sunSpirit.getBbWidth() * 0.8);
        double angle = sunSpirit.tickCount * 0.28;
        for (int index = 0; index < 2; index++) {
            double orbit = angle + Math.PI * index;
            double y = sunSpirit.getY() + 0.35
                    + height * (0.5 + 0.3 * Math.sin(orbit * 1.5));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    sunSpirit.getX() + Math.cos(orbit) * radius,
                    y,
                    sunSpirit.getZ() + Math.sin(orbit) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                sunSpirit.getX(), sunSpirit.getY() + height * 0.55,
                sunSpirit.getZ(), 6,
                radius * 0.65, height * 0.4, radius * 0.65, 0.01);
        if (sunSpirit.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.FLAME,
                    sunSpirit.getX(), sunSpirit.getY() + height * 0.5,
                    sunSpirit.getZ(), 8,
                    radius * 0.8, height * 0.45, radius * 0.8, 0.015);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    sunSpirit.getX(), sunSpirit.getY() + height * 0.8,
                    sunSpirit.getZ(), 3,
                    radius * 0.45, height * 0.25, radius * 0.45, 0.01);
        }
        if (sunSpirit.tickCount % 20 == 0) {
            level.sendParticles(ParticleTypes.LAVA,
                    sunSpirit.getX(), sunSpirit.getY() + height * 0.45,
                    sunSpirit.getZ(), 4,
                    radius * 0.55, height * 0.25, radius * 0.55, 0.02);
        }
    }

    private static void emitChargeParticles(SunSpirit sunSpirit, boolean ice) {
        if (!(sunSpirit.level() instanceof ServerLevel level)
                || sunSpirit.tickCount % 2 != 0) {
            return;
        }
        level.sendParticles(ice ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                sunSpirit.getX(), sunSpirit.getY() + 1.0, sunSpirit.getZ(),
                6, 0.8, 0.8, 0.8, 0.02);
    }

    private static void emitSummonParticles(SunSpirit sunSpirit, Vec3 origin) {
        if (!(sunSpirit.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                origin.x, origin.y + 0.5, origin.z,
                8, 2.5, 0.4, 2.5, 0.01);
    }

    private static void emitRisingFlame(SunSpirit sunSpirit, Vec3 origin) {
        if (sunSpirit.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME,
                    origin.x, origin.y + 0.5, origin.z,
                    80, 3.0, 1.0, 3.0, 0.08);
            level.playSound(null, sunSpirit.blockPosition(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE,
                    2.0F, 0.75F);
        }
    }

    private static void emitTitanFist(SunSpirit sunSpirit, Vec3 origin,
                                      Vec3 direction) {
        if (sunSpirit.level() instanceof ServerLevel level) {
            Vec3 center = origin.add(direction.scale(
                    BossRefactorAetherConfig.SUN_SPIRIT_RANGE
                        .titanFistLength.get() * 0.5));
            level.sendParticles(ParticleTypes.EXPLOSION,
                    center.x, center.y + 0.5, center.z,
                    12, 2.0, 0.8, 2.0, 0.04);
            level.playSound(null, sunSpirit.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    1.8F, 0.7F);
        }
    }

    private static void emitSigilTelegraph(ServerLevel level,
                                           SunSpiritFlameSigil sigil) {
        Vec3 center = sigil.position;
        double radius = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .flameSigilRadius.get();
        double rotation = sigil.remainingTicks * 0.12;
        for (int index = 0; index < 16; index++) {
            double angle = Math.PI * 2.0 * index / 16.0 + rotation;
            level.sendParticles(ParticleTypes.SMALL_FLAME,
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.15,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0 * index / 6.0 - rotation * 1.5;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    center.x + Math.cos(angle) * radius * 0.5,
                    center.y + 0.2,
                    center.z + Math.sin(angle) * radius * 0.5,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.SMOKE,
                center.x, center.y + 0.15, center.z,
                3, radius * 0.2, 0.05, radius * 0.2, 0.0);
    }

    private static void emitSigilEruption(ServerLevel level, Vec3 center) {
        double radius = BossRefactorAetherConfig.SUN_SPIRIT_RANGE
            .flameSigilRadius.get();
        level.sendParticles(ParticleTypes.FLAME,
                center.x, center.y + 0.5, center.z,
            120, radius * 0.7, 2.2, radius * 0.7, 0.1);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            center.x, center.y + 0.35, center.z,
            40, radius * 0.6, 1.6, radius * 0.6, 0.06);
        level.sendParticles(ParticleTypes.LAVA,
                center.x, center.y + 0.25, center.z,
            28, radius * 0.55, 0.7, radius * 0.55, 0.03);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.8F, 0.7F);
    }
}