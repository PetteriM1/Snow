package suomicraftpe.events.christmas;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.ChunkLoadEvent;
import cn.nukkit.level.Level;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

/**
 * SuomiCraft PE Events / Christmas
 * Created by PetteriM1 for SuomiCraft PE Network
 */
public class Main extends PluginBase implements Listener {

    public static Config config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);

        for (String s : config.getStringList("worlds")) {
            Level l = getServer().getLevelByName(s);

            if (l != null) {
                if (config.getInt("mode") == 0) {
                    l.setRaining(true);
                } else if (config.getInt("mode") == 1) {
                    l.setRaining(false);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!config.getStringList("worlds").contains(e.getLevel().getName())) {
            return;
        }

        if (config.getInt("mode") == 0) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    e.getChunk().setBiomeId(x, z, 12);
                }
            }
        } else if (config.getInt("mode") == 1) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    e.getChunk().setBiomeId(x, z, 1);
                }
            }
        }
    }
}