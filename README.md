# BNA Digital Banking

Projet de démonstration académique : un portail React commun (espaces client et administration selon le rôle) et un monolithe Spring Boot pour la simulation et les demandes de crédit fictives.

## Démarrage

1. Installer Java 25 (LTS), Maven 3.9+, Node.js 20+ et XAMPP. Le lanceur BNA peut aussi réutiliser une distribution Maven déjà présente dans le cache Maven local.
2. Dans le panneau de contrôle XAMPP, démarrer **MySQL**. Apache n'est pas nécessaire pour le backend.
3. Dans phpMyAdmin (`http://localhost/phpmyadmin`) connecté comme administrateur MySQL, exécuter une fois le SQL suivant pour créer une base et un utilisateur dédiés. Remplacer le mot de passe d'exemple par un mot de passe local de votre choix :

   ```sql
   CREATE DATABASE IF NOT EXISTS bna_digital_banking
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'bna_app'@'localhost'
     IDENTIFIED BY 'REMPLACER_PAR_UN_MOT_DE_PASSE_LOCAL';
   GRANT ALL PRIVILEGES ON bna_digital_banking.* TO 'bna_app'@'localhost';
   ```

4. Depuis la racine du dépôt, démarrer l'API. Le script utilise Maven depuis le PATH ou le cache Maven local, génère une clé JWT temporaire si nécessaire et demande le mot de passe MySQL sans l'afficher :

   ```powershell
   npm run dev:backend
   ```

   Garder ce terminal ouvert. Vous pouvez définir `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `JWT_SECRET` avant la commande pour éviter les valeurs par défaut ou l'invite. Hibernate crée ou met à jour les tables de l'application au démarrage.

5. Dans un autre terminal, démarrer le portail BNA commun depuis la racine :

   ```powershell
   npm run dev:client
   ```

   Pour lancer les deux services en parallèle dans VS Code, ouvrir la palette de commandes, choisir **Tasks: Run Task**, puis **BNA: Tout démarrer**. Le backend reste accessible sur `http://localhost:8080` et le portail commun sur `http://localhost:5173`.

6. Le portail client et l’espace admin sont accessibles sur `http://localhost:5173`. La connexion détecte le rôle du compte et affiche l’espace approprié.

L'utilisation de Docker est facultative. Si vous avez déjà MySQL avec XAMPP, ne lancez pas `docker compose up` : le serveur MySQL de XAMPP fournit la base locale.

API de santé : `http://localhost:8080/api/health`.
Les décisions de crédit ne sont jamais automatisées.

Commandes de validation depuis la racine : `npm run build` compile le backend et le portail, `npm run test:backend` exécute les tests JUnit (avec le réglage Byte Buddy nécessaire à Java 25) et `npm run lint:client` lance le linter.

Pour le développement local, l'API fournit `POST /api/auth/register`, `POST /api/auth/login` et `GET /api/users/me`. Les mots de passe doivent contenir au moins 12 caractères. `JWT_SECRET` est obligatoire et doit contenir au moins 32 octets ; utilisez un gestionnaire de secrets en déploiement. Seuls les comptes clients peuvent être créés par inscription publique ; l'attribution du rôle administrateur doit être faite par un processus d'administration sécurisé.

Simulations disponibles pour un client connecté : `POST /api/simulations/sans-rachat`, `POST /api/simulations/rachat-interne` et `POST /api/simulations/rachat-externe`. Les paramètres initiaux du TMM (7,49 %) et les marges/types de crédit transmis sont créés automatiquement dans la base au premier démarrage, sans écraser les valeurs modifiées ensuite. Le catalogue des produits est consultable avec `GET /api/simulations/catalogue?scenario=SANS_RACHAT` (ou `RACHAT_INTERNE` / `RACHAT_EXTERNE`). Les paramètres sont consultables et administrables via `/api/admin/parameters` par un compte `ADMIN`.

Le portail commun (`http://localhost:5173`) utilise un seul écran de connexion/inscription. Après authentification, l’API fournit le rôle ; `CLIENT` ouvre l’espace client et `ADMIN` l’espace d’administration. L’inscription publique ne crée jamais de compte administrateur. Pour créer le premier administrateur en développement local : inscrivez d'abord un compte client, puis dans phpMyAdmin exécutez `UPDATE app_users SET role = 'ADMIN' WHERE email = 'adresse-du-compte@example.com';` en remplaçant l'adresse par celle du compte. Déconnectez-vous puis reconnectez-vous avec ce compte : le rôle sera relu depuis l’API. Réservez cette opération manuelle à la base locale de développement.

Dans l’espace client, le parcours **Demander un crédit** affiche les produits et paramètres reçus du backend, adapte les champs au scénario, calcule la simulation côté API, puis présente un récapitulatif avant confirmation. La demande enregistrée et son statut sont consultables dans **Mes demandes**. Le client peut répondre à une demande de complément ou retirer sa demande avant décision finale. Dans l’espace admin, l'onglet **Demandes** permet de prendre en charge une demande, consulter l'instantané de simulation, demander un complément puis approuver ou refuser avec un motif obligatoire. L’onglet **Paramètres du simulateur** donne accès au TMM et aux marges. Toutes ces actions sont vérifiées par l'API et journalisées ; aucune décision n'est prise automatiquement. Le complément est textuel dans cette version : le dépôt de justificatifs n'est pas encore disponible.

API de demandes : `POST /api/applications` crée un brouillon et calcule son instantané côté serveur ; `PUT /api/applications/{id}` met à jour un brouillon ; `POST /api/applications/{id}/submit` recalcule et soumet uniquement une demande valide avec montant positif. `GET /api/applications` et `GET /api/applications/{id}` sont limités aux demandes du client connecté. Les routes d'instruction sont sous `/api/admin/applications` et réservées au rôle `ADMIN`. Un client ne peut pas soumettre deux fois la même demande ; une demande inéligible ou dépassant la durée ou la capacité configurée est refusée par l'API.

Notifications client : le backend enregistre une notification interne pour la création d'un brouillon et chaque transition réelle de statut, dans la même transaction que l'événement de demande. Les modifications de brouillon sans changement de statut et les consultations n'en génèrent pas. `GET /api/client/notifications`, `GET /api/client/notifications/unread-count`, `PATCH /api/client/notifications/{id}/read` et `PATCH /api/client/notifications/read-all` sont réservés au rôle `CLIENT` et utilisent l'identité portée par le JWT ; les notifications ne contiennent pas de montant ni de motif privé de décision. Le portail client actualise son centre de notifications toutes les 30 secondes pendant la session.

À la soumission, le backend conserve un score Java fictif dont les poids totalisent 100 : endettement (25), revenu disponible (20), stabilité des revenus (34) et régularité de l'épargne (21). Au démarrage, les évaluations déjà enregistrées sont recalculées avec ces poids et sans l'ancien critère synthétique d'historique de remboursement ; aucun retard fictif n'est généré ni utilisé. La stabilité est calculée à partir des mois consécutifs avec une opération de revenu `SALARY` enregistrée ; l'épargne à partir des mois avec un virement interne entrant sur le compte épargne au cours des 12 derniers mois. Si l'historique interne ne permet pas un calcul fiable, le facteur reste manquant et le score est provisoire ; le total est alors normalisé sur les poids effectivement disponibles. Les champs de risque ne sont pas modifiables ni envoyés par le formulaire de demande. Les niveaux affichés sont faible (75–100), modéré (50–74,99) et élevé (moins de 50). Le score est visible au client et à l'administration, n'est pas une règle BNA officielle et ne déclenche jamais une décision automatique. Le barème indicatif peut être ajusté par `SCORING_AVAILABLE_INCOME_FULL_SCORE_RATIO`, `SCORING_INCOME_STABILITY_FULL_SCORE_MONTHS` et `SCORING_SAVINGS_FULL_SCORE_MONTHS`.

Dans une demande `EN_EXAMEN`, l’administrateur peut lancer ou relancer l’analyse IA et comparer séparément l’éligibilité et le risque proposés avec les résultats Java. L’intégration utilise directement l’API Gemini `generateContent` et est **désactivée par défaut**. Pour l’activer, configurer dans l’environnement local du backend `LLM_ENABLED=true`, `LLM_BASE_URL` (par défaut `https://generativelanguage.googleapis.com/v1beta`), `LLM_MODEL` (par exemple `gemini-3.5-flash-lite`), `LLM_API_KEY`, `LLM_CONNECT_TIMEOUT` (par défaut `3s`) et `LLM_READ_TIMEOUT` (par défaut `12s`). La clé doit rester dans l’environnement local ou un gestionnaire de secrets : ne pas la mettre dans le code, le dépôt ou le chat. Seuls les faits financiers et de simulation nécessaires sont envoyés au fournisseur ; aucun nom, email, référence, CIN, numéro de compte ou identifiant client n’est transmis. En cas d’indisponibilité ou de réponse JSON invalide, le backend conserve un statut explicite et le score Java reste indépendant. Ni l’IA ni la comparaison ne prennent la décision de crédit, qui reste humaine et motivée.

Pour conserver une configuration locale entre les terminaux et les redémarrages, copier `.env.example` en `.env` à la racine du dépôt, puis y renseigner `DB_PASSWORD`. Pour activer l’analyse Gemini, renseigner `LLM_API_KEY` avec une clé Gemini et `LLM_MODEL` avec un modèle disponible (par exemple `gemini-3.5-flash-lite`), puis passer `LLM_ENABLED` à `true`. Laisser `LLM_BASE_URL` sur l’URL Gemini native par défaut. Le script `npm run dev:backend` charge ce fichier dans le processus local ; les variables déjà définies dans le terminal restent prioritaires. `.env` est ignoré par Git : ne jamais y mettre une clé réelle dans `.env.example`, dans le code ou dans un commit. Après modification, redémarrer le backend. L’IA actuelle propose un niveau qualitatif (`LOW`, `MODERATE`, `HIGH` ou `UNKNOWN`), une éligibilité proposée et des explications ; elle ne calcule pas de note numérique officielle sur 100 et ne décide pas de l’octroi. Définir les règles officielles du scoring IA et des cas d’essai validés avant d’ajouter un calcul numérique ou décisionnel.

L'onglet **Mes comptes & budget** est une simulation locale persistée, sans connexion bancaire réelle. Au premier accès, deux comptes fictifs sont créés avec un solde courant de 2 000 DT et un solde épargne de 5 000 DT ; ces valeurs peuvent être ajustées par `DEMO_CURRENT_ACCOUNT_BALANCE` et `DEMO_SAVINGS_ACCOUNT_BALANCE`. Le client peut enregistrer des revenus et dépenses datés et catégorisés, ou transférer une somme entre ses propres comptes. Les soldes négatifs et les virements vers les comptes d'un autre client sont refusés. Les limites mensuelles du budget sont des alertes, non des blocages ; les virements internes ne sont comptés ni comme revenu ni comme dépense.

API comptes : `GET /api/accounts`, `GET /api/accounts/categories`, `GET /api/accounts/{id}/transactions`, `POST /api/accounts/{id}/transactions` et `POST /api/accounts/transfers`. API budget : `GET /api/budget?month=AAAA-MM` et `PUT /api/budget` avec `month`, `category` et `monthlyLimit`. Ces routes sont réservées au rôle `CLIENT` et chaque opération est limitée aux comptes du propriétaire.

Tests backend : `cd backend; mvn test`.

## Conception

Voir l'[architecture fonctionnelle](docs/ARCHITECTURE_FONCTIONNELLE.md) pour les acteurs, parcours, données, API et ordre de développement du MVP.
