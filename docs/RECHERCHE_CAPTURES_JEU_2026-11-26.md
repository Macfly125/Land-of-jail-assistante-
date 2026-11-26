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
