package drasouls.multitools.items.multitool;

import necesse.engine.GameLog;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketShowAttack;
import necesse.engine.network.packet.PacketPlayerStopAttack;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;

public class MultitoolAttackHandler extends AttackHandler {
    public final MultitoolLevelEvent event;

    private final int animAttacks;
    private final int seed;
    private int animCounter = 1;
    private int tileX;
    private int tileY;
    private int targetsHash;
    private MultitoolPacketType packetType = MultitoolPacketType.NULL;

    // this class: packet stuff
    public MultitoolAttackHandler(PlayerMob player, PlayerInventorySlot slot, InventoryItem item, int levelX, int levelY, int animAttacks, int animSpeed, int attackSeed) {
        super(player, slot, animSpeed / animAttacks);
        this.animAttacks = animAttacks;
        this.seed = attackSeed;
        this.event = new MultitoolLevelEvent(player, levelX, levelY, item);
        this.event.setAttackHandler(this);
        player.getLevel().entityManager.addLevelEventHidden(this.event);
    }

    // CLIENT_TARGET_UPDATE
    public void sendTargetUpdate(int newTileX, int newTileY, int targetsHash) {
        checkClient();
        this.packetType = MultitoolPacketType.CLIENT_TARGET_UPDATE;
        this.tileX = newTileX;
        this.tileY = newTileY;
        this.targetsHash = targetsHash;
        this.sendPacketUpdate(false);
    }

    // SERVER_TARGET_REFRESH
    public void requestTargetRefresh() {
        checkServer();
        this.packetType = MultitoolPacketType.SERVER_TARGET_REFRESH;
        this.sendPacketUpdate(false);
    }

    public void sendTargetRefresh() {
        checkClient();
        this.packetType = MultitoolPacketType.SERVER_TARGET_REFRESH;
        this.sendPacketUpdate(false);
    }

    // SERVER_TARGET_RECHECK
    public void requestTargetRecheck() {
        checkServer();
        this.packetType = MultitoolPacketType.SERVER_TARGET_RECHECK;
        this.sendPacketUpdate(false);
    }

    public void sendTargetRecheck() {
        checkClient();
        this.packetType = MultitoolPacketType.SERVER_TARGET_RECHECK;
        this.sendPacketUpdate(false);
    }


    private void checkClient() {
        if (! this.event.level.isClientLevel()) throw new IllegalStateException("Client method called from server side!");
    }

    private void checkServer() {
        if (! this.event.level.isServerLevel()) throw new IllegalStateException("Server method called from client side!");
    }


    // Send
    @Override
    protected void setupContentPacket(PacketWriter writer) {
        writer.putNextEnum(this.packetType);
        switch (this.packetType) {
            case CLIENT_TARGET_UPDATE:
                writer.putNextInt(this.tileX);
                writer.putNextInt(this.tileY);
                writer.putNextInt(this.targetsHash);
                break;
            case SERVER_TARGET_REFRESH:
                if (this.event.level.isClientLevel()) {
                    this.event.writeTargetsToPacket(writer);
                }
                break;
            case SERVER_TARGET_RECHECK:
                if (this.event.level.isClientLevel()) {
                    this.event.checkAndWriteTargetsToPacket(writer);
                }
        }

        this.packetType = MultitoolPacketType.NULL;
    }

    // Receive
    @Override
    public void onPacketUpdate(PacketReader reader) {
        MultitoolPacketType incomingType = reader.getNextEnum(MultitoolPacketType.class);
        switch (incomingType) {
            case CLIENT_TARGET_UPDATE:
                this.tileX = reader.getNextInt();
                this.tileY = reader.getNextInt();
                this.targetsHash = reader.getNextInt();
                this.event.onTargetUpdate(this.tileX, this.tileY, this.targetsHash);
                break;
            case SERVER_TARGET_REFRESH:
                if (this.event.level.isClientLevel()) {
                    this.sendTargetRefresh();
                } else if (this.event.level.isServerLevel()) {
                    this.event.readTargetsFromPacket(reader);
                }
                break;
            case SERVER_TARGET_RECHECK:
                if (this.event.level.isClientLevel()) {
                    this.sendTargetRecheck();
                } else if (this.event.level.isServerLevel()) {
                    this.event.readTargetsFromPacket(reader);
                }
        }
    }


    // Animation and attack re-triggering stuff
    @Override
    public void onUpdate() {
        int attackX = this.event.levelX;
        int attackY = this.event.levelY;

        if (this.animCounter >= this.animAttacks) {
            Packet attackContent = new Packet();
            this.item.item.setupAttackContentPacket(new PacketWriter(attackContent), this.player.getLevel(), attackX, attackY, this.player, this.item);
            this.player.showAttack(this.item, attackX, attackY, this.seed, attackContent);
            if (this.player.getLevel().isServerLevel()) {
                ServerClient client = this.player.getServerClient();
                this.player.getLevel().getServer().network.sendToClientsAtExcept(new PacketShowAttack(this.player, this.item, attackX, attackY, this.seed, attackContent), client, client);
            }
            this.animCounter = 0;
        }
        this.animCounter++;
    }

    // Mouse button up
    @Override
    public void onEndAttack(boolean bySelf) {
        this.event.over();
    }


    public enum MultitoolPacketType {
        NULL,                   // Invalid state
        CLIENT_TARGET_UPDATE,   // Tell the server we've added a new target
        SERVER_TARGET_REFRESH,  // Asks the client for the list of targets
        SERVER_TARGET_RECHECK,  // Asks the client to recheck dead targets, same packet content as SERVER_TARGET_REFRESH
    }
}
