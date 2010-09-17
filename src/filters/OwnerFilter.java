package filters;

import java.util.Collection;

import shared.Planet;
import shared.Race;

public class OwnerFilter extends PlanetFilter {

    Race _owner;
    
    public OwnerFilter(Collection<Planet> planets, Race owner) {
        super(planets);
        _owner = owner;
    }
    
    @Override
    public boolean filter(Planet planet) {
        return planet.owner() == _owner;
    }

}
