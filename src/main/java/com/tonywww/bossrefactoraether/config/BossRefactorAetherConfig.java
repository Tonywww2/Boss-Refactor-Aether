package com.tonywww.bossrefactoraether.config;

import com.tonywww.bossrefactoraether.slider.SliderMechanics;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritMechanics;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenMechanics;
import net.minecraftforge.common.ForgeConfigSpec;

public final class BossRefactorAetherConfig {
        private static final double MAX_BASE_DAMAGE = 1_000_000.0;
        private static final double MAX_ATTACK_DAMAGE_MULTIPLIER = 1_000.0;
        private static final double MAX_SPEED_MULTIPLIER = 10.0;
        private static final double MAX_DISTANCE = 128.0;
        private static final int MAX_TICKS = 72_000;

        public static final ForgeConfigSpec COMMON_SPEC;
        public static final SliderCombatConfig SLIDER_COMBAT;
        public static final SliderDamageConfig SLIDER_DAMAGE;
        public static final SliderMovementConfig SLIDER_MOVEMENT;
        public static final SliderTimingConfig SLIDER_TIMING;
        public static final SliderRangeConfig SLIDER_RANGE;
        public static final SliderDisplayConfig SLIDER_DISPLAY;
        public static final ValkyrieQueenCombatConfig VALKYRIE_QUEEN_COMBAT;
        public static final ValkyrieQueenDamageConfig VALKYRIE_QUEEN_DAMAGE;
        public static final ValkyrieQueenTimingConfig VALKYRIE_QUEEN_TIMING;
        public static final ValkyrieQueenRangeConfig VALKYRIE_QUEEN_RANGE;
        public static final SunSpiritCombatConfig SUN_SPIRIT_COMBAT;
        public static final SunSpiritDamageConfig SUN_SPIRIT_DAMAGE;
        public static final SunSpiritTimingConfig SUN_SPIRIT_TIMING;
        public static final SunSpiritRangeConfig SUN_SPIRIT_RANGE;
        public static final AttackTelegraphConfig ATTACK_TELEGRAPH;

        static {
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
                builder.comment("Aether Slider settings.", "滑行魔石设置。")
                                .push("slider");
                SLIDER_COMBAT = new SliderCombatConfig(builder);
                SLIDER_DAMAGE = new SliderDamageConfig(builder);
                SLIDER_MOVEMENT = new SliderMovementConfig(builder);
                SLIDER_TIMING = new SliderTimingConfig(builder);
                SLIDER_RANGE = new SliderRangeConfig(builder);
                SLIDER_DISPLAY = new SliderDisplayConfig(builder);
                builder.pop();

                builder.comment("Valkyrie Queen settings.", "武神女王设置。")
                                .push("valkyrie_queen");
                VALKYRIE_QUEEN_COMBAT = new ValkyrieQueenCombatConfig(builder);
                VALKYRIE_QUEEN_DAMAGE = new ValkyrieQueenDamageConfig(builder);
                VALKYRIE_QUEEN_TIMING = new ValkyrieQueenTimingConfig(builder);
                VALKYRIE_QUEEN_RANGE = new ValkyrieQueenRangeConfig(builder);
                builder.pop();

                builder.comment("Sun Spirit settings.", "烈阳巨灵设置。")
                                .push("sun_spirit");
                SUN_SPIRIT_COMBAT = new SunSpiritCombatConfig(builder);
                SUN_SPIRIT_DAMAGE = new SunSpiritDamageConfig(builder);
                SUN_SPIRIT_TIMING = new SunSpiritTimingConfig(builder);
                SUN_SPIRIT_RANGE = new SunSpiritRangeConfig(builder);
                builder.pop();

                ATTACK_TELEGRAPH = new AttackTelegraphConfig(builder);
                COMMON_SPEC = builder.build();
        }

        private BossRefactorAetherConfig() {
        }

        public static final class SliderCombatConfig {
                public final ForgeConfigSpec.DoubleValue phaseTwoHealthRatio;
                public final ForgeConfigSpec.IntValue maxBarrierLayers;
                public final ForgeConfigSpec.IntValue maxGlidePower;
                public final ForgeConfigSpec.IntValue phaseTwoMinGlidePower;
                public final ForgeConfigSpec.IntValue chainGlidePowerCost;
                public final ForgeConfigSpec.DoubleValue phaseTwoDamageMultiplier;
                public final ForgeConfigSpec.DoubleValue glidePowerDamagePerLayer;
                public final ForgeConfigSpec.DoubleValue barrierReductionPerLayer;
                public final ForgeConfigSpec.IntValue parryRecoveryTicks;
                public final ForgeConfigSpec.IntValue stunTicks;
                public final ForgeConfigSpec.IntValue shieldCooldownTicks;
                public final ForgeConfigSpec.IntValue phaseOneDashes;
                public final ForgeConfigSpec.IntValue phaseTwoDashes;
                public final ForgeConfigSpec.DoubleValue extraDashChance;
                public final ForgeConfigSpec.DoubleValue fullPickaxeAttackStrength;
                public final ForgeConfigSpec.BooleanValue immuneToNegativeEffects;
                public final ForgeConfigSpec.IntValue glidePowerGainPerPatrolEdge;
                public final ForgeConfigSpec.IntValue glidePowerGainOnBarrierBreak;
                public final ForgeConfigSpec.BooleanValue phaseTwoFirstDashUnblockable;

                private SliderCombatConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Core Slider combat rules.", "滑行魔石核心战斗规则。")
                                        .push("combat");
                        phaseTwoHealthRatio = defineDouble(builder, "phase_two_health_ratio", 0.5,
                                        0.0, 1.0, "Phase-two health threshold ratio.", "二阶段生命比例阈值。");
                        maxBarrierLayers = defineInt(builder, "max_barrier_layers",
                                        SliderMechanics.MAX_BARRIER_LAYERS, 0, 100,
                                        "Maximum Stone Barrier layers.", "石质屏障最大层数。");
                        maxGlidePower = defineInt(builder, "max_glide_power",
                                        SliderMechanics.MAX_GLIDE_POWER, 1, 100,
                                        "Maximum Glide Power layers.", "滑行力最大层数。");
                        phaseTwoMinGlidePower = defineInt(builder, "phase_two_min_glide_power",
                                        SliderMechanics.PHASE_TWO_MIN_GLIDE_POWER, 0, 100,
                                        "Minimum phase-two Glide Power outside stun.",
                                        "二阶段非眩晕时的最低滑行力。");
                        chainGlidePowerCost = defineInt(builder, "continuous_glide_cost",
                                        SliderMechanics.CHAIN_GLIDE_POWER_COST, 1, 100,
                                        "Glide Power consumed by Continuous Glide.", "连续滑行消耗的滑行力。");
                        phaseTwoDamageMultiplier = defineDouble(builder, "phase_two_damage_multiplier",
                                        SliderMechanics.PHASE_TWO_MULTIPLIER, 0.0, 100.0,
                                        "Phase-two damage multiplier.", "二阶段伤害倍率。");
                        glidePowerDamagePerLayer = defineDouble(builder,
                                        "glide_power_damage_per_layer", SliderMechanics.GLIDE_POWER_PER_LAYER,
                                        0.0, 10.0, "Damage added by each Glide Power layer.",
                                        "每层滑行力增加的伤害倍率。");
                        barrierReductionPerLayer = defineDouble(builder,
                                        "barrier_reduction_per_layer", SliderMechanics.BARRIER_REDUCTION_PER_LAYER,
                                        0.0, 1.0, "Damage reduction per Stone Barrier layer.",
                                        "每层石质屏障提供的伤害减免。");
                        parryRecoveryTicks = defineInt(builder, "parry_recovery_ticks",
                                        SliderMechanics.PARRY_RECOVERY_TICKS, 1, MAX_TICKS,
                                        "Recovery duration after a successful parry, in ticks.",
                                        "成功招架后的恢复时间，单位为 tick。");
                        stunTicks = defineInt(builder, "stun_ticks", SliderMechanics.STUN_TICKS,
                                        0, MAX_TICKS, "Stun duration after barrier break, in ticks.",
                                        "屏障全部破碎后的眩晕时间，单位为 tick。");
                        shieldCooldownTicks = defineInt(builder, "shield_cooldown_ticks",
                                        SliderMechanics.SHIELD_COOLDOWN_TICKS, 0, MAX_TICKS,
                                        "Shield cooldown after blocking, in ticks.",
                                        "成功格挡后的盾牌冷却，单位为 tick。");
                        phaseOneDashes = defineInt(builder, "phase_one_continuous_glide_dashes",
                                        SliderMechanics.PHASE_ONE_DASHES, 1, 100,
                                        "Phase-one Continuous Glide dash count.", "一阶段连续滑行冲刺次数。");
                        phaseTwoDashes = defineInt(builder, "phase_two_continuous_glide_dashes",
                                        SliderMechanics.PHASE_TWO_DASHES, 1, 100,
                                        "Phase-two Continuous Glide dash count.", "二阶段连续滑行冲刺次数。");
                        extraDashChance = defineDouble(builder, "extra_dash_chance", 0.5,
                                        0.0, 1.0, "Chance for one extra dash.", "额外追加一次冲刺的概率。");
                        fullPickaxeAttackStrength = defineDouble(builder,
                                        "full_pickaxe_attack_strength", SliderMechanics.FULL_ATTACK_STRENGTH,
                                        0.0, 1.0, "Required charged-pickaxe attack strength.",
                                        "满蓄力镐攻击所需的攻击强度。");
                        immuneToNegativeEffects = builder.comment(
                                        "Make the Slider immune to all HARMFUL mob effects and remove existing harmful effects.",
                                        "使滑行魔石免疫全部 HARMFUL 负面效果，并清除已有负面效果。")
                                        .define("immune_to_negative_effects", true);
                        glidePowerGainPerPatrolEdge = defineInt(
                                        builder, "glide_power_gain_per_patrol_edge", 2, 0, 100,
                                        "Glide Power gained after completing one arena edge.",
                                        "每完成一条场地边缘巡航时获得的滑行力。");
                        glidePowerGainOnBarrierBreak = defineInt(builder,
                                        "glide_power_gain_on_barrier_break", 1, 0, 100,
                                        "Glide Power gained when one Stone Barrier layer is removed.",
                                        "失去一层石质屏障时获得的滑行力。");
                        phaseTwoFirstDashUnblockable = builder.comment(
                                        "Make the first phase-two Continuous Glide dash unblockable and unparryable.",
                                        "使二阶段连续滑行的第一次冲刺不可格挡且不可招架。")
                                        .define("phase_two_first_dash_unblockable", true);
                        builder.pop();
                }
        }

        public static final class SliderDamageConfig {
                public final DamageFormula normalCollision;
                public final DamageFormula chainDash;

                private SliderDamageConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment(
                                        "Damage is composed before phase and Glide Power multipliers.",
                                        "先计算基础伤害，再应用阶段和滑行力倍率。")
                                        .push("damage");

                        normalCollision = defineDamageFormula(
                                        builder,
                                        "normal_collision",
                                        SliderMechanics.DEFAULT_NORMAL_COLLISION_BASE_DAMAGE,
                                        SliderMechanics.DEFAULT_NORMAL_COLLISION_ATTACK_DAMAGE_MULTIPLIER,
                                        "Collision during normal perimeter patrol.",
                                        "普通周界巡航期间的碰撞伤害。");
                        chainDash = defineDamageFormula(
                                        builder,
                                        "chain_dash",
                                        SliderMechanics.DEFAULT_CHAIN_DASH_BASE_DAMAGE,
                                        SliderMechanics.DEFAULT_CHAIN_DASH_ATTACK_DAMAGE_MULTIPLIER,
                                        "Continuous Glide dash.", "连续滑行冲刺。");

                        builder.pop();
                }
        }

        public static final class DamageFormula {
                public final ForgeConfigSpec.DoubleValue baseDamage;
                public final ForgeConfigSpec.DoubleValue attackDamageMultiplier;

                private DamageFormula(ForgeConfigSpec.DoubleValue baseDamage,
                                ForgeConfigSpec.DoubleValue attackDamageMultiplier) {
                        this.baseDamage = baseDamage;
                        this.attackDamageMultiplier = attackDamageMultiplier;
                }
        }

        public static final class SliderMovementConfig {
                public final ForgeConfigSpec.DoubleValue baseSpeedMultiplier;
                public final ForgeConfigSpec.DoubleValue phaseTwoSpeedMultiplier;
                public final ForgeConfigSpec.DoubleValue glidePowerSpeedPerLayer;
                public final ForgeConfigSpec.DoubleValue verticalAlignmentSpeedMultiplier;
                public final ForgeConfigSpec.DoubleValue edgeReturnSpeedMultiplier;
                public final ForgeConfigSpec.DoubleValue perimeterPatrolSpeedMultiplier;
                public final ForgeConfigSpec.DoubleValue chainDashSpeedMultiplier;

                private SliderMovementConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment(
                                        "Movement speed settings for the Aether Slider.",
                                        "滑行魔石移动速度设置。",
                                        "Base speed is relative to the original Aether maximum speed.",
                                        "基础速度相对于天境原始最大速度。",
                                        "Phase, Glide Power, and action multipliers are applied afterward.",
                                        "之后应用阶段、滑行力和动作倍率。")
                                        .push("movement");

                        baseSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "base_speed_multiplier",
                                        SliderMechanics.DEFAULT_BASE_SPEED_MULTIPLIER,
                                        "Overall multiplier before phase and Glide Power.",
                                        "阶段和滑行力之前应用的总倍率。");
                        phaseTwoSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "phase_two_speed_multiplier",
                                        SliderMechanics.DEFAULT_PHASE_TWO_SPEED_MULTIPLIER,
                                        "Speed multiplier during phase two.", "二阶段速度倍率。");
                        glidePowerSpeedPerLayer = builder
                                        .comment("Speed added by each Glide Power layer.",
                                                        "每层滑行力增加的速度倍率。")
                                        .defineInRange(
                                                        "glide_power_speed_per_layer",
                                                        SliderMechanics.DEFAULT_GLIDE_POWER_SPEED_PER_LAYER,
                                                        0.0, 1.0);
                        verticalAlignmentSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "vertical_alignment_speed_multiplier",
                                        SliderMechanics.DEFAULT_VERTICAL_ALIGN_SPEED_MULTIPLIER,
                                        "Multiplier while moving vertically into attack height.",
                                        "纵向移动到可攻击高度时的速度倍率。");
                        edgeReturnSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "edge_return_speed_multiplier",
                                        SliderMechanics.DEFAULT_EDGE_RETURN_SPEED_MULTIPLIER,
                                        "Multiplier while moving to the nearest arena edge.",
                                        "移动到最近场地边缘时的速度倍率。");
                        perimeterPatrolSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "perimeter_patrol_speed_multiplier",
                                        SliderMechanics.DEFAULT_PERIMETER_PATROL_SPEED_MULTIPLIER,
                                        "Multiplier while patrolling the square arena perimeter.",
                                        "沿方形场地边缘巡航时的速度倍率。");
                        chainDashSpeedMultiplier = defineSpeedMultiplier(
                                        builder,
                                        "chain_dash_speed_multiplier",
                                        SliderMechanics.DEFAULT_CHAIN_SPEED_MULTIPLIER,
                                        "Multiplier for Continuous Glide dashes.", "连续滑行冲刺速度倍率。");

                        builder.pop();
                }
        }

        public static final class SliderTimingConfig {
                public final ForgeConfigSpec.IntValue chargeTicks;
                public final ForgeConfigSpec.IntValue dashTickLimit;
                public final ForgeConfigSpec.IntValue dashIntervalTicks;
                public final ForgeConfigSpec.IntValue perimeterCornerPauseTicks;

                private SliderTimingConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Slider timing in ticks.", "滑行魔石时间参数，单位为 tick。")
                                        .push("timing");
                        chargeTicks = defineInt(builder, "continuous_glide_windup_ticks",
                                        SliderMechanics.CHARGE_TICKS, 0, MAX_TICKS,
                                        "Continuous Glide windup.", "连续滑行前摇。");
                        dashTickLimit = defineInt(builder, "continuous_glide_dash_tick_limit",
                                        SliderMechanics.DASH_TICK_LIMIT, 1, MAX_TICKS,
                                        "Maximum duration of each dash.", "每次连续冲刺的最大持续时间。");
                        dashIntervalTicks = defineInt(builder, "continuous_glide_interval_ticks",
                                        SliderMechanics.DASH_INTERVAL_TICKS, 0, MAX_TICKS,
                                        "Pause between dashes.", "连续冲刺之间的停顿时间。");
                        perimeterCornerPauseTicks = defineInt(
                                        builder, "perimeter_corner_pause_ticks",
                                        SliderMechanics.PERIMETER_CORNER_PAUSE_TICKS, 0, MAX_TICKS,
                                        "Pause after normal perimeter movement reaches a corner.",
                                        "普通周界移动抵达角点后的停留时间。");
                        builder.pop();
                }
        }

        public static final class SliderRangeConfig {
                public final ForgeConfigSpec.DoubleValue continuousGlideDistance;
                public final ForgeConfigSpec.DoubleValue perimeterEdgeClearance;
                public final ForgeConfigSpec.DoubleValue perimeterArrivalTolerance;
                public final ForgeConfigSpec.DoubleValue verticalAlignmentTolerance;

                private SliderRangeConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Slider range and geometry.", "滑行魔石范围与几何参数。")
                                        .push("range");
                        continuousGlideDistance = defineDistance(builder, "continuous_glide_distance",
                                        SliderMechanics.DASH_DISTANCE_LIMIT, "Maximum dash distance.",
                                        "连续滑行单次冲刺最大距离。");
                        perimeterEdgeClearance = defineDistance(builder, "perimeter_edge_clearance",
                                        SliderMechanics.PERIMETER_EDGE_CLEARANCE,
                                        "Extra clearance between the Slider and arena walls.",
                                        "魔石与场地墙体之间的额外间距。");
                        perimeterArrivalTolerance = defineDistance(
                                        builder, "perimeter_arrival_tolerance",
                                        SliderMechanics.PERIMETER_ARRIVAL_TOLERANCE,
                                        "Distance used to accept an edge or corner as reached.",
                                        "判定到达场地边缘或角点的距离。");
                        verticalAlignmentTolerance = defineDistance(
                                        builder, "vertical_alignment_tolerance",
                                        SliderMechanics.VERTICAL_ALIGNMENT_TOLERANCE,
                                        "Allowed center-height difference after vertical alignment.",
                                        "纵向对齐完成后允许的中心高度差。");
                        builder.pop();
                }
        }

        public static final class SliderDisplayConfig {
                public final ForgeConfigSpec.DoubleValue statusLabelHeightOffset;

                private SliderDisplayConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Slider client display settings.", "滑行魔石客户端显示设置。")
                                        .push("display");
                        statusLabelHeightOffset = defineDouble(builder,
                                        "status_label_height_offset", 1.35, 0.0, 8.0,
                                        "Status-label height above the Slider's bounding box.",
                                        "状态标签高于魔石碰撞箱顶部的高度。");
                        builder.pop();
                }
        }

        public static final class ValkyrieQueenCombatConfig {
                public final ForgeConfigSpec.DoubleValue phaseTwoHealthRatio;
                public final ForgeConfigSpec.DoubleValue reactiveTeleportChance;
                public final ForgeConfigSpec.IntValue reactiveTeleportCooldownTicks;
                public final ForgeConfigSpec.DoubleValue phaseTwoKnockbackResistance;
                public final ForgeConfigSpec.IntValue basicsBeforeSkill;
                public final ForgeConfigSpec.IntValue basicsBeforeSpear;
                public final ForgeConfigSpec.IntValue initialSkillDelayTicks;
                public final ForgeConfigSpec.IntValue initialSpearDelayTicks;
                public final ForgeConfigSpec.IntValue skillCooldownTicks;
                public final ForgeConfigSpec.IntValue spearCooldownTicks;
                public final ForgeConfigSpec.DoubleValue skillOneChainChance;
                public final ForgeConfigSpec.BooleanValue tacticalFlankingEnabled;
                public final ForgeConfigSpec.DoubleValue pursuitRange;
                public final ForgeConfigSpec.DoubleValue approachSpeed;
                public final ForgeConfigSpec.DoubleValue flankSpeed;
                public final ForgeConfigSpec.DoubleValue flankDistance;
                public final ForgeConfigSpec.DoubleValue flankArrivalDistance;
                public final ForgeConfigSpec.DoubleValue flankBypassDistance;
                public final ForgeConfigSpec.DoubleValue sideApproachWeight;
                public final ForgeConfigSpec.IntValue flankRepathTicks;
                public final ForgeConfigSpec.IntValue tacticalMovementTimeoutTicks;
                public final ForgeConfigSpec.BooleanValue chaseJumpEnabled;
                public final ForgeConfigSpec.DoubleValue chaseJumpHeightThreshold;
                public final ForgeConfigSpec.DoubleValue chaseJumpTriggerDistance;

                private ValkyrieQueenCombatConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Core Valkyrie Queen combat rules.", "武神女王核心战斗规则。")
                                        .push("combat");
                        phaseTwoHealthRatio = defineDouble(builder, "phase_two_health_ratio",
                                        ValkyrieQueenMechanics.PHASE_TWO_HEALTH_RATIO, 0.0, 1.0,
                                        "Phase-two health threshold ratio.", "二阶段生命比例阈值。");
                        reactiveTeleportChance = defineDouble(builder, "reactive_teleport_chance",
                                        ValkyrieQueenMechanics.TELEPORT_CHANCE, 0.0, 1.0,
                                        "Chance to teleport after taking damage.", "受伤后触发传送的概率。");
                        reactiveTeleportCooldownTicks = defineInt(builder,
                                        "reactive_teleport_cooldown_ticks",
                                        ValkyrieQueenMechanics.TELEPORT_COOLDOWN_TICKS, 0, MAX_TICKS,
                                        "Reactive-teleport cooldown, in ticks.", "受击传送冷却，单位为 tick。");
                        phaseTwoKnockbackResistance = defineDouble(builder,
                                        "phase_two_knockback_resistance", 1.0, 0.0, 1.0,
                                        "Knockback resistance added in phase two.", "二阶段增加的击退抗性。");
                        basicsBeforeSkill = defineInt(builder, "basic_attacks_before_skill", 3,
                                        0, 100, "Basic attacks required before a martial skill.",
                                        "释放武神技前需要完成的基本技次数。");
                        basicsBeforeSpear = defineInt(builder, "basic_attacks_before_spear", 2,
                                        0, 100, "Basic attacks required before Spear Throw.",
                                        "释放长枪投掷前需要完成的基本技次数。");
                        initialSkillDelayTicks = defineInt(builder, "initial_skill_delay_ticks", 60,
                                        0, MAX_TICKS, "Initial martial-skill delay.", "武神技初始延迟。");
                        initialSpearDelayTicks = defineInt(builder, "initial_spear_delay_ticks", 160,
                                        0, MAX_TICKS, "Initial Spear Throw delay.", "长枪投掷初始延迟。");
                        skillCooldownTicks = defineInt(builder, "skill_cooldown_ticks",
                                        ValkyrieQueenMechanics.SKILL_COOLDOWN_TICKS, 0, MAX_TICKS,
                                        "Cooldown shared by Martial Skill I and II.", "武神技一和二共用冷却。");
                        spearCooldownTicks = defineInt(builder, "spear_cooldown_ticks",
                                        ValkyrieQueenMechanics.SPEAR_COOLDOWN_TICKS, 0, MAX_TICKS,
                                        "Spear Throw cooldown.", "武神技·投掷冷却。");
                        skillOneChainChance = defineDouble(builder, "skill_one_chain_chance",
                                        ValkyrieQueenMechanics.SKILL_ONE_CHAIN_CHANCE, 0.0, 1.0,
                                        "Chance for Martial Skill I to chain into Martial Skill II.",
                                        "武神技一衔接武神技二的概率。");
                        tacticalFlankingEnabled = builder.comment(
                                        "Whether the Queen pathfinds to a target's side before attacking.",
                                        "武神女王是否在攻击前寻路切入目标侧方。")
                                        .define("tactical_flanking_enabled", true);
                        pursuitRange = defineDistance(builder, "pursuit_range", 48.0,
                                        "Range used to reacquire the nearest eligible player during a boss fight.",
                                        "Boss 战中重新捕获最近合格玩家的追击范围。");
                        approachSpeed = defineSpeedMultiplier(builder, "approach_speed", 1.10,
                                        "Pathfinding speed used by direct pursuit.",
                                        "直接追击时的寻路速度。");
                        flankSpeed = defineSpeedMultiplier(builder, "flank_speed", 1.35,
                                        "Pathfinding speed used while moving to a target's side.",
                                        "切入目标侧方时的寻路速度。");
                        flankDistance = defineDistance(builder, "flank_distance", 3.5,
                                        "Desired distance from the target at a flank point.",
                                        "侧翼点与目标之间的期望距离。");
                        flankArrivalDistance = defineDistance(builder, "flank_arrival_distance", 1.15,
                                        "Horizontal distance at which a flank point counts as reached.",
                                        "判定已抵达侧翼点的水平距离。");
                        flankBypassDistance = defineDistance(builder, "flank_bypass_distance", 2.0,
                                        "Distance below which the Queen attacks immediately instead of circling.",
                                        "低于此距离时武神女王直接攻击，不再绕行切侧。");
                        sideApproachWeight = defineDouble(builder, "side_approach_weight", 0.35,
                                        0.05, 0.45,
                                        "Random weight for each side; the remaining weight selects the rear.",
                                        "每个侧方的随机权重，剩余权重用于选择后方。");
                        flankRepathTicks = defineInt(builder, "flank_repath_ticks", 8, 1, 200,
                                        "Ticks between flank-path updates.", "侧翼寻路重算间隔，单位为 tick。");
                        tacticalMovementTimeoutTicks = defineInt(builder,
                                        "tactical_movement_timeout_ticks", 40, 1, MAX_TICKS,
                                        "Expected tactical-movement time; timing out prepares the next skill.",
                                        "战术移动预期时间；超时后直接准备下一项技能，单位为 tick。");
                        chaseJumpEnabled = builder.comment(
                                        "Whether the Queen actively jumps over obstacles while pursuing.",
                                        "武神女王追击时是否主动跳过障碍。")
                                        .define("chase_jump_enabled", true);
                        chaseJumpHeightThreshold = defineDistance(builder,
                                        "chase_jump_height_threshold", 0.6,
                                        "Target height advantage that can trigger a chase jump.",
                                        "可触发追击跳跃的目标高度差。");
                        chaseJumpTriggerDistance = defineDistance(builder,
                                        "chase_jump_trigger_distance", 4.5,
                                        "Maximum horizontal target distance for height-triggered jumping.",
                                        "高度差触发跳跃时允许的最大水平距离。");
                        builder.pop();
                }
        }

        public static final class ValkyrieQueenDamageConfig {
                public final DamageFormula diagonalSlash;
                public final DamageFormula horizontalSlash;
                public final DamageFormula verticalChop;
                public final DamageFormula swordWave;
                public final DamageFormula dash;
                public final DamageFormula skillTwoSpin;
                public final DamageFormula basicLanceSpin;
                public final DamageFormula spearThrow;
                public final DamageFormula lightningBonus;
                public final DamageFormula thunderCloud;
                public final DamageFormula thunderCrystal;

                private ValkyrieQueenDamageConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Valkyrie Queen damage formulas.", "武神女王伤害公式。")
                                        .push("damage");
                        diagonalSlash = defineDamageFormula(builder, "diagonal_slash", 0.0,
                                        ValkyrieQueenMechanics.BASIC_DAMAGE_MULTIPLIER,
                                        "Diagonal basic slash.", "基本技：砍击。");
                        horizontalSlash = defineDamageFormula(builder, "horizontal_slash", 0.0,
                                        ValkyrieQueenMechanics.BASIC_DAMAGE_MULTIPLIER,
                                        "Horizontal basic slash.", "基本技：横斩。");
                        verticalChop = defineDamageFormula(builder, "vertical_chop", 0.0,
                                        ValkyrieQueenMechanics.BASIC_DAMAGE_MULTIPLIER,
                                        "Vertical basic chop.", "基本技：竖劈。");
                        swordWave = defineDamageFormula(builder, "sword_wave", 0.0,
                                        ValkyrieQueenMechanics.SWORD_WAVE_DAMAGE_MULTIPLIER,
                                        "Martial Skill I sword wave.", "武神技一剑气。");
                        dash = defineDamageFormula(builder, "skill_two_dash", 0.0,
                                        ValkyrieQueenMechanics.DASH_DAMAGE_MULTIPLIER,
                                        "Martial Skill II dash.", "武神技二突进。");
                        skillTwoSpin = defineDamageFormula(builder, "skill_two_spin", 0.0,
                                        ValkyrieQueenMechanics.SKILL_TWO_SPIN_DAMAGE_MULTIPLIER,
                                        "Martial Skill II lance spin.", "武神技二长枪回旋。");
                        basicLanceSpin = defineDamageFormula(builder, "basic_lance_spin", 0.0,
                                        ValkyrieQueenMechanics.BASIC_LANCE_SPIN_DAMAGE_MULTIPLIER,
                                        "Phase-two follow-up lance spin.", "二阶段基本技追加长枪回旋。");
                        spearThrow = defineDamageFormula(builder, "spear_throw", 0.0,
                                        ValkyrieQueenMechanics.SPEAR_THROW_DAMAGE_MULTIPLIER,
                                        "Spear Throw impact.", "武神技·投掷落点冲击。");
                        lightningBonus = defineDamageFormula(builder, "lightning_bonus", 0.0,
                                        ValkyrieQueenMechanics.LIGHTNING_DAMAGE_MULTIPLIER,
                                        "Phase-two lightning bonus.", "二阶段攻击附带雷电伤害。");
                        thunderCloud = defineDamageFormula(builder, "thunder_cloud", 0.0,
                                        ValkyrieQueenMechanics.LIGHTNING_DAMAGE_MULTIPLIER,
                                        "Thunder-cloud damage pulse.", "雷云伤害脉冲。");
                        thunderCrystal = defineDamageFormula(builder, "thunder_crystal", 0.0,
                                        5.0 / 13.5,
                                        "Thunder Crystal hit left by reactive teleport.",
                                        "受击传送留下的雷音球命中伤害。");
                        builder.pop();
                }
        }

        public static final class ValkyrieQueenTimingConfig {
                public final ForgeConfigSpec.IntValue basicWindupTicks;
                public final ForgeConfigSpec.IntValue basicRecoveryTicks;
                public final ForgeConfigSpec.IntValue skillRecoveryTicks;
                public final ForgeConfigSpec.IntValue skillOneWindupTicks;
                public final ForgeConfigSpec.IntValue skillTwoWindupTicks;
                public final ForgeConfigSpec.IntValue spearWindupTicks;
                public final ForgeConfigSpec.IntValue swordWaveGapTicks;
                public final ForgeConfigSpec.IntValue skillOneFireTicks;
                public final ForgeConfigSpec.IntValue spinWindupTicks;
                public final ForgeConfigSpec.IntValue dashTickLimit;
                public final ForgeConfigSpec.IntValue spearFlightTicks;
                public final ForgeConfigSpec.IntValue spearRetrieveTickLimit;
                public final ForgeConfigSpec.IntValue thunderCloudTicks;
                public final ForgeConfigSpec.IntValue thunderCloudDamageInterval;
                public final ForgeConfigSpec.IntValue parryRecoveryTicks;

                private ValkyrieQueenTimingConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment(
                                        "Valkyrie Queen timing in ticks. Windups are intentionally readable.",
                                        "武神女王时间参数，单位为 tick。默认前摇设计为清晰可读。")
                                        .push("timing");
                        basicWindupTicks = defineInt(builder, "basic_windup_ticks", 18,
                                        0, MAX_TICKS, "Basic-attack windup.", "基本技前摇。");
                        basicRecoveryTicks = defineInt(builder, "basic_recovery_ticks",
                                        ValkyrieQueenMechanics.BASIC_RECOVERY_TICKS, 0, MAX_TICKS,
                                        "Recovery after a basic attack.", "基本技后的恢复时间。");
                        skillRecoveryTicks = defineInt(builder, "skill_recovery_ticks",
                                        ValkyrieQueenMechanics.SKILL_RECOVERY_TICKS, 0, MAX_TICKS,
                                        "Recovery after a martial skill.", "武神技后的恢复时间。");
                        skillOneWindupTicks = defineInt(builder, "skill_one_windup_ticks", 36,
                                        0, MAX_TICKS, "Martial Skill I windup.", "武神技一前摇。");
                        skillTwoWindupTicks = defineInt(builder, "skill_two_windup_ticks", 34,
                                        0, MAX_TICKS, "Martial Skill II windup.", "武神技二前摇。");
                        spearWindupTicks = defineInt(builder, "spear_throw_windup_ticks", 44,
                                        0, MAX_TICKS, "Spear Throw windup.", "武神技·投掷前摇。");
                        swordWaveGapTicks = defineInt(builder, "sword_wave_gap_ticks",
                                        ValkyrieQueenMechanics.SWORD_WAVE_GAP_TICKS, 0, MAX_TICKS,
                                        "Delay between the two sword waves.", "两道剑气之间的间隔。");
                        skillOneFireTicks = defineInt(builder, "skill_one_fire_ticks",
                                        ValkyrieQueenMechanics.SKILL_ONE_FIRE_TICKS, 1, MAX_TICKS,
                                        "Sword-wave firing-phase duration.", "剑气释放阶段持续时间。");
                        spinWindupTicks = defineInt(builder, "lance_spin_windup_ticks", 16,
                                        0, MAX_TICKS, "Windup before each lance spin.", "每次长枪回旋前摇。");
                        dashTickLimit = defineInt(builder, "skill_two_dash_tick_limit",
                                        ValkyrieQueenMechanics.DASH_TICK_LIMIT, 1, MAX_TICKS,
                                        "Maximum Martial Skill II dash duration.", "武神技二突进最大时间。");
                        spearFlightTicks = defineInt(builder, "spear_flight_ticks",
                                        ValkyrieQueenMechanics.SPEAR_FLIGHT_TICKS, 1, MAX_TICKS,
                                        "Maximum spear-flight duration.", "长枪飞行最大时间。");
                        spearRetrieveTickLimit = defineInt(builder, "spear_retrieve_tick_limit",
                                        ValkyrieQueenMechanics.SPEAR_RETRIEVE_TICK_LIMIT, 1, MAX_TICKS,
                                        "Maximum spear-retrieval duration.", "长枪回收最大时间。");
                        thunderCloudTicks = defineInt(builder, "thunder_cloud_ticks",
                                        ValkyrieQueenMechanics.THUNDER_CLOUD_TICKS, 1, MAX_TICKS,
                                        "Thunder-cloud duration.", "雷云持续时间。");
                        thunderCloudDamageInterval = defineInt(builder,
                                        "thunder_cloud_damage_interval_ticks",
                                        ValkyrieQueenMechanics.THUNDER_CLOUD_DAMAGE_INTERVAL, 1, MAX_TICKS,
                                        "Delay between cloud damage pulses.", "雷云伤害脉冲间隔。");
                        parryRecoveryTicks = defineInt(builder, "parry_recovery_ticks",
                                        ValkyrieQueenMechanics.PARRY_RECOVERY_TICKS, 1, MAX_TICKS,
                                        "Recovery after being parried.", "被招架后的恢复时间。");
                        builder.pop();
                }
        }

        public static final class ValkyrieQueenRangeConfig {
                public final ForgeConfigSpec.DoubleValue diagonalRange;
                public final ForgeConfigSpec.DoubleValue horizontalRange;
                public final ForgeConfigSpec.DoubleValue verticalRange;
                public final ForgeConfigSpec.DoubleValue diagonalHalfAngle;
                public final ForgeConfigSpec.DoubleValue horizontalHalfAngle;
                public final ForgeConfigSpec.DoubleValue verticalHalfAngle;
                public final ForgeConfigSpec.DoubleValue attackDownwardRange;
                public final ForgeConfigSpec.DoubleValue meleeVerticalTolerance;
                public final ForgeConfigSpec.DoubleValue swordWaveSpeed;
                public final ForgeConfigSpec.DoubleValue swordWaveDistance;
                public final ForgeConfigSpec.DoubleValue swordWaveHitRadius;
                public final ForgeConfigSpec.DoubleValue swordWaveHitHalfHeight;
                public final ForgeConfigSpec.DoubleValue dashSpeed;
                public final ForgeConfigSpec.DoubleValue dashDistance;
                public final ForgeConfigSpec.DoubleValue dashHitInflation;
                public final ForgeConfigSpec.DoubleValue spearSpeed;
                public final ForgeConfigSpec.DoubleValue spearRetrieveSpeed;
                public final ForgeConfigSpec.DoubleValue skillTwoSpinRadius;
                public final ForgeConfigSpec.DoubleValue basicLanceSpinRadius;
                public final ForgeConfigSpec.DoubleValue spearImpactRadius;
                public final ForgeConfigSpec.DoubleValue thunderCloudRadius;
                public final ForgeConfigSpec.DoubleValue thunderCloudHalfHeight;
                public final ForgeConfigSpec.DoubleValue skillTriggerDistance;
                public final ForgeConfigSpec.DoubleValue spearTriggerDistance;

                private ValkyrieQueenRangeConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Valkyrie Queen range and movement geometry.",
                                        "武神女王范围与移动几何参数。")
                                        .push("range");
                        diagonalRange = defineDistance(builder, "diagonal_slash_range",
                                        ValkyrieQueenMechanics.DIAGONAL_RANGE, "Diagonal-slash radius.", "砍击半径。");
                        horizontalRange = defineDistance(builder, "horizontal_slash_range",
                                        ValkyrieQueenMechanics.HORIZONTAL_RANGE, "Horizontal-slash radius.", "横斩半径。");
                        verticalRange = defineDistance(builder, "vertical_chop_range",
                                        ValkyrieQueenMechanics.VERTICAL_RANGE, "Vertical-chop radius.", "竖劈半径。");
                        diagonalHalfAngle = defineAngle(builder, "diagonal_slash_half_angle",
                                        ValkyrieQueenMechanics.DIAGONAL_HALF_ANGLE, "Diagonal-slash half-angle.",
                                        "砍击扇形半角。");
                        horizontalHalfAngle = defineAngle(builder, "horizontal_slash_half_angle",
                                        ValkyrieQueenMechanics.HORIZONTAL_HALF_ANGLE, "Horizontal-slash half-angle.",
                                        "横斩扇形半角。");
                        verticalHalfAngle = defineAngle(builder, "vertical_chop_half_angle",
                                        ValkyrieQueenMechanics.VERTICAL_HALF_ANGLE, "Vertical-chop half-angle.",
                                        "竖劈扇形半角。");
                        attackDownwardRange = defineDistance(builder, "attack_downward_range", 12.0,
                                        "Downward hit range shared by all Valkyrie Queen skills.",
                                        "武神女王所有技能共用的向下命中范围。");
                        meleeVerticalTolerance = defineDistance(builder, "melee_vertical_tolerance", 3.0,
                                        "Upward tolerance for basic and radial direct attacks.",
                                        "基本技与圆形直接攻击的向上命中容差。");
                        swordWaveSpeed = defineSpeedMultiplier(builder, "sword_wave_speed",
                                        ValkyrieQueenMechanics.SWORD_WAVE_SPEED, "Sword-wave speed.", "剑气速度。");
                        swordWaveDistance = defineDistance(builder, "sword_wave_distance",
                                        ValkyrieQueenMechanics.SWORD_WAVE_DISTANCE, "Sword-wave travel distance.",
                                        "剑气飞行距离。");
                        swordWaveHitRadius = defineDistance(builder, "sword_wave_hit_radius", 1.0,
                                        "Sword-wave horizontal hit radius.", "剑气水平命中半径。");
                        swordWaveHitHalfHeight = defineDistance(builder, "sword_wave_hit_half_height", 1.4,
                                        "Sword-wave upward hit height.", "剑气向上命中高度。");
                        dashSpeed = defineSpeedMultiplier(builder, "skill_two_dash_speed",
                                        ValkyrieQueenMechanics.DASH_SPEED, "Martial Skill II dash speed.", "武神技二突进速度。");
                        dashDistance = defineDistance(builder, "skill_two_dash_distance",
                                        ValkyrieQueenMechanics.DASH_DISTANCE, "Martial Skill II dash distance.",
                                        "武神技二突进距离。");
                        dashHitInflation = defineDistance(builder, "skill_two_dash_hit_inflation", 0.35,
                                        "Extra dash hit-box size.", "突进命中箱额外膨胀值。");
                        spearSpeed = defineSpeedMultiplier(builder, "spear_speed",
                                        ValkyrieQueenMechanics.SPEAR_SPEED, "Thrown-spear speed.", "投掷长枪速度。");
                        spearRetrieveSpeed = defineSpeedMultiplier(builder, "spear_retrieve_speed",
                                        ValkyrieQueenMechanics.SPEAR_RETRIEVE_SPEED, "Spear-retrieval speed.",
                                        "回收长枪速度。");
                        skillTwoSpinRadius = defineDistance(builder, "skill_two_spin_radius",
                                        ValkyrieQueenMechanics.SKILL_TWO_SPIN_RADIUS, "Martial Skill II spin radius.",
                                        "武神技二回旋半径。");
                        basicLanceSpinRadius = defineDistance(builder, "basic_lance_spin_radius",
                                        ValkyrieQueenMechanics.BASIC_LANCE_SPIN_RADIUS, "Basic follow-up spin radius.",
                                        "基本技追加回旋半径。");
                        spearImpactRadius = defineDistance(builder, "spear_impact_radius",
                                        ValkyrieQueenMechanics.SPEAR_IMPACT_RADIUS, "Spear-impact radius.",
                                        "长枪落点伤害半径。");
                        thunderCloudRadius = defineDistance(builder, "thunder_cloud_radius",
                                        ValkyrieQueenMechanics.THUNDER_CLOUD_RADIUS, "Thunder-cloud radius.",
                                        "雷云水平半径。");
                        thunderCloudHalfHeight = defineDistance(builder, "thunder_cloud_half_height", 3.0,
                                        "Thunder-cloud upward hit height.", "雷云向上命中高度。");
                        skillTriggerDistance = defineDistance(builder, "skill_trigger_distance", 20.0,
                                        "Maximum martial-skill start distance.", "武神技最大起手距离。");
                        spearTriggerDistance = defineDistance(builder, "spear_trigger_distance", 22.0,
                                        "Maximum Spear Throw start distance.", "长枪投掷最大起手距离。");
                        builder.pop();
                }
        }

        public static final class SunSpiritCombatConfig {
                public final ForgeConfigSpec.DoubleValue healthPerMinion;
                public final ForgeConfigSpec.DoubleValue minionDamageMultiplier;
                public final ForgeConfigSpec.DoubleValue reflectedIceHealthRatio;
                public final ForgeConfigSpec.DoubleValue phaseTwoHealthRatio;
                public final ForgeConfigSpec.DoubleValue phaseTwoDamageMultiplier;
                public final ForgeConfigSpec.DoubleValue pursuitRange;
                public final ForgeConfigSpec.IntValue minionsPerHealthThreshold;
                public final ForgeConfigSpec.IntValue emptyFieldSummonCount;
                public final ForgeConfigSpec.DoubleValue iceProjectileChance;
                public final ForgeConfigSpec.DoubleValue knockbackResistance;
                public final ForgeConfigSpec.DoubleValue minionKnockbackResistance;

                private SunSpiritCombatConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Core Sun Spirit combat rules.", "烈阳巨灵核心战斗规则。")
                                        .push("combat");
                        healthPerMinion = defineDouble(builder, "health_lost_per_minion",
                                        SunSpiritMechanics.HEALTH_THRESHOLD_RATIO, 0.01, 1.0,
                                        "Maximum-health ratio lost for each automatic minion summon.",
                                        "每次自动召唤一只仆从所需损失的最大生命比例。");
                        minionDamageMultiplier = defineDouble(builder, "minion_damage_multiplier",
                                        SunSpiritMechanics.MINION_DAMAGE_MULTIPLIER, 0.0, 1.0,
                                        "Incoming-damage multiplier while an owned minion is alive.",
                                        "存在所属仆从时受到的伤害倍率。");
                        reflectedIceHealthRatio = defineDouble(builder, "reflected_ice_health_ratio",
                                        SunSpiritMechanics.REFLECTED_ICE_HEALTH_RATIO, 0.0, 1.0,
                                        "Maximum-health ratio removed by a reflected Ice Crystal.",
                                        "反弹冰结球命中时扣除的最大生命比例。");
                        phaseTwoHealthRatio = defineDouble(builder, "phase_two_health_ratio",
                                        SunSpiritMechanics.PHASE_TWO_HEALTH_RATIO, 0.0, 1.0,
                                        "Phase-two health threshold ratio.", "二阶段生命比例阈值。");
                        phaseTwoDamageMultiplier = defineDouble(builder, "phase_two_damage_multiplier",
                                        SunSpiritMechanics.PHASE_TWO_DAMAGE_MULTIPLIER, 0.0, 100.0,
                                        "Damage multiplier in phase two.", "二阶段伤害倍率。");
                        pursuitRange = defineDistance(builder, "pursuit_range", 48.0,
                                        "Range used to acquire eligible players.", "主动锁定合格玩家的范围。");
                        minionsPerHealthThreshold = defineInt(builder,
                                        "minions_per_health_threshold", 1, 0, 20,
                                        "Minions summoned per crossed health threshold.",
                                        "每跨过一个生命阈值召唤的仆从数量。");
                        emptyFieldSummonCount = defineInt(builder, "empty_field_summon_count", 2,
                                        0, 20, "Minions summoned by the dedicated summon skill.",
                                        "场上无仆从时，专用召唤技能召唤的数量。");
                        iceProjectileChance = defineDouble(builder, "ice_projectile_chance", 0.5,
                                        0.0, 1.0, "Chance for a projectile attack to use an Ice Crystal.",
                                        "投射物攻击使用冰结球的概率。");
                        knockbackResistance = defineDouble(builder, "knockback_resistance", 1.0,
                                        0.0, 1.0, "Sun Spirit knockback resistance.",
                                        "烈阳巨灵击退抗性。");
                        minionKnockbackResistance = defineDouble(builder,
                                        "minion_knockback_resistance", 1.0, 0.0, 1.0,
                                        "Owned Fire Minion knockback resistance.",
                                        "所属烈焰仆从击退抗性。");
                        builder.pop();
                }
        }

        public static final class SunSpiritDamageConfig {
                public final DamageFormula projectile;
                public final DamageFormula risingFlame;
                public final DamageFormula flameSigil;
                public final DamageFormula titanFist;

                private SunSpiritDamageConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Sun Spirit damage formulas.", "烈阳巨灵伤害公式。")
                                        .push("damage");
                        projectile = defineDamageFormula(builder, "projectile", 0.0, 1.0,
                                        "Rolling Fireball and Ice Crystal.", "滚炎球与冰结球。");
                        risingFlame = defineDamageFormula(builder, "rising_flame", 0.0, 1.5,
                                        "Rising Flame circular burst.", "烈焰升腾环形爆发。");
                        flameSigil = defineDamageFormula(builder, "flame_sigil", 0.0, 1.0,
                                        "Delayed flame sigil.", "延迟升起的烈焰法阵。");
                        titanFist = defineDamageFormula(builder, "titan_fist", 0.0, 1.2,
                                        "Titan Fist rectangle.", "巨神之拳矩形攻击。");
                        builder.pop();
                }
        }

        public static final class SunSpiritTimingConfig {
                public final ForgeConfigSpec.IntValue projectileWindupTicks;
                public final ForgeConfigSpec.IntValue risingFlameWindupTicks;
                public final ForgeConfigSpec.IntValue titanFistWindupTicks;
                public final ForgeConfigSpec.IntValue summonWindupTicks;
                public final ForgeConfigSpec.IntValue attackRecoveryTicks;
                public final ForgeConfigSpec.IntValue summonRecoveryTicks;
                public final ForgeConfigSpec.IntValue summonCooldownTicks;
                public final ForgeConfigSpec.IntValue initialSummonDelayTicks;
                public final ForgeConfigSpec.IntValue flameSigilDelayTicks;
                public final ForgeConfigSpec.IntValue flameSeconds;
                public final ForgeConfigSpec.IntValue shieldCooldownTicks;
                public final ForgeConfigSpec.IntValue parryRecoveryTicks;
                public final ForgeConfigSpec.IntValue phaseTwoSigilIntervalTicks;
                public final ForgeConfigSpec.IntValue initialPhaseTwoSigilDelayTicks;

                private SunSpiritTimingConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Sun Spirit timing in ticks.", "烈阳巨灵时间参数，单位为 tick。")
                                        .push("timing");
                        projectileWindupTicks = defineInt(builder, "projectile_windup_ticks", 24,
                                        0, MAX_TICKS, "Projectile windup.", "滚炎球/冰结球前摇。");
                        risingFlameWindupTicks = defineInt(builder, "rising_flame_windup_ticks", 36,
                                        0, MAX_TICKS, "Rising Flame windup.", "烈焰升腾前摇。");
                        titanFistWindupTicks = defineInt(builder, "titan_fist_windup_ticks", 32,
                                        0, MAX_TICKS, "Titan Fist windup.", "巨神之拳前摇。");
                        summonWindupTicks = defineInt(builder, "summon_windup_ticks", 60,
                                        0, MAX_TICKS, "Distinct dedicated-summon windup.",
                                        "具有明显区别的专用召唤前摇。");
                        attackRecoveryTicks = defineInt(builder, "attack_recovery_ticks", 40,
                                        0, MAX_TICKS, "Recovery after ordinary skills.", "普通技能后摇。");
                        summonRecoveryTicks = defineInt(builder, "summon_recovery_ticks", 50,
                                        0, MAX_TICKS, "Recovery after summoning or interruption.",
                                        "召唤完成或被打断后的恢复时间。");
                        summonCooldownTicks = defineInt(builder, "summon_cooldown_ticks", 300,
                                        0, MAX_TICKS, "Dedicated summon cooldown.", "专用召唤技能冷却。");
                        initialSummonDelayTicks = defineInt(builder, "initial_summon_delay_ticks", 100,
                                        0, MAX_TICKS, "Initial dedicated-summon delay.", "专用召唤初始延迟。");
                        flameSigilDelayTicks = defineInt(builder, "flame_sigil_delay_ticks", 36,
                                        0, MAX_TICKS, "Delay before a flame sigil erupts.", "烈焰法阵喷发延迟。");
                        flameSeconds = defineInt(builder, "flame_seconds", 5,
                                        0, 300, "Fire duration after a sigil hit, in seconds.",
                                        "法阵命中后的点燃秒数。");
                        shieldCooldownTicks = defineInt(builder, "shield_cooldown_ticks", 100,
                                        0, MAX_TICKS, "Shield cooldown after blocking a heavy skill.",
                                        "格挡重型技能后的盾牌冷却。");
                        parryRecoveryTicks = defineInt(builder, "parry_recovery_ticks", 40,
                                        1, MAX_TICKS, "Recovery after a SlashBlade parry.",
                                        "被拔刀剑招架后的恢复时间。");
                        phaseTwoSigilIntervalTicks = defineInt(builder,
                                        "phase_two_sigil_interval_ticks", 160, 1, MAX_TICKS,
                                        "Interval between automatic phase-two flame sigils.",
                                        "二阶段自动烈焰法阵间隔。");
                        initialPhaseTwoSigilDelayTicks = defineInt(builder,
                                        "initial_phase_two_sigil_delay_ticks", 80, 0, MAX_TICKS,
                                        "Delay before the first automatic phase-two sigil.",
                                        "进入二阶段后首个自动法阵的延迟。");
                        builder.pop();
                }
        }

        public static final class SunSpiritRangeConfig {
                public final ForgeConfigSpec.DoubleValue projectileSpeed;
                public final ForgeConfigSpec.DoubleValue risingFlameRadius;
                public final ForgeConfigSpec.DoubleValue titanFistLength;
                public final ForgeConfigSpec.DoubleValue titanFistHalfWidth;
                public final ForgeConfigSpec.DoubleValue flameSigilRadius;
                public final ForgeConfigSpec.DoubleValue verticalHitRange;
                public final ForgeConfigSpec.DoubleValue summonRadius;
                public final ForgeConfigSpec.DoubleValue summonTelegraphRadius;
                public final ForgeConfigSpec.DoubleValue minionSearchRange;

                private SunSpiritRangeConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment("Sun Spirit ranges and projectile speed.",
                                        "烈阳巨灵范围与投射物速度。")
                                        .push("range");
                        projectileSpeed = defineSpeedMultiplier(builder, "projectile_speed", 1.1,
                                        "Fire and Ice Crystal launch speed.", "滚炎球与冰结球发射速度。");
                        risingFlameRadius = defineDistance(builder, "rising_flame_radius", 6.0,
                                        "Rising Flame radius.", "烈焰升腾半径。");
                        titanFistLength = defineDistance(builder, "titan_fist_length", 10.0,
                                        "Titan Fist forward length.", "巨神之拳前方长度。");
                        titanFistHalfWidth = defineDistance(builder, "titan_fist_half_width", 3.0,
                                        "Titan Fist rectangle half-width.", "巨神之拳矩形半宽。");
                        flameSigilRadius = defineDistance(builder, "flame_sigil_radius", 3.5,
                                        "Flame sigil radius.", "烈焰法阵半径。");
                        verticalHitRange = defineDistance(builder, "vertical_hit_range", 12.0,
                                        "Vertical hit range for direct skills.", "直接技能垂直命中范围。");
                        summonRadius = defineDistance(builder, "summon_radius", 4.0,
                                        "Minion spawn radius.", "仆从生成半径。");
                        summonTelegraphRadius = defineDistance(builder, "summon_telegraph_radius", 8.0,
                                        "Distinct summon-windup telegraph radius.", "专用召唤前摇提示半径。");
                        minionSearchRange = defineDistance(builder, "minion_search_range", 96.0,
                                        "Range used to count owned minions.", "统计所属仆从的范围。");
                        builder.pop();
                }
        }

        public static final class AttackTelegraphConfig {
                public final ForgeConfigSpec.BooleanValue enabled;
                public final ForgeConfigSpec.DoubleValue maxRenderDistance;
                public final ForgeConfigSpec.IntValue red;
                public final ForgeConfigSpec.IntValue green;
                public final ForgeConfigSpec.IntValue blue;
                public final ForgeConfigSpec.IntValue alpha;
                public final ForgeConfigSpec.DoubleValue lineWidth;
                public final ForgeConfigSpec.BooleanValue fillEnabled;
                public final ForgeConfigSpec.IntValue fillStartAlpha;
                public final ForgeConfigSpec.IntValue fillEndAlpha;
                public final ForgeConfigSpec.IntValue curveSegments;
                public final ForgeConfigSpec.DoubleValue heightOffset;

                private AttackTelegraphConfig(ForgeConfigSpec.Builder builder) {
                        builder.comment(
                                        "Attack-range outline rendering using Minecraft's built-in line shader.",
                                        "使用 Minecraft 内置线条着色器渲染攻击范围轮廓。")
                                        .push("attack_telegraph");
                        enabled = builder.comment("Render red attack-range outlines.", "渲染红色攻击范围轮廓。")
                                        .define("enabled", true);
                        maxRenderDistance = defineDouble(builder, "max_render_distance", 96.0,
                                        8.0, 512.0, "Maximum render distance.", "最大渲染距离。");
                        red = defineInt(builder, "red", 255, 0, 255, "Red channel.", "红色通道。");
                        green = defineInt(builder, "green", 35, 0, 255, "Green channel.", "绿色通道。");
                        blue = defineInt(builder, "blue", 35, 0, 255, "Blue channel.", "蓝色通道。");
                        alpha = defineInt(builder, "alpha", 210, 0, 255, "Outline alpha.", "轮廓透明度。");
                        lineWidth = defineDouble(builder, "line_width", 5.0, 1.0, 12.0,
                                        "Requested shader line width; hardware may clamp it.",
                                        "请求的着色器线宽；显卡可能限制实际值。");
                        fillEnabled = builder.comment(
                                        "Fill the marked attack area with translucent color.",
                                        "使用半透明颜色填充标记的攻击区域。")
                                        .define("fill_enabled", true);
                        fillStartAlpha = defineInt(builder, "fill_start_alpha", 20, 0, 255,
                                        "Fill alpha at the beginning of the windup.",
                                        "前摇开始时的填充透明度。");
                        fillEndAlpha = defineInt(builder, "fill_end_alpha", 150, 0, 255,
                                        "Fill alpha when the windup is almost complete.",
                                        "前摇即将完成时的填充透明度。");
                        curveSegments = defineInt(builder, "curve_segments", 48, 8, 256,
                                        "Segments used for circles and arcs.", "圆形和扇形使用的分段数。");
                        heightOffset = defineDouble(builder, "height_offset", 0.08,
                                        -4.0, 4.0, "Vertical outline offset.", "轮廓垂直偏移。");
                        builder.pop();
                }
        }

        private static DamageFormula defineDamageFormula(ForgeConfigSpec.Builder builder,
                        String path,
                        double defaultBaseDamage,
                        double defaultAttackDamageMultiplier,
                        String english,
                        String chinese) {
                builder.comment(english, chinese).push(path);
                ForgeConfigSpec.DoubleValue baseDamage = builder
                                .comment("Flat damage before the attack contribution.",
                                                "攻击力贡献之前的固定伤害。")
                                .defineInRange("base_damage", defaultBaseDamage, 0.0, MAX_BASE_DAMAGE);
                ForgeConfigSpec.DoubleValue attackDamageMultiplier = builder
                                .comment("Current attack-damage multiplier. Formula: base + attack * multiplier.",
                                                "当前攻击力倍率。公式：固定伤害 + 攻击力 * 倍率。")
                                .defineInRange("attack_damage_multiplier", defaultAttackDamageMultiplier,
                                                0.0, MAX_ATTACK_DAMAGE_MULTIPLIER);
                builder.pop();
                return new DamageFormula(baseDamage, attackDamageMultiplier);
        }

        private static ForgeConfigSpec.DoubleValue defineSpeedMultiplier(
                        ForgeConfigSpec.Builder builder,
                        String path,
                        double defaultValue,
                        String english,
                        String chinese) {
                return defineDouble(builder, path, defaultValue, 0.0, MAX_SPEED_MULTIPLIER,
                                english, chinese);
        }

        private static ForgeConfigSpec.DoubleValue defineDistance(
                        ForgeConfigSpec.Builder builder, String path, double defaultValue,
                        String english, String chinese) {
                return defineDouble(builder, path, defaultValue, 0.0, MAX_DISTANCE,
                                english, chinese);
        }

        private static ForgeConfigSpec.DoubleValue defineAngle(
                        ForgeConfigSpec.Builder builder, String path, double defaultValue,
                        String english, String chinese) {
                return defineDouble(builder, path, defaultValue, 0.0, 180.0,
                                english, chinese);
        }

        private static ForgeConfigSpec.DoubleValue defineDouble(
                        ForgeConfigSpec.Builder builder, String path, double defaultValue,
                        double minimum, double maximum, String english, String chinese) {
                return builder.comment(english, chinese)
                                .defineInRange(path, defaultValue, minimum, maximum);
        }

        private static ForgeConfigSpec.IntValue defineInt(
                        ForgeConfigSpec.Builder builder, String path, int defaultValue,
                        int minimum, int maximum, String english, String chinese) {
                return builder.comment(english, chinese)
                                .defineInRange(path, defaultValue, minimum, maximum);
        }
}
