package shared;


public class DistanceCache {

    static int[][] _distances = new int[32][32]; // 32 planets

    static {
        reset();
    }

    public static int distance(Planet p1, Planet p2) {
        assert(p1.id() < 32 && p2.id() < 32) : "increase DistanceCache";
        int dist = _distances[p1.id()][p2.id()]; 
        if (dist == -1) {
            dist = (int)Math.ceil(Math.sqrt(p1.distSquared(p2)));
            _distances[p1.id()][p2.id()] = dist;
            _distances[p2.id()][p1.id()] = dist;
        }
        return dist; 
    }

    public static void reset() {
        for (int i=0; i<_distances.length; i++)
            for (int j=0; j<_distances[i].length; j++)
                _distances[i][j] = -1;
    }

}
