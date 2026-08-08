package com.tonywww.bossrefactoraether.valkyriequeen;

import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import com.aetherteam.aether.entity.projectile.crystal.ThunderCrystal;
import com.aetherteam.aether.item.AetherItems;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.mixin.AbstractValkyrieTeleportAccessor;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class ValkyrieQueenCombatService {
    private static final UUID PHASE_TWO_KNOCKBACK_MODIFIER_ID = UUID.fromString(
        "ca59ba33-5810-4a15-8565-cb95cb5610e4");
    private static final String SPEAR_ENTITY_TAG =
        BossRefactorAether.MOD_ID + ".valkyrie_queen_spear";
    private static final double DEFAULT_ATTACK_DAMAGE = 10.0;

    private enum ApproachResult {
        MOVING,
        POSITIONED,
        TIMED_OUT
    }

    private ValkyrieQueenCombatService() {
    }

    public static ValkyrieQueenCombatState state(ValkyrieQueen queen) {
        return ((ValkyrieQueenStateAccess) queen)
                .bossRefactorAether$getValkyrieQueenCombatState();
    }

    public static void tick(ValkyrieQueen queen) {
        if (queen.level().isClientSide()) {
            return;
        }
        ValkyrieQueenCombatState state = state(queen);
        tickSwordWaves(queen, state);
        tickThunderCloud(queen, state);

        if (!state.phaseTwo && queen.getHealth() > 0.0F
            && queen.getHealth() < queen.getMaxHealth()
            * BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                .phaseTwoHealthRatio.get()) {
            enterPhaseTwo(queen, state);
        }
        if (!queen.isReady() || !queen.isBossFight() || queen.isDeadOrDying()) {
            cancelActiveAttack(queen, state);
            return;
        }

        long gameTime = queen.level().getGameTime();
        if (state.skillReadyAt == 0L) {
            state.skillReadyAt = gameTime + BossRefactorAetherConfig
                    .VALKYRIE_QUEEN_COMBAT.initialSkillDelayTicks.get();
        }
        if (state.spearReadyAt == 0L) {
            state.spearReadyAt = gameTime + BossRefactorAetherConfig
                    .VALKYRIE_QUEEN_COMBAT.initialSpearDelayTicks.get();
        }

        LivingEntity target = validTarget(queen);
        if (target == null) {
            queen.setAggressive(false);
            cancelActiveAttack(queen, state);
            return;
        }
        queen.setAggressive(true);
        faceTarget(queen, target);

        switch (state.attackPhase) {
            case IDLE -> tickIdle(queen, target, state, gameTime);
            case BASIC_WINDUP -> tickBasicWindup(queen, target, state);
            case BASIC_LANCE_SPIN -> tickBasicLanceSpin(queen, state);
            case SKILL_ONE_CHARGE -> tickSkillOneCharge(queen, target, state);
            case SKILL_ONE_FIRE -> tickSkillOneFire(queen, target, state);
            case SKILL_TWO_CHARGE -> tickSkillTwoCharge(queen, target, state);
            case SKILL_TWO_DASH -> tickSkillTwoDash(queen, state);
            case SKILL_TWO_SPIN -> tickSkillTwoSpin(queen, state);
            case SPEAR_CHARGE -> tickSpearCharge(queen, target, state);
            case SPEAR_FLIGHT -> tickSpearFlight(queen, target, state);
            case SPEAR_RETRIEVE -> tickSpearRetrieve(queen, state);
            case RECOVERY -> tickRecovery(queen, state);
        }
    }

    public static void reset(ValkyrieQueen queen) {
        ValkyrieQueenCombatState state = state(queen);
        if (queen.level().isClientSide()) {
            state.resetTransient();
            return;
        }
        clearTelegraph(queen);
        closeParryWindow(queen, state);
        cleanupSpearEntities(queen);
        removePhaseTwoModifier(queen);
        clearLance(queen);
        state.phaseTwo = false;
        state.teleportReadyAt = 0L;
        state.skillReadyAt = 0L;
        state.spearReadyAt = 0L;
        state.basicIndex = 0;
        state.skillIndex = 0;
        state.resetTransient();
    }

    public static void onLoaded(ValkyrieQueen queen) {
        ValkyrieQueenCombatState state = state(queen);
        state.resetTransient();
        if (queen.level().isClientSide()) {
            return;
        }
        clearTelegraph(queen);
        cleanupSpearEntities(queen);
        if (state.phaseTwo) {
            applyPhaseTwoLoadout(queen);
        } else {
            removePhaseTwoModifier(queen);
            clearLance(queen);
        }
    }

    public static boolean isCurrentAttackParryable(ValkyrieQueen queen) {
        return switch (state(queen).attackPhase) {
            case SKILL_ONE_CHARGE, SKILL_ONE_FIRE,
                    SKILL_TWO_CHARGE, SKILL_TWO_DASH -> true;
            default -> false;
        };
    }

    public static void acceptParry(ValkyrieQueen queen) {
        ValkyrieQueenCombatState state = state(queen);
        state.swordWaves.clear();
        closeParryWindow(queen, state);
        enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .parryRecoveryTicks.get());
    }

    public static boolean onDamaged(ValkyrieQueen queen, @Nullable LivingEntity attacker) {
        if (queen.level().isClientSide() || !queen.isAlive()
                || !queen.isReady() || !queen.isBossFight()) {
            return false;
        }
        ValkyrieQueenCombatState state = state(queen);
        long gameTime = queen.level().getGameTime();
        if (gameTime < state.teleportReadyAt
            || queen.getRandom().nextDouble() >= BossRefactorAetherConfig
                .VALKYRIE_QUEEN_COMBAT.reactiveTeleportChance.get()) {
            return false;
        }
        LivingEntity teleportTarget = attacker != null ? attacker : validTarget(queen);
        if (teleportTarget == null) {
            return false;
        }

        Vec3 origin = queen.position();
        if (((AbstractValkyrieTeleportAccessor) queen)
                .bossRefactorAether$teleportAroundTarget(teleportTarget)) {
            state.teleportReadyAt = gameTime
                    + BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                        .reactiveTeleportCooldownTicks.get();
            boolean crystalSpawned = spawnThunderCrystal(queen, teleportTarget, origin);
            if (!crystalSpawned) {
                BossRefactorAether.LOGGER.warn(
                        "Valkyrie Queen teleported but the Thunder Crystal was rejected at {}",
                        origin);
            }
            state.swordWaves.clear();
            closeParryWindow(queen, state);
            enterRecovery(queen, state, 10);
            return crystalSpawned;
        }
        return false;
    }

    private static void tickIdle(ValkyrieQueen queen, LivingEntity target,
                                 ValkyrieQueenCombatState state, long gameTime) {
        if (state.recoveryTicks > 0) {
            state.recoveryTicks--;
            if (approachTarget(queen, target, state) == ApproachResult.TIMED_OUT) {
                startNextSkill(queen, state, gameTime);
            }
            return;
        }

        double distance = horizontalDistance(queen.position(), target.position());
        double maximumBasicRange = maximumBasicRange();
        if (distance > maximumBasicRange) {
            state.flankReady = false;
        }
        boolean spearReady = state.phaseTwo
            && state.basicsSinceSkill >= BossRefactorAetherConfig
                .VALKYRIE_QUEEN_COMBAT.basicsBeforeSpear.get()
            && gameTime >= state.spearReadyAt
            && distance <= BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .spearTriggerDistance.get();
        boolean skillReady = state.basicsSinceSkill >= BossRefactorAetherConfig
                .VALKYRIE_QUEEN_COMBAT.basicsBeforeSkill.get()
            && gameTime >= state.skillReadyAt
            && distance <= BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .skillTriggerDistance.get();
        boolean shouldFlank = BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                .tacticalFlankingEnabled.get()
            && !state.flankReady
            && distance > BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                .flankBypassDistance.get();
        if (shouldFlank || (!spearReady && !skillReady
                && distance > maximumBasicRange)) {
            ApproachResult approachResult = approachTarget(queen, target, state);
            if (approachResult == ApproachResult.TIMED_OUT) {
                startNextSkill(queen, state, gameTime);
                return;
            }
            if (approachResult == ApproachResult.MOVING) {
                return;
            }
        }

        distance = horizontalDistance(queen.position(), target.position());
        if (spearReady) {
            startSpearThrow(queen, state, gameTime);
            return;
        }
        if (skillReady) {
            startNextSkill(queen, state, gameTime);
            return;
        }
        if (distance <= maximumBasicRange) {
            startBasicAttack(queen, state);
        } else {
            if (approachTarget(queen, target, state) == ApproachResult.TIMED_OUT) {
                startNextSkill(queen, state, gameTime);
            }
        }
    }

    private static void startNextSkill(ValkyrieQueen queen,
                                       ValkyrieQueenCombatState state,
                                       long gameTime) {
        state.skillReadyAt = gameTime + BossRefactorAetherConfig
            .VALKYRIE_QUEEN_COMBAT.skillCooldownTicks.get();
        state.basicsSinceSkill = 0;
        resetApproach(state);
        if (ValkyrieQueenMechanics.shouldUseSkillOne(state.skillIndex++)) {
            startSkillOne(queen, state);
        } else {
            startSkillTwo(queen, state);
        }
    }

    private static void startBasicAttack(ValkyrieQueen queen,
                                         ValkyrieQueenCombatState state) {
        state.basicAttack = ValkyrieQueenMechanics.basicAttackForIndex(state.basicIndex++);
        state.attackPhase = ValkyrieQueenAttackPhase.BASIC_WINDUP;
        state.phaseTicks = 0;
        stopMovement(queen);
        setTelegraph(queen, AttackTelegraphShape.ARC, horizontalLook(queen),
            basicRange(state.basicAttack),
            basicHalfAngle(state.basicAttack), 0.0);
    }

    private static void tickBasicWindup(ValkyrieQueen queen, LivingEntity target,
                                        ValkyrieQueenCombatState state) {
        stopMovement(queen);
        faceTarget(queen, target);
        Vec3 attackDirection = horizontalDirection(
            queen.position(), target.position(), queen);
        setTelegraph(queen, AttackTelegraphShape.ARC,
            attackDirection,
            basicRange(state.basicAttack), basicHalfAngle(state.basicAttack), 0.0,
            AttackTelegraph.windupProgress(
                state.phaseTicks,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .basicWindupTicks.get()));
        state.phaseTicks++;
        emitBasicTelegraph(queen, state.basicAttack);
        if (state.phaseTicks < BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .basicWindupTicks.get()) {
            return;
        }

        queen.swing(InteractionHand.MAIN_HAND);
        clearTelegraph(queen);
        damageArc(
                queen,
                state.basicAttack,
            attackDirection,
            basicDamageFormula(state.basicAttack));
        emitBasicSlash(queen, state.basicAttack);
        state.basicsSinceSkill++;
        if (state.phaseTwo) {
            state.attackPhase = ValkyrieQueenAttackPhase.BASIC_LANCE_SPIN;
            state.phaseTicks = 0;
                setTelegraph(queen, AttackTelegraphShape.CIRCLE, horizontalLook(queen),
                    0.0, 0.0,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                        .basicLanceSpinRadius.get());
        } else {
                enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .basicRecoveryTicks.get());
        }
    }

    private static void tickBasicLanceSpin(ValkyrieQueen queen,
                                           ValkyrieQueenCombatState state) {
        stopMovement(queen);
        state.phaseTicks++;
        double spinRadius = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .basicLanceSpinRadius.get();
        setTelegraph(queen, AttackTelegraphShape.CIRCLE, horizontalLook(queen),
            0.0, 0.0, spinRadius,
            AttackTelegraph.windupProgress(
                state.phaseTicks,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spinWindupTicks.get()));
        emitSpinTelegraph(queen, spinRadius);
        if (state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .spinWindupTicks.get()) {
            queen.swing(InteractionHand.OFF_HAND);
            damageRadius(
                    queen,
                    queen.position(),
                    spinRadius,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.basicLanceSpin,
                    true);
            emitSpinAttack(queen, spinRadius);
                enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .basicRecoveryTicks.get());
        }
    }

    private static void startSkillOne(ValkyrieQueen queen,
                                      ValkyrieQueenCombatState state) {
        state.attackPhase = ValkyrieQueenAttackPhase.SKILL_ONE_CHARGE;
        state.phaseTicks = 0;
        stopMovement(queen);
        openParryWindow(queen, state);
        setTelegraph(queen, AttackTelegraphShape.CORRIDOR, horizontalLook(queen),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.swordWaveDistance.get(),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.swordWaveHitRadius.get(),
            0.0);
    }

    private static void tickSkillOneCharge(ValkyrieQueen queen, LivingEntity target,
                                           ValkyrieQueenCombatState state) {
        stopMovement(queen);
        faceTarget(queen, target);
        setTelegraph(queen, AttackTelegraphShape.CORRIDOR,
            horizontalDirection(queen.position(), target.position(), queen),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.swordWaveDistance.get(),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.swordWaveHitRadius.get(),
            0.0,
            AttackTelegraph.windupProgress(
                state.phaseTicks,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .skillOneWindupTicks.get()));
        state.phaseTicks++;
        emitChargeParticles(queen, false);
        if (state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .skillOneWindupTicks.get()) {
            state.attackPhase = ValkyrieQueenAttackPhase.SKILL_ONE_FIRE;
            state.phaseTicks = 0;
            spawnSwordWave(queen, target);
            clearTelegraph(queen);
        }
    }

    private static void tickSkillOneFire(ValkyrieQueen queen, LivingEntity target,
                                         ValkyrieQueenCombatState state) {
        stopMovement(queen);
        faceTarget(queen, target);
        state.phaseTicks++;
        if (state.phaseTicks == BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .swordWaveGapTicks.get()) {
            spawnSwordWave(queen, target);
        }
        if (state.phaseTicks < BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .skillOneFireTicks.get()) {
            return;
        }

        if (queen.getRandom().nextDouble()
                < BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                    .skillOneChainChance.get()) {
            startSkillTwo(queen, state);
        } else {
            closeParryWindow(queen, state);
                enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .skillRecoveryTicks.get());
        }
    }

    private static void startSkillTwo(ValkyrieQueen queen,
                                      ValkyrieQueenCombatState state) {
        state.attackPhase = ValkyrieQueenAttackPhase.SKILL_TWO_CHARGE;
        state.phaseTicks = 0;
        stopMovement(queen);
        openParryWindow(queen, state);
        setTelegraph(queen, AttackTelegraphShape.CORRIDOR, horizontalLook(queen),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.dashDistance.get(),
            queen.getBbWidth() * 0.7
                + BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .dashHitInflation.get(),
            0.0);
    }

    private static void tickSkillTwoCharge(ValkyrieQueen queen, LivingEntity target,
                                           ValkyrieQueenCombatState state) {
        stopMovement(queen);
        faceTarget(queen, target);
        setTelegraph(queen, AttackTelegraphShape.CORRIDOR,
            horizontalDirection(queen.position(), target.position(), queen),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.dashDistance.get(),
            queen.getBbWidth() * 0.7
                + BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .dashHitInflation.get(),
            0.0,
            AttackTelegraph.windupProgress(
                    state.phaseTicks,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                            .skillTwoWindupTicks.get()));
        state.phaseTicks++;
        emitChargeParticles(queen, true);
        if (state.phaseTicks < BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .skillTwoWindupTicks.get()) {
            return;
        }

        state.attackPhase = ValkyrieQueenAttackPhase.SKILL_TWO_DASH;
        state.phaseTicks = 0;
        state.dashStart = queen.position();
        state.dashDirection = horizontalDirection(queen.position(), target.position(), queen);
        state.dashHits.clear();
        clearTelegraph(queen);
        queen.level().playSound(null, queen.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3,
                SoundSource.HOSTILE, 1.5F, 1.0F);
    }

    private static void tickSkillTwoDash(ValkyrieQueen queen,
                                         ValkyrieQueenCombatState state) {
        Vec3 movement = state.dashDirection.scale(
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.dashSpeed.get());
        AABB sweptBounds = expandDownward(
            queen.getBoundingBox().expandTowards(movement).inflate(
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .dashHitInflation.get()),
            attackDownwardRange());
        for (Player player : eligiblePlayers(queen, sweptBounds)) {
            if (state.dashHits.add(player.getUUID())) {
                dealAttackDamage(
                        queen,
                        player,
                        BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.dash,
                        queen.position(),
                        true);
            }
        }
        queen.setDeltaMovement(movement);
        emitDashParticles(queen);
        state.phaseTicks++;
        double traveled = queen.position().distanceTo(state.dashStart);
        if (queen.horizontalCollision
                || state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .dashTickLimit.get()
                || traveled >= BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .dashDistance.get()) {
            closeParryWindow(queen, state);
            state.attackPhase = ValkyrieQueenAttackPhase.SKILL_TWO_SPIN;
            state.phaseTicks = 0;
            stopMovement(queen);
            equipLance(queen);
                setTelegraph(queen, AttackTelegraphShape.CIRCLE, horizontalLook(queen),
                    0.0, 0.0,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                        .skillTwoSpinRadius.get());
        }
    }

    private static void tickSkillTwoSpin(ValkyrieQueen queen,
                                         ValkyrieQueenCombatState state) {
        stopMovement(queen);
        state.phaseTicks++;
        double spinRadius = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .skillTwoSpinRadius.get();
        setTelegraph(queen, AttackTelegraphShape.CIRCLE, horizontalLook(queen),
            0.0, 0.0, spinRadius,
            AttackTelegraph.windupProgress(
                state.phaseTicks,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spinWindupTicks.get()));
        emitSpinTelegraph(queen, spinRadius);
        if (state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .spinWindupTicks.get()) {
            queen.swing(InteractionHand.OFF_HAND);
            damageRadius(
                    queen,
                    queen.position(),
                    spinRadius,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.skillTwoSpin,
                    true);
            emitSpinAttack(queen, spinRadius);
            if (!state.phaseTwo) {
                clearLance(queen);
            }
                enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .skillRecoveryTicks.get());
        }
    }

    private static void startSpearThrow(ValkyrieQueen queen,
                                        ValkyrieQueenCombatState state,
                                        long gameTime) {
        state.attackPhase = ValkyrieQueenAttackPhase.SPEAR_CHARGE;
        state.phaseTicks = 0;
        state.basicsSinceSkill = 0;
        state.spearReadyAt = gameTime + BossRefactorAetherConfig
            .VALKYRIE_QUEEN_COMBAT.spearCooldownTicks.get();
        stopMovement(queen);
        LivingEntity target = validTarget(queen);
        if (target != null) {
            double corridorLength = Math.min(
                horizontalDistance(queen.position(), target.position()),
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .spearSpeed.get()
                    * BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                        .spearFlightTicks.get());
            setTelegraph(
                queen,
                AttackTelegraphShape.CORRIDOR_WITH_END_CIRCLE,
                horizontalDirection(queen.position(), target.position(), queen),
                corridorLength,
                0.65,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .spearImpactRadius.get());
        }
    }

    private static void tickSpearCharge(ValkyrieQueen queen, LivingEntity target,
                                        ValkyrieQueenCombatState state) {
        stopMovement(queen);
        faceTarget(queen, target);
        double corridorLength = Math.min(
            horizontalDistance(queen.position(), target.position()),
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.spearSpeed.get()
                * BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spearFlightTicks.get());
        setTelegraph(queen, AttackTelegraphShape.CORRIDOR_WITH_END_CIRCLE,
            horizontalDirection(queen.position(), target.position(), queen),
            corridorLength, 0.65,
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.spearImpactRadius.get(),
            AttackTelegraph.windupProgress(
                state.phaseTicks,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spearWindupTicks.get()));
        state.phaseTicks++;
        emitSpearChargeParticles(queen);
        if (state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .spearWindupTicks.get()) {
            launchSpear(queen, target, state);
        }
    }

    private static void launchSpear(ValkyrieQueen queen, LivingEntity target,
                                    ValkyrieQueenCombatState state) {
        ServerLevel level = (ServerLevel) queen.level();
        clearLance(queen);
        state.spearPosition = queen.getEyePosition().add(0.0, -0.3, 0.0);
        state.spearDirection = horizontalDirection(
                state.spearPosition, target.getEyePosition(), queen);
        state.spearDistance = 0.0;
        state.phaseTicks = 0;

        ItemEntity spear = new ItemEntity(
                level,
                state.spearPosition.x,
                state.spearPosition.y,
                state.spearPosition.z,
                new ItemStack(AetherItems.VALKYRIE_LANCE.get()));
        spear.setNoGravity(true);
        spear.setNeverPickUp();
        spear.setInvulnerable(true);
        spear.addTag(SPEAR_ENTITY_TAG);
        spear.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(spear);
        state.spearEntityId = spear.getUUID();
        state.attackPhase = ValkyrieQueenAttackPhase.SPEAR_FLIGHT;
        clearTelegraph(queen);
        level.playSound(null, queen.blockPosition(), SoundEvents.TRIDENT_THROW,
                SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    private static void tickSpearFlight(ValkyrieQueen queen, LivingEntity target,
                                        ValkyrieQueenCombatState state) {
        ServerLevel level = (ServerLevel) queen.level();
        Vec3 previous = state.spearPosition;
        Vec3 next = previous.add(
            state.spearDirection.scale(
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.spearSpeed.get()));
        BlockHitResult blockHit = level.clip(new ClipContext(
                previous,
                next,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                queen));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        if (hitBlock) {
            next = blockHit.getLocation();
        }
        state.spearPosition = next;
        state.spearDistance += previous.distanceTo(next);
        state.phaseTicks++;

        ItemEntity spear = findSpear(level, state.spearEntityId);
        if (spear != null) {
            spear.setPos(next.x, next.y, next.z);
            spear.setDeltaMovement(Vec3.ZERO);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                next.x, next.y, next.z, 5, 0.15, 0.15, 0.15, 0.02);

        boolean reachedTargetColumn = horizontalDistance(next, target.position()) <= 1.5
            && isWithinAttackHeight(next.y, target, 1.5);
        if (hitBlock || reachedTargetColumn
                || state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spearFlightTicks.get()) {
            impactSpear(queen, state);
        }
    }

    private static void impactSpear(ValkyrieQueen queen,
                                    ValkyrieQueenCombatState state) {
        damageRadius(
                queen,
                state.spearPosition,
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.spearImpactRadius.get(),
                BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.spearThrow,
                true);
        state.thunderCloudPosition = state.spearPosition;
        state.thunderCloudTicks = BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .thunderCloudTicks.get();
        state.attackPhase = ValkyrieQueenAttackPhase.SPEAR_RETRIEVE;
        state.phaseTicks = 0;
        queen.level().playSound(null, queen.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.HOSTILE, 1.2F, 1.1F);
        spawnVisualLightning((ServerLevel) queen.level(), state.spearPosition);
    }

    private static void tickSpearRetrieve(ValkyrieQueen queen,
                                          ValkyrieQueenCombatState state) {
        Vec3 offset = state.spearPosition.subtract(queen.position());
        double distance = offset.length();
        state.phaseTicks++;
        if (distance <= 1.5) {
            finishSpearRetrieve(queen, state);
            return;
        }
        queen.setDeltaMovement(offset.normalize().scale(
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.spearRetrieveSpeed.get()));
        emitDashParticles(queen);
        if (queen.horizontalCollision
                || state.phaseTicks >= BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                    .spearRetrieveTickLimit.get()) {
            queen.teleportTo(
                    state.spearPosition.x,
                    state.spearPosition.y,
                    state.spearPosition.z);
            finishSpearRetrieve(queen, state);
        }
    }

    private static void finishSpearRetrieve(ValkyrieQueen queen,
                                            ValkyrieQueenCombatState state) {
        ItemEntity spear = findSpear((ServerLevel) queen.level(), state.spearEntityId);
        if (spear != null) {
            spear.discard();
        }
        state.spearEntityId = null;
        equipLance(queen);
        enterRecovery(queen, state, BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
            .skillRecoveryTicks.get());
    }

    private static void tickRecovery(ValkyrieQueen queen,
                                     ValkyrieQueenCombatState state) {
        stopMovement(queen);
        state.phaseTicks++;
        if (state.phaseTicks >= state.recoveryTicks) {
            state.attackPhase = ValkyrieQueenAttackPhase.IDLE;
            state.phaseTicks = 0;
            state.recoveryTicks = 0;
        }
    }

    private static void enterRecovery(ValkyrieQueen queen,
                                      ValkyrieQueenCombatState state,
                                      int ticks) {
        state.attackPhase = ValkyrieQueenAttackPhase.RECOVERY;
        state.phaseTicks = 0;
        state.recoveryTicks = ticks;
        state.dashHits.clear();
        resetApproach(state);
        clearTelegraph(queen);
        stopMovement(queen);
    }

    private static void tickSwordWaves(ValkyrieQueen queen,
                                       ValkyrieQueenCombatState state) {
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }
        Iterator<ValkyrieQueenSwordWave> iterator = state.swordWaves.iterator();
        while (iterator.hasNext()) {
            ValkyrieQueenSwordWave wave = iterator.next();
                Vec3 movement = wave.direction.scale(
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.swordWaveSpeed.get());
            wave.position = wave.position.add(movement);
            wave.distance += movement.length();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    wave.position.x, wave.position.y, wave.position.z,
                    2, 0.55, 0.45, 0.55, 0.0);
            level.sendParticles(ParticleTypes.END_ROD,
                    wave.position.x, wave.position.y, wave.position.z,
                    3, 0.3, 0.3, 0.3, 0.01);

            AABB bounds = attackBounds(
                    wave.position,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                        .swordWaveHitRadius.get(),
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                        .swordWaveHitHalfHeight.get());
            for (Player player : eligiblePlayers(queen, bounds)) {
                if (wave.hits.add(player.getUUID())) {
                    dealAttackDamage(
                            queen,
                            player,
                            BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.swordWave,
                            wave.position,
                            true);
                }
            }
                if (wave.distance >= BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                    .swordWaveDistance.get()) {
                iterator.remove();
            }
        }
    }

    private static void spawnSwordWave(ValkyrieQueen queen, LivingEntity target) {
        ValkyrieQueenCombatState state = state(queen);
        Vec3 direction = horizontalDirection(queen.position(), target.position(), queen);
        Vec3 origin = queen.position().add(direction.scale(0.9)).add(0.0, 1.0, 0.0);
        state.swordWaves.add(new ValkyrieQueenSwordWave(origin, direction));
        queen.swing(InteractionHand.MAIN_HAND);
        queen.level().playSound(null, queen.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.5F, 0.65F);
    }

    private static void tickThunderCloud(ValkyrieQueen queen,
                                         ValkyrieQueenCombatState state) {
        if (state.thunderCloudTicks <= 0
                || !(queen.level() instanceof ServerLevel level)) {
            return;
        }
        state.thunderCloudTicks--;
        Vec3 center = state.thunderCloudPosition;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y + 0.5, center.z,
                6, 2.2, 0.6, 2.2, 0.02);
        level.sendParticles(ParticleTypes.CLOUD,
                center.x, center.y + 1.0, center.z,
                3, 2.0, 0.3, 2.0, 0.01);
        if (state.thunderCloudTicks
            % BossRefactorAetherConfig.VALKYRIE_QUEEN_TIMING
                .thunderCloudDamageInterval.get() != 0) {
            return;
        }
        double cloudRadius = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .thunderCloudRadius.get();
        double upwardRange = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .thunderCloudHalfHeight.get();
        AABB bounds = attackBounds(center, cloudRadius, upwardRange);
        for (Player player : eligiblePlayers(queen, bounds)) {
            if (ValkyrieQueenMechanics.isWithinHorizontalRadius(
                        center, player.position(), cloudRadius)
                    && isWithinAttackHeight(center.y, player, upwardRange)) {
                dealLightningDamage(
                    queen,
                    player,
                    center,
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.thunderCloud);
            }
        }
    }

    private static void damageArc(ValkyrieQueen queen,
                                  ValkyrieQueenBasicAttack attack,
                                  Vec3 forward,
                                  BossRefactorAetherConfig.DamageFormula formula) {
        double radius = basicRange(attack);
        double upwardRange = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .meleeVerticalTolerance.get();
        AABB bounds = attackBounds(queen.position(), radius, upwardRange);
        for (Player player : eligiblePlayers(queen, bounds)) {
                if (isWithinAttackHeight(queen.getY(), player, upwardRange)
                    && queen.hasLineOfSight(player)
                    && ValkyrieQueenMechanics.isInHorizontalArc(
                            queen.position(),
                            forward,
                            player.position(),
                            radius,
                            basicHalfAngle(attack))) {
                dealAttackDamage(queen, player, formula, queen.position(), true);
            }
        }
    }

    private static void damageRadius(ValkyrieQueen queen, Vec3 center,
                                     double radius,
                                     BossRefactorAetherConfig.DamageFormula formula,
                                     boolean lightningBonus) {
        double upwardRange = BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .meleeVerticalTolerance.get();
        AABB bounds = attackBounds(center, radius, upwardRange);
        for (Player player : eligiblePlayers(queen, bounds)) {
            if (ValkyrieQueenMechanics.isWithinHorizontalRadius(
                        center, player.position(), radius)
                    && isWithinAttackHeight(center.y, player, upwardRange)) {
                dealAttackDamage(queen, player, formula, center, lightningBonus);
            }
        }
    }

    private static boolean dealAttackDamage(ValkyrieQueen queen, Player player,
                                            BossRefactorAetherConfig.DamageFormula formula,
                                            Vec3 sourcePosition,
                                            boolean lightningBonus) {
        DamageSource source = sourceAt(queen, sourcePosition);
        boolean hurt = player.hurt(source, configuredDamage(queen, formula));
        if (hurt && state(queen).phaseTwo && lightningBonus) {
            dealLightningDamage(
                    queen,
                    player,
                    player.position(),
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE.lightningBonus);
        }
        return hurt;
    }

    private static void dealLightningDamage(ValkyrieQueen queen, Player player,
                                            Vec3 sourcePosition,
                                            BossRefactorAetherConfig.DamageFormula formula) {
        ServerLevel level = (ServerLevel) queen.level();
        DamageSource source = new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ValkyrieQueenDamageTypes.LIGHTNING),
                queen,
                queen,
                sourcePosition);
        player.hurt(source, configuredDamage(queen, formula));
        spawnVisualLightning(level, player.position());
    }

    private static DamageSource sourceAt(ValkyrieQueen queen, Vec3 sourcePosition) {
        DamageSource base = queen.damageSources().mobAttack(queen);
        return new DamageSource(
                base.typeHolder(), queen, queen, sourcePosition);
    }

    private static double attackDamage(ValkyrieQueen queen) {
        AttributeInstance attackDamage = queen.getAttribute(Attributes.ATTACK_DAMAGE);
        return attackDamage != null ? attackDamage.getValue() : DEFAULT_ATTACK_DAMAGE;
    }

    private static float configuredDamage(
            ValkyrieQueen queen, BossRefactorAetherConfig.DamageFormula formula) {
        return (float) (formula.baseDamage.get()
                + attackDamage(queen) * formula.attackDamageMultiplier.get());
    }

    private static void enterPhaseTwo(ValkyrieQueen queen,
                                      ValkyrieQueenCombatState state) {
        state.phaseTwo = true;
        applyPhaseTwoLoadout(queen);
        if (queen.level() instanceof ServerLevel level) {
            spawnVisualLightning(level, queen.position());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    queen.getX(), queen.getY() + 1.0, queen.getZ(),
                    40, 1.2, 1.5, 1.2, 0.08);
            level.playSound(null, queen.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.HOSTILE, 2.0F, 0.8F);
        }
    }

    private static void applyPhaseTwoLoadout(ValkyrieQueen queen) {
        AttributeInstance resistance = queen.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null
                && resistance.getModifier(PHASE_TWO_KNOCKBACK_MODIFIER_ID) == null) {
            resistance.addTransientModifier(new AttributeModifier(
                    PHASE_TWO_KNOCKBACK_MODIFIER_ID,
                    "Valkyrie Queen phase two knockback immunity",
                    BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
                        .phaseTwoKnockbackResistance.get(),
                    AttributeModifier.Operation.ADDITION));
        }
        equipLance(queen);
    }

    private static void removePhaseTwoModifier(ValkyrieQueen queen) {
        AttributeInstance resistance = queen.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) {
            resistance.removeModifier(PHASE_TWO_KNOCKBACK_MODIFIER_ID);
        }
    }

    private static void equipLance(ValkyrieQueen queen) {
        queen.setItemSlot(
                EquipmentSlot.OFFHAND,
                new ItemStack(AetherItems.VALKYRIE_LANCE.get()));
        queen.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private static void clearLance(ValkyrieQueen queen) {
        if (queen.getOffhandItem().is(AetherItems.VALKYRIE_LANCE.get())) {
            queen.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void openParryWindow(ValkyrieQueen queen,
                                        ValkyrieQueenCombatState state) {
        if (!state.parryWindowOpen
                && ValkyrieQueenParryIntegration.bridge().openWindow(queen)) {
            state.parryWindowOpen = true;
        }
    }

    private static void closeParryWindow(ValkyrieQueen queen,
                                         ValkyrieQueenCombatState state) {
        if (state.parryWindowOpen) {
            ValkyrieQueenParryIntegration.bridge().closeWindow(queen);
            state.parryWindowOpen = false;
        }
    }

    private static void cancelActiveAttack(ValkyrieQueen queen,
                                           ValkyrieQueenCombatState state) {
        clearTelegraph(queen);
        queen.setAggressive(false);
        resetApproach(state);
        if (state.attackPhase == ValkyrieQueenAttackPhase.IDLE
                && state.swordWaves.isEmpty() && state.spearEntityId == null) {
            stopMovement(queen);
            return;
        }
        closeParryWindow(queen, state);
        state.attackPhase = ValkyrieQueenAttackPhase.IDLE;
        state.phaseTicks = 0;
        state.recoveryTicks = 0;
        state.swordWaves.clear();
        state.dashHits.clear();
        cleanupSpearEntities(queen);
        state.spearEntityId = null;
        if (state.phaseTwo) {
            equipLance(queen);
        }
        stopMovement(queen);
    }

    @Nullable
    private static LivingEntity validTarget(ValkyrieQueen queen) {
        LivingEntity target = queen.getTarget();
        if (isEligibleTarget(target)) {
            return target;
        }

        double pursuitRange = BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT
            .pursuitRange.get();
        Player nearestPlayer = null;
        double nearestDistance = pursuitRange * pursuitRange;
        for (Player player : eligiblePlayers(
                queen, queen.getBoundingBox().inflate(pursuitRange))) {
            double distance = queen.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestPlayer = player;
                nearestDistance = distance;
            }
        }
        queen.setTarget(nearestPlayer);
        return nearestPlayer;
    }

    private static boolean isEligibleTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && !player.isSpectator());
    }

    private static List<Player> eligiblePlayers(ValkyrieQueen queen, AABB bounds) {
        return queen.level().getEntitiesOfClass(Player.class, bounds,
                player -> player.isAlive() && !player.isCreative()
                        && !player.isSpectator());
    }

    private static ApproachResult approachTarget(ValkyrieQueen queen,
                                                 LivingEntity target,
                                                 ValkyrieQueenCombatState state) {
        BossRefactorAetherConfig.ValkyrieQueenCombatConfig config =
            BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT;
        if (!config.tacticalFlankingEnabled.get()) {
            resetApproach(state);
            queen.getNavigation().moveTo(target, config.approachSpeed.get());
            jumpWhileChasing(queen, target);
            return horizontalDistance(queen.position(), target.position())
                    <= maximumBasicRange()
                ? ApproachResult.POSITIONED
                : ApproachResult.MOVING;
        }

        if (state.approachPosition == null) {
            state.approachPosition = ValkyrieQueenMechanics.approachPositionForRoll(
                    queen.getRandom().nextDouble(), config.sideApproachWeight.get());
            state.flankMovementTicks = 0;
            state.flankRepathTicks = 0;
        }
        if (state.flankRepathTicks <= 0 || queen.getNavigation().isDone()) {
            state.flankPosition = ValkyrieQueenMechanics.flankPosition(
                    queen.position(), target.position(), target.getLookAngle(),
                    config.flankDistance.get(), state.approachPosition);
            boolean pathFound = queen.getNavigation().moveTo(
                    state.flankPosition.x, state.flankPosition.y,
                    state.flankPosition.z, config.flankSpeed.get());
            if (!pathFound) {
                state.flankPosition = target.position();
                queen.getNavigation().moveTo(target, config.approachSpeed.get());
            }
            state.flankRepathTicks = config.flankRepathTicks.get();
        } else {
            state.flankRepathTicks--;
        }
        jumpWhileChasing(queen, target);

        if (!ValkyrieQueenMechanics.hasReachedFlank(
                queen.position(), state.flankPosition,
                config.flankArrivalDistance.get())) {
            state.flankMovementTicks++;
            if (ValkyrieQueenMechanics.hasMovementTimedOut(
                    state.flankMovementTicks,
                    config.tacticalMovementTimeoutTicks.get())) {
                queen.getNavigation().stop();
                return ApproachResult.TIMED_OUT;
            }
            return ApproachResult.MOVING;
        }
        state.flankReady = true;
        queen.getNavigation().stop();
        return ApproachResult.POSITIONED;
    }

    private static void resetApproach(ValkyrieQueenCombatState state) {
        state.approachPosition = null;
        state.flankRepathTicks = 0;
        state.flankMovementTicks = 0;
        state.flankReady = false;
        state.flankPosition = Vec3.ZERO;
    }

    private static void jumpWhileChasing(ValkyrieQueen queen, LivingEntity target) {
        BossRefactorAetherConfig.ValkyrieQueenCombatConfig config =
            BossRefactorAetherConfig.VALKYRIE_QUEEN_COMBAT;
        if (!config.chaseJumpEnabled.get() || !queen.onGround()) {
            return;
        }
        double deltaX = target.getX() - queen.getX();
        double deltaZ = target.getZ() - queen.getZ();
        double triggerDistance = config.chaseJumpTriggerDistance.get();
        boolean targetIsHigher = target.getY() - queen.getY()
                >= config.chaseJumpHeightThreshold.get()
            && deltaX * deltaX + deltaZ * deltaZ
                <= triggerDistance * triggerDistance;
        if (queen.horizontalCollision || targetIsHigher) {
            queen.getJumpControl().jump();
        }
    }

    private static void faceTarget(ValkyrieQueen queen, LivingEntity target) {
        queen.getLookControl().setLookAt(target, 40.0F, 40.0F);
    }

    private static void stopMovement(ValkyrieQueen queen) {
        queen.getNavigation().stop();
        queen.setDeltaMovement(Vec3.ZERO);
    }

    private static Vec3 horizontalLook(ValkyrieQueen queen) {
        Vec3 look = queen.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0E-8) {
            double radians = Math.toRadians(queen.getYRot());
            return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
        }
        return horizontal.normalize();
    }

        private static double maximumBasicRange() {
        return Math.max(
            BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.diagonalRange.get(),
            Math.max(
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.horizontalRange.get(),
                BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE.verticalRange.get()));
        }

        private static double basicRange(ValkyrieQueenBasicAttack attack) {
        return switch (attack) {
            case DIAGONAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .diagonalRange.get();
            case HORIZONTAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .horizontalRange.get();
            case VERTICAL_CHOP -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .verticalRange.get();
        };
        }

        private static double basicHalfAngle(ValkyrieQueenBasicAttack attack) {
        return switch (attack) {
            case DIAGONAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .diagonalHalfAngle.get();
            case HORIZONTAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .horizontalHalfAngle.get();
            case VERTICAL_CHOP -> BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
                .verticalHalfAngle.get();
        };
        }

        private static BossRefactorAetherConfig.DamageFormula basicDamageFormula(
            ValkyrieQueenBasicAttack attack) {
        return switch (attack) {
            case DIAGONAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE
                .diagonalSlash;
            case HORIZONTAL_SLASH -> BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE
                .horizontalSlash;
            case VERTICAL_CHOP -> BossRefactorAetherConfig.VALKYRIE_QUEEN_DAMAGE
                .verticalChop;
        };
        }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to,
                                            ValkyrieQueen queen) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return direction.lengthSqr() < 1.0E-8 ? horizontalLook(queen) : direction.normalize();
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double deltaX = second.x - first.x;
        double deltaZ = second.z - first.z;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private static double attackDownwardRange() {
        return BossRefactorAetherConfig.VALKYRIE_QUEEN_RANGE
            .attackDownwardRange.get();
    }

    private static AABB attackBounds(Vec3 center, double horizontalRadius,
                                     double upwardRange) {
        double radius = Math.max(0.0, horizontalRadius);
        return new AABB(
                center.x - radius,
                center.y - attackDownwardRange(),
                center.z - radius,
                center.x + radius,
                center.y + Math.max(0.0, upwardRange),
                center.z + radius);
    }

    private static AABB expandDownward(AABB bounds, double downwardRange) {
        return new AABB(
                bounds.minX,
                bounds.minY - Math.max(0.0, downwardRange),
                bounds.minZ,
                bounds.maxX,
                bounds.maxY,
                bounds.maxZ);
    }

    private static boolean isWithinAttackHeight(double sourceY, Entity target,
                                                double upwardRange) {
        AABB targetBounds = target.getBoundingBox();
        return ValkyrieQueenMechanics.isWithinVerticalAttackRange(
                sourceY,
                targetBounds.minY,
                targetBounds.maxY,
                attackDownwardRange(),
                upwardRange);
    }

    private static boolean spawnThunderCrystal(ValkyrieQueen queen,
                                               LivingEntity target, Vec3 origin) {
        ThunderCrystal crystal = new ThunderCrystal(
                AetherEntityTypes.THUNDER_CRYSTAL.get(),
                queen.level(),
                queen,
                target);
        crystal.setPos(origin.x, origin.y + queen.getBbHeight() * 0.5, origin.z);
        return queen.level().addFreshEntity(crystal);
    }

    @Nullable
    private static ItemEntity findSpear(ServerLevel level, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof ItemEntity itemEntity ? itemEntity : null;
    }

    private static void cleanupSpearEntities(ValkyrieQueen queen) {
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }
        AABB bounds = queen.getBoundingBox().inflate(64.0);
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class,
                bounds,
                entity -> entity.getTags().contains(SPEAR_ENTITY_TAG))) {
            item.discard();
        }
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.FLASH,
            position.x, position.y + 1.0, position.z,
            1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
            position.x, position.y + 1.0, position.z,
            18, 0.65, 1.0, 0.65, 0.08);
        level.playSound(null, position.x, position.y, position.z,
            SoundEvents.LIGHTNING_BOLT_IMPACT,
            SoundSource.HOSTILE, 0.8F, 1.25F);
    }

    private static void emitChargeParticles(ValkyrieQueen queen, boolean dash) {
        if (!(queen.level() instanceof ServerLevel level) || queen.tickCount % 2 != 0) {
            return;
        }
        level.sendParticles(
                dash ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.ENCHANT,
                queen.getX(), queen.getY() + 1.0, queen.getZ(),
                8, 0.65, 0.9, 0.65, 0.03);
    }

    private static void emitSpearChargeParticles(ValkyrieQueen queen) {
        if (!(queen.level() instanceof ServerLevel level) || queen.tickCount % 2 != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                queen.getX(), queen.getY() + 1.0, queen.getZ(),
                12, 0.9, 1.1, 0.9, 0.06);
        level.sendParticles(ParticleTypes.END_ROD,
                queen.getX(), queen.getY() + 1.0, queen.getZ(),
                4, 0.6, 0.8, 0.6, 0.02);
    }

    private static void emitBasicTelegraph(ValkyrieQueen queen,
                                           ValkyrieQueenBasicAttack attack) {
        if (!(queen.level() instanceof ServerLevel level) || queen.tickCount % 3 != 0) {
            return;
        }
        Vec3 forward = horizontalLook(queen);
        double range = basicRange(attack);
        for (int step = 1; step <= 4; step++) {
            Vec3 position = queen.position().add(
                    forward.scale(range * step / 4.0)).add(0.0, 0.25, 0.0);
            level.sendParticles(ParticleTypes.CRIT,
                    position.x, position.y, position.z,
                    1, 0.08, 0.08, 0.08, 0.0);
        }
    }

    private static void emitBasicSlash(ValkyrieQueen queen,
                                       ValkyrieQueenBasicAttack attack) {
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }
        int particles = switch (attack) {
            case DIAGONAL_SLASH -> 10;
            case HORIZONTAL_SLASH -> 18;
            case VERTICAL_CHOP -> 8;
        };
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                queen.getX(), queen.getY() + 1.0, queen.getZ(),
                particles, 1.4, 0.8, 1.4, 0.0);
        level.playSound(null, queen.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private static void emitSpinTelegraph(ValkyrieQueen queen, double radius) {
        if (!(queen.level() instanceof ServerLevel level) || queen.tickCount % 2 != 0) {
            return;
        }
        for (int index = 0; index < 12; index++) {
            double angle = Math.PI * 2.0 * index / 12.0;
            level.sendParticles(ParticleTypes.CRIT,
                    queen.getX() + Math.cos(angle) * radius,
                    queen.getY() + 0.2,
                    queen.getZ() + Math.sin(angle) * radius,
                    1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void emitSpinAttack(ValkyrieQueen queen, double radius) {
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }
        for (int index = 0; index < 24; index++) {
            double angle = Math.PI * 2.0 * index / 24.0;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    queen.getX() + Math.cos(angle) * radius * 0.75,
                    queen.getY() + 0.8,
                    queen.getZ() + Math.sin(angle) * radius * 0.75,
                    1, 0.15, 0.1, 0.15, 0.0);
        }
        level.playSound(null, queen.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private static void emitDashParticles(ValkyrieQueen queen) {
        if (!(queen.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.CLOUD,
                queen.getX(), queen.getY() + 0.5, queen.getZ(),
                4, 0.35, 0.45, 0.35, 0.01);
        if (state(queen).phaseTwo) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    queen.getX(), queen.getY() + 0.8, queen.getZ(),
                    4, 0.4, 0.5, 0.4, 0.03);
        }
    }

    private static void setTelegraph(ValkyrieQueen queen, AttackTelegraphShape shape,
                                     Vec3 direction, double length,
                                     double width, double radius) {
        setTelegraph(queen, shape, direction, length, width, radius, 0.0F);
    }

    private static void setTelegraph(ValkyrieQueen queen, AttackTelegraphShape shape,
                                     Vec3 direction, double length,
                                     double width, double radius, float progress) {
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-8) {
            horizontal = horizontalLook(queen);
        } else {
            horizontal = horizontal.normalize();
        }
        if (queen instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(
                new AttackTelegraph(
                    shape,
                    (float) horizontal.x,
                    (float) horizontal.z,
                    (float) Math.max(0.0, length),
                    (float) Math.max(0.0, width),
                            (float) Math.max(0.0, radius),
                            progress));
        }
    }

    private static void clearTelegraph(ValkyrieQueen queen) {
        if (queen instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(AttackTelegraph.NONE);
        }
    }
}