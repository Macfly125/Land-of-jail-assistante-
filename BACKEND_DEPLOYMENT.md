# Fonctionnement sans service cloud payant

Le projet privilégie une architecture Android locale et gratuite.

- Les données déjà téléchargées restent consultables hors ligne grâce au cache local.
- La vérification des mises à jour doit être déclenchée à l'ouverture de l'application, une seule fois par session.
- Les téléchargements doivent être évités lorsque le contenu n'a pas changé.
- Aucun déploiement Google Cloud Run n'est requis.
- Aucune clé Gemini ou facturation Google Cloud ne doit être nécessaire au fonctionnement de base.

## Limites à respecter

Les sources publiques peuvent changer, limiter les accès ou ne pas fournir de flux structuré. L'application doit afficher la date de dernière vérification et distinguer les données confirmées des informations incertaines. En l'absence de réseau, elle conserve les dernières données valides.

La veille ne doit pas effectuer de surveillance permanente ni lancer des rafraîchissements répétés en arrière-plan. Une tâche ponctuelle peut terminer un rafraîchissement déjà lancé, puis s'arrêter.

## Développement

Les fonctionnalités essentielles (catalogue, reconnaissance OCR, conseils et gestion locale) doivent rester utilisables sans backend distant. Toute intégration externe facultative doit avoir une solution de repli locale et ne jamais bloquer l'application.
