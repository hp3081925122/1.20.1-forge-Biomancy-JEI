package com.biomancy_jei.emi;

import com.biomancy_jei.jei.BiomancyJeiPlugin;
import com.biomancy_jei.jei.DespoilRecipe;
import com.biomancy_jei.jei.DespoilRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

// 将同一份血肉镰刀掉落数据转换为原生 EMI 信息页面。
public final class EmiDespoilRecipe implements EmiRecipe {
    private static final int WIDTH = 164;
    private static final int CONTENT_X = 4;
    private static final int CONTENT_Y = 35;
    private static final int CONTENT_WIDTH = 160;
    private final DespoilRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public EmiDespoilRecipe(DespoilRecipe recipe) {
        // 将镰刀与所有刷怪蛋作为 EMI 查询输入，并保留当前掉落物为输出。
        this.recipe = recipe;
        List<EmiIngredient> lookupInputs = new ArrayList<>();
        lookupInputs.add(EmiStack.of(BiomancyJeiPlugin.getSickleStack()));
        lookupInputs.add(EmiStack.of(BiomancyJeiPlugin.getDespoilBook()));
        for (var target : recipe.targets()) {
            lookupInputs.add(EmiStack.of(target));
        }
        this.inputs = List.copyOf(lookupInputs);
        this.outputs = List.of(EmiStack.of(recipe.drop()));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EmiDespoilPlugin.DESPOIL;
    }

    @Override
    public Identifier getId() {
        // 使用 synthetic id，避免 EMI 将信息页误判为原版配方。
        Identifier id = recipe.id();
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "/" + id.getPath());
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
        return CONTENT_Y + DespoilRecipeCategory.getContentHeight(EmiLayoutHelper.maxRecipeHeight());
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 固定展示镰刀、附魔书、二选一提示和当前掉落物输出。
        widgets.addSlot(EmiStack.of(BiomancyJeiPlugin.getSickleStack()), 6, 14).drawBack(true).recipeContext(this);
        widgets.addSlot(EmiStack.of(BiomancyJeiPlugin.getDespoilBook()), 42, 14).drawBack(true).recipeContext(this);
        widgets.addText(Component.translatable("jei.biomancy_jei.despoil.or"), 28, 20, 0xFF404040, false);
        widgets.addSlot(EmiStack.of(recipe.drop()), 122, 14).drawBack(true).recipeContext(this);
        widgets.add(new EmiDespoilScrollWidget(this, recipe, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, widgets.getHeight() - CONTENT_Y));
    }
}
