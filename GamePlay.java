import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class GamePlay extends JFrame implements KeyListener {
    // Game constants
    private static final int WIDTH = 400, HEIGHT = 300;
    private static final int PLAYER_WIDTH = 80, PLAYER_HEIGHT = 80;
    private static final int OBSTACLE_WIDTH = 50, OBSTACLE_HEIGHT = 50;
    private static final int POWERUP_SIZE = 40;
    private static final int PLAYER_SPEED = 5, JUMP_HEIGHT = 150, GRAVITY = 7;
    private static final int GAME_DURATION = 7; // 7 seconds per level

    // Game variables
    private int score = 0, health = 100;
    private int playerX = 100, playerY = HEIGHT - PLAYER_HEIGHT - 50;
    private boolean isJumping = false, isShieldActive = false, isGameOver = false;
    private int shieldCooldown = 0, timeLeft = GAME_DURATION, level = 1;
    private int obstacleSpeed = 4;

    // Game objects
    private List<Rectangle> obstacles = new ArrayList<>();
    private List<PowerUp> powerups = new ArrayList<>();
    private List<Point> stars = new ArrayList<>();

    // Assets
    private BufferedImage playerImg, goldDiamondImg, purpleDiamondImg, obstacleImg;
    private Clip jumpSound, collectSound, deathSound;
    private javax.swing.Timer gameTimer, clockTimer;

    // PowerUp class to track type (gold/purple)
    private class PowerUp extends Rectangle {
        boolean isGold;
        PowerUp(int x, int y, boolean isGold) {
            super(x, y, POWERUP_SIZE, POWERUP_SIZE);
            this.isGold = isGold;
        }
    }

    public GamePlay() {
        setTitle("Magical Unicorn Adventure");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        loadAssets();
        setupUI();
        startGame();
    }

    private void loadAssets() {
        try {
            playerImg = ImageIO.read(new File("UnicornPic.png"));
            goldDiamondImg = ImageIO.read(new File("GoldDiamond.png"));
            purpleDiamondImg = ImageIO.read(new File("PurpleDiamond.png"));
            obstacleImg = ImageIO.read(new File("AngryCloud.png"));

            jumpSound = AudioSystem.getClip();
            jumpSound.open(AudioSystem.getAudioInputStream(new File("Jump_sound.wav")));
            collectSound = AudioSystem.getClip();
            collectSound.open(AudioSystem.getAudioInputStream(new File("Collect_sound.wav")));
            deathSound = AudioSystem.getClip();
            deathSound.open(AudioSystem.getAudioInputStream(new File("Death_sound.wav")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };

        gamePanel.setLayout(null);
        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);
        stars = generateStars(200);
    }

    private void startGame() {
        // Game loop timer (60fps)
        gameTimer = new javax.swing.Timer(16, e -> {
            if (!isGameOver) {
                updateGame();
                repaint();
            }
        });

        // Countdown timer (1 second intervals)
        clockTimer = new javax.swing.Timer(1000, e -> {
            if (!isGameOver && --timeLeft <= 0) {
                levelUp();
            }
        });

        gameTimer.start();
        clockTimer.start();
    }

    private void drawGame(Graphics g) {
        // Draw sky background
        g.setColor(new Color(173, 198, 209));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw ground
        g.setColor(new Color(244, 195, 255));
        g.fillRect(0, HEIGHT - 50, WIDTH, 50);

        // Draw stars
        for (Point star : stars) {
            g.setColor(new Color(255, 255, 255, new Random().nextInt(155) + 100));
            g.fillOval(star.x, star.y, 5, 3);
        }

        // Draw powerups (gold and purple diamonds)
        for (PowerUp powerup : powerups) {
            BufferedImage img = powerup.isGold ? goldDiamondImg : purpleDiamondImg;
            g.drawImage(img, powerup.x, powerup.y, POWERUP_SIZE, POWERUP_SIZE, null);
        }

        // Draw obstacles
        for (Rectangle obstacle : obstacles) {
            g.drawImage(obstacleImg, obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
        }

        // Draw player
        g.drawImage(playerImg, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        // Draw shield if active
        if (isShieldActive) {
            g.setColor(new Color(210, 188, 246, 70));
            g.fillOval(playerX - 15, playerY - 15, PLAYER_WIDTH + 30, PLAYER_HEIGHT + 30);
        }

        // Draw UI elements
        drawUI(g);

        // Game over screen
        if (isGameOver) {
            drawGameOver(g);
        }
    }

    private void drawUI(Graphics g) {
        // Health bar
        g.setColor(Color.BLUE);
        g.fillRect(20, 20, 200, 20);
        g.setColor(Color.PINK);
        g.fillRect(20, 20, (int)(health * 2), 20);
        g.setColor(Color.BLACK);
        g.drawRect(20, 20, 200, 20);

        // Score and info
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 20, 60);
        g.drawString("Level: " + level, 20, 90);
        g.drawString("Time: " + timeLeft, WIDTH - 150, 30);

        // Shield status
        g.setColor(isShieldActive ? Color.CYAN : Color.GRAY);
        g.drawString("Shield: " + (isShieldActive ? "ON" : "OFF"), WIDTH - 150, 60);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(173, 198, 209, 255));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("GAME OVER", WIDTH/2 - 140, HEIGHT/2 - 50);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("Final Score: " + score, WIDTH/2 - 80, HEIGHT/2);
        g.drawString("Level Reached: " + level, WIDTH/2 - 80, HEIGHT/2 + 30);
        g.drawString("Press R to restart", WIDTH/2 - 100, HEIGHT/2 + 70);
    }

    private void updateGame() {
        // Jump physics
        if (isJumping) {
            playerY -= GRAVITY;
            if (playerY <= HEIGHT - PLAYER_HEIGHT - 50 - JUMP_HEIGHT) {
                isJumping = false;
            }
        } else if (playerY < HEIGHT - PLAYER_HEIGHT - 50) {
            playerY += GRAVITY;
        }

        // Move obstacles left
        for (Iterator<Rectangle> it = obstacles.iterator(); it.hasNext();) {
            Rectangle obstacle = it.next();
            obstacle.x -= obstacleSpeed;
            if (obstacle.x + OBSTACLE_WIDTH < 0) it.remove();
        }

        // Move powerups left
        for (Iterator<PowerUp> it = powerups.iterator(); it.hasNext();) {
            PowerUp powerup = it.next();
            powerup.x -= obstacleSpeed;
            if (powerup.x + POWERUP_SIZE < 0) it.remove();
        }

        // Generate obstacles and powerups
        spawnObjects();

        // Shield cooldown
        updateShield();

        // Check collisions
        checkCollisions();
    }

    private void spawnObjects() {
        // Spawn obstacles more frequently at higher levels
        if (Math.random() < 0.02 + (level * 0.005)) {
            obstacles.add(new Rectangle(WIDTH, HEIGHT - OBSTACLE_HEIGHT - 50,
                    OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        }

        // Spawn powerups (gold or purple)
        if (Math.random() < 0.01) {
            boolean isGold = new Random().nextBoolean();
            int yPos = HEIGHT - 100 - new Random().nextInt(200);
            powerups.add(new PowerUp(WIDTH, yPos, isGold));
        }
    }

    private void updateShield() {
        if (isShieldActive && ++shieldCooldown > 180) { // 3 seconds
            isShieldActive = false;
            shieldCooldown = 0;
        }
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Check powerup collection
        for (Iterator<PowerUp> it = powerups.iterator(); it.hasNext();) {
            PowerUp powerup = it.next();
            if (playerRect.intersects(powerup)) {
                it.remove();
                health = Math.min(100, health + (powerup.isGold ? 25 : 15));
                score += powerup.isGold ? 15 : 10;
                playSound(collectSound);
            }
        }

        // Check obstacle collision
        for (Iterator<Rectangle> it = obstacles.iterator(); it.hasNext();) {
            Rectangle obstacle = it.next();
            if (playerRect.intersects(obstacle)) {
                if (!isShieldActive) {
                    health -= 10;
                    if (health <= 0) gameOver();
                }
                it.remove();
                break;
            }
        }
    }

    private void levelUp() {
        level++;
        timeLeft = GAME_DURATION;
        obstacleSpeed += 1; // Increase speed each level

        // Increase spawn rates
        if (level % 3 == 0) {
            obstacleSpeed += 1; // Additional speed boost every 3 levels
        }
    }

    private void gameOver() {
        isGameOver = true;
        gameTimer.stop();
        clockTimer.stop();
        playSound(deathSound);
    }

    private void resetGame() {
        score = 0;
        health = 100;
        level = 1;
        timeLeft = GAME_DURATION;
        obstacleSpeed = 4;
        isGameOver = false;
        obstacles.clear();
        powerups.clear();
        playerY = HEIGHT - PLAYER_HEIGHT - 50;
        gameTimer.start();
        clockTimer.start();
    }

    private void playSound(Clip sound) {
        if (sound != null) {
            sound.setFramePosition(0);
            sound.start();
        }
    }

    private List<Point> generateStars(int count) {
        List<Point> stars = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            stars.add(new Point(rand.nextInt(WIDTH), rand.nextInt(HEIGHT - 100)));
        }
        return stars;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (isGameOver && e.getKeyCode() == KeyEvent.VK_R) {
            resetGame();
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_SPACE:
                if (playerY >= HEIGHT - PLAYER_HEIGHT - 50) {
                    isJumping = true;
                    playSound(jumpSound);
                }
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                if (!isShieldActive && shieldCooldown == 0) {
                    isShieldActive = true;
                }
                break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GamePlay().setVisible(true));
    }
}