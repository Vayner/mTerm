package com.enderwolf.mterm.utility;

import net.minecraft.client.gui.FontRenderer;

/**
 * Helper functions for GUI element drawing.
 */
public class GuiHelpers {

    /**
     * Renders the specified text to the screen, center-aligned.
     */
    public static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color, boolean shadows) {
        fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, color, shadows);
    }
}
