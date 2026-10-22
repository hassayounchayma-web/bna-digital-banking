# Règles métier des simulateurs de crédit

> Référentiel d'audit établi à partir de la spécification fournie le 21 octobre 2026. Les marges, durées et seuils ci-dessous sont des paramètres de simulation de travail, pas une affirmation sur les conditions bancaires actuellement en vigueur.

## 1. Données et règles communes

- Le client saisit un seul salaire net moyen mensuel. Il n'y a pas de saisie de trois salaires ni de moyenne calculée.
- Le client saisit le loyer mensuel brut perçu. La part retenue est `loyer mensuel × 0,50`, calculée une seule fois côté backend.
- `RevenuNetMoyen = SalaireNetMoyen + RevenusLocatifsRetenus`. Le salaire reste une donnée distincte, notamment pour le garde-fou.
- `PlafondSelonRevenu = RevenuNetMoyen × TauxEndettement`.
- `PlafondSelonSalaire = SalaireNetMoyen × GardeFou`.
- `PlafondEndettement = min(PlafondSelonRevenu, PlafondSelonSalaire)`.
- `CapaciteDisponible = PlafondEndettement - RetenuesApplicables`. Une capacité nulle ou négative entraîne `CAPACITE_INSUFFISANTE` et aucun montant maximal positif.
- `TauxAnnuel = TMM + MargeProduit`. Le TMM est configurable; 7,49 % est une valeur de référence pour les exemples, pas une constante métier. Le taux annuel en pourcentage est converti en décimal avant division par 12.
- La durée est un entier strictement positif, au plus égal à la durée maximale du produit.
- Pour un capital `C`, un taux mensuel décimal `r` et `n` mensualités :
  - mensualité : `C × r / (1 - (1 + r)^(-n))`;
  - montant maximal : `CapaciteDisponible × (1 - (1 + r)^(-n)) / r`;
  - si `r = 0`, respectivement `C / n` et `CapaciteDisponible × n`.
- Les calculs utilisent `BigDecimal`, sans arrondi intermédiaire monétaire excessif. L'affichage monétaire est arrondi à deux décimales, `HALF_UP`.
- Statuts métier de référence : `SIMULATION_VALIDE`, `CAPACITE_INSUFFISANTE`, `NON_ELIGIBLE`, `PARAMETRES_INVALIDES`, `REGLE_A_CLARIFIER`. Une erreur ne doit pas être mise dans un champ numérique.

## 2. Crédit sans rachat

### Taux d'endettement selon le revenu net moyen

| Revenu net moyen | Taux |
|---|---:|
| < 1 000 DT | 40 % |
| ≥ 1 000 et < 2 000 DT | 50 % |
| ≥ 2 000 et < 3 000 DT | 55 % |
| ≥ 3 000 et < 4 000 DT | 60 % |
| ≥ 4 000 DT | 65 % |

### Garde-fou selon le salaire net moyen

| Salaire net moyen | Garde-fou |
|---|---:|
| < 1 000 DT | 50 % |
| ≥ 1 000 et < 2 000 DT | 60 % |
| ≥ 2 000 et < 3 000 DT | 65 % |
| ≥ 3 000 et < 4 000 DT | 70 % |
| ≥ 4 000 DT | 75 % |

Retenues applicables : `RetenuesSED + RetenuesCreditsBNA`. Aucun capital restant dû n'est saisi. Le montant demandé est facultatif; s'il est fourni, il doit être strictement positif, ne pas dépasser le montant maximal et sa mensualité doit rester dans la capacité.

### Produits de référence

| Produit | Durée maximale | Marge |
|---|---:|---:|
| Dépenses courantes | 36 mois | 2,25 % |
| Aménagement | 60 mois | 2,50 % |
| Véhicule neuf | 84 mois | 3,00 % |
| Véhicule occasion | 60 mois | 3,00 % |
| Aménagement hypothécaire ≤ 7 ans | 84 mois | 3,00 % |
| Aménagement hypothécaire > 7 ans et ≤ 12 ans | 144 mois | 3,25 % |
| Acquisition logement ≤ 7 ans | 84 mois | 3,00 % |
| Acquisition logement > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |
| Construction ≤ 7 ans | 84 mois | 3,00 % |
| Construction > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |
| Achat terrain ≤ 7 ans | 84 mois | 3,00 % |
| Achat terrain > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |

## 3. Rachat interne BNA

Les taux d'endettement et les garde-fous sont ceux du scénario sans rachat. Le capital restant dû BNA racheté est obligatoire et strictement positif.

- Retenues applicables : `RetenuesSED + RetenuesCreditsBNA_NON_RACHETES`.
- Les échéances des prêts BNA rachetés ne sont pas retenues après le rachat.
- Le montant finançable est comparé au capital restant dû. S'il est inférieur, le résultat est `CAPACITE_INSUFFISANTE`.
- Si un montant de nouveau crédit est demandé, il doit satisfaire `CapitalRestantDu ≤ MontantDemande ≤ MontantMaximal`.
- `ReliquatApresRachat = MontantDemande - CapitalRestantDu`. C'est un capital, pas une capacité mensuelle.
- Après rachat, le taux d'endettement est calculé avec les retenues SED, les retenues BNA non rachetées et la mensualité du nouveau prêt, rapportées au revenu net moyen.

### Produits de référence

| Produit | Durée maximale | Marge |
|---|---:|---:|
| Dépenses courantes | 36 mois | 3,50 % |
| Aménagement | 60 mois | 3,75 % |
| Véhicule neuf | 84 mois | 3,00 % |
| Véhicule occasion | 60 mois | 3,00 % |
| Aménagement hypothécaire ≤ 7 ans | 84 mois | 3,00 % |
| Aménagement hypothécaire > 7 ans et ≤ 12 ans | 144 mois | 3,25 % |
| Acquisition logement ≤ 7 ans | 84 mois | 3,00 % |
| Acquisition logement > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |
| Construction ≤ 7 ans | 84 mois | 3,00 % |
| Construction > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |
| Achat terrain ≤ 7 ans | 84 mois | 3,00 % |
| Achat terrain > 7 ans et ≤ 15 ans | 180 mois | 3,25 % |

**À clarifier :** une référence au coefficient « 25 % » existe dans le simulateur de rachat interne, mais son assiette et son rôle ne sont pas définis. Aucun calcul ne doit l'appliquer avant clarification.

## 4. Rachat externe

- Si le revenu net moyen est inférieur à 1 500 DT : `NON_ELIGIBLE`.
- Taux d'endettement selon le revenu net moyen :

| Revenu net moyen | Taux |
|---|---:|
| < 1 500 DT | Non éligible |
| ≥ 1 500 et < 2 000 DT | 50 % |
| ≥ 2 000 et < 3 000 DT | 55 % |
| ≥ 3 000 et < 4 000 DT | 60 % |
| ≥ 4 000 DT | 65 % |

- Garde-fou selon le salaire net moyen :

| Salaire net moyen | Garde-fou |
|---|---:|
| ≥ 1 500 et < 2 000 DT | 60 % |
| ≥ 2 000 et < 3 000 DT | 65 % |
| ≥ 3 000 et < 4 000 DT | 70 % |
| ≥ 4 000 DT | 75 % |

- Si le revenu atteint 1 500 DT grâce aux loyers mais que le salaire est inférieur à 1 500 DT, le garde-fou n'est pas défini : retourner `REGLE_A_CLARIFIER` (ou une configuration incomplète selon la convention retenue), sans inventer de taux.
- Retenues applicables : uniquement les retenues SED non rachetées.
- Le capital restant dû externe est obligatoire et strictement positif. Les conditions de montant maximal, montant demandé minimal, reliquat et endettement après rachat sont celles du rachat interne.
- Seuls les produits Dépenses courantes (36 mois, marge 2,25 %) et Aménagement (60 mois, marge 2,50 %) sont autorisés.

## 5. Champs selon scénario

| Donnée | Sans rachat | Rachat interne | Rachat externe |
|---|---|---|---|
| Salaire net moyen unique | Oui | Oui | Oui |
| Loyer mensuel brut | Oui | Oui | Oui |
| Retenues SED | Oui | Oui | Oui |
| Retenues BNA | Oui, toutes | Oui, non rachetées | Non |
| Capital restant dû | Non | Oui, BNA | Oui, externe |
| Montant demandé | Facultatif | Montant du nouveau crédit | Montant du nouveau crédit |
| Reliquat après rachat | Non | Oui | Oui |
