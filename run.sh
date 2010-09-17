#!/bin/bash
    echo "Opponent: $1"
    echo "Testing: $2"
    player_1_counter=0
    player_2_counter=0
    player_2_turn_counter=0
    total_games=0;
    for i in {1..100}
    do
        RES=`java -cp ../ai-contest/bin Engine ../ai-contest/maps/map$i.txt 3000 200 log.txt "$1" "$2" 2>&1 |
              tail -n 3 | egrep "^.*?:Turn|^.*?:Player"`

        TURN=`echo $RES | egrep "Turn" | sed -r 's/.*Turn ([0-9]+).*/\1/g'`
        RES2=`echo $RES | egrep "Player" | sed -r 's/.*(Player [0-9]) Wins!.*/\1/g'`
        total_games=`expr $total_games + 1`
        if [ "$RES2" = "Player 1" ] ; then
            player_1_counter=`expr $player_1_counter + 1`
            echo "lost on map$i, total $player_2_counter/$total_games"
        else
            player_2_counter=`expr $player_2_counter + 1`
            player_2_turn_counter=`expr $player_2_turn_counter + $TURN`
        fi

    done
    player_2_turn_counter=`expr $player_2_turn_counter / 100`
    echo "against '$1' : $player_2_counter/100, avg turns: $player_2_turn_counter"

