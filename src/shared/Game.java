package shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Game {

    boolean           _isFirstTurn = true;
    ArrayList<Planet> _planets = new ArrayList<Planet>(32);
    ArrayList<Fleet>  _fleets = new ArrayList<Fleet>(128);
    Parser            _parser;
    
    @SuppressWarnings("unchecked")
    Set<Planet>[]    _owners = new HashSet[] {new HashSet<Planet>(), 
                                              new HashSet<Planet>(),
                                              new HashSet<Planet>()};
    int[]            _growths = new int[3];
    
    public Game(String from) {
        initFullState(from);
    }
    
    public Game() {
        _parser = new Parser("");
    }
    
    public void updateFullState(String from) {
        if (_isFirstTurn) {
            // just in case we haven't invoked init* first
            initFullState(from);
        } else {
            _parser = new Parser(from);
            while (_parser.hasNext()) {
                switch (_parser.kind()) {
                    case FLEET:
                        _parser.updateFleet(_fleets);
                        break;
                    case PLANET:
                        Planet planet = _parser.updatePlanet(_planets);
                        updatePlanetOwner(planet);
                        break;
                    default:
                        _parser.discard();
                }
            }
            // make sure to discard the fleets' tail after arraylist reusage
            _fleets.subList(_parser._fleetId, _fleets.size()).clear();
            assert(_parser._planetId == _planets.size()) : "number of planets doesn't change";
            updatePlanetFleets();
        }
    }
    
    public void initFullState(String from) {
        _planets = new ArrayList<Planet>(32);
        _fleets = new ArrayList<Fleet>(128);
        
        _parser = new Parser(from);
        
        while (_parser.hasNext()) {
            switch (_parser.kind()) {
                case FLEET: {
                    Fleet fleet = (Fleet)_parser.next();
                    addFleet(fleet);
                    break;
                }
                case PLANET: {
                    Planet planet = (Planet)_parser.next(); 
                    _planets.add(planet);
                    updatePlanetOwner(planet);
                    break;
                }
                default:
                    _parser.discard();
                    break;
            }
        }
        updatePlanetFleets();
        _isFirstTurn = false;
    }
    
    boolean addFleet(Fleet newFleet) {
        // merge fleets if needed
        for (Fleet f : _fleets)
            if (f._dst == newFleet._dst &&
                f._src == newFleet._src &&
                f._eta == newFleet._eta &&
                f._trip == newFleet._trip &&
                f._owner == newFleet._owner) {
                    f.addShips(newFleet.ships());
                    return true;
            }
        // just add the new fleet if not found
        _fleets.add(newFleet);
        return false;
    }

    public void updateOneLine(String line) {
        _parser.setSingleLine(line);
        if (_parser.hasNext()) {
            switch (_parser.kind()) {
                case FLEET:
                    _parser.updateFleet(_fleets);
                    break;
                case PLANET:
                    Planet planet = _parser.updatePlanet(_planets);
                    updatePlanetOwner(planet);
                    break;
                default:
                    _parser.discard();
            }
        }
    }
    
    public void resetTurn() {
        // make sure to truncate fleets list, discarding non-used 'tail'
        _fleets.subList(_parser._fleetId, _fleets.size()).clear();
        updatePlanetFleets();
        assert(_parser._planetId == _planets.size()) : "number of planets doesn't change";
        _parser.reset();
    }
    
    private void updatePlanetFleets() {
        for (Planet planet : _planets) {
            planet.clearFleets();
            for (Fleet fleet : _fleets)
                if (fleet.dst() == planet.id())
                    planet.addIncomingFleet(fleet);
        }
    }

    public void updatePlanetOwner(Planet planet) {
        boolean wasAdded = _owners[planet.owner().ordinal()].add(planet);
        if (wasAdded)
            _growths[planet.owner().ordinal()] += planet.growth();
        
        for (int i : planet.owner().others()) {
            boolean wasRemoved = _owners[i].remove(planet);
            if (wasRemoved)
                _growths[i] -= planet.growth();
        }
        
        assert(_owners[0].size() + 
               _owners[1].size() +
               _owners[2].size() == _planets.size()) : "check total planets amount";
        
        assert(checkTotalGrowth()) : "growth rate tracking sanity check";
    }

    public void addFutureOrder(FutureOrder order) {
        if (order.ships() == 0)
            return;
        planet(order.from().id()).addFutureOrder(order);
        planet(order.to().id()).addFutureOrder(order.arrivalCopy());
    }

    public void advanceFutureOrders() {
        for (Planet planet : _planets)
            planet.advanceFutureOrders();
    }
    
    public void clearFutureOrders() {
        for (Planet planet : _planets)
            planet.clearFutureOrders();
    }
    
    private boolean checkTotalGrowth() {
        int totalGrowth = 0;
        for (Planet planet : _planets)
            totalGrowth += planet.growth();
        return _growths[0] + _growths[1] + _growths[2] == totalGrowth;
    }

    public ArrayList<Planet> planets() {
        return _planets;
    }
    
    public Set<Planet> planets(Race race) {
        return _owners[race.ordinal()];
    }
    
    public ArrayList<Fleet> fleets() {
        return _fleets;
    }

    public Planet planet(int i) {
        return _planets.get(i);
    }

    public int growth(Race race) {
        return _growths[race.ordinal()];
    }

    public int ships(Race owner) {
        int ships = 0;
        for (Planet planet : planets(owner))
            ships += planet.ships();
        for (Fleet fleet : fleets())
            if (fleet.owner() == owner)
                ships += fleet.ships();
        return ships;
    }

    public int fleets(Race owner) {
        int ships = 0;
        for (Fleet fleet : fleets())
            if (fleet.owner() == owner)
                ships += fleet.ships();
        return ships;
    }
}
