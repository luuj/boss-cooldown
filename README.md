# Enemy tick counter for Runelite

## Adding your own tick counters:

The format for adding to the list of NPCs is:

npc name, animation id, ticks for that animation, color to display

For example, Jal-ImKot,7597,4,3 is for the NPC Jal-ImKot, animation id 7597 (which can be found using the developer tool's plugin), 4 tick attack cycle, and color display 3 (red). If an enemy has multiple animations (e.g. Jal-Zek, who can both attack with magic or melee you), you can keep appending animation id's and # of ticks on the same line (e.g. Jal-Zek,7610,4,1,7612,4,3).




