# Fonctionnement Android local avec API Render conservée

Le projet privilégie une architecture Android locale et gratuite, avec le service Render `https://land-of-jail-assistant-api.onrender.com/` comme hébergement du site web Express et comme API distante officielle utilisée par Android. L'ancien site Vercel `https://land-of-jail-assistante.vercel.app/` n'est pas l'adresse de référence et ne doit plus être présenté comme l'assistant actif. Vercel et Bolt ne font pas partie du circuit de déploiement.

- Les données déjà téléchargées restent consultables hors ligne grâce au cache local.
- La vérification des mises à jour doit être déclenchée à l'ouverture de l'application, une seule fois par session.
- Les téléchargements doivent être évités lorsque le contenu n'a pas changé.
- Aucun déploiement Google Cloud Run n'est requis.
- Aucune clé Gemini ou facturation Google Cloud ne doit être nécessaire au fonctionnement de base.

## Limites à respecter

Les sources publiques peuvent changer, limiter les accès ou ne pas fournir de flux structuré. L'application doit afficher la date de dernière vérification et distinguer les données confirmées des informations incertaines. En l'absence de réseau, elle conserve les dernières données valides.

La veille ne doit pas effectuer de surveillance permanente ni lancer des rafraîchissements répétés en arrière-plan. Une tâche ponctuelle peut terminer un rafraîchissement déjà lancé, puis s'arrêter.

## Hébergement Render

- Conserver le serveur Express et ses routes `/api/*`.
- Configurer Android sur `https://land-of-jail-assistant-api.onrender.com` pour les fonctions distantes facultatives (conseils, veille, analyse distante si activée).
- Conserver le service Render existant ; ne pas le remplacer par Vercel.
- Le site web servi par Express en production doit continuer à être compilé dans `dist`.
- Une indisponibilité de Render ne doit pas bloquer les fonctions Android locales.

## Développement

Les fonctionnalités essentielles (catalogue, reconnaissance OCR, conseils et gestion locale) doivent rester utilisables sans backend distant. Toute intégration externe facultative doit avoir une solution de repli locale et ne jamais bloquer l'application.
