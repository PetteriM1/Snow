package suomicraftpe.events.christmas;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.level.ChunkLoadEvent;
import cn.nukkit.event.level.WeatherChangeEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.network.protocol.BiomeDefinitionListPacket;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.plugin.PluginBase;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.List;

/**
 * SuomiCraft PE Events / Christmas
 * Created by PetteriM1 for SuomiCraft PE Network
 */
public class Main extends PluginBase implements Listener {

    private static int mode;
    private static boolean legacy;
    private static List<String> worlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mode = getConfig().getInt("mode");
        legacy = getConfig().getBoolean("legacy");
        worlds = getConfig().getStringList("worlds");
        getServer().getPluginManager().registerEvents(this, this);

        if (!legacy) {
            if (mode != 0) {
                getLogger().error("The 'mode' setting is only available in the legacy mode");
            }
            try {
                new BiomeDefinitionListPacket();
                Class<?> c_BiomeDefinitionListPacket = Class.forName("cn.nukkit.network.protocol.BiomeDefinitionListPacket");
                Field f_TAG = c_BiomeDefinitionListPacket.getDeclaredField("TAG");
                f_TAG.setAccessible(true);
                Field f_modifiers = Field.class.getDeclaredField("modifiers");
                f_modifiers.setAccessible(true);
                f_modifiers.setInt(f_TAG, f_TAG.getModifiers() & ~Modifier.FINAL);
                byte[] TAG = (byte[]) f_TAG.get(null);
                Tag compoundTag = NBTIO.readTag(new ByteArrayInputStream(TAG), ByteOrder.BIG_ENDIAN, true);
                for (Tag tag : ((CompoundTag) compoundTag).getAllTags()) {
                    ((CompoundTag) tag).putFloat("temperature", -0.5f);
                }
                TAG = NBTIO.writeNetwork(compoundTag);
                f_TAG.set(null, TAG);
            } catch (Exception ex) {
                getLogger().error("Failed to patch BiomeDefinitionListPacket", ex);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (legacy && worlds.contains(e.getLevel().getName())) {
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
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        setRaining(e.getPlayer());
    }

    @EventHandler
    public void onLevelChange(EntityLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            setRaining((Player) e.getEntity());
        }
    }

    private void setRaining(Player p) {
        if (mode == 0 && worlds.contains(p.getLevel().getName())) {
            getServer().getScheduler().scheduleDelayedTask(this, () -> {
                if (p.isOnline() && worlds.contains(p.getLevel().getName())) {
                    LevelEventPacket pk = new LevelEventPacket();
                    pk.evid = LevelEventPacket.EVENT_START_RAIN;
                    pk.data = 6000000;
                    p.dataPacket(pk);
                }
            }, 10);
        }
    }
}
