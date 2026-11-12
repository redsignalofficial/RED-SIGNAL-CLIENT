package us.rin.gui.clickgui.utils;

public enum Section {
    HOMEPAGE("🏠 Home Page"),
    CRASHERS("🔥 Crashers"),
    EXPLOITS("💣 Exploits"),
    BOTS("👥 Bots"),
    SCRIPTS("✨ Scripts"),
    STYLES("🎨 Styles");

    private final String displayName;

    Section(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}