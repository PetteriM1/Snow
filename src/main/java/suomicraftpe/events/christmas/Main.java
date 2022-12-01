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

    private static final int MODE_SNOW = 0;
    private static final int MODE_RESET = 1;

    private static int mode;
    private static boolean legacy;
    private static List<String> worlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mode = getConfig().getInt("mode");
        legacy = getConfig().getBoolean("legacy");
        worlds = getConfig().getStringList("worlds");

        if (legacy) {
            getLogger().warning("The biome of loaded chunks is changed when legacy mode is used. Remember to take a backup!");
        } else {
            new BiomeDefinitionListPacket();
            Class<?> c_BiomeDefinitionListPacket;
            try {
                c_BiomeDefinitionListPacket = Class.forName("cn.nukkit.network.protocol.BiomeDefinitionListPacket");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            patchBiomeList(c_BiomeDefinitionListPacket, "TAG", false);
            patchBiomeList(c_BiomeDefinitionListPacket, "TAG_486", true);
            patchBiomeList(c_BiomeDefinitionListPacket, "TAG_419", true);
            patchBiomeList(c_BiomeDefinitionListPacket, "TAG_361", true);
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void patchBiomeList(Class<?> c_BiomeDefinitionListPacket, String fieldName, boolean multiversion) {
        try {
            Field f_TAG = c_BiomeDefinitionListPacket.getDeclaredField(fieldName);
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
            if (multiversion) {
                getLogger().debug("Failed to patch BiomeDefinitionListPacket " + fieldName + ", maybe this software doesn't have multiversion support", ex);
            } else {
                getLogger().error("Failed to patch BiomeDefinitionListPacket " + fieldName, ex);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (legacy && worlds.contains(e.getLevel().getName())) {
            if (mode == MODE_SNOW) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        e.getChunk().setBiomeId(x, z, 12);
                    }
                }
            } else if (mode == MODE_RESET) {
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
        if ((mode == MODE_SNOW || !legacy) && worlds.contains(e.getLevel().getName())) {
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
        if ((mode == MODE_SNOW || !legacy) && worlds.contains(p.getLevel().getName())) {
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
