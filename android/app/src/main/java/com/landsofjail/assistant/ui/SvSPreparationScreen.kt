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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.landsofjail.assistant.model.AppLanguage

private data class SvSAction(val fr:String,val en:String,val de:String,val tr:String,val ru:String,val zh:String,val it:String,val points:String="")
private data class SvSDay(val number:Int,val title:SvSAction,val actions:List<SvSAction>)
private fun SvSAction.text(l:AppLanguage)=when(l){AppLanguage.FR->fr;AppLanguage.EN->en;AppLanguage.DE->de;AppLanguage.TR->tr;AppLanguage.RU->ru;AppLanguage.ZH->zh;AppLanguage.IT->it}
private fun a(fr:String,en:String,points:String=""):SvSAction {
 val t=svsTranslations[fr]
 return SvSAction(fr,en,t?.get(0)?:en,t?.get(1)?:en,t?.get(2)?:en,t?.get(3)?:en,t?.get(4)?:en,points)
}
private val svsTranslations:Map<String,List<String>> = mapOf(
 "Forticlad, bâtiments et recherches" to listOf("Forticlad, Gebäude und Forschung","Forticlad, binalar ve araştırma","Фортиклад, здания и исследования","Forticlad、建筑与研究","Forticlad, edifici e ricerca"),
 "Noyaux Forticlad" to listOf("Forticlad-Kerne","Forticlad Çekirdekleri","Ядра Forticlad","Forticlad核心","Nuclei Forticlad"),
 "Hyperalliage" to listOf("Hyperlegierung","Hiper Alaşım","Гиперсплав","超合金","Iperlega"),
 "Noyaux Forticlad avancés" to listOf("Fortgeschrittene Forticlad-Kerne","Gelişmiş Forticlad Çekirdekleri","Улучшенные ядра Forticlad","高级Forticlad核心","Nuclei Forticlad avanzati"),
 "Tomes et satellites (puces, noyaux Orbit/Cosmic)" to listOf("Bände und Satelliten (Chips, Orbit-/Cosmic-Kerne)","Ciltler ve uydular (çipler, Orbit/Cosmic çekirdekleri)","Фолианты и спутники (чипы, ядра Orbit/Cosmic)","典籍与卫星（芯片、Orbit/Cosmic核心）","TomI e satelliti (chip, nuclei Orbit/Cosmic)"),
 "Accélérateurs construction, recherche et entraînement" to listOf("Beschleuniger für Bau, Forschung und Ausbildung","İnşaat, araştırma ve eğitim hızlandırmaları","Ускорители строительства, исследований и обучения","建筑、研究和训练加速道具","Accelerazioni costruzione, ricerca e addestramento"),
 "Collecte, fragments et spins" to listOf("Sammeln, Splitter und Drehs","Toplama, parçalar ve çarklar","Сбор ресурсов, фрагменты и прокрутки","采集、碎片与转盘","Raccolta, frammenti e giri"),
 "Collecter sur la carte : nourriture, bois, acier, essence" to listOf("Auf der Karte Nahrung, Holz, Stahl und Benzin sammeln","Haritada yiyecek, odun, çelik ve benzin topla","Собирайте на карте еду, дерево, сталь и бензин","在地图上采集食物、木材、钢材和汽油","Raccogli cibo, legno, acciaio e benzina sulla mappa"),
 "Fragments de héros rares / épiques / légendaires" to listOf("Seltene / epische / legendäre Heldenfragmente","Nadir / destansı / efsanevi kahraman parçaları","Редкие / эпические / легендарные фрагменты героев","稀有/史诗/传奇英雄碎片","Frammenti eroe rari / epici / leggendari"),
 "Spins de renforcement" to listOf("Verstärkungsdrehs","Güçlendirme çarkları","Прокрутки усиления","强化转盘","Giri di potenziamento"),
 "Noyaux Forticlad et Hyperalliage" to listOf("Forticlad-Kerne und Hyperlegierung","Forticlad çekirdekleri ve Hiper Alaşım","Ядра Forticlad и гиперсплав","Forticlad核心与超合金","Nuclei Forticlad e Iperlega"),
 "Fugitifs, rallies et modules" to listOf("Flüchtige, Rallyes und Module","Kaçaklar, ralliler ve modüller","Беглецы, ралли и модули","逃犯、集结与模块","Fuggitivi, rally e moduli"),
 "Vaincre des fugitifs (selon leur niveau)" to listOf("Flüchtige besiegen (je nach Level)","Kaçakları yen (seviyelerine göre)","Побеждайте беглецов (в зависимости от уровня)","击败逃犯（取决于等级）","Sconfiggi i fuggitivi (in base al livello)"),
 "Lancer un rallye contre une bête" to listOf("Eine Bestien-Rallye starten","Bir canavara karşı ralli başlat","Начать рейд на зверя","发起野兽集结","Avvia un rally contro una bestia"),
 "Ouvrir les coffres de modules Solo / Formation / Barrière" to listOf("Solo-/Formations-/Barriere-Modultruhen öffnen","Solo / Formasyon / Bariyer modül sandıklarını aç","Открывайте сундуки модулей: одиночный / строй / барьер","开启单人/阵型/屏障模块宝箱","Apri casse moduli Solo / Formazione / Barriera"),
 "Modules de puissance / avancés" to listOf("Leistungs- / fortgeschrittene Module","Güç / gelişmiş modüller","Модули силы / улучшенные модули","力量/高级模块","Moduli potenza / avanzati"),
 "Entraînement, promotion et équipements" to listOf("Ausbildung, Beförderung und Ausrüstung","Eğitim, terfi ve ekipman","Обучение, повышение и снаряжение","训练、晋升与装备","Addestramento, promozione ed equipaggiamento"),
 "Entraîner ou promouvoir les troupes" to listOf("Truppen ausbilden oder befördern","Birlikleri eğit veya terfi ettir","Обучайте или повышайте уровень войск","训练或晋升部队","Addestra o promuovi le truppe"),
 "Pièces d'équipement exclusif de héros" to listOf("Exklusive Heldenausrüstungsteile","Kahramana özel ekipman parçaları","Детали эксклюзивного снаряжения героев","英雄专属装备部件","Componenti equipaggiamento esclusivo eroi"),
 "Engrenages de précision, bobines de potentiel, aimants" to listOf("Präzisionszahnräder, Potenzialspulen, Magnete","Hassas dişliler, potansiyel bobinleri, mıknatıslar","Точные шестерни, катушки потенциала, магниты","精密齿轮、潜能线圈、磁铁","Ingranaggi di precisione, bobine potenziale, magneti"),
 "Puces satellite et noyaux Orbit/Cosmic" to listOf("Satellitenchips und Orbit-/Cosmic-Kerne","Uydu çipleri ve Orbit/Cosmic çekirdekleri","Спутниковые чипы и ядра Orbit/Cosmic","卫星芯片与Orbit/Cosmic核心","Chip satellitari e nuclei Orbit/Cosmic"),
 "Accélérateurs d'entraînement" to listOf("Ausbildungsbeschleuniger","Eğitim hızlandırmaları","Ускорители обучения","训练加速道具","Accelerazioni addestramento"),
 "Journée mixte : utiliser le reliquat éligible" to listOf("Gemischter Tag: übrige geeignete Vorräte verwenden","Karma gün: kalan uygun stokları kullan","Смешанный день: используйте оставшиеся подходящие запасы","混合日：使用剩余的适用物资","Giornata mista: usa le scorte idonee rimanenti"),
 "Coffres de modules et modules de puissance" to listOf("Modultruhen und Leistungsmodule","Modül sandıkları ve güç modülleri","Сундуки модулей и модули силы","模块宝箱与力量模块","Casse moduli e moduli potenza"),
 "Noyaux Forticlad, Hyperalliage et noyaux avancés" to listOf("Forticlad-Kerne, Hyperlegierung und fortgeschrittene Kerne","Forticlad çekirdekleri, Hiper Alaşım ve gelişmiş çekirdekler","Ядра Forticlad, гиперсплав и улучшенные ядра","Forticlad核心、超合金和高级核心","Nuclei Forticlad, Iperlega e nuclei avanzati"),
 "Pièces exclusives, engrenages, bobines et aimants" to listOf("Exklusive Teile, Zahnräder, Spulen und Magnete","Özel parçalar, dişliler, bobinler ve mıknatıslar","Эксклюзивные детали, шестерни, катушки и магниты","专属部件、齿轮、线圈和磁铁","Parti esclusive, ingranaggi, bobine e magneti"),
 "Note de collection gagnée" to listOf("Erhaltene Sammlungswertung","Kazanılan koleksiyon puanı","Полученный рейтинг коллекции","获得的收藏评分","Valutazione collezione ottenuta")
)
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

private data class SvSScoreItem(val fr:String,val en:String,val points:Long) {
 fun label(language:AppLanguage)=when(language){AppLanguage.FR->fr;else->en}
}
private val svsScoreItems=listOf(
 SvSScoreItem("Noyau Forticlad","Forticlad Core",3000),
 SvSScoreItem("Hyperalliage","Hyperalloy",1500),
 SvSScoreItem("Noyau Forticlad avancé","Advanced Forticlad Core",45000),
 SvSScoreItem("Minute d'accélérateur","Speedup minute",90),
 SvSScoreItem("Fragment rare","Rare Hero Shard",1050),
 SvSScoreItem("Fragment épique","Epic Hero Shard",3660),
 SvSScoreItem("Fragment légendaire","Legendary Hero Shard",9120),
 SvSScoreItem("Spin de renforcement","Reinforcement Spin",24000),
 SvSScoreItem("Pièce équipement exclusif","Exclusive Equipment Part",12000),
 SvSScoreItem("Engrenage de précision","Precision Gear",6000),
 SvSScoreItem("Bobine de potentiel","Potential Coil",60000),
 SvSScoreItem("Aimant","Magnet",3000),
 SvSScoreItem("Note de collection","Collection Rating",108)
)

@Composable
fun SvSPreparationScreen(language:AppLanguage){
 var selectedDay by remember { mutableIntStateOf(1) }
 var scoreIndex by remember { mutableIntStateOf(0) }
 var quantity by remember { mutableStateOf("0") }
 var scoreMenu by remember { mutableStateOf(false) }
 val scoreItem=svsScoreItems[scoreIndex]
 val calculatedPoints=(quantity.toLongOrNull() ?: 0L).coerceAtLeast(0L)*scoreItem.points
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
  item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(when(language){AppLanguage.FR->"Calculateur de points SVS";AppLanguage.EN->"SvS points calculator";AppLanguage.DE->"SvS-Punkterechner";AppLanguage.TR->"SvS puan hesaplayıcı";AppLanguage.RU->"Калькулятор очков SvS";AppLanguage.ZH->"SvS积分计算器";AppLanguage.IT->"Calcolatore punti SvS"},style=MaterialTheme.typography.titleSmall)
   Text(scoreItem.label(language)+" · "+scoreItem.points+" pts/unité",style=MaterialTheme.typography.bodySmall)
   Button(onClick={scoreMenu=true}){Text(when(language){AppLanguage.FR->"Choisir l'objet";AppLanguage.EN->"Choose item";else->"Choose item"})}
   DropdownMenu(expanded=scoreMenu,onDismissRequest={scoreMenu=false}){
    svsScoreItems.forEachIndexed{index,item->DropdownMenuItem(text={Text(item.label(language))},onClick={scoreIndex=index;scoreMenu=false})}
   }
   OutlinedTextField(value=quantity,onValueChange={quantity=it.filter{ch->ch.isDigit()}.take(12)},label={Text(when(language){AppLanguage.FR->"Quantité possédée / utilisée";AppLanguage.EN->"Owned / used quantity";AppLanguage.DE->"Besitzte / verwendete Menge";AppLanguage.TR->"Sahip olunan / kullanılan miktar";AppLanguage.RU->"Количество";AppLanguage.ZH->"拥有/使用数量";AppLanguage.IT->"Quantità posseduta / usata"})},singleLine=true)
   Text(when(language){AppLanguage.FR->"Total estimé :";AppLanguage.EN->"Estimated total:";AppLanguage.DE->"Geschätzte Gesamtpunktzahl:";AppLanguage.TR->"Tahmini toplam:";AppLanguage.RU->"Примерное количество очков:";AppLanguage.ZH->"预计总积分：";AppLanguage.IT->"Totale stimato:"}+" "+ "%,d".format(java.util.Locale.US,calculatedPoints)+" pts",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
   Text(when(language){AppLanguage.FR->"Estimation indicative : vérifie les valeurs et les règles du SVS en cours avant de dépenser.";AppLanguage.EN->"Reference estimate: verify the current SvS values and rules before spending.";AppLanguage.DE->"Richtwert: Prüfe vor dem Ausgeben die aktuellen SvS-Werte.";AppLanguage.TR->"Tahmini değerdir; harcamadan önce etkinlik kurallarını kontrol et.";AppLanguage.RU->"Оценка; перед расходованием проверьте правила текущего события.";AppLanguage.ZH->"仅供估算，使用前请核对当前活动规则。";AppLanguage.IT->"Stima indicativa: verifica le regole dell'evento prima di spendere."},style=MaterialTheme.typography.bodySmall)
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
