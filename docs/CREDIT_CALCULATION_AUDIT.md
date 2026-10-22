# Audit des calculs de crédit

## État après corrections ciblées

La présente mise à jour complète les constats initiaux ci-dessous. Le loyer brut est maintenant envoyé au backend et le taux de 50 % y est appliqué. Les trois scénarios retournent `CAPACITE_INSUFFISANTE` pour une capacité non positive; le rachat externe refuse un revenu net inférieur à 1 500 DT et retourne `REGLE_A_CLARIFIER` lorsque le salaire est sous 1 500 DT malgré un revenu admissible. Les montants demandés des rachats sont validés par rapport au capital restant dû et au maximum finançable. Une demande absente reste permise et ne produit pas de mensualité fictive.

Les cas numériques A, B et C, les seuils de taux et garde-fou demandés, les erreurs métier, les produits autorisés et les trois endpoints sont désormais couverts par JUnit. La suite complète du backend a passé 67 tests, sans échec ni erreur, avec `JWT_SECRET` de test et `-Dnet.bytebuddy.experimental=true` pour la compatibilité Java 25 des mocks existants.

Les anomalies encore ouvertes et les précautions liées aux paramètres persistés sont résumées à la fin du présent document.

**Date :** 21 octobre 2026  
**Périmètre :** audit initial puis corrections ciblées et tests des trois simulateurs. Les modules de scoring Java et IA OpenAI n'ont pas été modifiés. Les données actuellement présentes dans MySQL n'ont pas été interrogées; les conclusions sur les tarifs persistés portent donc sur les valeurs d'initialisation du code.

## 1. Vue d'ensemble

| Scénario | État général | Écarts majeurs |
|---|---|---|
| Sans rachat | Partiellement conforme | Marges véhicule/immobilier initialisées inférieures à la référence; statut positif historique `ELIGIBLE` |
| Rachat interne | Partiellement conforme | Le contrôle du montant demandé par rapport au capital et au maximum est corrigé; coefficient « 25 % » non appliqué en attente de définition |
| Rachat externe | Partiellement conforme | Seuil de revenu minimum et capacité insuffisante corrigés; liste produit à verrouiller et règles tarifaires persistées à vérifier |

Les formules d'annuité sont déjà mutualisées dans `SimulationService` et utilisent `BigDecimal`. Le TMM et les marges sont configurables par les endpoints d'administration. Les anomalies principalement relevées sont les contrats de données, les statuts, les validations, les tarifs initiaux et l'intégration de la règle du loyer.

## 2. Détail des règles communes (constats de l'audit initial, actualisés ci-dessous)

| Règle | Attendu | Implémentation observée | État | Fichiers concernés | Correction proposée |
|---|---|---|---|---|---|
| Salaire net moyen | Une seule valeur saisie, utilisée telle quelle; zéro autorisé par la spécification | L'API et l'écran utilisent une seule valeur directe; le libellé est corrigé et DTO/formulaire acceptent zéro. | Conforme | DTO de simulation, `CreditSimulator.jsx` | Conserver |
| Loyer | Saisie brute et application unique du taux de 50 % côté backend | React transmet `loyerMensuel` brut; le backend applique une seule fois le coefficient 0,50. Le flux des demandes d'application conserve son champ historique `halfRentIncome` et le convertit avant le calcul. | Conforme dans les endpoints de simulation | DTO de simulation, `App.jsx`, `SimulationService.java`, `CreditApplicationService.java` | Conserver; la précision des anciens montants stockés comme demi-loyers reste une limite |
| Revenu distinct du salaire | Revenu = salaire + loyer retenu; salaire utilisé pour le garde-fou | Le service conserve ces deux valeurs et calcule le plafond par le minimum des deux plafonds. | Conforme | `SimulationService.java` | Conserver cette distinction |
| Endettement standard | Tranches 40/50/55/60/65 % par revenu | `standardDebtRate` reprend les seuils 1 000, 2 000, 3 000 et 4 000 DT inclusifs aux bornes supérieures. | Conforme | `SimulationService.java` | Ajouter des tests de frontière |
| Garde-fou standard | Tranches 50/60/65/70/75 % par salaire | `standardSafeguard` reprend les seuils 1 000, 2 000, 3 000 et 4 000 DT. | Conforme | `SimulationService.java` | Ajouter des tests de frontière |
| Plafond | Minimum du plafond revenu et du plafond salaire | `ceiling` calcule bien le minimum. | Conforme | `SimulationService.java` | Conserver |
| Capacité non positive | Statut insuffisant; aucun maximum positif | Les trois scénarios testent `available <= 0` et retournent `CAPACITE_INSUFFISANTE` avec montant maximal zéro et mensualité nulle. | Conforme | `SimulationService.java`, `SimulationResult.java` | Conserver |
| TMM configurable | Configuration backend; 7,49 % valeur de référence seulement | TMM stocké en pourcentage; la valeur 7,49 est insérée au démarrage seulement si la ligne est absente. Endpoints admin permettent lecture et mise à jour. | Conforme, sous réserve de la valeur persistée | `TmmEntity.java`, `CreditParameterService.java`, `SimulationParameterInitializer.java`, `CreditParameterController.java` | Vérifier la valeur en base lors du déploiement; ne pas présenter 7,49 % comme taux en vigueur |
| Conversion des taux | TMM et marge en pourcentage, addition puis conversion en taux mensuel décimal | Le provider convertit TMM et marge en valeurs décimales; le service les additionne puis divise le taux annuel décimal par 12. Les taux d'affichage sont reconvertis en pourcentage. | Conforme | `CreditParameterService.java`, `SimulationService.java` | Conserver; verrouiller par tests |
| Formule montant maximal et mensualité | Formules VA/VPM, paiements en fin de période; formule zéro | `presentValue` et `payment` utilisent les formules amortissables équivalentes, y compris le cas du taux mensuel nul. | Conforme | `SimulationService.java` | Conserver; ajouter les cas de test de référence et de taux zéro |
| Précision et arrondi | Pas d'arrondi intermédiaire excessif; politique explicite à deux décimales | `MathContext.DECIMAL128`; `money` applique `HALF_UP` à deux décimales sur les valeurs produites. | Conforme | `SimulationService.java` | Ajouter tests d'arrondi et documenter les montants de référence |
| Durée | Positive et ≤ maximum produit | Les DTO ont `@Min(1)` et le service refuse une durée supérieure au maximum configuré. | Conforme au niveau API | DTO de scénario, `SimulationService.java` | Ajouter tests durée 0 et durée maximale + 1 |
| Paramètres de produit | Produit du scénario, marge et durée maximum configurables | Catalogue lu depuis les marges du scénario; erreur 422 en l'absence de configuration. L'initialiseur n'insère des défauts que si la configuration manque et ne corrige pas une valeur déjà présente en base. | Partiellement conforme; valeur réelle en base inconnue | `CreditParameterService.java`, `SimulationParameterInitializer.java`, `CreditMarginEntity.java` | Confirmer et corriger les lignes existantes après validation; ne pas supposer que les valeurs d'initialisation reflètent la base |
| Statuts et erreurs | Statuts structurés définis dans la spécification | `NON_ELIGIBLE`, `CAPACITE_INSUFFISANTE`, `PARAMETRES_INVALIDES` et `REGLE_A_CLARIFIER` sont désormais disponibles; les erreurs sont séparées des valeurs numériques. `ELIGIBLE` est conservé au lieu de `SIMULATION_VALIDE` pour éviter une rupture immédiate du contrat existant. | Partiellement conforme au vocabulaire exact | `SimulationResult.java`, `SimulationService.java` | Une migration de statut favorable demanderait une décision de compatibilité API |
| Résultat sans montant demandé | Afficher le maximum et les résultats utiles; ne pas inventer une mensualité demandée | API et formulaire autorisent l'absence du montant; la mensualité reste nulle tant qu'aucun montant n'est demandé. | Conforme | DTO, `SimulationService.java`, `CreditSimulator.jsx` | Conserver |
| Validation montant demandé | Strictement positif quand présent; ≤ maximum; mensualité ≤ capacité | Les DTO acceptent null mais imposent le positif lorsqu'une valeur existe; le service vérifie positivité, bornes et capacité sans tolérance de 0,01 DT. | Conforme | DTO, `SimulationService.java` | Conserver |
| Champs et résultats affichés | Présenter salaire, loyer, revenu, plafonds, retenues, capacité, taux, durée, montants, statut et informations de rachat | Le formulaire expose un seul salaire, le loyer et les retenues adaptées au scénario. Le résultat affiche revenu, taux/garde-fou, capacité, taux annuel, demande, maximum, mensualité et taux après simulation. Plafond, retenues, TMM séparé, capital restant dû et reliquat ne sont pas affichés. | Partiellement conforme | `CreditSimulator.jsx`, `SimulationResult.java` | Compléter l'affichage à partir de champs renvoyés par l'API, sans recalcul React |

## 3. Détail par scénario (constats de l'audit initial)

### 3.1 Sans rachat

| Règle | Attendu | Implémentation observée | État | Fichiers concernés | Correction proposée |
|---|---|---|---|---|---|
| Retenues | SED + toutes les retenues BNA | Les deux valeurs sont soustraites de la capacité et utilisées dans les retenues après simulation. | Conforme | `SansRachatRequest.java`, `SimulationService.java` | Conserver |
| Capital refinancé | Aucun | Le DTO n'expose pas de capital restant dû et l'écran ne le demande pas. | Conforme | `SansRachatRequest.java`, `CreditSimulator.jsx` | Conserver |
| Montant demandé | Facultatif | API et formulaire acceptent l'absence; le résultat renvoie le maximum sans mensualité demandée. | Conforme | `SansRachatRequest.java`, `CreditSimulator.jsx` | Conserver |
| Capacité insuffisante | Statut dédié, maximum non positif | Capacité nulle ou négative entraîne `CAPACITE_INSUFFISANTE` et un maximum de zéro. | Conforme | `SimulationService.java` | Conserver |
| Dépenses courantes / aménagement | 36 mois / 2,25 %; 60 mois / 2,50 % | Valeurs d'initialisation identiques. | Conforme par défaut | `SimulationParameterInitializer.java` | Vérifier base, car les défauts n'écrasent pas l'existant |
| Véhicules | 84 mois neuf et 60 mois occasion; marge 3,00 % | Durées conformes, mais deux marges initialisées à 2,00 %. | Non conforme | `SimulationParameterInitializer.java` | Après validation, aligner à 3,00 % |
| Immobilier / terrain | Durées conformes; marge 3,00 % jusqu'à 7 ans et 3,25 % au-delà | Durées conformes; marges initialisées à 2,00 % et 2,25 %. | Non conforme | `SimulationParameterInitializer.java` | Après validation, aligner à 3,00 % et 3,25 % |

### 3.2 Rachat interne

| Règle | Attendu | Implémentation observée | État | Fichiers concernés | Correction proposée |
|---|---|---|---|---|---|
| Taux et garde-fou | Même chose que sans rachat | Le service appelle les règles standard. | Conforme | `SimulationService.java` | Conserver |
| Retenues | SED + BNA non rachetées seulement | La capacité et le taux après simulation utilisent les retenues SED et BNA non rachetées; les prêts rachetés ne sont pas déduits. | Conforme | `RachatInterneRequest.java`, `SimulationService.java` | Conserver |
| Capital restant dû | Obligatoire et strictement positif | DTO et service refusent une valeur nulle ou négative. | Conforme | `RachatInterneRequest.java`, `SimulationService.java` | Conserver |
| Montant maximal vs capital | Si le maximum est inférieur au capital, capacité insuffisante | Le service compare le montant finançable au capital; `SimulationResult.capacityInsufficient` renvoie un maximum zéro. | Conforme sur la décision | `SimulationService.java`, `SimulationResult.java` | Conserver; tester l'inégalité à l'eurocent |
| Montant de nouveau crédit | Facultatif au stade simulation; si présent, au moins égal au capital et au plus égal au maximum | Le montant absent reste autorisé; s'il est fourni, le service rejette sous le capital ou au-dessus du maximum avec un message explicite. | Conforme | `RachatInterneRequest.java`, `SimulationService.java`, `CreditSimulator.jsx` | Conserver |
| Reliquat | Nouveau crédit − capital racheté | Calculé uniquement après validation d'une demande; aucune valeur négative ne peut être produite. L'écran ne rend pas encore les champs capital/reliquat. | Partiellement conforme | `SimulationService.java`, `SimulationResult.java`, `CreditSimulator.jsx` | Afficher le capital et le reliquat dans une amélioration UI distincte |
| Marges et durées | Produits de référence complets | Les valeurs d'initialisation matchent la spécification pour les produits listés. | Conforme par défaut | `SimulationParameterInitializer.java` | Vérifier les valeurs persistées et tests de catalogue |
| « 25 % » | Ne pas appliquer sans définition | Aucune application arbitraire trouvée dans le service. | À clarifier | `SimulationService.java`; règle d'origine à confirmer | Obtenir l'assiette et la signification avant toute modification métier |

### 3.3 Rachat externe

| Règle | Attendu | Implémentation observée | État | Fichiers concernés | Correction proposée |
|---|---|---|---|---|---|
| Revenu minimum | Revenu net < 1 500 DT → `NON_ELIGIBLE` | Le service calcule le revenu depuis le salaire et le loyer brut et rejette sous le seuil sans montant maximal. | Conforme | `SimulationService.java`, `RachatExterneRequest.java` | Conserver |
| Taux d'endettement | Pas de taux sous 1 500; puis 50/55/60/65 % | Sous le seuil, le calcul du taux ne s'effectue pas; les seuils et valeurs à partir de 1 500 sont inchangés et testés. | Conforme | `SimulationService.java` | Conserver |
| Garde-fou | Salaire ≥ 1 500 : 60/65/70/75 % selon les seuils; salaire inférieur en cas de revenu locatif suffisant = règle à clarifier | Revenu sous 1 500 renvoie `NON_ELIGIBLE`; revenu admissible avec salaire inférieur à 1 500 renvoie `REGLE_A_CLARIFIER`, sans taux ni maximum. | Conforme à la clarification utilisateur | `SimulationService.java` | Ne pas inventer de taux |
| Retenues | SED non rachetées uniquement | Le service soustrait et réutilise uniquement `retenuesSed`. | Conforme | `RachatExterneRequest.java`, `SimulationService.java` | Conserver |
| Capital restant dû | Obligatoire et strictement positif | DTO et service refusent une valeur nulle ou négative. | Conforme | `RachatExterneRequest.java`, `SimulationService.java` | Conserver |
| Montant demandé / reliquat | Facultatif; si présent, montant ≥ capital et ≤ maximum; reliquat non négatif | Les deux bornes sont vérifiées et les erreurs sont explicites; pas de tolérance sur le maximum. | Conforme | `RachatExterneRequest.java`, `SimulationService.java`, `CreditSimulator.jsx` | Conserver |
| Produits autorisés | Deux produits uniquement | Les valeurs par défaut n'insèrent que Dépenses courantes et Aménagement. Cependant, le catalogue retourne toutes les lignes de la base pour le scénario; une configuration ancienne ou ajoutée peut exposer un produit supplémentaire. | Partiellement conforme | `CreditParameterService.java`, `SimulationParameterInitializer.java` | Appliquer une liste autorisée indépendante des lignes historiques, après validation |
| Marges et durées | Dépenses courantes 36 mois/2,25 %; Aménagement 60 mois/2,50 % | Valeurs par défaut conformes. | Conforme par défaut | `SimulationParameterInitializer.java` | Vérifier base et couverture de tests |

## 4. API et interface (constats de l'audit initial)

- Endpoints repérés : `POST /api/simulations/sans-rachat`, `POST /api/simulations/rachat-interne`, `POST /api/simulations/rachat-externe`, catalogue `GET /api/simulations/catalogue?scenario=...`.
- Le backend possède des DTO distincts par scénario et des validations Jakarta; les écarts portent sur certaines contraintes métier et sur la forme des statuts.
- Le formulaire React choisit les champs de retenues selon le scénario et n'affiche un capital à racheter que pour les deux rachats.
- Le libellé du salaire contredit la règle d'une valeur directe. Le montant demandé est toujours `required`, y compris sans rachat où il devrait être facultatif.
- La moitié du loyer et les conversions des valeurs du formulaire sont faites côté React. Les montants maximaux et mensualités, eux, sont calculés par Spring Boot. Pour respecter strictement la règle « calculs financiers exclusivement backend », le loyer brut devrait aussi être transmis au serveur sans pré-calcul.
- Le panneau résultat ne rend pas plusieurs données demandées (plafond retenu, retenues, TMM, capital et reliquat). Il affiche `ELIGIBLE` comme seul statut favorable et traite les autres statuts comme avertissement; le remplacement par les statuts demandés doit être coordonné avec l'interface.
- Les paramètres sont modifiables sous `/api/admin/parameters`; les valeurs initialisées ne permettent pas de conclure que la base existante contient les mêmes paramètres.

## 5. Tests existants et couverture constatée (état initial avant les tests ajoutés)

Le fichier `SimulationServiceTest.java` couvre actuellement un calcul sans rachat avec un cas différent des données de référence, une durée dépassée, un montant supérieur au maximum, un rachat interne insuffisant, un rachat externe sur une tranche standard et les formules à taux nul.

Il ne couvre pas les scénarios chiffrés A–E, le seuil d'éligibilité externe 1 500 DT, toutes les limites de tranches, salaire nul, validation du capital strictement positif, montant inférieur au capital, franchissement à 0,01 DT du maximum, ni l'arrondi de référence du montant maximal.

## 6. Corrections recommandées dans l'audit initial (historique)

1. Faire confirmer le cas du garde-fou externe avec salaire < 1 500 DT et revenu net ≥ 1 500 DT, ainsi que le coefficient « 25 % » du rachat interne.
2. Confirmer les marges souhaitées pour véhicule et immobilier/terrain du sans rachat; le document reprend la spécification fournie.
3. Aligner le contrat loyer brut, validation des montants, statut structuré et résultat sans montant demandé.
4. Ajouter le seuil d'éligibilité externe et verrouiller la liste de produits autorisés.
5. Aligner le formulaire et l'affichage des résultats avec les champs et règles par scénario.
6. Ajouter les tests chiffrés et les cas limites du fichier `CREDIT_TEST_CASES.md`.

## 7. État actuel et anomalies restantes

1. Les marges par défaut sont maintenant alignées sur la référence, mais les valeurs déjà persistées dans les bases existantes ne sont pas écrasées automatiquement et restent à vérifier.
2. L'écran ne présente pas encore le plafond, les retenues applicables, le TMM séparé, le capital racheté et le reliquat après rachat.
3. Le statut favorable reste `ELIGIBLE` (au lieu de `SIMULATION_VALIDE`) pour préserver le contrat existant.
4. Les données MySQL existantes n'ont pas été inspectées. Le coefficient « 25 % » du rachat interne reste sans application; le garde-fou externe non défini retourne `REGLE_A_CLARIFIER`.

## 8. Limites de cet audit

L'audit repose sur le code source et les valeurs d'initialisation. Les lignes actuellement enregistrées dans MySQL, les paramètres utilisés dans un environnement déployé et les réponses observées en exécution n'ont pas été vérifiés. Il s'agit de l'audit demandé, sans correction de code : les changements listés ci-dessus attendent la validation métier.
