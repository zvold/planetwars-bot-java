package compare;

import java.util.Collection;

import shared.Planet;

public class PlanetsAdjComparator implements IScore<Planet> {

    Collection<Planet> _planets;

    public PlanetsAdjComparator(Collection<Planet> planets) {
        _planets = planets;
    }
    
    @Override
    public int compare(Planet p1, Planet p2) {
        return (int)(score(p1) - score(p2));
    }

    @Override
    public double score(Planet target) {
        double score = 0;
        for (Planet planet : _planets) {
            if (planet == target)
                continue;
            score += (double)1.0d / (double)planet.distance(target);
        }
        score *= 100.0d;
        
        return -score;
    }

}
