# Inventaire du corpus de captures OCR Land of Jail

Dernière mise à jour : 2026-11-26  
Branche de travail : `audit/ameliorations-android`

## Règles d'admission

Une entrée ne devient un échantillon OCR exploitable que si elle correspond à une véritable capture d'écran du jeu ou à une image extraite d'une séquence de gameplay identifiable.

- Une image promotionnelle, une illustration, une icône isolée ou une miniature ne valide pas la lecture d'un écran de jeu.
- Pour une image extraite d'une vidéo, conserver l'URL de la vidéo et l'horodatage exact. La qualifier de « frame vidéo », pas de capture originale fournie par le joueur.
- Ne jamais inventer un niveau, un coût, une quantité ou une ressource attendue lorsque l'image est ambiguë.
- Conserver la langue, la résolution et le cadrage d'origine. Ne pas recadrer l'unique exemplaire de référence.
- Toute annotation attendue doit être relue par un humain avant d'être utilisée comme vérité terrain.
- Une capture ne doit être publiée dans le dépôt que si ses droits et ses conditions de réutilisation sont compatibles. À défaut, conserver seulement la référence/source et demander une capture au joueur.

## États

- `pending` : aucun échantillon authentique validé dans le dépôt.
- `located` : source potentielle identifiée, pas encore contrôlée image par image.
- `collected` : fichier authentique obtenu et provenance enregistrée.
- `annotated` : champs attendus vérifiés manuellement.
- `ocr-tested` : test automatisé exécuté sur cet échantillon avec résultat conservé.

## Matrice de couverture

| Catégorie | Écran/panneau précis à couvrir | État au 2026-11-26 | Champs attendus à annoter |
|---|---|---|---|
| Ressources | Bandeau supérieur avec les compteurs et icônes | pending | type de ressource, quantité, position, confiance, ambiguïtés |
| Bâtiments / QG | Vue bâtiment et panneau d'amélioration | pending | nom, niveau actuel, niveau cible, coût, prérequis |
| Bureau du directeur | Panneau de niveau et exigences | located | niveau, conditions, ressources, valeurs manquantes |
| Robots | Liste, fiche robot et amélioration | located | nom, portrait, niveau, rang, matériaux, statistiques |
| Héros | Liste, fiche héros, fragments et compétences | located | nom, portrait, rareté, niveau, étoiles, fragments, compétences |
| Équipements | Inventaire et panneau d'amélioration | located | emplacement, qualité, niveau, matériaux, statistiques |
| Forticlad | Sélecteur, pièces et calculateur d'amélioration | located | pièce, rang/niveau, matériau, quantité, coût |
| Laboratoire standard | Recherche, niveau et coûts | located | technologie, niveau, durée, coût, prérequis |
| Troupes | Entraînement, capacités et composition | pending | type, quantité, niveau, puissance |
| Événements / Cage | Écran de formation et rapport de combat | pending | unités, héros, dégâts, résultats explicitement visibles |
| Autres écrans | Écran non classé à identifier | pending | texte exact et contexte d'écran |

**Important :** `located` indique seulement qu'une source candidate ou une référence à des observations de gameplay existe. Aucun fichier n'est encore collecté, annoté ou testé dans le dépôt. Le statut `pending` signifie qu'aucune capture authentique annotée n'est enregistrée.

## Sources candidates repérées — non validées comme échantillons

| Catégorie | Source candidate | Ce qu'elle apporte | Limite actuelle |
|---|---|---|---|
| Robots | [Vidéo XcelPlaysGames — Maxing Robots 9 and 10](https://www.youtube.com/watch?v=bS5iAvk0RFA) | La description annonce le processus complet d'amélioration des robots. | Frames et horodatages à extraire et contrôler ; aucune frame n'est encore retenue. |
| Bureau du directeur / QG | [LOJ XLab — exigences Warden's Office](https://lojxlab.com/library/lands-of-jail-office-level-upgrade-requirements-ar/) | Référence à des observations et captures du panneau d'amélioration de MIRÖX (S216, 6 octobre 2026). | Page principalement textuelle ; la capture originale doit être retrouvée et contrôlée. |
| Bureau du directeur / bâtiments | [LOJ XLab — Upgrade Planner](https://lojxlab.com/upgrades/) | Signale quatre cellules de données à confirmer par captures, dont le coût du Bureau du directeur niveau 7. | Données et demandes de preuves, pas un corpus d'images OCR. |
| Équipements / île | [Vidéo XcelPlaysGames — Island Guidance](https://www.youtube.com/watch?v=eTC0OB1jZwo) | Parcours vidéo de l'île et des améliorations. | Écrans exacts, langue, horodatages et lisibilité à vérifier. |
| Laboratoire | [LOJ Wiki — Lab](https://lojwiki.com/lab/) | Référence structurée des technologies et coûts. | Tableaux et illustrations ne sont pas des captures authentiques du panneau de recherche. |

| Robots | [LOJ XLab — Robot Upgrade Cost](https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/) | Données robot et références à des écrans de modules observés en jeu par MIRÖX (S216, 7 octobre 2026) ; signale aussi une divergence à trancher par capture pour les modules avancés 60–70. | Tableaux reconstruits en texte ; captures originales reliées à Discord non récupérées ici. |
| Héros | [LOJ XLab — Hero Star Cost](https://lojxlab.com/library/lands-of-jail-hero-star-upgrade-cost/) | Référence à des écrans d'étoiles et de fragments observés en jeu par MIRÖX (S216, 7 octobre 2026). | Aucune capture brute individuelle ni horodatage vidéo accessible dans la page. |
| Équipements | [LOJ XLab — Exclusive Weapons](https://lojxlab.com/library/lands-of-jail-exclusive-weapons-upgrade-cost/) et [Mastery Reformation](https://lojxlab.com/library/lands-of-jail-equipment-reformation-cost/) | Références à des écrans d'équipement exclusif et de reformation observés en jeu ; plusieurs valeurs sont explicitement confirmées et d'autres signalées comme incertaines. | Les sources originales sont créditées via Discord ; les captures brutes ne sont pas intégrées dans ces pages textuelles. |
| Forticlad | [LOJ XLab — Forticlad Lab](https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/) | Référence à des panneaux du laboratoire Forticlad et à trois arbres technologiques observés en jeu (MIRÖX, S216, 6 octobre 2026). | Données textuelles ; aucune image brute des panneaux/arbres disponible dans le registre. |

Les pages de référence peuvent aider à comprendre le vocabulaire et à trouver des valeurs candidates, mais ne doivent pas être utilisées comme vérité terrain visuelle. Les miniatures YouTube et les images promotionnelles Google Play sont exclues du corpus.

## Registre des échantillons

Ajouter une ligne par capture ou frame réellement contrôlée. Ne pas remplir les champs par supposition.

| ID | Catégorie | Écran exact | Type de preuve | Source URL | Horodatage | Langue | Résolution | Fichier / empreinte | Annotation relue | Test OCR |
|---|---|---|---|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — | — | — | — | — |

## Modèle de fiche d'échantillon

Pour chaque échantillon, documenter :

1. Identifiant stable et catégorie.
2. Nom exact de l'écran et du panneau.
3. Origine : capture utilisateur, capture publique, ou frame vidéo.
4. URL source et horodatage si vidéo.
5. Langue, résolution, orientation et éventuelle compression.
6. Champs réellement visibles, valeur attendue et zone approximative.
7. Texte partiellement masqué, recadré, flou ou ambigu.
8. Validation humaine (date et statut).
9. Version du parseur/OCR et résultat : exact, partiel, rejeté ou demande de confirmation.

## Contrôle complémentaire : portraits de héros

L'audit CI du 2026-11-26 a vérifié la disponibilité HTTP des 50 portraits configurés : 50 disponibles, 0 indisponible. Ce contrôle confirme l'accessibilité des fichiers distants, mais **ne confirme pas visuellement** que chaque image représente le bon héros et ne constitue pas un corpus de captures d'écran de fiches héros.

## Prochaine étape

Collecter et annoter au moins un échantillon authentique par catégorie, puis ajouter des cas difficiles (petit texte, nombres proches, plusieurs langues, éléments masqués). Tant que cette couverture n'existe pas, les scores heuristiques restent des indices de classement et ne doivent pas déclencher seuls une validation automatique.


## Extension de la recherche Internet — inventaire par catégorie (26 novembre 2026)

Cette section ajoute les références publiques découvertes pour préparer l'OCR. Elle distingue explicitement les **données dérivées d'écrans de jeu** des **fichiers image réellement accessibles**. Une page qui cite des captures ne signifie pas que les fichiers originaux sont téléchargeables ou réutilisables.

### Sources transversales

| Source | Informations accessibles | Utilité OCR | Nature de la preuve / limite |
|---|---|---|---|
| [LOJ Wiki FR](https://lojwiki.com/fr/) | 50 héros, 22 bâtiments, 3 quartiers de troupes, 176 technologies, 12 armures de puissance de prisonnier, 11 satellites, île du Refuge, arrestations, trésor, inventaire, alliance et événements. | Dictionnaires de noms, libellés, niveaux, catégories et champs attendus ; comparaison multilingue. | Base structurée de données de jeu, pas un corpus brut de captures. |
| [LOJ XLab Library](https://lojxlab.com/library/) | Bibliothèque communautaire avec guides, vidéos, tableaux de coûts et statuts « Observed/Tested ». | Références de vocabulaire, nomenclature des écrans et cohérence des champs. | Chaque élément doit être évalué séparément ; les tableaux reconstruits ne sont pas des images OCR. |
| [LOJ XLab HEROpedia](https://lojxlab.com/heropedia/) | Écrans Héros, équipement exclusif et compétences de 27 héros S1–S6 ; 4 héros S7 depuis un enregistrement d'écran du 16 novembre 2026. | Noms, classes, compétences, niveaux et champs des fiches héros. | La page indique une provenance par captures/recording, mais les illustrations de substitution générées ne sont pas des portraits de jeu. |
| [LOJ XLab Upgrade Planner](https://lojxlab.com/upgrades/) | 10 tableaux de coûts et parcours d'amélioration ; signale quatre cellules contradictoires ou à confirmer. | Contrôles de cohérence après extraction, jamais vérité terrain pour un pixel ou une capture. | Données tabulaires reconstruites ; les quatre anomalies restent à confirmer en jeu. |
| [LOJ Wiki Guides](https://lojwiki.com/fr/) | Guides et calculateurs pour bâtiments, troupes, labo, armures, satellites, équipement, événements. | Lexique et couverture des écrans supplémentaires. | Source secondaire ; valider les changements de version et la langue. |

### Références détaillées à exploiter

| Catégorie OCR | Références Internet | Champs/indices à intégrer au dictionnaire de reconnaissance | Statut image |
|---|---|---|---|
| Ressources / inventaire | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [Upgrade Planner](https://lojxlab.com/upgrades/) | Nourriture, bois, acier, essence/gasoline, or/argent, diamants ; icône, libellé, quantité, séparateurs de milliers, position du compteur, bandeau supérieur et inventaire. Ne pas inférer un stock depuis un mot isolé. | Références de données seulement ; aucune capture de bandeau de ressources validée. |
| Bâtiments / QG | [Détails des bâtiments LOJ Wiki](https://lojwiki.com/fr/) ; [Warden's Office](https://lojxlab.com/library/lands-of-jail-office-level-upgrade-requirements/) | 22 bâtiments ; nom, niveau courant/cible, bouton d'amélioration, coût par ressource, durée, prérequis, bâtiment requis, niveau du Bureau du directeur. | LOJ XLab cite les panneaux en jeu de MIRÖX (S216, 6 octobre 2026), mais les images brutes ne sont pas dans le dépôt. |
| Bureau du directeur / Forticlad | [Table Warden's Office](https://lojxlab.com/library/lands-of-jail-office-level-upgrade-requirements/) ; [Planner](https://lojxlab.com/upgrades/) | Niveaux 2–30, niveaux Forticlad 1–8 répertoriés ; ressources, durées et paliers. Garder Forticlad 9–10 non mesurés tant qu'aucune preuve en jeu n'est fournie. | Observations de panneaux signalées ; pas de fichier image brut collecté. |
| Héros | [HEROpedia](https://lojxlab.com/heropedia/) ; [Hero Star costs](https://lojxlab.com/library/lands-of-jail-hero-star-upgrade-cost/) ; [LOJ Wiki FR](https://lojwiki.com/fr/) | 50 héros répertoriés par le Wiki ; nom/alias, classe (Shieldbearer/Bomber/Shooter), rareté, niveau, étoiles, fragments, compétences, équipement exclusif. Pour le portrait, exiger une correspondance visuelle validée et ne pas utiliser l'art de substitution. | Provenance d'écrans héros et d'un enregistrement S7 déclarée par XLab ; originaux non encore intégrés au corpus. |
| Robots / armure de prisonnier | [Robot Upgrade Cost](https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/) ; [LOJ Wiki FR](https://lojwiki.com/fr/) | 12 armures de puissance de prisonnier ; nom/type, niveau 1–100, modules de puissance, modules avancés, données d'armure, cœurs I–V, nœuds troupe/type, chips et statistiques. | XLab cite des écrans de nœuds observés le 7 octobre 2026 ; pas d'images brutes déposées. Conflit modules avancés 60–70 : 40 ou 140, à vérifier sur capture. |
| Équipement héros | [Hero Equipment](https://lojxlab.com/library/lands-of-jail-hero-equipment-100-200-cost/) ; [LOJ Wiki FR](https://lojwiki.com/fr/) | Emplacement, classe de troupe, niveau, EXP, boulons, aimants, bobines de potentiel, promotion, qualité/rareté, statistiques. | Données de tables ; retrouver les captures sources avant de les utiliser comme vérité terrain. |
| Reformation / armes exclusives | [Equipment Reformation](https://lojxlab.com/library/lands-of-jail-equipment-reformation-cost/) ; [Exclusive Weapons](https://lojxlab.com/library/lands-of-jail-exclusive-weapons-upgrade-cost/) | Niveau de reformation 1–20, engrenages, aimants ; arme exclusive 0–10, pièces, niveau, bonus et classe concernée. | Tables retranscrites de sources communautaires ; captures originales non collectées. |
| Forticlad / laboratoire Forticlad | [Forticlad Lab](https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/) ; [Planner](https://lojxlab.com/upgrades/) ; [LOJ Wiki FR](https://lojwiki.com/fr/) | Tiers, arbres technologiques, cinq étapes, type de troupe, hyperalliage, coûts, niveaux Bastion, prérequis et progression. | XLab indique des panneaux et arbres observés en jeu ; pas de captures brutes présentes. Anomalie gaz T4/Bastion : 85M ou 8,5M à vérifier. |
| Laboratoire standard | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [LOJ Wiki Lab](https://lojwiki.com/lab/) | 176 technologies ; nom de recherche, branche, niveau, coût, durée, prérequis, effet et bouton de recherche. | Référentiel structuré, pas de capture authentique validée. |
| Satellites | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [LOJ XLab Library](https://lojxlab.com/library/) | 11 satellites, rareté R/SR/SSR, niveau, ascension, disques de données, pièces planétaires, compétences et statistiques. | Références et tableaux ; captures brutes à retrouver. |
| Troupes / quartiers | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [LOJ XLab Upgrade Planner](https://lojxlab.com/upgrades/) | 3 quartiers et types Shieldbearer/Bomber/Shooter ; tier, quantité, coût d'entraînement/soin, durée, déblocage, puissance et capacité. | Données structurées ; pas de capture d'entraînement/soin annotée. |
| Cage / combats | [LOJ XLab Cage](https://lojxlab.com/) ; [LOJ Wiki Guides](https://lojwiki.com/fr/) | Écran de sélection, héros, troupes, rapports, dégâts infligés, résultats réellement visibles. Séparer dégâts infligés et statistiques défensives. | Guides et pages d'événements ; pas de capture de rapport Cage annotée dans le dépôt. |
| Île du Refuge | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [vidéo Island Guidance](https://www.youtube.com/watch?v=eTC0OB1jZwo) | Manoir, 9 bâtiments, décorations, objets de luxe, prisonniers, prospérité, coûts et tirages. | Une vidéo candidate est identifiée ; extraire les frames seulement après contrôle des horodatages et de la lisibilité. |
| Trésor / collections / tomes | [LOJ Wiki FR](https://lojwiki.com/fr/) ; [LOJ XLab Library](https://lojxlab.com/library/) | 6 pièces de collection, 18 tomes, rareté/étoiles, monnaies, niveaux, bonus et coûts. | Tables communautaires ; aucune capture brute annotée. |
| Alliance / sac / événements / arrestations | [LOJ Wiki FR](https://lojwiki.com/fr/) | Sac (1 350 objets répertoriés), alliance (technologies, bâtiments, boutique), calendrier d'événements, arrestation et récompenses. | Référentiel de vocabulaire et de catégories ; captures de chaque écran à collecter. |

### Règles d'usage des données trouvées en ligne

1. **Dictionnaire OCR :** utiliser les noms, alias, libellés, types de ressources, classes, technologies et termes des sources pour enrichir les listes de mots, sans faire d'un mot générique une preuve de catégorie.
2. **Contrôle de cohérence :** utiliser les coûts et niveaux des tableaux pour signaler une valeur improbable ou demander confirmation ; ne jamais corriger silencieusement le texte lu par l'OCR.
3. **Vraie vérité terrain :** seuls les pixels d'une capture de jeu ou d'une frame identifiée, annotée et relue peuvent alimenter les tests d'image. Une image générée, un pictogramme isolé, une bannière de site ou une table HTML ne compte pas.
4. **Frames vidéo :** enregistrer URL, horodatage, résolution, langue et auteur ; une frame ne devient pas une capture originale du joueur.
5. **Droits et provenance :** ne pas aspirer/repackager les images communautaires sans autorisation claire. Garder les liens et métadonnées tant que la réutilisation n'est pas établie.
6. **Conflits :** conserver les deux valeurs candidates, la source et le niveau de confiance. Exemple : Advanced Power Modules 60–70 (40 contre 140) et Forticlad T4/Bastion Gas (85M contre 8,5M).
7. **Statut de couverture :** toutes les références listées ici sont des sources documentaires candidates ; elles ne font pas passer une catégorie à `collected`, `annotated` ou `ocr-tested` sans fichiers et résultats de test réels.

### Priorités de collecte à partir des sources

- Priorité 1 : ressources/bandeau, bâtiments/QG/Bureau du directeur, héros, robots, équipements, Forticlad et laboratoire.
- Priorité 2 : troupes, Cage/rapports, satellites, île du Refuge, trésor/tomes.
- Priorité 3 : sac, alliance, arrestations et écrans d'événements.
- Pour chaque priorité, rechercher d'abord les captures originales créditées sur les pages sources et les frames vidéo identifiables ; si elles ne sont pas accessibles ou réutilisables, conserver la référence et demander une capture utilisateur ultérieure.


### Vérification visuelle de quelques illustrations de sources

Des liens d'images intégrés aux pages LOJ XLab ont été ouverts et inspectés :

- [Illustration Warden's Office](https://cdn.lojxlab.com/wp-content/uploads/2026/10/lojx-hero-office.jpg.webp) : composition éditoriale avec une petite vue du bureau en jeu. Trop petite et intégrée à une infographie pour servir de capture OCR de référence.
- [Illustration Hero Equipment](https://cdn.lojxlab.com/wp-content/uploads/2026/10/lojx-hero-hero-equipment.jpg.webp) : infographie qui montre une interface de promotion d'équipement (Exotic T2 → T3), les trois matériaux et les quantités. Utile pour repérer les zones et libellés, mais ce n'est pas le fichier brut de la capture.
- [Page Forticlad Lab](https://lojxlab.com/library/lands-of-jail-forticlad-lab-upgrade-cost/) : la page documente le tableau et les observations de laboratoire ; le lien d'image examiné renvoie à Discord et n'a pas fourni un fichier image public exploitable dans cette vérification.

Ces illustrations sont conservées comme **références visuelles documentaires uniquement**. Elles ne changent pas le statut du corpus : aucun échantillon OCR brut annoté n'est ajouté par cette inspection.
