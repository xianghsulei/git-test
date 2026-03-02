package com.atguigu.git;


    // SnakeGame.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.Random;

public class Demo {


    public static class SnakeGame extends JFrame {
        // 游戏配置：严格匹配需求 20行×30列有效区域
        public static final int ROWS = 22;    // 总行：0-21（0/21为墙）
        public static final int COLS = 32;    // 总列：0-31（0/31为墙）
        public static final int CELL_SIZE = 20; // 单元格大小
        public static final int INIT_SPEED = 200; // 初始速度200ms
        public static final int MIN_SPEED = 100;  // 最低速度100ms

        // 游戏状态
        private GamePanel gamePanel;
        private Timer gameTimer;
        private int speed = INIT_SPEED;
        private int score = 0;
        private long startTime;
        private boolean isGameOver = false;
        private boolean isWin = false;

        // 优化：道具系统
        private boolean doubleScore = false; // 双倍得分道具
        private long doubleScoreEndTime = 0;
        private int slowDownCount = 0; // 减速道具计数

        public SnakeGame() {
            initWindow();
            initGame();
        }

        // 初始化窗口
        private void initWindow() {
            setTitle("贪吃蛇 - 20×30标准版");
            setSize(COLS * CELL_SIZE + 20, ROWS * CELL_SIZE + 80);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setResizable(false);

            // 重新开始按钮
            JButton restartBtn = new JButton("重新开始");
            restartBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            restartBtn.addActionListener(e -> restartGame());
            add(restartBtn, BorderLayout.SOUTH);
        }

        // 初始化游戏数据
        private void initGame() {
            gamePanel = new GamePanel();
            add(gamePanel, BorderLayout.CENTER);
            speed = INIT_SPEED;
            score = 0;
            isGameOver = false;
            isWin = false;
            doubleScore = false;
            startTime = System.currentTimeMillis();

            // 游戏定时器：自动移动
            gameTimer = new Timer(speed, new GameLoop());
            gameTimer.start();

            // 键盘监听
            addKeyListener(new KeyControl());
            setFocusable(true);
        }

        // 重新开始游戏
        private void restartGame() {
            remove(gamePanel);
            initGame();
            validate();
            repaint();
        }

        // 游戏主循环：核心移动逻辑
        private class GameLoop implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isGameOver || isWin) return;
                gamePanel.moveSnake();
                gamePanel.repaint();
                // 检查道具时效
                checkPropTime();
            }
        }

        // 检查道具持续时间
        private void checkPropTime() {
            if (doubleScore && System.currentTimeMillis() > doubleScoreEndTime) {
                doubleScore = false;
            }
            if (slowDownCount > 0) {
                slowDownCount--;
                if (slowDownCount == 0) {
                    speed = Math.max(MIN_SPEED, speed - 20);
                }
            }
        }

        // 键盘控制
        private class KeyControl extends KeyAdapter {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGameOver || isWin) return;
                Direction currentDir = gamePanel.currentDirection;
                Direction newDir = currentDir;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: newDir = Direction.UP; break;
                    case KeyEvent.VK_DOWN: newDir = Direction.DOWN; break;
                    case KeyEvent.VK_LEFT: newDir = Direction.LEFT; break;
                    case KeyEvent.VK_RIGHT: newDir = Direction.RIGHT; break;
                }

                // 禁止180°转向
                if ((currentDir == Direction.UP && newDir == Direction.DOWN) ||
                        (currentDir == Direction.DOWN && newDir == Direction.UP) ||
                        (currentDir == Direction.LEFT && newDir == Direction.RIGHT) ||
                        (currentDir == Direction.RIGHT && newDir == Direction.LEFT)) {
                    return;
                }
                gamePanel.currentDirection = newDir;
            }
        }

        // 游戏绘制面板
        private class GamePanel extends JPanel {
            LinkedList<Point> snake; // 蛇身体：第一个是头
            Direction currentDirection;
            Point food;
            Point propSlow; // 减速道具
            Point propDouble; // 双倍得分道具
            Random random = new Random();

            public GamePanel() {
                // 初始化蛇：头(10,16) 身体(10,15)(10,14) 方向向右
                snake = new LinkedList<>();
                snake.add(new Point(16, 10));
                snake.add(new Point(15, 10));
                snake.add(new Point(14, 10));
                currentDirection = Direction.RIGHT;
                // 生成食物和道具
                generateFood();
                generateProp();
            }

            // 生成食物：重试5次，失败则通关
            private void generateFood() {
                int retry = 0;
                while (retry < 5) {
                    int x = random.nextInt(COLS - 2) + 1; // 1-30
                    int y = random.nextInt(ROWS - 2) + 1; // 1-20
                    Point newFood = new Point(x, y);
                    if (!snake.contains(newFood)) {
                        food = newFood;
                        return;
                    }
                    retry++;
                }
                // 重试5次失败：地图满，游戏通关
                isWin = true;
                gameTimer.stop();
            }

            // 优化：生成道具
            private void generateProp() {
                // 减速道具（蓝色）30%概率
                if (random.nextDouble() < 0.3) {
                    int x = random.nextInt(COLS - 2) + 1;
                    int y = random.nextInt(ROWS - 2) + 1;
                    propSlow = new Point(x, y);
                } else {
                    propSlow = null;
                }
                // 双倍得分道具（黄色）20%概率
                if (random.nextDouble() < 0.2) {
                    int x = random.nextInt(COLS - 2) + 1;
                    int y = random.nextInt(ROWS - 2) + 1;
                    propDouble = new Point(x, y);
                } else {
                    propDouble = null;
                }
            }

            // 蛇移动：核心逻辑（严格遵循碰撞时序）
            public void moveSnake() {
                Point head = snake.getFirst();
                Point newHead = new Point(head);

                // 计算新蛇头
                switch (currentDirection) {
                    case UP: newHead.y--; break;
                    case DOWN: newHead.y++; break;
                    case LEFT: newHead.x--; break;
                    case RIGHT: newHead.x++; break;
                }

                // 1. 检测吃到减速道具
                if (propSlow != null && newHead.equals(propSlow)) {
                    speed = Math.min(INIT_SPEED, speed + 20);
                    gameTimer.setDelay(speed);
                    slowDownCount = 25; // 持续5秒（200ms*25）
                    propSlow = null;
                }

                // 2. 检测吃到双倍得分道具
                if (propDouble != null && newHead.equals(propDouble)) {
                    doubleScore = true;
                    doubleScoreEndTime = System.currentTimeMillis() + 5000; // 持续5秒
                    propDouble = null;
                }

                // 3. 检测吃到食物
                if (newHead.equals(food)) {
                    // 计分：双倍道具生效
                    score += doubleScore ? 20 : 10;
                    // 加速：最低100ms
                    speed = Math.max(MIN_SPEED, speed - 10);
                    gameTimer.setDelay(speed);
                    // 生成新食物+道具
                    generateFood();
                    generateProp();
                    // 吃到食物：不删除尾部，长度+1
                    snake.addFirst(newHead);
                } else {
                    // 未吃食物：移动+删尾
                    snake.addFirst(newHead);
                    snake.removeLast();

                    // 4. 碰撞检测：墙壁/自身
                    if (isCollide(newHead)) {
                        isGameOver = true;
                        gameTimer.stop();
                    }
                }
            }

            // 碰撞检测
            private boolean isCollide(Point head) {
                // 撞墙：x=0/31 或 y=0/21
                if (head.x <= 0 || head.x >= COLS - 1 || head.y <= 0 || head.y >= ROWS - 1) {
                    return true;
                }
                // 撞自身
                for (int i = 1; i < snake.size(); i++) {
                    if (head.equals(snake.get(i))) {
                        return true;
                    }
                }
                return false;
            }

            // 绘制游戏
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制边界墙壁 *
                g.setColor(Color.BLACK);
                g.setFont(new Font("Consolas", Font.BOLD, 20));
                // 上下墙
                for (int x = 0; x < COLS; x++) {
                    drawString(g, "*", x, 0);
                    drawString(g, "*", x, ROWS - 1);
                }
                // 左右墙
                for (int y = 0; y < ROWS; y++) {
                    drawString(g, "*", 0, y);
                    drawString(g, "*", COLS - 1, y);
                }

                // 绘制蛇 #
                g.setColor(Color.BLUE);
                for (Point p : snake) {
                    drawString(g, "#", p.x, p.y);
                }

                // 绘制食物（红色圆）
                g.setColor(Color.RED);
                g.fillOval(food.x * CELL_SIZE + 2, food.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);

                // 绘制道具：减速(蓝色方块)、双倍得分(黄色星星)
                if (propSlow != null) {
                    g.setColor(Color.CYAN);
                    g.fillRect(propSlow.x * CELL_SIZE + 2, propSlow.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                }
                if (propDouble != null) {
                    g.setColor(Color.YELLOW);
                    g.fillRect(propDouble.x * CELL_SIZE + 2, propDouble.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
                }

                // 绘制文字信息
                g.setColor(Color.BLACK);
                g.setFont(new Font("微软雅黑", Font.PLAIN, 16));
                g.drawString("得分：" + score, 10, 20);
                g.drawString("速度：" + speed + "ms", 120, 20);
                if (doubleScore) g.drawString("双倍得分！", 240, 20);

                // 游戏结束/通关提示
                if (isGameOver || isWin) {
                    long totalTime = (System.currentTimeMillis() - startTime) / 1000;
                    String tip = isWin ? "恭喜通关！地图已满" : "游戏结束！";
                    g.setFont(new Font("微软雅黑", Font.BOLD, 24));
                    g.setColor(Color.RED);
                    g.drawString(tip, COLS * CELL_SIZE / 2 - 80, ROWS * CELL_SIZE / 2 - 20);
                    g.drawString("最终得分：" + score + "  时长：" + totalTime + "秒",
                            COLS * CELL_SIZE / 2 - 140, ROWS * CELL_SIZE / 2 + 20);
                }
            }

            // 工具：绘制单元格字符
            private void drawString(Graphics g, String text, int x, int y) {
                g.drawString(text, x * CELL_SIZE + 2, y * CELL_SIZE + CELL_SIZE - 2);
            }
        }

        // 启动游戏
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> new SnakeGame().setVisible(true));
        }
    }
    // Direction.java
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
