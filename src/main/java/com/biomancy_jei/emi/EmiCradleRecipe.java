package com.biomancy_jei.emi;

import com.biomancy_jei.jei.BiomancyJeiPlugin;
import com.biomancy_jei.jei.CradleRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

// 将原初摇篮数据转换为原生 EMI 信息页，并保留全部查询入口。
public final class EmiCradleRecipe implements EmiRecipe {
    private static final int WIDTH = 164;
    private static final int HEIGHT = 180;
    private final CradleRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EmiCradleRecipe(CradleRecipe recipe) {
        // 把摇篮和每个可投喂物品加入输入，把所有可能生物加入输出。
        this.recipe = recipe;
        List<EmiIngredient> lookupInputs = new ArrayList<>();
        lookupInputs.add(EmiStack.of(recipe.cradle()));
        for (CradleRecipe.CradleTribute tribute : recipe.tributes()) {
            lookupInputs.add(EmiStack.of(tribute.item()));
        }
        this.inputs = List.copyOf(lookupInputs);
        this.outputs = recipe.mobs().stream().map(mob -> EmiStack.of(mob.egg())).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EmiCradlePlugin.CRADLE;
    }

    @Override
    public ResourceLocation getId() {
        // 斜杠前缀标记这是不对应原版配方管理器的合成信息页。
        return ResourceLocation.fromNamespaceAndPath(recipe.id().getNamespace(), "/" + recipe.id().getPath());
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 固定显示原初摇篮，其他槽位和 tooltip 由自定义滚动控件处理。
        widgets.addSlot(EmiStack.of(BiomancyJeiPlugin.getCradleStack()), 74, 2).drawBack(true).recipeContext(this);
        widgets.add(new EmiCradleScrollWidget(this, recipe, 0, 0, WIDTH, HEIGHT));
    }
}
