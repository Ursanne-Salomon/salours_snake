package com.example.snake;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int snakeRow = 0;
    private int snakeColumn = 0;
    private TextView snakeCell;
    private TextView redSquare; // Référence au carré rouge
    private int rowCount = 20;
    private int columnCount = 20;
    private int cellSize;

    private Handler handler = new Handler(); // Gestionnaire pour le délai
    private Random random = new Random(); // Générateur de positions aléatoires

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

        // Créer un carré rouge (initialement invisible)
        redSquare = new TextView(this);
        redSquare.setBackgroundColor(Color.RED);
        redSquare.setWidth(cellSize);
        redSquare.setHeight(cellSize);
        redSquare.setVisibility(TextView.INVISIBLE); // Caché au début

        gridLayout.addView(redSquare);

        // Lancer la boucle pour afficher les carrés rouges aléatoires
        startRedSquareLoop();
    }

    // Méthode pour gérer les carrés rouges
    private void startRedSquareLoop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Générer des positions aléatoires
                int randomRow = random.nextInt(rowCount);
                int randomColumn = random.nextInt(columnCount);

                // Mettre à jour la position du carré rouge
                GridLayout.LayoutParams redSquareParams = new GridLayout.LayoutParams();
                redSquareParams.rowSpec = GridLayout.spec(randomRow);
                redSquareParams.columnSpec = GridLayout.spec(randomColumn);
                redSquareParams.setMargins(1, 1, 1, 1);
                redSquare.setLayoutParams(redSquareParams);
                redSquare.setVisibility(TextView.VISIBLE);

                // Cacher le carré après 2 secondes
                handler.postDelayed(() -> redSquare.setVisibility(TextView.INVISIBLE), 2000);

                // Relancer la boucle pour afficher un nouveau carré rouge
                handler.postDelayed(this, 2000); // Redémarrer après 2 secondes
            }
        });
    }
}
