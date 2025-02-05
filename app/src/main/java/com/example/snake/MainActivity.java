package com.example.snake;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jeu de Snake avec :
 *  - Tête en ImageView, 4 images : snake_head_up.png, snake_head_right.png, snake_head_down.png, snake_head_left.png
 *  - Corps en TextView verts
 *  - Pomme en ImageView (apple.png)
 *  - Contrôles par accéléromètre
 *  - Score, pause, restart
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Directions possibles
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    // Accéléromètre
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // Tête du serpent
    private int snakeRow = 0;     // Position (ligne)
    private int snakeColumn = 0;  // Position (colonne)
    private int currentDirection = RIGHT; // Direction initiale
    private ImageView snakeHeadView;      // L'image de la tête

    // Corps du serpent (positions + TextViews)
    private List<int[]> snakeCoordinates; // [0] => tête, [1..] => corps
    private List<TextView> snakeBodyViews; // Les vues (TextView) de chaque segment du corps

    // Grille
    private int rowCount = 20;
    private int columnCount = 20;
    private int cellSize;

    // Pomme
    private int appleRow = -1;
    private int appleColumn = -1;
    private ImageView appleCell;

    // Score
    private int score = 0;
    private TextView scoreText;

    // État du jeu
    private boolean isGameOver = false;
    private boolean isPaused = false;

    // Button Pause/Restart
    private Button pauseButton;

    // Pour limiter la fréquence de mise à jour par l'accéléromètre
    private long lastUpdate = 0;
    private static final int UPDATE_THRESHOLD = 100; // en ms

    // Générateur aléatoire
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Le layout XML

        random = new Random();

        // 1) Calculer combien de cellules on met dans la grille
        calculateGridSize();

        // 2) Initialiser la grille
        initializeGridLayout();

        // 3) Initialiser le serpent (tête + corps)
        initializeSnake();

        // 4) Initialiser la pomme
        initializeApple();

        // 5) Initialiser l'accéléromètre
        initializeSensors();

        // 6) Gérer l'affichage du score
        scoreText = findViewById(R.id.score_text);
        scoreText.setText("Score : " + score);

        // 7) Bouton Pause / Restart
        pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(v -> {
            // Si le jeu est déjà terminé, on relance
            if (isGameOver) {
                restartGame();
                return;
            }
            // Sinon, on alterne pause / reprise
            if (!isPaused) {
                pauseGame();
            } else {
                resumeGame();
            }
        });
    }

    /**
     * Calcule la taille de la grille en fonction de la taille de l'écran.
     */
    private void calculateGridSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        int minCellSize = 50;
        columnCount = screenWidth / minCellSize;
        rowCount = screenHeight / minCellSize;

        if (columnCount < 10) columnCount = 10;
        if (rowCount < 10) rowCount = 10;
        if (columnCount > 30) columnCount = 30;
        if (rowCount > 50) rowCount = 50;
    }

    /**
     * Crée/paramètre le GridLayout et y ajoute des cellules grises.
     */
    private void initializeGridLayout() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);

        // Calcul de la taille d'une cellule
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth / columnCount, screenHeight / rowCount);

        // Fixer la taille totale du GridLayout pour éviter qu'il déborde
        gridLayout.getLayoutParams().width = cellSize * columnCount;
        gridLayout.getLayoutParams().height = cellSize * rowCount;

        // Ajouter des TextView "gris" pour chaque case (simple décor)
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                addGridCell(gridLayout, i, j);
            }
        }
    }

    private void addGridCell(GridLayout gridLayout, int row, int column) {
        TextView cell = new TextView(this);
        cell.setBackgroundColor(Color.LTGRAY);
        cell.setWidth(cellSize);
        cell.setHeight(cellSize);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.rowSpec = GridLayout.spec(row);
        params.columnSpec = GridLayout.spec(column);
        params.setMargins(1, 1, 1, 1);
        cell.setLayoutParams(params);

        gridLayout.addView(cell);
    }

    /**
     * Initialise le serpent : on crée la liste de coordonnées et la tête (ImageView).
     */
    private void initializeSnake() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        snakeCoordinates = new ArrayList<>();
        snakeBodyViews = new ArrayList<>();

        // Position initiale : milieu de la grille
        snakeRow = rowCount / 2;
        snakeColumn = columnCount / 2;

        // Ajout de la coordonnée de la tête
        snakeCoordinates.add(new int[]{snakeRow, snakeColumn});

        // Création de la tête (orientée vers le bas au démarrage, par exemple)
        snakeHeadView = new ImageView(this);
        snakeHeadView.setImageResource(R.drawable.snake_head_down);
        snakeHeadView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GridLayout.LayoutParams headParams = new GridLayout.LayoutParams();
        headParams.rowSpec = GridLayout.spec(snakeRow);
        headParams.columnSpec = GridLayout.spec(snakeColumn);
        headParams.width = cellSize;
        headParams.height = cellSize;
        headParams.setMargins(1, 1, 1, 1);
        snakeHeadView.setLayoutParams(headParams);

        gridLayout.addView(snakeHeadView);
    }

    /**
     * Méthode utilitaire : ajoute un segment de corps à la fin du serpent.
     */
    private void addBodySegment(GridLayout gridLayout, int row, int col) {
        snakeCoordinates.add(new int[]{row, col});

        TextView bodyPart = new TextView(this);
        bodyPart.setBackgroundColor(Color.GREEN);

        GridLayout.LayoutParams bodyParams = new GridLayout.LayoutParams();
        bodyParams.rowSpec = GridLayout.spec(row);
        bodyParams.columnSpec = GridLayout.spec(col);
        bodyParams.width = cellSize;
        bodyParams.height = cellSize;
        bodyParams.setMargins(1, 1, 1, 1);
        bodyPart.setLayoutParams(bodyParams);

        snakeBodyViews.add(bodyPart);
        gridLayout.addView(bodyPart);
    }

    /**
     * Déplace la liste des coordonnées : chaque segment prend la place du précédent,
     * et la tête prend snakeRow/snakeColumn.
     */
    private void moveSnakeCoordinates() {
        for (int i = snakeCoordinates.size() - 1; i > 0; i--) {
            snakeCoordinates.set(i, snakeCoordinates.get(i - 1));
        }
        // Mettre à jour la tête
        snakeCoordinates.set(0, new int[]{snakeRow, snakeColumn});
    }

    /**
     * Met à jour l'affichage du serpent (tête + corps) dans le GridLayout.
     * @param gridLayout le layout à utiliser
     */
    private void updateSnakePosition(GridLayout gridLayout) {
        if (isGameOver) return;

        // Retirer l'ancienne tête
        gridLayout.removeView(snakeHeadView);

        // Retirer l'ancien corps
        for (TextView bodyPartView : snakeBodyViews) {
            gridLayout.removeView(bodyPartView);
        }
        snakeBodyViews.clear();

        // Déplacer les coordonnées
        moveSnakeCoordinates();

        // Vérifier collision avec le corps
        int headRow = snakeCoordinates.get(0)[0];
        int headCol = snakeCoordinates.get(0)[1];
        for (int i = 1; i < snakeCoordinates.size(); i++) {
            if (snakeCoordinates.get(i)[0] == headRow
                    && snakeCoordinates.get(i)[1] == headCol) {
                endGame();
                return;
            }
        }

        // Réafficher la tête
        GridLayout.LayoutParams headParams = new GridLayout.LayoutParams();
        headParams.rowSpec = GridLayout.spec(headRow);
        headParams.columnSpec = GridLayout.spec(headCol);
        headParams.width = cellSize;
        headParams.height = cellSize;
        headParams.setMargins(1, 1, 1, 1);
        snakeHeadView.setLayoutParams(headParams);
        gridLayout.addView(snakeHeadView);

        // Réafficher le corps
        for (int i = 1; i < snakeCoordinates.size(); i++) {
            int row = snakeCoordinates.get(i)[0];
            int col = snakeCoordinates.get(i)[1];

            TextView bodyPart = new TextView(this);
            bodyPart.setBackgroundColor(Color.GREEN);

            GridLayout.LayoutParams bodyParams = new GridLayout.LayoutParams();
            bodyParams.rowSpec = GridLayout.spec(row);
            bodyParams.columnSpec = GridLayout.spec(col);
            bodyParams.width = cellSize;
            bodyParams.height = cellSize;
            bodyParams.setMargins(1, 1, 1, 1);
            bodyPart.setLayoutParams(bodyParams);

            snakeBodyViews.add(bodyPart);
            gridLayout.addView(bodyPart);
        }

        // Vérifier si on mange la pomme
        if (headRow == appleRow && headCol == appleColumn) {
            growSnake();
            spawnApple(gridLayout);
            updateScore();
        }
    }

    /**
     * Quand le serpent mange la pomme, on ajoute un segment à la fin.
     */
    private void growSnake() {
        int[] lastSegment = snakeCoordinates.get(snakeCoordinates.size() - 1);
        addBodySegment((GridLayout) findViewById(R.id.grid_layout),
                lastSegment[0],
                lastSegment[1]);
    }

    /**
     * Met à jour le score.
     */
    private void updateScore() {
        score++;
        scoreText.setText("Score : " + score);
    }

    /**
     * Initialise la pomme avec l'image apple.png et la place quelque part.
     */
    private void initializeApple() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        appleCell = new ImageView(this);
        appleCell.setImageResource(R.drawable.apple);
        appleCell.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GridLayout.LayoutParams appleParams = new GridLayout.LayoutParams();
        appleParams.width = cellSize;
        appleParams.height = cellSize;
        appleCell.setLayoutParams(appleParams);

        // Placement initial
        spawnApple(gridLayout);
    }

    /**
     * Place la pomme de façon aléatoire dans la grille.
     */
    private void spawnApple(GridLayout gridLayout) {
        appleRow = random.nextInt(rowCount);
        appleColumn = random.nextInt(columnCount);

        // Retirer l'ancienne pomme
        gridLayout.removeView(appleCell);

        GridLayout.LayoutParams appleParams = new GridLayout.LayoutParams();
        appleParams.rowSpec = GridLayout.spec(appleRow);
        appleParams.columnSpec = GridLayout.spec(appleColumn);
        appleParams.width = cellSize;
        appleParams.height = cellSize;
        appleCell.setLayoutParams(appleParams);

        // Ajouter la pomme
        gridLayout.addView(appleCell);
    }

    /**
     * Initialisation de l'accéléromètre.
     */
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListener();
    }

    private void registerSensorListener() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListener();
    }

    private void unregisterSensorListener() {
        sensorManager.unregisterListener(this);
    }

    /**
     * Gestion des changements de l'accéléromètre (inclinaison).
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isPaused || isGameOver) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleSensorChange(event);
        }
    }

    private void handleSensorChange(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > UPDATE_THRESHOLD) {
            lastUpdate = currentTime;

            float x = event.values[0]; // Inclinaison horizontale
            float y = event.values[1]; // Inclinaison verticale

            // Déterminer quelle inclinaison est la plus forte
            if (Math.abs(y) > Math.abs(x)) {
                // Axe vertical
                if (y < -1) {
                    moveSnake(0, -1, UP);    // Haut
                } else if (y > 1) {
                    moveSnake(0, 1, DOWN);  // Bas
                }
            } else {
                // Axe horizontal
                if (x < -1) {
                    moveSnake(-1, 0, LEFT);  // Gauche
                } else if (x > 1) {
                    moveSnake(1, 0, RIGHT);  // Droite
                }
            }
        }
    }

    /**
     * Déplace le serpent (deltaRow, deltaColumn) et change l'image de la tête selon la direction.
     */
    private void moveSnake(int deltaRow, int deltaColumn, int newDirection) {
        if (isGameOver || isPaused) {
            return;
        }

        // Empêcher de faire demi-tour
        if ((currentDirection == UP && newDirection == DOWN) ||
                (currentDirection == DOWN && newDirection == UP) ||
                (currentDirection == LEFT && newDirection == RIGHT) ||
                (currentDirection == RIGHT && newDirection == LEFT)) {
            return;
        }

        currentDirection = newDirection;

        // ---- CHANGER L'IMAGE SELON LA NOUVELLE DIRECTION ----
        switch (currentDirection) {
            case UP:
                snakeHeadView.setImageResource(R.drawable.snake_head_left);
                break;
            case RIGHT:
                snakeHeadView.setImageResource(R.drawable.snake_head_down);
                break;
            case DOWN:
                snakeHeadView.setImageResource(R.drawable.snake_head_right);
                break;
            case LEFT:
                snakeHeadView.setImageResource(R.drawable.snake_head_up);
                break;
        }

        // Mettre à jour la position
        snakeRow += deltaRow;
        snakeColumn += deltaColumn;

        // Limiter si vous ne voulez pas que le serpent traverse la grille
        if (snakeRow < 0) snakeRow = 0;
        if (snakeRow >= rowCount) snakeRow = rowCount - 1;
        if (snakeColumn < 0) snakeColumn = 0;
        if (snakeColumn >= columnCount) snakeColumn = columnCount - 1;

        // Mettre à jour l'affichage
        updateSnakePosition((GridLayout) findViewById(R.id.grid_layout));
    }

    /**
     * Met le jeu en pause.
     */
    private void pauseGame() {
        isPaused = true;
        pauseButton.setText("Reprendre");
        unregisterSensorListener();
    }

    /**
     * Reprend la partie.
     */
    private void resumeGame() {
        isPaused = false;
        pauseButton.setText("Pause");
        registerSensorListener();
    }

    /**
     * Redémarre la partie depuis zéro.
     */
    private void restartGame() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.removeAllViews(); // Effacer toutes les views (cellules, tête, corps, pomme...)

        isGameOver = false;
        isPaused = false;
        score = 0;
        scoreText.setText("Score : " + score);
        pauseButton.setText("Pause");

        // Reconstruire la grille
        initializeGridLayout();

        // Recréer le serpent
        initializeSnake();

        // Recréer la pomme
        initializeApple();

        // Relancer l'accéléromètre
        registerSensorListener();
    }

    /**
     * Quand on perd, on assombrit la grille et on affiche un message, puis on change le bouton en "Restart".
     */
    private void endGame() {
        isGameOver = true;

        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.setBackgroundColor(Color.argb(150, 0, 0, 0)); // Filtre gris

        TextView gameOverText = new TextView(this);
        gameOverText.setText("Vous avez perdu !");
        gameOverText.setTextSize(30);
        gameOverText.setTextColor(Color.WHITE);
        gameOverText.setBackgroundColor(Color.BLACK);
        gameOverText.setPadding(50, 50, 50, 50);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(10, 10, 10, 10);

        gameOverText.setLayoutParams(params);
        gridLayout.addView(gameOverText);

        pauseButton.setText("Restart");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé
    }
}
