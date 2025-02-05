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
 * Vue personnalisée (SnakeView) qui dessine un Snake sur un Canvas :
 *  - Affiche une grille (cases carrées).
 *  - Gère la position et le dessin de la pomme.
 *  - Gère la position et le dessin du serpent (tête + corps).
 *  - Gère la logique : score, collisions, game over, etc.
 *  - Autorise un demi-tour uniquement si le serpent n'a qu'un segment (sinon ignore la commande).
 */
public class SnakeView extends View {

    //----------------------------------------------------------------------------------------------
    // Constantes et champs de configuration
    //----------------------------------------------------------------------------------------------

    /** Nombre de lignes dans la grille. */
    private int rowCount = 11;
    /** Nombre de colonnes dans la grille. */
    private int columnCount = 11;

    /** Taille (en pixels) de chaque cellule. */
    private float cellSize;

    /** Décalage X/Y pour centrer la grille dans la vue. */
    private float offsetX;
    private float offsetY;

    /** Largeur/Hauteur totales de la grille (en pixels). */
    private float totalGridWidth;
    private float totalGridHeight;

    //----------------------------------------------------------------------------------------------
    // Objets de dessin (Paint, Bitmaps)
    //----------------------------------------------------------------------------------------------

    /** Peinture pour tracer les lignes de la grille. */
    private Paint paintGrid;

    /** Peinture pour remplir le corps du serpent. */
    private Paint paintBodyFill;

    /** Peinture pour tracer la bordure autour du corps. */
    private Paint paintBodyStroke;

    // Bitmaps pour la tête du serpent (4 directions).
    private Bitmap headUp, headDown, headLeft, headRight;
    // Bitmap pour la pomme.
    private Bitmap appleBitmap;

    //----------------------------------------------------------------------------------------------
    // État du jeu : serpent, pomme, score, etc.
    //----------------------------------------------------------------------------------------------

    /** Liste des coordonnées [row, col] du serpent. index=0 => tête. */
    private List<int[]> snakeCoordinates;

    /** Position de la pomme (row, col). */
    private int appleRow;
    private int appleCol;

    /** Score du joueur. */
    private int score = 0;

    /** Indique si la partie est terminée. */
    private boolean isGameOver = false;

    // Directions possibles
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    /** Direction actuelle du serpent (UP, DOWN, LEFT, RIGHT). */
    private int currentDirection = RIGHT;

    /** Générateur aléatoire (pour placer la pomme). */
    private Random random;

    //----------------------------------------------------------------------------------------------
    // Constructeurs
    //----------------------------------------------------------------------------------------------

    public SnakeView(Context context) {
        super(context);
        init();
    }

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    //----------------------------------------------------------------------------------------------
    // Méthodes d'initialisation et configuration
    //----------------------------------------------------------------------------------------------

    /**
     * Méthode d'initialisation, appelée par les constructeurs.
     */
    private void init() {
        // Pour l'aléatoire (pomme, etc.)
        random = new Random();

        // Prépare la liste des coordonnées du serpent.
        snakeCoordinates = new ArrayList<>();

        // Peinture de la grille (lignes grises).
        paintGrid = new Paint();
        paintGrid.setColor(Color.GRAY);
        paintGrid.setStrokeWidth(2f);

        // Peinture de remplissage pour le corps du serpent.
        paintBodyFill = new Paint();
        paintBodyFill.setColor(Color.rgb(160, 196, 50)); // Vert clair
        paintBodyFill.setStyle(Paint.Style.FILL);

        // Peinture de bordure pour le corps.
        paintBodyStroke = new Paint();
        paintBodyStroke.setColor(Color.BLACK); // Bordure noire
        paintBodyStroke.setStyle(Paint.Style.STROKE);
        paintBodyStroke.setStrokeWidth(2f);

        // Chargement des bitmaps pour la tête (4 directions).
        headUp = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_up);
        headDown = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_down);
        headLeft = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_left);
        headRight = BitmapFactory.decodeResource(getResources(), R.drawable.snake_head_right);

        // Chargement du bitmap pour la pomme.
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);
    }

    //----------------------------------------------------------------------------------------------
    // Gestion de la taille (pour calculer cellSize, offsetX, offsetY)
    //----------------------------------------------------------------------------------------------

    /**
     * Appelée lorsque la vue est mesurée : on calcule la taille des cellules carrées et on centre.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Taille brute : largeur / colonnes et hauteur / lignes
        float cellW = (float) w / columnCount;
        float cellH = (float) h / rowCount;

        // On veut des cases carrées => on prend la plus petite dimension
        cellSize = Math.min(cellW, cellH);

        // Largeur / hauteur totales de la grille
        totalGridWidth = cellSize * columnCount;
        totalGridHeight = cellSize * rowCount;

        // Décalage pour centrer la grille
        offsetX = (w - totalGridWidth) / 2f;
        offsetY = (h - totalGridHeight) / 2f;

        // Initialise (ou réinitialise) la position du serpent + pomme
        resetPositions();
    }

    /**
     * Réinitialise le serpent (tête au centre), le score, et place la pomme.
     */
    private void resetPositions() {
        snakeCoordinates.clear();
        isGameOver = false;
        score = 0;

        // Position de départ : milieu de la grille
        int startRow = rowCount / 2;
        int startCol = columnCount / 2;
        snakeCoordinates.add(new int[]{startRow, startCol});
        currentDirection = RIGHT; // Direction initiale

        // Place la pomme
        spawnApple();
    }

    //----------------------------------------------------------------------------------------------
    // Dessin principal
    //----------------------------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1) Fond noir
        canvas.drawColor(Color.BLACK);

        // 2) Dessiner la grille
        drawGrid(canvas);

        // 3) Vérifier si Game Over => dessiner message et stopper
        if (isGameOver) {
            drawGameOver(canvas);
            return;
        }

        // 4) Dessiner la pomme
        drawApple(canvas);

        // 5) Dessiner le serpent (tête + corps)
        drawSnake(canvas);
    }

    /**
     * Dessine la grille (lignes horizontales et verticales).
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
     * Dessine un voile semi-transparent et le texte "GAME OVER" au centre.
     */
    private void drawGameOver(Canvas canvas) {
        // Voile
        Paint p = new Paint();
        p.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(offsetX, offsetY, offsetX + totalGridWidth, offsetY + totalGridHeight, p);

        // Texte "GAME OVER!"
        p.setColor(Color.WHITE);
        p.setTextSize(70);

        float textX = offsetX + totalGridWidth / 4f;
        float textY = offsetY + totalGridHeight / 2f;
        canvas.drawText("GAME OVER!", textX, textY, p);
    }

    /**
     * Dessine la pomme à sa position (appleRow, appleCol).
     */
    private void drawApple(Canvas canvas) {
        float left = offsetX + appleCol * cellSize;
        float top  = offsetY + appleRow * cellSize;

        // Redimensionner le bitmap pour qu'il tienne pile dans la case
        Bitmap scaledApple = Bitmap.createScaledBitmap(
                appleBitmap,
                (int) cellSize,
                (int) cellSize,
                true
        );
        canvas.drawBitmap(scaledApple, left, top, null);
    }

    /**
     * Dessine la tête et le corps du serpent, avec un contour pour le corps.
     */
    private void drawSnake(Canvas canvas) {
        for (int i = 0; i < snakeCoordinates.size(); i++) {
            int[] segment = snakeCoordinates.get(i);
            int r = segment[0];
            int c = segment[1];

            // Calculer la position en pixels
            float left = offsetX + c * cellSize;
            float top  = offsetY + r * cellSize;

            if (i == 0) {
                // Tête
                Bitmap headToDraw = headRight; // Par défaut
                switch (currentDirection) {
                    case UP:    headToDraw = headLeft;   break;
                    case DOWN:  headToDraw = headRight;  break;
                    case LEFT:  headToDraw = headUp;     break;
                    case RIGHT: headToDraw = headDown;   break;
                }
                Bitmap scaledHead = Bitmap.createScaledBitmap(
                        headToDraw,
                        (int) cellSize,
                        (int) cellSize,
                        true
                );
                canvas.drawBitmap(scaledHead, left, top, null);
            } else {
                // Corps : remplissage + bordure
                canvas.drawRect(left, top, left + cellSize, top + cellSize, paintBodyFill);
                canvas.drawRect(left, top, left + cellSize, top + cellSize, paintBodyStroke);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Gestion de la pomme
    //----------------------------------------------------------------------------------------------

    /**
     * Sélectionne une nouvelle position libre pour la pomme.
     * Elle ne doit pas se trouver sur le serpent.
     */
    private void spawnApple() {
        List<int[]> freeCells = new ArrayList<>();

        // Parcourir toutes les cases de la grille
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                // Vérifier si la case est occupée par le serpent
                boolean isOccupied = false;
                for (int[] seg : snakeCoordinates) {
                    if (seg[0] == r && seg[1] == c) {
                        isOccupied = true;
                        break;
                    }
                }
                if (!isOccupied) {
                    freeCells.add(new int[]{r, c});
                }
            }
        }

        // Choisir au hasard dans la liste des cases libres
        if (!freeCells.isEmpty()) {
            int index = random.nextInt(freeCells.size());
            appleRow = freeCells.get(index)[0];
            appleCol = freeCells.get(index)[1];
        } else {
            // Le serpent occupe toute la grille => plus de place
            // (On pourrait déclarer un endGame, etc.)
        }
    }

    //----------------------------------------------------------------------------------------------
    // Getters et contrôle
    //----------------------------------------------------------------------------------------------

    /** Retourne le score actuel. */
    public int getScore() {
        return score;
    }

    /** Vrai si la partie est terminée. */
    public boolean isGameOver() {
        return isGameOver;
    }

    /**
     * Recommence le jeu depuis zéro (appelé par MainActivity quand on veut restart).
     */
    public void restartGame() {
        resetPositions();
        invalidate();
    }

    //----------------------------------------------------------------------------------------------
    // Mouvement du serpent (appelé depuis MainActivity, par ex. via l'accéléromètre)
    //----------------------------------------------------------------------------------------------

    /**
     * Déplace le serpent selon deltaRow/deltaCol, en tenant compte de la direction :
     * - Ignore le demi-tour si le serpent a plus d'un segment.
     * - Met à jour la position de la tête et décale le corps.
     * - Vérifie les collisions (corps, pomme).
     *
     * @param deltaRow -1 (haut), +1 (bas), 0 (pas de mouvement vertical)
     * @param deltaCol -1 (gauche), +1 (droite), 0 (pas de mouvement horizontal)
     * @param newDirection L'une des constantes UP, DOWN, LEFT, RIGHT
     */
    public void moveSnake(int deltaRow, int deltaCol, int newDirection) {
        // Si le jeu est déjà terminé, on ne fait rien
        if (isGameOver) return;

        // Vérifier si on veut faire un demi-tour et si le serpent a plus d'un segment
        if (snakeCoordinates.size() > 1) {
            if ((currentDirection == UP && newDirection == DOWN) ||
                    (currentDirection == DOWN && newDirection == UP) ||
                    (currentDirection == LEFT && newDirection == RIGHT) ||
                    (currentDirection == RIGHT && newDirection == LEFT)) {
                // On ignore la nouvelle direction => serpent continue tout droit
                return;
            }
        }

        // Met à jour la direction
        currentDirection = newDirection;

        // Récupérer la position de la tête
        int[] head = snakeCoordinates.get(0);
        int headRow = head[0];
        int headCol = head[1];

        // Calculer la nouvelle position de la tête
        headRow += deltaRow;
        headCol += deltaCol;

        // Limiter à la grille
        if (headRow < 0) headRow = 0;
        if (headRow >= rowCount) headRow = rowCount - 1;
        if (headCol < 0) headCol = 0;
        if (headCol >= columnCount) headCol = columnCount - 1;

        // Décaler le corps : chaque segment prend la position du précédent
        for (int i = snakeCoordinates.size() - 1; i > 0; i--) {
            snakeCoordinates.set(i, snakeCoordinates.get(i - 1));
        }
        // Placer la nouvelle tête en index 0
        snakeCoordinates.set(0, new int[]{headRow, headCol});

        // Vérifier la collision avec le corps (à partir de l'index 1)
        for (int i = 1; i < snakeCoordinates.size(); i++) {
            int[] seg = snakeCoordinates.get(i);
            if (seg[0] == headRow && seg[1] == headCol) {
                // Collision => Game Over
                isGameOver = true;
                invalidate();
                return;
            }
        }

        // Vérifier si on mange la pomme
        if (headRow == appleRow && headCol == appleCol) {
            // On agrandit le serpent (un segment supplémentaire)
            growSnake();
            // On replace la pomme
            spawnApple();
            // Incrémenter le score
            score++;
        }

        // Redessiner la vue
        invalidate();
    }

    /**
     * Ajoute un segment à la fin du serpent (copie la position du dernier).
     */
    private void growSnake() {
        int[] lastSegment = snakeCoordinates.get(snakeCoordinates.size() - 1);
        snakeCoordinates.add(new int[]{lastSegment[0], lastSegment[1]});
    }
}
