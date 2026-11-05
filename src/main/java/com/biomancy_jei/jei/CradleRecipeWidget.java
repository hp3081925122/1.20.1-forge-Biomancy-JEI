package com.biomancy_jei.jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.ScalableDrawable;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.common.util.MathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 在 JEI 原初摇篮页面中绘制生物结果和可滚动的投喂物品。
public final class CradleRecipeWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int MIN_SCROLL_MARKER_HEIGHT = 14;
    private final List<IRecipeSlotDrawable> mobSlots;
    private final List<IRecipeSlotDrawable> tributeSlots;
    private final List<CradleRecipe.CradleMob> mobs;
    private final List<CradleRecipe.CradleTribute> tributes;
    private final ImmutableRect2i area;
    private final ImmutableRect2i tributeContentsArea;
    private final ImmutableRect2i scrollbarArea;
    private final ScalableDrawable scrollbarMarker;
    private final ScalableDrawable scrollbarBackground;
    private final int contentHeight;
    private final List<CradleRecipeCategory.SlotPlacement> mobPlacements;
    private final List<CradleRecipeCategory.SlotPlacement> tributePlacements;
    private double dragOriginY = -1.0D;
    private float scrollOffsetY;

    private final ImmutableRect2i tributePanelArea;

    public CradleRecipeWidget(CradleRecipe recipe, int height, int tributeX, int tributeY, int tributeWidth, int tributeHeight, List<IRecipeSlotDrawable> mobSlots, List<IRecipeSlotDrawable> tributeSlots) {
        // 保存两个区域的槽位，并让滚动条只控制投喂物品区域。
        this.mobSlots = new ArrayList<>(mobSlots);
        this.tributeSlots = new ArrayList<>(tributeSlots);
        this.mobs = recipe.mobs();
        this.tributes = recipe.tributes();
        this.area = new ImmutableRect2i(0, 0, CradleRecipeCategory.WIDTH, height);
        this.tributePanelArea = new ImmutableRect2i(tributeX, tributeY, tributeWidth, tributeHeight);
        this.tributeContentsArea = new ImmutableRect2i(tributeX, tributeY, tributeWidth - SCROLLBAR_EXTRA_WIDTH, tributeHeight);
        this.scrollbarArea = new ImmutableRect2i(tributeX + tributeWidth - SCROLLBAR_WIDTH, tributeY, SCROLLBAR_WIDTH, tributeHeight);
        Textures textures = Internal.getTextures();
        this.scrollbarMarker = textures.getScrollbarMarker();
        this.scrollbarBackground = textures.getScrollbarBackground();
        this.contentHeight = CradleRecipeCategory.getTributeContentHeight(tributes.size());
        this.mobPlacements = CradleRecipeCategory.getMobPlacements(mobSlots.size());
        this.tributePlacements = CradleRecipeCategory.getTributePlacements(tributeSlots.size());
        for (int index = 0; index < mobSlots.size() && index < mobPlacements.size(); index++) {
            CradleRecipeCategory.SlotPlacement placement = mobPlacements.get(index);
            mobSlots.get(index).setPosition(CradleRecipeCategory.MOB_X + placement.x(), CradleRecipeCategory.MOB_Y + placement.y());
        }
    }

    @Override
    public ScreenPosition getPosition() {
        return area.getScreenPosition();
    }

    @Override
    public ScreenRectangle getArea() {
        return area.toScreenRectangle();
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 先绘制固定的生物刷怪蛋，再裁剪并绘制投喂物品网格。
        CradleRecipeCategory.drawPanel(guiGraphics, tributePanelArea.x(), tributePanelArea.y(), tributePanelArea.width(), tributePanelArea.height());
        for (int index = 0; index < mobSlots.size() && index < mobPlacements.size(); index++) {
            IRecipeSlotDrawable slot = mobSlots.get(index);
            CradleRecipeCategory.SlotPlacement placement = mobPlacements.get(index);
            slot.setPosition(CradleRecipeCategory.MOB_X + placement.x(), CradleRecipeCategory.MOB_Y + placement.y());
            slot.draw(guiGraphics);
        }
        scrollbarBackground.draw(guiGraphics, scrollbarArea);
        scrollbarMarker.draw(guiGraphics, getScrollbarMarkerArea());
        PoseStack poseStack = guiGraphics.pose();
        ScreenRectangle scissorArea = MathUtil.transform(tributeContentsArea, poseStack.last().pose());
        guiGraphics.enableScissor(scissorArea.left(), scissorArea.top(), scissorArea.right(), scissorArea.bottom());
        poseStack.pushPose();
        poseStack.translate(0.0D, -getScrollPixels(), 0.0D);
        drawTributeSlots(guiGraphics);
        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        // 根据鼠标所在区域追加生物随机条件或投喂属性修正。
        for (int index = 0; index < mobSlots.size(); index++) {
            IRecipeSlotDrawable slot = mobSlots.get(index);
            if (slot.isMouseOver(mouseX, mouseY)) {
                slot.getTooltip(tooltip);
                return;
            }
        }
        if (!tributeContentsArea.contains(mouseX, mouseY)) {
            return;
        }
        double adjustedMouseY = mouseY + getScrollPixels();
        for (int index = 0; index < tributeSlots.size(); index++) {
            IRecipeSlotDrawable slot = tributeSlots.get(index);
            if (slot.isMouseOver(mouseX, adjustedMouseY)) {
                slot.getTooltip(tooltip);
                return;
            }
        }
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        // 将鼠标位置映射到滚动前的槽位坐标，保留 JEI 原生查询行为。
        for (IRecipeSlotDrawable slot : mobSlots) {
            if (slot.isMouseOver(mouseX, mouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(slot, area.x(), area.y()));
            }
        }
        if (!tributeContentsArea.contains(mouseX, mouseY)) {
            return Optional.empty();
        }
        double adjustedMouseY = mouseY + getScrollPixels();
        for (IRecipeSlotDrawable slot : tributeSlots) {
            if (slot.isMouseOver(mouseX, adjustedMouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(slot, area.x(), area.y() - getScrollPixels()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput userInput) {
        // 左键点击投喂区域滚动条时定位滑块并开始拖拽。
        if (!userInput.is(Internal.getKeyMappings().getLeftClick())) {
            return false;
        }
        if (!userInput.isSimulate()) {
            dragOriginY = -1.0D;
        }
        if (!scrollbarArea.contains(mouseX, mouseY) || getHiddenAmount() == 0) {
            return false;
        }
        if (userInput.isSimulate()) {
            ImmutableRect2i markerArea = getScrollbarMarkerArea();
            if (!markerArea.contains(mouseX, mouseY)) {
                moveScrollbarCenterTo(markerArea, mouseY);
                markerArea = getScrollbarMarkerArea();
            }
            dragOriginY = mouseY - markerArea.y();
        }
        return true;
    }

    @Override
    public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        // 鼠标位于投喂物品视口时消费滚轮，避免 JEI 切换到下一页。
        if (!tributeContentsArea.contains(mouseX, mouseY)) {
            return false;
        }
        if (getHiddenAmount() > 0) {
            float scrollAmount = (float) (scrollDeltaY * 18.0D / Math.max(contentHeight, 1));
            scrollOffsetY = Mth.clamp(scrollOffsetY - scrollAmount, 0.0F, 1.0F);
        } else {
            scrollOffsetY = 0.0F;
        }
        return true;
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key mouseKey, double dragX, double dragY) {
        // 只在左键拖动滑块时更新投喂物品滚动位置。
        if (dragOriginY < 0.0D || mouseKey.getValue() != 0) {
            return false;
        }
        moveScrollbarTo(getScrollbarMarkerArea(), mouseY - dragOriginY);
        return true;
    }

    // 绘制投喂物品槽位并使用统一的网格坐标。
    private void drawTributeSlots(GuiGraphics guiGraphics) {
        int slotCount = Math.min(tributeSlots.size(), tributePlacements.size());
        for (int index = 0; index < slotCount; index++) {
            IRecipeSlotDrawable slot = tributeSlots.get(index);
            CradleRecipeCategory.SlotPlacement placement = tributePlacements.get(index);
            slot.setPosition(tributeContentsArea.x() + placement.x(), tributeContentsArea.y() + placement.y());
            slot.draw(guiGraphics);
        }
    }

    // 将投喂物品的六项数值追加到 JEI 原生物品提示中。
    static void addMobTooltip(ITooltipBuilder tooltip, CradleRecipe.CradleMob mob) {
        tooltip.add(Component.translatable("jei.biomancy_jei.cradle.condition.title").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jei.biomancy_jei.cradle.condition.header").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(mob.condition().translationKey()).withStyle(ChatFormatting.GRAY));
    }

    static void addTributeTooltip(ITooltipBuilder tooltip, CradleRecipe.CradleTribute tribute) {
        tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.modifiers").withStyle(ChatFormatting.GRAY));
        if (tribute.biomass() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.biomass", signed(tribute.biomass())).withStyle(ChatFormatting.GRAY));
        if (tribute.lifeEnergy() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.life_energy", signed(tribute.lifeEnergy())).withStyle(ChatFormatting.GRAY));
        if (tribute.success() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.success", signed(tribute.success())).withStyle(ChatFormatting.GRAY));
        if (tribute.hostile() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.hostile", signed(tribute.hostile())).withStyle(ChatFormatting.GRAY));
        if (tribute.anomaly() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.anomaly", signed(tribute.anomaly())).withStyle(ChatFormatting.GRAY));
        if (tribute.disease() != 0) tooltip.add(Component.translatable("jei.biomancy_jei.cradle.tribute.disease", signed(tribute.disease())).withStyle(ChatFormatting.GRAY));
    }

    // 统一为正数显示加号，便于区分增加和减少。
    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    // 计算超出投喂物品视口的内容像素。
    private int getHiddenAmount() {
        return Math.max(contentHeight - tributeContentsArea.height(), 0);
    }

    // 将归一化滚动值转换为投喂物品内容偏移。
    private int getScrollPixels() {
        return Math.round((float) getHiddenAmount() * scrollOffsetY);
    }

    // 按内容比例计算滚动滑块高度。
    private int getScrollbarMarkerHeight() {
        int totalSpace = scrollbarArea.height() - 2;
        int markerHeight = Math.round((float) totalSpace * ((float) tributeContentsArea.height() / (float) (tributeContentsArea.height() + getHiddenAmount())));
        return Math.max(markerHeight, MIN_SCROLL_MARKER_HEIGHT);
    }

    // 按当前滚动值计算滚动滑块纵坐标。
    private int getScrollbarMarkerY() {
        int markerHeight = getScrollbarMarkerHeight();
        int totalSpace = scrollbarArea.height() - 2 - markerHeight;
        return scrollbarArea.y() + 1 + Math.round(totalSpace * scrollOffsetY);
    }

    // 取得当前滚动滑块的点击区域。
    private ImmutableRect2i getScrollbarMarkerArea() {
        return new ImmutableRect2i(scrollbarArea.x() + 1, getScrollbarMarkerY(), scrollbarArea.width() - 2, getScrollbarMarkerHeight());
    }

    // 点击轨道时把滑块中心移动到鼠标位置。
    private void moveScrollbarCenterTo(ImmutableRect2i markerArea, double centerY) {
        moveScrollbarTo(markerArea, centerY - (double) markerArea.height() / 2.0D);
    }

    // 将滑块位置映射为归一化滚动值。
    private void moveScrollbarTo(ImmutableRect2i markerArea, double topY) {
        int minY = scrollbarArea.y();
        int maxY = scrollbarArea.y() + scrollbarArea.height() - markerArea.height();
        int totalSpace = maxY - minY;
        if (totalSpace <= 0) {
            scrollOffsetY = 0.0F;
            return;
        }
        scrollOffsetY = Mth.clamp((float) ((topY - (double) minY) / (double) totalSpace), 0.0F, 1.0F);
    }
}
