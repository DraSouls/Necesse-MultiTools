package drasouls.multitools.ui;

import drasouls.multitools.items.PlannerItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemInteractAction;

// Unused.
public class PlannerSidebarForm extends SidebarForm {
    private static FormFairTypeLabel stateLabel;
    private static FormLocalCheckBox altCheckbox;

    private Client client;

    public PlannerSidebarForm(InventoryItem item) {
        super("drs_plannersidebar", 160, 80, item);
        if (! (item.item instanceof PlannerItem))
            throw new IllegalArgumentException("item using this sidebar should be a PlannerItem");

        FormFlow flow = new FormFlow(5);

        altCheckbox = this.addComponent(new FormLocalCheckBox("ui", "drs_plannerusealt", 5, flow.next(30), item.getGndData().getBoolean("usealt"), 150));
        altCheckbox.onClicked(e -> {
            item.getGndData().setBoolean("usealt", e.from.checked);
            if (client != null)
                client.network.sendPacket(new PacketUpdateGNDData(item, "usealt"));
        });

        FontOptions fo = new FontOptions(12);
        stateLabel = this.addComponent(new FormFairTypeLabel(Localization.translate("ui", "drs_planneridle"), 5, flow.next()))
                .setFontOptions(fo)
                .setMaxWidth(150)
                .setParsers(TypeParsers.InputIcon(fo));

        GNDItem p1 = item.getGndData().getItem("p1");
        GNDItem p2 = item.getGndData().getItem("p2");
        updateState(p1 instanceof GNDItemMap, p2 instanceof GNDItemMap, true);
    }

    public static void updateState(boolean hasFirst, boolean hasSecond, boolean hasAlt) {
        if (stateLabel == null) return;
        if (!hasAlt) altCheckbox.checked = false;
        altCheckbox.setActive(hasAlt);
        if (!hasFirst) {
            stateLabel.setText(Localization.translate("ui", "drs_planneridle"));
        } else if (!hasSecond) {
            stateLabel.setText(Localization.translate("ui", "drs_plannerdragging"));
        } else {
            stateLabel.setText(Localization.translate("ui", "drs_plannerplacing"));
        }
    }

    @Override
    public void onAdded(Client client) {
        super.onAdded(client);
        this.client = client;
    }

    @Override
    public void onSidebarUpdate(int x, int y) {
        super.onSidebarUpdate(x, y);
    }
}
