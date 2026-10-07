import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
class Produit(
    val id: Int,
    val nom: String,
    var quantite: Int
) {
    fun afficherDetails() {
        println("Produit ID: $id vert{} Nom:$nom | Quantité en stock: $quantite")
    }
}

class Stock {
    val produits = mutableMapOf<Int, Produit>()

    fun ajouterProduit(produit: Produit) {
        produits[produit.id] = produit
        println("Produit '${produit.nom}' ajouté au stock.")
    }

    suspend fun ajouterQuantite(idProduit: Int, quantite: Int) {
        delay(500) // Simulation d'une opération asynchrone
        val produit = produits[idProduit]
        if (produit != null) {
            produit.quantite += quantite
            println("+$quantite ajoutée(s) au produit '${produit.nom}'. Nouvelle quantité: ${produit.quantite}")
        } else {
            println("Produit ID $idProduit introuvable.")
        }
    }

    suspend fun retirerQuantite(idProduit: Int, quantite: Int) {
        delay(500) // Simulation d'une opération asynchrone
        val produit = produits[idProduit]
        if (produit == null) {
            throw Exception("Produit ID $idProduit introuvable !")
        }
        if (produit.quantite < quantite) {
            throw Exception("Stock insuffisant pour '${produit.nom}' ! (Demandé: $quantite, Disponible:${produit.quantite})")
        }
        produit.quantite -= quantite
        println("-$quantite retirée(s) du produit '${produit.nom}'. Quantité restante: ${produit.quantite}")
    }

    fun afficherStock() {
        println("\n=== ÉTAT ACTUEL DU STOCK ===")
        for (produit in produits.values) {
            produit.afficherDetails()
        }
    }
}

// ==========================================
// PARTIE 2 : Commandes Clients
// ==========================================

// Classe Commande
class CommandeClient(
    val idCommande: Int,
    val produits: List<Pair<Int, Int>> // Pair(idProduit, quantite)
) {
    fun afficherCommande() {
        println("Commande n°$idCommande -> Produits demandés: $produits")
    }
}

class GestionnaireCommandes(val stock: Stock) {
    val commandes = mutableListOf<CommandeClient>()

    suspend fun traiterCommande(commande: CommandeClient) {
        println("\n[Traitement] Début de la commande n°${commande.idCommande}...")
        for (item in commande.produits) {
            val idProduit = item.first
            val quantiteDemandee = item.second
            try {
                stock.retirerQuantite(idProduit, quantiteDemandee)
            } catch (e: Exception) {
                println("Erreur commande n°${commande.idCommande} :${e.message}")
            }
        }
        println("[Traitement] Fin de la commande n°${commande.idCommande}")
    }

    suspend fun gererCommandes(listeCommandes: List<CommandeClient>) = coroutineScope {
        for (cmd in listeCommandes) {
            launch {
                traiterCommande(cmd)
            }
        }
    }
}


class Entrepot(
    val stock: Stock,
    val gestionnaireCommandes: GestionnaireCommandes
) {
    suspend fun ajouterProduitAuStock(produit: Produit) = coroutineScope {
        launch {
            stock.ajouterProduit(produit)
        }
    }

    suspend fun retirerProduitDuStock(idProduit: Int, quantite: Int) = coroutineScope {
        launch {
            try {
                stock.retirerQuantite(idProduit, quantite)
            } catch (e: Exception) {
                println("Erreur Entrepôt : ${e.message}")
            }
        }
    }

    suspend fun gererInventaire() = coroutineScope {
        launch {
            println("\n--- INVENTAIRE EN COURS ---")
            delay(300)
            stock.afficherStock()
            println("--- FIN DE L'INVENTAIRE ---\n")
        }
    }
}

// ==========================================
// PARTIE 4 : Flux d'Événements avec Flow
// ==========================================

// Événement de mise à jour du stock
data class EvenementStock(val meesage: String)

class GestionnaireEvenements {
    private val _fluxEvenements = MutableSharedFlow<EvenementStock>()
    val fluxEvenements: SharedFlow<EvenementStock> = _fluxEvenements

    suspend fun emettreEvenement(message: String) {
        _fluxEvenements.emit(EvenementStock(message))
    }
}

// ==========================================
// PROGRAMME PRINCIPAL (MAIN)
// ==========================================

fun main() = runBlocking {
    val stock = Stock()
    val gestionnaireCmd = GestionnaireCommandes(stock)
    val entrepot = Entrepot(stock, gestionnaireCmd)
    val gestionnaireEvenements = GestionnaireEvenements()