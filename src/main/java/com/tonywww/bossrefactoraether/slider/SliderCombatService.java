package com.tonywww.bossrefactoraether.slider;

import com.aetherteam.aether.block.AetherBlocks;
import com.aetherteam.aether.entity.monster.dungeon.boss.Slider;
import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.mixin.LivingEntityDamageBlockAccessor;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraph;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SliderCombatService {
    public static final ResourceLocation SHIELD_BREAK_SOURCE = ResourceLocation.fromNamespaceAndPath(
        BossRefactorAether.MOD_ID, "shield_block");
    public static final ResourceLocation PICKAXE_BREAK_SOURCE = ResourceLocation.fromNamespaceAndPath(
        BossRefactorAether.MOD_ID, "pickaxe_break");

    private SliderCombatService() {
    }

    public static SliderCombatState state(Slider slider) {
        return ((SliderStateAccess) slider).bossRefactorAether$getCombatState();
    }

    public static int glidePower(Slider slider) {
        return ((SliderStateAccess) slider).bossRefactorAether$getGlidePower();
    }

    public static double movementMultiplier(Slider slider) {
        BossRefactorAetherConfig.SliderMovementConfig movement =
            BossRefactorAetherConfig.SLIDER_MOVEMENT;
        return SliderMechanics.speedMultiplier(
            state(slider).phaseTwo,
            glidePower(slider),
            BossRefactorAetherConfig.SLIDER_COMBAT.maxGlidePower.get(),
            movement.baseSpeedMultiplier.get(),
            movement.phaseTwoSpeedMultiplier.get(),
            movement.glidePowerSpeedPerLayer.get());
    }

    public static boolean isStunned(Slider slider) {
        return state(slider).isStunned(slider.level().getGameTime());
    }

    public static boolean isCurrentAttackParryable(Slider slider) {
        return state(slider).isCurrentAttackParryable();
    }

    public static void destroyBlocksAlongMovement(Slider slider) {
        if (!(slider.level() instanceof ServerLevel level)
                || slider.isDeadOrDying()
                || !ForgeEventFactory.getMobGriefingEvent(level, slider)) {
            return;
        }

        Vec3 movement = slider.getDeltaMovement();
        double distance = movement.length();
        if (distance < 1.0E-4) {
            tryForceBreakBlockingBlocks(level, slider, movement);
            return;
        }

        int samples = SliderMechanics.blockBreakSampleCount(
            distance,
            BossRefactorAetherConfig.SLIDER_COMBAT.blockBreakSampleStep.get(),
            BossRefactorAetherConfig.SLIDER_COMBAT.maxBlockBreakSamples.get());
        AABB origin = slider.getBoundingBox();
        Set<BlockPos> visited = new HashSet<>();
        for (int sampleIndex = 1; sampleIndex <= samples; sampleIndex++) {
            double progress = sampleIndex / (double) samples;
            AABB sampleBounds = origin.move(movement.scale(progress)).inflate(0.01);
            for (BlockPos candidate : BlockPos.betweenClosed(
                    BlockPos.containing(sampleBounds.minX, sampleBounds.minY, sampleBounds.minZ),
                    BlockPos.containing(sampleBounds.maxX, sampleBounds.maxY, sampleBounds.maxZ))) {
                BlockPos position = candidate.immutable();
                if (visited.contains(position)) {
                    continue;
                }
                if (processIntersectingBlock(level, slider, position, sampleBounds)) {
                    visited.add(position);
                }
            }
        }

        tryForceBreakBlockingBlocks(level, slider, movement);
    }

    public static float normalCollisionDamage(Slider slider, float fallbackAttackDamage) {
        return configuredDamage(slider, BossRefactorAetherConfig.SLIDER_DAMAGE.normalCollision,
                fallbackAttackDamage);
    }

    public static boolean tryShieldBlock(Player player, Slider slider, DamageSource source) {
        if (player.level().isClientSide()
                || !((LivingEntityDamageBlockAccessor) player)
                        .bossRefactorAether$isDamageSourceBlocked(source)) {
            return false;
        }
        return completeShieldBlock(player, slider, true);
    }

    public static boolean acceptShieldBlock(Player player, DamageSource source,
                                            boolean broadcastSuccess) {
        Slider slider = sliderFromDamageSource(source);
        return slider != null && completeShieldBlock(player, slider, broadcastSuccess);
    }

    public static void markNormalMoveHit(Slider slider) {
        SliderCombatState state = state(slider);
        if (state.normalMoveActive) {
            state.normalMoveHit = true;
        }
    }

    public static void recordChargedPickaxeAttack(Slider slider, Player player) {
        state(slider).recordChargedPickaxeAttack(
                player.getUUID(), slider.level().getGameTime());
    }

    public static boolean consumeChargedPickaxeAttack(Slider slider, Player player) {
        return state(slider).consumeChargedPickaxeAttack(
                player.getUUID(), slider.level().getGameTime());
    }

    public static void tick(Slider slider) {
        if (slider.level().isClientSide()) {
            return;
        }
        SliderCombatState state = state(slider);
        long gameTime = slider.level().getGameTime();

        if (BossRefactorAetherConfig.SLIDER_COMBAT.immuneToNegativeEffects.get()) {
            removeNegativeEffects(slider);
        }

        if (!state.configuredStateInitialized) {
            state.configuredStateInitialized = true;
            setBarrierLayers(
                slider,
                BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get());
        }

        if (!state.phaseTwo && slider.getHealth() > 0.0F
            && slider.getHealth() < slider.getMaxHealth()
            * BossRefactorAetherConfig.SLIDER_COMBAT.phaseTwoHealthRatio.get()) {
            enterPhaseTwo(slider);
        }
        if (!slider.isAwake() || slider.isDeadOrDying()) {
            cancelSkill(slider);
            return;
        }
        if (state.stunEnd > 0L) {
            if (gameTime >= state.stunEnd) {
                recoverFromStun(slider);
            } else {
                slider.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        if (!state.isSkillActive()
            && glidePower(slider)
            >= BossRefactorAetherConfig.SLIDER_COMBAT.chainGlidePowerCost.get()) {
            state.skillQueued = true;
        }
        if (state.isSkillActive()) {
            tickSkill(slider);
        } else if (state.skillQueued && state.movementPhase == SliderMovementPhase.IDLE) {
            startCharge(slider);
        } else {
            tickTacticalMovement(slider);
        }
    }

    public static void reset(Slider slider) {
        SliderCombatState state = state(slider);
        cancelSkill(slider);
        setBarrierLayers(slider, BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get());
        state.phaseTwo = false;
        state.stunEnd = 0L;
        state.configuredStateInitialized = true;
        setGlidePower(slider, 0);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    public static void onLoaded(Slider slider) {
        state(slider).parryWindowOpen = true;
        closeParryWindow(slider);
        setBarrierLayers(slider, state(slider).barrierLayers);
        setGlidePower(slider, glidePower(slider));
        slider.setDeltaMovement(Vec3.ZERO);
    }

    public static int consumeBarrier(Slider slider, SliderBarrierBreakCause cause,
                                     @Nullable LivingEntity actor, boolean mirrorFinalBreak) {
        if (slider.level().isClientSide() || isStunned(slider)) {
            return state(slider).barrierLayers;
        }
        SliderCombatState state = state(slider);
        if (state.barrierLayers <= 0) {
            return 0;
        }

        setGlidePower(slider, glidePower(slider)
            + BossRefactorAetherConfig.SLIDER_COMBAT
                .glidePowerGainOnBarrierBreak.get());
        setBarrierLayers(slider, state.barrierLayers - 1);
        barrierHitEffects(slider, state.barrierLayers == 0);

        if (cause == SliderBarrierBreakCause.SHIELD || cause == SliderBarrierBreakCause.PARRY) {
            markNormalMoveHit(slider);
            interruptCurrentAttack(slider);
        }
        if (state.barrierLayers == 0) {
            applyLocalStun(slider);
            if (mirrorFinalBreak) {
                ResourceLocation sourceId = cause == SliderBarrierBreakCause.PICKAXE
                        ? PICKAXE_BREAK_SOURCE
                        : SHIELD_BREAK_SOURCE;
                SliderParryIntegration.bridge().mirrorBarrierBreak(slider, actor, sourceId);
            }
        }
        return state.barrierLayers;
    }

    public static int consumeBarrierFromParryAttempt(Slider slider,
                                                     @Nullable LivingEntity actor) {
        state(slider).parryWindowOpen = false;
        return consumeBarrier(
                slider, SliderBarrierBreakCause.PARRY, actor, false);
    }

    public static void acceptParryWithoutBarrier(Slider slider) {
        state(slider).parryWindowOpen = false;
        markNormalMoveHit(slider);
        interruptCurrentAttack(slider);
    }

    public static float barrierDamageMultiplier(Slider slider) {
        SliderCombatState state = state(slider);
        if (state.isStunned(slider.level().getGameTime())) {
            return 1.0F;
        }
        return SliderMechanics.barrierDamageMultiplier(
            state.barrierLayers,
            BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get(),
            BossRefactorAetherConfig.SLIDER_COMBAT.barrierReductionPerLayer.get());
    }

    public static void interruptCurrentAttack(Slider slider) {
        SliderCombatState state = state(slider);
        if (state.skillPhase == SliderSkillPhase.DASHING) {
            finishCurrentDash(slider);
        } else if (state.movementPhase == SliderMovementPhase.STRIKING) {
            finishTacticalStrike(slider);
        }
    }

    private static void setGlidePower(Slider slider, int glidePower) {
        SliderCombatState state = state(slider);
        ((SliderStateAccess) slider).bossRefactorAether$setGlidePower(
            Math.max(
                state.phaseTwo && !state.isStunned(slider.level().getGameTime())
                    ? BossRefactorAetherConfig.SLIDER_COMBAT
                        .phaseTwoMinGlidePower.get()
                    : 0,
                glidePower));
    }

    private static void setBarrierLayers(Slider slider, int barrierLayers) {
        int clamped = SliderMechanics.clampBarrierLayers(
            barrierLayers,
            BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get());
        state(slider).barrierLayers = clamped;
        ((SliderStateAccess) slider).bossRefactorAether$setBarrierLayers(clamped);
    }

    private static void enterPhaseTwo(Slider slider) {
        SliderCombatState state = state(slider);
        state.phaseTwo = true;
        setBarrierLayers(slider, BossRefactorAetherConfig.SLIDER_COMBAT.maxBarrierLayers.get());
        setGlidePower(slider, glidePower(slider));
        if (slider.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION,
                    slider.getX(), slider.getY() + slider.getBbHeight() * 0.5, slider.getZ(),
                    12, 0.8, 0.8, 0.8, 0.02);
            level.playSound(null, slider.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR,
                    SoundSource.HOSTILE, 2.0F, 0.65F);
        }
    }

    private static void applyLocalStun(Slider slider) {
        SliderCombatState state = state(slider);
        int stunTicks = BossRefactorAetherConfig.SLIDER_COMBAT.stunTicks.get();
        state.stunEnd = slider.level().getGameTime() + stunTicks;
        state.normalMoveActive = false;
        state.normalMoveHit = false;
        state.skillQueued = false;
        cancelSkill(slider);
        setGlidePower(slider, 0);
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setMoveDelay(stunTicks);
        slider.setAttackCooldown(stunTicks);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void recoverFromStun(Slider slider) {
        SliderCombatState state = state(slider);
        state.stunEnd = 0L;
        setGlidePower(slider, glidePower(slider));
        slider.setMoveDelay(slider.calculateMoveDelay());
        slider.setAttackCooldown(0);
    }

    private static void startCharge(Slider slider) {
        SliderCombatState state = state(slider);
        int cost = BossRefactorAetherConfig.SLIDER_COMBAT.chainGlidePowerCost.get();
        if (glidePower(slider) < cost) {
            state.skillQueued = false;
            return;
        }
        resetTacticalMovement(slider);
        state.skillQueued = false;
        setGlidePower(slider, SliderMechanics.glidePowerAfterChainCost(
            glidePower(slider), state.phaseTwo,
            BossRefactorAetherConfig.SLIDER_COMBAT.maxGlidePower.get(),
            cost,
            BossRefactorAetherConfig.SLIDER_COMBAT.phaseTwoMinGlidePower.get()));
        state.skillPhase = SliderSkillPhase.CHARGING;
        state.phaseTicks = 0;
        state.completedDashes = 0;
        state.totalDashes = state.phaseTwo
            ? BossRefactorAetherConfig.SLIDER_COMBAT.phaseTwoDashes.get()
            : BossRefactorAetherConfig.SLIDER_COMBAT.phaseOneDashes.get();
        state.extraDashDecided = false;
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setDeltaMovement(Vec3.ZERO);
        closeParryWindow(slider);
        updateContinuousGlideTelegraph(slider, 0);
    }

    private static void tickSkill(Slider slider) {
        SliderCombatState state = state(slider);
        switch (state.skillPhase) {
            case CHARGING -> tickCharge(slider);
            case DASHING -> tickDash(slider);
            case DASH_INTERVAL -> tickDashInterval(slider);
            case IDLE -> {
            }
        }
    }

    private static void tickCharge(Slider slider) {
        SliderCombatState state = state(slider);
        slider.setDeltaMovement(Vec3.ZERO);
        state.phaseTicks++;
        updateContinuousGlideTelegraph(slider, state.phaseTicks);
        if (slider.level() instanceof ServerLevel level && state.phaseTicks % 2 == 0) {
            level.sendParticles(DustParticleOptions.REDSTONE,
                    slider.getX(), slider.getY() + slider.getBbHeight() * 0.5, slider.getZ(),
                    12, 1.0, 1.0, 1.0, 0.02);
        }
        if (state.phaseTicks >= BossRefactorAetherConfig.SLIDER_TIMING.chargeTicks.get()) {
            startDash(slider);
        }
    }

    private static void startDash(Slider slider) {
        LivingEntity target = slider.getTarget();
        if (target == null || !target.isAlive()) {
            cancelSkill(slider);
            return;
        }
        SliderCombatState state = state(slider);
        double deltaX = target.getX() - slider.getX();
        double deltaZ = target.getZ() - slider.getZ();
        Direction.Axis dashAxis = SliderMechanics.chooseAttackAxis(deltaX, deltaZ);
        double axisOffset = dashAxis == Direction.Axis.X ? deltaX : deltaZ;
        double dashSign = Math.abs(axisOffset) < 1.0E-6
            ? (slider.getRandom().nextBoolean() ? 1.0 : -1.0)
            : Math.signum(axisOffset);
        state.dashDirection = SliderMechanics.axisMotion(dashAxis, dashSign);
        state.dashStart = slider.position();
        state.phaseTicks = 0;
        state.dashHits.clear();
        boolean unblockable = BossRefactorAetherConfig.SLIDER_COMBAT
            .phaseTwoFirstDashUnblockable.get()
            && SliderMechanics.isUnblockableChainDash(
                state.phaseTwo, state.completedDashes);
        state.currentDashParryable = !unblockable;
        state.skillPhase = SliderSkillPhase.DASHING;
        if (state.currentDashParryable) {
            openParryWindow(slider);
        } else {
            closeParryWindow(slider);
        }
        double firstStep = SliderMechanics.nextDashStep(
            chainSpeed(slider), 0.0,
            BossRefactorAetherConfig.SLIDER_RANGE.continuousGlideDistance.get());
        slider.setMoveDirection(horizontalDirection(dashAxis, dashSign));
        slider.setDeltaMovement(state.dashDirection.scale(firstStep));
        if (unblockable) {
            emitUnblockableDashMarker(slider, true);
        }
        clearTelegraph(slider);
    }

    private static void tickDash(Slider slider) {
        SliderCombatState state = state(slider);
        if (!state.currentDashParryable) {
            emitUnblockableDashMarker(slider, false);
        }
        if (slider.horizontalCollision) {
            finishCurrentDash(slider);
            return;
        }

        double traveled = slider.position().distanceTo(state.dashStart);
        Vec3 previousMotion = slider.getDeltaMovement();
        AABB sweptBounds = slider.getBoundingBox()
            .expandTowards(previousMotion.scale(-1.0))
                .inflate(0.25);
        List<Player> players = slider.level().getEntitiesOfClass(Player.class, sweptBounds,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : players) {
            if (!state.dashHits.add(player.getUUID())) {
                continue;
            }
                boolean unblockable = BossRefactorAetherConfig.SLIDER_COMBAT
                    .phaseTwoFirstDashUnblockable.get()
                    && SliderMechanics.isUnblockableChainDash(
                        state.phaseTwo, state.completedDashes);
                DamageSource damageSource = SliderDamageTypes.chainDash(slider, unblockable);
                if (tryShieldBlock(player, slider, damageSource)) {
                return;
                }
            player.hurt(
                    damageSource,
                    configuredDamage(slider, BossRefactorAetherConfig.SLIDER_DAMAGE.chainDash,
                            SliderMechanics.DEFAULT_ATTACK_DAMAGE)
            );
            if (state.skillPhase != SliderSkillPhase.DASHING) {
                return;
            }
            finishCurrentDash(slider);
            return;
        }

        state.phaseTicks++;
        if (state.phaseTicks >= BossRefactorAetherConfig.SLIDER_TIMING.dashTickLimit.get()
            || traveled >= BossRefactorAetherConfig.SLIDER_RANGE
                .continuousGlideDistance.get()) {
            finishCurrentDash(slider);
            return;
        }
        double nextStep = SliderMechanics.nextDashStep(
            chainSpeed(slider), traveled,
            BossRefactorAetherConfig.SLIDER_RANGE.continuousGlideDistance.get());
        Direction.Axis dashAxis = Math.abs(state.dashDirection.x) > 0.0
            ? Direction.Axis.X
            : Direction.Axis.Z;
        slider.setMoveDirection(horizontalDirection(
            dashAxis, coordinate(state.dashDirection, dashAxis)));
        slider.setDeltaMovement(state.dashDirection.scale(nextStep));
    }

    private static void tickTacticalMovement(Slider slider) {
        SliderCombatState state = state(slider);
        if (state.movementPhase == SliderMovementPhase.RECOVERING) {
            tickTacticalRecovery(slider);
            return;
        }

        LivingEntity target = slider.getTarget();
        if (target == null || !target.isAlive()) {
            resetTacticalMovement(slider);
            return;
        }
        slider.setTargetPoint(null);
        state.movementTicks++;
        switch (state.movementPhase) {
            case IDLE -> planTacticalLane(slider, target);
            case ALIGNING -> tickTacticalAlignment(slider, target);
            case RETREATING -> tickTacticalRetreat(slider, target);
            case BAITING -> tickTacticalBait(slider, target);
            case STRIKING -> tickTacticalStrike(slider);
            case RECOVERING -> {
            }
        }
    }

    private static void planTacticalLane(Slider slider, LivingEntity target) {
        SliderCombatState state = state(slider);
        clearTelegraph(slider);
        double deltaX = target.getX() - slider.getX();
        double deltaZ = target.getZ() - slider.getZ();
        state.attackAxis = SliderMechanics.chooseAttackAxis(deltaX, deltaZ);
        state.laneCoordinate = SliderMechanics.predictedLaneCoordinate(
                state.attackAxis,
                target.getX(), target.getZ(),
            target.getDeltaMovement().x, target.getDeltaMovement().z,
            BossRefactorAetherConfig.SLIDER_RANGE.laneLeadTicks.get(),
            BossRefactorAetherConfig.SLIDER_RANGE.maxLaneLead.get());
        double attackOffset = state.attackAxis == Direction.Axis.X ? deltaX : deltaZ;
        state.tacticalDirection = Math.abs(attackOffset) < 1.0E-6
                ? (slider.getRandom().nextBoolean() ? 1.0 : -1.0)
                : Math.signum(attackOffset);
        state.movementPhase = SliderMovementPhase.ALIGNING;
        state.movementTicks = 0;
        stopTacticalMovement(slider);
    }

    private static void tickTacticalAlignment(Slider slider, LivingEntity target) {
        SliderCombatState state = state(slider);
        Direction.Axis alignmentAxis = SliderMechanics.perpendicularAxis(state.attackAxis);
        double currentCoordinate = coordinate(slider.position(), alignmentAxis);
        double remaining = state.laneCoordinate - currentCoordinate;
        if (Math.abs(remaining) <= BossRefactorAetherConfig.SLIDER_RANGE
            .alignmentTolerance.get()) {
            if (!SliderMechanics.isInAttackLane(
                    state.attackAxis, slider.getX(), slider.getZ(),
                target.getX(), target.getZ(),
                BossRefactorAetherConfig.SLIDER_RANGE.laneHalfWidth.get())) {
                planTacticalLane(slider, target);
                return;
            }
            state.movementPhase = SliderMovementPhase.RETREATING;
            state.movementTicks = 0;
            stopTacticalMovement(slider);
            return;
        }
        if (state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING
            .alignTickLimit.get()) {
            planTacticalLane(slider, target);
            return;
        }
        double step = SliderMechanics.stepToward(
                currentCoordinate,
                state.laneCoordinate,
            tacticalSpeed(slider,
                BossRefactorAetherConfig.SLIDER_MOVEMENT
                    .alignmentSpeedMultiplier.get()));
        setAxisMovement(slider, alignmentAxis, step);
    }

    private static void tickTacticalRetreat(Slider slider, LivingEntity target) {
        SliderCombatState state = state(slider);
        double signedTargetDistance = signedTargetDistance(slider, target, state);
        if (signedTargetDistance <= 0.0
                || !SliderMechanics.isInAttackLane(
                        state.attackAxis, slider.getX(), slider.getZ(),
                    target.getX(), target.getZ(),
                    BossRefactorAetherConfig.SLIDER_RANGE.laneHalfWidth.get())) {
            planTacticalLane(slider, target);
            return;
        }
        if ((slider.horizontalCollision && state.movementTicks > 1)
                || signedTargetDistance >= BossRefactorAetherConfig.SLIDER_RANGE
                    .retreatDistance.get()
                || state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING
                    .retreatTickLimit.get()) {
            state.movementPhase = SliderMovementPhase.BAITING;
            state.movementTicks = 0;
            stopTacticalMovement(slider);
            return;
        }
        setAxisMovement(
                slider,
                state.attackAxis,
                -state.tacticalDirection * tacticalSpeed(
                    slider,
                    BossRefactorAetherConfig.SLIDER_MOVEMENT
                        .retreatSpeedMultiplier.get()));
    }

    private static void tickTacticalBait(Slider slider, LivingEntity target) {
        SliderCombatState state = state(slider);
        stopTacticalMovement(slider);
        setTelegraph(
            slider,
            AttackTelegraphShape.CORRIDOR,
            SliderMechanics.axisMotion(state.attackAxis, state.tacticalDirection),
            BossRefactorAetherConfig.SLIDER_RANGE.strikeDistance.get(),
            BossRefactorAetherConfig.SLIDER_RANGE.laneHalfWidth.get(),
                0.0,
                AttackTelegraph.windupProgress(
                    state.movementTicks,
                    BossRefactorAetherConfig.SLIDER_TIMING.baitMinTicks.get()));
        if (slider.level() instanceof ServerLevel level && state.movementTicks % 4 == 0) {
            emitLaneTelegraph(level, slider, state.attackAxis, state.tacticalDirection);
        }

        double signedTargetDistance = signedTargetDistance(slider, target, state);
        boolean inLane = SliderMechanics.isInAttackLane(
            state.attackAxis, slider.getX(), slider.getZ(), target.getX(), target.getZ(),
            BossRefactorAetherConfig.SLIDER_RANGE.laneHalfWidth.get());
        if (signedTargetDistance <= 0.0) {
            planTacticalLane(slider, target);
        } else if (inLane && state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING
            .baitMinTicks.get()) {
            startTacticalStrike(slider);
        } else if (state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING
            .baitMaxTicks.get()) {
            planTacticalLane(slider, target);
        }
    }

    private static void startTacticalStrike(Slider slider) {
        SliderCombatState state = state(slider);
        state.movementPhase = SliderMovementPhase.STRIKING;
        state.movementTicks = 0;
        state.tacticalStart = slider.position();
        state.normalMoveActive = true;
        state.normalMoveHit = false;
        clearTelegraph(slider);
        openParryWindow(slider);
        setAxisMovement(
                slider,
                state.attackAxis,
                state.tacticalDirection * tacticalSpeed(
                    slider,
                    BossRefactorAetherConfig.SLIDER_MOVEMENT
                        .strikeSpeedMultiplier.get()));
    }

    private static void tickTacticalStrike(Slider slider) {
        SliderCombatState state = state(slider);
        Vec3 previousMotion = slider.getDeltaMovement();
        AABB sweptBounds = slider.getBoundingBox()
                .expandTowards(previousMotion.scale(-1.0))
                .inflate(0.25);
        List<Player> players = slider.level().getEntitiesOfClass(Player.class, sweptBounds,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator());
        for (Player player : players) {
            DamageSource source = SliderDamageTypes.collision(slider);
            if (tryShieldBlock(player, slider, source)) {
                if (!isStunned(slider)
                        && state(slider).movementPhase == SliderMovementPhase.STRIKING) {
                    finishTacticalStrike(slider);
                }
                return;
            }
            markNormalMoveHit(slider);
            player.hurt(
                    source,
                    normalCollisionDamage(slider, (float) SliderMechanics.DEFAULT_ATTACK_DAMAGE));
            finishTacticalStrike(slider);
            return;
        }

        double traveled = slider.position().distanceTo(state.tacticalStart);
        if (slider.horizontalCollision
                || state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING
                    .strikeTickLimit.get()
                || traveled >= BossRefactorAetherConfig.SLIDER_RANGE.strikeDistance.get()) {
            finishTacticalStrike(slider);
            return;
        }
        setAxisMovement(
                slider,
                state.attackAxis,
                state.tacticalDirection * tacticalSpeed(
                    slider,
                    BossRefactorAetherConfig.SLIDER_MOVEMENT
                        .strikeSpeedMultiplier.get()));
    }

    private static void finishTacticalStrike(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        if (state.normalMoveActive) {
            int gain = state.normalMoveHit
                ? BossRefactorAetherConfig.SLIDER_COMBAT.glidePowerGainOnHit.get()
                : BossRefactorAetherConfig.SLIDER_COMBAT.glidePowerGainOnMiss.get();
            setGlidePower(slider, glidePower(slider) + gain);
        }
        state.normalMoveActive = false;
        state.normalMoveHit = false;
        if (glidePower(slider) >= BossRefactorAetherConfig.SLIDER_COMBAT
            .chainGlidePowerCost.get()) {
            state.skillQueued = true;
        }
        state.movementPhase = SliderMovementPhase.RECOVERING;
        state.movementTicks = 0;
        stopTacticalMovement(slider);
    }

    private static void tickTacticalRecovery(Slider slider) {
        SliderCombatState state = state(slider);
        stopTacticalMovement(slider);
        state.movementTicks++;
        if (state.movementTicks >= BossRefactorAetherConfig.SLIDER_TIMING.recoveryTicks.get()) {
            state.movementPhase = SliderMovementPhase.IDLE;
            state.movementTicks = 0;
        }
    }

    private static void resetTacticalMovement(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        state.movementPhase = SliderMovementPhase.IDLE;
        state.movementTicks = 0;
        state.normalMoveActive = false;
        state.normalMoveHit = false;
        state.tacticalStart = Vec3.ZERO;
        clearTelegraph(slider);
        stopTacticalMovement(slider);
    }

    private static void stopTacticalMovement(Slider slider) {
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void setAxisMovement(Slider slider, Direction.Axis axis, double movement) {
        slider.setMoveDirection(horizontalDirection(axis, movement));
        slider.setDeltaMovement(SliderMechanics.axisMotion(axis, movement));
    }

    private static Direction horizontalDirection(Direction.Axis axis, double movement) {
        if (axis == Direction.Axis.X) {
            return movement >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return movement >= 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static double coordinate(Vec3 position, Direction.Axis axis) {
        return axis == Direction.Axis.X ? position.x : position.z;
    }

    private static double signedTargetDistance(
            Slider slider, LivingEntity target, SliderCombatState state) {
        double distance = state.attackAxis == Direction.Axis.X
                ? target.getX() - slider.getX()
                : target.getZ() - slider.getZ();
        return distance * state.tacticalDirection;
    }

    private static double tacticalSpeed(Slider slider, double multiplier) {
        return Math.max(0.0, slider.getMaxVelocity() * multiplier);
    }

    private static void emitLaneTelegraph(ServerLevel level, Slider slider,
                                          Direction.Axis axis, double direction) {
        for (int distance = 1; distance <= 6; distance++) {
            Vec3 offset = SliderMechanics.axisMotion(axis, direction * distance);
            level.sendParticles(
                    DustParticleOptions.REDSTONE,
                    slider.getX() + offset.x,
                    slider.getY() + 0.15,
                    slider.getZ() + offset.z,
                    1, 0.05, 0.02, 0.05, 0.0);
        }
    }

    private static void emitUnblockableDashMarker(Slider slider, boolean starting) {
        if (!(slider.level() instanceof ServerLevel level)) {
            return;
        }
        double centerY = slider.getY() + slider.getBbHeight() * 0.5;
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                slider.getX(), centerY, slider.getZ(),
                starting ? 32 : 8, 0.8, 0.8, 0.8, 0.04);
        level.sendParticles(
                ParticleTypes.END_ROD,
                slider.getX(), centerY, slider.getZ(),
                starting ? 20 : 4, 0.65, 0.65, 0.65, 0.08);
        if (starting) {
            level.playSound(null, slider.blockPosition(), SoundEvents.WITHER_SHOOT,
                    SoundSource.HOSTILE, 1.5F, 0.65F);
        }
    }

    private static void finishCurrentDash(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        slider.setDeltaMovement(Vec3.ZERO);
        state.completedDashes++;

        if (state.completedDashes >= state.totalDashes && !state.extraDashDecided) {
            state.extraDashDecided = true;
                if (slider.getRandom().nextDouble()
                    < BossRefactorAetherConfig.SLIDER_COMBAT.extraDashChance.get()) {
                state.totalDashes++;
            }
        }
        if (state.completedDashes >= state.totalDashes) {
            finishSkill(slider);
            return;
        }
        state.skillPhase = SliderSkillPhase.DASH_INTERVAL;
        state.phaseTicks = 0;
    }

    private static float configuredDamage(
            Slider slider, BossRefactorAetherConfig.DamageFormula formula,
            double fallbackAttackDamage) {
        AttributeInstance attackDamage = slider.getAttribute(Attributes.ATTACK_DAMAGE);
        return SliderMechanics.configuredDamage(
                formula.baseDamage.get(),
            attackDamage != null ? attackDamage.getValue() : fallbackAttackDamage,
                formula.attackDamageMultiplier.get(),
                state(slider).phaseTwo,
                glidePower(slider),
                BossRefactorAetherConfig.SLIDER_COMBAT.maxGlidePower.get(),
                BossRefactorAetherConfig.SLIDER_COMBAT.phaseTwoDamageMultiplier.get(),
                BossRefactorAetherConfig.SLIDER_COMBAT.glidePowerDamagePerLayer.get());
    }

    private static boolean completeShieldBlock(Player player, Slider slider,
                                               boolean broadcastSuccess) {
        SliderCombatState state = state(slider);
        long gameTime = slider.level().getGameTime();
        if (slider.level().isClientSide()) {
            return false;
        }
        if (!state.claimShieldBlock(player.getUUID(), gameTime)) {
            return true;
        }
        if (state.barrierLayers > 0 && !state.isStunned(gameTime)) {
            consumeBarrier(slider, SliderBarrierBreakCause.SHIELD, player, true);
        } else {
            markNormalMoveHit(slider);
            interruptCurrentAttack(slider);
        }

        ItemStack blockingItem = player.getUseItem();
        if (!blockingItem.isEmpty()) {
            player.getCooldowns().addCooldown(
                    blockingItem.getItem(),
                    BossRefactorAetherConfig.SLIDER_COMBAT.shieldCooldownTicks.get());
        }
        if (broadcastSuccess) {
            player.level().broadcastEntityEvent(player, (byte) 29);
        }
        player.stopUsingItem();
        return true;
    }

    @Nullable
    private static Slider sliderFromDamageSource(DamageSource source) {
        Entity causingEntity = source.getEntity();
        if (causingEntity instanceof Slider slider) {
            return slider;
        }
        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof Slider slider) {
            return slider;
        }
        if (directEntity instanceof Projectile projectile
                && projectile.getOwner() instanceof Slider slider) {
            return slider;
        }
        return null;
    }

    private static boolean processIntersectingBlock(ServerLevel level, Slider slider,
                                                    BlockPos position, AABB sampleBounds) {
        BlockState blockState = level.getBlockState(position);
        if (blockState.isAir()) {
            return true;
        }

        VoxelShape collisionShape = blockState.getCollisionShape(level, position);
        boolean intersects = collisionShape.toAabbs().stream()
                .map(bounds -> bounds.move(position))
                .anyMatch(sampleBounds::intersects);
        if (!intersects) {
            return collisionShape.isEmpty();
        }
        if (!blockState.hasBlockEntity()
                && !blockState.is(SliderBlockTags.UNBREAKABLE)
                && blockState.getDestroySpeed(level, position) >= 0.0F
                && blockState.canEntityDestroy(level, position, slider)
                && ForgeEventFactory.onEntityDestroyBlock(slider, position, blockState)) {
            level.destroyBlock(position, false, slider);
        }
        return true;
    }

    private static void forceBreakBlockingBlocks(ServerLevel level, Slider slider,
                                                 Vec3 movement) {
        Vec3 horizontal = new Vec3(movement.x, 0.0, movement.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            SliderCombatState state = state(slider);
            horizontal = switch (state.movementPhase) {
            case ALIGNING -> SliderMechanics.axisMotion(
                SliderMechanics.perpendicularAxis(state.attackAxis),
                state.laneCoordinate - coordinate(
                    slider.position(),
                    SliderMechanics.perpendicularAxis(state.attackAxis)));
            case RETREATING -> SliderMechanics.axisMotion(
                state.attackAxis, -state.tacticalDirection);
            case STRIKING -> SliderMechanics.axisMotion(
                state.attackAxis, state.tacticalDirection);
            default -> Vec3.ZERO;
            };
            if (horizontal.lengthSqr() < 1.0E-6) {
            return;
            }
        }
        AABB blockingBounds = slider.getBoundingBox()
                .expandTowards(horizontal.normalize().scale(0.8))
                .inflate(0.05);
        for (BlockPos candidate : BlockPos.betweenClosed(
                BlockPos.containing(
                        blockingBounds.minX, blockingBounds.minY, blockingBounds.minZ),
                BlockPos.containing(
                        blockingBounds.maxX, blockingBounds.maxY, blockingBounds.maxZ))) {
            BlockPos position = candidate.immutable();
            BlockState blockState = level.getBlockState(position);
            if (blockState.isAir() || blockState.hasBlockEntity()
                    || blockState.is(SliderBlockTags.UNBREAKABLE)
                    || blockState.getDestroySpeed(level, position) < 0.0F) {
                continue;
            }
            boolean intersects = blockState.getCollisionShape(level, position).toAabbs().stream()
                    .map(bounds -> bounds.move(position))
                    .anyMatch(blockingBounds::intersects);
            if (intersects
                    && ForgeEventFactory.onEntityDestroyBlock(
                            slider, position, blockState)) {
                level.destroyBlock(position, false, slider);
            }
        }
    }

    private static void tryForceBreakBlockingBlocks(ServerLevel level, Slider slider,
                                                    Vec3 movement) {
        SliderMovementPhase phase = state(slider).movementPhase;
        boolean ordinaryMovement = phase == SliderMovementPhase.ALIGNING
                || phase == SliderMovementPhase.RETREATING
                || phase == SliderMovementPhase.STRIKING;
        if (ordinaryMovement
                && slider.horizontalCollision
                && BossRefactorAetherConfig.SLIDER_COMBAT
                        .forceBreakBlockedBlocks.get()) {
            forceBreakBlockingBlocks(level, slider, movement);
        }
    }

    private static void removeNegativeEffects(Slider slider) {
        List<MobEffectInstance> harmfulEffects = slider.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        for (MobEffectInstance effect : harmfulEffects) {
            slider.removeEffect(effect.getEffect());
        }
    }

    private static void tickDashInterval(Slider slider) {
        SliderCombatState state = state(slider);
        slider.setDeltaMovement(Vec3.ZERO);
        state.phaseTicks++;
        if (state.phaseTicks >= BossRefactorAetherConfig.SLIDER_TIMING
            .dashIntervalTicks.get()) {
            startDash(slider);
        }
    }

    private static void finishSkill(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        state.resetTransient();
        clearTelegraph(slider);
        slider.setMoveDelay(slider.calculateMoveDelay());
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void cancelSkill(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        state.resetTransient();
        clearTelegraph(slider);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void openParryWindow(Slider slider) {
        SliderCombatState state = state(slider);
        if (!state.parryWindowOpen && SliderParryIntegration.bridge().openWindow(slider)) {
            state.parryWindowOpen = true;
        }
    }

    private static void closeParryWindow(Slider slider) {
        SliderCombatState state = state(slider);
        if (state.parryWindowOpen) {
            SliderParryIntegration.bridge().closeWindow(slider);
            state.parryWindowOpen = false;
        }
    }

    private static double chainSpeed(Slider slider) {
        return slider.getMaxVelocity()
            * BossRefactorAetherConfig.SLIDER_MOVEMENT
                .chainDashSpeedMultiplier.get();
    }

    private static void barrierHitEffects(Slider slider, boolean broken) {
        if (!(slider.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(new BlockParticleOption(
                        ParticleTypes.BLOCK, AetherBlocks.CARVED_STONE.get().defaultBlockState()),
                slider.getX(), slider.getY() + slider.getBbHeight() * 0.5, slider.getZ(),
                broken ? 40 : 16, 0.8, 0.8, 0.8, 0.08);
        level.playSound(null, slider.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, broken ? 2.0F : 1.0F, broken ? 0.55F : 0.9F);
    }

    private static void setTelegraph(Slider slider, AttackTelegraphShape shape,
                                     Vec3 direction, double length,
                                     double width, double radius, float progress) {
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-8) {
            return;
        }
        horizontal = horizontal.normalize();
        if (slider instanceof AttackTelegraphAccess access) {
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

            private static void updateContinuousGlideTelegraph(Slider slider, int elapsedTicks) {
            LivingEntity target = slider.getTarget();
            if (target == null || !target.isAlive()) {
                clearTelegraph(slider);
                return;
            }
            Direction.Axis axis = SliderMechanics.chooseAttackAxis(
                target.getX() - slider.getX(), target.getZ() - slider.getZ());
            double signed = axis == Direction.Axis.X
                ? target.getX() - slider.getX()
                : target.getZ() - slider.getZ();
            Vec3 direction = SliderMechanics.axisMotion(
                axis, signed >= 0.0 ? 1.0 : -1.0);
            setTelegraph(
                slider,
                AttackTelegraphShape.CORRIDOR,
                direction,
                BossRefactorAetherConfig.SLIDER_RANGE.continuousGlideDistance.get(),
                slider.getBbWidth() * 0.65,
                0.0,
                AttackTelegraph.windupProgress(
                    elapsedTicks,
                    BossRefactorAetherConfig.SLIDER_TIMING.chargeTicks.get()));
            }

    private static void clearTelegraph(Slider slider) {
        if (slider instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(AttackTelegraph.NONE);
        }
    }
}
