package us.rin.gui.common;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommonUi {

    public static final class UiUtils {

        public static boolean isInside(double mx, double my, double x, double y, double w, double h) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        public static int lightenColor(int color) {
            return adjustColorBrightness(color, 0.2f);
        }

        public static int adjustColorBrightness(int color, float factor) {
            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            r = Math.min(255, (int)(r + (255 - r) * factor));
            g = Math.min(255, (int)(g + (255 - g) * factor));
            b = Math.min(255, (int)(b + (255 - b) * factor));

            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    public static abstract class BaseWidget {
        public final int id;
        public int x, y, width, height;

        protected BaseWidget(int id) {
            this.id = id;
        }

        public void setBounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        protected boolean isMouseOver(double mx, double my) {
            return UiUtils.isInside(mx, my, x, y, width, height);
        }

        protected boolean isMouseOverScrolled(double mx, double my, int scrollOffset) {
            return UiUtils.isInside(mx, my, x, y - scrollOffset, width, height);
        }

        public abstract void render(PoseStack pose, double mx, double my);
        public abstract boolean handleClick(double mx, double my);
    }

    public static final class ChoiceBoxWidget extends BaseWidget {
        private static final int BORDER_RADIUS = 6;
        private static final int TEXT_PADDING = 6;
        private static final int BOX_COLOR = 0xCC323232;
        private static final int ITEM_COLOR = 0xCC282828;
        private static final int TEXT_COLOR = 0xFFFFFFFF;

        public final List<String> items = new ArrayList<>();
        public int selected = 0;
        public boolean open = false;

        public ChoiceBoxWidget(int id, String[] values) {
            super(id);
            Collections.addAll(items, values);
        }

        @Override
        public void render(PoseStack pose, double mx, double my) {
            renderBox(pose);
            if (open) {
                renderDropdownItems(pose, 0);
            }
        }

        public void renderBox(PoseStack pose) {
            RenderUtils.drawRoundedRect(pose, x, y, width, height, BORDER_RADIUS, BOX_COLOR);
            String selectedText = getSelectedItem();
            RenderUtils.drawCustomString(pose, selectedText, x + TEXT_PADDING, y + TEXT_PADDING, TEXT_COLOR, false, 14);
        }

        public void renderOverlayItems(PoseStack pose, int scrollOffset) {
            renderDropdownItems(pose, scrollOffset);
        }

        private void renderDropdownItems(PoseStack pose, int scrollOffset) {
            if (!open) return;

            int drawX = x;
            int drawY = y - scrollOffset + height;

            for (int i = 0; i < items.size(); i++) {
                int itemY = drawY + i * height;
                RenderUtils.drawRoundedRect(pose, drawX, itemY, width, height, 0, 0xFF282828);
                RenderUtils.drawCustomString(pose, items.get(i), drawX + TEXT_PADDING, itemY + TEXT_PADDING, TEXT_COLOR, false, 12);
            }
        }

        @Override
        public boolean handleClick(double mx, double my) {
            return handleClickAbsolute(mx, my);
        }

        public boolean handleClickAbsolute(double mx, double my) {
            if (isMouseOver(mx, my)) {
                toggleOpen();
                return true;
            }

            if (open) {
                return handleDropdownClick(mx, my, 0);
            }

            return false;
        }

        public boolean handleClickScrolled(double mx, double my, int scrollOffset) {
            if (isMouseOverScrolled(mx, my, scrollOffset)) {
                toggleOpen();
                return true;
            }

            if (open) {
                return handleDropdownClick(mx, my, scrollOffset);
            }

            return false;
        }

        private boolean handleDropdownClick(double mx, double my, int scrollOffset) {
            int baseY = y + height;
            for (int i = 0; i < items.size(); i++) {
                int itemY = baseY + i * height - scrollOffset;
                if (UiUtils.isInside(mx, my, x, itemY, width, height)) {
                    selectItem(i);
                    return true;
                }
            }
            return false;
        }

        public void toggleOpen() {
            open = !open;
        }

        private void selectItem(int index) {
            selected = index;
            open = false;
        }

        public void selectValue(String value) {
            int index = indexOf(value);
            if (index >= 0) {
                selected = index;
            }
        }

        public String getSelected() {
            return getSelectedItem();
        }

        private String getSelectedItem() {
            if (items.isEmpty()) return "";
            return items.get(Math.max(0, Math.min(selected, items.size() - 1)));
        }

        public int indexOf(String value) {
            if (value == null) return 0;
            for (int i = 0; i < items.size(); i++) {
                if (value.equals(items.get(i))) {
                    return i;
                }
            }
            return 0;
        }
    }
}