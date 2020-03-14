package example;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.plugin.Plugin;
import arc.Events;
import arc.struct.Array;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.plugin.Plugin;
//import mindustry.plugin.*;
//import mindustry.type.UnitType;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.content.Items;
import mindustry.type.Item;
import mindustry.content.UnitTypes;
import mindustry.entities.type.BaseUnit;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.modules.ItemModule;
//import sun.management.counter.perf.PerfLongArrayCounter;

import java.util.HashMap;

import static mindustry.Vars.*;

public class ExamplePlugin extends Plugin{
    String password="none";
    int[] layout=new int[100];
    int layout_capacity=5000;
    public ExamplePlugin(){
        //listen for a block selection event
        Events.on(BuildSelectEvent.class, event -> {
            if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player){
                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + ((Player)event.builder).name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y);
            }
        });
    }
    public boolean verifi_item(Item item){
        return item.name.equals("blast-compound") ||
                item.name.equals("spore-pod") ||
                item.name.equals("pyratite") ||
                item.name.equals("coal") ||
                item.name.equals("send") ||
                item.name.equals("scrap");
    }
    private void show_layout(Player player) {
        int idx=0;
        String message="";
        for(Item item:content.items()){
            if(verifi_item(item)){continue;}
            message+=(layout[idx]!=layout_capacity ? "[white]":"[green]");
            message+=item.name+":"+layout[idx]+"  ";
            idx++;
        }
        player.sendMessage(message);
    }
    public void fill_layout(Player player){
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        int idx=0;
        for(Item item:content.items()){
            if(verifi_item(item)){continue;}
            int stored=layout[idx];
            if(core.items.has(item,layout_capacity-stored)){
                core.items.remove(item,layout_capacity-stored);
                layout[idx]=layout_capacity;
            }else{
                int amount=core.items.get(item);
                int owerflow=stored+amount-5000;
                if (owerflow>0){
                    layout[idx]=layout_capacity;
                    core.items.remove(item,amount-owerflow);
                }else{
                    layout[idx]+=amount;
                    core.items.remove(item,amount);
                }

            }
            idx++;
        }

    }
    public void use_layout(Player player){
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        int idx=0;
        for(Item item:content.items()){
            if(verifi_item(item)){continue;}
            core.items.add(item,layout[idx]);
            layout[idx]=0;
            idx++;
        }

    }
    public int get_storige_size() {
        int res=0;
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                //loop through and log all found reactors
                Block block = world.tile(x, y).block();
                if (Blocks.coreShard.equals(block)) {
                    res += 4000;
                } else if (Blocks.coreFoundation.equals(block)) {
                    res += 9000;
                } else if (Blocks.coreNucleus.equals(block)) {
                    res += 1300;
                }
            }
        }
        return res;
    }
    public void build_core(int cost,Player player,Block core_tipe){
        boolean built=false;
        boolean can_build=true;
        Teams.TeamData teamData = state.teams.get(player.getTeam());
        CoreBlock.CoreEntity core = teamData.cores.first();
        for(Item item:content.items()){
            if(verifi_item(item)){continue;}
            if (!core.items.has(item, cost)) {
                can_build=false;
                player.sendMessage("[scarlet]" + item.name + ":" + Integer.toString(core.items.get(item))+"/"+Integer.toString(cost));
            }
        }
        if(can_build) {
            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core_tipe, 0, (byte) 0, player.getTeam(), false);
            if (world.tile(player.tileX(), player.tileY()).block() == core_tipe) {
                built = true;
                player.sendMessage("[green]Core spawned!");
                for(Item item:content.items()){
                    if(verifi_item(item)){continue;}
                    core.items.remove(item, cost);
                }

            } else {
                player.sendMessage("[scarlet]Core spawn failed!Invalid placemant!");
            }
            return;
        }

        player.sendMessage("[scarlet]Core spawn failed!Not enougth resorces.");
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

        handler.register("reactors", "List all thorium reactors in the map.", args -> {
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    //loop through and log all found reactors
                    if(world.tile(x, y).block() == Blocks.thoriumReactor){
                        Log.info("Reactor at {0}, {1}", x, y);
                    }
                }
            }
        });
        handler.register("show-password","If you havent set commands wont be vayable.",args->{
            if(password=="none"){
                Log.info("Password wosnt set yet.");
                return;
            }
            Log.info("Password is "+password+".");
        });
        handler.register("set-password","<password>","Sets password for special commands.",args->
        {
            if(args[0].length()<4){
                Log.info("The password it too short , minimum is 4 characters.");
                return;
            }
            password=args[0];
            Log.info("Passwor wos set.");

        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){

        handler.<Player>register("add","add items",(args,player)->{
            Teams.TeamData teamData = state.teams.get(player.getTeam());
            CoreBlock.CoreEntity core = teamData.cores.first();
            for(Item item:content.items()){

                if(verifi_item(item)){continue;}
                core.items.add(item, 4000);
            }
        });
        handler.<Player>register("storige-info","Displays information about storige.",(arg,player)->{
            player.sendMessage("The size if storige is"+Integer.toString(get_storige_size())+"!");

        });
        handler.<Player>register("reply", "<text...>", "A simple ping command that echoes a player's text.", (args, player) -> {
            player.sendMessage("You said: [accent] " + args[0]);
        });

        //register a whisper command which can be used to send other players messages
        handler.<Player>register("whisper", "<player> <text...>", "Whisper text to another player.", (args, player) -> {
            //find player by name
            Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(args[0]));

            //give error message with scarlet-colored text if player isn't found
            if(other == null){
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }
            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1]);
        });
        handler.<Player>register("build-core","<small/normal/big>", "Make new core", (arg, player) -> {
            // Core type
            int storige=get_storige_size();
            Block to_build = Blocks.coreShard;
            int cost=(int)(storige*.25f);
            switch(arg[0]){
                case "normal":
                    to_build = Blocks.coreFoundation;
                    cost=(int)(storige*.5f);
                    break;
                case "big":
                    to_build = Blocks.coreNucleus;
                    cost=(int)(storige*.75f);
                    break;
            }
            build_core(cost,player,to_build);
        });
        handler.<Player>register("layout-show","Shows how may resorce you have stored in layout.",(arg, player) -> {
            player.sendMessage("[green]LAYOUT STATE");
            show_layout(player);

        });
        handler.<Player>register("layout-use","Uses layout resorces.",(arg, player) -> {
            use_layout(player);
            player.sendMessage("[green]Layout wos used!");
        });
        handler.<Player>register("layout-fill","Fills layout with resorces from core up to 5000 for each resorce",(arg, player) -> {
            fill_layout(player);
            player.sendMessage("[green]Layout wos filled!");
        });
    }


}

