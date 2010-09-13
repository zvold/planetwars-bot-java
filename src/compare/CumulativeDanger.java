package compare;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import shared.Planet;

public class CumulativeDanger<K extends Planet, E> implements Comparator<Pair<K, E>>, IScore<K> {

    Map<K, Double> _scoreCache = new HashMap<K, Double>();
    Set<K> _planets;
    int _ahead;

    public CumulativeDanger(Set<K> planets, int ahead) {
        _planets = planets;
        _ahead = ahead;
    }

    @Override
    public int compare(Pair<K, E> p1, Pair<K, E> p2) {
        return (int) (score(p1._key) - score(p2._key));
    }

    @Override
    public double score(K target) {
        if (_scoreCache.containsKey(target))
            return _scoreCache.get(target);
        else {
            double score = 0;

            for (Planet planet : _planets)
                if (!planet.equals(target))
                    score += (double) (planet.ships() + planet.growth() * _ahead) / 
                             (double) (target.distance(planet));

            score *= (double) (target.ships() + target.growth() * _ahead);
            score *= 100.0d;

            assert (!Double.isNaN(score)) : "NaN check";

            _scoreCache.put(target, score);
            return score;
        }
    }

}
