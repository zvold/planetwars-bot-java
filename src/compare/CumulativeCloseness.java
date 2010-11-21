package compare;

import java.util.Collection;

import shared.Planet;
import shared.Race;

public class CumulativeCloseness implements IScore<Planet> {

    Collection<Planet> _planets;

    public CumulativeCloseness(Collection<Planet> planets) {
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
            if (planet.owner() == Race.NEUTRAL)
                continue;
            double distance = (planet == target) ? 1.0d : (double)planet.distance(target);
            int sign = (planet.owner() == Race.ALLY) ? -1 : 1;
            score += (double)sign * (double)planet.ships() / distance;
        }
        score *= (double)target.growth();
        score *= 10000.0d;
        
        return score;
    }

}
