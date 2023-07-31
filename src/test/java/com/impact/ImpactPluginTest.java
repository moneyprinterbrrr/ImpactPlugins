package com.impact;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;
import com.example.PrayerFlicker.PrayerFlickerPlugin;
import com.impact.Construction.ConstructionPlugin;
import com.impact.DialogContinue.DialogContinuePlugin;
import com.impact.HideCast.HideCastPlugin;
import com.impact.PowerGather.PowerGatherPlugin;
import com.impact.NeverLog.NeverLogPlugin;
import com.impact.Alchemy.AlchemyPlugin;
import com.impact.ReorderPrayers.ReorderPrayersPlugin;
import com.impact.SpecialAttackBar.SpecialAttackBarPlugin;
import com.impact.Thiever.ThieverPlugin;
import com.impact.Firemaker.FiremakerPlugin;
import com.impact.ItemCombine.ItemCombinePlugin;
import com.impact.LavaCrafter.LavaCrafterPlugin;
import com.impact.NightmareZone.NightmareZonePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// TODO: jar builder

public class ImpactPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(
                EthanApiPlugin.class, PacketUtilsPlugin.class, PowerGatherPlugin.class,
                NeverLogPlugin.class, AlchemyPlugin.class, ThieverPlugin.class,
                FiremakerPlugin.class, ItemCombinePlugin.class, LavaCrafterPlugin.class,
                NightmareZonePlugin.class, SpecialAttackBarPlugin.class, ReorderPrayersPlugin.class,
                HideCastPlugin.class, DialogContinuePlugin.class, PrayerFlickerPlugin.class,
                ConstructionPlugin.class
        );
        RuneLite.main(args);
    }
}