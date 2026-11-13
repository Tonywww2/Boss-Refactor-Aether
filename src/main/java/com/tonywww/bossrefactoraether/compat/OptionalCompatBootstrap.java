package com.tonywww.bossrefactoraether.compat;

import com.tonywww.bossrefactoraether.BossRefactorAether;
import com.tonywww.bossrefactoraether.slider.SliderParryIntegration;
import com.tonywww.bossrefactoraether.sunspirit.SunSpiritParryIntegration;
import com.tonywww.bossrefactoraether.valkyriequeen.ValkyrieQueenParryIntegration;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;

public final class OptionalCompatBootstrap {
    private static final String SENDIMS_MOD_ID = "slashblade_sendims";
    private static final String SENDIMS_BOOTSTRAP =
            "com.tonywww.bossrefactoraether.compat.sendims.SenDimsSliderCompat";

    private static boolean senDimsDisabled;

    private OptionalCompatBootstrap() {
    }

    public static void initialize() {
        if (!ModList.get().isLoaded(SENDIMS_MOD_ID)) {
            BossRefactorAether.LOGGER.info(
                    "SlashBlade SenDimS is not installed; boss parry integration is disabled");
            return;
        }

        // 特供模组只是联动：版本不兼容时记录警告并关闭联动，不影响本模组加载。
        try {
            Class<?> bootstrap = Class.forName(SENDIMS_BOOTSTRAP);
            bootstrap.getMethod("initialize").invoke(null);
        } catch (InvocationTargetException exception) {
            disableSenDims("SenDimS compatibility failed during initialization",
                    exception.getCause());
        } catch (ReflectiveOperationException | LinkageError
                 | RuntimeException exception) {
            disableSenDims("the installed SenDimS is not compatible", exception);
        }
    }

    public static void disableSenDims(String reason, Throwable cause) {
        SliderParryIntegration.reset();
        ValkyrieQueenParryIntegration.reset();
        SunSpiritParryIntegration.reset();
        if (senDimsDisabled) {
            return;
        }
        senDimsDisabled = true;
        BossRefactorAether.LOGGER.warn(
                "SenDimS boss integration disabled: {}", reason, cause);
    }
}