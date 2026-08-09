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

    public static boolean shouldOverrideOriginalMovement(Slider slider) {
        SliderCombatState state = state(slider);
        return SliderMechanics.shouldOverrideOriginalMovement(
            slider.isAwake(),
            slider.getDungeon() != null,
            isStunned(slider),
            state.isSkillActive());
    }

    public static void trackDungeonWithoutRoomReset(Slider slider) {
        if (slider.getDungeon() == null) {
            return;
        }
        slider.getDungeon().trackPlayers();
    }

    public static void destroyBlocksAlongMovement(Slider slider) {
        if (!(slider.level() instanceof ServerLevel level)
                || slider.isDeadOrDying()
                || !shouldBreakBlocksAlongMovement(slider)) {
            return;
        }

        Vec3 movement = slider.getDeltaMovement();
        boolean mobGriefingAllowed = ForgeEventFactory.getMobGriefingEvent(level, slider);
        boolean blockBreakingAllowed = SliderMechanics.isBlockBreakingAllowed(
                slider.isBossFight(),
                mobGriefingAllowed,
                BossRefactorAetherConfig.SLIDER_COMBAT
                    .blockBreakIgnoresMobGriefing.get());
        if (!blockBreakingAllowed) {
            return;
        }
        double distance = movement.length();
        if (distance < 1.0E-4) {
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

    }

    private static boolean shouldBreakBlocksAlongMovement(Slider slider) {
        return SliderMechanics.shouldBreakBlocksAlongMovement(
            state(slider).isSkillActive());
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
            deactivateArenaMovement(slider);
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
        if (slider.getDungeon() == null) {
            releaseArenaMovementControl(slider);
            return;
        }
        if (!state.isSkillActive()
            && glidePower(slider)
            >= BossRefactorAetherConfig.SLIDER_COMBAT.chainGlidePowerCost.get()) {
            state.skillQueued = true;
        }
        if (state.isSkillActive()) {
            tickSkill(slider);
        } else {
            tickArenaMovement(slider);
        }
        synchronizeParryWindow(slider);
    }

    public static void reset(Slider slider) {
        SliderCombatState state = state(slider);
        deactivateArenaMovement(slider);
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
        interruptCurrentAttack(slider);
        applyLocalStun(slider);
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
        if (state.skillPhase == SliderSkillPhase.CHARGING) {
            cancelSkill(slider);
        } else if (state.skillPhase == SliderSkillPhase.DASHING) {
            finishCurrentDash(slider);
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
        state.movementPhase = SliderMovementPhase.IDLE;
        state.movementTicks = 0;
        stopArenaMovement(slider);
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
        state.currentDashParryable = !BossRefactorAetherConfig.SLIDER_COMBAT
            .phaseTwoFirstDashUnblockable.get()
            || !SliderMechanics.isUnblockableChainDash(state.phaseTwo, 0);
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setDeltaMovement(Vec3.ZERO);
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
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
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
        AttackTelegraph telegraph = slider instanceof AttackTelegraphAccess access
            ? access.bossRefactorAether$getAttackTelegraph()
            : AttackTelegraph.NONE;
        Vec3 lockedDirection = new Vec3(
            telegraph.directionX(), 0.0, telegraph.directionZ());
        double deltaX = target.getX() - slider.getX();
        double deltaZ = target.getZ() - slider.getZ();
        Direction.Axis dashAxis = lockedDirection.lengthSqr() >= 1.0E-8
            ? SliderMechanics.chooseAttackAxis(
                lockedDirection.x, lockedDirection.z)
            : SliderMechanics.chooseAttackAxis(deltaX, deltaZ);
        double axisOffset = lockedDirection.lengthSqr() >= 1.0E-8
            ? (dashAxis == Direction.Axis.X
                ? lockedDirection.x : lockedDirection.z)
            : (dashAxis == Direction.Axis.X ? deltaX : deltaZ);
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
        slider.setTargetPoint(null);
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

    private static void tickArenaMovement(Slider slider) {
        SliderCombatState state = state(slider);
        if (!state.patrolDirectionInitialized) {
            state.patrolClockwise = slider.getRandom().nextBoolean();
            state.patrolDirectionInitialized = true;
        }
        LivingEntity target = slider.getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
        }

        if (state.movementPhase == SliderMovementPhase.IDLE) {
            beginReturnToEdge(slider);
        }
        if (state.movementPhase == SliderMovementPhase.VERTICAL_ALIGNING) {
            tickVerticalAlignment(slider, target);
            return;
        }
        if ((state.movementPhase == SliderMovementPhase.RETURNING_TO_EDGE
                || state.movementPhase == SliderMovementPhase.PATROLLING)
                && target != null && needsVerticalAlignment(slider, target)) {
            startVerticalAlignment(slider, state.movementPhase);
            return;
        }
        if (state.movementPhase == SliderMovementPhase.PATROLLING
                && state.skillQueued && target != null
                && canSkillHitFromCurrentPosition(slider, target)) {
            startCharge(slider);
            return;
        }

        switch (state.movementPhase) {
            case RETURNING_TO_EDGE -> tickReturnToEdge(slider);
            case PATROLLING -> tickPerimeterPatrol(slider, target);
            case IDLE, VERTICAL_ALIGNING -> {
            }
        }
    }

    private static void beginReturnToEdge(Slider slider) {
        AABB perimeter = arenaPerimeter(slider);
        if (perimeter == null) {
            releaseArenaMovementControl(slider);
            return;
        }
        SliderCombatState state = state(slider);
        state.perimeterEdge = SliderMechanics.nearestPerimeterEdge(
                slider.getX(), slider.getZ(),
                perimeter.minX, perimeter.maxX, perimeter.minZ, perimeter.maxZ);
        state.movementPhase = SliderMovementPhase.RETURNING_TO_EDGE;
        state.movementTicks = 0;
        stopArenaMovement(slider);
    }

    private static void tickReturnToEdge(Slider slider) {
        SliderCombatState state = state(slider);
        AABB perimeter = arenaPerimeter(slider);
        if (perimeter == null) {
            releaseArenaMovementControl(slider);
            return;
        }
        Vec3 destination = SliderMechanics.closestPointOnPerimeterEdge(
                state.perimeterEdge, slider.getX(), slider.getZ(),
                perimeter.minX, perimeter.maxX, perimeter.minZ, perimeter.maxZ);
        if (horizontalDistanceSquared(slider.position(), destination)
                <= square(BossRefactorAetherConfig.SLIDER_RANGE
                    .perimeterArrivalTolerance.get())) {
            state.movementPhase = SliderMovementPhase.PATROLLING;
            state.movementTicks = 0;
            stopArenaMovement(slider);
            return;
        }
        setHorizontalMovement(slider, SliderMechanics.horizontalStepToward(
                slider.getX(), slider.getZ(), destination.x, destination.z,
                arenaMovementSpeed(slider, BossRefactorAetherConfig.SLIDER_MOVEMENT
                    .edgeReturnSpeedMultiplier.get())));
    }

    private static void tickPerimeterPatrol(
            Slider slider, @Nullable LivingEntity target) {
        SliderCombatState state = state(slider);
        AABB perimeter = arenaPerimeter(slider);
        if (perimeter == null) {
            releaseArenaMovementControl(slider);
            return;
        }
        Vec3 destination = SliderMechanics.patrolCorner(
                state.perimeterEdge, state.patrolClockwise,
                perimeter.minX, perimeter.maxX, perimeter.minZ, perimeter.maxZ);
        if (horizontalDistanceSquared(slider.position(), destination)
                <= square(BossRefactorAetherConfig.SLIDER_RANGE
                    .perimeterArrivalTolerance.get())) {
            if (state.movementTicks > 0) {
                grantPerimeterProgress(slider);
            }
            state.perimeterEdge = SliderMechanics.nextPerimeterEdge(
                    state.perimeterEdge, state.patrolClockwise);
            state.movementTicks = 0;
            if (state.skillQueued && target != null
                    && canSkillHitFromCurrentPosition(slider, target)) {
                startCharge(slider);
                return;
            }
            destination = SliderMechanics.patrolCorner(
                    state.perimeterEdge, state.patrolClockwise,
                    perimeter.minX, perimeter.maxX, perimeter.minZ, perimeter.maxZ);
        }
        state.movementTicks++;
        setHorizontalMovement(slider, SliderMechanics.horizontalStepToward(
                slider.getX(), slider.getZ(), destination.x, destination.z,
                arenaMovementSpeed(slider, BossRefactorAetherConfig.SLIDER_MOVEMENT
                    .perimeterPatrolSpeedMultiplier.get())));
    }

    private static void startVerticalAlignment(
            Slider slider, SliderMovementPhase resumeMovementPhase) {
        SliderCombatState state = state(slider);
        state.resumeMovementPhase = resumeMovementPhase;
        state.movementPhase = SliderMovementPhase.VERTICAL_ALIGNING;
        clearTelegraph(slider);
        stopArenaMovement(slider);
    }

    private static void tickVerticalAlignment(
            Slider slider, @Nullable LivingEntity target) {
        SliderCombatState state = state(slider);
        if (target == null || !needsVerticalAlignment(slider, target)) {
            state.movementPhase = state.resumeMovementPhase;
            stopArenaMovement(slider);
            return;
        }
        AABB sliderBounds = slider.getBoundingBox();
        AABB targetBounds = target.getBoundingBox();
        double step = SliderMechanics.centerHeightAlignmentStep(
                sliderBounds.minY, sliderBounds.maxY,
                targetBounds.minY, targetBounds.maxY,
                arenaMovementSpeed(slider, BossRefactorAetherConfig.SLIDER_MOVEMENT
                    .verticalAlignmentSpeedMultiplier.get()));
        setVerticalMovement(slider, step);
    }

    private static boolean needsVerticalAlignment(Slider slider, LivingEntity target) {
        AABB sliderBounds = slider.getBoundingBox();
        AABB targetBounds = target.getBoundingBox();
        return !SliderMechanics.isCenterHeightAligned(
                sliderBounds.minY, sliderBounds.maxY,
                targetBounds.minY, targetBounds.maxY,
                BossRefactorAetherConfig.SLIDER_RANGE
                    .verticalAlignmentTolerance.get());
    }

    private static boolean canSkillHitFromCurrentPosition(
            Slider slider, LivingEntity target) {
        double laneHalfWidth = slider.getBbWidth() * 0.5
                + target.getBbWidth() * 0.5
                + SliderMechanics.DASH_HIT_INFLATION;
        double maximumReach = SliderMechanics.maximumDashReach(
            BossRefactorAetherConfig.SLIDER_RANGE.continuousGlideDistance.get(),
            chainSpeed(slider),
            BossRefactorAetherConfig.SLIDER_TIMING.dashTickLimit.get());
        return SliderMechanics.canHitWithAxisDash(
                slider.getX(), slider.getZ(), target.getX(), target.getZ(),
            maximumReach, laneHalfWidth);
    }

    private static void grantPerimeterProgress(Slider slider) {
        setGlidePower(slider, glidePower(slider)
                + BossRefactorAetherConfig.SLIDER_COMBAT
                    .glidePowerGainPerPatrolEdge.get());
        if (glidePower(slider) >= BossRefactorAetherConfig.SLIDER_COMBAT
                .chainGlidePowerCost.get()) {
            state(slider).skillQueued = true;
        }
    }

    private static void releaseArenaMovementControl(Slider slider) {
        SliderCombatState state = state(slider);
        if (!state.hasArenaMovementState()) {
            return;
        }
        closeParryWindow(slider);
        state.resetTransient();
        clearTelegraph(slider);
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void stopArenaMovement(Slider slider) {
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setDeltaMovement(Vec3.ZERO);
    }

    private static void setHorizontalMovement(Slider slider, Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-8) {
            stopArenaMovement(slider);
            return;
        }
        Direction.Axis axis = Math.abs(movement.x) >= Math.abs(movement.z)
                ? Direction.Axis.X : Direction.Axis.Z;
        slider.setMoveDirection(horizontalDirection(axis, coordinate(movement, axis)));
        slider.setTargetPoint(null);
        slider.setDeltaMovement(movement);
    }

    private static void setVerticalMovement(Slider slider, double movement) {
        slider.setMoveDirection(movement >= 0.0 ? Direction.UP : Direction.DOWN);
        slider.setDeltaMovement(new Vec3(0.0, movement, 0.0));
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

    private static double arenaMovementSpeed(Slider slider, double multiplier) {
        return SliderMechanics.explicitMovementSpeed(
            slider.getMaxVelocity(), movementMultiplier(slider), multiplier);
    }

    @Nullable
    private static AABB arenaPerimeter(Slider slider) {
        if (slider.getDungeon() == null) {
            return null;
        }
        AABB room = slider.getDungeon().roomBounds();
        double inset = SliderMechanics.perimeterInset(
            slider.getBbWidth(),
            BossRefactorAetherConfig.SLIDER_RANGE.perimeterEdgeClearance.get());
        double minimumX = SliderMechanics.insetMinimum(room.minX, room.maxX, inset);
        double maximumX = SliderMechanics.insetMaximum(room.minX, room.maxX, inset);
        double minimumZ = SliderMechanics.insetMinimum(room.minZ, room.maxZ, inset);
        double maximumZ = SliderMechanics.insetMaximum(room.minZ, room.maxZ, inset);
        return new AABB(minimumX, room.minY, minimumZ, maximumX, room.maxY, maximumZ);
    }

    private static double horizontalDistanceSquared(Vec3 first, Vec3 second) {
        double deltaX = first.x - second.x;
        double deltaZ = first.z - second.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static double square(double value) {
        return value * value;
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
        if (canDestroyMovementBlock(level, slider, position, blockState)
                && ForgeEventFactory.onEntityDestroyBlock(slider, position, blockState)) {
            level.destroyBlock(position, false, slider);
        }
        return true;
    }

    private static boolean canDestroyMovementBlock(
            ServerLevel level, Slider slider, BlockPos position,
            BlockState blockState) {
        return SliderMechanics.canDestroyMovementBlock(
                blockState.is(SliderBlockTags.FORCE_BREAKABLE),
                blockState.hasBlockEntity(),
                blockState.is(SliderBlockTags.UNBREAKABLE),
                blockState.getDestroySpeed(level, position),
                blockState.canEntityDestroy(level, position, slider));
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
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
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
        state.resetSkillTransient();
        clearTelegraph(slider);
        slider.setMoveDirection(null);
        slider.setTargetPoint(null);
        slider.setMoveDelay(slider.calculateMoveDelay());
        slider.setDeltaMovement(Vec3.ZERO);
        beginReturnToEdge(slider);
    }

    private static void cancelSkill(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        state.resetSkillTransient();
        clearTelegraph(slider);
        stopArenaMovement(slider);
        if (slider.isAwake() && !slider.isDeadOrDying()
                && slider.getDungeon() != null) {
            beginReturnToEdge(slider);
        } else {
            state.movementPhase = SliderMovementPhase.IDLE;
        }
    }

    private static void deactivateArenaMovement(Slider slider) {
        SliderCombatState state = state(slider);
        closeParryWindow(slider);
        state.resetTransient();
        clearTelegraph(slider);
        stopArenaMovement(slider);
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

    private static void synchronizeParryWindow(Slider slider) {
        if (isCurrentAttackParryable(slider)) {
            openParryWindow(slider);
        } else {
            closeParryWindow(slider);
        }
    }

    private static double chainSpeed(Slider slider) {
        return SliderMechanics.explicitMovementSpeed(
                slider.getMaxVelocity(),
                movementMultiplier(slider),
                BossRefactorAetherConfig.SLIDER_MOVEMENT
                    .chainDashSpeedMultiplier.get());
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
                    (float) slider.getX(),
                    (float) slider.getY(),
                    (float) slider.getZ(),
                    (float) horizontal.x,
                    (float) horizontal.z,
                    (float) Math.max(0.0, length),
                    (float) Math.max(0.0, width),
                        (float) Math.max(0.0, radius),
                        progress));
        }
    }

    private static void updateTelegraphProgress(Slider slider, float progress) {
        if (slider instanceof AttackTelegraphAccess access) {
            AttackTelegraph telegraph = access.bossRefactorAether$getAttackTelegraph();
            if (telegraph.shape() != AttackTelegraphShape.NONE) {
                access.bossRefactorAether$setAttackTelegraph(
                        telegraph.withProgress(progress));
            }
        }
    }

            private static void updateContinuousGlideTelegraph(Slider slider, int elapsedTicks) {
            float progress = AttackTelegraph.windupProgress(
                    elapsedTicks,
                    BossRefactorAetherConfig.SLIDER_TIMING.chargeTicks.get());
            if (slider instanceof AttackTelegraphAccess access
                    && access.bossRefactorAether$getAttackTelegraph().shape()
                        == AttackTelegraphShape.CORRIDOR) {
                updateTelegraphProgress(slider, progress);
                return;
            }
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
                progress);
            }

    private static void clearTelegraph(Slider slider) {
        if (slider instanceof AttackTelegraphAccess access) {
            access.bossRefactorAether$setAttackTelegraph(AttackTelegraph.NONE);
        }
    }
}
