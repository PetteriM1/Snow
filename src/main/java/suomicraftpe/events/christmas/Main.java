package suomicraftpe.events.christmas;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.level.ChunkLoadEvent;
import cn.nukkit.event.level.WeatherChangeEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.plugin.PluginBase;

import java.util.List;

/**
 * SuomiCraft PE Events / Christmas
 * Created by PetteriM1 for SuomiCraft PE Network
 */
public class Main extends PluginBase implements Listener {

    private static int mode;
    private static List<String> worlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mode = getConfig().getInt("mode");
        worlds = getConfig().getStringList("worlds");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (worlds.contains(e.getLevel().getName())) {
            if (mode == 0) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        e.getChunk().setBiomeId(x, z, 12);
                    }
                }
            } else if (mode == 1) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        e.getChunk().setBiomeId(x, z, 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (mode == 0 && worlds.contains(e.getLevel().getName())) {
            e.setCancelled();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (worlds.contains(e.getPlayer().getLevel().getName())) {
            setRaining(e.getPlayer());
        }
    }

    @EventHandler
    public void onLevelChange(EntityLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            if (worlds.contains(e.getTarget().getName())) {
                setRaining((Player) e.getEntity());
            }
        }
    }

    private void setRaining(Player p) {
        if (mode == 0) {
            getServer().getScheduler().scheduleDelayedTask(this, () -> {
                try {
                    LevelEventPacket pk = new LevelEventPacket();
                    pk.evid = LevelEventPacket.EVENT_START_RAIN;
                    pk.data = Integer.MAX_VALUE;
                    p.dataPacket(pk);
                } catch (Exception ignore) {}
            }, 20);
        }
    }
}