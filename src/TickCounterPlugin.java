package net.runelite.client.plugins.tickcounter;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@PluginDescriptor(
        name = "<html><font color=#b82584>[J] Tick Counter",
        description = "Help",
        tags = {"inferno"},
        enabledByDefault = false
)
public class TickCounterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TickCounterOverlay overlay;
    @Inject
    private TickCounterConfig config;
    private static final Splitter SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    public ArrayList<NpcInfo> npcList = new ArrayList();

    public TickCounterPlugin() {
    }

    @Provides
    TickCounterConfig getConfig(ConfigManager configManager) {
        return (TickCounterConfig)configManager.getConfig(TickCounterConfig.class);
    }

    protected void startUp() {
        this.reset();
        this.overlayManager.add(this.overlay);
    }

    protected void shutDown() {
        this.reset();
        this.overlayManager.remove(this.overlay);
    }

    private void reset() {
        this.npcList.clear();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        for(int i = this.npcList.size() - 1; i >= 0; --i) {
            --((NpcInfo)this.npcList.get(i)).ticks;
            if (((NpcInfo)this.npcList.get(i)).ticks == 0) {
                this.npcList.remove(i);
            }
        }

    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (actor.getName() != null && actor instanceof NPC) {
            NPC npc = (NPC)actor;

            List<String> strList = SPLITTER.splitToList(this.config.allNPC());
            Iterator var4 = strList.iterator();

            while(var4.hasNext()) {
                String str = (String)var4.next();
                String[] stringList = str.split(",");
                if (stringList.length > 2) {
                    if (actor.getName().equalsIgnoreCase(stringList[0])){
                        int numEntries = (stringList.length - 1)/2;
                        for (int i=0; i<numEntries; i++){
                            if (actor.getAnimation() == Integer.valueOf(stringList[(i*2)+1].trim())){
                                this.npcList.add(new NpcInfo(npc, Integer.valueOf(stringList[(i*2)+2].trim())+1, this.config.npcColor()));
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
