# Cas de test des simulateurs de crédit

Les cas chiffrés A–C et les validations indiquées dans la section 5 ont été implémentés en JUnit. Les montants maximaux sont calculés avec la formule d'annuité en pleine précision puis arrondis à deux décimales avec `HALF_UP`. Pour vérifier les mensualités des cas A–C, les tests renseignent respectivement 10 000 DT, 12 000 DT et 15 000 DT comme montants demandés; les mensualités indépendamment calculées attendues sont 321,45 DT, 392,81 DT et 318,63 DT. Les seuils des garde-fous, les arrondis de frontière et certaines validations DTO restent à tester.

## 1. Cas chiffrés de référence

| Cas | Scénario et entrée | Résultats attendus |
|---|---|---|
| A | Sans rachat : salaire 2 500 DT; loyer 400 DT; SED 200 DT; crédits BNA 250 DT; Dépenses courantes; 36 mois; TMM 7,49 %; marge 2,25 % | Loyer retenu 200 DT; revenu net moyen 2 700 DT; endettement 55 %; garde-fou 65 %; plafond revenu 1 485 DT; plafond salaire 1 625 DT; plafond retenu 1 485 DT; retenues 450 DT; capacité 1 035 DT; taux annuel 9,74 %; montant maximal **32 197,60 DT** |
| B | Rachat interne : salaire 2 500 DT; loyer 400 DT; SED 200 DT; BNA non rachetées 150 DT; capital restant dû 12 000 DT; Dépenses courantes; 36 mois; TMM 7,49 %; marge 3,50 % | Revenu 2 700 DT; plafond 1 485 DT; retenues applicables 350 DT; capacité 1 135 DT; taux annuel 10,99 %; montant finançable **34 673,45 DT**, supérieur au capital |
| C | Rachat externe : salaire 2 200 DT; loyer 0 DT; SED non rachetées 250 DT; capital externe 15 000 DT; Aménagement; 60 mois; TMM 7,49 %; marge 2,50 % | Revenu 2 200 DT; endettement 55 %; garde-fou 65 %; plafond revenu 1 210 DT; plafond salaire 1 430 DT; plafond retenu 1 210 DT; capacité 960 DT; taux annuel 9,99 %; montant finançable **45 193,22 DT**, supérieur au capital |
| D | Rachat externe : salaire 1 200 DT; loyer 0 DT; revenu net moyen 1 200 DT | `NON_ELIGIBLE`, motif : revenu net moyen inférieur à 1 500 DT; aucun montant maximal positif |
| E | Sans rachat : salaire 1 000 DT; loyer 0 DT; revenus nets 1 000 DT; SED 600 DT; crédit BNA 0 DT; produit avec paramètres valides | Plafond revenu 500 DT; plafond salaire 600 DT; retenues 600 DT; capacité ≤ 0; `CAPACITE_INSUFFISANTE`; aucun maximum positif |

Pour les cas A à C, vérifier aussi l'identité `paiement(montantMaximal) ≤ capacité`, à la précision monétaire documentée. Pour B et C, vérifier que le montant maximal couvre le capital restant dû. Les données d'entrée ne doivent pas être pré-arrondies au-delà du centime.

## 2. Limites des tranches

Utiliser des tests paramétrés pour chaque seuil. Les bornes inclusives doivent être testées des deux côtés. La colonne « standard » s'applique aux scénarios sans rachat et rachat interne.

| Valeur | Taux standard revenu | Taux rachat externe |
|---:|---:|---:|
| 999,99 DT | 40 % | Non éligible |
| 1 000 DT | 50 % | Non éligible |
| 1 499,99 DT | 50 % | Non éligible |
| 1 500 DT | 50 % | 50 % |
| 1 999,99 DT | 50 % | 50 % |
| 2 000 DT | 55 % | 55 % |
| 2 999,99 DT | 55 % | 55 % |
| 3 000 DT | 60 % | 60 % |
| 3 999,99 DT | 60 % | 60 % |
| 4 000 DT | 65 % | 65 % |

Vérifier séparément les garde-fous standard selon le salaire : 999,99 → 50 %, 1 000 → 60 %, 1 999,99 → 60 %, 2 000 → 65 %, 2 999,99 → 65 %, 3 000 → 70 %, 3 999,99 → 70 %, 4 000 → 75 %.

Pour le rachat externe, vérifier les garde-fous 1 500–1 999,99 → 60 %, 2 000–2 999,99 → 65 %, 3 000–3 999,99 → 70 %, ≥ 4 000 → 75 %. **Le cas salaire < 1 500 DT avec revenu net ≥ 1 500 DT reste bloqué à la clarification métier** et ne doit pas recevoir un taux inventé.

## 3. Validations, limites et erreurs

| Test | Entrée / condition | Résultat attendu |
|---|---|---|
| Durée zéro | Durée = 0 | Rejet de validation ou `PARAMETRES_INVALIDES`; aucun résultat financier positif |
| Durée dépassée | Durée = maximum produit + 1 | Rejet métier structuré; aucun calcul de mensualité |
| Durée maximum | Durée exactement égale au maximum | Acceptée si les autres paramètres sont valides |
| Taux nul | TMM + marge = 0 %, capacité positive, durée `n` | Montant maximal = capacité × `n`; mensualité = capital / `n` |
| Demande supérieure au maximum | Montant demandé > maximum, même de 0,01 DT | Refus; ne pas accepter une tolérance qui viole la borne |
| Demande à la limite | Mensualité du montant demandé égale à la capacité, sans dépasser | Acceptée |
| Capital interne non positif | Capital = 0 ou négatif | Rejet; un capital de rachat doit être strictement positif |
| Capital externe non positif | Capital = 0 ou négatif | Rejet; un capital de rachat doit être strictement positif |
| Capital supérieur au montant finançable | Capital restant dû > maximum finançable | `CAPACITE_INSUFFISANTE`; aucun maximum positif présenté |
| Montant de rachat inférieur au capital | Montant demandé < capital restant dû | Rejet; aucun reliquat négatif |
| Montant demandé nul | `null` permet le calcul du maximum sans mensualité; zéro renseigné est refusé comme non positif | Ne jamais interpréter silencieusement zéro comme un nouveau crédit valide |
| Capacité nulle ou négative | Retenues ≥ plafond | `CAPACITE_INSUFFISANTE`, sans résultat positif |
| Salaire / loyer / retenue négatif | Valeur négative | Rejet de validation |
| Salaire nul | Salaire = 0 (si contrat final autorise zéro) | Pas de division par zéro; garde-fou calculé conformément à la tranche, résultat cohérent |
| Revenu nul | Salaire et loyer retenu = 0 | Pas de division par zéro; statut métier défini, aucun montant positif |
| Loyer brut | Loyer = 400 DT | Revenu locatif retenu = 200 DT exactement; vérifier l'absence de double application |
| Arrondi à mi-centime | Valeur produite finissant à 0,005 DT | Résultat affiché arrondi `HALF_UP` à deux décimales |
| Paramètre TMM absent | Pas de TMM configuré | Erreur de configuration explicite; aucun fallback financier silencieux |
| Marge produit absente | Produit/scénario non configuré | Erreur de configuration explicite |
| Produit interdit externe | Produit automobile/immobilier même s'il existe en base | Absent du catalogue et refusé par l'API |

## 4. Retenues et résultats après rachat

Pour les deux rachats, vérifier dans des cas distincts que les mensualités des crédits rachetés n'entrent pas dans les retenues applicables. Vérifier qu'elles ne sont pas déduites deux fois.

- Interne : retenues applicables = SED + BNA non rachetées; après rachat = ces retenues + mensualité du nouveau crédit.
- Externe : retenues applicables = SED non rachetées; après rachat = SED non rachetées + mensualité du nouveau crédit.
- Dans les deux cas : reliquat = montant du nouveau crédit − capital restant dû et ne peut pas être négatif.
- Taux après rachat = total des retenues après rachat / revenu net moyen; aucune division si revenu nul.

## 5. Couverture JUnit exécutée

`SimulationServiceTest` implémente les cas A–C, les taux d'endettement et garde-fous aux seuils indiqués, la non-éligibilité externe, la règle externe à clarifier, les cas de capacité insuffisante, les montants de rachat sous le capital et au-dessus du maximum, la durée invalide/dépassée, le montant demandé facultatif et les formules à taux nul. `SimulationControllerIntegrationTest` vérifie les trois endpoints, le catalogue et les produits autorisés, les DTO, les réponses HTTP et les messages d'erreur. La suite backend complète a exécuté 67 tests avec 0 échec et 0 erreur. Les arrondis de frontière et plusieurs validations DTO détaillées ne sont pas encore testés.
