package drasouls.multitools.packet;

import necesse.engine.GameLog;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;

import java.util.HashSet;
import java.util.Set;

// Client to Server in case of client-only inventoryItem updates (like UI updating)
// Need better way to do this. ;~;
public class PacketUpdateGNDData extends Packet {
    private static final Set<String> WHITELIST = new HashSet<String>(){{
        add("filter");
        add("usealt");
    }};
    public final InventoryItem inventoryItem;
    public final String gndItemName;

    public PacketUpdateGNDData(byte[] data) {
        super(data);
        PacketReader reader = new PacketReader(this);
        this.inventoryItem = InventoryItem.fromContentPacket(reader);
        this.gndItemName = reader.getNextString();
        if (! WHITELIST.contains(this.gndItemName))
            GameLog.warn.println(this.gndItemName + " is not whitelisted for PacketUpdateGNDData. Ignoring.");
    }

    public PacketUpdateGNDData(InventoryItem item, String gndItemName) {
        this.inventoryItem = item;
        this.gndItemName = gndItemName;
        PacketWriter writer = new PacketWriter(this);
        item.addPacketContent(writer);
        writer.putNextString(gndItemName);
        if (! WHITELIST.contains(gndItemName))
            GameLog.warn.println(gndItemName + " is not whitelisted for PacketUpdateGNDData. Ignoring.");
    }

    // TODO restrict
    @Override
    public void processServer(NetworkPacket packet, Server server, ServerClient client) {
        if (this.inventoryItem == null) return;
        if (! WHITELIST.contains(this.gndItemName)) return;
        // ServerClient playerMob guaranteed non-null
        InventoryItem selected = client.playerMob.getSelectedItem();
        if (selected != null && selected.item.equals(this.inventoryItem.item)) {
            selected.getGndData().setItem(this.gndItemName, this.inventoryItem.getGndData().getItem(this.gndItemName));
        }
    }
}
