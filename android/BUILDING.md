# Génération de l’APK

Le workflow GitHub Actions `.github/workflows/android-apk.yml` compile automatiquement l’application lors d’une modification du dossier `android/`.

## APK Release

L’APK Release est signé avec la clé stable si les quatre secrets GitHub suivants sont configurés :

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Cette clé doit être la même que celle utilisée pour les versions installées précédemment. Sans elle, un APK Release ne peut pas mettre à jour l’application existante.

## APK Debug

Si la signature stable n’est pas disponible, le workflow compile un APK Debug destiné aux essais. Il peut nécessiter la désinstallation de l’ancienne version avant installation si la signature diffère.

## Récupérer le fichier

Après une compilation réussie, ouvrir l’exécution du workflow « Compiler APK Android » dans GitHub Actions, puis télécharger l’artefact correspondant :

- `lands-of-jail-assistant-release-apk` pour la version Release signée ;
- `lands-of-jail-assistant-debug-test-only` pour la version Debug de test.

L’artefact est conservé pendant la durée indiquée dans le workflow.
