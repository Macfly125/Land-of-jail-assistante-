# Recherche de captures publiques — Land of Jail
Date de consultation : 2026-11-26

## Objectif
Repérer des images et vidéos publiques du jeu pouvant aider à constituer un corpus de référence pour l'OCR et la classification des écrans Android.

## Captures officielles Google Play
Page officielle du jeu : https://play.google.com/store/apps/details?id=com.justgame.jails&hl=fr

Captures ouvertes depuis la galerie Google Play :
- https://play-lh.googleusercontent.com/vUq5VVXkH-wJBhoVKbMsxG0spTr1atyL-03avfBMFmFEFpp-7WWuhAF2Wd4u3PWT92EJXMLo0cX-_iwH4rSo6A=w526-h296
- https://play-lh.googleusercontent.com/mjZIcCL45F-10iE-UbvTcg-tU8IYqNlG9VB5Yf2Lkn3QE6msLBLalEljEOjdV31-ryF7H8AD_dV8XlcU1YwzYw=w526-h296
- https://play-lh.googleusercontent.com/Xb-kGYZKuH6fhxSWA--NsWYSlVn_2HcFhPtJUES7eVwCYrHdU7gDKyDk8fpF5mBfZmSHvlBMzXzd_zc2L-jT6g=w526-h296
- https://play-lh.googleusercontent.com/zBqq8h4JsMlao7JThiMj6ySOg0BxisnjfQwt0Y2mllgEwmXFVZ-C-0FtEtQxkX2SyOeAhBZvNjJNx-xFj8ZupA=w526-h296

Constat : les images accessibles dans cette galerie sont principalement des visuels promotionnels et des scènes de gestion de prison. Elles ne couvrent pas de manière démontrée les écrans techniques du compte (barre de ressources, QG, robots, équipements, Laboratoire, Forticlad, inventaire et niveaux). Elles ne doivent donc pas être utilisées comme vérité terrain pour ces catégories.

## Sources de gameplay complémentaires
- FoxData, interface de gestion et panneau d'amélioration d'une cellule : https://foxdata.com/en/blogs/lands-of-jail-a-hybrid-slg-hit-redefining-growth-strategy-in-2025/
- AppGrowing, scènes de gameplay : https://appgrowing.net/blog/en/landsofjail/
- LOJ Wiki, guide indépendant et illustrations des systèmes (héros, robots, équipements, Forticlad, satellites) : https://loj-wiki.com/
- Vidéo gameplay robot niveau 25 : https://www.youtube.com/watch?v=250tRCU0-GU
- Aperçu saison 7 (robots, Forticlad, troupes, héros et événements) : https://www.youtube.com/watch?v=n-2pI8GcUVI

## Règles d'utilisation pour les tests
1. Distinguer capture de gameplay, publicité, illustration de guide et icône isolée.
2. Une image promotionnelle ne prouve pas la bonne reconnaissance d'un écran technique.
3. Ne pas annoter une catégorie (robots, héros, ressources, etc.) sans indice visible et non ambigu.
4. Ne pas enregistrer de niveaux, quantités, coûts ou matériaux déduits d'une image floue.
5. Les images tierces restent soumises aux droits de leurs auteurs ; ce registre conserve les références, pas une autorisation de redistribution.
6. Pour la validation OCR réelle, privilégier les captures d'écran brutes du jeu et les images extraites de séquences de gameplay avec contexte et étiquette vérifiée.

## État
- Recherche web effectuée et références classées.
- Aucun corpus local de captures techniques annotées n'est encore validé.
- Prochaine étape : récupérer des captures publiques de gameplay montrant clairement les écrans techniques, ou obtenir des captures brutes de joueurs avec autorisation, puis les intégrer comme fixtures de test.


## Références techniques supplémentaires — 26/11/2026

### LOJ XLab
- Laboratoire documentaire : https://lojxlab.com/the-lab/ — inventorie les systèmes héros, progression, événements et combat ; les tableaux sont annoncés comme contrôlés et datés.
- Hangar d'armures/robots : https://loj.dookee.ru/en/development/hangar/ — catalogue de 14 robots et compétences ; plusieurs fiches distinguent explicitement les données provenant de captures joueurs, les statistiques non vérifiées et les effets encore inconnus.
- Robots — visuel de synthèse : https://loj.dookee.ru/images/robots/pikaboo-robot-icons.webp — utile pour repérer les portraits et les noms, mais c'est une infographie communautaire, pas une capture brute de l'interface du jeu.
- Laboratoire Forticlad : https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/ — tableau de coûts par palier ; la page mentionne des observations en jeu et des captures datées, mais le tableau ne remplace pas une image source brute.
- Robot upgrade costs : https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/ — données de progression et coûts, à utiliser comme référence de cohérence et non comme exemple OCR d'écran.
- Laboratoire principal : https://lojwiki.com/lab/ — structure des arbres Logistique, Économie et Militaire ; page distincte du laboratoire Forticlad.

### Classement pour le corpus OCR
- [x] Références techniques complémentaires identifiées pour robots, laboratoire principal, Forticlad et coûts d'amélioration.
- [x] Séparer les captures de joueurs citées comme origine des données, les infographies de portraits et les tableaux reconstruits : seuls les vrais écrans de jeu peuvent servir à mesurer l'OCR visuel.
- [ ] Récupérer les fichiers image originaux des captures de joueurs avec autorisation et métadonnées (catégorie, langue, résolution, écran, date, niveau).
- [ ] Constituer des fixtures distinctes pour ressources/barre supérieure, QG, robots, héros/équipements, laboratoire et Forticlad.
- [ ] N'activer aucune donnée issue d'une infographie comme vérité terrain OCR ; utiliser les tableaux uniquement pour vérifier la cohérence des valeurs extraites.

Limite constatée : les pages publiques permettent d'identifier des références et parfois l'existence de captures joueurs, mais les images brutes techniques ne sont pas toutes directement accessibles ni autorisées à la redistribution. Aucun fichier image brut supplémentaire n'a été ajouté au dépôt dans cette étape.


## Captures de gameplay repérées — recherche complémentaire 26/11/2026

Ces pages exposent des images de gameplay identifiables. Elles sont référencées pour examen ; elles ne sont pas copiées dans le dépôt sans autorisation de réutilisation.

- GameWith (article japonais de lancement) : https://gamewith.jp/gamedb/13266/articles/42321 — capture de l'écran principal isométrique avec barre supérieure et quantités de ressources (argent, nourriture, bois, acier, essence, diamants), cellules et navigation. Référence utile pour la détection de la barre de ressources, mais interface japonaise et ancienne.
- Yumatti (capture publiée dans un guide de progression) : https://www.yumatti.com/point-activity-land-object/ — écran de montée de niveau d'un prisonnier, avec niveau actuel/cible, bonus, coût et bouton d'amélioration. Catégorie : prisonnier/personnage, pas héros.
- Publication X de @nyanreimama : https://x.com/nyanreimama/status/2033891222243463315 — écran de progression du Bureau du directeur niveau 24, bénéfices, prérequis bâtiments et coûts. À vérifier manuellement car l'image est une capture partagée par un joueur et le contexte serveur/langue est limité.
- Vidéo XcelPlaysGames : https://www.youtube.com/watch?v=bS5iAvk0RFA — processus complet d'amélioration des armures/robots 9 et 10 ; source potentielle d'images de séquence à examiner, mais aucune frame n'a été extraite dans ce dépôt.
- Uptodown : https://lands-of-jail.en.uptodown.com/android — galerie secondaire d'images de gameplay, à trier écran par écran avant toute utilisation OCR.

### Catégories couvertes par ces nouvelles références
- [x] Barre de ressources et écran principal : une capture de gameplay publique identifiée.
- [x] Bureau du directeur/QG : une capture publique d'amélioration avec prérequis et coûts identifiée.
- [x] Prisonnier/personnage : une capture publique d'amélioration identifiée ; ne pas la confondre avec l'écran Héros.
- [ ] Robot/armure : vidéo d'amélioration identifiée, extraction de frames et validation des chiffres non effectuées.
- [ ] Héros, équipement, recherche et Forticlad : trouver des captures brutes de ces écrans et leurs contextes.

Aucun fichier image n'a été ajouté au dépôt : les sources sont externes et les droits de réutilisation doivent être vérifiés. Ne pas entraîner ni valider une métrique OCR sur les descriptions textuelles des pages ; utiliser les pixels d'une capture autorisée et une annotation humaine indépendante.


## Références de robots et laboratoires — approfondissement 26/11/2026

### Hangar robots / armures
- LOJ Dookee, catalogue Power Armor : https://loj.dookee.ru/en/development/hangar/ — 14 fiches de robots, portraits, statistiques par niveau et descriptions de compétences. La page précise les cas où les données proviennent de captures joueurs, les chiffres non vérifiés et les valeurs non disponibles.
- La fiche inclut notamment Light Cone (capture du 30/01/2026 au niveau 60), Ranger (statistiques au niveau 9), Yokozuna (niveau 10) et Musashimaru (niveau 40). Ces fiches permettent de construire des cas de contrôle de cohérence, mais leurs tableaux ne sont pas des images brutes pour évaluer l'OCR.
- Vidéo Dauntless, robot n°12 / saison 7 : https://www.youtube.com/watch?v=LCYZfp55Ml0 — description annonce statistiques, compétences Arrest/War et les 15 noyaux des modules Barrier, Formation et Solo. Vidéo repérée, aucune image extraite.

### Laboratoires et coûts
- Forticlad Lab : https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/ — tableau de coûts et observations en jeu datées du 06/10/2026 ; les captures d'origine sont créditées à MIRÖX (S216).
- Robots niveaux 1–100 et Core I–V : https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/ — coûts documentés, avec conflit explicitement signalé sur les Advanced Power Modules 60–70 (40 contre 140). Ne pas masquer ce conflit dans les données de l'application.
- Laboratoire principal : https://lojwiki.com/buildings/lab/ — niveaux du bâtiment, coûts et prérequis jusqu'au niveau 30 ; à distinguer du Forticlad Lab.
- Bibliothèque LOJ XLab : https://lojxlab.com/library/ — catalogue des guides communautaires avec état de vérification (« Observed », « Tested ») et crédit aux auteurs.

### Conséquences pour le développement
- [x] Identifier une source de référence détaillée pour les 14 robots et leurs données connues.
- [x] Identifier une vidéo de gameplay du robot Dauntless couvrant les 15 noyaux.
- [x] Identifier les références distinctes pour le Laboratoire classique et le Forticlad Lab.
- [x] Repérer et conserver explicitement le conflit de données 60–70 des modules avancés ; ne pas choisir une valeur comme vérité sans capture de l'écran du jeu.
- [ ] Extraire les frames des vidéos avec horodatage et annotation humaine.
- [ ] Obtenir les captures sources originales citées par les auteurs (notamment MIRÖX S216) avec autorisation d'usage en tests.
- [ ] Ajouter des fixtures OCR séparées pour chaque panneau de robot, noyau, laboratoire classique et Forticlad ; conserver résolution, langue, saison et provenance.

Aucune image binaire n'a été importée dans le dépôt dans cette étape. Les tableaux sont des références de cohérence, pas des preuves de reconnaissance d'image.


## Héros et équipements — sources de gameplay supplémentaires — 26/11/2026

### HEROpedia LOJ XLab
- https://lojxlab.com/heropedia/ — catalogue de 31 héros, saisons 1 à 7, avec classe, six compétences, équipement exclusif et type de bataille associé.
- La page précise une provenance issue d'écrans en jeu : 27 héros des saisons 1 à 6 capturés par MORÖX sur S216 en septembre–octobre 2026 ; les quatre héros de saison 7 proviennent d'un enregistrement d'écran de MIRÖX sur S216 daté du 16/11/2026.
- Les contrôles annoncés par la source portent sur 129 vérifications des données héros, 1 828 des compétences et 33 pour la saison 7. Ce sont des contrôles documentaires de la source, pas des tests OCR indépendants de notre application.
- Attention : la page indique que certaines illustrations de cartes sont des visuels de classe générés par le Lab et non les portraits originaux du jeu. Ne pas les utiliser comme gabarits de portraits.
- La page relie aussi les coûts d'étoiles, armes exclusives et promotions d'équipement 100–200 : utiles pour les références de cohérence, pas pour l'OCR de captures.

### Vidéo équipement héros
- https://www.youtube.com/watch?v=Zw6MbLf3h-8 — vidéo AllbuzhaGaming du 09/06/2026 consacrée à l'équipement des héros, aux types/effets, à l'amélioration et à la reformation, avec comparaison des niveaux 100 et 101.
- Vidéo repérée par recherche ; l'ouverture directe a été limitée par YouTube. Aucune frame n'a été extraite ni annotée.

### Galerie secondaire
- https://lands-of-jail.en.uptodown.com/android — la recherche web signale une galerie de gameplay (vidéo et captures). La page renvoie actuellement une erreur 410 à l'ouverture automatisée ; images non inspectées et non admissibles comme fixtures à ce stade.

### État et prochaines actions
- [x] Identifier une référence de données héros explicitement relevée à partir d'écrans de jeu, avec provenance, saisons et dates.
- [x] Identifier une vidéo dédiée à l'amélioration/reformation des équipements héros.
- [x] Documenter que les illustrations générées de classe ne sont pas de l'art original du jeu.
- [ ] Obtenir auprès des auteurs les captures originales des écrans héros/équipements avec autorisation d'usage en tests.
- [ ] Extraire des frames de la vidéo équipement avec horodatage, puis vérifier manuellement que chaque frame montre l'écran de jeu attendu.
- [ ] Constituer des fixtures distinctes : liste héros, fiche héros/compétences, équipement exclusif, équipement standard et reformation.
- [ ] Ne pas marquer la reconnaissance héros/équipement comme validée avant tests sur images réelles autorisées.

Aucun binaire image n'a été ajouté : cette étape ajoute des références et des limites de provenance uniquement.


### Vérification d'une fiche héros et de ses visuels — Edwin
- Fiche individuelle Edwin : https://lojxlab.com/heropedia/edwin/ — la page sépare explicitement le portrait du héros et l'image de son équipement exclusif Judicator.
- La source précise que ces visuels correspondent à l'art du jeu, et que les données viennent des écrans héros et équipement d'Edwin capturés sur S216 en septembre–octobre 2026 ; les six compétences proviennent des écrans Skills d'octobre 2026.
- Les images sont utiles comme références visuelles de l'identité du héros et de l'équipement, mais elles ne montrent pas nécessairement les panneaux UI complets avec niveaux, chiffres et boutons. Elles ne constituent donc pas, seules, des fixtures OCR d'écran complet.
- [ ] Demander les captures d'écran complètes et autorisées à l'auteur ; garder le portrait/équipement comme gabarits secondaires seulement si les droits et la résolution le permettent.


## Forticlad, laboratoires et robots — approfondissement — 26/11/2026

### Forticlad — calculateur de progression RUS
- https://loj.dookee.ru/en/development/forticlad/ — calculateur interactif couvrant le Bureau du directeur, les trois casernes et le Centre de communication, avec étapes payées, inventaire FC/AFC partagé, coûts manquants et ordre d'amélioration.
- Exemple de référence FC5 → FC6 : Bureau 1 800 FC / 120 AFC ; chaque caserne 800 FC / 50 AFC ; Centre de communication 450 FC / 25 AFC. Total multi-bâtiments 4 650 FC / 295 AFC. Il s'agit des valeurs du scénario par défaut du calculateur, pas d'un coût universel pour tout profil.
- La page avertit que les prérequis du Centre de communication pour le Bureau FC9–FC10 ne sont pas confirmés ; coûts de laboratoire après FC6 et infirmerie après FC8 non extrapolés.
- Mise à jour du jeu du 23/11/2026 : tâches Strongest Alliance comprenant désormais des dépenses de Forticlad Cores ; la source officielle citée ne donne pas les points ni le jour exact. À traiter comme contexte événementiel, pas comme coût.
- La page est un calculateur/tableau, pas une capture brute complète du jeu. Elle référence néanmoins des panneaux et valeurs observés.

### Forticlad Lab — niveaux et technologie
- https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/ — détail par technologie et palier, avec valeurs d'Hyperalliage, nourriture, bois, acier, gaz et Gold Cards.
- Observations de panneaux en jeu attribuées à MIRÖX, S216, 06/10/2026. Image référencée « Forticlad Lab in game, with the Lab hero card » ; fichier source non téléchargé ni ajouté au dépôt.
- Données de référence : 25 730 Hyperalliage pour atteindre le déblocage Bastion T11 d'un type de troupe ; 48 150 pour un laboratoire complet d'un type ; 144 450 pour les trois types. À utiliser comme vérification de cohérence, en conservant la date et la source.
- Les paliers sont distincts : T1 expédition (5 niveaux), T2 quatre technologies × 10, T3 rallye (12), T4 déblocage T11 (1), T5 soins et entraînement (2 × 10). Ne pas fusionner T2 ou T5 en une seule technologie.
- Les tableaux ne sont pas des fixtures OCR. Il faut récupérer l'image originale et les écrans complets autorisés.

### Robots — captures de joueurs citées
- https://loj.dookee.ru/en/development/hangar/ — catalogue 14 robots. La page cite des captures joueurs identifiées : Light Cone niveau 60 (30/01/2026), Yokozuna niveau 10 et Musashimaru niveau 40. Pour ces deux derniers, les chiffres verts après la flèche sont des aperçus d'amélioration et ne doivent pas être confondus avec les valeurs actuelles.
- Les valeurs des robots non observées restent inconnues ; les statistiques maximales d'affiches communautaires ne sont pas les valeurs actuelles du compte.
- La page mentionne une mise à jour annoncée le 23/10 et publiée le 26/10 sur la réduction de dégâts en Arrest Combat : les panneaux/effets doivent être revérifiés dans le client du jeu, sans recopier d'anciens pourcentages comme actuels.
- Le classement Trial Cage cité (Dauntless, Phantom Cat, Musashimaru, Infercore) est une recommandation communautaire à tester à niveaux, troupes et héros constants ; ce n'est pas une règle universelle.

### Laboratoire principal
- https://lojwiki.com/lab/ — trois arbres distincts : Logistique, Économie et Militaire ; prérequis par niveau et bâtiment Laboratoire requis. Le Laboratoire Forticlad est un bâtiment séparé, débloqué après le niveau 30.
- À utiliser pour construire le graphe des prérequis et le calculateur, mais les tables web ne remplacent pas des captures réelles pour l'OCR.

### État
- [x] Identifier le calculateur FC/AFC multi-bâtiments et relever ses limites de données.
- [x] Distinguer les cinq tiers du Forticlad Lab et leurs types de technologies.
- [x] Repérer les captures joueurs explicitement citées pour Light Cone, Yokozuna et Musashimaru.
- [x] Consigner la modification de jeu du 23/11/2026 sur les tâches Strongest Alliance Forticlad.
- [ ] Obtenir les captures originales avec accord des auteurs ; aucun binaire image ajouté.
- [ ] Créer fixtures OCR indépendantes pour les panneaux de robot, les noyaux, les étapes FC/AFC, les arbres du laboratoire principal et les technologies Forticlad.
- [ ] Confirmer dans le client les valeurs contestées et les effets après mise à jour ; aucune donnée incertaine ne doit alimenter un calcul ferme.

