package drasouls.multitools.ui;

import drasouls.multitools.items.PlannerItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;

// Unused.
public class PlannerSidebarForm extends SidebarForm {
    private static FormLocalLabel stateLabel;

    private Client client;

    public PlannerSidebarForm(InventoryItem item) {
        super("drs_plannersidebar", 160, 120, item);
        if (! (item.item instanceof PlannerItem))
            throw new IllegalArgumentException("item using this sidebar should be a PlannerItem");

        FormFlow flow = new FormFlow(5);

        this.addComponent(new FormLocalCheckBox("ui", "drs_plannerusealt", 5, flow.next(20), item.getGndData().getBoolean("usealt"), 150)
                .onClicked(e -> {
                    item.getGndData().setBoolean("usealt", e.from.checked);
                    if (client != null)
                        client.network.sendPacket(new PacketUpdateGNDData(item, "usealt"));
                })
        );

        stateLabel = this.addComponent(new FormLocalLabel("ui", "drs_planneridle", new FontOptions(16), -1, 5, flow.next()));

        GNDItem p1 = item.getGndData().getItem("p1");
        GNDItem p2 = item.getGndData().getItem("p2");
        updateState(p1 instanceof GNDItemMap, p2 instanceof GNDItemMap);
    }

    public static void updateState(boolean hasFirst, boolean hasSecond) {
        if (stateLabel == null) return;
        if (! (hasFirst || hasSecond)) {
            stateLabel.setLocalization("ui", "drs_planneridle");
        } else if (hasFirst) {
            stateLabel.setLocalization("ui", "drs_plannerdragging");
        } else {
            stateLabel.setLocalization("ui", "drs_plannerplacing");
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
