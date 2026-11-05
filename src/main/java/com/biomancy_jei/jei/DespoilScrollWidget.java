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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 在 JEI 页面中裁剪并滚动显示生物刷怪蛋槽位。
public final class DespoilScrollWidget implements ISlottedRecipeWidget, IJeiInputHandler {
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int MIN_SCROLL_MARKER_HEIGHT = 14;
    private final List<IRecipeSlotDrawable> contentSlots;
    private final ImmutableRect2i area;
    private final ImmutableRect2i contentsArea;
    private final ImmutableRect2i scrollbarArea;
    private final ScalableDrawable scrollbarMarker;
    private final ScalableDrawable scrollbarBackground;
    private final int contentHeight;
    private final List<DespoilRecipeCategory.SlotPlacement> slotPlacements;
    private double dragOriginY = -1.0D;
    private float scrollOffsetY = 0.0F;

    public DespoilScrollWidget(DespoilRecipe recipe, int x, int y, int width, int height, List<IRecipeSlotDrawable> contentSlots) {
        // 保存滚动区尺寸、槽位和统一计算的内容坐标。
        this.contentSlots = new ArrayList<>(contentSlots);
        this.area = new ImmutableRect2i(x, y, width, height);
        this.contentsArea = new ImmutableRect2i(0, 0, width - SCROLLBAR_EXTRA_WIDTH, height);
        this.scrollbarArea = new ImmutableRect2i(width - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH, height);
        Textures textures = Internal.getTextures();
        this.scrollbarMarker = textures.getScrollbarMarker();
        this.scrollbarBackground = textures.getScrollbarBackground();
        this.contentHeight = DespoilRecipeCategory.getTargetContentHeight(recipe);
        this.slotPlacements = DespoilRecipeCategory.getTargetPlacements(recipe);
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
        // 先绘制滚动条，再在裁剪区域内按滚动偏移绘制槽位。
        DespoilRecipeCategory.drawPanel(guiGraphics, 0, 0, area.width(), area.height());
        scrollbarBackground.draw(guiGraphics, scrollbarArea);
        scrollbarMarker.draw(guiGraphics, getScrollbarMarkerArea());
        PoseStack poseStack = guiGraphics.pose();
        ScreenRectangle scissorArea = MathUtil.transform(contentsArea, poseStack.last().pose());
        guiGraphics.enableScissor(scissorArea.left(), scissorArea.top(), scissorArea.right(), scissorArea.bottom());
        poseStack.pushPose();
        poseStack.translate(0.0D, -getScrollPixels(), 0.0D);
        drawSlots(guiGraphics);
        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        // 将鼠标坐标换算回未滚动内容，用于显示正确的刷怪蛋提示。
        if (!contentsArea.contains(mouseX, mouseY)) {
            return;
        }
        double adjustedMouseY = mouseY + getScrollPixels();
        for (IRecipeSlotDrawable slot : contentSlots) {
            if (slot.isMouseOver(mouseX, adjustedMouseY)) {
                slot.getTooltip(tooltip);
                return;
            }
        }
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        // 将点击位置映射到滚动后的槽位，保留 JEI 原生查询操作。
        if (!contentsArea.contains(mouseX, mouseY)) {
            return Optional.empty();
        }
        double adjustedMouseY = mouseY + getScrollPixels();
        for (IRecipeSlotDrawable slot : contentSlots) {
            if (slot.isMouseOver(mouseX, adjustedMouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(slot, area.x(), area.y() - getScrollPixels()));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean handleInput(double mouseX, double mouseY, IJeiUserInput userInput) {
        // 左键点击滚动条可定位滑块，并开始拖拽。
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
        // 鼠标位于滚动区时消费滚轮，防止 JEI 直接切换页面。
        if (!area.contains(mouseX, mouseY)) {
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
        // 仅在左键拖住滑块时更新滚动位置。
        if (dragOriginY < 0.0D || mouseKey.getValue() != 0) {
            return false;
        }
        moveScrollbarTo(getScrollbarMarkerArea(), mouseY - dragOriginY);
        return true;
    }

    // 按计算好的网格位置绘制当前可见刷怪蛋槽位。
    private void drawSlots(GuiGraphics guiGraphics) {
        int slotCount = Math.min(contentSlots.size(), slotPlacements.size());
        for (int index = 0; index < slotCount; index++) {
            IRecipeSlotDrawable slot = contentSlots.get(index);
            DespoilRecipeCategory.SlotPlacement placement = slotPlacements.get(index);
            slot.setPosition(placement.x(), placement.y());
            slot.draw(guiGraphics);
        }
    }

    // 计算超出视口的可滚动像素。
    private int getHiddenAmount() {
        return Math.max(contentHeight - contentsArea.height(), 0);
    }

    // 将归一化滚动值转换为内容像素偏移。
    private int getScrollPixels() {
        return Math.round((float) getHiddenAmount() * scrollOffsetY);
    }

    // 按内容比例计算滚动滑块高度。
    private int getScrollbarMarkerHeight() {
        int totalSpace = scrollbarArea.height() - 2;
        int markerHeight = Math.round((float) totalSpace * ((float) contentsArea.height() / (float) (contentsArea.height() + getHiddenAmount())));
        return Math.max(markerHeight, MIN_SCROLL_MARKER_HEIGHT);
    }

    // 按当前滚动值计算滑块纵坐标。
    private int getScrollbarMarkerY() {
        int markerHeight = getScrollbarMarkerHeight();
        int totalSpace = scrollbarArea.height() - 2 - markerHeight;
        return scrollbarArea.y() + 1 + Math.round(totalSpace * scrollOffsetY);
    }

    // 取得当前滚动滑块的可点击区域。
    private ImmutableRect2i getScrollbarMarkerArea() {
        return new ImmutableRect2i(scrollbarArea.x() + 1, getScrollbarMarkerY(), scrollbarArea.width() - 2, getScrollbarMarkerHeight());
    }

    // 点击滑轨时将滑块中心移动到鼠标位置。
    private void moveScrollbarCenterTo(ImmutableRect2i markerArea, double centerY) {
        moveScrollbarTo(markerArea, centerY - (double) markerArea.height() / 2.0D);
    }

    // 将滑块位置换算为归一化滚动值。
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
