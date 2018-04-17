import javafx.application.Application;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class GUI extends JFrame implements Observer {
    public static final int TILE_SIZE = 64;
    private SidebarGUI sidebar;
    private JPanel mapPanel;

    private TileButton[][] mapArray;
    private BufferedImage paperMonsterImage, rockMonsterImage, scissorMonsterImage;
    public BufferedImage rockTowerImage, scissorTowerImage, paperTowerImage;
    private static GUI instance;
    private ButtonListener buttonListener;

    public enum ToolType {
        BUILD, DESTROY, SELECT
    }

    public ToolType selectedTool = ToolType.SELECT;
    public TowerType selectedTowerType = TowerType.ROCK;

    public static GUI getInstance() {
        if (instance == null) {
            instance = new GUI();
        }
        return instance;
    }

    private GUI() {
        setName("Tower Defense");

        try {
            paperMonsterImage = ImageIO.read(new File("resources/monsters/paperMonster.png"));
            rockMonsterImage = ImageIO.read(new File("resources/monsters/rockMonster.png"));
            scissorMonsterImage = ImageIO.read(new File("resources/monsters/scissorMonster.png"));

            rockTowerImage = ImageIO.read(new File("resources/towers/rockTower.png"));
            paperTowerImage = ImageIO.read(new File("resources/towers/paperTower.png"));
            scissorTowerImage = ImageIO.read(new File("resources/towers/scissorTower.png"));
        } catch (IOException e) {
            System.out.println("Error reading images");
            return;
        }

        setLayout(new BorderLayout());
        Game.getInstance(); // initialize the Game
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        createMapPanel();
        add(mapPanel, BorderLayout.CENTER);
        setVisible(true);

        Game.getInstance().addObserver(this);
        sidebar = new SidebarGUI();
        add(sidebar, BorderLayout.EAST);
        Map map = Game.getInstance().getMap();
        setSize(sidebar.getWidth() + map.getWidth() * TILE_SIZE, 22 + map.getHeight() * TILE_SIZE);
    }

    private void createMapPanel() {
        Map map = Game.getInstance().getMap();
        mapPanel = new JPanel();
        mapPanel.setLayout(new GridLayout(map.getHeight(), map.getWidth(), 0, 0));
        mapArray = new TileButton[map.getHeight()][map.getWidth()];


        try {
            // create the images
            BufferedImage grass = ImageIO.read(new File("resources/tiles/grass.png"));
            BufferedImage vertical = ImageIO.read(new File("resources/tiles/vertical.png"));
            BufferedImage horizontal = ImageIO.read(new File("resources/tiles/horizontal.png"));
            BufferedImage rightToDown = ImageIO.read(new File("resources/tiles/rightToDown.png"));
            BufferedImage rightToUp = ImageIO.read(new File("resources/tiles/rightToUp.png"));
            BufferedImage downToRight = ImageIO.read(new File("resources/tiles/downToRight.png"));
            BufferedImage upToRight = ImageIO.read(new File("resources/tiles/upToRight.png"));

            for (int row = 0; row < map.getHeight(); row++) {
                for (int col = 0; col < map.getWidth(); col++) {
                    //create a new JPanel for each Tile
                    mapArray[row][col] = new TileButton();

                    switch (map.getTile(col, row).type) {
                        // add an image, depending on what the Tile's type is
                        case Map.R:
                            mapArray[row][col].tileImage = horizontal;
                            break;
                        case Map.U:
                        case Map.D:
                            mapArray[row][col].tileImage = vertical;
                            break;
                        case Map.UR:
                            mapArray[row][col].tileImage = upToRight;
                            break;
                        case Map.DR:
                            mapArray[row][col].tileImage = downToRight;
                            break;
                        case Map.RU:
                            mapArray[row][col].tileImage = rightToUp;
                            break;
                        case Map.RD:
                            mapArray[row][col].tileImage = rightToDown;
                            break;
                        default:
                            mapArray[row][col].tileImage = grass;
                            break;
                    }

                    mapPanel.add(mapArray[row][col]);
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred when loading the images");
        }
    }

    public void clearMonsterImages() {
        for (int row = 0; row < mapArray.length; row++) {
            for (int col = 0; col < mapArray[row].length; col++) {
                mapArray[row][col].monsterImage = null;
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        sidebar.updateGoldLabel();
        sidebar.updateRoundLabel();
        sidebar.updateHealthLabel();

        for (Monster m : Game.getInstance().getMap().getMonsters()) {
            int col = m.getCol();
            int row = m.getRow();
            mapArray[row][col].rotation = m.getRotation();
            switch (m.getType()) {
                case PAPER:
                    mapArray[row][col].monsterImage = paperMonsterImage;
                    break;
                case ROCK:
                    mapArray[row][col].monsterImage = rockMonsterImage;
                    break;
                case SCISSORS:
                    mapArray[row][col].monsterImage = scissorMonsterImage;
                    break;
            }
        }

        for (Tower t : Game.getInstance().getMap().getTowers()) {
            int col = t.getCol();
            int row = t.getRow();
            mapArray[row][col].rotation = t.getRotation();
            if (t.getFramesSinceLastAttack() <= 1) {
                mapArray[row][col].sizeDiff = 2;
            } else {
                mapArray[row][col].sizeDiff = 0;
                switch(t.getType()) {
                    case PAPER:
                        mapArray[row][col].towerImage = paperTowerImage;
                        break;
                    case ROCK:
                        mapArray[row][col].towerImage = rockTowerImage;
                        break;
                    case SCISSORS:
                        mapArray[row][col].towerImage = scissorTowerImage;
                        break;
                }
            }
        }

        repaint();
    }

    public class TileButton extends JButton {
        BufferedImage tileImage;
        Image monsterImage;
        Image towerImage;
        double rotation;
        int sizeDiff;

        TileButton() {
            super();
            setSize(GUI.TILE_SIZE, GUI.TILE_SIZE);
            sizeDiff = 0;
            if (buttonListener == null)
                buttonListener = new ButtonListener();
            addActionListener(buttonListener);
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g1 = (Graphics2D) g;
            g1.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g1.drawImage(tileImage, 0, 0, getWidth(), getHeight(), null);

            int tileImageWidth = tileImage.getWidth(this);
            int tileImageHeight = tileImage.getHeight(this);
            g1.rotate(rotation, getWidth()/2, getHeight()/2);

            double widthPercent = (double) getWidth() / (double) tileImageWidth;
            double heightPercent = (double) getHeight() / (double) tileImageHeight;
            if (monsterImage != null) {
                int monsterImageWidth = (int) (monsterImage.getWidth(this) * widthPercent);
                int monsterImageHeight = (int) (monsterImage.getHeight(this) * heightPercent);
                //@TODO: Null pointer here?
                g1.drawImage(monsterImage, (getWidth() / 2) - (monsterImageWidth / 2),
                        (getHeight() / 2) - (monsterImageHeight / 2),
                        monsterImageWidth,
                        monsterImageHeight, this);
            }
            if (towerImage != null) {
                int towerImageWidth = (int) (towerImage.getWidth(this) * widthPercent);
                int towerImageHeight = (int) (towerImage.getHeight(this) * widthPercent);
                g1.drawImage(towerImage,
                        (getWidth() / 2) - (towerImageWidth / 2),
                        (getHeight() / 2) - (towerImageHeight / 2),
                        towerImageWidth + sizeDiff,
                        towerImageHeight + sizeDiff, this);
            }
        }
    }

    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Map map = Game.getInstance().getMap();
            for (int row = 0; row < map.getHeight(); row++) {
                for (int col = 0; col < map.getWidth(); col++) {
                    if (e.getSource() == mapArray[row][col]) {

                        if (selectedTool == ToolType.BUILD) {
                            if (map.isBuildable(col, row)) {
                                switch (selectedTowerType) {
                                    case ROCK:
                                        if (RockTower.getCost() <= Game.getInstance().getGold()) {
                                            Game.getInstance().removeGold(RockTower.getCost());
                                            map.addTower(new RockTower(col, row), col, row);
                                            mapArray[row][col].towerImage = rockTowerImage;
                                        }
                                        break;
                                    case PAPER:
                                        if (PaperTower.getCost() <= Game.getInstance().getGold()) {
                                            Game.getInstance().removeGold(PaperTower.getCost());
                                            map.addTower(new PaperTower(col, row), col, row);
                                            mapArray[row][col].towerImage = paperTowerImage;
                                        }
                                        break;
                                    case SCISSORS:
                                        if (ScissorTower.getCost() <= Game.getInstance().getGold()) {
                                            Game.getInstance().removeGold(ScissorTower.getCost());
                                            map.addTower(new ScissorTower(col, row), col, row);
                                            mapArray[row][col].towerImage = scissorTowerImage;
                                        }
                                        break;
                                }
                            }
                        } else if (selectedTool == ToolType.DESTROY){
                            // is in DESTROY mode
                            if (map.getTower(col, row) != null) {
                                // a tower exists here, so destroy it
                                map.destroyTower(col, row);
                                mapArray[row][col].towerImage = null;
                            }
                        }
                    }
                }
            }
        }
    }

    public static class GameOverDialog extends JDialog implements ActionListener {
        JButton quitButton, newGameButton;

        public GameOverDialog() {
            setTitle("Game Over!");
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    Game.getInstance().reset();
                    dispose();
                }
            });
            setLayout(new FlowLayout());
            /* create the various elements */

            quitButton = new JButton("Quit");
            quitButton.addActionListener(this);

            newGameButton = new JButton("New Game");
            newGameButton.addActionListener(this);

            /* add the elements to the panel */
            add(BorderLayout.CENTER, new JLabel("You lost on round " + RoundManager.getRound() + "!"));

            add(BorderLayout.SOUTH, quitButton);
            add(BorderLayout.SOUTH, newGameButton);

            setVisible(true);
            pack();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == newGameButton) {
                Game.getInstance().reset();
                dispose();
            } else if (e.getSource() == quitButton) {
                System.exit(0);
            }
        }
    }

    public void reset() {
        remove(mapPanel);
        createMapPanel();
        add(mapPanel);
    }

    public static void main(String[] args) {
        GUI.getInstance();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread gameThread = new Thread(() -> Game.getInstance().start());
        gameThread.start();
        Application.launch(BackgroundMusic.class, "resources/music/FCMusic.mp3", "true", "344");
    }
}