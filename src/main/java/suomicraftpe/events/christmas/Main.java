package suomicraftpe.events.christmas;

import cn.nukkit.Nukkit;
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
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Deflater;

/**
 * SuomiCraft PE Events / Christmas
 * Created by PetteriM1 for SuomiCraft PE Network
 */
public class Main extends PluginBase implements Listener {

    private static final int MODE_SNOW = 0;
    private static final int MODE_RESET = 1;

    private static int mode;
    private static boolean legacy;
    private static Set<String> worlds;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mode = getConfig().getInt("mode");
        legacy = getConfig().getBoolean("legacy");
        worlds = new HashSet<>(getConfig().getStringList("worlds"));

        if (legacy) {
            getLogger().warning("The biome of loaded chunks is changed when legacy mode is used. Remember to take a backup!");
        } else {
            patchBiomeList();
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void patchBiomeList() {
        try {
            Class<?> c_BiomeDefinitionListPacket = Class.forName("cn.nukkit.network.protocol.BiomeDefinitionListPacket");

            byte[] TAG = ByteStreams.toByteArray(Nukkit.class.getClassLoader().getResourceAsStream("biome_definitions.dat"));
            Tag compoundTag = NBTIO.readTag(new ByteArrayInputStream(TAG), ByteOrder.BIG_ENDIAN, true);
            for (Tag tag : ((CompoundTag) compoundTag).getAllTags()) {
                ((CompoundTag) tag).putFloat("temperature", -0.5f);
            }
            TAG = NBTIO.writeNetwork(compoundTag);

            BiomeDefinitionListPacket pk = new BiomeDefinitionListPacket();
            Field f_tag = c_BiomeDefinitionListPacket.getDeclaredField("tag");
            f_tag.setAccessible(true);
            f_tag.set(pk, TAG);
            pk.tryEncode();

            Field f_CACHED_PACKET = c_BiomeDefinitionListPacket.getDeclaredField("CACHED_PACKET");
            f_CACHED_PACKET.setAccessible(true);
            Field f_modifiers = Field.class.getDeclaredField("modifiers");
            f_modifiers.setAccessible(true);
            f_modifiers.setInt(f_CACHED_PACKET, f_CACHED_PACKET.getModifiers() & ~Modifier.FINAL);
            f_CACHED_PACKET.set(null, pk.compress(Deflater.BEST_COMPRESSION));
        } catch (Exception ex) {
            getLogger().error("Failed to patch BiomeDefinitionListPacket", ex);
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
