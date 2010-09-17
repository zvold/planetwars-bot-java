package compare;

import java.util.Collection;

import shared.Planet;

public class SimpleCloseness implements IScore<Planet> {

    Collection<Planet> _planets;

    public SimpleCloseness(Collection<Planet> planets) {
        _planets = planets;
    }

    @Override
    public int compare(Planet p1, Planet p2) {
        return (int) (score(p1) - score(p2));
    }

    @Override
    public double score(Planet target) {
        double score = 0;
        for (Planet planet : _planets) {
            if (planet == target)
                continue;
            score += (double)planet.ships() / (double)planet.distance(target);
        }
        score *= 100.0d;
        
        return -score;
    }

}
