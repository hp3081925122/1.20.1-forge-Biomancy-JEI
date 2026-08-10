package com.biomancy_jei.emi;

import com.biomancy_jei.jei.DespoilRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Matrix4f;

import java.util.List;

// 在 EMI 信息页中裁剪、滚动并交互显示生物刷怪蛋。
public final class EmiDespoilScrollWidget extends Widget implements EmiScrollableWidget {
    private static final int SLOT_SIZE = 18;
    private static final int TARGET_COLUMNS = 8;
    private final EmiRecipe recipe;
    private final DespoilRecipe despoilRecipe;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int contentHeight;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public EmiDespoilScrollWidget(EmiRecipe recipe, DespoilRecipe despoilRecipe, int x, int y, int width, int height) {
        // 保存固定视口与掉落物对应的所有刷怪蛋。
        this.recipe = recipe;
        this.despoilRecipe = despoilRecipe;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        int rows = Math.max(1, (despoilRecipe.targets().size() + TARGET_COLUMNS - 1) / TARGET_COLUMNS);
        this.contentHeight = Math.max(height, rows * SLOT_SIZE);
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 裁剪生物网格，并使用屏幕坐标裁剪以适配 EMI 的 WidgetGroup 平移。
        drawPanel(guiGraphics);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        int screenX = Math.round(matrix.m30());
        int screenY = Math.round(matrix.m31());
        guiGraphics.enableScissor(screenX + x + 1, screenY + y + 1, screenX + x + width - 14, screenY + y + height - 1);
        for (int index = 0; index < despoilRecipe.targets().size(); index++) {
            int slotX = x + index % TARGET_COLUMNS * SLOT_SIZE;
            int slotY = y + index / TARGET_COLUMNS * SLOT_SIZE - scrollOffset;
            if (slotY + SLOT_SIZE < y || slotY > y + height) {
                continue;
            }
            new SlotWidget(EmiStack.of(despoilRecipe.targets().get(index)), slotX, slotY)
                    .drawBack(true)
                    .recipeContext(recipe)
                    .render(guiGraphics, mouseX, mouseY, delta);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        // 使用 EMI 原生槽位生成刷怪蛋提示。
        int targetIndex = getTargetIndex(mouseX, mouseY);
        if (targetIndex < 0) {
            return List.of();
        }
        int slotX = x + targetIndex % TARGET_COLUMNS * SLOT_SIZE;
        int slotY = y + targetIndex / TARGET_COLUMNS * SLOT_SIZE - scrollOffset;
        return new SlotWidget(EmiStack.of(despoilRecipe.targets().get(targetIndex)), slotX, slotY)
                .drawBack(true)
                .recipeContext(recipe)
                .getTooltip(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        // 左键点击滑轨或滑块时开始滚动条拖拽。
        if (button == 0 && contentHeight > height && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollbarGrabOffset = mouseY >= scrollbarThumbY() && mouseY < scrollbarThumbY() + scrollbarThumbHeight()
                    ? mouseY - scrollbarThumbY()
                    : scrollbarThumbHeight() / 2;
            updateScrollFromMouse(mouseY);
            return true;
        }
        int targetIndex = getTargetIndex(mouseX, mouseY);
        if (targetIndex < 0) {
            return false;
        }
        int slotX = x + targetIndex % TARGET_COLUMNS * SLOT_SIZE;
        int slotY = y + targetIndex / TARGET_COLUMNS * SLOT_SIZE - scrollOffset;
        return new SlotWidget(EmiStack.of(despoilRecipe.targets().get(targetIndex)), slotX, slotY)
                .drawBack(true)
                .recipeContext(recipe)
                .mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean biomancyJei$mouseScrolled(int mouseX, int mouseY, double delta) {
        // 鼠标位于视口且内容可滚动时消费滚轮，避免 EMI 切换配方页。
        if (!getBounds().contains(mouseX, mouseY) || contentHeight <= height) {
            return false;
        }
        scrollOffset = clamp(scrollOffset - (int) Math.signum(delta) * SLOT_SIZE, 0, contentHeight - height);
        return true;
    }

    @Override
    public boolean biomancyJei$mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        // 拖拽过程中即使鼠标离开视口也继续更新滑块。
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        updateScrollFromMouse(mouseY);
        return true;
    }

    @Override
    public boolean biomancyJei$mouseReleased(int mouseX, int mouseY, int button) {
        // 左键释放后结束滑块拖拽状态。
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        draggingScrollbar = false;
        return true;
    }

    // 将鼠标坐标映射为当前可见的刷怪蛋索引。
    private int getTargetIndex(int mouseX, int mouseY) {
        if (!getBounds().contains(mouseX, mouseY) || mouseX >= x + width - 14) {
            return -1;
        }
        int contentX = mouseX - x;
        int contentY = mouseY - y + scrollOffset;
        int column = contentX / SLOT_SIZE;
        int row = contentY / SLOT_SIZE;
        int targetIndex = row * TARGET_COLUMNS + column;
        return targetIndex >= 0 && targetIndex < despoilRecipe.targets().size() ? targetIndex : -1;
    }

    // 绘制与 JEI 页面一致的滚动区域边框。
    private void drawPanel(GuiGraphics guiGraphics) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFE3E3E3);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFF8F8F8);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFFF8F8F8);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF8A8A8A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF8A8A8A);
    }

    // 绘制固定滑轨和按内容比例变化的滑块。
    private void drawScrollbar(GuiGraphics guiGraphics) {
        int barX = x + width - 12;
        guiGraphics.fill(barX, y + 4, barX + 8, y + height - 4, 0xFFB5B5B5);
        if (contentHeight <= height) {
            guiGraphics.fill(barX + 1, y + 5, barX + 7, y + height - 5, 0xFFE0E0E0);
            return;
        }
        guiGraphics.fill(barX + 1, scrollbarThumbY(), barX + 7, scrollbarThumbY() + scrollbarThumbHeight(), 0xFF8A8A8A);
    }

    // 判断鼠标是否位于滚动滑轨内。
    private boolean isOverScrollbar(int mouseX, int mouseY) {
        int barX = x + width - 12;
        return mouseX >= barX && mouseX < barX + 8 && mouseY >= y + 4 && mouseY < y + height - 4;
    }

    // 取得滚动滑轨的像素高度。
    private int scrollbarTrackHeight() {
        return height - 10;
    }

    // 按视口与内容比例计算滑块高度。
    private int scrollbarThumbHeight() {
        return Math.max(16, height * scrollbarTrackHeight() / contentHeight);
    }

    // 按当前滚动偏移计算滑块纵坐标。
    private int scrollbarThumbY() {
        return y + 5 + scrollOffset * (scrollbarTrackHeight() - scrollbarThumbHeight()) / Math.max(1, contentHeight - height);
    }

    // 将拖拽位置映射为内容滚动偏移。
    private void updateScrollFromMouse(int mouseY) {
        int trackY = y + 5;
        int availableTrack = Math.max(1, scrollbarTrackHeight() - scrollbarThumbHeight());
        int thumbTop = clamp(mouseY - scrollbarGrabOffset, trackY, trackY + availableTrack);
        scrollOffset = clamp((thumbTop - trackY) * (contentHeight - height) / availableTrack, 0, contentHeight - height);
    }

    // 将数值限制在给定范围。
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
