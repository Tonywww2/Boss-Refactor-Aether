package com.tonywww.bossrefactoraether.compat;

import com.tonywww.bossrefactoraether.BossRefactorAether;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;

public final class OptionalCompatBootstrap {
    private static final String SENDIMS_MOD_ID = "slashblade_sendims";
    private static final String SENDIMS_BOOTSTRAP =
            "com.tonywww.bossrefactoraether.compat.sendims.SenDimsSliderCompat";

    private OptionalCompatBootstrap() {
    }

    public static void initialize() {
        if (!ModList.get().isLoaded(SENDIMS_MOD_ID)) {
            BossRefactorAether.LOGGER.info(
                    "SlashBlade SenDimS is not installed; boss parry integration is disabled");
            return;
        }

        try {
            Class<?> bootstrap = Class.forName(SENDIMS_BOOTSTRAP);
            bootstrap.getMethod("initialize").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            BossRefactorAether.LOGGER.error("Unable to load SenDimS boss compatibility", exception);
        } catch (InvocationTargetException exception) {
            BossRefactorAether.LOGGER.error(
                    "SenDimS boss compatibility failed during initialization",
                    exception.getCause());
        } catch (LinkageError error) {
            BossRefactorAether.LOGGER.error(
                    "SenDimS boss compatibility has an incompatible API", error);
        }
    }
}