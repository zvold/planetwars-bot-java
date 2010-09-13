package bot;

import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;
import continuous.Adjacency;

public class SimpleBot extends BaseBot implements OwnerChangeListener {

    public static final int TURNS_EXTRA = 5;
    public static final int TURNS_PREDICT = 50;
    
    Random _rnd = new Random(System.currentTimeMillis());
    Adjacency _adj;
    Simulator _sim;
    MinShipsListener _minListener;
    boolean _ownerChanged;

    public static void main(String[] args) {
        parseArgs(args);
        SimpleBot bot = new SimpleBot();
        bot.run();
    }

    SimpleBot() {
        _sim = new Simulator();
        _minListener = new MinShipsListener();
        _sim.addListener(_minListener);
        _sim.addListener(this);
    }
    
    @Override
    void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer);
        }
        _adj.doWork(Utils.timeout() / 2);
        Utils.log("# doTurn() started at " + _timer.totalTime() + " ms total");

        ArrayList<Planet> attack = new ArrayList<Planet>();
        selectForAttack(_game.planets(NEUTRAL), attack);
        selectForAttack(_game.planets(ENEMY), attack);
        Utils.log("# " + attack.size() + " targets selected at " + _timer.totalTime() + " ms total");

        CumulativeCloseness comp = new CumulativeCloseness(_game.planets());

        while (_timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            Collections.sort(attack, comp);
            // get best potential planet to attack
            Planet target = attack.get(0);

            // get set of several nearest allied planets
            Set<Planet> sources = getNearestAllies(target, _adj.neighbors(target));

            if (!sources.isEmpty()) {
                int totalShips = 0;
                for (Planet src : sources) {
                    _minListener.reset();
                    _sim.simulate(src, src.distance(target) + 1);
                    totalShips += _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                  _minListener.ships(Race.ALLY) : 0;
                }

                int furthestSource = 0;
                for (Planet src : sources)
                    if (src.distance(target) > furthestSource)
                        furthestSource = src.distance(target);

                // TODO: future may be allied, so predict neede ships number more accurately
                Planet future = _sim.simulate(target, furthestSource + 1);
                int shipsNeeded = future.ships() + 1;

                if (totalShips != 0 && totalShips >= shipsNeeded) {
                    int shipsSent = 0;
                    for (Planet src : sources) {
                        _minListener.reset();
                        _sim.simulate(src, src.distance(target) + 1);
                        int shipsAvail = _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                         _minListener.ships(Race.ALLY) : 0;
                        int num = (int)(0.5d + (double)shipsNeeded *
                                               (double)shipsAvail / (double)totalShips);
                        num = num > src.ships() ? src.ships() : num;
                        shipsSent += num;
                        issueOrder(src, target, num);
                        src.setShips(src.ships() - num);
                    }
                    Utils.log("# " + target + " attacked with "
                                   + shipsSent + " ships from " 
                                   + sources.size() + " sources");
                    Utils.log("# future " + future + " was predicted");
                }
            }
            attack.remove(target);
        }
        Utils.log("# total attack time " + _timer.totalTime() + " ms");
    }

    private void selectForAttack(Set<Planet> planets, ArrayList<Planet> attack) {
        for (Planet planet : planets) {
            this.reset();
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                continue;

            attack.add(planet);
        }
    }

    private Set<Planet> getNearestAllies(Planet target, ArrayList<Planet> neighbors) {
        Set<Planet> ret = new HashSet<Planet>();
        ArrayList<Planet> allies = new ArrayList<Planet>();
        for (Planet planet : neighbors)
            if (planet.owner() == ALLY)
                allies.add(planet);

        if (!allies.isEmpty()) {
            Planet source = allies.get(0);
            ret.add(source);
            int distance = source.distance(target);
            for (int i = 1; i < allies.size(); i++) {
                Planet planet = allies.get(i);
                
                assert (planet.distance(target) >= distance) : 
                        "check sorting" + planet.distance(target) + " >= " + distance;
                
                if (Math.abs(planet.distance(target) - distance) < TURNS_EXTRA)
                    ret.add(planet);
                else
                    break;
            }
        }

        return ret;
    }

    class CumulativeCloseness implements Comparator<Planet> {

        Collection<Planet> _planets;

        CumulativeCloseness(Collection<Planet> planets) {
            _planets = planets;
        }

        @Override
        public int compare(Planet p1, Planet p2) {
            return (int) (score(p1) - score(p2));
        }

        double score(Planet target) {
            double score = 0;
            for (Planet planet : _planets) {
                if (planet.owner() == Race.NEUTRAL)
                    continue;
                int sign = (planet.owner() == Race.ALLY) ? -1 : 1;
                score += (double)sign * (double)planet.ships() / (double)planet.distance(target);
            }
            score *= (double)target.growth() / (double)target.ships();
            score *= 100.0d;
            
            return score;
        }

    }

    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        _ownerChanged = true;
    }

    @Override
    public void reset() {
        _ownerChanged = false;
    }

}
