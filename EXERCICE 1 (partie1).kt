import kotlinx.coroutines.*
import kotlin.random.Random


class Commande(
    val id: Int,
    val plats: List<String>,
    val total: Double
) {
    fun afficherDetails() {
        println("Commande n°$id | Plats: $plats | Total: $total DH")
    }
}

class Serveur(val nom: String) {

    fun prendreCommande(plats: List<String>, prix: List<Double>): Commande {
        var totalDouble = 0.0
        for (p in prix) {
            totalDouble += p
        }
        val idUnique = Random.nextInt(1, 1000)
        val commande = Commande(idUnique, plats, totalDouble)
        println("Le serveur $nom a pris la commande n°$idUnique.")
        return commande
    }

    fun afficherCommande(commandes: List<Commande>) {
        println("\n--- Commandes prises par $nom ---")
        for (c in commandes) {
            c.afficherDetails()
        }
    }
}


class Cuisinier(val nom: String) {

    suspend fun preparerPlat(plat: String): String {
        println("$nom commence la préparation du plat : $plat")
        delay(1000)
        println("$nom a terminé le plat : $plat")
        return plat
    }

    suspend fun preparerCommande(commande: Commande): List<String> = coroutineScope {
        val tasks = mutableListOf<Deferred<String>>()
        for (plat in commande.plats) {
            val task = async { preparerPlat(plat) }
            tasks.add(task)
        }
        tasks.awaitAll()
    }
}

class Cuisine(val cuisiniers: List<Cuisinier>) {

    suspend fun gererPreparationCommande(commande: Commande) = coroutineScope {
        println("\n[Cuisine] Début de la préparation de la commande n°${commande.id}...")
        val tasks = mutableListOf<Deferred<String>>()

        for (i in commande.plats.indices) {
            val plat = commande.plats[i]
            val cuisinier = cuisiniers[i % cuisiniers.size]
            val task = async { cuisinier.preparerPlat(plat) }
            tasks.add(task)
        }

        tasks.awaitAll()
        println("[Cuisine] Tous les plats de la commande n°${commande.id} sont prêts !")
    }
}


class Caisse {

    suspend fun traiterPaiement(commande: Commande): Boolean {
        println("\n[Caisse] Traitement du paiement pour la commande n°${commande.id}...")
        delay(1000)

        val reussi = Random.nextInt(100) < 80

        if (reussi) {
            println("[Caisse] Paiement de ${commande.total} DH validé avec succès !")
            return true
        } else {
            throw Exception("[Caisse] Échec du paiement pour la commande n°${commande.id} !")
        }
    }

    fun annulerPaiement(commande: Commande, job: Job) {
        job.cancel()
        println("[Caisse] Le paiement de la commande n°${commande.id} a été annulé.")
    }
}


class Restaurant(
    val serveurs: List<Serveur>,
    val cuisine: Cuisine,
    val caisse: Caisse
) {
    val commandes = mutableListOf<Commande>()

    suspend fun prendreCommandeEtTraiter(serveur: Serveur, plats: List<String>, prix: List<Double>) {
        val commande = serveur.prendreCommande(plats, prix)
        commandes.add(commande)

        cuisine.gererPreparationCommande(commande)

        try {
            caisse.traiterPaiement(commande)
        } catch (e: Exception) {
            println("Erreur : ${e.message}")
        }
    }

    fun afficherCommandesEnCours() {
        println("\n=== COMMANDES EN COURS ===")
        for (c in commandes) {
            c.afficherDetails()
        }
    }
}


fun main() = runBlocking {
    val serveurKarim = Serveur("Karim")
    val chefAli = Cuisinier("Chef Ali")
    val chefSami = Cuisinier("Chef Sami")

    val cuisine = Cuisine(listOf(chefAli, chefSami))
    val caisse = Caisse()
    val restaurant = Restaurant(listOf(serveurKarim), cuisine, caisse)

    val listePlats = listOf("Tajine de Poulet", "Salade Marocaine", "Jus d'Orange")
    val listePrix = listOf(60.0, 20.0, 15.0)

    restaurant.prendreCommandeEtTraiter(serveurKarim, listePlats, listePrix)
    restaurant.afficherCommandesEnCours()
}