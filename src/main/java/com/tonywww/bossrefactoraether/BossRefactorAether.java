package com.tonywww.bossrefactoraether;

import com.aetherteam.aether.entity.AetherEntityTypes;
import com.mojang.logging.LogUtils;
import com.tonywww.bossrefactoraether.compat.OptionalCompatBootstrap;
import com.tonywww.bossrefactoraether.config.BossRefactorAetherConfig;
import com.tonywww.bossrefactoraether.slider.SliderMechanics;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BossRefactorAether.MOD_ID)
public class BossRefactorAether {

    public static final String MOD_ID = "bossrefactoraether";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public BossRefactorAether(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::modifyEntityAttributes);
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.COMMON, BossRefactorAetherConfig.COMMON_SPEC);

        LOGGER.info("{} loading", MOD_ID);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(OptionalCompatBootstrap::initialize);
    }

    private void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(AetherEntityTypes.SLIDER.get(), Attributes.ATTACK_DAMAGE,
                SliderMechanics.DEFAULT_ATTACK_DAMAGE);
    }
}
