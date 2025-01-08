package com.example.snake;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupération du GridLayout
        GridLayout gridLayout = findViewById(R.id.grid_layout);

        // Nombre de colonnes et de lignes
        int rowCount = 16;
        int columnCount = 40;

        // Définir le nombre de colonnes et de lignes dans le GridLayout
        gridLayout.setRowCount(rowCount);
        gridLayout.setColumnCount(columnCount);

        // Récupération des dimensions de l'écran
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // Calcul de la taille des cellules en fonction de la hauteur et de la largeur
        int cellSize = Math.min(screenWidth / columnCount, screenHeight / rowCount);

        // Ajuster la taille totale de la grille pour éviter les débordements
        gridLayout.getLayoutParams().width = cellSize * columnCount;
        gridLayout.getLayoutParams().height = cellSize * rowCount;

        // Ajouter des cellules à la grille
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                // Créez une vue pour chaque cellule
                TextView cell = new TextView(this);
                cell.setBackgroundColor(Color.WHITE); // Couleur de base des cellules
                cell.setWidth(cellSize);
                cell.setHeight(cellSize);

                // Ajoutez la vue au GridLayout
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.setMargins(1, 1, 1, 1); // Marges pour espacement entre cellules
                cell.setLayoutParams(params);

                gridLayout.addView(cell);
            }
        }
    }
}
