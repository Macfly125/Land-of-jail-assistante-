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
| Héros | Liste, fiche héros, fragments et compétences | pending | nom, portrait, rareté, niveau, étoiles, fragments, compétences |
| Équipements | Inventaire et panneau d'amélioration | located | emplacement, qualité, niveau, matériaux, statistiques |
| Forticlad | Sélecteur, pièces et calculateur d'amélioration | pending | pièce, rang/niveau, matériau, quantité, coût |
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
