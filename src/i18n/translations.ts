import { AdviceMode, AppLanguage } from '../types/game';

export interface UiCopy {
  navDashboard: string;
  navHeroes: string;
  navResources: string;
  navAdvice: string;
  navHistory: string;
  shareCapture: string;
  captureShort: string;
  dashboardTitle: string;
  dashboardSubtitle: string;
  adviceHeroTitle: string;
  adviceHeroDesc: string;
  searchInternetBtn: string;
  refreshInternetBtn: string;
  searchingBtn: string;
  sourcesTitle: string;
  uncertainTitle: string;
  tacticalSummaryTitle: string;
  recommendedCompTitle: string;
  roleLabel: string;
  heroesContextLabel: string;
  noHeroesYet: string;
  languageLabel: string;
  photosGalleryTitle: string;
}

export const UI_COPY: Record<AppLanguage, UiCopy> = {
  fr: {
    navDashboard: 'Tableau',
    navHeroes: 'Héros',
    navResources: 'Ressources',
    navAdvice: 'Conseils',
    navHistory: 'Historique',
    shareCapture: 'Partager une capture',
    captureShort: 'Capturer',
    dashboardTitle: 'Tableau de bord du compte',
    dashboardSubtitle:
      'Suivi par captures partagées ou analyse OCR autorisée à la demande. Aucune action automatique dans le jeu.',
    adviceHeroTitle: 'Conseils & Événements Lands of Jail',
    adviceHeroDesc:
      'Stratégies pour Trial Cage, Crystal Clash, Conquête de sable, SvS, Turmoil Surge et Gold Hunter avec sites dans votre langue et photos.',
    searchInternetBtn: 'Rechercher sur Internet (avec photos)',
    refreshInternetBtn: 'Actualiser les conseils & photos',
    searchingBtn: 'Recherche Internet en cours…',
    sourcesTitle: 'Sites dans votre langue, Photos & Dates',
    uncertainTitle: 'Points à vérifier en jeu',
    tacticalSummaryTitle: 'Synthèse tactique',
    recommendedCompTitle: 'Composition & synergie recommandée',
    roleLabel: 'Rôle du joueur pris en compte',
    heroesContextLabel: 'Héros visibles injectés dans la stratégie',
    noHeroesYet: 'Aucun héros extrait (conseils généraux selon le rôle)',
    languageLabel: 'Langue de l’application',
    photosGalleryTitle: 'Photos et schémas issus des guides',
  },
  tr: {
    navDashboard: 'Panel',
    navHeroes: 'Kahramanlar',
    navResources: 'Kaynaklar',
    navAdvice: 'Etkinlikler',
    navHistory: 'Geçmiş',
    shareCapture: 'Ekran görüntüsü paylaş',
    captureShort: 'Yakala',
    dashboardTitle: 'Hesap Kontrol Paneli',
    dashboardSubtitle:
      'Paylaşılan ekran görüntüleri ve izinli OCR analizi ile takip. Oyun içinde otomatik işlem yapılmaz.',
    adviceHeroTitle: 'Lands of Jail Etkinlikler ve Taktikler',
    adviceHeroDesc:
      'Deneme Kafesi, Kristal Çatışması, Kum Denizi Fethi, SvS, Kargaşa Dalgası ve Altın Avcısı için Türkçe rehber siteleri ve fotoğraflar.',
    searchInternetBtn: 'İnternette Ara (fotoğraflı)',
    refreshInternetBtn: 'Rehber ve fotoğrafları yenile',
    searchingBtn: 'İnternet aranıyor…',
    sourcesTitle: 'Türkçe Kaynaklar, Fotoğraflar ve Tarihler',
    uncertainTitle: 'Oyun içinde doğrulanacak noktalar',
    tacticalSummaryTitle: 'Taktik Özeti',
    recommendedCompTitle: 'Önerilen Diziliş ve Sinerji',
    roleLabel: 'Oyuncu rolü',
    heroesContextLabel: 'Stratejiye eklenen kahramanlar',
    noHeroesYet: 'Henüz kahraman taranmadı (genel öneriler)',
    languageLabel: 'Uygulama dili',
    photosGalleryTitle: 'Rehberlerden Fotoğraflar',
  },
  ru: {
    navDashboard: 'Главная',
    navHeroes: 'Герои',
    navResources: 'Ресурсы',
    navAdvice: 'События',
    navHistory: 'История',
    shareCapture: 'Загрузить скрин',
    captureShort: 'Скрин',
    dashboardTitle: 'Панель аккаунта',
    dashboardSubtitle:
      'Отслеживание по скриншотам и OCR-анализу по вашему разрешению. Без вмешательства в игру.',
    adviceHeroTitle: 'События и гайды Lands of Jail',
    adviceHeroDesc:
      'Тактики для Trial Cage, Crystal Clash, Завоевания Песчаного моря, SvS, Волны смуты и Охотника за золотом с сайтами на русском и фото.',
    searchInternetBtn: 'Найти в Интернете (с фото)',
    refreshInternetBtn: 'Обновить гайды и фото',
    searchingBtn: 'Поиск в Интернете…',
    sourcesTitle: 'Источники на русском, фото и даты',
    uncertainTitle: 'Проверить в игре',
    tacticalSummaryTitle: 'Тактическая сводка',
    recommendedCompTitle: 'Рекомендуемый состав',
    roleLabel: 'Роль игрока',
    heroesContextLabel: 'Обнаруженные герои',
    noHeroesYet: 'Герои ещё не отсканированы (общие советы)',
    languageLabel: 'Язык приложения',
    photosGalleryTitle: 'Фотографии из гайдов',
  },
  zh: {
    navDashboard: '首页',
    navHeroes: '英雄',
    navResources: '资源',
    navAdvice: '活动攻略',
    navHistory: '历史',
    shareCapture: '分享截图',
    captureShort: '截图',
    dashboardTitle: '账号控制面板',
    dashboardSubtitle: '通过分享截图与授权 OCR 识别追踪账号进度，不在游戏内执行任何自动操作。',
    adviceHeroTitle: 'Lands of Jail 全活动攻略',
    adviceHeroDesc:
      '提供试炼、水晶争夺、沙海征服、跨服战(SvS)、动乱危机与黄金猎人攻略、中文站点来源与配图。',
    searchInternetBtn: '搜索网络攻略（含图片）',
    refreshInternetBtn: '刷新攻略与图片',
    searchingBtn: '正在搜索网络…',
    sourcesTitle: '中文攻略来源、图片与日期',
    uncertainTitle: '需在游戏内确认的项目',
    tacticalSummaryTitle: '战术总结',
    recommendedCompTitle: '推荐阵容与搭配',
    roleLabel: '玩家定位',
    heroesContextLabel: '已识别并加入策略的英雄',
    noHeroesYet: '尚未扫描到英雄（显示通用角色建议）',
    languageLabel: '应用语言',
    photosGalleryTitle: '攻略配图与示意图',
  },
  de: {
    navDashboard: 'Übersicht',
    navHeroes: 'Helden',
    navResources: 'Ressourcen',
    navAdvice: 'Events',
    navHistory: 'Verlauf',
    shareCapture: 'Screenshot teilen',
    captureShort: 'Erfassen',
    dashboardTitle: 'Konto-Übersicht',
    dashboardSubtitle:
      'Verfolgung über geteilte Screenshots oder freigegebenen OCR-Scan. Keine automatischen Aktionen im Spiel.',
    adviceHeroTitle: 'Lands of Jail Events & Taktiken',
    adviceHeroDesc:
      'Guides für Trial Cage, Kristallkampf, Sandmeer-Eroberung, SvS, Turmoil Surge und Goldjäger mit deutschen Seiten & Fotos.',
    searchInternetBtn: 'Im Internet suchen (mit Fotos)',
    refreshInternetBtn: 'Guides & Fotos aktualisieren',
    searchingBtn: 'Suche läuft…',
    sourcesTitle: 'Deutsche Quellen, Fotos & Datum',
    uncertainTitle: 'Im Spiel zu prüfen',
    tacticalSummaryTitle: 'Taktische Zusammenfassung',
    recommendedCompTitle: 'Empfohlene Aufstellung',
    roleLabel: 'Spielerrolle',
    heroesContextLabel: 'Sichtbare Helden in der Strategie',
    noHeroesYet: 'Noch keine Helden gescannt',
    languageLabel: 'App-Sprache',
    photosGalleryTitle: 'Bilder & Diagramme aus Guides',
  },
  en: {
    navDashboard: 'Dashboard',
    navHeroes: 'Heroes',
    navResources: 'Resources',
    navAdvice: 'Events',
    navHistory: 'History',
    shareCapture: 'Share screenshot',
    captureShort: 'Capture',
    dashboardTitle: 'Account Dashboard',
    dashboardSubtitle:
      'Track your account via shared screenshots or on-demand consented OCR capture. Zero in-game automation.',
    adviceHeroTitle: 'Lands of Jail Events & Strategy',
    adviceHeroDesc:
      'Live tactics for Trial Cage, Crystal Clash, Sandsea Conquest, SvS, Turmoil Surge and Gold Hunter with guide photos, dates and sources.',
    searchInternetBtn: 'Search Internet (with photos)',
    refreshInternetBtn: 'Refresh guides & photos',
    searchingBtn: 'Searching Internet…',
    sourcesTitle: 'English Sources, Photos & Dates',
    uncertainTitle: 'Items to verify in-game',
    tacticalSummaryTitle: 'Tactical Summary',
    recommendedCompTitle: 'Recommended Lineup & Synergy',
    roleLabel: 'Player role',
    heroesContextLabel: 'Visible heroes included in strategy',
    noHeroesYet: 'No heroes scanned yet (general role-based advice)',
    languageLabel: 'App language',
    photosGalleryTitle: 'Photos & diagrams from guides',
  },
};

export const EVENT_BRIEF_BY_LANG: Record<AdviceMode, Record<AppLanguage, string>> = {
  trial_cage: {
    fr: 'Placez vos Shieldbearers devant et vos Shooters DPS derrière. Séparez les compétences de Leader de ralliement et la règle des 4 compétences pour les Rejoigneurs.',
    tr: 'Ön safa Shieldbearers (tank) ve arkaya yüksek hasarlı Shooters koyun. Ralli Lideri ile Katılımcı kahramanların becerilerini ayırın.',
    ru: 'Поставьте Shieldbearers в первый ряд, а Shooters (урон) — назад. Разделяйте навыки лидера ралли и присоединяющихся (правило 4 навыков).',
    zh: '前排放置盾兵英雄（Shieldbearers），后排配置高输出射手（Shooters）。区分集结队长与跟车队员的前排技能规则。',
    de: 'Shieldbearers nach vorne, Shooters mit hohem Schaden nach hinten. Rally-Leader und Joiner (4-Skill-Regel) sauber trennen.',
    en: 'Place Shieldbearers frontline and high-DPS Shooters backline. Separate Rally Leader heroes from Joiners (4-skill rule).',
  },
  crystal_clash: {
    fr: 'La victoire dépend des Points de Cristal d’Alliance : capturez Crystal Prisons, Ironholds et Energy Hubs en Phase 1, poussez la Central Ironprison en Phase 2, et utilisez les fenêtres de soin (~12 min).',
    tr: 'Zafer İttifak Kristal Puanlarına bağlıdır: 1. Aşamada Crystal Prisons ve Energy Hubs ele geçirin, 2. Aşamada Central Ironprison’a yüklenin ve iyileşme pencerelerini kullanın.',
    ru: 'Победа зависит от очков кристаллов альянса: в 1-й фазе захватывайте Crystal Prisons и Energy Hubs, во 2-й — давите на Central Ironprison, используя окна лечения (~12 мин).',
    zh: '胜负取决于联盟水晶积分：第1阶段抢占Crystal Prisons与Energy Hubs，第2阶段强攻Central Ironprison，并合理利用约12分钟的治疗窗口。',
    de: 'Entscheidend sind Allianz-Kristallpunkte: Phase 1 Prisons & Energy Hubs sichern, Phase 2 Central Ironprison angreifen, Heilphasen (~12 Min) nutzen.',
    en: 'Win via Alliance Crystal Points: capture Crystal Prisons, Ironholds & Energy Hubs in Phase 1, push Central Ironprison in Phase 2, and cycle 12-min healing windows.',
  },
  sandsea_conquest: {
    fr: 'Conquête de sable : économisez votre Carburant (Fuel) sur les routes, protégez les Vessel Stations, prenez les Phares (Lighthouses) puis coordonnez la poussée vers S100 sous les ordres du Tacticien.',
    tr: 'Kum Denizi Fethi: yollarda Yakıtı (Fuel) koruyun, Gemi İstasyonlarını savunun, Deniz Fenerlerini alın ve Taktikçinizle S100’e koordineli ilerleyin.',
    ru: 'Завоевание Песчаного моря: экономьте топливо (Fuel) на маршрутах, защищайте станции судов, захватывайте маяки и координируйте рывок к S100 с тактиком.',
    zh: '沙海征服：在沙漠路线上节省燃料（Fuel），保护船坞站点，占领灯塔，并在战术官指挥下集结推进至S100。',
    de: 'Sandsea Conquest: Treibstoff (Fuel) sparen, Vessel Stations schützen, Leuchttürme einnehmen und mit dem Taktiker koordiniert auf S100 vorstoßen.',
    en: 'Sandsea Conquest: conserve squad Fuel on desert routes, defend Vessel Stations, secure Lighthouses and execute timed pushes to S100 under your Tactician.',
  },
  svs: {
    fr: 'SvS / Supreme Honor : réinitialisez la puissance héros/robots non essentiels avant le matchmaking si recommandé, économisez accélérations/éclats pour la Phase de Préparation, puis attaquez ou défendez en Phase Guerre.',
    tr: 'SvS / Yüce Onur: eşleşme öncesi gereksiz gücü sıfırlayın, hızlandırma ve parçaları Hazırlık Aşaması hedeflerine saklayın, Savaş Aşamasında saldırın veya savunun.',
    ru: 'SvS / Высшее отличие: оптимизируйте силу перед подбором, копите ускорения и осколки для фазы подготовки, затем атакуйте и защищайте в фазе войны.',
    zh: '跨服战（SvS / 至高荣耀）：匹配前优化战力，将加速道具与英雄碎片留到准备阶段达标，战争阶段协同攻防。',
    de: 'SvS / Supreme Honor: Macht vor dem Matchmaking prüfen, Beschleuniger/Splitter für die Vorbereitungsphase sparen und in der Kriegsphase zuschlagen.',
    en: 'SvS / Supreme Honor: optimize power before matchmaking, save speedups & shards for Preparation Phase milestones, then raid or defend prisons during War Phase.',
  },
  turmoil_surge: {
    fr: 'Turmoil Surge : 20 vagues sur ~40 min. Les vagues 10 & 20 ciblent les Tourelles (Sentry Turrets), et les vagues 7, 14 & 17 (Frénésie) visent les joueurs en ligne. Échangez vos garnisons avec vos alliés pour maximiser les points.',
    tr: 'Turmoil Surge: ~40 dakikada 20 dalga. 10. ve 20. dalgalar Kulelere, 7, 14 ve 17. dalgalar çevrimiçi oyunculara saldırır. Puan için müttefiklerle garnizon takası yapın.',
    ru: 'Волна смуты: 20 волн (~40 мин). Волны 10 и 20 бьют по турелям, а 7, 14 и 17 (ярость) — по игрокам онлайн. Обменивайтесь подкреплениями с союзниками.',
    zh: '动乱危机：共20波（约40分钟）。第10、20波攻击哨戒炮塔；第7、14、17波（狂暴）只攻击在线玩家。与盟友互换驻防部队以刷满积分。',
    de: 'Turmoil Surge: 20 Wellen (~40 Min). Welle 10 & 20 treffen Sentry Turrets; Frenzy-Wellen 7, 14 & 17 aktive Spieler. Garnisonen mit Verbündeten tauschen.',
    en: 'Turmoil Surge: 20 waves (~40 min). Waves 10 & 20 hit Sentry Turrets; Frenzy waves 7, 14 & 17 target online players. Swap defensive reinforcements with allies to maximize kill points.',
  },
  gorilla_legion: {
    fr: 'Boss individuel à dégâts : vérifiez le gorille et son bonus actif sur l’écran du jeu. Les ratios communautaires varient selon le jour et les sources ; commencez par 0/15/85 (Shield/Bomber/Shooter), puis comparez avec 0/0/100 ou le ratio du guide du jour. Gardez les héros, l’équipement et le nombre de troupes constants pendant le test.',
    tr: 'Bireysel hasar boss etkinliği: oyunda gorilin türünü ve aktif bonusunu doğrulayın. Topluluk oranları güne ve kaynağa göre değişir; 0/15/85 ile başlayıp 0/0/100 veya günlük rehber oranıyla karşılaştırın.',
    ru: 'Индивидуальный босс на урон: проверьте тип гориллы и активный бонус в игре. Сообщество расходится в пропорциях; начните с 0/15/85 и сравните с 0/0/100 или дневной рекомендацией.',
    zh: '个人伤害首领活动：先在游戏内确认大猩猩类型和当天加成。社区比例存在分歧，可先测试0/15/85，再与0/0/100或当日攻略比例比较。',
    de: 'Einzelboss mit Schadenswertung: Gorilla und aktiven Tagesbonus im Spiel prüfen. Community-Verhältnisse unterscheiden sich; mit 0/15/85 beginnen und 0/0/100 oder den Tageswert vergleichen.',
    en: 'Individual damage-boss event: verify the gorilla and active bonus in-game. Community ratios conflict; start at 0/15/85, then compare 0/0/100 or the day-specific guide ratio.',
  },
  gold_hunter: {
    fr: 'Gold Hunter & Power Sprint : ne dépensez vos éclats, livres de compétences, matériaux d’Arsenal et gros bâtiments que pendant les fenêtres de récompenses d’événement.',
    tr: 'Altın Avcısı ve Güç Yarışı: kahraman parçalarını, beceri kitaplarını, ekipman malzemelerini ve büyük inşaatları yalnızca etkinlik günlerinde harcayın.',
    ru: 'Охотник за золотом: тратьте осколки героев, книги навыков, материалы арсенала и завершайте крупные постройки только в окна наград события.',
    zh: '黄金猎人 & 冲榜赛：将英雄碎片、技能书、军械库材料以及大型建筑升级留到活动积分窗口再使用。',
    de: 'Gold Hunter & Power Sprint: Splitter, Skillbücher, Ausrüstungsmaterialien und große Bauabschlüsse gezielt für Event-Meilensteine aufsparen.',
    en: 'Gold Hunter & Power Sprint: hold hero shards, skill books, Arsenal gear materials and long building completions for milestone event windows.',
  },
};
