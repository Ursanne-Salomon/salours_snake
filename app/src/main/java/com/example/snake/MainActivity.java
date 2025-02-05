package com.example.snake;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity qui gère :
 *  - Le sensor (accéléromètre) pour contrôler le serpent
 *  - Le score (via un TextView)
 *  - La pause / reprise / restart
 *  - Les interactions avec le SnakeView (dessin + logique du serpent)
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Référence vers notre vue personnalisée
    private SnakeView snakeView;

    // Accéléromètre
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // Pour gérer la pause et le game over
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // Bouton (Pause/Restart) et TextView (score)
    private Button pauseButton;
    private TextView scoreText;

    // Pour limiter la fréquence d’update via l’accéléromètre
    private long lastUpdate = 0;
    private static final int UPDATE_THRESHOLD = 100; // en ms

    // Directions (mêmes constantes que dans SnakeView)
    private static final int UP = SnakeView.UP;
    private static final int DOWN = SnakeView.DOWN;
    private static final int LEFT = SnakeView.LEFT;
    private static final int RIGHT = SnakeView.RIGHT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Charge le layout qui contient SnakeView + score + bouton
        setContentView(R.layout.activity_main);

        // Récupérer la vue personnalisée
        snakeView = findViewById(R.id.snake_view);

        // Récupérer le scoreText et le bouton
        scoreText = findViewById(R.id.score_text);
        pauseButton = findViewById(R.id.pause_button);

        // Gérer le clic du bouton
        pauseButton.setOnClickListener(v -> {
            if (isGameOver) {
                // On relance une partie
                snakeView.restartGame();
                isGameOver = false;
                isPaused = false;
                pauseButton.setText("Pause");
                updateScoreText(); // remet "Score : 0" par exemple
                return;
            }
            // Sinon, on alterne pause / reprise
            if (!isPaused) {
                pauseGame();
            } else {
                resumeGame();
            }
        });

        // Initialiser l’accéléromètre
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Quand l’Activity devient visible, on enregistre le listener du sensor.
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListener();
    }

    /**
     * Quand l’Activity est en pause (ex: écran éteint), on arrête le listener.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListener();
    }

    private void registerSensorListener() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterSensorListener() {
        sensorManager.unregisterListener(this);
    }

    /**
     * Met en pause le jeu : arrête l'accéléromètre et change le texte du bouton.
     */
    private void pauseGame() {
        isPaused = true;
        pauseButton.setText("Reprendre");
        unregisterSensorListener();
    }

    /**
     * Reprend la partie : relance l'accéléromètre.
     */
    private void resumeGame() {
        isPaused = false;
        pauseButton.setText("Pause");
        registerSensorListener();
    }

    /**
     * Détecte les changements du capteur d’accéléromètre
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Si le jeu est en pause ou déjà perdu, pas de mouvement
        if (isPaused || snakeView.isGameOver()) {
            return;
        }

        // On vérifie qu’on lit l’accéléromètre
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleSensorChange(event);
        }

        // Mettre à jour l’affichage du score
        updateScoreText();

        // Vérifier si le jeu vient de se terminer
        if (snakeView.isGameOver()) {
            isGameOver = true;
            pauseButton.setText("Restart");
        }
    }

    private void handleSensorChange(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > UPDATE_THRESHOLD) {
            lastUpdate = currentTime;

            float x = event.values[0];
            float y = event.values[1];

            // On priorise le mouvement vertical si |y| > |x|
            if (Math.abs(y) > Math.abs(x)) {
                // Mouvement vertical
                if (y < -1) {
                    // Haut
                    snakeView.moveSnake(0, -1, UP);
                } else if (y > 1) {
                    // Bas
                    snakeView.moveSnake(0, 1, DOWN);
                }
            } else {
                // Mouvement horizontal
                if (x < -1) {
                    // Gauche
                    snakeView.moveSnake(-1, 0, LEFT);
                } else if (x > 1) {
                    // Droite
                    snakeView.moveSnake(1, 0, RIGHT);
                }
            }
        }
    }

    /**
     * Met à jour le TextView "Score : X"
     */
    private void updateScoreText() {
        int currentScore = snakeView.getScore();
        scoreText.setText("Score : " + currentScore);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé
    }
}
