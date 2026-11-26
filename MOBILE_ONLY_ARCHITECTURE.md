# Architecture cible : application mobile uniquement

## Décision produit

Land of Jail Assistant est un produit mobile. L'expérience destinée aux joueurs doit être fournie par l'application installée sur leur téléphone, et non par le site web React/Vite.

## Android en priorité

- Application Android native en Kotlin et Jetpack Compose.
- Les écrans, la navigation, l'import des captures, l'OCR, les profils, les héros, les recommandations et les réglages sont utilisés dans l'application.
- Aucun WebView pour afficher le site comme interface principale.
- Les liens externes éventuels servent uniquement aux sources et références, jamais à remplacer les écrans de l'application.
- Les APK sont compilés et vérifiés par GitHub Actions lorsque le code Android change. Pas de compilation quotidienne inutile.

## Hébergement conservé : Render

Le service `https://land-of-jail-assistant-api.onrender.com/` est l'hébergement à conserver pour le site et les services API du projet. Render remplace le rôle d'hébergement précédemment envisagé sur Vercel ; aucun déploiement Vercel ou flux Bolt n'est requis.

Le serveur Express expose notamment `/api/health`, les routes d'analyse de captures et `/api/game-intel`. En production, il sert également le frontend compilé depuis `dist`. Toute évolution doit préserver ces routes et le fonctionnement du site sur Render.

Les clés privées et secrets restent côté serveur et ne sont jamais intégrés dans l'APK. Android utilise les API Render uniquement pour les fonctions qui en ont besoin ; les fonctions locales doivent rester utilisables sans réseau.

## iOS ensuite

iOS ne commence qu'après validation de la version Android sur les appareils réels et stabilisation des fonctions principales. L'API et les formats de données doivent rester indépendants de la plateforme afin que l'application iOS puisse utiliser le même catalogue, les mêmes règles de recommandation et les mêmes services IA.

L'application iOS aura une interface native adaptée à iPhone ; elle ne sera pas une simple page web emballée dans une application.

## Critères avant de déclarer Android prêt

- Installation et mise à jour APK sans perte des données du joueur.
- Import et analyse des captures, avec contrôle de confiance OCR/IA.
- Catalogue de héros, compétences, robots et événements cohérent et versionné.
- Recommandations adaptées aux données réelles du compte, sans inventer niveaux, étoiles ou statistiques.
- Gestion claire des erreurs réseau et de l'indisponibilité du service IA.
- Tests sur appareil Android réel et vérification des principales fonctions.
