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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// 定义血肉镰刀掉落分类的固定头部和可滚动刷怪蛋区域。
public final class DespoilRecipeCategory implements IRecipeCategory<DespoilRecipe> {
    public static final RecipeType<DespoilRecipe> RECIPE_TYPE = RecipeType.create(BiomancyJei.MOD_ID, "despoil", DespoilRecipe.class);
    private static final int WIDTH = 164;
    private static final int CONTENT_X = 4;
    public static final int CONTENT_Y = 35;
    private static final int CONTENT_WIDTH = 160;
    public static final int CONTENT_HEIGHT = 101;
    public static final int MIN_CONTENT_HEIGHT = 36;
    private static final int TARGET_COLUMNS = 8;
    private static final int SLOT_SIZE = 18;
    private final IDrawable icon;
    private final ItemStack sickle;
    private final ItemStack despoilBook;

    public DespoilRecipeCategory(IGuiHelper guiHelper, ItemStack sickle, ItemStack despoilBook) {
        this.icon = guiHelper.createDrawableItemStack(sickle);
        this.sickle = sickle;
        this.despoilBook = despoilBook;
    }

    @Override
    public RecipeType<DespoilRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.biomancy_jei.despoil");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return CONTENT_Y + getContentHeight(JeiLayoutHelper.maxRecipeHeight());
    }

    public static int getContentHeight(int availableHeight) {
        return JeiLayoutHelper.fitViewport(availableHeight, CONTENT_Y, MIN_CONTENT_HEIGHT, CONTENT_HEIGHT);
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(DespoilRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DespoilRecipe recipe, IFocusGroup focuses) {
        // 固定展示血肉镰刀输入与当前掉落物输出。
        builder.addInputSlot(20, 14).setStandardSlotBackground().addItemStack(sickle);
        builder.addInputSlot(50, 14).setStandardSlotBackground().addItemStack(despoilBook);
        builder.addOutputSlot(122, 14).setStandardSlotBackground().addItemStack(recipe.drop());

        // 刷怪蛋槽位由滚动控件统一定位并绘制。
        for (ItemStack target : recipe.targets()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0)
                    .setStandardSlotBackground()
                    .addItemStack(target);
        }

        // 为所有刷怪蛋保留 JEI 查询入口。
        if (!recipe.targets().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.targets());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DespoilRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> slots = builder.getRecipeSlots().getSlots();
        List<IRecipeSlotDrawable> targetSlots = slots.size() <= 3
                ? List.of()
                : new ArrayList<>(slots.subList(3, slots.size()));
        DespoilScrollWidget widget = new DespoilScrollWidget(recipe, CONTENT_X, CONTENT_Y, CONTENT_WIDTH, getHeight() - CONTENT_Y, targetSlots);
        builder.addSlottedWidget(widget, targetSlots);
        builder.addInputHandler(widget);
    }

    @Override
    public void draw(DespoilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 绘制固定头部，刷怪蛋列表底板由滚动控件按实际高度绘制。
        Component or = Component.translatable("jei.biomancy_jei.despoil.or");
        guiGraphics.drawString(Minecraft.getInstance().font, or, 44 - Minecraft.getInstance().font.width(or) / 2, 20, 0x404040, false);
    }

    // 计算每个刷怪蛋在滚动内容中的固定网格坐标。
    static List<SlotPlacement> getTargetPlacements(DespoilRecipe recipe) {
        List<SlotPlacement> placements = new ArrayList<>();
        for (int index = 0; index < recipe.targets().size(); index++) {
            placements.add(new SlotPlacement(index % TARGET_COLUMNS * SLOT_SIZE, index / TARGET_COLUMNS * SLOT_SIZE));
        }
        return placements;
    }

    // 根据刷怪蛋总数计算未裁剪内容的高度。
    static int getTargetContentHeight(DespoilRecipe recipe) {
        int rows = Math.max(1, (recipe.targets().size() + TARGET_COLUMNS - 1) / TARGET_COLUMNS);
        return rows * SLOT_SIZE;
    }

    // 绘制 JEI 样式的滚动区域边框。
    static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    record SlotPlacement(int x, int y) {
    }
}
