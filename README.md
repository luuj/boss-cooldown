# Enemy tick counter for Runelite

Tick counter for BlueLite / Steroid client. Simply place the jar file in the external plugin folder.

For inferno enemy tick counters, paste the following text in the plugin settings: 

Jal-MejRah,7578,3  
Jal-Ak,7581,6,7583,6,7582,9  
Jal-ImKot,7597,4  
Jal-Xil,7605,4,7604,4  
Jal-Zek,7610,4,7612,4  


## Adding your own tick counters:

The format for adding to the list of NPC's this plugin keeps track of is:

npc name, animation id, ticks for that animation

For example, Jal-ImKot,7597,4 is for the NPC Jal-ImKot, it's animation id (which can be found using the developer tool's plugin or google) is 7597, and the number of ticks before this enemy 
attacks again is 4. If an enemy has multiple animations (e.g. Jal-Zek, who can both attack with magic or melee you), you can keep appending animation id's and # of ticks on the same line (e.g. Jal-Zek,7610,4,7612,4).




