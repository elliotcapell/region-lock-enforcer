# Region Lock Enforcer

This plugin allows you to draw a custom border around any area of the map, preventing clicks/movement outside of it, and to remove teleports from interfaces/menus to block their use. Note that it will not block any clicks in instances or underground.

These region profiles can be exported and shared with others who follow the same snowflake area restrictions as you.

## How To Use:

Open the sidebar panel to either create a new region, or import an existing one. A 'region' is a profile containing a border, and a teleport whitelist.

### To import a region:
- Click Import Region and find the .JSON file you were given by somebody else, and click open
- It will be automatically selected, but you can also select a different region from the list to activate it

### To create a region:
- Click the '+' button in the Select Region section and give it a name
- You can then add a border, and modify the Teleport Allow List
- Ensure your Toggle Editor Hotkey is set in the settings

#### To draw the border:
- While in editing mode
    - Shift-click to mark tiles
    - Shift click a marked tile to un-mark it
    - You can also do this on the world map, where it will mark grids that you can configure the size of in settings
    - Press your Toggle Editor Hotkey to exit/enter editing mode at will
- When you are done, click 'Finish', and the program will automatically draw your border on the outer perimeter of the shape produced by your marked tiles. Note that your marked tiles must create an enclosed shape.
- You can edit a previously 'Finished' border by clicking the edit icon in the side panel
- You can change the styling (line colour, or use props) of each border individually by clicking the paint brush icon
- Note that a region can have many distinct borders, if desired

#### To define a teleport allow list:
- All teleports on this list will by disabled by default when the plugin is active
- To enable a teleport, simply find it in the list and check the box

### To export a region:
- Click Export Region and save it as a .JSON file with whatever name you wish
