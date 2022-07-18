package drasouls.multitools.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;

// Client to Server in case of client-only inventoryItem updates (like UI updating)
// Need better way to do this. ;~;
public class PacketUpdateGNDData extends Packet {
    public final InventoryItem inventoryItem;

    public PacketUpdateGNDData(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.inventoryItem = InventoryItem.fromContentPacket(reader);
    }

    public PacketUpdateGNDData(InventoryItem item) {
        this.inventoryItem = item;
        PacketWriter writer = new PacketWriter(this);
        item.addPacketContent(writer);
    }

    // TODO restrict
    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        if (this.inventoryItem == null) return;
        // ServerClient playerMob guaranteed non-null
        InventoryItem selected = client.playerMob.getSelectedItem();
        if (selected != null && selected.item.equals(this.inventoryItem.item)) {
            selected.setGndData(this.inventoryItem.getGndData());
        }
    }
}
