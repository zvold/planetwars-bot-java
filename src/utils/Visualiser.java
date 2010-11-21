package utils;

import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import shared.DistanceCache;
import shared.Game;
import shared.Planet;
import shared.Race;

import compare.DistanceCloseness;
import compare.IScore;

public class Visualiser {
    
    public static final int SIZE = 640;
    public static final int STEP = 8;
    
    JFrame      _f;
    TextArea    _text;
    DrawArea    _area;
    Game        _game = new Game();
    
    IScore<Planet>  _scorer;
    float[][]       _score = new float[SIZE/STEP][SIZE/STEP];

    double _xMin, _yMin, _xMax, _yMax;
    int _sizes[]  = new int[] {10,13,18,21,23,29};
    
    public Visualiser() {
        _f = new JFrame("Have a nice day!");
        _f.setSize(1000, 700);
        
        _f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        BoxLayout l = new BoxLayout(_f.getContentPane(), BoxLayout.X_AXIS);
        _f.setLayout(l);
        
        _area = new DrawArea();
        _area.setMinimumSize(new Dimension(SIZE, SIZE));
        _area.setPreferredSize(new Dimension(640, 640));

        _text = new TextArea();
        
        _f.getContentPane().add(_area);
        _f.getContentPane().add(_text);
        
        _f.validate();
        _f.setVisible(true);
        
        _text.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {                                                                                                                 
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {                                                                                                       
                    String gameState = _text.getText();
                    Game game = new Game(gameState);
                    setGame(game);
                }                                                                                                                                                
            }
            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyReleased(KeyEvent e) {}
        });        
    }

    public void setGame(Game game) {
        _game = game;
        _scorer = new DistanceCloseness(_game.planets(ALLY));
        calcDimensions();
        calcScore();
        _area.repaint();
        
        List<Planet> list = new ArrayList<Planet>(_game.planets());
        Collections.sort(list, _scorer);
        printPlanetsScores(list, NEUTRAL, _scorer);
        printPlanetsScores(list, ENEMY, _scorer);
    }

    private void printPlanetsScores(List<Planet> list, Race owner, IScore<Planet> _scorer2) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Planet p : list)
            if (p.owner() == owner) {
                double score = _scorer.score(p);
                if (score > max)
                    max = score;
                if (score < min)
                    min = score;
            }
        
        System.out.println("Sorted " + owner + ":");        
        for (Planet p : list)
            if (p.owner() == owner) {
                double score = _scorer.score(p);
                double percent = (score - min) * 100 / (max - min);
                System.out.println(((double)(int)(percent * 100))/100.0  + "%\t" +
                                   score + ":\t" + p);                
            }
    }

    float minScore;
    float maxScore;
    
    private void calcScore() {
        Planet p = new Planet(0, 0, ALLY);
        minScore = Float.MAX_VALUE;
        maxScore = Float.MIN_VALUE;
        for (int i=0; i<SIZE/STEP; i++) {
            for (int j=0; j<SIZE/STEP; j++) {
                double x = _xMin + (double)(STEP/2 + STEP*i) * (_xMax - _xMin) / (double)SIZE;
                double y = _yMin + (double)(STEP/2 + STEP*j) * (_yMax - _yMin) / (double)SIZE;
                p.setX(x);
                p.setY(y);
                DistanceCache.reset();
                float s = (float)_scorer.score(p);
                if (s < minScore) minScore = s;
                if (s > maxScore) maxScore = s;
                _score[i][j] = s;
            }
        }
    }

    private void calcDimensions() {
        _xMin = _yMin = Double.MAX_VALUE;
        _xMax = _yMax = Double.MIN_VALUE;
        for (Planet planet : _game.planets()) {
            if (planet.x() < _xMin) _xMin = planet.x();
            if (planet.y() < _yMin) _yMin = planet.y();
            if (planet.x() > _xMax) _xMax = planet.x();
            if (planet.y() > _yMax) _yMax = planet.y();
        }
    }

    class DrawArea extends Canvas {
        private static final long serialVersionUID = 1L;

        @Override
        public void paint(Graphics g) {
          Graphics2D g2d = (Graphics2D) getGraphics();
          drawScores(g2d);
          for (Planet planet : _game.planets())
              drawPlanet(planet, g2d);
        }

        private void drawScores(Graphics2D g) {
            for (int i=0; i<SIZE/STEP; i++)
                for (int j=0; j<SIZE/STEP; j++) {
                    int red   = (int)((_score[i][j] - minScore) * 255.0f / (maxScore - minScore));
                    int green = (int)((maxScore - _score[i][j]) * 255.0f / (maxScore - minScore));
                    if (!average(_score[i][j]))
                        g.setColor(new Color(red, green, 0));
                     else 
                        g.setColor(new Color(red, green, 75));
                    if (nearZero(_score[i][j]))
                        g.setColor(g.getColor().brighter());
                    g.fillRect(STEP*i, SIZE-STEP*(j+1), STEP, STEP);
                }
        }

        private boolean nearZero(float f) {
            return Math.abs(f - 0.0) < (maxScore - minScore)/100.0f;
        }

        private boolean average(float f) {
            return Math.abs(f - (minScore + (maxScore - minScore)/2.0f)) < (maxScore - minScore)/50.0f;
        }

        private void drawPlanet(Planet planet, Graphics2D g2d) {
            int x = 20 + (int)((planet.x() - _xMin) * (SIZE - 40) / (_xMax - _xMin));
            int y = 20 + (int)((planet.y() - _yMin) * (SIZE - 40) / (_yMax - _yMin));
            y = SIZE - y;
            int r = _sizes[planet.growth()];
            switch (planet.owner()) {
                case ALLY: g2d.setColor(Color.BLUE); break;
                case ENEMY: g2d.setColor(Color.RED); break;
                case NEUTRAL: g2d.setColor(Color.GRAY); break;
            }
            g2d.fillOval(x-r/2, y-r/2, r, r);
            g2d.setColor(g2d.getColor().darker());
            g2d.drawOval(x-r/2, y-r/2, r, r);
        }
        
    }
    
    public static void main(String[] args) {
        new Visualiser();
    }
    
}

