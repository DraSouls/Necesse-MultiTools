package drasouls.multitools.old.items.multiminer;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketPlayerStopAttack;
import necesse.engine.network.packet.PacketShowAttack;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.inventory.PlayerInventorySlot;

import java.awt.geom.Point2D;

public class MultiMineAttackHandler extends AttackHandler {
    public int seed;
    public MultiMineLevelEvent event;

    // Seems like this class is meant for making the player "constantly hold" the item until mouse release.
    // Also lets the player aim around while holding that item.
    public MultiMineAttackHandler(PlayerMob player, PlayerInventorySlot slot, int updateInterval, int attackSeed, MultiMineLevelEvent event) {
        super(player, slot, updateInterval);
        this.event = event;
    }

    // aimAngle should run on event's clientTick()
    @Override
    public void onUpdate() {
        Point2D.Float dir = GameMath.getAngleDir(this.event.aimAngle);
        int attackX = (int)(this.player.x + (dir.x * 100.0f));
        int attackY = (int)(this.player.y + (dir.y * 100.0f));

        // sends a packet that triggers showAttack on the server
        Packet attackContent = new Packet();
        this.item.item.setupAttackContentPacket(new PacketWriter(attackContent), this.player.getLevel(), attackX, attackY, this.player, this.item);
        this.player.showAttack(this.item, attackX, attackY, 0, attackContent);

        // and this shows the attack to other clients (server sends packet that triggers (other) clients' showAttack
        if (this.player.getLevel().isServerLevel()) {
            ServerClient client = this.player.getServerClient();
            this.player.getLevel().getServer().network.sendToClientsAtExcept(new PacketShowAttack(this.player, this.item, attackX, attackY, this.seed, attackContent), client, client);
        }
    }

    @Override
    public void onEndAttack(boolean bySelf) {
        this.player.stopAttack();
        if (this.player.getLevel().isServerLevel()) {
            ServerClient client = this.player.getServerClient();
            this.player.getLevel().getServer().network.sendToClientsAtExcept(new PacketPlayerStopAttack(client.slot), client, client);
        }
    }
}
