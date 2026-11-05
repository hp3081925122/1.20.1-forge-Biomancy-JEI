package com.biomancy_jei.emi;

import com.biomancy_jei.BiomancyJei;
import com.biomancy_jei.jei.BiomancyJeiPlugin;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;

// 注册原初摇篮的原生 EMI 分类，避免复杂 JEI 滚动页被 JEMI 错误包装。
@EmiEntrypoint
public final class EmiCradlePlugin implements EmiPlugin {
    public static final EmiRecipeCategory CRADLE = new EmiRecipeCategory(
            Identifier.fromNamespaceAndPath(BiomancyJei.MOD_ID, "primordial_cradle"),
            EmiStack.of(BiomancyJeiPlugin.getCradleStack())
    );

    @Override
    public void register(EmiRegistry registry) {
        // 使用合成页相同的 synthetic recipe id 和共享数据对象。
        registry.addCategory(CRADLE);
        registry.addRecipe(new EmiCradleRecipe(BiomancyJeiPlugin.getCradleRecipe()));
    }
}
