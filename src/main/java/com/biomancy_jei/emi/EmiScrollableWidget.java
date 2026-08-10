package com.biomancy_jei.emi;

// 供 EMI 的 RecipeScreen Mixin 转发滚轮与滑块拖拽事件。
public interface EmiScrollableWidget {
    boolean biomancyJei$mouseScrolled(int mouseX, int mouseY, double delta);

    boolean biomancyJei$mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY);

    boolean biomancyJei$mouseReleased(int mouseX, int mouseY, int button);
}
