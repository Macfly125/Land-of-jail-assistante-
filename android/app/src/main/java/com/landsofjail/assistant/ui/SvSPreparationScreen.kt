package com.landsofjail.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.landsofjail.assistant.model.AppLanguage

private data class SvSAction(val fr:String,val en:String,val de:String,val tr:String,val ru:String,val zh:String,val it:String,val points:String="")
private data class SvSDay(val number:Int,val title:SvSAction,val actions:List<SvSAction>)
private fun SvSAction.text(l:AppLanguage)=when(l){AppLanguage.FR->fr;AppLanguage.EN->en;AppLanguage.DE->de;AppLanguage.TR->tr;AppLanguage.RU->ru;AppLanguage.ZH->zh;AppLanguage.IT->it}
private fun a(fr:String,en:String,points:String="")=SvSAction(fr,en,en,en,en,en,en,points)
private val svsDays=listOf(
 SvSDay(1,a("Forticlad, bâtiments et recherches","Forticlad, buildings and research"),listOf(
  a("Noyaux Forticlad","Forticlad Cores","3 000 pts/unité"),a("Hyperalliage","Hyperalloy","1 500 pts/unité"),a("Noyaux Forticlad avancés","Advanced Forticlad Cores","45 000 pts/unité"),a("Tomes et satellites (puces, noyaux Orbit/Cosmic)","Tomes and satellites (chips, Orbit/Cosmic Cores)","210 pts/rating · 9 800 / 3 500 / 43 750"),a("Accélérateurs construction, recherche et entraînement","Construction, research and training speedups","90 pts/minute")
 )),
 SvSDay(2,a("Collecte, fragments et spins","Gathering, shards and spins"),listOf(
  a("Collecter sur la carte : nourriture, bois, acier, essence","Gather Food, Wood, Steel and Gasoline on the map","6 pts par lot : 1 000 nourriture/bois · 200 acier · 50 essence"),a("Fragments de héros rares / épiques / légendaires","Rare / Epic / Legendary Hero Shards","1 050 / 3 660 / 9 120 pts"),a("Spins de renforcement","Reinforcement Spins","24 000 pts/spin"),a("Noyaux Forticlad et Hyperalliage","Forticlad Cores and Hyperalloy","3 000 / 1 500 pts"),a("Accélérateurs construction, recherche et entraînement","Construction, research and training speedups","90 pts/minute")
 )),
 SvSDay(3,a("Fugitifs, rallies et modules","Fugitives, rallies and modules"),listOf(
  a("Vaincre des fugitifs (selon leur niveau)","Defeat fugitives (points depend on level)","27 000 à 36 000 pts/cible"),a("Lancer un rallye contre une bête","Start a Beast rally","90 000 pts/rallye lancé"),a("Ouvrir les coffres de modules Solo / Formation / Barrière","Open Solo / Formation / Barrier Module Chests","1 824 / 3 600 / 5 400 pts"),a("Modules de puissance / avancés","Power / Advanced Power Modules","1 920 / 9 600 pts"),a("Fragments de héros et spins de renforcement","Hero shards and Reinforcement Spins","Comme au jour 2")
 )),
 SvSDay(4,a("Entraînement, promotion et équipements","Training, promotion and equipment"),listOf(
  a("Entraîner ou promouvoir les troupes","Train or promote troops","Points selon le niveau ; promotion = différence entre niveaux"),a("Pièces d'équipement exclusif de héros","Hero Exclusive Equipment Parts","12 000 pts/pièce"),a("Engrenages de précision, bobines de potentiel, aimants","Precision Gears, Potential Coils, Magnets","6 000 / 60 000 / 3 000 pts"),a("Puces satellite et noyaux Orbit/Cosmic","Satellite chips and Orbit/Cosmic Cores","9 800 / 3 500 / 43 750 pts"),a("Accélérateurs d'entraînement","Training speedups","Terminent les files, mais ne rapportent pas de points aujourd'hui")
 )),
 SvSDay(5,a("Journée mixte : utiliser le reliquat éligible","Mixed day: use remaining eligible stock"),listOf(
  a("Coffres de modules et modules de puissance","Module chests and Power Modules","Comme au jour 3"),a("Noyaux Forticlad, Hyperalliage et noyaux avancés","Forticlad Cores, Hyperalloy and Advanced Cores","3 000 / 1 500 / 45 000 pts"),a("Pièces exclusives, engrenages, bobines et aimants","Exclusive parts, gears, coils and magnets","12 000 / 6 000 / 60 000 / 3 000 pts"),a("Accélérateurs construction, recherche et entraînement","Construction, research and training speedups","90 pts/minute"),a("Note de collection gagnée","Collecting Rating gained","108 pts/point")
 ))
)

@Composable
fun SvSPreparationScreen(language:AppLanguage){
 var selectedDay by remember { mutableIntStateOf(1) }
 val day=svsDays.first{it.number==selectedDay}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
  item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text("SVS · "+when(language){AppLanguage.FR->"Préparation en 5 jours";AppLanguage.EN->"5-day preparation";AppLanguage.DE->"5-Tage-Vorbereitung";AppLanguage.TR->"5 günlük hazırlık";AppLanguage.RU->"Подготовка за 5 дней";AppLanguage.ZH->"五天备战";AppLanguage.IT->"Preparazione in 5 giorni"},style=MaterialTheme.typography.titleMedium)
   Text(when(language){AppLanguage.FR->"Commence à économiser avant le premier reset. Garde les matériaux jusqu'au jour où ils rapportent des points. Les valeurs sont des points, pas des quotas quotidiens.";AppLanguage.EN->"Start saving before the first reset. Hold materials until their scoring day. Values are points, not daily quotas.";else->"Save before the first reset and spend items on their scoring day. Values are points, not daily quotas."},style=MaterialTheme.typography.bodySmall)
  }}}
  item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
   Text(when(language){AppLanguage.FR->"À économiser avant le SVS";AppLanguage.EN->"Save before SvS";else->"Save before SvS"},style=MaterialTheme.typography.titleSmall)
   listOf("• Nourriture, bois, acier, essence : garde tes réserves pour les améliorations ; organise surtout leur collecte au jour 2.","• Accélérateurs construction/recherche/entraînement et universels : jours 1, 2 et 5.","• Noyaux Forticlad, Hyperalliage, noyaux avancés, tomes et satellites : jours 1, 2 ou 5 selon l'objet.","• Fragments de héros et spins : jours 2 et 3.","• Endurance : réserve-la pour les fugitifs et départs de rallye au jour 3.","• Coffres de modules : jours 3 ou 5 ; équipements et objets de puissance : jours 4 ou 5.","• Prépare tes files de troupes avant le jour 4, puis entraîne/promus ce jour-là.").forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
  }}}
  item { Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){(1..5).forEach{n->FilterChip(selected=selectedDay==n,onClick={selectedDay=n},label={Text("J$n")})}}}
  item { Text("Jour $selectedDay · ${day.title.text(language)}",style=MaterialTheme.typography.titleMedium) }
  items(day.actions){action->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(action.text(language));if(action.points.isNotBlank())Text(action.points,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)}}}
  item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   Text(when(language){AppLanguage.FR->"Règles importantes";AppLanguage.EN->"Important rules";else->"Important rules"},style=MaterialTheme.typography.titleSmall)
   Text(when(language){AppLanguage.FR->"Ouvrir des packs de ressources ne donne pas les points de collecte : nourriture, bois, acier et essence comptent lorsqu'ils sont récoltés sur la carte. Au jour 4, les accélérateurs d'entraînement terminent les files mais leurs minutes ne rapportent pas de points. La promotion rapporte la différence entre les points du niveau cible et ceux du niveau de départ. Vérifie toujours les tâches affichées dans ton événement avant de dépenser.";AppLanguage.EN->"Opening resource packs does not score gathering points: Food, Wood, Steel and Gasoline count when gathered on the map. On Day 4, training speedups finish queues but their minutes do not score. Promotion scores the difference between target- and current-tier troop points. Always check the in-game event tasks before spending.";else->"Resource packs do not score as gathering: resources count when collected on the map. Day 4 training speedup minutes do not score. Promotion scores the difference between troop tiers. Check the live event tasks before spending."},style=MaterialTheme.typography.bodySmall)
  }}}
 }
}
