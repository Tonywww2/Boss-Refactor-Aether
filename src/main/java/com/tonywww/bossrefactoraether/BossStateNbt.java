package com.tonywww.bossrefactoraether;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.OptionalInt;
import java.util.OptionalLong;

public final class BossStateNbt {
    public static final int MAX_DURATION_TICKS = 72_000;

    private BossStateNbt() {
    }

    public static OptionalLong readDuration(CompoundTag tag, String key) {
        return readNonNegativeLong(tag, key, MAX_DURATION_TICKS);
    }

    public static OptionalLong readNonNegativeLong(
            CompoundTag tag, String key, long maximum) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)
                || !(tag.get(key) instanceof NumericTag numeric)) {
            return OptionalLong.empty();
        }
        double value = numeric.getAsDouble();
        long integer = numeric.getAsLong();
        if (!Double.isFinite(value)
                || value < 0.0
                || value > Math.max(0L, maximum)
                || value != Math.rint(value)
                || value != (double) integer) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(integer);
    }

    public static OptionalInt readNonNegativeInt(
            CompoundTag tag, String key, int maximum) {
        OptionalLong value = readNonNegativeLong(
                tag, key, Math.max(0, maximum));
        return value.isPresent()
                ? OptionalInt.of((int) value.getAsLong())
                : OptionalInt.empty();
    }
}