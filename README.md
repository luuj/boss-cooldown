# Enemy tick counter for Runelite

## Adding your own tick counters:

The format for adding to the list of NPC's this plugin keeps track of is:

npc name, animation id, ticks for that animation, color to display

For example, Jal-ImKot,7597,4,3 is for the NPC Jal-ImKot, it's animation id (which can be found using the developer tool's plugin or google) is 7597, the number of ticks before this enemy 
attacks again is 4, and the color to display is 3 (red). If an enemy has multiple animations (e.g. Jal-Zek, who can both attack with magic or melee you), you can keep appending animation id's and # of ticks on the same line (e.g. Jal-Zek,7610,4,1,7612,4,3).




