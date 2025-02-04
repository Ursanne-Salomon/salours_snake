package com.example.snake;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Directions possibles
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int snakeRow = 0;


    private int snakeColumn = 0;
    private int currentDirection = RIGHT; // Direction initiale du Snake
    private List<int[]> snakeBody; // Liste des segments du corps du snake
    private List<TextView> snakeViews; // Liste des TextViews correspondant au corps
    private int rowCount = 20;
    private int columnCount = 20;
    private int cellSize;

    private int appleRow = -1; // Position de la pomme
    private int appleColumn = -1;
    private TextView appleCell;

    private Random random;

    private long lastUpdate = 0;
    private static final int UPDATE_THRESHOLD = 100; // Temps minimal entre deux mises à jour (ms)

    private int score = 0;
    private TextView scoreText;

    private boolean isGameOver = false;

    private Button pauseButton;
    private boolean isPaused = false;    // Pour gérer l'état de pause

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        random = new Random();

        snakeBody = new ArrayList<>();
        snakeViews = new ArrayList<>();

        calculateGridSize();
        initializeGridLayout();
        initializeSnake();
        initializeApple();
        initializeSensors();
        scoreText = findViewById(R.id.score_text);
        scoreText.setText("Score : " + score);

        pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(v -> {
            // Si le jeu est déjà terminé, on peut relancer une nouvelle partie
            if (isGameOver) {
                restartGame();
                return;
            }

            // Sinon, on gère la pause
            if (!isPaused) {
                pauseGame();
            } else {
                resumeGame();
            }
        });
    }

    /**
     * Met à jour le score du joueur.
     *
     */
    private void updateScore() {
        score++; // Incrémentation du score uniquement ici
        scoreText.setText("Score : " + score);
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
     * Initialise la grille de jeu.
     */
    private void initializeGridLayout() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth / columnCount, screenHeight / rowCount);

        gridLayout.getLayoutParams().width = cellSize * columnCount;
        gridLayout.getLayoutParams().height = cellSize * rowCount;

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                addGridCell(gridLayout, i, j);
            }
        }
    }

    private void restartGame() {
        // 1) Réinitialiser la grille
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.removeAllViews(); // Efface tout ce qui avait été ajouté

        // 2) Remettre les paramètres du jeu à zéro
        snakeBody.clear();
        snakeViews.clear();
        isGameOver = false;
        isPaused = false;
        score = 0;
        scoreText.setText("Score : " + score);

        // 3) Reconstruire la grille et replacer le Snake et la pomme
        initializeGridLayout();
        initializeSnake();
        initializeApple();

        // 4) Réenregistrer le capteur
        registerSensorListener();

        // 5) Mettre à jour le texte du bouton pour que ce soit un bouton Pause
        pauseButton.setText("Pause");
    }


    /**
     * Ajoute une cellule à la grille.
     * @param gridLayout
     * @param row
     * @param column
     */
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
     * Initialise le snake en positionnant le premier segment.
     */
    private void initializeSnake() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        // Ajouter le premier segment du snake (tête)
        snakeRow = rowCount / 2;
        
        snakeColumn = columnCount / 2;
        snakeBody.add(new int[]{snakeRow, snakeColumn});

        TextView snakeHead = new TextView(this);
        snakeHead.setBackgroundColor(Color.GREEN);
        snakeHead.setWidth(cellSize);
        snakeHead.setHeight(cellSize);

        GridLayout.LayoutParams snakeParams = new GridLayout.LayoutParams();
        snakeParams.rowSpec = GridLayout.spec(snakeRow);
        snakeParams.columnSpec = GridLayout.spec(snakeColumn);
        snakeParams.setMargins(1, 1, 1, 1);
        snakeHead.setLayoutParams(snakeParams);

        snakeViews.add(snakeHead);
        gridLayout.addView(snakeHead);
    }

    /**
     * Met à jour la position du snake sur la grille.
     * @param gridLayout
     */
    private void updateSnakePosition(GridLayout gridLayout) {
        if (isGameOver) {
            return; // Arrêter le jeu si déjà perdu
        }

        // Supprimer l'affichage actuel du snake
        for (TextView segment : snakeViews) {
            gridLayout.removeView(segment);
        }

        // Vérifier si la tête touche le corps
        for (int i = 1; i < snakeBody.size(); i++) {
            if (snakeRow == snakeBody.get(i)[0] && snakeColumn == snakeBody.get(i)[1]) {
                endGame();
                return;
            }
        }

        // Mettre à jour les positions du corps
        for (int i = snakeBody.size() - 1; i > 0; i--) {
            snakeBody.set(i, snakeBody.get(i - 1));
        }

        // Déplacer la tête
        snakeBody.set(0, new int[]{snakeRow, snakeColumn});

        // Réafficher chaque segment
        snakeViews.clear();
        for (int[] segment : snakeBody) {
            TextView snakeSegment = new TextView(this);
            snakeSegment.setBackgroundColor(Color.GREEN);
            snakeSegment.setWidth(cellSize);
            snakeSegment.setHeight(cellSize);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.rowSpec = GridLayout.spec(segment[0]);
            params.columnSpec = GridLayout.spec(segment[1]);
            params.setMargins(1, 1, 1, 1);
            snakeSegment.setLayoutParams(params);

            snakeViews.add(snakeSegment);
            gridLayout.addView(snakeSegment);
        }

        // Vérifier si le snake a mangé une pomme
        if (snakeRow == appleRow && snakeColumn == appleColumn) {
            growSnake();
            spawnApple(gridLayout);
            updateScore();
        }
    }

    /**
     * Augmente la taille du snake en ajoutant un nouveau segment.
     */
    private void growSnake() {
        // Ajouter un nouveau segment à la position du dernier segment
        int[] lastSegment = snakeBody.get(snakeBody.size() - 1);
        snakeBody.add(new int[]{lastSegment[0], lastSegment[1]});
    }

    /**
     * Initialise la position de la pomme.
     */
    private void initializeApple() {
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        appleCell = new TextView(this);
        appleCell.setBackgroundColor(Color.RED);
        appleCell.setWidth(cellSize);
        appleCell.setHeight(cellSize);

        spawnApple(gridLayout);
    }

    /**
     * Génère une nouvelle position pour la pomme.
     * @param gridLayout
     */
    private void spawnApple(GridLayout gridLayout) {
        // Générer une nouvelle position pour la pomme
        appleRow = random.nextInt(rowCount);
        appleColumn = random.nextInt(columnCount);

        GridLayout.LayoutParams appleParams = new GridLayout.LayoutParams();
        appleParams.rowSpec = GridLayout.spec(appleRow);
        appleParams.columnSpec = GridLayout.spec(appleColumn);


        appleParams.setMargins(1, 1, 1, 1);
        appleCell.setLayoutParams(appleParams);

        gridLayout.removeView(appleCell);
        gridLayout.addView(appleCell);
    }

    /**
     * Initialise le capteur d'accéléromètre.
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

    /**
     * Enregistre le listener du capteur d'accéléromètre.
     */
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

    private void pauseGame() {
        isPaused = true;
        pauseButton.setText("Reprendre"); // Texte du bouton pendant la pause
        // Arrêter d'écouter le capteur
        unregisterSensorListener();
    }

    private void resumeGame() {
        isPaused = false;
        pauseButton.setText("Pause");
        // Reprendre l’écoute du capteur
        registerSensorListener();
    }


    /**
     * Enregistre le listener du capteur d'accéléromètre.
     */
    private void unregisterSensorListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isPaused || isGameOver) {
            return; // On ne fait rien si c'est en pause ou que le jeu est fini
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleSensorChange(event);
        }
    }


    /**
     * Gère les changements de valeurs du capteur d'accéléromètre.
     * @param event
     */
    private void handleSensorChange(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > UPDATE_THRESHOLD) {
            lastUpdate = currentTime;

            float x = event.values[0];
            float y = event.values[1];

            if (Math.abs(y) > Math.abs(x)) {
                if (y < -1) {
                    moveSnake(0, -1, UP);
                } else if (y > 1) {
                    moveSnake(0, 1, DOWN);
                }
            } else {
                if (x < -1) {
                    moveSnake(-1, 0, LEFT);
                } else if (x > 1) {
                    moveSnake(1, 0, RIGHT);
                }
            }
        }
    }

    /**
     * Déplace le snake dans la direction spécifiée.
     * @param deltaRow
     * @param deltaColumn
     * @param newDirection
     */
    private void moveSnake(int deltaRow, int deltaColumn, int newDirection) {
        if (isGameOver || isPaused) {
            return; // Bloquer les déplacements si le jeu est terminé ou en pause
        }

        if ((currentDirection == UP && newDirection == DOWN) ||
                (currentDirection == DOWN && newDirection == UP) ||
                (currentDirection == LEFT && newDirection == RIGHT) ||
                (currentDirection == RIGHT && newDirection == LEFT)) {
            return;
        }

        currentDirection = newDirection;

        snakeRow += deltaRow;
        snakeColumn += deltaColumn;

        if (snakeRow < 0) snakeRow = 0;
        if (snakeRow >= rowCount) snakeRow = rowCount - 1;
        if (snakeColumn < 0) snakeColumn = 0;
        if (snakeColumn >= columnCount) snakeColumn = columnCount - 1;

        updateSnakePosition((GridLayout) findViewById(R.id.grid_layout));
    }

    /**
     * Termine le jeu et affiche un message de fin.
     */
    private void endGame() {
        isGameOver = true;

        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.setBackgroundColor(Color.argb(150, 0, 0, 0));

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

        // Mettre le texte du bouton sur "Restart"
        pauseButton.setText("Restart");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas nécessaire
    }
}
