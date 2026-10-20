# BNA Digital Banking

Projet de démonstration académique : un portail React commun (espaces client et administration selon le rôle) et un monolithe Spring Boot pour la simulation et les demandes de crédit fictives.

## Démarrage

1. Installer Java 25 (LTS), Maven 3.9+, Node.js 20+ et XAMPP.
2. Dans le panneau de contrôle XAMPP, démarrer **MySQL**. Apache n'est pas nécessaire pour le backend.
3. Dans phpMyAdmin (`http://localhost/phpmyadmin`) connecté comme administrateur MySQL, exécuter une fois le SQL suivant pour créer une base et un utilisateur dédiés. Remplacer le mot de passe d'exemple par un mot de passe local de votre choix :

   ```sql
   CREATE DATABASE IF NOT EXISTS bna_digital_banking
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'bna_app'@'localhost'
     IDENTIFIED BY 'REMPLACER_PAR_UN_MOT_DE_PASSE_LOCAL';
   GRANT ALL PRIVILEGES ON bna_digital_banking.* TO 'bna_app'@'localhost';
   ```

4. Depuis la racine du dépôt, ouvrir un terminal PowerShell, définir les paramètres de connexion et la clé JWT, puis démarrer l'API :

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/bna_digital_banking?serverTimezone=UTC'
   $env:DB_USERNAME = 'bna_app'
   $env:DB_PASSWORD = Read-Host 'Mot de passe MySQL de bna_app'
   $bytes = New-Object byte[] 32
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $rng.GetBytes($bytes)
   $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
   $rng.Dispose()
   cd backend
   mvn spring-boot:run
   ```

   Garder ce terminal ouvert. Les variables définies ainsi ne valent que pour ce terminal. Hibernate crée ou met à jour les tables de l'application au démarrage.

5. Dans un autre terminal, démarrer le portail BNA commun :

   ```powershell
   cd frontend-client
   npm install
   npm run dev
   ```

6. Le portail client et le portail admin sont accessibles sur `http://localhost:5173`. La connexion détecte le rôle du compte et affiche l’espace approprié. L’ancien point d’entrée admin reste disponible pour compatibilité et redirige vers le portail commun :

   ```powershell
   cd frontend-admin
   npm install
   npm run dev
   ```

   Ouvrir `http://localhost:5174` redirige vers `http://localhost:5173`. En usage normal, il suffit de démarrer et d’ouvrir le portail commun sur le port 5173.

L'utilisation de Docker est facultative. Si vous avez déjà MySQL avec XAMPP, ne lancez pas `docker compose up` : le serveur MySQL de XAMPP fournit la base locale.

API de santé : `http://localhost:8080/api/health`.
Les décisions de crédit ne sont jamais automatisées.

Pour le développement local, l'API fournit `POST /api/auth/register`, `POST /api/auth/login` et `GET /api/users/me`. Les mots de passe doivent contenir au moins 12 caractères. `JWT_SECRET` est obligatoire et doit contenir au moins 32 octets ; utilisez un gestionnaire de secrets en déploiement. Seuls les comptes clients peuvent être créés par inscription publique ; l'attribution du rôle administrateur doit être faite par un processus d'administration sécurisé.

Simulations disponibles pour un client connecté : `POST /api/simulations/sans-rachat`, `POST /api/simulations/rachat-interne` et `POST /api/simulations/rachat-externe`. Les paramètres initiaux du TMM (7,49 %) et les marges/types de crédit transmis sont créés automatiquement dans la base au premier démarrage, sans écraser les valeurs modifiées ensuite. Le catalogue des produits est consultable avec `GET /api/simulations/catalogue?scenario=SANS_RACHAT` (ou `RACHAT_INTERNE` / `RACHAT_EXTERNE`). Les paramètres sont consultables et administrables via `/api/admin/parameters` par un compte `ADMIN`.

Le portail commun (`http://localhost:5173`) utilise un seul écran de connexion/inscription. Après authentification, l’API fournit le rôle ; `CLIENT` ouvre l’espace client et `ADMIN` l’espace d’administration. L’inscription publique ne crée jamais de compte administrateur. Pour créer le premier administrateur en développement local : inscrivez d'abord un compte client, puis dans phpMyAdmin exécutez `UPDATE app_users SET role = 'ADMIN' WHERE email = 'adresse-du-compte@example.com';` en remplaçant l'adresse par celle du compte. Déconnectez-vous puis reconnectez-vous avec ce compte : le rôle sera relu depuis l’API. Réservez cette opération manuelle à la base locale de développement.

Dans l’espace client, le parcours **Demander un crédit** affiche les produits et paramètres reçus du backend, adapte les champs au scénario, calcule la simulation côté API, puis présente un récapitulatif avant confirmation. La demande enregistrée et son statut sont consultables dans **Mes demandes**. Le client peut répondre à une demande de complément ou retirer sa demande avant décision finale. Dans l’espace admin, l'onglet **Demandes** permet de prendre en charge une demande, consulter l'instantané de simulation, demander un complément puis approuver ou refuser avec un motif obligatoire. L’onglet **Paramètres du simulateur** donne accès au TMM et aux marges. Toutes ces actions sont vérifiées par l'API et journalisées ; aucune décision n'est prise automatiquement. Le complément est textuel dans cette version : le dépôt de justificatifs n'est pas encore disponible.

API de demandes : `POST /api/applications` crée un brouillon et calcule son instantané côté serveur ; `PUT /api/applications/{id}` met à jour un brouillon ; `POST /api/applications/{id}/submit` recalcule et soumet uniquement une demande valide avec montant positif. `GET /api/applications` et `GET /api/applications/{id}` sont limités aux demandes du client connecté. Les routes d'instruction sont sous `/api/admin/applications` et réservées au rôle `ADMIN`. Un client ne peut pas soumettre deux fois la même demande ; une demande inéligible ou dépassant la durée ou la capacité configurée est refusée par l'API.

À la soumission, le backend conserve un score Java fictif et ses facteurs. Les pondérations sont : endettement 30, revenu disponible 25, stabilité des revenus 20, historique de remboursement 15 et régularité de l'épargne 10. La stabilité est calculée à partir des mois consécutifs avec une opération de revenu `SALARY` enregistrée ; l'épargne à partir des mois avec un virement interne entrant sur le compte épargne au cours des 12 derniers mois. Si l'historique interne ne permet pas un calcul fiable, le facteur reste manquant et le score est provisoire. **Les retards de remboursement sont actuellement un jeu de démonstration synthétique généré de façon stable par client : ce n'est ni une détection de paiements réels ni un historique bancaire.** Les champs de risque ne sont plus modifiables ni envoyés par le formulaire de demande. Les niveaux affichés sont faible (75–100), modéré (50–74,99) et élevé (moins de 50). Le score est visible au client et à l'administration, n'est pas une règle BNA officielle et ne déclenche jamais une décision automatique. Le barème indicatif peut être ajusté par `SCORING_AVAILABLE_INCOME_FULL_SCORE_RATIO`, `SCORING_INCOME_STABILITY_FULL_SCORE_MONTHS`, `SCORING_LATE_PAYMENT_PENALTY_POINTS` et `SCORING_SAVINGS_FULL_SCORE_MONTHS`.

Dans une demande `EN_EXAMEN`, l’administrateur peut lancer ou relancer l’analyse IA et comparer séparément l’éligibilité et le risque proposés avec les résultats Java. L’intégration compatible OpenAI Chat Completions est **désactivée par défaut**. Pour l’activer, configurer dans l’environnement local du backend `LLM_ENABLED=true`, `LLM_BASE_URL` (par défaut `https://api.openai.com/v1`), `LLM_MODEL`, `LLM_API_KEY`, `LLM_CONNECT_TIMEOUT` (par défaut `3s`) et `LLM_READ_TIMEOUT` (par défaut `12s`). La clé doit rester dans l’environnement local ou un gestionnaire de secrets : ne pas la mettre dans le code, le dépôt ou le chat. Seuls les faits financiers et de simulation nécessaires sont envoyés au fournisseur ; aucun nom, email, référence, CIN, numéro de compte ou identifiant client n’est transmis. En cas d’indisponibilité ou de réponse JSON invalide, le backend conserve un statut explicite et le score Java reste indépendant. Ni l’IA ni la comparaison ne prennent la décision de crédit, qui reste humaine et motivée.

L'onglet **Mes comptes & budget** est une simulation locale persistée, sans connexion bancaire réelle. Au premier accès, deux comptes fictifs sont créés avec un solde courant de 2 000 DT et un solde épargne de 5 000 DT ; ces valeurs peuvent être ajustées par `DEMO_CURRENT_ACCOUNT_BALANCE` et `DEMO_SAVINGS_ACCOUNT_BALANCE`. Le client peut enregistrer des revenus et dépenses datés et catégorisés, ou transférer une somme entre ses propres comptes. Les soldes négatifs et les virements vers les comptes d'un autre client sont refusés. Les limites mensuelles du budget sont des alertes, non des blocages ; les virements internes ne sont comptés ni comme revenu ni comme dépense.

API comptes : `GET /api/accounts`, `GET /api/accounts/categories`, `GET /api/accounts/{id}/transactions`, `POST /api/accounts/{id}/transactions` et `POST /api/accounts/transfers`. API budget : `GET /api/budget?month=AAAA-MM` et `PUT /api/budget` avec `month`, `category` et `monthlyLimit`. Ces routes sont réservées au rôle `CLIENT` et chaque opération est limitée aux comptes du propriétaire.

Tests backend : `cd backend; mvn test`.

## Conception

Voir l'[architecture fonctionnelle](docs/ARCHITECTURE_FONCTIONNELLE.md) pour les acteurs, parcours, données, API et ordre de développement du MVP.
