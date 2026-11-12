package us.whitedev.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import us.whitedev.helpers.ClientConfigHelper;
import us.whitedev.helpers.AccountHelper;
import us.whitedev.helpers.AccountHelper.SavedAccount;
import us.whitedev.helpers.AccountHelper.AccountType;

import java.util.List;

public class AltManagerScreen extends Screen {
    private final AccountHelper accountHelper = new AccountHelper();
    private AccountList accountList;
    private EditBox nameInput;
    private Button microsoftButton;
    private Button cookieButton;
    private String currentAccount = "Not logged in";

    public AltManagerScreen() {
        super(Component.literal("Alt Manager"));
    }

    @Override
    protected void init() {
        super.init();
        accountHelper.loadAccounts();

        int top = 40;
        int bottom = this.height - 60;
        int listWidth = Math.min(380, this.width - 120);
        int left = (this.width - listWidth) / 2;
        int itemHeight = 36;

        int editWidth = Math.min(300, listWidth);
        int editX = (this.width - editWidth) / 2;
        nameInput = new EditBox(this.font, editX, top, editWidth, 20, Component.literal("Nickname"));
        nameInput.setValue(ClientConfigHelper.LAST_OFFLINE_NICK != null && !ClientConfigHelper.LAST_OFFLINE_NICK.isBlank() ? ClientConfigHelper.LAST_OFFLINE_NICK : "iBuyPower_Xx");
        nameInput.setBordered(true);

        int btnW = (listWidth - 8) / 2;
        int btnH = 20;
        int gapY = 4;
        int y = top + 24;
        int rightX = left + btnW + 8;

        microsoftButton = Button.builder(Component.literal("Add Microsoft Account"), b -> onMicrosoftLogin())
                .bounds(left, y, btnW, btnH).build();

        Button loginButton = Button.builder(Component.literal("Login to Selected"), b -> onLoginSelected())
                .bounds(rightX, y, btnW, btnH).build();

        y += btnH + gapY;

        Button offlineButton = Button.builder(Component.literal("Login Offline"), b -> onOfflineLogin())
                .bounds(left, y, btnW, btnH).build();

        cookieButton = Button.builder(Component.literal("Cookie Login"), b -> onCookieLogin())
                .bounds(rightX, y, btnW, btnH).build();

        y += btnH + gapY;

        Button removeButton = Button.builder(Component.literal("Remove Selected"), b -> onRemoveAccount())
                .bounds(left, y, btnW, btnH).build();

        Button backButton = Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(rightX, y, btnW, btnH).build();

        int listStartTop = y + btnH + 10;

        accountList = new AccountList(this.minecraft, listWidth, this.height, listStartTop, bottom, itemHeight);
        accountList.setLeftPos(left);

        this.addRenderableWidget(accountList);
        this.addRenderableWidget(nameInput);
        this.addRenderableWidget(microsoftButton);
        this.addRenderableWidget(loginButton);
        this.addRenderableWidget(offlineButton);
        this.addRenderableWidget(cookieButton);
        this.addRenderableWidget(removeButton);
        this.addRenderableWidget(backButton);

        reloadAccounts();
        updateCurrentAccount();
    }

    private void reloadAccounts() {
        accountList.clearEntries();
        List<SavedAccount> accounts = AccountHelper.accounts;
        for (SavedAccount acc : accounts) {
            accountList.addEntry(new AccountEntry(acc));
        }
    }

    private void updateCurrentAccount() {
        try {
            currentAccount = Minecraft.getInstance().getUser().getName();
        } catch (Throwable t) {
            currentAccount = "Error getting name";
        }
    }

    private void onMicrosoftLogin() {
        microsoftButton.active = false;
        new Thread(() -> {
            try {
                accountHelper.loginByMicrosoft();
                Minecraft.getInstance().execute(() -> {
                    ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE = false;
                    new ClientConfigHelper().saveClientConfig();
                    reloadAccounts();
                    updateCurrentAccount();
                    microsoftButton.active = true;
                });
            } catch (Exception e) {
                e.printStackTrace();
                Minecraft.getInstance().execute(() -> microsoftButton.active = true);
            }
        }).start();
    }

    private void onLoginSelected() {
        AccountEntry sel = accountList.getSelected();
        if (sel == null) return;

        SavedAccount account = sel.getAccount();
        new Thread(() -> {
            try {
                if (account.type == AccountType.MICROSOFT) {
                    ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE = false;
                    new ClientConfigHelper().saveClientConfig();
                    accountHelper.tokenLogin(account.data);
                } else {
                    ClientConfigHelper.LAST_OFFLINE_NICK = account.username;
                    ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE = true;
                    new ClientConfigHelper().saveClientConfig();
                    accountHelper.offlineLogin(account.username);
                }
                Minecraft.getInstance().execute(this::updateCurrentAccount);
            } catch (Exception ex) {
                ex.printStackTrace();
                Minecraft.getInstance().execute(() -> {
                    currentAccount = "§cLogin failed! Session expired.";
                });
            }
        }).start();
    }

    private void onOfflineLogin() {
        String nick = nameInput.getValue();
        if (nick.isEmpty()) return;
        ClientConfigHelper.LAST_OFFLINE_NICK = nick;
        ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE = true;
        new ClientConfigHelper().saveClientConfig();
        accountHelper.offlineLogin(nick);
        reloadAccounts();
        updateCurrentAccount();
    }

    private void onRemoveAccount() {
        AccountEntry sel = accountList.getSelected();
        if (sel == null) return;
        accountHelper.removeAccount(sel.getAccount().username);
        accountList.setSelected(null);
        reloadAccounts();
    }

    private void onCookieLogin() {
        cookieButton.active = false;
        new Thread(() -> {
            try {
                accountHelper.cookieLogin();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Minecraft.getInstance().execute(() -> {
                ClientConfigHelper.LAST_LOGIN_WAS_OFFLINE = false;
                new ClientConfigHelper().saveClientConfig();
                reloadAccounts();
                updateCurrentAccount();
                cookieButton.active = true;
            });
        }).start();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Current: " + currentAccount), this.width / 2, 25, 0x55FF55);
        this.accountList.render(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    private static class AccountList extends ObjectSelectionList<AccountEntry> {
        public AccountList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            this.centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return this.width - 40;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 12;
        }

        @Override
        protected void renderSelection(GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
        }

        public void clearEntries() { this.children().clear(); }
    }

    private static class AccountEntry extends ObjectSelectionList.Entry<AccountEntry> {
        private final SavedAccount account;

        AccountEntry(SavedAccount account) {
            this.account = account;
        }

        public SavedAccount getAccount() {
            return account;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int y, int x, int listWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            boolean isSelected = false;
            if (mc.screen instanceof AltManagerScreen screen) {
                isSelected = screen.accountList.getSelected() == this;
            }

            int adjX = x;
            int adjY = y + 2;
            int adjWidth = listWidth - 6;
            int adjHeight = entryHeight - 4;

            int bgColor = 0x44000000;
            int outlineColor = 0xFF666666;

            if (isSelected) {
                bgColor = 0xFF444444;
                outlineColor = 0xFFFFFFFF;
            } else if (hovered) {
                bgColor = 0x88333333;
                outlineColor = 0xFFAAAAAA;
            }

            guiGraphics.fill(adjX, adjY, adjX + adjWidth, adjY + adjHeight, bgColor);
            guiGraphics.renderOutline(adjX, adjY, adjWidth, adjHeight, outlineColor);

            guiGraphics.drawString(mc.font, account.username, adjX + 5, adjY + 5, 0xFFFFFF);
            String typeText = (account.type == AccountType.MICROSOFT) ? "Microsoft" : "Cracked";
            int color = (account.type == AccountType.MICROSOFT) ? 0x55FF55 : 0xAAAAAA;
            guiGraphics.drawString(mc.font, typeText, adjX + 5, adjY + 18, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (Minecraft.getInstance().screen instanceof AltManagerScreen parent) {
                parent.accountList.setSelected(this);
                return true;
            }
            return false;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(account.username);
        }
    }
}