package net.runelite.client.plugins.tickcd;

import com.google.inject.Provides;
import com.google.common.base.Splitter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "<html><font color=#b82584>[J] Boss Ticks",
        description = "",
        tags = {"boss", "tick"},
        enabledByDefault = false
)
public class TickcdPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TickcdOverlay overlay;
    @Inject
    private TickcdConfig config;
    private boolean OlmActive;
    private short OlmPhase;
    private short jadCount;
    private static final Splitter SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();
    public ArrayList<NpcInfo> npcList = new ArrayList();
    public ArrayList<NPC> jads = new ArrayList();

    public TickcdPlugin() {
    }

    @Provides
    TickcdConfig getConfig(ConfigManager configManager) {
        return (TickcdConfig)configManager.getConfig(TickcdConfig.class);
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
        this.jads.clear();
        OlmPhase = 1;
        OlmActive = false;
        jadCount = 0;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        for(int i = this.npcList.size() - 1; i >= 0; --i) {
            NpcInfo curr = (NpcInfo)this.npcList.get(i);
            --curr.ticks;

            // Color blob attacks
            if (curr.currNPC.getName().equalsIgnoreCase("Jal-Ak")){
                if (curr.ticks == 3){
                    if(client.getLocalPlayer().getOverheadIcon() == HeadIcon.MAGIC)
                        curr.color = Color.GREEN;
                    if(client.getLocalPlayer().getOverheadIcon() == HeadIcon.RANGED)
                        curr.color = Color.CYAN;
                }
            }

            if (curr.ticks <= 0 || curr.currNPC.isDead()) {
                //Special counter for Olm
                if (config.enableOlm() && OlmActive) {
                    if (curr.currNPC.getId() == 7554) {
                        if (!curr.currNPC.isDead()) {
                            curr.ticks += 4;
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

            for (String str : strList) {
                String[] stringList = str.split(",");
                if (stringList.length > 3) {
                    if (Objects.requireNonNull(actor.getName()).equalsIgnoreCase(stringList[0])) {
                        int numEntries = (stringList.length - 1) / 3;
                        for (int i = 0; i < numEntries; i++) {
                            if (actor.getAnimation() == Integer.parseInt(stringList[(i * 3) + 1].trim())) {
                                Color selectCol = this.config.npcColor();
                                switch (Integer.parseInt(stringList[(i * 3) + 3].trim())) {
                                    case 1:
                                        selectCol = this.config.npcColor();
                                        break;
                                    case 2:
                                        selectCol = this.config.npcColor2();
                                        break;
                                    case 3:
                                        selectCol = this.config.npcColor3();
                                        break;
                                    case 4:
                                        selectCol = this.config.npcColor4();
                                        break;
                                }
                                //Update existing entry if another animation occurs
                                Optional<NpcInfo> tempNPC = containsNPC(npcList, npc);
                                if(tempNPC.isPresent()){
                                    tempNPC.get().ticks = Integer.parseInt(stringList[(i * 3) + 2].trim()) + 1;
                                    tempNPC.get().color = selectCol;
                                }else {
                                    this.npcList.add(new NpcInfo(npc, Integer.parseInt(stringList[(i * 3) + 2].trim()) + 1, selectCol));
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /* Olm code*/
    @Subscribe
    public void onNpcDespawned(final NpcDespawned event){
        final NPC npc = event.getNpc();

        if(config.enableOlm()){
            if (npc.getId() == 7554){
                if (OlmPhase == 4){
                    OlmPhase = 1;
                }
                OlmActive = false;
            }
        }

        if(config.enableJad()){
            if (Objects.equals(npc.getName(), "JalTok-Jad")){
                this.jads.remove(npc);
            }
        }
    }

    @Subscribe
    private void onNpcChanged(final NpcChanged event){
        final NPC npc = event.getNpc();

        if(config.enableOlm()){
            if (npc.getId() == 7554){
                OlmActive=true;

                if (OlmPhase == 1){
                    this.npcList.add(new NpcInfo(npc, 5, this.config.npcColor()));
                }else{
                    this.npcList.add(new NpcInfo(npc, 4, this.config.npcColor()));
                }
                ++OlmPhase;
            }
        }
    }

    /*Jad code*/
    @Subscribe
    public void onNpcSpawned(final NpcSpawned event){
        final NPC npc = event.getNpc();

        if(config.enableJad()){
            if (Objects.equals(npc.getName(), "JalTok-Jad")){
                this.npcList.add(new NpcInfo(npc,8,this.config.npcColor()));
                this.jads.add(npc);
            }
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed)
    {
        int soundId = soundEffectPlayed.getSoundId();
        //if(config.enableJad() && soundId == 163 && this.jads.size() == 1){
        if(config.enableJad() && soundId == 163){
            if(jadCount >= this.jads.size()){
                jadCount = 0;
            }
            NPC curr = (NPC) this.jads.get(jadCount);
            this.npcList.add(new NpcInfo(curr,9,this.config.npcColor()));

            jadCount++;
        }
    }

    public Optional<NpcInfo> containsNPC(final ArrayList<NpcInfo>  list, final NPC name){
        return list.stream().filter(o -> o.currNPC.equals(name)).findFirst();
    }
}
