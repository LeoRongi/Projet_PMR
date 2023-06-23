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
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*

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

class Itineraire : AppCompatActivity() {

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

        voiceButton.setOnClickListener{
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say something...")
            try {
                activityResultLauncher.launch(intent)
            }catch (exp:ActivityNotFoundException)
            {
                Toast.makeText(applicationContext,"Device not supported", Toast.LENGTH_SHORT).show()
            }
        }
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                val words = speechText[0].split(" ")
                Log.i("speechText", words.toString())
                when (words[0]) {
                    "générer" -> {
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



        mapPanel.setMap(map)
        val pointsToHide = mutableListOf<Point>()
        generateButton.setOnClickListener {
            Log.i("Nouvelle génération", "_____________________________________________________________________")
            var currentStep = 0
            pointsToHide.clear()


            val randomPoints = generateRandomPoints(editText.text.toString().toInt(), map)


            val checkPoints = checkpoints(randomPoints)
            val (optimalOrder, optimalPath) = sortCheckpoints(checkPoints)
            var currentPath = optimalPath[currentStep]

            mapPanel.setPoints(randomPoints, checkPoints, optimalOrder, currentPath)

            nextStep.setOnClickListener {
                if (currentStep +1 > optimalPath.size-1) {
                    Toast.makeText(this, "Fin du trajet", Toast.LENGTH_SHORT).show()
                } else {
                    pointsToHide.add(currentPath[0])
                    currentStep += 1
                    currentPath = optimalPath[currentStep]
                    mapPanel.setPath(currentPath)
                    mapPanel.setHiddenCheckpoints(pointsToHide)
                }
            }
            previousStep.setOnClickListener {
                if (currentStep -1 < 0) {
                    Toast.makeText(this, "Début du trajet", Toast.LENGTH_SHORT).show()
                } else {
                    currentStep -= 1
                    currentPath = optimalPath[currentStep]
                    pointsToHide.removeAt(pointsToHide.size-1)
                    mapPanel.setPoints(randomPoints, checkPoints, optimalOrder, currentPath)
                    mapPanel.setHiddenCheckpoints(pointsToHide)
                }
            }
        }
    }


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
        Log.i("Unique permutations size", permutations.distinct().size.toString())

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

    private fun generatePermutations(points: MutableList<Point>, size: Int, permutations: MutableList<List<Point>>) {
        if (size == 1 ) {
            permutations.add(points.toList())
            return
        }

        for (i in 0 until size) {
            generatePermutations(points, size - 1, permutations)
            if (size % 2 == 1) {
                swap(points, 0, size - 1)
            } else {
                swap(points, i, size - 1)
            }
            if (permutations.size >= 30000) // Ajout de la vérification du nombre de permutations après chaque itération
                return

        }
    }


    private fun swap(points: MutableList<Point>, i: Int, j: Int) {
        val temp = points[i]
        points[i] = points[j]
        points[j] = temp
    }


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
                if (0 < point.x && 0 < point.y && point.x < ligne - 1 && point.y < colonne ) {
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
