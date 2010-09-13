#!/bin/bash
for file in ../ai-contest/bin/*Bot.class
do
    player_1_counter=0
    player_2_counter=0
    player_2_turn_counter=0
    echo "Bot: $file"
    BOTNAME=`echo $file | sed -r 's/.*\/(.*)\.class/\1/'`
    echo "Bot name: $BOTNAME"
    for i in {1..100}
    do
        #echo "Map: $i"
        RES=`java -cp ../ai-contest/bin Engine ../ai-contest/maps/map$i.txt 1000 1000 log.txt \
             "java -cp ../ai-contest/bin $BOTNAME" "java -cp bin bot.SimpleBot" 2>&1 |
              tail -n 3 | egrep "^.*?:Turn|^.*?:Player"`

#        echo $RES
        TURN=`echo $RES | egrep "Turn" | sed -r 's/.*Turn ([0-9]+).*/\1/g'`
        RES2=`echo $RES | egrep "Player" | sed -r 's/.*(Player [0-9]) Wins!.*/\1/g'`

#        echo $TURN
#        echo $RES2
        if [ "$RES2" = "Player 1" ] ; then
            player_1_counter=`expr $player_1_counter + 1`
        else
            player_2_counter=`expr $player_2_counter + 1`
            player_2_turn_counter=`expr $player_2_turn_counter + $TURN`
        fi

    done
    player_2_turn_counter=`expr $player_2_turn_counter / 100`
    echo "$file : $player_2_counter/100, avg turns: $player_2_turn_counter"
done
