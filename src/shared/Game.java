package shared;

import java.util.ArrayList;
import java.util.List;

import filters.OwnerFilter;

public class Game {

    ArrayList<Planet>    _planets = new ArrayList<Planet>(32);
   
    /**
     * Creates the Game object from the full game state (a String ending with
     * "go").
     * 
     * @param from
     */
    public Game(String from) {
        initFullState(from);
    }
    
    /**
     * Creates a Game w/o the state. Subsequent calls to updateFullState(),
     * initFullState(), or updateOneLine() expected.
     */
    public Game() {
        Parser.init("");
    }
    
    /**
     * Updates the Game object with the information from passed full game state
     * (a String ending with "go"). Tries to re-use existing Planet and Fleet
     * objects.
     * 
     * @param from
     */
    public void updateFullState(String from) {
        if (_planets.isEmpty()) {
            // just in case we haven't invoked init* first
            initFullState(from);
        } else {
            startTurnParsing();
            Parser.init(from);
            while (Parser.hasNext()) {
                switch (Parser.kind()) {
                    case FLEET:
                        Fleet fleet = (Fleet)Parser.next();
                        addFleet(fleet);
                        break;
                    case PLANET:
                        Parser.updatePlanet(_planets);
                        break;
                    default:
                        Parser.discard();
                }
            }
        }
    }
    
    private void clearAllFleets() {
        for (Planet planet : _planets) {
            planet._incoming[Race.ALLY.ordinal()].clear();
            planet._incoming[Race.ENEMY.ordinal()].clear();
        }
    }

    /**
     * Creates a Game object from scratch.
     * 
     * @param from Full game state String, ending with "go".
     */
    public void initFullState(String from) {
        _planets.clear();
        Parser.init(from);
        while (Parser.hasNext()) {
            switch (Parser.kind()) {
                case FLEET: {
                    Fleet fleet = (Fleet)Parser.next();
                    addFleet(fleet);
                    break;
                }
                case PLANET: {
                    Planet planet = (Planet)Parser.next(); 
                    _planets.add(planet);
                    break;
                }
                default:
                    Parser.discard();
                    break;
            }
        }
    }
    
    boolean addFleet(Fleet newFleet) {
        Planet planet = _planets.get(newFleet.dst());
        assert(planet != null) : "must have a planet";
        for (Fleet f : planet.incoming(newFleet.owner()))
            if (f.dst() == newFleet.dst() &&
                f.src() == newFleet.src() &&
                f._eta == newFleet._eta &&
                f._trip == newFleet._trip &&
                f._owner == newFleet._owner) {
                        f.addShips(newFleet.ships());
                        return true;
            }
        planet._incoming[newFleet.owner().ordinal()].add(newFleet);
        return false;
    }

    public void updateOneLine(String line) {
        Parser.setSingleLine(line);
        if (Parser.hasNext()) {
            switch (Parser.kind()) {
                case FLEET:
                    Fleet fleet = (Fleet)Parser.next();
                    addFleet(fleet);
                    break;
                case PLANET:
                    Parser.updatePlanet(_planets);
                    break;
                default:
                    Parser.discard();
            }
        }
    }
    
    /**
     * Maintenance method to be called when the turn parsing is finished, i.e.
     * after "go" command is encountered.
     */
    public void startTurnParsing() {
        assert(Parser._planetId == _planets.size()) : "number of planets doesn't change";
        Parser.reset();
        clearAllFleets();
    }
    
    /**
     * Adds a future order to both source and destination planet (an arrival copy).
     * @param order
     */
    public void addFutureOrder(FutureOrder order) {
        if (order.ships() == 0) // don't bother
            return;
        planet(order.from()).addFutureOrder(order);
        planet(order.to()).addFutureOrder(arrivalCopy(order));
    }

    private FutureOrder arrivalCopy(FutureOrder order) {
        Planet src = planet(order.from());
        Planet dst = planet(order.to());
        return new FutureOrder(order.id(), order.owner(), src, dst, order.ships(), 
                               order.turn() + src.distance(dst));
    }

    /**
     * Removes corresponding arrival copy from the destination planet.
     * @param turn 
     * 
     * @return The removed order.
     */
    public FutureOrder removeArrival(FutureOrder order) {
        Planet dst = planet(order.to());
        return dst.removeFutureOrder(order);
    }
  
    public void advanceFutureOrders() {
        for (Planet planet : _planets)
            planet.advanceFutureOrders();
    }
    
    public void clearFutureOrders() {
        for (Planet planet : _planets)
            planet.clearFutureOrders();
    }
    
    public List<Planet> planets() {
        return _planets;
    }
    
    public List<Planet> planets(Race owner) {
        return new OwnerFilter(_planets, owner).select();
    }
    
    public Planet planet(int i) {
        return _planets.get(i);
    }

    /**
     * Returns total growth rate for a given owner.
     */
    public int growth(Race owner) {
        int ret = 0;
        for (Planet planet : planets(owner))
            ret += planet.growth();
        return ret;
    }

    /**
     * Returns total number of ships for a given owner.
     */
    public int ships(Race owner) {
        int ships = 0;
        for (Planet planet : planets(owner))
            ships += planet.ships();
        return ships + fleets(owner);
    }

    /**
     * Returns total number of ships in flight for a given owner.
     */
    public int fleets(Race owner) {
        int ships = 0;
        for (Planet planet : _planets) {
            for (Fleet fleet : planet.incoming(owner))
                ships += fleet.ships();
        }
        return ships;
    }

    public List<Fleet> fleets() {
        List<Fleet> ret = new ArrayList<Fleet>();
        for (Planet planet : _planets) {
            ret.addAll(planet.incoming(Race.ALLY));
            ret.addAll(planet.incoming(Race.ENEMY));
        }
        return ret;
    }

    public void clearAllData() {
        for (Planet planet : _planets)
            planet.setData(null);
    }

}
