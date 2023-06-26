package com.example.projet_pmr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.collections.ArrayList

val map = arrayOf(
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 1, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
)
val zigzag = listOf(
    Point(13, 6),
    Point(13, 5),
    Point(13, 4),
    Point(13, 3),
    Point(13, 2),
    Point(13, 1),
    Point(13, 0),
    Point(12, 0),
    Point(11, 0),
    Point(10, 0),
    Point(10, 1),
    Point(10, 2),
    Point(10, 3),
    Point(10, 4),
    Point(10, 5),
    Point(10, 6),
    Point(9, 6),
    Point(8, 6),
    Point(7, 6),
    Point(7, 5),
    Point(7, 4),
    Point(7, 3),
    Point(7, 2),
    Point(7, 1),
    Point(7, 0),
    Point(6, 0),
    Point(5, 0),
    Point(4, 0),
    Point(4, 1),
    Point(4, 2),
    Point(4, 3),
    Point(4, 4),
    Point(4, 5),
    Point(4, 6),
    Point(3, 6),
    Point(2, 6),
    Point(2, 7),
    Point(1, 7),
    Point(1, 6),
    Point(1, 5),
    Point(1, 4),
    Point(1, 3),
    Point(1, 2),
    Point(1, 1),
    Point(1, 0)
)

val ligne = map.size
val colonne = map[0].size
val startPoint = Point(ligne - 1, colonne - 1)
val endPoint = Point(7, colonne-1)

@Suppress("DEPRECATION")
class Itineraire : AppCompatActivity() {

    private lateinit var coordinatesList: MutableList<Point>
    private lateinit var invertedCoordinatesList : MutableList<Point>
    private val relationTable = mutableMapOf<Point, String>()

    @SuppressLint("SetTextI18n", "UnsafeOptInUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itineraire)

        lateinit var activityResultLauncher: ActivityResultLauncher<Intent>


        val mapPanel: MapPanel = findViewById(R.id.mapPanel)
        val generateButton: Button = findViewById(R.id.generateButton)
        val nextStep: Button = findViewById(R.id.nextStep)
        val previousStep: Button = findViewById(R.id.previousStep)
        val editText: EditText = findViewById(R.id.nbArticle)
        val voiceButton: ImageButton = findViewById(R.id.voiceButton)
        val nextArticle : TextView = findViewById(R.id.nextArticle)
        val qrCodeButton: ImageButton = findViewById(R.id.qrCodeButton)


        coordinatesList = intent.getSerializableExtra("coordinatesList") as? MutableList<Point> ?: mutableListOf()
        val articleNamesList = intent.getStringArrayListExtra("articleNamesList")
        val barcode = intent.getStringExtra("barcode")
        if (barcode != null) {
            Log.i("barcode", barcode)
        }
        else Log.i("barcode", "Null")


        editText.visibility = View.GONE
        if (coordinatesList.size == 0) {
            editText.visibility = View.VISIBLE
            Toast.makeText(this, "La liste est vide. Vous pouvez générer des itinéraires", Toast.LENGTH_SHORT).show()
        }
        else {
            invertedCoordinatesList = inverseCoordinates(coordinatesList).toMutableList()
            Log.i("articleNamesList", articleNamesList.toString())
            logCoordinates(invertedCoordinatesList)

            //Création d'une relation entre les coordonnées de chaque point et le nom de l'article

            if (invertedCoordinatesList.size == articleNamesList!!.size) {
                for (i in invertedCoordinatesList.indices) {
                    val point = invertedCoordinatesList[i]
                    val articleName = articleNamesList[i]
                    relationTable[point] = articleName
                }
            }
        }




        //Listener du bouton de commande vocale
        voiceButton.setOnClickListener{
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Dites calculer, suivant ou précédent pour naviguer dans l'itinéraire")
            try {
                activityResultLauncher.launch(intent)
            }catch (exp:ActivityNotFoundException)
            {
                Toast.makeText(applicationContext,"Device not supported", Toast.LENGTH_SHORT).show()
            }
        }
        //Paramétrage de la récupération des données Speech to Text
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                val words = speechText[0].split(" ")
                Log.i("speechText", words.toString())

                //Actions suivant le mot prononcé
                when (words[0]) {
                    "générer" -> {
                        if (coordinatesList.size == 0) {
                            if (words.size > 1) {
                                if (words[1].toIntOrNull() != null) {
                                    Log.i("words[1]",words[1])
                                    editText.setText(words[1])
                                }
                                else Toast.makeText(
                                    applicationContext,
                                    "Le mot qui suit générer doit être un entier",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                            generateButton.performClick()
                    }

                    "calculer"-> {
                        generateButton.performClick()
                    }
                    "suivant" -> {
                        nextStep.performClick()
                    }
                    "précédent" -> {
                        previousStep.performClick()
                    }
                    "article", "articles" -> {

                        if (words[1].toIntOrNull() != null) {
                            editText.setText(words[1])
                        }
                        else Toast.makeText(applicationContext, "Le mot qui suit article doit être un entier", Toast.LENGTH_SHORT).show()

                    }
                    else -> {
                        Toast.makeText(applicationContext, "Commande inconnue", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        //On dessine la carte
        mapPanel.setMap(map)
        val pointsToHide = mutableListOf<Point>()

        //Listener du bouton calcul d'itinéraire
        generateButton.setOnClickListener {
            Log.i("Nouvelle génération", "_____________________________________________________________________")

            //Si la liste de course est vide, on passe en mode génération aléatoire d'articles en utilisant l'EditText
            if (coordinatesList.size == 0) { invertedCoordinatesList = generateRandomPoints(editText.text.toString().toInt(), map).toMutableList()}

            //Définition des variables utiles à la gestion de l'itinéraire
            var currentStep = 0
            val checkPoints = checkpoints(invertedCoordinatesList)
            val (optimalOrder, optimalPath) = sortCheckpoints(checkPoints)
            var currentPath = optimalPath[currentStep]
            var currentArticle = getAdjacentArticle(optimalOrder[1])
            pointsToHide.clear()

            //On dessine les points d'intérêt et le premier chemin à parcourir, on affiche le prochain article à ramasser
            mapPanel.setPoints(invertedCoordinatesList, checkPoints, optimalOrder, currentPath)
            nextArticle.text = "Prochain article: $currentArticle"



            //Listener du bouton d'étape suivante
            nextStep.setOnClickListener {
                if (currentStep +1 > optimalPath.size-1) {
                    Toast.makeText(this, "Fin du trajet", Toast.LENGTH_SHORT).show()
                } else {
                    pointsToHide.add(currentPath[0])
                    currentStep += 1
                    currentPath = optimalPath[currentStep]
                    currentArticle = getAdjacentArticle(optimalOrder[currentStep+1])
                    mapPanel.setPath(currentPath)
                    mapPanel.setHiddenCheckpoints(pointsToHide)
                    nextArticle.text = "Prochain article: $currentArticle"
                }
            }
            //Listener du bouton d'étape précédente
            previousStep.setOnClickListener {
                if (currentStep -1 < 0) {
                    Toast.makeText(this, "Début du trajet", Toast.LENGTH_SHORT).show()
                } else {
                    currentStep -= 1
                    currentPath = optimalPath[currentStep]
                    currentArticle = getAdjacentArticle(optimalOrder[currentStep+1])
                    pointsToHide.removeAt(pointsToHide.size-1)
                    mapPanel.setPoints(invertedCoordinatesList, checkPoints, optimalOrder, currentPath)
                    mapPanel.setHiddenCheckpoints(pointsToHide)
                    nextArticle.text = "Prochain article: $currentArticle"
                }
            }

            //Listener du bouton de scan QRcode
            qrCodeButton.setOnClickListener{
                val intent = Intent(applicationContext, NavActivity::class.java)
                intent.putExtra("coordinatesList", ArrayList(currentPath))
                startActivity(intent)
            }
        }
        }




    //Fonction pour obtenir les articles adjacents à une case chemin du magasin
    fun getAdjacentArticle(point: Point) : List<String> {
        val adjacentArticle = mutableListOf<String>()
        Log.i("point", point.toString())
        val dessus = Point(point.x-1,point.y)
        val dessous = Point(point.x+1,point.y)
        if (relationTable[dessus] != null) adjacentArticle.add(relationTable[dessus]!!)
        Log.i("dessus", relationTable[dessus].toString())
        if (relationTable[dessous] != null) adjacentArticle.add(relationTable[dessous]!!)
        Log.i("dessous", relationTable[dessous].toString())
        return adjacentArticle
    }

    //Fonction pour intervertir les coordonnées x et y d'une liste de points
    fun inverseCoordinates(points: List<Point>): List<Point> {
        val invertedPoints = mutableListOf<Point>()
        for (point in points) {
            val invertedPoint = Point(point.y, point.x)
            invertedPoints.add(invertedPoint)
        }
        return invertedPoints
    }

    //Fonction pour afficher les coordonnées d'une liste de point dans logcat
    private fun logCoordinates(coordList : List<Point>) {
        for (point in coordList) {
            Log.d("Coordinates", "X: ${point.x}, Y: ${point.y}")
        }
    }
    //Fonction pour générer un nombre count d'articles aléatoires dans le magasin
    private fun generateRandomPoints(count: Int, map: Array<IntArray>): List<Point> {
        val random = Random()
        val points = ArrayList<Point>()
        val availablePoints = ArrayList<Point>()

        for (i in map.indices) {
            for (j in map[i].indices) {
                if (map[i][j] == 0) {
                    availablePoints.add(Point(i, j))
                }
            }
        }

        val maxPoints = minOf(count, availablePoints.size)
        for (i in 0 until maxPoints) {
            val randomIndex = random.nextInt(availablePoints.size)
            val point = availablePoints[randomIndex]
            points.add(point)
            availablePoints.removeAt(randomIndex)
        }

        return points
    }

    //Fonction pour déterminer les cases chemin adjacentes à des articles situés sur les cases de la liste points
    private fun checkpoints(points: List<Point>): List<Point> {
        val adjustedPoints = mutableListOf<Point>()

        for (point in points) {
            val adjustedX =
                if (point.x==14) {point.x-1}
                else if (map[point.x + 1][point.y] == 1) {
                    point.x + 1
                } else {
                    point.x - 1
                }
            adjustedPoints.add(Point(adjustedX, point.y))
        }
        Log.i("Checkpoints", adjustedPoints.distinct().toString())
        return adjustedPoints.distinct()
    }

    //Fonction pour déterminer le plus court chemin passant par tous les points
    // de la liste checkpoints en partant de la case startPoint et finissant par endPoint

    private fun sortCheckpoints(checkpoints: List<Point>): Pair<List<Point>, List<List<Point>>> {

        val sortedCheckpoints = checkpoints.sortedBy{-it.x}.sortedBy { zigzag.indexOf(it) }
        val permutations = mutableListOf<List<Point>>()
        var (shortestDistance,premierPath) = calculateTotalDistance(startPoint,endPoint,sortedCheckpoints)
        Log.i("Premiere distance", shortestDistance.toString())
        val optimalPath = mutableListOf<List<Point>>()
        val optimalOrder = mutableListOf<Point>()
        for (premiershortpath in premierPath) {optimalPath.add(premiershortpath)}
        for (point in sortedCheckpoints) {optimalOrder.add(point)}

        generatePermutations(sortedCheckpoints.toMutableList(), checkpoints.size, permutations)
        Log.i("Permutation size", permutations.size.toString())

        for (permutation in permutations) {
            val (distance, path) = calculateTotalDistance(startPoint, endPoint, permutation)
            if (distance < shortestDistance) {
                shortestDistance = distance
                optimalOrder.clear()
                optimalPath.clear()
                for (point in permutation) {optimalOrder.add(point)}
                for (shortpath in path) {optimalPath.add(shortpath)}
            }
        }
        Log.i("startPoint", startPoint.toString())
        Log.i("endPoint", endPoint.toString())
        optimalOrder.add(0,startPoint)
        optimalOrder.add(endPoint)
        Log.i("optimalOrder", optimalOrder.toString())
        Log.i("Size of optimalPath", optimalPath.size.toString())
        Log.i("distance", shortestDistance.toString())
        return Pair(optimalOrder, optimalPath)
    }

    //Fonction pour faire les permutations des points de la liste points,
    // permet d'essayer des combinaisons de points différentes
    // basé sur l'algorithme de Heap
    private fun generatePermutations(points: MutableList<Point>, size: Int, permutations: MutableList<List<Point>>) {
        if (size == 1 ) {
            permutations.add(points.toList())
            return
        }

        for (i in 0 until size) {
            if (size % 2 == 1) {
                swap(points, 0, size - 1)
            } else {
                swap(points, i, size - 1)
            }
            generatePermutations(points, size - 1, permutations)
            if (permutations.size >= 30000) // Ajout de la vérification du nombre de permutations après chaque itération
                return

        }
    }

    //Fonction pour inverser 2 points d'une liste selon leurs indexes
    private fun swap(points: MutableList<Point>, i: Int, j: Int) {
        val temp = points[i]
        points[i] = points[j]
        points[j] = temp
    }

    //Fonction pour calculer la longueur totale d'un itinéraire
    private fun calculateTotalDistance(startPoint: Point, endPoint: Point, points: List<Point>): Pair<Float, List<List<Point>>> {
        var totalDistance = 0f
        val startToEnd = points.toMutableList()
        startToEnd.add(endPoint)
        val currentPoint = Point(startPoint.x, startPoint.y)
        val listOfShortestPath = mutableListOf<List<Point>>()

        for (point in startToEnd) {
            val (distance,shortestPath) = calculateDistance(map, currentPoint, point)
            totalDistance += distance
            currentPoint.set(point.x, point.y)
            listOfShortestPath += shortestPath
        }

        return Pair(totalDistance, listOfShortestPath)
    }



    //Fonction pour calculer la distance la plus courte d'un pour p1 à un point p2
    // utilise l'algorithme de parcourt en largeur BFS
    private fun calculateDistance(matrix: Array<IntArray>, p1: Point, p2: Point): Pair<Int, List<Point>> {
        val startX = p1.x
        val startY = p1.y
        val endX = p2.x
        val endY = p2.y
        val rows = matrix.size
        val columns = matrix[0].size

        // Vérifier si les coordonnées de départ et d'arrivée sont valides
        if (startX < 0 || startX >= rows || startY < 0 || startY >= columns ||
            endX < 0 || endX >= rows || endY < 0 || endY >= columns
        ) {
            throw IllegalArgumentException("sX : $startX, sY : $startY, eX : $endX, eY : $endY, rows : $rows, col : $columns")
        }

        // Vérifier si les coordonnées de départ et d'arrivée sont walkable
        if (matrix[startX][startY] == 0 || matrix[endX][endY] == 0) {
            throw IllegalArgumentException("Start or end point is not walkable")
        }

        // Créer une matrice pour marquer les cases visitées
        val visited = Array(rows) { BooleanArray(columns) }

        // Définir les déplacements possibles (haut, bas, gauche, droite)
        val dx = intArrayOf(-1, 1, 0, 0)
        val dy = intArrayOf(0, 0, -1, 1)

        // File d'attente pour le parcours en largeur (BFS)
        val queue: Queue<Pair<Int, Int>> = LinkedList()

        // Ajouter les coordonnées de départ à la file d'attente
        queue.offer(Pair(startX, startY))
        visited[startX][startY] = true

        // Tableau pour enregistrer le chemin parcouru
        val path = Array(rows) { arrayOfNulls<Point>(columns) }

        // Parcours en largeur (BFS) pour trouver la distance et enregistrer le chemin
        while (queue.isNotEmpty()) {
            val size = queue.size

            for (i in 0 until size) {
                val current = queue.poll()

                // Vérifier si on a atteint la destination
                if (current != null) {
                    if (current.first == endX && current.second == endY) {
                        // Construire le chemin à partir des coordonnées enregistrées dans `path`
                        val shortestPath = reconstructPath(p1,p2, path)
                        return Pair(shortestPath.size - 1, shortestPath)

                    }
                }

                // Explorer les déplacements possibles
                for (j in 0 until 4) {
                    val nextX = current!!.first + dx[j]
                    val nextY = current.second + dy[j]

                    // Vérifier si la case suivante est dans les limites de la matrice
                    if (nextX in 0 until rows && nextY in 0 until columns) {
                        // Vérifier si la case suivante est walkable et non visitée
                        if (matrix[nextX][nextY] == 1 && !visited[nextX][nextY]) {
                            // Marquer la case suivante comme visitée, l'ajouter à la file d'attente et enregistrer le chemin
                            visited[nextX][nextY] = true
                            queue.offer(Pair(nextX, nextY))
                            path[nextX][nextY] = Point(current.first, current.second)
                        }
                    }
                }
            }
        }

        // Si la destination n'a pas été atteinte, cela signifie qu'il n'y a pas de chemin valide
        throw IllegalArgumentException("No valid path between start and end points")
    }

    private fun reconstructPath(p1: Point, p2: Point, path: Array<Array<Point?>>): List<Point> {
        val startX = p1.x
        val startY = p1.y
        val endX = p2.x
        val endY = p2.y
        val shortestPath = mutableListOf<Point>()
        var currentX = endX
        var currentY = endY

        // Remonter le chemin à partir de la destination jusqu'à l'origine
        while (currentX != startX || currentY != startY) {
            val currentPoint = path[currentX][currentY]
                ?: throw IllegalArgumentException("Invalid path, missing intermediate points")
            shortestPath.add(Point(currentX, currentY))
            currentX = currentPoint.x
            currentY = currentPoint.y
        }

        shortestPath.add(Point(startX, startY))
        shortestPath.reverse()

        return shortestPath
    }

}

//Classe graphique de notre application
class MapPanel(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val squareSize = 100
    private val wallColor = Color.BLACK
    private val floorColor = Color.WHITE
    private val red = ContextCompat.getColor(context, R.color.red)
    private val blue = ContextCompat.getColor(context, R.color.blue)

    private var map: Array<IntArray>? = null
    private var randomPoints: List<Point>? = null
    private var checkpoints: List<Point>? = null
    private var orderedCheckpoints: List<Point>? = null
    private var currentPath : List<Point>? = null
    private var hiddenCheckpoints : List<Point>? = null



    fun setMap(map: Array<IntArray>) {
        this.map = map
        invalidate()
    }

    fun setPoints(randomPoints: List<Point>, checkpoints: List<Point>, orderedCheckpoints: List<Point>, currentPath: List<Point>) {
        this.randomPoints = randomPoints
        this.checkpoints = checkpoints
        this.orderedCheckpoints = orderedCheckpoints
        this.currentPath = currentPath
        invalidate()
    }
    fun setPath(currentPath: List<Point>) {
        this.currentPath = currentPath
        invalidate()
    }

    fun setHiddenCheckpoints(hiddenCheckpoints: List<Point>) {
        this.hiddenCheckpoints = hiddenCheckpoints
        invalidate()
    }




    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //On dessine la carte vierge suivant la matrice map

        map?.let {
            val paint = Paint()

            for (i in it.indices) {
                for (j in it[i].indices) {
                    val color = when {
                        it[i][j] == 1 -> floorColor
                        else -> wallColor
                    }
                    paint.color = color
                    canvas.drawRect(
                        j * squareSize.toFloat()+2,
                        i * squareSize.toFloat()+2,
                        (j + 1) * squareSize.toFloat(),
                        (i + 1) * squareSize.toFloat(),
                        paint
                    )
                }
            }
        }

        //On dessine les points d'articles en rouge
        randomPoints?.let {
            val randomPointPaint = Paint()
            randomPointPaint.color = red
            randomPointPaint.strokeWidth = 8f // Épaisseur des lignes de la croix

            for (point in it) {
                val centerX = (point.y + 0.5f) * squareSize.toFloat()
                val centerY = (point.x + 0.5f) * squareSize.toFloat()

                // Tracer les lignes horizontales et verticales
                canvas.drawLine(centerX - squareSize / 4f, centerY, centerX + squareSize / 4f, centerY, randomPointPaint)
                canvas.drawLine(centerX, centerY - squareSize / 4f, centerX, centerY + squareSize / 4f, randomPointPaint)
            }
        }

//On dessine les points de passage pour ramasser les articles en vert, avec ordre de passage inscrit
        orderedCheckpoints?.let {
            val checkpointPaint = Paint()
            checkpointPaint.color = blue
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
            }

            for ((index, point) in it.withIndex()) {
                val text = if (index == 0) "Start" else if (index==it.size-1) "End" else (index).toString()
                canvas.drawRect(
                    point.y * squareSize.toFloat()+2,
                    point.x * squareSize.toFloat()+2,
                    (point.y + 1) * squareSize.toFloat(),
                    (point.x + 1) * squareSize.toFloat(),
                    checkpointPaint
                )

                val textX = (point.y + 0.5f) * squareSize.toFloat()
                val textY = (point.x + 0.5f) * squareSize.toFloat() + textPaint.textSize / 3
                canvas.drawText(text, textX, textY, textPaint)
            }
        }

        //On efface les points un fois atteints
        hiddenCheckpoints?.let{
            val hiddePointPaint = Paint()
            val hiddeArticlePaint = Paint()
            hiddePointPaint.color = Color.WHITE

            for (point in it) {
                canvas.drawRect(
                    point.y * squareSize.toFloat() + 2,
                    point.x * squareSize.toFloat() + 2,
                    (point.y + 1) * squareSize.toFloat(),
                    (point.x + 1) * squareSize.toFloat(),
                    hiddePointPaint
                )
                if (0 < point.x &&  point.x < ligne - 1 && point.y < colonne ) {
                    if (map!![point.x + 1][point.y] == 0) {
                        canvas.drawRect(
                            point.y * squareSize.toFloat() + 2,
                            (point.x + 1) * squareSize.toFloat() + 2,
                            (point.y + 1) * squareSize.toFloat(),
                            (point.x + 2) * squareSize.toFloat(),
                            hiddeArticlePaint
                        )
                    }
                    if (map!![point.x - 1][point.y] == 0) {
                        canvas.drawRect(
                            point.y * squareSize.toFloat() + 2,
                            (point.x - 1) * squareSize.toFloat() + 2,
                            (point.y + 1) * squareSize.toFloat(),
                            (point.x) * squareSize.toFloat(),
                            hiddeArticlePaint
                        )
                    }
                }
            }
        }

        //On dessine le chemin passant par les points de currentPath
        currentPath?.let { path ->
            val pathPaint = Paint()
            val startendPaint = Paint()
            val pointRadius = squareSize / 3f
            startendPaint.color = Color.YELLOW
            pathPaint.color = Color.BLUE
            pathPaint.style = Paint.Style.STROKE
            pathPaint.strokeWidth = 12f


            val pathPoints = path.map { point ->
                PointF(
                    (point.y + 0.5f) * squareSize.toFloat(),
                    (point.x + 0.5f) * squareSize.toFloat()
                )
            }
            val pathPath = Path()
            if (pathPoints.isNotEmpty()) {
                pathPath.moveTo(pathPoints[0].x, pathPoints[0].y)
                for (i in 1 until pathPoints.size) {
                    pathPath.lineTo(pathPoints[i].x, pathPoints[i].y)
                }
            }
            canvas.drawPath(pathPath, pathPaint)

            for ((index, point) in path.withIndex()) {
                if (index == 0) {
                    canvas.drawCircle(
                        point.y * squareSize.toFloat() + 2 + squareSize / 2f,
                        point.x * squareSize.toFloat() + 2
                                + squareSize / 2f,
                        pointRadius,
                        Paint().apply {
                            color = red
                            style = Paint.Style.FILL
                        }
                    )
                }
                if (index == path.size - 1) {
                    canvas.drawRect(
                        point.y * squareSize.toFloat() + 2,
                        point.x * squareSize.toFloat() + 2,
                        (point.y + 1) * squareSize.toFloat(),
                        (point.x + 1) * squareSize.toFloat(),
                        startendPaint
                    )
                }
            }
        }
    }

}
