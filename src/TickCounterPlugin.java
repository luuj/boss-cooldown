package net.runelite.client.plugins.tickcd;

import com.google.inject.Provides;
import com.google.common.base.Splitter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "<html><font color=#b82584>[J] Boss Tick Counter",
        description = "Overlays a tick counter for enemies",
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
    private boolean XarpusActive, VerzikActive, OlmActive, JadActive;
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
        XarpusActive = VerzikActive = OlmActive = JadActive = false;
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

            if (((NpcInfo)this.npcList.get(i)).ticks <= 0) {
                if (config.enableJad() && JadActive) {
                    if (((NpcInfo) this.npcList.get(i)).currNPC.getName().equalsIgnoreCase("JalTok-Jad") ||
                            ((NpcInfo) this.npcList.get(i)).currNPC.getName().equalsIgnoreCase("TzTok-Jad")) {
                        if (!((NpcInfo) this.npcList.get(i)).currNPC.isDead()) {
                            ((NpcInfo) this.npcList.get(i)).ticks += 8;
                            continue;
                        }
                    }
                }
                if (config.enableVerzik() && VerzikActive) {
                    if (((NpcInfo) this.npcList.get(i)).currNPC.getId() == 8374) {
                        if (!((NpcInfo) this.npcList.get(i)).currNPC.isDead()) {
                            ((NpcInfo) this.npcList.get(i)).ticks += 7;
                            continue;
                        }
                    }
                }
                if (config.enableXarp() && XarpusActive) {
                    if (((NpcInfo) this.npcList.get(i)).currNPC.getId() == 8340) {
                        if (!((NpcInfo) this.npcList.get(i)).currNPC.isDead()) {
                            ((NpcInfo) this.npcList.get(i)).ticks += 8;
                            continue;
                        }
                    }
                }
                if (config.enableOlm() && OlmActive) {
                    if (((NpcInfo) this.npcList.get(i)).currNPC.getId() == 7554) {
                        if (!((NpcInfo) this.npcList.get(i)).currNPC.isDead()) {
                            ((NpcInfo) this.npcList.get(i)).ticks += 4;
                            continue;
                        }
                    }
                }

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
                if (stringList.length > 3) {
                    if (actor.getName().equalsIgnoreCase(stringList[0])){
                        int numEntries = (stringList.length - 1)/3;
                        for (int i=0; i<numEntries; i++){
                            if (actor.getAnimation() == Integer.valueOf(stringList[(i*3)+1].trim())){
                                Color selectCol = this.config.npcColor();
                                switch(Integer.valueOf(stringList[(i*3)+3].trim())){
                                    case 1: selectCol = this.config.npcColor(); break;
                                    case 2: selectCol = this.config.npcColor2(); break;
                                    case 3: selectCol = this.config.npcColor3(); break;
                                }
                                this.npcList.add(new NpcInfo(npc, Integer.valueOf(stringList[(i*3)+2].trim())+1, selectCol));
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onNpcSpawned(final NpcSpawned event){
        final NPC npc = event.getNpc();

        if(config.enableJad()){
            if (npc.getName().equalsIgnoreCase("JalTok-Jad") ||
                    npc.getName().equalsIgnoreCase("TzTok-Jad") ){
                JadActive = true;
                this.npcList.add(new NpcInfo(npc, 8, this.config.npcColor()));
            }
        }
        if(config.enableVerzik()){
            if (npc.getId() == 8374){
                VerzikActive = true;
                this.npcList.add(new NpcInfo(npc, 7, this.config.npcColor()));
            }
        }
        if(config.enableOlm()){
            if (npc.getId() == 7554){
                OlmActive = true;
                this.npcList.add(new NpcInfo(npc, 4, this.config.npcColor()));
            }
        }
        if(config.enableMaiden()){
            if (npc.getId() == 8366){
                this.npcList.add(new NpcInfo(npc, 16, this.config.npcColor()));
            }
        }
    }

    @Subscribe
    public void onNpcDespawned(final NpcDespawned event){
        final NPC npc = event.getNpc();

        if(config.enableJad()){
            if (npc.getName().equalsIgnoreCase("JalTok-Jad") ||
                    npc.getName().equalsIgnoreCase("TzTok-Jad") ){
                JadActive = false;
            }
        }
        if(config.enableVerzik()){
            if (npc.getId() == 8374){
                VerzikActive = false;
            }
        }
        if(config.enableOlm()){
            if (npc.getId() == 7554){
                OlmActive = false;
            }
        }
    }

    @Subscribe
    private void onNpcChanged(final NpcChanged event){
        final NPC npc = event.getNpc();

        if(config.enableXarp()){
            if (npc.getId() == 8340){
                XarpusActive=true;
                this.npcList.add(new NpcInfo(npc, 8, this.config.npcColor()));
            }
        }
        if(config.enableOlm()){
            if (npc.getId() == 7554){
                OlmActive=true;
                this.npcList.add(new NpcInfo(npc, 4, this.config.npcColor()));
            }
        }
    }
}
