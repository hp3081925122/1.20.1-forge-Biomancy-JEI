package com.biomancy_jei.emi;

import com.biomancy_jei.BiomancyJei;
import com.biomancy_jei.jei.BiomancyJeiPlugin;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;

// 注册 Biomancy 血肉镰刀掉落的原生 EMI 分类和配方。
@EmiEntrypoint
public final class EmiDespoilPlugin implements EmiPlugin {
    public static final EmiRecipeCategory DESPOIL = new EmiRecipeCategory(
            Identifier.fromNamespaceAndPath(BiomancyJei.MOD_ID, "despoil"),
            EmiStack.of(BiomancyJeiPlugin.getSickleStack())
    );

    @Override
    public void register(EmiRegistry registry) {
        // 使用与 JEI 相同的分类标识，阻止 JEMI 再生成重复的错误页面。
        registry.addCategory(DESPOIL);
        for (var recipe : BiomancyJeiPlugin.getSharedRecipes()) {
            registry.addRecipe(new EmiDespoilRecipe(recipe));
        }
    }
}
