package bot;

import java.util.ArrayList;
import java.util.List;

import shared.Fleet;
import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.FirstLoseRecorder;
import simulate.LastLoseRecorder;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;
import continuous.Adjacency;

public abstract class SimulatorBot extends BaseBot  implements OwnerChangeListener {

    public abstract class Algorithm {
        SimulatorBot _bot;
        
        public Algorithm(SimulatorBot bot) {
            _bot = bot;
        }

        public void issueOrder(Planet src, Planet dst, int ships) {
            _bot.issueOrder(src, dst, ships);
        }

        public void log(String msg) {
            _bot.log(msg);
        }

        public SimulatorBot bot() {
            return _bot;
        }
        
        public abstract boolean execute();
    }
    
    public static final int TURNS_PREDICT = 50;
    
    boolean                     _ownerChanged;
    public Simulator             _sim;
    public LastLoseRecorder             _lastLostListener;
    public LastLoseRecorder             _lastN2ELostListener;
    public FirstLoseRecorder             _firstA2ELostListener;
    public MinShipsListener             _minShipsListener;
    public Adjacency                     _adj;

    public SimulatorBot() {
        _sim = new Simulator();
        
        // listener for *->enemy ownership changes
        _lastLostListener = new LastLoseRecorder(Race.ENEMY);
        _sim.addListener(_lastLostListener);

        // listener for neutral->enemy ownership changes
        _lastN2ELostListener = new LastLoseRecorder(Race.NEUTRAL, Race.ENEMY);
        _sim.addListener(_lastN2ELostListener);
        
        // listener for ally->enemy ownership changes
        _firstA2ELostListener= new FirstLoseRecorder(Race.ALLY, Race.ENEMY);
        _sim.addListener(_firstA2ELostListener);
        
        // listener for minimum available ships during simulation
        _minShipsListener = new MinShipsListener();
        _sim.addListener(_minShipsListener);
        
        _sim.addListener(this);
    }
    
    public void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer, this);
        }
        _adj.doWork(Utils.timeout() / 2);
    }
    
    // 
    // OwnerChangeListener interface methods
    //
    
    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        _ownerChanged = true;
    }

    @Override
    public void reset() {
        _ownerChanged = false;
    }

    @Override
    public int turn() {
        return 0;
    }

    public int shipsAvailable(Planet planet, Race owner) {
        _sim.simulate(planet, TURNS_PREDICT);
        if (_ownerChanged)
            return 0;
        return _minShipsListener.ships(owner);
    }

    public int shipsAvailableSafe(Planet planet, Race owner) {
        Race other = (owner == Race.ALLY ? Race.ENEMY : Race.ALLY);
        Planet closest = _adj.getNearestNeighbor(planet, other);
        if (closest == null)
            return shipsAvailable(planet, owner);
        Planet copy = planet.deepCopy();
        copy.addIncomingFleet(new Fleet(other, closest.ships(), closest, planet));
        _sim.simulate(copy, TURNS_PREDICT);
        if (_ownerChanged)
            return 0;
        return _minShipsListener.ships(owner);
    }

    public List<Planet> selectOwnerToSum(Race owner, Planet target,
                                         int shipsNeeded, int distance) {
        List<Planet> ret = new ArrayList<Planet>();
        int sumShips = 0;
        int shipsAvail = 0;
        for (Planet planet : _adj.neighbors(target)) {
            if (target.distance(planet) > distance || sumShips >= shipsNeeded)
                break;
            if (planet.owner() == owner) {
                if ((shipsAvail = shipsAvailable(planet, owner)) == 0)
                    continue;
                sumShips += shipsAvail;
                ret.add(planet);
            }
        }
        return ret;
    }

    public List<Planet> selectCloserThan(Race owner, Planet target, int distance) {
        List<Planet> ret = new ArrayList<Planet>();
        for (Planet planet : _adj.neighbors(target)) {
            if (planet.distance(target) > distance)
                break;
            if (planet.owner() == owner)
                ret.add(planet);
        }
        return ret;
    }

    public int sumFutureShips(ArrayList<Planet> planets, int turns) {
        int ret = 0;
        for (Planet planet : planets) {
            assert(planet.owner() != Race.NEUTRAL) : "no neutrals";
            Planet future = _sim.simulate(planet, turns);
            if (future.owner() == planet.owner())
                ret += future.ships(); 
        }
        return ret;
    }

    public int sumFutureShipsTarget(Planet target, ArrayList<Planet> planets, int turns) {
        int ret = 0;
        for (Planet planet : planets) {
            assert(planet.owner() != Race.NEUTRAL) : "no neutrals";
            Planet future = _sim.simulate(planet, turns - planet.distance(target));
            if (future.owner() == planet.owner())
                ret += future.ships(); 
        }
        return ret;
    }
    
    public int getFurthest(Planet target, ArrayList<Planet> planets) {
        int ret = 0;
        for (Planet planet : planets)
            if (planet.distance(target) > ret)
                ret = planet.distance(target);
        return ret;
    }
    
}
