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

    //----------------------------------------------------------------------------------------------
    // Constantes et champs
    //----------------------------------------------------------------------------------------------

    /** Vue personnalisée où est dessiné le Snake (classe SnakeView). */
    private SnakeView snakeView;

    /** Gestion du capteur d'accéléromètre. */
    private SensorManager sensorManager;
    private Sensor accelerometer;

    /** État de pause et de fin de jeu. */
    private boolean isPaused = false;
    private boolean isGameOver = false;

    /** Bouton Pause/Restart et affichage du score. */
    private Button pauseButton;
    private TextView scoreText;

    /** Fréquence minimale de mise à jour via l'accéléromètre (en ms). */
    private long lastUpdate = 0;
    private static final int UPDATE_THRESHOLD = 100;

    /** Directions (identiques à celles définies dans SnakeView). */
    private static final int UP = SnakeView.UP;
    private static final int DOWN = SnakeView.DOWN;
    private static final int LEFT = SnakeView.LEFT;
    private static final int RIGHT = SnakeView.RIGHT;

    //----------------------------------------------------------------------------------------------
    // Cycle de vie de l'Activity
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Charge le layout XML qui contient notre SnakeView + le TextView (score) + le bouton Pause
        setContentView(R.layout.activity_main);

        // 1) Récupération des références aux vues
        snakeView = findViewById(R.id.snake_view);
        scoreText = findViewById(R.id.score_text);
        pauseButton = findViewById(R.id.pause_button);

        // 2) Configuration du bouton Pause/Restart
        pauseButton.setOnClickListener(v -> {
            if (isGameOver) {
                // Si le jeu est déjà terminé, on relance la partie
                snakeView.restartGame();
                isGameOver = false;
                isPaused = false;
                pauseButton.setText("Pause");
                updateScoreText(); // Remet "Score : 0"
                return;
            }

            // Sinon, on bascule entre Pause et Reprise
            if (!isPaused) {
                pauseGame();
            } else {
                resumeGame();
            }
        });

        // 3) Initialisation de l'accéléromètre
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Appelé lorsque l'Activity devient visible (premier plan) :
     * on enregistre le listener pour l'accéléromètre.
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListener();
    }

    /**
     * Appelé lorsque l'Activity passe en pause (par exemple, écran éteint) :
     * on désactive l'écoute de l'accéléromètre.
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListener();
    }

    //----------------------------------------------------------------------------------------------
    // Gestion du Sensor (accéléromètre)
    //----------------------------------------------------------------------------------------------

    /**
     * Enregistre le listener de l'accéléromètre si le capteur est disponible.
     */
    private void registerSensorListener() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Désenregistre le listener de l'accéléromètre.
     */
    private void unregisterSensorListener() {
        sensorManager.unregisterListener(this);
    }

    //----------------------------------------------------------------------------------------------
    // Méthodes pause/reprise
    //----------------------------------------------------------------------------------------------

    /**
     * Met en pause le jeu : arrête l'accéléromètre et change le texte du bouton.
     */
    private void pauseGame() {
        isPaused = true;
        pauseButton.setText("Reprendre");
        unregisterSensorListener();
    }

    /**
     * Reprend la partie : réactive l'accéléromètre et change le texte du bouton.
     */
    private void resumeGame() {
        isPaused = false;
        pauseButton.setText("Pause");
        registerSensorListener();
    }

    //----------------------------------------------------------------------------------------------
    // SensorEventListener : gestion des changements de l'accéléromètre
    //----------------------------------------------------------------------------------------------

    /**
     * Callback lorsque le capteur envoie de nouvelles valeurs (accéléromètre).
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Si le jeu est en pause ou déjà perdu, on n'agit pas
        if (isPaused || snakeView.isGameOver()) {
            return;
        }

        // Vérifier qu'on reçoit bien l'accéléromètre
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleSensorChange(event);
        }

        // Mettre à jour l'affichage du score après le mouvement
        updateScoreText();

        // Vérifier si le jeu vient de se terminer (collision => game over)
        if (snakeView.isGameOver()) {
            isGameOver = true;
            pauseButton.setText("Restart");
        }
    }

    /**
     * Gère la logique pour l'accéléromètre : détermine si on va haut/bas/gauche/droite.
     */
    private void handleSensorChange(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        // Empêche de déclencher trop souvent (limite à UPDATE_THRESHOLD ms)
        if ((currentTime - lastUpdate) > UPDATE_THRESHOLD) {
            lastUpdate = currentTime;

            float x = event.values[0]; // Inclinaison horizontale
            float y = event.values[1]; // Inclinaison verticale

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
     * Callback non utilisé ici (SensorEventListener).
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Rien de spécial
    }

    //----------------------------------------------------------------------------------------------
    // Méthodes utilitaires
    //----------------------------------------------------------------------------------------------

    /**
     * Met à jour le TextView pour afficher "Score : X".
     */
    private void updateScoreText() {
        int currentScore = snakeView.getScore();
        scoreText.setText("Score : " + currentScore);
    }
}