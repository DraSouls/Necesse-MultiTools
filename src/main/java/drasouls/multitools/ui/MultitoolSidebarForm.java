package drasouls.multitools.ui;

import drasouls.multitools.ObjectCategories;
import drasouls.multitools.TileCategories;
import drasouls.multitools.items.MultitoolToolItem;
import drasouls.multitools.packet.PacketUpdateGNDData;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultitoolSidebarForm extends SidebarForm {
    private Client client;

    public MultitoolSidebarForm(InventoryItem item, GNDItemMap categoryFilter) {
        super("drs_multitoolsidebar", 160, 120, item);
        Objects.requireNonNull(categoryFilter);

        FormFlow listFlow = new FormFlow(5);
        this.addComponent(new FormLocalLabel("ui", "drs_filters", new FontOptions(16), -1, 5, listFlow.next(25)));

        FormTextButton selectAll = this.addComponent(new FormLocalTextButton("ui", "drs_all", 5, listFlow.next(0), 70, FormInputSize.SIZE_16, ButtonColor.BASE));
        FormTextButton selectNone = this.addComponent(new FormLocalTextButton("ui", "drs_none", 85, listFlow.next(20), 70, FormInputSize.SIZE_16, ButtonColor.BASE));

        List<FormCheckBox> checkBoxes = new ArrayList<>();

        ObjectCategories.COMPUTED.keySet().forEach(category ->
                checkBoxes.add(this.addComponent(new FormLocalCheckBox("categories", "drs_" + category, 5, listFlow.next(20), categoryFilter.getBoolean(category)) {
                    @Override
                    public GameTooltips getTooltip() {
                        return new StringTooltips(Localization.translate("categories", "drs_" + category + "_desc"));
                    }
                }).onClicked(e -> {
                    MultitoolToolItem.getCategoryFilter(item).setBoolean(category, e.from.checked);
                    if (client != null)
                        client.network.sendPacket(new PacketUpdateGNDData(item));
                }))
        );

        TileCategories.COMPUTED.keySet().forEach(category ->
                checkBoxes.add(this.addComponent(new FormLocalCheckBox("categories", "drs_" + category, 5, listFlow.next(20), categoryFilter.getBoolean(category)) {
                    @Override
                    public GameTooltips getTooltip() {
                        return new StringTooltips(Localization.translate("categories", "drs_" + category + "_desc"));
                    }
                }).onClicked(e -> {
                    MultitoolToolItem.getCategoryFilter(item).setBoolean(category, e.from.checked);
                    if (client != null)
                        client.network.sendPacket(new PacketUpdateGNDData(item));
                }))
        );

        selectAll.onClicked(e -> {
            checkBoxes.forEach(cb -> cb.checked = true);
            GNDItemMap map = MultitoolToolItem.getCategoryFilter(item);
            ObjectCategories.COMPUTED.keySet().forEach(cat -> map.setBoolean(cat, true));
            TileCategories.COMPUTED.keySet().forEach(cat -> map.setBoolean(cat, true));
            item.getGndData().setItem("filter", map);
            if (client != null)
                client.network.sendPacket(new PacketUpdateGNDData(item));
        });
        selectNone.onClicked(e -> {
            checkBoxes.forEach(cb -> cb.checked = false);
            item.getGndData().setItem("filter", new GNDItemMap());
            if (client != null)
                client.network.sendPacket(new PacketUpdateGNDData(item));
        });

        this.setHeight(listFlow.next() + 5);
    }

    @Override
    public void onAdded(Client client) {
        super.onAdded(client);
        this.client = client;
    }
}
