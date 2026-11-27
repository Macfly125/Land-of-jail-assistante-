# Contrat OCR — panneaux de bâtiments

Dernière mise à jour : 2026-11-27  
Branche : `audit/ameliorations-android`

Ce document définit les règles de lecture à appliquer avant de connecter l'OCR à la progression des bâtiments. Il ne constitue pas une preuve de reconnaissance visuelle : les références publiques recensées dans `screenshot-corpus-manifest.md` restent des candidates tant que les images originales ne sont pas collectées, annotées et relues.

## Champs indépendants

| Champ | Exemple de libellé | Règle |
|---|---|---|
| Bâtiment | Kitchen, Cell, Factory, Warden's Office | Exiger un nom reconnu dans son contexte de panneau. Ne pas déduire le bâtiment depuis le seul niveau. |
| Niveau actuel | Lv.1, Level 1, Niveau 1 | N'accepter que si le libellé de niveau et le nom du bâtiment sont associés au même panneau. |
| Progression | barre 50 %, 135/270 | Garder séparée du niveau et du coût. Un pourcentage seul ne valide aucun niveau. |
| Action / coût du bouton | Build 135/10 | Conserver comme texte brut tant que l'icône et la sémantique du coût ne sont pas établies. Ne pas le convertir automatiquement en stock global. |
| Installation / équipement | Stove | Champ de sous-élément du bâtiment, pas un bâtiment ni une ressource globale. |
| Besoin / production | Hunger 0+18.6 | Statistique locale du bâtiment. Ne pas l'ajouter au stock Nourriture du compte. |
| Durée | Time 0s+8s | Statistique locale / temps de production. Ne pas confondre avec la durée de construction issue du catalogue. |
| Prérequis | Warden's Office Lv.8 | Enregistrer comme exigence textuelle tant qu'un lien de dépendance fiable n'est pas calculé. |

## Règles de confirmation

1. Un bâtiment n'est confirmé que si son nom est reconnu et localisé dans le panneau correspondant.
2. Un niveau n'est confirmé que si le nombre est explicitement rattaché à un libellé de niveau. Le niveau d'un autre bâtiment visible à proximité ne doit jamais être repris.
3. Les nombres sans libellé restent des observations brutes, pas des niveaux, coûts ou quantités.
4. Si plusieurs niveaux ou panneaux sont visibles, ne pas associer par simple proximité de texte sans géométrie cohérente.
5. Les valeurs du catalogue de progression sont des références communautaires sourcées, non des valeurs vérifiées sur le compte du joueur. Elles ne doivent pas corriger ou remplacer une observation OCR.
6. Si le texte est tronqué, masqué, traduit dans une langue non couverte ou ambigu, conserver le champ comme inconnu et demander confirmation.
7. Le texte `Build 135/10`, `Hunger 0+18.6` et `Time 0s+8s` de la candidate Kitchen recensée ne doit pas servir de fixture de test : la provenance composite / originale et les droits restent à établir.

## Fixture de référence candidate

La candidate Malavida screenshot 7 est décrite dans le manifeste comme un panneau Kitchen Lv.1 avec barre de progression 50 %, Stove, Build 135/10, Hunger 0+18.6 et Time 0s+8s. Elle permet de définir les champs à séparer, mais pas de certifier la lecture des pixels ni d'entraîner un classificateur.

Source candidate : https://imag.malavida.com/mvimgbig/download-fs/lands-of-jail-40257-7.jpg

## Conditions avant activation en production

- Ajouter des captures de gameplay authentiques avec langue, résolution, provenance et annotation relue.
- Tester au minimum les panneaux Kitchen, Cell, Factory, Warden's Office et Command Center.
- Couvrir plusieurs niveaux, nombres proches, valeurs abrégées K/M, texte tronqué et plusieurs panneaux visibles.
- Vérifier les faux positifs : niveau pris dans un autre panneau, Hunger pris pour Food global, Build pris pour un coût de stock, Time pris pour une durée de construction.
- Garder les champs incertains hors du calculateur de progression jusqu'à confirmation.
