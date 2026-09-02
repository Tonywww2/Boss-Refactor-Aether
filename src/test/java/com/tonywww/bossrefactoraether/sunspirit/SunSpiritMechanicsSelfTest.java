package com.tonywww.bossrefactoraether.sunspirit;

import com.tonywww.bossrefactoraether.mixin.SunSpiritMixin;
import com.tonywww.bossrefactoraether.telegraph.AttackTelegraphAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class SunSpiritMechanicsSelfTest {
    private SunSpiritMechanicsSelfTest() {
    }

    public static void main(String[] args) {
        check(SunSpiritMechanics.crossedHealthThresholds(451.0F, 500.0F, 0.10) == 0,
                "less than ten percent lost must not summon a minion");
        check(SunSpiritMechanics.crossedHealthThresholds(450.0F, 500.0F, 0.10) == 1,
                "ten percent lost must cross the first summon threshold");
        check(SunSpiritMechanics.crossedHealthThresholds(349.0F, 500.0F, 0.10) == 3,
                "large hits must preserve every crossed summon threshold");
        check(!SunSpiritMechanics.isPhaseTwo(250.0F, 500.0F, 0.50),
                "half health must remain phase one");
        check(SunSpiritMechanics.isPhaseTwo(249.9F, 500.0F, 0.50),
                "health below half must enter phase two");
        check(SunSpiritMechanics.attackForIndex(0) == SunSpiritAttack.PROJECTILE,
                "the first attack must be a projectile");
        check(SunSpiritMechanics.attackForIndex(1) == SunSpiritAttack.RISING_FLAME,
                "the second attack must be Rising Flame");
        check(SunSpiritMechanics.attackForIndex(2) == SunSpiritAttack.TITAN_FIST,
                "the third attack must be Titan Fist");
        check(!SunSpiritMechanics.shouldUseBaseInvulnerability(false),
                "dialogue must retain the Sun Spirit's original invulnerability");
        check(SunSpiritMechanics.shouldUseBaseInvulnerability(true),
                "combat must release the Sun Spirit's special invulnerability");
        verifyParryBreakPhases();
        checkClose(SunSpiritMechanics.phaseDamageMultiplier(true, 1.2), 1.2,
                "phase two must apply its damage multiplier");
        checkClose(SunSpiritMechanics.minionProtectedDamage(20.0F, true, 0.5), 10.0,
                "an owned minion must reduce incoming damage by fifty percent");
        checkClose(SunSpiritMechanics.minionProtectedDamage(20.0F, false, 0.5), 20.0,
                "damage must remain unchanged without an owned minion");
        checkClose(SunSpiritMechanics.reflectedIceDamage(500.0F, 0.05), 25.0,
                "reflected ice must remove five percent maximum health");
        check(SunSpiritMechanics.isSlashBladeIdentifier("slashblade"),
                "base SlashBlade attacks must be recognized");
        check(SunSpiritMechanics.isSlashBladeIdentifier("slashblade_sendims"),
                "SenDimS SlashBlade attacks must be recognized");
        check(!SunSpiritMechanics.isSlashBladeIdentifier("minecraft"),
                "ordinary attacks must remain able to block projectiles");

        Vec3 origin = Vec3.ZERO;
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        check(SunSpiritMechanics.isInsideForwardRectangle(
                        origin, forward, new Vec3(1.9, -8.0, 6.0), 8.0, 2.0),
                "Titan Fist must hit inside its horizontal rectangle despite height");
        check(!SunSpiritMechanics.isInsideForwardRectangle(
                        origin, forward, new Vec3(2.1, 0.0, 6.0), 8.0, 2.0),
                "Titan Fist must reject points outside its width");
        check(!SunSpiritMechanics.isInsideForwardRectangle(
                        origin, forward, new Vec3(0.0, 0.0, -0.1), 8.0, 2.0),
                "Titan Fist must not hit behind the boss");
        check(SunSpiritMechanics.isInsideHorizontalCircle(
                        new Vec3(0.0, 8.0, 0.0), new Vec3(3.0, 0.0, 0.0), 4.0),
                "Rising Flame horizontal radius must ignore vertical separation");
        Vec3 groundOrigin = SunSpiritMechanics.groundTelegraphOrigin(
                new Vec3(4.0, 12.0, 7.0), 2.0);
        checkClose(groundOrigin.x, 4.0,
                "ground telegraphs must retain the boss X coordinate");
        checkClose(groundOrigin.y, 2.0,
                "ground telegraphs must use the arena floor instead of boss altitude");
        checkClose(groundOrigin.z, 7.0,
                "ground telegraphs must retain the boss Z coordinate");
        check(SunSpiritStateAccess.class.isAssignableFrom(SunSpiritMixin.class),
                "SunSpiritMixin must expose combat state access");
        check(AttackTelegraphAccess.class.isAssignableFrom(SunSpiritMixin.class),
                "SunSpiritMixin must expose attack telegraphs");
        verifyFlameSigilCountdown();
        verifyPersistence();
    }

    private static void verifyFlameSigilCountdown() {
        SunSpiritFlameSigil delayed = new SunSpiritFlameSigil(Vec3.ZERO, 2);
        check(!delayed.advanceAndShouldErupt(),
                "a delayed flame sigil must retain its first warning tick");
        check(delayed.remainingTicks == 1,
                "a flame sigil countdown must advance exactly once per tick");
        check(delayed.advanceAndShouldErupt(),
                "a flame sigil must erupt when its configured delay expires");
        check(new SunSpiritFlameSigil(Vec3.ZERO, 0).advanceAndShouldErupt(),
                "a zero-delay flame sigil must erupt immediately");
    }

    private static void verifyParryBreakPhases() {
        check(SunSpiritAttackPhase.RISING_FLAME_WINDUP.isParryBreak(),
                "Rising Flame must open the SenDimS parry-break window");
        check(SunSpiritAttackPhase.TITAN_FIST_WINDUP.isParryBreak(),
                "Titan Fist must open the SenDimS parry-break window");
        check(!SunSpiritAttackPhase.PROJECTILE_WINDUP.isParryBreak(),
                "ordinary projectiles must not be parry-break attacks");
        check(!SunSpiritAttackPhase.SUMMON_WINDUP.isParryBreak(),
                "summoning must not be a parry-break attack");
        check(!SunSpiritAttackPhase.RECOVERY.isParryBreak(),
                "recovery must close the SenDimS parry-break window");
        int parryBreakPhases = 0;
        for (SunSpiritAttackPhase phase : SunSpiritAttackPhase.values()) {
            if (phase.isParryBreak()) {
                parryBreakPhases++;
            }
        }
        check(parryBreakPhases == 2,
                "Sun Spirit must expose exactly two parry-break phases");
    }

    private static void verifyPersistence() {
        SunSpiritCombatState original = new SunSpiritCombatState();
        original.phaseTwo = true;
        original.healthThresholdsSummoned = 4;
        original.attackIndex = 7;
        original.summonReadyAt = 1300L;
        original.phaseSigilReadyAt = 1500L;
        CompoundTag tag = new CompoundTag();
        original.write(tag, 1000L);

        SunSpiritCombatState loaded = new SunSpiritCombatState();
        loaded.read(tag, 2000L);
        check(loaded.phaseTwo, "phase two must survive a save and load");
        check(loaded.healthThresholdsSummoned == 4,
                "health summon thresholds must survive a save and load");
        check(loaded.attackIndex == 7,
                "attack rotation must survive a save and load");
        check(loaded.summonReadyAt == 2300L,
                "summon cooldown must retain its remaining duration");
        check(loaded.phaseSigilReadyAt == 2500L,
                "phase sigil cooldown must retain its remaining duration");

        SunSpiritCombatState missing = new SunSpiritCombatState();
        missing.read(new CompoundTag(), 2000L);
        check(!missing.phaseTwo,
                "a pre-mod Sun Spirit must begin in phase one");
        check(missing.healthThresholdsSummoned == 0 && missing.attackIndex == 0,
                "missing Sun Spirit counters must begin at zero");
        check(missing.summonReadyAt == 0L && missing.phaseSigilReadyAt == 0L,
                "missing Sun Spirit cooldowns must retain first-tick initialization sentinels");

        CompoundTag partial = new CompoundTag();
        partial.putBoolean("bossrefactoraether.sun_spirit.phase_two", true);
        partial.putLong("bossrefactoraether.sun_spirit.summon_cooldown", 60L);
        partial.putString("bossrefactoraether.sun_spirit.phase_sigil_cooldown", "invalid");
        partial.putLong("bossrefactoraether.sun_spirit.health_thresholds", Long.MAX_VALUE);
        partial.putDouble("bossrefactoraether.sun_spirit.attack_index", 3.5);
        SunSpiritCombatState migrated = new SunSpiritCombatState();
        migrated.read(partial, 2000L);
        check(migrated.phaseTwo,
                "a valid legacy Sun Spirit phase must be retained");
        check(migrated.summonReadyAt == 2060L,
                "a valid legacy Sun Spirit cooldown must be retained");
        check(migrated.phaseSigilReadyAt == 0L,
                "a missing or mistyped phase sigil cooldown must be reinitialized");
        check(migrated.healthThresholdsSummoned == 0 && migrated.attackIndex == 0,
                "oversized or fractional Sun Spirit counters must be discarded");

        check(tag.getInt("bossrefactoraether.sun_spirit.data_version") == 1,
                "new Sun Spirit state must carry an explicit data version");
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-6,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}