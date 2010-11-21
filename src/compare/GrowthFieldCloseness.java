package compare;

import java.util.Collection;

import shared.Planet;

public class GrowthFieldCloseness implements IScore<Planet> {

    Collection<Planet> _planets;

    public GrowthFieldCloseness(Collection<Planet> planets) {
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
            double distance = (planet == target) ? 1.0d : (double)planet.distance(target);            
            score -= (double)planet.growth() / distance;
        }
        score *= 100.0d;
        
        return score;
    }

}
