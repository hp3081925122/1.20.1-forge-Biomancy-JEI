package com.biomancy_jei.emi;

import com.biomancy_jei.jei.CradleRecipe;
import com.biomancy_jei.jei.CradleRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.elements.ScalableDrawable;
import mezz.jei.common.gui.textures.Textures;
import mezz.jei.common.util.ImmutableRect2i;

import java.util.ArrayList;
import java.util.List;

// 在 EMI 原初摇篮页面中绘制刷怪蛋、投喂物品和对应的动态 tooltip。
public final class EmiCradleScrollWidget extends Widget implements EmiScrollableWidget {
    private static final int SCROLLBAR_WIDTH = 14;
    private static final int SCROLLBAR_EXTRA_WIDTH = 16;
    private static final int MIN_SCROLL_MARKER_HEIGHT = 14;
    private final EmiRecipe recipe;
    private final CradleRecipe cradleRecipe;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int tributeHeight;
    private final int contentHeight;
    private final ScalableDrawable scrollbarMarker;
    private final ScalableDrawable scrollbarBackground;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public EmiCradleScrollWidget(EmiRecipe recipe, CradleRecipe cradleRecipe, int x, int y, int width, int height) {
        // 保存固定页面尺寸和投喂物品的隐藏内容高度。
        this.recipe = recipe;
        this.cradleRecipe = cradleRecipe;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tributeHeight = height - CradleRecipeCategory.TRIBUTE_Y;
        Textures textures = Internal.getTextures();
        this.scrollbarMarker = textures.getScrollbarMarker();
        this.scrollbarBackground = textures.getScrollbarBackground();
        this.contentHeight = Math.max(tributeHeight, CradleRecipeCategory.getTributeContentHeight(cradleRecipe.tributes().size()));
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 绘制标签、面板和固定的生物刷怪蛋。
        CradleRecipeCategory.drawPanel(guiGraphics, CradleRecipeCategory.TRIBUTE_X, CradleRecipeCategory.TRIBUTE_Y, CradleRecipeCategory.TRIBUTE_WIDTH, tributeHeight);
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        Component activation = Component.translatable("jei.biomancy_jei.cradle.activation");
        Component output = Component.translatable("jei.biomancy_jei.cradle.output");
        Component tributes = Component.translatable("jei.biomancy_jei.cradle.tributes");
        CradleRecipeCategory.drawCenteredWrappedText(guiGraphics, font, activation, CradleRecipeCategory.ACTIVATION_Y, 0x404040);
        guiGraphics.drawString(font, output, width / 2 - font.width(output) / 2, CradleRecipeCategory.OUTPUT_Y, 0x404040, false);
        guiGraphics.drawString(font, tributes, width / 2 - font.width(tributes) / 2, CradleRecipeCategory.TRIBUTES_Y, 0x404040, false);
        List<CradleRecipeCategory.SlotPlacement> mobPlacements = CradleRecipeCategory.getMobPlacements(cradleRecipe.mobs().size());
        for (int index = 0; index < cradleRecipe.mobs().size() && index < mobPlacements.size(); index++) {
            CradleRecipeCategory.SlotPlacement placement = mobPlacements.get(index);
            new SlotWidget(EmiStack.of(cradleRecipe.mobs().get(index).egg()), CradleRecipeCategory.MOB_X + placement.x(), CradleRecipeCategory.MOB_Y + placement.y())
                    .drawBack(true)
                    .recipeContext(recipe)
                    .render(guiGraphics, mouseX, mouseY, delta);
        }

        // 裁剪投喂物品网格，避免滚动内容越过下方区域。
        Matrix4f matrix = guiGraphics.pose().last().pose();
        int screenX = Math.round(matrix.m30());
        int screenY = Math.round(matrix.m31());
        guiGraphics.enableScissor(screenX + CradleRecipeCategory.TRIBUTE_X + 1, screenY + CradleRecipeCategory.TRIBUTE_Y + 1, screenX + CradleRecipeCategory.TRIBUTE_X + CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_EXTRA_WIDTH, screenY + CradleRecipeCategory.TRIBUTE_Y + tributeHeight - 1);
        List<CradleRecipeCategory.SlotPlacement> tributePlacements = CradleRecipeCategory.getTributePlacements(cradleRecipe.tributes().size());
        for (int index = 0; index < cradleRecipe.tributes().size() && index < tributePlacements.size(); index++) {
            int slotX = CradleRecipeCategory.TRIBUTE_X + tributePlacements.get(index).x();
            int slotY = CradleRecipeCategory.TRIBUTE_Y + tributePlacements.get(index).y() - scrollOffset;
            if (slotY + 18 < CradleRecipeCategory.TRIBUTE_Y || slotY > CradleRecipeCategory.TRIBUTE_Y + tributeHeight) {
                continue;
            }
            new SlotWidget(EmiStack.of(cradleRecipe.tributes().get(index).item()), slotX, slotY)
                    .drawBack(true)
                    .recipeContext(recipe)
                    .render(guiGraphics, mouseX, mouseY, delta);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        // 先判断刷怪蛋，再将投喂物品的数值修正追加到原生 tooltip。
        int mobIndex = getMobIndex(mouseX, mouseY);
        if (mobIndex >= 0) {
            CradleRecipeCategory.SlotPlacement placement = CradleRecipeCategory.getMobPlacements(cradleRecipe.mobs().size()).get(mobIndex);
            List<ClientTooltipComponent> tooltip = new ArrayList<>(new SlotWidget(EmiStack.of(cradleRecipe.mobs().get(mobIndex).egg()), CradleRecipeCategory.MOB_X + placement.x(), CradleRecipeCategory.MOB_Y + placement.y()).drawBack(true).recipeContext(recipe).getTooltip(mouseX, mouseY));
            tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.condition.title").withStyle(ChatFormatting.GRAY)));
            tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.condition.header").withStyle(ChatFormatting.GRAY)));
            tooltip.add(line(Component.translatable(cradleRecipe.mobs().get(mobIndex).condition().translationKey()).withStyle(ChatFormatting.GRAY)));
            return tooltip;
        }
        int tributeIndex = getTributeIndex(mouseX, mouseY);
        if (tributeIndex < 0) {
            return List.of();
        }
        CradleRecipeCategory.SlotPlacement placement = CradleRecipeCategory.getTributePlacements(cradleRecipe.tributes().size()).get(tributeIndex);
        int slotX = CradleRecipeCategory.TRIBUTE_X + placement.x();
        int slotY = CradleRecipeCategory.TRIBUTE_Y + placement.y() - scrollOffset;
        List<ClientTooltipComponent> tooltip = new ArrayList<>(new SlotWidget(EmiStack.of(cradleRecipe.tributes().get(tributeIndex).item()), slotX, slotY).drawBack(true).recipeContext(recipe).getTooltip(mouseX, mouseY));
        addTributeTooltip(tooltip, cradleRecipe.tributes().get(tributeIndex));
        return tooltip;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        // 左键点击滑轨时开始拖拽，否则把点击交给对应物品槽位。
        if (button == 0 && contentHeight > tributeHeight && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollbarGrabOffset = mouseY >= scrollbarThumbY() && mouseY < scrollbarThumbY() + scrollbarThumbHeight()
                    ? mouseY - scrollbarThumbY()
                    : scrollbarThumbHeight() / 2;
            updateScrollFromMouse(mouseY);
            return true;
        }
        int mobIndex = getMobIndex(mouseX, mouseY);
        if (mobIndex >= 0) {
            CradleRecipeCategory.SlotPlacement placement = CradleRecipeCategory.getMobPlacements(cradleRecipe.mobs().size()).get(mobIndex);
            return new SlotWidget(EmiStack.of(cradleRecipe.mobs().get(mobIndex).egg()), CradleRecipeCategory.MOB_X + placement.x(), CradleRecipeCategory.MOB_Y + placement.y()).drawBack(true).recipeContext(recipe).mouseClicked(mouseX, mouseY, button);
        }
        int tributeIndex = getTributeIndex(mouseX, mouseY);
        if (tributeIndex < 0) {
            return false;
        }
        CradleRecipeCategory.SlotPlacement placement = CradleRecipeCategory.getTributePlacements(cradleRecipe.tributes().size()).get(tributeIndex);
        int slotX = CradleRecipeCategory.TRIBUTE_X + placement.x();
        int slotY = CradleRecipeCategory.TRIBUTE_Y + placement.y() - scrollOffset;
        return new SlotWidget(EmiStack.of(cradleRecipe.tributes().get(tributeIndex).item()), slotX, slotY).drawBack(true).recipeContext(recipe).mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean biomancyJei$mouseScrolled(int mouseX, int mouseY, double delta) {
        // 只在投喂物品视口内消费滚轮事件。
        if (!new Bounds(CradleRecipeCategory.TRIBUTE_X, CradleRecipeCategory.TRIBUTE_Y, CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_EXTRA_WIDTH, tributeHeight).contains(mouseX, mouseY)) {
            return false;
        }
        if (contentHeight > tributeHeight) {
            scrollOffset = clamp(scrollOffset - (int) Math.signum(delta) * 18, 0, contentHeight - tributeHeight);
        }
        return true;
    }

    @Override
    public boolean biomancyJei$mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        // 拖拽期间允许鼠标离开滚动条，以保持滑块连续移动。
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        updateScrollFromMouse(mouseY);
        return true;
    }

    @Override
    public boolean biomancyJei$mouseReleased(int mouseX, int mouseY, int button) {
        // 左键释放后清理滑块拖动状态。
        if (!draggingScrollbar || button != 0) {
            return false;
        }
        draggingScrollbar = false;
        return true;
    }

    // 根据鼠标位置取得固定生物刷怪蛋索引。
    private int getMobIndex(int mouseX, int mouseY) {
        if (mouseY < CradleRecipeCategory.MOB_Y || mouseY >= CradleRecipeCategory.MOB_Y + CradleRecipeCategory.MOB_HEIGHT) {
            return -1;
        }
        List<CradleRecipeCategory.SlotPlacement> placements = CradleRecipeCategory.getMobPlacements(cradleRecipe.mobs().size());
        for (int index = 0; index < placements.size(); index++) {
            CradleRecipeCategory.SlotPlacement placement = placements.get(index);
            int slotX = CradleRecipeCategory.MOB_X + placement.x();
            if (mouseX >= slotX && mouseX < slotX + 18) {
                return index;
            }
        }
        return -1;
    }

    // 根据滚动偏移取得当前鼠标下的投喂物品索引。
    private int getTributeIndex(int mouseX, int mouseY) {
        if (mouseX < CradleRecipeCategory.TRIBUTE_X || mouseX >= CradleRecipeCategory.TRIBUTE_X + CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_EXTRA_WIDTH || mouseY < CradleRecipeCategory.TRIBUTE_Y || mouseY >= CradleRecipeCategory.TRIBUTE_Y + tributeHeight) {
            return -1;
        }
        int column = (mouseX - CradleRecipeCategory.TRIBUTE_X) / 18;
        if (column >= CradleRecipeCategory.COLUMNS) {
            return -1;
        }
        int row = (mouseY - CradleRecipeCategory.TRIBUTE_Y + scrollOffset) / 18;
        int index = row * CradleRecipeCategory.COLUMNS + column;
        return index >= 0 && index < cradleRecipe.tributes().size() ? index : -1;
    }

    // 添加投喂物品的六项数值 tooltip。
    private static void addTributeTooltip(List<ClientTooltipComponent> tooltip, CradleRecipe.CradleTribute tribute) {
        tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.modifiers").withStyle(ChatFormatting.GRAY)));
        if (tribute.biomass() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.biomass", signed(tribute.biomass())).withStyle(ChatFormatting.GRAY)));
        if (tribute.lifeEnergy() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.life_energy", signed(tribute.lifeEnergy())).withStyle(ChatFormatting.GRAY)));
        if (tribute.success() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.success", signed(tribute.success())).withStyle(ChatFormatting.GRAY)));
        if (tribute.hostile() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.hostile", signed(tribute.hostile())).withStyle(ChatFormatting.GRAY)));
        if (tribute.anomaly() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.anomaly", signed(tribute.anomaly())).withStyle(ChatFormatting.GRAY)));
        if (tribute.disease() != 0) tooltip.add(line(Component.translatable("jei.biomancy_jei.cradle.tribute.disease", signed(tribute.disease())).withStyle(ChatFormatting.GRAY)));
    }

    // 将 Minecraft 文本组件转为 EMI tooltip 组件。
    private static ClientTooltipComponent line(Component component) {
        return ClientTooltipComponent.create(component.getVisualOrderText());
    }

    // 统一显示属性修正的正负号。
    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    // 使用 JEI 原生滚动条材质绘制滑轨和滑块。
    private void drawScrollbar(GuiGraphics guiGraphics) {
        scrollbarBackground.draw(guiGraphics, new ImmutableRect2i(CradleRecipeCategory.TRIBUTE_X + CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_WIDTH, CradleRecipeCategory.TRIBUTE_Y, SCROLLBAR_WIDTH, tributeHeight));
        scrollbarMarker.draw(guiGraphics, new ImmutableRect2i(CradleRecipeCategory.TRIBUTE_X + CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_WIDTH + 1, scrollbarThumbY(), SCROLLBAR_WIDTH - 2, scrollbarThumbHeight()));
    }

    // 判断鼠标是否位于滚动条轨道。
    private boolean isOverScrollbar(int mouseX, int mouseY) {
        int barX = CradleRecipeCategory.TRIBUTE_X + CradleRecipeCategory.TRIBUTE_WIDTH - SCROLLBAR_WIDTH;
        return mouseX >= barX && mouseX < barX + SCROLLBAR_WIDTH && mouseY >= CradleRecipeCategory.TRIBUTE_Y && mouseY < CradleRecipeCategory.TRIBUTE_Y + tributeHeight;
    }

    // 取得滚动条轨道高度。
    private int scrollbarTrackHeight() {
        return tributeHeight - 2;
    }

    // 按视口与内容比例计算滑块高度。
    private int scrollbarThumbHeight() {
        return Math.min(scrollbarTrackHeight(), Math.max(MIN_SCROLL_MARKER_HEIGHT, tributeHeight * scrollbarTrackHeight() / contentHeight));
    }

    // 按当前滚动偏移计算滑块纵坐标。
    private int scrollbarThumbY() {
        return CradleRecipeCategory.TRIBUTE_Y + 1 + scrollOffset * (scrollbarTrackHeight() - scrollbarThumbHeight()) / Math.max(1, contentHeight - tributeHeight);
    }

    // 将拖拽位置换算成内容滚动偏移。
    private void updateScrollFromMouse(int mouseY) {
        int trackY = CradleRecipeCategory.TRIBUTE_Y + 1;
        int availableTrack = Math.max(1, scrollbarTrackHeight() - scrollbarThumbHeight());
        int thumbTop = clamp(mouseY - scrollbarGrabOffset, trackY, trackY + availableTrack);
        scrollOffset = clamp((thumbTop - trackY) * (contentHeight - tributeHeight) / availableTrack, 0, Math.max(0, contentHeight - tributeHeight));
    }

    // 将整数值限制在闭区间内。
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
