# Évaluation d'un moteur OCR cyrillique pour Android

Date : 2026-11-27  
Branche : `audit/ameliorations-android`

## Constat vérifié dans le code

`LandsOfJailOcrParser` exécute actuellement les recognizers ML Kit Latin, chinois, japonais et coréen. Il n'existe pas de recognizer cyrillique dans la chaîne actuelle. Les dictionnaires russes du classificateur de catégories peuvent reconnaître du texte russe **déjà extrait**, mais ne prouvent pas que les pixels d'une capture russe sont lus.

Le module Android cible API 35, minSdk 26 et construit un APK universel avec les splits ABI désactivés. L'ajout d'une bibliothèque OCR native doit donc être évalué sur la taille du paquet, les ABI embarquées, le temps de chargement et la mémoire ; aucun coût binaire précis ne doit être annoncé avant un build comparatif.

## Option étudiée : Tesseract4Android

Références officielles consultées :

- [Tesseract4Android README](https://github.com/adaptech-cz/Tesseract4Android)
- [Tesseract4Android releases](https://github.com/adaptech-cz/Tesseract4Android/releases)
- [rus.traineddata — tessdata](https://github.com/tesseract-ocr/tessdata/blob/main/rus.traineddata)
- [rus.traineddata — tessdata_best](https://github.com/tesseract-ocr/tessdata_best/blob/main/rus.traineddata)
- [Documentation des modèles Tesseract](https://tesseract-ocr.github.io/tessdoc/Data-Files.html)

Éléments confirmés :

- Tesseract4Android 4.9.0 utilise Tesseract OCR 5.5.1 et Leptonica 1.85.0.
- Android 5.0 / API 21 minimum ; compatible avec le minSdk 26 de l'application.
- Deux variantes sont publiées : Standard mono-thread et OpenMP multi-thread.
- Les fichiers de modèles v4 doivent être placés dans un dossier `tessdata` lisible par l'application.
- Pour Android 10/API 29 et plus, le stockage privé de l'application est le chemin adapté.
- La documentation indique qu'un modèle peut être téléchargé plutôt que livré dans l'APK, afin d'éviter de dupliquer les données linguistiques.
- La page GitHub du modèle russe `rus.traineddata` indique environ 19 Mo dans le dépôt `tessdata` et environ 14,6 Mo dans `tessdata_best`. Ces tailles ne sont pas une mesure de la variante `tessdata_fast` et doivent être revérifiées sur l'asset exact avant intégration.
- Les modèles `tessdata_fast` privilégient la vitesse ; `tessdata_best` privilégie la précision au prix d'une reconnaissance plus lente. Les modèles fast/best utilisent le moteur LSTM (OEM 1).

## Décision actuelle

**Ne pas ajouter Tesseract4Android à l'APK dans ce commit.** La bibliothèque contient du code natif et le projet distribue un APK universel. Le gain réel en reconnaissance russe n'a pas encore été mesuré sur des captures Lands of Jail, et le coût de taille/performances n'a pas été mesuré sur un appareil.

Le choix d'architecture à tester en prototype est :

1. Conserver ML Kit comme chemin principal pour les scripts actuellement couverts.
2. Déclencher le chemin russe uniquement si l'écran ou le texte contient des indices cyrilliques plausibles, ou si ML Kit n'a pas fourni de texte utile.
3. Télécharger à la demande un seul modèle russe versionné, depuis une URL officielle immuable.
4. Écrire dans un fichier temporaire privé, vérifier la taille attendue et le SHA-256 publié/configuré, puis renommer atomiquement vers `filesDir/tesseract/tessdata/rus.traineddata`.
5. Gérer téléchargement interrompu, reprise ou redémarrage propre, espace disque insuffisant, réseau absent, modèle corrompu et suppression/remplacement de version.
6. Si le modèle manque et que l'app est hors ligne, retourner un statut explicite « langue russe non disponible hors ligne » ; ne pas convertir une classification lexicale en preuve OCR.
7. Utiliser une instance Tesseract isolée, appeler `recycle()` dans tous les chemins de sortie et exécuter la reconnaissance hors du thread UI.
8. Limiter les zones de lecture et les caractères autorisés lorsque le type de panneau est connu, sans imposer une whitelist qui supprimerait les valeurs ou libellés inconnus.

## Conditions obligatoires avant intégration

- Un petit corpus d'images de jeu réellement en cyrillique, avec provenance, résolution et annotation humaine.
- Comparaison de `tessdata_fast` et `tessdata_best` sur les mêmes images : taux exact par ligne, nombres exacts, temps de traitement et mémoire.
- Tests instrumentés sur émulateur et au moins un appareil Android réel avant de déclarer la fonctionnalité opérationnelle.
- Mesure de l'augmentation exacte de l'APK universel et de l'espace disque occupé par le modèle téléchargé.
- Vérification du SHA-256, du type MIME, de la taille et de l'URL de téléchargement ; aucune donnée modèle exécutée ou décompressée depuis un emplacement public partagé.
- Test de non-régression : aucune valeur incertaine ne doit être enregistrée comme progression confirmée.

## Limite à ne pas masquer

Les tests de dictionnaire russe du classificateur sont des tests de texte déjà disponible. Ils ne constituent pas des tests de reconnaissance de pixels. Tant que le prototype et le corpus cyrillique ne passent pas les conditions ci-dessus, l'application doit annoncer cette limite plutôt que prétendre prendre en charge les captures russes.

## État

- Évaluation documentaire : effectuée.
- Dépendance native intégrée : non.
- Modèle russe intégré ou téléchargé par l'application : non.
- Corpus de captures cyrilliques annotées : non disponible dans le dépôt.
- Validation OCR russe sur images : non effectuée.
