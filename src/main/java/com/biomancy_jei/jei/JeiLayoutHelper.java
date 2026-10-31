package com.biomancy_jei.jei;

import mezz.jei.common.Internal;
import mezz.jei.common.config.IClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class JeiLayoutHelper {
    private static final int SCREEN_MARGIN = 58;
    private static final int CENTER_SEARCH_BAR_SCREEN_MARGIN = 76;
    private static final int RECIPE_AREA_HEADER = 40;
    private static final int RECIPE_BORDER = 8;

    private JeiLayoutHelper() {
    }

    public static int fitViewport(int availableHeight, int fixedHeight, int minViewport, int maxViewport) {
        return Mth.clamp(availableHeight - fixedHeight, minViewport, maxViewport);
    }

    public static int maxRecipeHeight() {
        IClientConfig config = Internal.getJeiClientConfigs().getClientConfig();
        int screenMargin = config.isCenterSearchBarEnabled() ? CENTER_SEARCH_BAR_SCREEN_MARGIN : SCREEN_MARGIN;
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight() - screenMargin;
        guiHeight = Math.min(Math.max(guiHeight, IClientConfig.minRecipeGuiHeight), config.getMaxRecipeGuiHeight());
        return guiHeight - RECIPE_AREA_HEADER - RECIPE_BORDER;
    }
}
