package continuous;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import shared.Planet;

import compare.GrowthFieldCloseness;
import compare.IScore;

public class Knapsack {

    static Data[][] _data;
    
    public static Collection<Planet> solve(Planet home, List<Planet> planets, int W) {
        int num = planets.size();
        
        IScore<Planet> scorer = new GrowthFieldCloseness(planets);
        
        // initialize
        _data = new Data[W + 1][num + 1];
        for (int i=0; i<W+1; i++)
            _data[i][0] = new Data();
        for (int i=1; i<num+1; i++)
            _data[0][i] = new Data();
        
        Data maxData = _data[0][0];
        for (int w=1; w<W + 1; w++) {
            for (int i=1; i<num + 1; i++) {
                Planet planet = planets.get(i - 1);
                int pw = planet.ships() + 1; 
                if (pw > w) {
                    // planet weight is more than the current weight limit
                    _data[w][i] = new Data(_data[w][i-1]);
                } else {
                    int value = - (int)(100.0d * scorer.score(planet));
                    // planet weight is less or equal to the current weight limit
                    if (_data[w][i - 1]._value >= _data[w - pw][i - 1]._value + value) {
                        _data[w][i] = new Data(_data[w][i - 1]);
                    } else {
                        _data[w][i] = new Data(_data[w - pw][i - 1]);
                        _data[w][i]._value += value;
                        _data[w][i]._item = planet;
                        if (_data[w][i]._value > maxData._value)
                            maxData = _data[w][i];
                    }
                }
            }
        }
        
        Set<Planet> ret = new HashSet<Planet>();
        // backtrack from maxData
        do {
            if (maxData._item != null)
                ret.add(maxData._item);
        } while ((maxData = maxData._backEdge) != null);
        
        return ret;
    }
    
    static class Data {
        Data() {}
        Data(Data data) {
            _backEdge = data;
            _value = data._value;
            _item = null;
        }
        Data    _backEdge;
        int     _value;
        Planet  _item;
    }
    
}
