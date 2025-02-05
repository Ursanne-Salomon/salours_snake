package com.example.snake;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vue personnalisée qui dessine un Snake sur un Canvas :
 *  - Grille (cases carrées)
 *  - Pomme
 *  - Serpent (tête + corps)
 *  - Score, collisions, etc.
 */
public class SnakeView extends View {

    // Tailles de la grille (nombre de lignes/colonnes)
    private int rowCount = 20;
    private int columnCount = 20;

    // Taille en pixels de chaque cellule (carré)
    private float cellSize;

    // Décalage pour centrer la grille
    private float offsetX;
    private float offsetY;

    // Largeur/Hauteur totales de la grille en pixels
    private float totalGridWidth;
    private float totalGridHeight;

    // Peinture pour tracer la grille
    private Paint paintGrid;

    // Serpent : liste de coordonnées [row, col]
    private List<int[]> snakeCoordinates;

    // Pomme : position (row, col)
    private int appleRow;
    private int appleCol;

    // Score
    private int score = 0;

    // Direction : constants
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    private int currentDirection = RIGHT; // Par défaut

    // Bitmaps pour la tête du serpent
    private Bitmap headUp, headDown, headLeft, headRight;
    // Bitmap pour la pomme
    private Bitmap appleBitmap;

    // Peinture pour le corps
    private Paint paintBody;

    // Indicateur de fin de jeu
    private boolean isGameOver = false;

    // Générateur aléatoire
    private Random random;

    public SnakeView(Context context) {
        super(context);
        init();
    }

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Méthode d'initialisation appelée par les constructeurs
     */
    private void init() {
        random = new Random();

        // Serpent
        snakeCoordinates = new ArrayList<>();

        // Peinture de la grille
        paintGrid = new Paint();
        paintGrid.setColor(Color.GRAY);
        paintGrid.setStrokeWidth(2f);

        // Peinture du corps
        paintBody = new Paint();
        paintBody.setColor(Color.rgb(160, 196, 50));

        // Charger les bitmaps
        headUp = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_up);
        headDown = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_down);
        headLeft = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_left);
        headRight = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_right);

        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);
    }

    /**
     * Appelée quand la taille de la vue est connue : on calcule la taille des cellules
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // On veut des cases carrées => on prend la taille min
        float cellW = (float) w / columnCount;
        float cellH = (float) h / rowCount;
        cellSize = Math.min(cellW, cellH);

        // Calcul de la largeur/hauteur totales
        totalGridWidth = cellSize * columnCount;
        totalGridHeight = cellSize * rowCount;

        // Offsets pour centrer la grille
        offsetX = (w - totalGridWidth) / 2f;
        offsetY = (h - totalGridHeight) / 2f;

        // On place le serpent (tête au milieu) si ce n'est pas déjà fait
        resetPositions();
    }

    /**
     * Remet la tête au centre, met le score à 0, etc.
     */
    private void resetPositions() {
        snakeCoordinates.clear();
        isGameOver = false;
        score = 0;

        // Placer la tête au centre
        int startRow = rowCount / 2;
        int startCol = columnCount / 2;
        snakeCoordinates.add(new int[]{startRow, startCol});
        currentDirection = RIGHT; // direction par défaut

        // Placer la pomme
        spawnApple();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1) Fond noir
        canvas.drawColor(Color.BLACK);

        // 2) Dessiner la grille (lignes horizontales + verticales)
        drawGrid(canvas);

        // 3) Si le jeu est terminé, on affiche un voile + un texte
        if (isGameOver) {
            drawGameOver(canvas);
            return;
        }

        // 4) Dessiner la pomme
        drawApple(canvas);

        // 5) Dessiner le serpent : la tête + le corps
        drawSnake(canvas);
    }

    /**
     * Dessine la grille à l'écran (lignes grises)
     */
    private void drawGrid(Canvas canvas) {
        // Lignes horizontales
        for (int r = 0; r <= rowCount; r++) {
            float y = offsetY + r * cellSize;
            canvas.drawLine(offsetX, y, offsetX + totalGridWidth, y, paintGrid);
        }
        // Lignes verticales
        for (int c = 0; c <= columnCount; c++) {
            float x = offsetX + c * cellSize;
            canvas.drawLine(x, offsetY, x, offsetY + totalGridHeight, paintGrid);
        }
    }

    /**
     * Affiche un voile + message "Game Over"
     */
    private void drawGameOver(Canvas canvas) {
        // Voile semi-transparent
        Paint p = new Paint();
        p.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(offsetX, offsetY, offsetX + totalGridWidth, offsetY + totalGridHeight, p);

        p.setColor(Color.WHITE);
        p.setTextSize(70);
        // On centre le texte "GAME OVER" plus ou moins
        float textX = offsetX + totalGridWidth / 4f;
        float textY = offsetY + totalGridHeight / 2f;
        canvas.drawText("GAME OVER!", textX, textY, p);
    }

    /**
     * Dessine la pomme
     */
    private void drawApple(Canvas canvas) {
        // Coordonnées de la pomme
        float left = offsetX + appleCol * cellSize;
        float top = offsetY + appleRow * cellSize;

        // Redimensionner l'image de la pomme
        Bitmap scaledApple = Bitmap.createScaledBitmap(
                appleBitmap,
                (int) cellSize,
                (int) cellSize,
                true
        );
        canvas.drawBitmap(scaledApple, left, top, null);
    }

    /**
     * Dessine la tête et le corps du serpent
     */
    private void drawSnake(Canvas canvas) {
        for (int i = 0; i < snakeCoordinates.size(); i++) {
            int[] segment = snakeCoordinates.get(i);
            int r = segment[0];
            int c = segment[1];

            float left = offsetX + c * cellSize;
            float top = offsetY + r * cellSize;

            if (i == 0) {
                // TÊTE
                Bitmap headToDraw = headRight; // par défaut
                switch (currentDirection) {
                    case UP:    headToDraw = headLeft;    break;
                    case DOWN:  headToDraw = headRight;  break;
                    case LEFT:  headToDraw = headUp;  break;
                    case RIGHT: headToDraw = headDown; break;
                }
                Bitmap scaledHead = Bitmap.createScaledBitmap(headToDraw,
                        (int) cellSize, (int) cellSize, true);
                canvas.drawBitmap(scaledHead, left, top, null);

            } else {
                // CORPS : rectangle vert
                canvas.drawRect(
                        left,
                        top,
                        left + cellSize,
                        top + cellSize,
                        paintBody
                );
            }
        }
    }

    /**
     * Place la pomme à une position aléatoire
     */
    private void spawnApple() {
        appleRow = random.nextInt(rowCount);
        appleCol = random.nextInt(columnCount);
        // Facultatif : vérifier qu'on ne la place pas sur le serpent
        // ...
    }

    /**
     * Retourne le score actuel
     */
    public int getScore() {
        return score;
    }

    /**
     * Vrai si le jeu est terminé
     */
    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * Recommence le jeu depuis zéro
     */
    public void restartGame() {
        resetPositions();
        invalidate();
    }

    /**
     * Déplacer le serpent en tenant compte de la direction
     *
     * @param deltaRow      -1 pour haut, +1 pour bas
     * @param deltaCol      -1 pour gauche, +1 pour droite
     * @param newDirection  l'une des constantes UP, DOWN, LEFT, RIGHT
     */
    public void moveSnake(int deltaRow, int deltaCol, int newDirection) {
        if (isGameOver) return;

        // Empêcher le demi-tour
        if ((currentDirection == UP && newDirection == DOWN) ||
                (currentDirection == DOWN && newDirection == UP) ||
                (currentDirection == LEFT && newDirection == RIGHT) ||
                (currentDirection == RIGHT && newDirection == LEFT)) {
            // On ignore le changement
        } else {
            currentDirection = newDirection;
        }

        // Récupérer la position de la tête
        int[] head = snakeCoordinates.get(0);
        int headRow = head[0];
        int headCol = head[1];

        // Nouvelle position
        headRow += deltaRow;
        headCol += deltaCol;

        // Limiter le serpent à la grille
        if (headRow < 0) headRow = 0;
        if (headRow >= rowCount) headRow = rowCount - 1;
        if (headCol < 0) headCol = 0;
        if (headCol >= columnCount) headCol = columnCount - 1;

        // Décaler le corps
        for (int i = snakeCoordinates.size() - 1; i > 0; i--) {
            snakeCoordinates.set(i, snakeCoordinates.get(i - 1));
        }
        // Mettre à jour la tête
        snakeCoordinates.set(0, new int[]{headRow, headCol});

        // Vérifier collision avec le corps
        for (int i = 1; i < snakeCoordinates.size(); i++) {
            int[] seg = snakeCoordinates.get(i);
            if (seg[0] == headRow && seg[1] == headCol) {
                // game over
                isGameOver = true;
                invalidate();
                return;
            }
        }

        // Vérifier si on mange la pomme
        if (headRow == appleRow && headCol == appleCol) {
            // Agrandir
            growSnake();
            spawnApple();
            score++;
        }

        // Demander redessin
        invalidate();
    }

    /**
     * Agrandit le serpent de 1 segment à la fin
     */
    private void growSnake() {
        int[] lastSegment = snakeCoordinates.get(snakeCoordinates.size() - 1);
        // On ajoute un nouveau segment qui a la même position que le dernier
        snakeCoordinates.add(new int[]{lastSegment[0], lastSegment[1]});
    }
}

