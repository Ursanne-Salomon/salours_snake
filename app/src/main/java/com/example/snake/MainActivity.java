package com.example.snake;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int snakeRow = 0;
    private int snakeColumn = 0;
    private TextView snakeCell;
    private int rowCount = 20;
    private int columnCount = 20;
    private int cellSize;

    private long lastUpdate = 0;
    private static final int UPDATE_THRESHOLD = 100; // Temps minimal entre deux mises à jour (ms)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupération du GridLayout
        GridLayout gridLayout = findViewById(R.id.grid_layout);
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);

        // Calcul de la taille des cellules
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        cellSize = Math.min(screenWidth / columnCount, screenHeight / rowCount);

        // Ajuster les dimensions du GridLayout
        gridLayout.getLayoutParams().width = cellSize * columnCount;
        gridLayout.getLayoutParams().height = cellSize * rowCount;

        // Ajouter des cellules à la grille
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                TextView cell = new TextView(this);
                cell.setBackgroundColor(Color.LTGRAY);
                cell.setWidth(cellSize);
                cell.setHeight(cellSize);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);

                gridLayout.addView(cell);
            }
        }

        // Ajouter le carré représentant le snake
        snakeCell = new TextView(this);
        snakeCell.setBackgroundColor(Color.GREEN);
        snakeCell.setWidth(cellSize);
        snakeCell.setHeight(cellSize);

        GridLayout.LayoutParams snakeParams = new GridLayout.LayoutParams();
        snakeParams.rowSpec = GridLayout.spec(snakeRow);
        snakeParams.columnSpec = GridLayout.spec(snakeColumn);
        snakeParams.setMargins(1, 1, 1, 1);
        snakeCell.setLayoutParams(snakeParams);

        gridLayout.addView(snakeCell);

        // Initialisation du capteur
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastUpdate) > UPDATE_THRESHOLD) {
                lastUpdate = currentTime;

                float x = event.values[0]; // Déplacement horizontal
                float y = event.values[1]; // Déplacement vertical

                // Inversion des axes
                // x devient vertical et y devient horizontal
                if (Math.abs(y) > Math.abs(x)) {
                    if (y < -1) {
                        moveSnake(0, -1); // Gauche
                    } else if (y > 1) {
                        moveSnake(0, 1); // Droite
                    }
                } else {
                    if (x < -1) {
                        moveSnake(-1, 0); // Haut
                    } else if (x > 1) {
                        moveSnake(1, 0); // Bas
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé
    }

    private void moveSnake(int deltaRow, int deltaColumn) {
        // Mettre à jour la position
        snakeRow += deltaRow;
        snakeColumn += deltaColumn;

        // Vérifier les limites de la grille
        if (snakeRow < 0) snakeRow = 0;
        if (snakeRow >= rowCount) snakeRow = rowCount - 1;
        if (snakeColumn < 0) snakeColumn = 0;
        if (snakeColumn >= columnCount) snakeColumn = columnCount - 1;

        // Mettre à jour la position graphique
        GridLayout.LayoutParams snakeParams = new GridLayout.LayoutParams();
        snakeParams.rowSpec = GridLayout.spec(snakeRow);
        snakeParams.columnSpec = GridLayout.spec(snakeColumn);
        snakeParams.setMargins(1, 1, 1, 1);
        snakeCell.setLayoutParams(snakeParams);
    }
}
