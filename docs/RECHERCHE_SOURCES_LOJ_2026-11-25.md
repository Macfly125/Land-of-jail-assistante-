# Recherche de référence — Land of Jail Assistant

Dernière consultation : 25 novembre 2026

Ce document sert de registre de provenance pour les calculateurs, le coach de progression et les codes cadeaux. Les sites cités sont des références communautaires indépendantes, sauf mention contraire. Une donnée issue d'un site ne doit jamais être présentée comme un test effectué par l'application ou comme une annonce officielle vérifiée directement.

## Codes cadeaux

| Source | Ce qu'elle apporte | Limite de confiance |
|---|---|---|
| [GuideLOJ — codes](https://guideloj.com/fr/codes) | Page consultée le 25/11/2026 : 8 codes dans la section « Active codes », avec récompenses publiées | Site indépendant non officiel ; le libellé « actif » est celui de GuideLOJ et ne constitue pas un test de redemption réalisé par l'application. GuideLOJ avertit lui-même que des codes peuvent expirer, être limités ou dépendre de la région |
| [LOJ XLab — promo codes](https://lojxlab.com/promo-codes/) | Rapporte 8 codes provenant du canal Discord officiel #gift-codes, testés en jeu le 13/10/2026, avec une vérification annoncée au 01/11/2026 | Agrégateur communautaire ; la date du test n'est pas celle de l'ouverture de l'application |
| [Dookee — codes](https://loj.dookee.ru/en/codes/) | Compare les listes et distingue codes signalés, contestés et expirés | Comparaison communautaire datée du 01/11/2026, pas un test en direct |
| [Pocket Gamer — codes](https://www.pocketgamer.com/lands-of-jail/codes/) | Détecte des codes récents et des dates d'expiration publiées | Source éditoriale tierce ; les codes récents restent à confirmer dans le jeu |
| [MrGuider — codes](https://www.mrguider.org/codes/lands-of-jail-codes/) | Source secondaire de recoupement | Ne suffit jamais seule à déclarer un code valide |

Règles d'affichage :
- COMMUNITY_LISTED signifie seulement « trouvé dans une source communautaire », jamais « testé par l'application ».
- UNVERIFIED signifie que la disponibilité n'est pas confirmée.
- NEEDS_REVIEW signifie qu'il existe une contradiction ou une information incomplète.
- EXPIRED ne doit être appliqué que si une date d'expiration publiée est passée ou si une source fiable l'indique explicitement.
- « Déjà utilisé » est un résultat de redemption distinct d'un code expiré.
- Ne jamais inventer une récompense, une date de publication ou une date d'expiration. Une récompense absente de la source doit rester vide.
- Conserver la casse du code.
- Le lien d'invitation Discord ne donne pas accès aux messages. Les messages Discord ne sont pas lisibles via l'API publique sans bot/token autorisé. Ne pas prétendre interroger directement le serveur si l'application ne dispose pas d'un flux public ou d'une intégration autorisée. LOJ XLab peut être utilisé comme relais communautaire en indiquant sa provenance.

## Bâtiments, recherches et prérequis

- [LOJ Wiki — laboratoire](https://lojwiki.com/lab/) : le laboratoire principal requiert le Bureau du directeur niveau 9, plafond niveau 30 ; trois branches Logistique, Économie et Militaire. La page consultée indique 176 technologies et précise que les coûts/temps sont des valeurs de base avant bonus de vitesse. La recherche Forticlad est un bâtiment distinct.
- [LOJ Wiki — bâtiment laboratoire](https://lojwiki.com/buildings/lab/) : détaille les niveaux, coûts, prérequis et temps. Exemple : Laboratoire niveau 2 = 300 bois, 9 secondes, Bureau niveau 9 ; niveau 6 = 35K bois, 7K acier, 1 750 essence et 9 minutes. Ces exemples ne remplacent pas une table complète versionnée.
- [LOJ Wiki — Forticlad](https://lojwiki.com/calculator/forticlad/) : distingue les huit bâtiments, niveaux et paliers Forticlad ; chaque niveau comporte cinq étapes. Les niveaux supérieurs dépendent de la saison du serveur.
- [GuideLOJ — calculateur Forticlad](https://guideloj.com/en/calculators/forticlad) : récapitulatif par bâtiment et niveau ; certaines parties sont réservées à l'offre Pro. Ne pas dépendre d'un accès payant.

## Équipements de héros

- [LOJ Wiki — calculateur équipement](https://lojwiki.com/calculator/gear/) : sépare l'EXP d'équipement, les aimants, les bobines potentielles et les engrenages de précision. Le niveau 40 débloque la réforme ; la promotion de qualité et la réforme sont des pistes de coût distinctes. Les promotions au-delà de Légendaire exigent aussi des niveaux de réforme minimums : Légendaire 10, Légendaire T1 11, Légendaire T2 12, Exotique 13, Exotique T1 14, Exotique T2 15.
- [LOJ Wiki — équipement](https://lojwiki.com/gear/) : Arsenal requis et Bureau du directeur niveau 15.
- [Dookee — matériaux équipement](https://loj.dookee.ru/en/knowledge/materials/) : intervalle 100 → 120 pour une pièce = 117 000 pièces/bolts, 600 aimants et 20 bobines potentielles. Ne pas interpoler un coût partiel si la table ne le donne pas ; réforme et équipement exclusif restent séparés.
- Les coûts doivent être liés à la qualité, au niveau, au niveau de réforme et à la saison si applicable. Ne jamais confondre équipement standard, équipement exclusif et équipement d'armure.

## Armure de prisonnier et robots

- [LOJ Wiki — armure de prisonnier](https://lojwiki.com/calculator/prisoner-armor/) : suit les données d'armure, modules de puissance et modules avancés ; les plafonds peuvent être 60, 90 ou 100 selon l'armure. Les percées par dizaines coûtent des modules.
- [LOJ Wiki — hangar d'armure](https://lojwiki.com/power-armor/) : le Hangar d'armure de prisonnier requiert le Bureau du directeur niveau 25 ; disponibilité de chaque robot dépend aussi de la saison et du niveau du Bureau (25–30). Ne pas confondre ce déblocage avec l'Arsenal des équipements de héros (Bureau niveau 15).
- Les noyaux robots et leurs puces Solo, Formation et Barrière sont distincts de l'armure de prisonnier. Ne pas fusionner les deux calculateurs.
- [LOJ XLab — bibliothèque](https://lojxlab.com/library/) : répertorie des tableaux communautaires contrôlés, notamment niveaux robots 1–100 et noyaux I–V (116 460 puces annoncées comme total vérifié par la communauté). Il faut conserver le lien exact vers la table d'origine et ne pas extrapoler les lignes absentes.
- [LOJ XLab — coûts robots niveaux 1–100](https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/) : total annoncé 141 050 données d'armure, 1 040 modules de puissance et 325 modules avancés pour le niveau 1→100. Le tableau détaille chaque tranche de 10 niveaux. Une divergence reste explicitement signalée pour les modules avancés du niveau 60→70 : 40 selon deux tableaux/sources, 140 selon un calculateur ; conserver cette cellule en NEEDS_CONFIRMATION tant qu'une capture du jeu ne la tranche pas.
- [LOJ XLab — HEROpedia](https://lojxlab.com/heropedia/) : fiches héros, compétences, équipements exclusifs et saisons 1 à 7 ; les données sont présentées comme observées sur captures du jeu par les contributeurs. Le catalogue d'images générées pour illustrer les rôles n'est pas de l'art officiel.
- [Dookee — Hangar d'armure](https://loj.dookee.ru/en/development/hangar/) : catalogue consulté le 25/11/2026 de 14 robots, avec les noms Light Cone, Hercules, Pluto, Ranger, Infercore, Atlax, Phantom Cat, Halo, Yokozuna, Musashimaru, Bastion, Dauntless, Valiant et Scorpio. La page précise que certaines statistiques sont non vérifiées ou communautaires ; les portraits doivent être extraits comme gabarits contrôlés avant toute reconnaissance automatique, et les alias non confirmés doivent rester distincts.

## Sources transversales

- [LOJ Wiki — à propos](https://lojwiki.com/about/) : wiki indépendant qui déclare extraire les valeurs des tables et textes du client de jeu ; gratuit et sans compte. Certaines valeurs restent dépendantes de la saison serveur.
- [GuideLOJ — calculateurs](https://guideloj.com/en/calculators) : indique explicitement que les calculateurs sont des références/estimations, non officielles, et qu'il faut vérifier les valeurs en jeu avant dépense.
- [LOJ XLab — planificateur](https://lojxlab.com/upgrades/) : références de ratios de ressources et guides d'événements ; vérifier les conditions et les jours de saison/événement avant toute recommandation.

## Règle d'intégration

Chaque enregistrement de coût doit porter : identifiant de ressource, quantité, URL précise, date de consultation, saison/serveur si nécessaire, niveau de confiance (VERIFIED_IN_GAME, COMMUNITY_REFERENCE, NEEDS_CONFIRMATION) et version du jeu si connue. Les données NEEDS_CONFIRMATION ne doivent pas entrer dans un total présenté comme requis. Les stocks lus sur capture ne remplacent un stock confirmé qu'après validation explicite du joueur.