package com.biomancy_jei.mixin.emi;

import com.biomancy_jei.emi.EmiScrollableWidget;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// 将 EMI 配方页面的滚轮与滑块拖拽事件转发给本模组滚动控件。
@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {
    @Shadow
    private List<WidgetGroup> currentPage;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void biomancyJei$clickRecipeWidgets(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // 将屏幕坐标转换为 WidgetGroup 本地坐标并转发点击。
        if (currentPage == null) {
            return;
        }
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        for (WidgetGroup group : currentPage) {
            if (!new Bounds(group.x(), group.y(), group.getWidth(), group.getHeight()).contains(screenX, screenY)) {
                continue;
            }
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget
                        && widget.getBounds().contains(localX, localY)
                        && widget.mouseClicked(localX, localY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void biomancyJei$scrollRecipeWidgets(double mouseX, double mouseY, double horizontalAmount, double amount, CallbackInfoReturnable<Boolean> cir) {
        // 仅让鼠标所在的 Biomancy JEI 滚动控件消费滚轮事件。
        if (currentPage == null) {
            return;
        }
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        for (WidgetGroup group : currentPage) {
            if (!new Bounds(group.x(), group.y(), group.getWidth(), group.getHeight()).contains(screenX, screenY)) {
                continue;
            }
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable
                        && scrollable.biomancyJei$mouseScrolled(localX, localY, amount)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void biomancyJei$dragRecipeWidgets(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        // 拖拽时不限制鼠标必须位于控件范围，确保滑块可连续拖动。
        if (currentPage == null) {
            return;
        }
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable
                        && scrollable.biomancyJei$mouseDragged(localX, localY, button, dragX, dragY)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void biomancyJei$releaseRecipeWidgets(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // 左键释放时清理所有正在拖拽的 Biomancy JEI 滑块状态。
        if (currentPage == null) {
            return;
        }
        int screenX = (int) mouseX;
        int screenY = (int) mouseY;
        for (WidgetGroup group : currentPage) {
            int localX = screenX - group.x();
            int localY = screenY - group.y();
            for (Widget widget : group.widgets) {
                if (widget instanceof EmiScrollableWidget scrollable
                        && scrollable.biomancyJei$mouseReleased(localX, localY, button)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
