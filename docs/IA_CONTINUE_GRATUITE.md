# Architecture cible — IA évolutive et continue (priorité Android, sans coût obligatoire)

## Objectif

Préparer Land of Jail Assistant à accueillir progressivement une intelligence plus autonome, sans rendre l'application dépendante d'un service payant, d'une clé API payante ou d'un serveur Cloud Run avec facturation.

Le mot « continu » désigne ici une veille et une amélioration régulières, pas une promesse de calcul IA 24 h/24 sur un téléphone éteint.

## Ce qui peut fonctionner gratuitement dès maintenant

- **Dans l'application Android :** OCR et reconnaissance locale des captures, catalogue de jeu embarqué, règles tactiques explicables, historique et cache local.
- **À l'ouverture :** une seule tentative de synchronisation/veille, puis lecture du cache si le réseau est indisponible. Pas de boucle d'actualisation permanente.
- **En arrière-plan Android :** tâches différées et économes via WorkManager, seulement si une tâche utile est en attente et avec contraintes réseau/batterie. Android peut retarder ces tâches ; ce n'est pas du temps réel.
- **Côté dépôt GitHub :** GitHub Actions planifié pour compiler, tester et archiver un APK/rapport. Les quotas gratuits et limites de durée de GitHub s'appliquent.
- **Données de jeu :** collecte limitée aux sources publiques accessibles. Discord privé, comptes privés et contenus nécessitant une connexion ne sont pas supposés accessibles.

## Architecture à construire par étapes

### 1. Séparer les moteurs

Définir une interface de fournisseur d'intelligence indépendante de l'écran et du stockage :

- moteur local déterministe : règles, calculs, catalogue et OCR ;
- moteur IA embarqué éventuel : uniquement si un modèle compatible Android et suffisamment léger est disponible ;
- fournisseur distant facultatif : désactivé par défaut, activable seulement si l'utilisateur configure lui-même un service.

L'application doit rester entièrement utilisable lorsque les fournisseurs IA distants sont absents.

### 2. Construire une mémoire de connaissances fiable

Chaque information de jeu doit conserver :
- source et URL ;
- date de collecte ;
- version/saison concernée ;
- langue ;
- niveau de confiance ;
- statut « confirmé », « à vérifier » ou « obsolète ».

Une information communautaire ne doit jamais devenir une vérité confirmée uniquement parce qu'elle a été trouvée plusieurs fois. Les contradictions doivent être signalées.

### 3. Boucle de veille sans actualisations inutiles

- Une actualisation légère au lancement de l'application, au maximum une fois par session.
- Enregistrer la date de dernière vérification et un délai minimal avant une nouvelle tentative.
- Comparer les empreintes/contenus avant de télécharger ou traiter à nouveau.
- Utiliser WorkManager pour une vérification différée seulement lorsqu'elle apporte une vraie valeur ; respecter les contraintes Android et les préférences de batterie.
- Aucun service ne doit capturer l'écran du jeu en permanence sans action et autorisation explicites du joueur.

### 4. Amélioration contrôlée

La veille peut proposer des changements de catalogue, portraits, compétences, bâtiments ou stratégies. Elle ne doit pas modifier automatiquement les données validées ni le code de l'application.

Pipeline recommandé :
1. collecter une source publique ;
2. extraire une proposition ;
3. comparer aux données existantes ;
4. attribuer une confiance et signaler les contradictions ;
5. ajouter des tests ;
6. produire un rapport ;
7. intégrer seulement après validation et tests réussis.

### 5. Véritable IA en continu : options futures

Une IA générative qui raisonne et développe en continu nécessite du calcul quelque part :
- modèle local sur un ordinateur personnel qui reste allumé (pas de frais d'API, mais consommation électrique et matériel nécessaires) ;
- modèle local sur un appareil Android compatible, avec limites de mémoire, vitesse et batterie ;
- service hébergé, qui peut être gratuit dans une petite enveloppe mais n'est jamais garanti gratuit ni disponible sans limite.

Aucune option ne doit être activée par défaut si elle peut entraîner une facturation. Aucun secret/API key ne doit être intégré dans l'APK ou le dépôt.

## Critères d'acceptation

- L'application démarre et reste utilisable hors ligne.
- Une ouverture ne déclenche pas une série de rafraîchissements.
- L'absence de serveur ne provoque ni écran bloqué ni erreur fatale.
- Les conseils distinguent faits confirmés, estimations et hypothèses.
- Toute donnée importée possède une provenance et une date.
- Les workflows ne déploient aucun service payant et ne nécessitent aucune facturation cloud.
- Les tests et la compilation Android sont vérifiés avant de déclarer un APK prêt.

## État actuel connu

Le projet possède déjà un traitement OCR local, un dépôt de données de jeu, un mode sans backend distant par défaut et des workflows GitHub Actions. Cette architecture est une cible de développement : elle ne signifie pas qu'une IA autonome 24 h/24 est déjà installée.
