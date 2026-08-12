package com.tonywww.bossrefactoraether.slider;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SliderMechanics {
    public static final int MAX_BARRIER_LAYERS = 5;
    public static final int MAX_GLIDE_POWER = 10;
    public static final int PHASE_TWO_MIN_GLIDE_POWER = 2;
    public static final int CHAIN_GLIDE_POWER_COST = 6;
    public static final int PARRY_RECOVERY_TICKS = 60;
    public static final int STUN_TICKS = 100;
    public static final int SHIELD_COOLDOWN_TICKS = 60;
    public static final int CHARGE_TICKS = 30;
    public static final int DASH_TICK_LIMIT = 10;
    public static final int DASH_INTERVAL_TICKS = 4;
    public static final int CONTINUOUS_GLIDE_PREDICTION_TICKS = 8;
    public static final int PERIMETER_CORNER_PAUSE_TICKS = 15;
    public static final int ARENA_MOVEMENT_STALL_TICKS = 20;
    public static final int VERTICAL_ALIGNMENT_RETRY_TICKS = 40;
    public static final int PHASE_ONE_DASHES = 2;
    public static final int PHASE_TWO_DASHES = 3;
    public static final double DASH_DISTANCE_LIMIT = 12.0;
    public static final double DEFAULT_BASE_SPEED_MULTIPLIER = 0.4;
    public static final double DEFAULT_PHASE_TWO_SPEED_MULTIPLIER = 1.2;
    public static final double DEFAULT_GLIDE_POWER_SPEED_PER_LAYER = 0.02;
    public static final double DEFAULT_VERTICAL_ALIGN_SPEED_MULTIPLIER = 0.65;
    public static final double DEFAULT_EDGE_RETURN_SPEED_MULTIPLIER = 0.75;
    public static final double DEFAULT_PERIMETER_PATROL_SPEED_MULTIPLIER = 0.5;
    public static final double DEFAULT_CHASE_SPEED_MULTIPLIER = 1.0;
    public static final double DEFAULT_CHAIN_SPEED_MULTIPLIER = 1.6;
    public static final double STANDALONE_ROOM_HORIZONTAL_RADIUS = 11.0;
    public static final double STANDALONE_ROOM_VERTICAL_RADIUS = 7.0;
    public static final double ROOM_INTERIOR_INSET = 1.0;
    public static final double ARENA_RADIUS_REDUCTION = 3.0;
    public static final double PERIMETER_EDGE_CLEARANCE = 0.35;
    public static final double PERIMETER_ARRIVAL_TOLERANCE = 0.12;
    public static final double VERTICAL_ALIGNMENT_TOLERANCE = 0.1;
    public static final double MOVEMENT_PROGRESS_EPSILON = 1.0E-4;
    public static final double PATROL_HIT_INFLATION = 0.1;
    public static final double DASH_HIT_INFLATION = 0.25;
    public static final double CONTINUOUS_GLIDE_MAX_LEAD_DISTANCE = 4.0;
    public static final double DEFAULT_ATTACK_DAMAGE = 6.0;
    public static final double DEFAULT_NORMAL_COLLISION_BASE_DAMAGE = 2.0;
    public static final double DEFAULT_NORMAL_COLLISION_ATTACK_DAMAGE_MULTIPLIER = 1.0;
    public static final double DEFAULT_CHAIN_DASH_BASE_DAMAGE = 0.0;
    public static final double DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER = 1.5;
    public static final double PHASE_TWO_MULTIPLIER = 1.2;
    public static final double GLIDE_POWER_PER_LAYER = 0.02;
    public static final double BARRIER_REDUCTION_PER_LAYER = 0.05;
    public static final float FULL_ATTACK_STRENGTH = 1.0F;

    private SliderMechanics() {
    }

    public static int clampBarrierLayers(int layers) {
        return clampBarrierLayers(layers, MAX_BARRIER_LAYERS);
    }

    public static int clampBarrierLayers(int layers, int maximum) {
        return Math.max(0, Math.min(Math.max(0, maximum), layers));
    }

    public static int clampGlidePower(int layers) {
        return clampGlidePower(layers, MAX_GLIDE_POWER);
    }

    public static int clampGlidePower(int layers, int maximum) {
        return Math.max(0, Math.min(Math.max(0, maximum), layers));
    }

    public static int minimumGlidePower(boolean phaseTwo, boolean stunned) {
        return phaseTwo && !stunned ? PHASE_TWO_MIN_GLIDE_POWER : 0;
    }

    public static int glidePowerAfterChainCost(int glidePower, boolean phaseTwo) {
        return glidePowerAfterChainCost(glidePower, phaseTwo, MAX_GLIDE_POWER,
            CHAIN_GLIDE_POWER_COST, PHASE_TWO_MIN_GLIDE_POWER);
    }

    public static int glidePowerAfterChainCost(
            int glidePower, boolean phaseTwo,
            int maximumGlidePower, int cost,
            int phaseTwoMinimum) {
        return Math.max(
            phaseTwo ? Math.max(0, phaseTwoMinimum) : 0,
            clampGlidePower(glidePower, maximumGlidePower) - Math.max(0, cost));
    }

            public static int effectiveGlidePowerCost(
                int maximumGlidePower, int configuredCost) {
            return Math.min(
                Math.max(1, maximumGlidePower),
                Math.max(1, configuredCost));
            }

            public static boolean hasGlidePowerForSkill(
                int glidePower, int maximumGlidePower, int configuredCost) {
            return clampGlidePower(glidePower, maximumGlidePower)
                >= effectiveGlidePowerCost(maximumGlidePower, configuredCost);
            }

    public static boolean isFullyChargedAttack(float attackStrength) {
        return isFullyChargedAttack(attackStrength, FULL_ATTACK_STRENGTH);
    }

    public static boolean isFullyChargedAttack(float attackStrength, double threshold) {
        return attackStrength >= Mth.clamp(threshold, 0.0, 1.0);
    }

    public static boolean isUnblockableChainDash(boolean phaseTwo, int completedDashes) {
        return phaseTwo && completedDashes == 0;
    }

    public static double combatMultiplier(boolean phaseTwo, int glidePower) {
        return combatMultiplier(phaseTwo, glidePower, MAX_GLIDE_POWER,
            PHASE_TWO_MULTIPLIER, GLIDE_POWER_PER_LAYER);
    }

    public static double combatMultiplier(
            boolean phaseTwo, int glidePower,
            int maximumGlidePower,
            double phaseTwoMultiplier,
            double glidePowerPerLayer) {
        double phaseMultiplier = phaseTwo ? Math.max(0.0, phaseTwoMultiplier) : 1.0;
        return phaseMultiplier * (1.0 + Math.max(0.0, glidePowerPerLayer)
            * clampGlidePower(glidePower, maximumGlidePower));
    }

    public static double speedMultiplier(boolean phaseTwo, int glidePower,
                                         double baseMultiplier,
                                         double phaseTwoMultiplier,
                                         double glidePowerPerLayer) {
        return speedMultiplier(phaseTwo, glidePower, MAX_GLIDE_POWER,
            baseMultiplier, phaseTwoMultiplier, glidePowerPerLayer);
    }

    public static double speedMultiplier(
            boolean phaseTwo, int glidePower,
            int maximumGlidePower,
            double baseMultiplier,
            double phaseTwoMultiplier,
            double glidePowerPerLayer) {
        double phaseMultiplier = phaseTwo ? Math.max(0.0, phaseTwoMultiplier) : 1.0;
        return Math.max(0.0, baseMultiplier)
                * phaseMultiplier
                * (1.0 + Math.max(0.0, glidePowerPerLayer)
            * clampGlidePower(glidePower, maximumGlidePower));
    }

    public static float configuredDamage(double baseDamage, double attackDamage,
                                         double attackDamageMultiplier,
                                         boolean phaseTwo, int glidePower) {
        double composedDamage = baseDamage + attackDamage * attackDamageMultiplier;
        return (float) (composedDamage * combatMultiplier(phaseTwo, glidePower));
    }

    public static float configuredDamage(double baseDamage, double attackDamage,
                                         double attackDamageMultiplier,
                                         boolean phaseTwo, int glidePower,
                                         int maximumGlidePower,
                                         double phaseTwoMultiplier,
                                         double glidePowerPerLayer) {
        double composedDamage = baseDamage + attackDamage * attackDamageMultiplier;
        return (float) (composedDamage * combatMultiplier(
                phaseTwo, glidePower, maximumGlidePower,
                phaseTwoMultiplier, glidePowerPerLayer));
    }

    public static double normalSpeed(double baseSpeed, boolean phaseTwo, int glidePower) {
        return baseSpeed * speedMultiplier(
                phaseTwo,
                glidePower,
                DEFAULT_BASE_SPEED_MULTIPLIER,
                DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                DEFAULT_GLIDE_POWER_SPEED_PER_LAYER);
    }

    public static double chainSpeed(double baseSpeed, boolean phaseTwo, int glidePower) {
        return normalSpeed(baseSpeed, phaseTwo, glidePower)
                * DEFAULT_CHAIN_SPEED_MULTIPLIER;
    }

    public static double nextDashStep(double speed, double distanceTraveled) {
        return nextDashStep(speed, distanceTraveled, DASH_DISTANCE_LIMIT);
    }

    public static double nextDashStep(double speed, double distanceTraveled,
                                      double distanceLimit) {
        double remaining = Math.max(0.0, Math.max(0.0, distanceLimit) - distanceTraveled);
        return Math.min(Math.max(0.0, speed), remaining);
    }

    public static double maximumDashReach(
            double distanceLimit, double speed, int tickLimit) {
        return Math.min(
                Math.max(0.0, distanceLimit),
                Math.max(0.0, speed) * Math.max(1, tickLimit));
    }

    public static boolean isDashExecutionStateValid(
            Vec3 direction, double distanceLimit) {
        return direction.lengthSqr()
                    > MOVEMENT_PROGRESS_EPSILON * MOVEMENT_PROGRESS_EPSILON
                && distanceLimit > MOVEMENT_PROGRESS_EPSILON;
    }

    public static boolean isDashIntervalStateValid(
            int completedDashes, int totalDashes,
            boolean hasLockedTelegraph) {
        return completedDashes >= 0
                && completedDashes < totalDashes
                && hasLockedTelegraph;
    }

    public static boolean shouldResetEmptyBossRoom(
            boolean bossFight, boolean dungeonPlayersEmpty) {
        return bossFight && dungeonPlayersEmpty;
    }

    public static boolean shouldTakeOverOriginalMovement(boolean hasArena) {
        return hasArena;
    }

    public static boolean shouldDiscardExternalMovementOnAwaken(
            boolean wasAwake, boolean awake, boolean hasArena) {
        return !wasAwake && awake && hasArena;
    }

    public static boolean isDamageAllowedFromArena(
            boolean protectionEnabled, boolean sameLevel,
            AABB roomBounds, Vec3 sourcePosition) {
        return !protectionEnabled
                || roomBounds == null
                || sameLevel && roomBounds.contains(sourcePosition);
    }

    public static double explicitMovementSpeed(
            double originalMaxVelocity, double movementMultiplier,
            double actionMultiplier) {
        return Math.max(0.0, originalMaxVelocity)
            * Math.max(0.0, movementMultiplier)
            * Math.max(0.0, actionMultiplier);
    }

            public static AABB standaloneRoomBounds(Vec3 center) {
            return new AABB(
                center.x - STANDALONE_ROOM_HORIZONTAL_RADIUS,
                center.y - STANDALONE_ROOM_VERTICAL_RADIUS,
                center.z - STANDALONE_ROOM_HORIZONTAL_RADIUS,
                center.x + STANDALONE_ROOM_HORIZONTAL_RADIUS,
                center.y + STANDALONE_ROOM_VERTICAL_RADIUS,
                center.z + STANDALONE_ROOM_HORIZONTAL_RADIUS);
            }

    public static Direction.Axis chooseAttackAxis(double deltaX, double deltaZ) {
        return Math.abs(deltaX) >= Math.abs(deltaZ)
                ? Direction.Axis.X
                : Direction.Axis.Z;
    }

    public static Direction.Axis chooseReachableAttackAxis(
            double deltaX, double deltaZ,
            double maximumDistance, double laneHalfWidth) {
        return chooseReachableAttackAxis(
            deltaX, deltaZ, maximumDistance, maximumDistance, laneHalfWidth);
        }

        public static Direction.Axis chooseReachableAttackAxis(
            double deltaX, double deltaZ,
            double maximumXDistance, double maximumZDistance,
            double laneHalfWidth) {
        double xDistance = Math.max(0.0, maximumXDistance);
        double zDistance = Math.max(0.0, maximumZDistance);
        double width = Math.max(0.0, laneHalfWidth);
        boolean xAxisReachable = Math.abs(deltaX) <= xDistance
                && Math.abs(deltaZ) <= width;
        boolean zAxisReachable = Math.abs(deltaZ) <= zDistance
                && Math.abs(deltaX) <= width;
        if (xAxisReachable != zAxisReachable) {
            return xAxisReachable ? Direction.Axis.X : Direction.Axis.Z;
        }
        return chooseAttackAxis(deltaX, deltaZ);
    }

    public static boolean isAxisDashReachable(
            Direction.Axis axis, double deltaX, double deltaZ,
            double maximumXDistance, double maximumZDistance,
            double laneHalfWidth) {
        double width = Math.max(0.0, laneHalfWidth);
        return axis == Direction.Axis.X
                ? Math.abs(deltaX) <= Math.max(0.0, maximumXDistance)
                    && Math.abs(deltaZ) <= width
                : Math.abs(deltaZ) <= Math.max(0.0, maximumZDistance)
                    && Math.abs(deltaX) <= width;
    }

    public static Vec3 predictHorizontalTarget(
            Vec3 position, Vec3 movement,
            int predictionTicks, double maximumLeadDistance) {
        Vec3 lead = new Vec3(movement.x, 0.0, movement.z)
                .scale(Math.max(0, predictionTicks));
        double maximumLead = Math.max(0.0, maximumLeadDistance);
        if (lead.lengthSqr() > maximumLead * maximumLead
                && lead.lengthSqr() > 1.0E-12) {
            lead = lead.normalize().scale(maximumLead);
        }
        return position.add(lead);
    }

    public static Vec3 axisMotion(Direction.Axis axis, double signedDistance) {
        return axis == Direction.Axis.X
                ? new Vec3(signedDistance, 0.0, 0.0)
                : new Vec3(0.0, 0.0, signedDistance);
    }

    public static double stepToward(double current, double target, double maximumStep) {
        return Mth.clamp(target - current, -Math.abs(maximumStep), Math.abs(maximumStep));
    }

    public static boolean hasVerticalAttackOverlap(
            double attackerMinY, double attackerMaxY,
            double targetMinY, double targetMaxY,
            double tolerance) {
        double padding = Math.max(0.0, tolerance) + 1.0E-9;
        return attackerMaxY + padding >= targetMinY
                && targetMaxY + padding >= attackerMinY;
    }

    public static double verticalAttackAlignmentStep(
            double attackerMinY, double attackerMaxY,
            double targetMinY, double targetMaxY,
            double tolerance, double maximumStep) {
        double padding = Math.max(0.0, tolerance);
        if (attackerMaxY + padding < targetMinY) {
            return Math.min(
                    Math.abs(maximumStep),
                    targetMinY - attackerMaxY - padding);
        }
        if (targetMaxY + padding < attackerMinY) {
            return -Math.min(
                    Math.abs(maximumStep),
                    attackerMinY - targetMaxY - padding);
        }
        return 0.0;
    }

    public static double insetMinimum(double minimum, double maximum, double inset) {
        double lower = Math.min(minimum, maximum);
        double upper = Math.max(minimum, maximum);
        return Math.min(lower + Math.max(0.0, inset), (lower + upper) * 0.5);
    }

    public static double perimeterInset(double entityWidth, double extraClearance) {
        double collisionSafeInset = ROOM_INTERIOR_INSET
                + Math.max(0.0, entityWidth) * 0.5
                + Math.max(0.0, extraClearance);
        return Math.max(
            Math.max(0.0, ARENA_RADIUS_REDUCTION),
            collisionSafeInset);
    }

    public static double insetMaximum(double minimum, double maximum, double inset) {
        double lower = Math.min(minimum, maximum);
        double upper = Math.max(minimum, maximum);
        return Math.max(upper - Math.max(0.0, inset), (lower + upper) * 0.5);
    }

    public static Direction nearestPerimeterEdge(
            double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        Direction nearest = Direction.NORTH;
        double nearestDistance = distanceToPerimeterEdgeSquared(
                nearest, x, z, minimumX, maximumX, minimumZ, maximumZ);
        for (Direction candidate : new Direction[] {
                Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            double candidateDistance = distanceToPerimeterEdgeSquared(
                    candidate, x, z, minimumX, maximumX, minimumZ, maximumZ);
            if (candidateDistance < nearestDistance) {
                nearest = candidate;
                nearestDistance = candidateDistance;
            }
        }
        return nearest;
    }

    public static Vec3 closestPointOnPerimeterEdge(
            Direction edge, double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        double clampedX = Mth.clamp(x, minimumX, maximumX);
        double clampedZ = Mth.clamp(z, minimumZ, maximumZ);
        return switch (edge) {
            case NORTH -> new Vec3(clampedX, 0.0, minimumZ);
            case EAST -> new Vec3(maximumX, 0.0, clampedZ);
            case SOUTH -> new Vec3(clampedX, 0.0, maximumZ);
            case WEST -> new Vec3(minimumX, 0.0, clampedZ);
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Vec3 patrolCorner(
            Direction edge, boolean clockwise,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        return switch (edge) {
            case NORTH -> new Vec3(clockwise ? maximumX : minimumX, 0.0, minimumZ);
            case EAST -> new Vec3(maximumX, 0.0, clockwise ? maximumZ : minimumZ);
            case SOUTH -> new Vec3(clockwise ? minimumX : maximumX, 0.0, maximumZ);
            case WEST -> new Vec3(minimumX, 0.0, clockwise ? minimumZ : maximumZ);
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Direction nextPerimeterEdge(Direction edge, boolean clockwise) {
        return switch (edge) {
            case NORTH -> clockwise ? Direction.EAST : Direction.WEST;
            case EAST -> clockwise ? Direction.SOUTH : Direction.NORTH;
            case SOUTH -> clockwise ? Direction.WEST : Direction.EAST;
            case WEST -> clockwise ? Direction.NORTH : Direction.SOUTH;
            default -> throw new IllegalArgumentException("Perimeter edge must be horizontal");
        };
    }

    public static Vec3 horizontalStepToward(
            double x, double z, double targetX, double targetZ, double maximumStep) {
        Vec3 offset = new Vec3(targetX - x, 0.0, targetZ - z);
        double distance = offset.length();
        if (distance <= Math.max(0.0, maximumStep)) {
            return offset;
        }
        return distance < 1.0E-8
                ? Vec3.ZERO
                : offset.scale(Math.max(0.0, maximumStep) / distance);
    }

    public static Direction chooseChaseDirection(Vec3 current, Vec3 target) {
        double deltaX = target.x - current.x;
        double deltaY = target.y - current.y;
        double deltaZ = target.z - current.z;
        double absoluteX = Math.abs(deltaX);
        double absoluteY = Math.abs(deltaY);
        double absoluteZ = Math.abs(deltaZ);
        if (absoluteY > absoluteX && absoluteY > absoluteZ) {
            return deltaY > 0.0 ? Direction.UP : Direction.DOWN;
        }
        if (absoluteX > absoluteZ) {
            return deltaX > 0.0 ? Direction.EAST : Direction.WEST;
        }
        return deltaZ > 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static double chaseAxisDistance(
            Vec3 current, Vec3 target, Direction direction) {
        Vec3 offset = target.subtract(current);
        return offset.x * direction.getStepX()
                + offset.y * direction.getStepY()
                + offset.z * direction.getStepZ();
    }

    public static double nextChaseVelocity(
            double currentVelocity, double maximumVelocity,
            double velocityIncrease) {
        return Math.min(
                Math.max(0.0, maximumVelocity),
                Math.max(0.0, currentVelocity)
                    + Math.max(0.0, velocityIncrease));
    }

    public static Vec3 directionMotion(Direction direction, double distance) {
        double step = Math.max(0.0, distance);
        return new Vec3(
                direction.getStepX() * step,
                direction.getStepY() * step,
                direction.getStepZ() * step);
    }

    public static Vec3 clampToBounds(Vec3 position, AABB bounds) {
        return new Vec3(
                Mth.clamp(position.x, bounds.minX, bounds.maxX),
                Mth.clamp(position.y, bounds.minY, bounds.maxY),
                Mth.clamp(position.z, bounds.minZ, bounds.maxZ));
    }

    public static long perimeterCornerPauseEnd(long gameTime, int pauseTicks) {
        return gameTime + Math.max(0, pauseTicks);
    }

    public static boolean isPerimeterCornerPauseActive(
            long gameTime, long resumeGameTime) {
        return gameTime < resumeGameTime;
    }

            public static double boundedAxisDashDistance(
                double start, double direction,
                double minimum, double maximum,
                double requestedDistance) {
            double lower = Math.min(minimum, maximum);
            double upper = Math.max(minimum, maximum);
            double distanceToBoundary = direction >= 0.0
                ? upper - start
                : start - lower;
            return Math.min(
                Math.max(0.0, requestedDistance),
                Math.max(0.0, distanceToBoundary));
            }

    public static boolean hasMovementProgress(Vec3 previous, Vec3 current) {
        return previous.distanceToSqr(current)
                >= MOVEMENT_PROGRESS_EPSILON * MOVEMENT_PROGRESS_EPSILON;
    }

    public static AABB actualMovementSweep(
            AABB currentBounds, Vec3 previousPosition,
            Vec3 currentPosition, double inflation) {
        Vec3 actualMovement = currentPosition.subtract(previousPosition);
        return currentBounds.expandTowards(actualMovement.scale(-1.0))
                .inflate(Math.max(0.0, inflation));
    }

    public static boolean canHitWithAxisDash(
            double sliderX, double sliderZ,
            double targetX, double targetZ,
            double maximumDistance, double laneHalfWidth) {
        return canHitWithAxisDash(
                sliderX, sliderZ, targetX, targetZ,
                maximumDistance, maximumDistance, laneHalfWidth);
    }

    public static boolean canHitWithAxisDash(
            double sliderX, double sliderZ,
            double targetX, double targetZ,
            double maximumXDistance, double maximumZDistance,
            double laneHalfWidth) {
        double deltaX = Math.abs(targetX - sliderX);
        double deltaZ = Math.abs(targetZ - sliderZ);
        double xDistance = Math.max(0.0, maximumXDistance);
        double zDistance = Math.max(0.0, maximumZDistance);
        double width = Math.max(0.0, laneHalfWidth);
        Direction.Axis axis = chooseReachableAttackAxis(
            deltaX, deltaZ, xDistance, zDistance, width);
        return axis == Direction.Axis.X
            ? deltaX <= xDistance && deltaZ <= width
            : deltaZ <= zDistance && deltaX <= width;
    }

    public static double firstAxisDashIntersection(
            double startX, double startZ,
            double endX, double endZ,
            double targetX, double targetZ,
            double maximumDistance, double laneHalfWidth) {
        double distance = Math.max(0.0, maximumDistance);
        double width = Math.max(0.0, laneHalfWidth);
        double xDashIntersection = firstSegmentRectangleIntersection(
                startX, startZ, endX, endZ,
                targetX - distance, targetX + distance,
                targetZ - width, targetZ + width);
        double zDashIntersection = firstSegmentRectangleIntersection(
                startX, startZ, endX, endZ,
                targetX - width, targetX + width,
                targetZ - distance, targetZ + distance);
        if (Double.isNaN(xDashIntersection)) {
            return zDashIntersection;
        }
        if (Double.isNaN(zDashIntersection)) {
            return xDashIntersection;
        }
        return Math.min(xDashIntersection, zDashIntersection);
    }

    private static double firstSegmentRectangleIntersection(
            double startX, double startZ,
            double endX, double endZ,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        double entry = 0.0;
        double exit = 1.0;

        if (Math.abs(deltaX) < 1.0E-12) {
            if (startX < minimumX || startX > maximumX) {
                return Double.NaN;
            }
        } else {
            double first = (minimumX - startX) / deltaX;
            double second = (maximumX - startX) / deltaX;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
        }

        if (Math.abs(deltaZ) < 1.0E-12) {
            if (startZ < minimumZ || startZ > maximumZ) {
                return Double.NaN;
            }
        } else {
            double first = (minimumZ - startZ) / deltaZ;
            double second = (maximumZ - startZ) / deltaZ;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
        }
        return entry <= exit && exit >= 0.0 && entry <= 1.0
                ? Mth.clamp(entry, 0.0, 1.0)
                : Double.NaN;
    }

    private static double distanceToPerimeterEdgeSquared(
            Direction edge, double x, double z,
            double minimumX, double maximumX,
            double minimumZ, double maximumZ) {
        Vec3 point = closestPointOnPerimeterEdge(
                edge, x, z, minimumX, maximumX, minimumZ, maximumZ);
        double deltaX = point.x - x;
        double deltaZ = point.z - z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    public static float barrierDamageMultiplier(int barrierLayers) {
        return barrierDamageMultiplier(barrierLayers, MAX_BARRIER_LAYERS,
            BARRIER_REDUCTION_PER_LAYER);
    }

    public static float barrierDamageMultiplier(
            int barrierLayers, int maximumBarrierLayers,
            double reductionPerLayer) {
        return (float) Math.max(0.0, 1.0 - Math.max(0.0, reductionPerLayer)
            * clampBarrierLayers(barrierLayers, maximumBarrierLayers));
    }
}