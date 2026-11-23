# Déploiement du backend Land of Jail Assistant

Le backend est préparé pour un déploiement sur le service Google Cloud Run déjà utilisé par l'application Android. Le workflow ne crée pas de nouveau service : il met à jour uniquement le service dont le nom est fourni.

## Configuration GitHub requise

Dans les paramètres du dépôt, branche `audit/ameliorations-android` :

**Variables du dépôt**
- `GCP_PROJECT_ID` : identifiant du projet Google Cloud.
- `GCP_REGION` : région Cloud Run (l'ancienne URL indique `europe-west2`, à confirmer).
- `CLOUD_RUN_SERVICE` : nom exact du service Cloud Run existant.

**Secrets du dépôt**
- `GCP_WORKLOAD_IDENTITY_PROVIDER` : fournisseur Workload Identity Federation Google Cloud.
- `GCP_SERVICE_ACCOUNT` : adresse du compte de service autorisé à déployer.

Le compte de service doit disposer des droits nécessaires pour déployer sur le service existant et construire l'image (Cloud Run Admin/Developer selon l'organisation, Service Account User et accès au service Cloud Build/Artifact Registry).

## Lancer le déploiement

Une fois la configuration confirmée, lancer manuellement le workflow **Déployer le backend sur Cloud Run** depuis GitHub Actions. Il vérifie ensuite `/api/health` et `/api/game-intel`.

La clé `GEMINI_API_KEY` doit déjà être configurée comme variable d'environnement/secret du service Cloud Run. Le workflow ne l'affiche pas, ne la remplace pas et ne crée pas de clé.

## Coûts et sécurité

Ce workflow n'active aucune nouvelle facturation, ne crée pas de nouveau service et ne configure pas de minimum d'instances. Il utilise le service existant. Cloud Run, les builds et les appels Gemini peuvent néanmoins être facturés selon le compte, le quota et l'usage : vérifier le budget Google Cloud avant le lancement. L'authentification utilise Workload Identity Federation, sans clé JSON de compte de service stockée dans GitHub.
