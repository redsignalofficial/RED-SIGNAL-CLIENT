package us.rin.proxy.listeners;

import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.packetlib.Session;
import net.kyori.adventure.text.Component;
import us.rin.proxy.RedSignalProxy;
import us.rin.proxy.utils.ColorUtil;

import java.util.ArrayList;

public class ServerPingListener implements ServerInfoBuilder {

    @Override
    public ServerStatusInfo buildInfo(Session session) {
        return new ServerStatusInfo(
                new VersionInfo("§7Rin", 763),
                new PlayerInfo(1, 0, new ArrayList<>()),
                Component.text(ColorUtil.format(
                        "&b&lRed Signal&f&lClient &9&lProxy &8- &7Created by &fRin\n"
                                + "&8  [-------- &7Proxy Version&8: &f&l" + RedSignalProxy.VERSION + " &8--------]"
                )),
                null,
                false
        );
    }
}
