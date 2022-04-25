#!/bin/bash

RED='\e[31m'
BLACK='\e[30m'
GREEN='\033[0;32m'
LIGHT_GREEN='\033[1;32m'
YELLOW='\033[1;33m'
LIGHT_BLUE='\033[1;34m'
LIGHT_PURPLE='\033[1;35m'
CYAN='\033[0;36m'
LIGHT_CYAN='\033[1;36m'
WHITE='\033[0m'
C_END='\e[0m'


function inputData() {
  read -p "Please provide your best guess (number from 1 to 50): " GUESS
}

function caseYesNo() {
  read -p "Would you like to try again?(Y/N): " ANSWER
    case $ANSWER in
      [Yy]* ) printf "\nLet's try ${LIGHT_BLUE}again${C_END}. ";(continue 2>/dev/null);;
      [Nn]* ) printf "\nThat's fine. ${CYAN}Good luck next time${C_END}.\n\n";exit;;
      * ) printf "\n${RED}That's not the answer. Bye.${C_END}\n\n";exit;;
    esac
}

function guessingInput() {
  IS_NUMBER='^[0-9]+$'
  if [[ $GUESS =~ $IS_NUMBER ]]
  then
      if [[ $GUESS -eq $COMPUTER ]]
      then
        printf "\nYou won. The number is ${YELLOW}$COMPUTER${C_END}. Well done\n\n"
        VALID=1
        CHANCES_LEFT=0
      elif [[ $GUESS -gt $COMPUTER ]]
      then
        printf " --- Number too ${RED}HIGH${C_END}. "
        ((CHANCES_LEFT--))
        if [[ $CHANCES_LEFT -le 0 ]]
        then
          printf "\nNo chances left. The number was ${YELLOW}$COMPUTER${C_END}.\n"
          VALID=1
        else
          caseYesNo
        fi
      elif [[ $GUESS -lt $COMPUTER ]]
      then
        printf " --- Number too ${RED}LOW${C_END}. "
        ((CHANCES_LEFT--))
        if [[ $CHANCES_LEFT -le 0 ]]
        then
          printf "\nNo chances left. The number was ${YELLOW}$COMPUTER${C_END}.\n"
          VALID=1
        else
          caseYesNo
        fi
      fi
  else
    printf "\n${RED}Not a number!${C_END}\n\n"
    caseYesNo
  fi
}


COMPUTER=$[ $RANDOM % 50 + 1 ]
VALID=0
CHANCES_LEFT=5
CHANCES_TOTAL=$CHANCES_LEFT

while [[ $VALID = 0 ]]
do
  while [[ $CHANCES_LEFT -gt 0 ]]
  do
    if [[ $CHANCES_LEFT -lt $CHANCES_TOTAL ]]
    then
      printf "You have ${RED}$CHANCES_LEFT${C_END} chances left.\n\n"
      inputData
      guessingInput
    else
      inputData
      guessingInput
    fi
  done
done

printf "${RED}Game Over${C_END}\n\n"




