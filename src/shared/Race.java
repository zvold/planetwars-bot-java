package shared;


public enum Race {

    NEUTRAL, ALLY, ENEMY;

    final static int[][] _others = new int[Race.values().length][Race.values().length - 1];
    
    static {
        for (Race race1 : Race.values()) {
            int t = 0;
            for (Race race2 : Race.values())
                if (race1 != race2)
                    _others[race1.ordinal()][t++] = race2.ordinal();
        }
    }
    
    public int[] others() {
        return _others[ordinal()];
    }
    
    public Race opponent() {
        switch (this) {
            case NEUTRAL:
                return Race.NEUTRAL;
            case ALLY:
                return Race.ENEMY;
            case ENEMY:
                return Race.ALLY;
            default:
                throw new RuntimeException();
        }
    }
    
}
