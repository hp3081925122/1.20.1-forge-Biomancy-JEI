package com.biomancy_jei.jei;

import com.biomancy_jei.BiomancyJei;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// 定义原初摇篮页面的固定布局和滚动区域。
public final class CradleRecipeCategory implements IRecipeCategory<CradleRecipe> {
    public static final RecipeType<CradleRecipe> RECIPE_TYPE = RecipeType.create(BiomancyJei.MOD_ID, "primordial_cradle", CradleRecipe.class);
    public static final int WIDTH = 164;
    public static final int ACTIVATION_Y = 20;
    public static final int OUTPUT_Y = 42;
    public static final int MOB_X = 4;
    public static final int MOB_Y = 53;
    public static final int MOB_WIDTH = 160;
    public static final int MOB_HEIGHT = 18;
    public static final int TRIBUTE_X = 4;
    public static final int TRIBUTES_Y = 73;
    public static final int TRIBUTE_Y = 83;
    public static final int TRIBUTE_WIDTH = 160;
    public static final int TRIBUTE_HEIGHT = 97;
    public static final int MIN_TRIBUTE_HEIGHT = 36;
    public static final int ACTIVATION_MAX_LINES = 2;
    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 8;
    private final IDrawable icon;
    private final ItemStack cradle;

    public CradleRecipeCategory(IGuiHelper guiHelper, ItemStack cradle) {
        this.icon = guiHelper.createDrawableItemStack(cradle);
        this.cradle = cradle;
    }

    @Override
    public RecipeType<CradleRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biomancy_jei.cradle");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return TRIBUTE_Y + getTributeHeight(JeiLayoutHelper.maxRecipeHeight());
    }

    public static int getTributeHeight(int availableHeight) {
        return JeiLayoutHelper.fitViewport(availableHeight, TRIBUTE_Y, MIN_TRIBUTE_HEIGHT, TRIBUTE_HEIGHT);
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(CradleRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CradleRecipe recipe, IFocusGroup focuses) {
        // 固定展示原初摇篮，并为生物和投喂物品创建由滚动控件定位的槽位。
        builder.addInputSlot(74, 2).setStandardSlotBackground().addItemStack(cradle);
        for (CradleRecipe.CradleMob mob : recipe.mobs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 0, 0).setStandardSlotBackground().addItemStack(mob.egg());
        }
        for (CradleRecipe.CradleTribute tribute : recipe.tributes()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 0, 0).setStandardSlotBackground().addItemStack(tribute.item());
        }

        // 将所有可查询的生物刷怪蛋和投喂物品加入 JEI 搜索入口。
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.cradle());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.mobs().stream().map(CradleRecipe.CradleMob::egg).toList());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.tributes().stream().map(CradleRecipe.CradleTribute::item).toList());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CradleRecipe recipe, IFocusGroup focuses) {
        // 按固定槽位顺序拆分摇篮、生物刷怪蛋和投喂物品，交给同一个滚动控件处理 tooltip 与查询。
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots();
        int mobStart = 1;
        int mobEnd = Math.min(mobStart + recipe.mobs().size(), slots.size());
        int tributeEnd = Math.min(mobEnd + recipe.tributes().size(), slots.size());
        List<IRecipeSlotDrawable> mobSlots = new ArrayList<>(slots.subList(mobStart, mobEnd));
        List<IRecipeSlotDrawable> tributeSlots = new ArrayList<>(slots.subList(mobEnd, tributeEnd));
        int height = getHeight();
        CradleRecipeWidget widget = new CradleRecipeWidget(recipe, height, TRIBUTE_X, TRIBUTE_Y, TRIBUTE_WIDTH, height - TRIBUTE_Y, mobSlots, tributeSlots);
        List<IRecipeSlotDrawable> allSlots = new ArrayList<>(mobSlots);
        allSlots.addAll(tributeSlots);
        builder.addSlottedWidget(widget, allSlots);
        builder.addInputHandler(widget);
    }

    @Override
    public void draw(CradleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 绘制两段说明文字，投喂物品底板由滚动控件按实际高度绘制。
        Minecraft minecraft = Minecraft.getInstance();
        Component activation = Component.translatable("jei.biomancy_jei.cradle.activation");
        Component output = Component.translatable("jei.biomancy_jei.cradle.output");
        Component tributes = Component.translatable("jei.biomancy_jei.cradle.tributes");
        drawCenteredWrappedText(guiGraphics, minecraft.font, activation, ACTIVATION_Y, 0x404040);
        guiGraphics.drawString(minecraft.font, output, WIDTH / 2 - minecraft.font.width(output) / 2, OUTPUT_Y, 0x404040, false);
        guiGraphics.drawString(minecraft.font, tributes, WIDTH / 2 - minecraft.font.width(tributes) / 2, TRIBUTES_Y, 0x404040, false);
    }

    // 计算生物刷怪蛋居中的一行槽位坐标。
    public static List<SlotPlacement> getMobPlacements(int count) {
        List<SlotPlacement> placements = new ArrayList<>();
        int startX = Math.max(0, (MOB_WIDTH - count * SLOT_SIZE) / 2);
        for (int index = 0; index < count; index++) {
            placements.add(new SlotPlacement(startX + index * SLOT_SIZE, 0));
        }
        return placements;
    }

    // 计算投喂物品网格坐标，滚动控件会将这些坐标映射到视口。
    public static List<SlotPlacement> getTributePlacements(int count) {
        List<SlotPlacement> placements = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            placements.add(new SlotPlacement(index % COLUMNS * SLOT_SIZE, index / COLUMNS * SLOT_SIZE));
        }
        return placements;
    }

    // 根据投喂物品数量计算未裁剪内容高度。
    public static int getTributeContentHeight(int count) {
        int rows = Math.max(1, (count + COLUMNS - 1) / COLUMNS);
        return rows * SLOT_SIZE;
    }

    // 绘制 JEI 风格的内容面板边框。
    public static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    // 在 JEI 与 EMI 页面宽度内自动换行并居中绘制说明文字。
    public static void drawCenteredWrappedText(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, Component text, int y, int color) {
        List<FormattedCharSequence> lines = font.split(text, WIDTH - 4);
        for (int index = 0; index < Math.min(lines.size(), ACTIVATION_MAX_LINES); index++) {
            FormattedCharSequence line = lines.get(index);
            guiGraphics.drawString(font, line, WIDTH / 2 - font.width(line) / 2, y + index * 10, color, false);
        }
    }

    public record SlotPlacement(int x, int y) {
    }
}
