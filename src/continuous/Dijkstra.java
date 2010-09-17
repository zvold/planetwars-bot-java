package continuous;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import shared.Planet;
import shared.Timer;
import bot.BaseBot;

import compare.IScore;

public class Dijkstra extends TimedWork {

    Map<Planet, Node>   _nodes;
    PriorityQueue<Node> _queue;
    Set<Node>           _done;
    
    public Dijkstra(Timer timer, BaseBot bot) {
        super(timer, bot);
        _nodes = new HashMap<Planet, Node>(32);
        _queue = new PriorityQueue<Node>(32, new Node.DataComparator());
    }

    public void calculate(Planet target, 
                          Collection<Planet> sources, 
                          IScore<Planet> scorer) {
        init(target, sources);
        while (!_queue.isEmpty()) {
            Node u = _queue.remove();
            for (Planet v : sources)
                if (v != u._planet)
                    relax(u, v);
        }
    }

    private void relax(Node u, Planet v) {
        Node v_node = _nodes.get(v);
        int newMaxLength = Math.max(u._maxLength, u._planet.distance(v));
        if (newMaxLength < v_node._maxLength) {
            _queue.remove(v_node);
            v_node._backEdge = u._planet;
            v_node._maxLength = newMaxLength;
            _queue.add(v_node);
        }
    }

    private void init(Planet target, Collection<Planet> sources) {
        _nodes.clear();
        _queue.clear();
        for (Planet src : sources) {
            Node data = new Node(src);
            _nodes.put(src, data);
            _queue.add(data);
        }
        
        if (!sources.contains(target)) {
            Node data = new Node(target);
            _nodes.put(target, data);
            _queue.add(data);
        }
        
        Node targetNode = _nodes.get(target);
        _queue.remove(targetNode);
        targetNode._maxLength = 0;
        _queue.add(targetNode);
    }
    
    @Override
    public void doWorkChunk() {
        
    }

    @Override
    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String progress() {
        // TODO Auto-generated method stub
        return null;
    }

    static class Node {
        
        int     _maxLength = Integer.MAX_VALUE;
        Planet  _backEdge;
        Planet  _planet;
        
        Node(Planet planet) {
            _planet = planet;
        }
        
        @Override
        public String toString() {
            return "(" + _planet.id() + " <- " + (_backEdge == null ? _backEdge : _backEdge.id()) + 
                   ", max egde: " + _maxLength + ")";
        }
        
        static class DataComparator implements Comparator<Node> {
            @Override
            public int compare(Node d1, Node d2) {
                return d1._maxLength - d2._maxLength; 
            } 
        }
    }

    public Planet backEdge(Planet p) {
        Node node = _nodes.get(p);
        assert(node != null) : "dijkstra was requested for uncalculated planet";
        return node._backEdge;
    }
    
}
