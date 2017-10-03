/*
 * Copyright (C) 2016  Tobias Bielefeld
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you want to contact me, send me an e-mail at tobias.bielefeld@gmail.com
 */

package de.tobiasbielefeld.solitaire.helper;

import java.util.ArrayList;

import de.tobiasbielefeld.solitaire.classes.Card;
import de.tobiasbielefeld.solitaire.classes.Stack;
import de.tobiasbielefeld.solitaire.ui.GameManager;

import static de.tobiasbielefeld.solitaire.SharedData.*;

/**
 * Manages the records, so the player can undo movements. for that it has an entry subclass
 * which has a variable amount of cards, so multiple cards can be undo at once
 */

public class RecordList {

    public final static int MAX_RECORDS = 20;
    private ArrayList<Entry> entries = new ArrayList<>();

    public void reset() {                                                                                  //delete the content on reset
        entries.clear();
    }


    /**
     * Adds entries of the card list, if the maximum number of records was reached, delete
     * the last one. The origin of the cards will be the current stack
     *
     * @param cards The card list to add
     */
    public void add(ArrayList<Card> cards) {
        if (entries.size() == MAX_RECORDS)
            entries.remove(0);

        entries.add(new Entry(cards));
    }

    /**
     * Adds entries of the card list, if the maximum number of records was reached, delete
     * the last one. This version also takes a stack as origin of the cards
     *
     * @param cards  The card list to add
     * @param origin Other stack as origin, where the cards can be returned to
     */
    public void add(ArrayList<Card> cards, Stack origin) {
        if (entries.size() == MAX_RECORDS)
            entries.remove(0);

        entries.add(new Entry(cards, origin));
    }


    /**
     * Adds entries of the card list, if the maximum number of records was reached, delete
     * the last one. This version also takes a stack array list as origins, so every card can
     * have a different origin stack
     *
     * @param cards   the card list to add
     * @param origins Other stacks as origin, where the cards can be returned to
     */
    public void add(ArrayList<Card> cards, ArrayList<Stack> origins) {
        if (entries.size() == MAX_RECORDS) {
            entries.remove(0);
        }

        entries.add(new Entry(cards, origins));
    }

    /**
     * Adds more cards to the last entry but as the first cards of that entry, so these cards will be
     * moved at first, if the record is undone
     *
     * @param cards   Multiple cards to add
     * @param origins Origin stacks of these cards
     */
    public void addToLastEntry(ArrayList<Card> cards, ArrayList<Stack> origins) {
        if (entries.size() == 0) {
            entries.add(new Entry(cards, origins));
        } else {
            entries.get(entries.size() - 1).addInFront(cards, origins);
        }
    }

    /**
     * Adds more cards to the last entry but as the first cards of that entry, so these cards will be
     * moved at first, if the record is undone
     *
     * @param card   Single cards to add
     * @param origin Origin stack of these cards
     */
    public void addToLastEntry(Card card, Stack origin) {
        ArrayList<Card> cards = new ArrayList<>();
        ArrayList<Stack> origins = new ArrayList<>();

        cards.add(card);
        origins.add(origin);

        addToLastEntry(cards,origins);
    }

    /**
     * reverst one record, this will delete that record from the list and takes 25 points away
     * from the current score
     */
    public void undo(GameManager gm) {
        if (!entries.isEmpty()) {
            sounds.playSound(Sounds.names.CARD_RETURN);
            scores.update(-currentGame.getUndoCosts());
            entries.get(entries.size() - 1).undo(gm);
            //entries.remove(entries.size() - 1);
        }
    }

    public void undoMore() {
        if (!entries.isEmpty()) {
            entries.get(entries.size() - 1).undoMore();
        }
    }

    /**
     * a flip card will be added to the last entry.
     * so it can be flipped down in a undo
     *
     * @param card The card to add
     */
    public void addFlip(Card card) {

        if (entries.size() > 0)
            entries.get(entries.size() - 1).addFlip(card);
    }

    /**
     * Saves every entry
     */
    public void save() {
        putInt(RECORD_LIST_ENTRIES_SIZE, entries.size());

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).save(Integer.toString(i));
        }
    }

    /**
     * load the saved entries. Calling the Entry constructor with a string will load
     * its content from the shared Pref
     */
    public void load() {

        reset();

        int size = getInt(RECORD_LIST_ENTRIES_SIZE, -1);

        for (int i = 0; i < size; i++) {
            entries.add(new Entry(Integer.toString(i)));
        }
    }

    public void deleteLast() {
        if (entries.size() > 0) {
            entries.remove(entries.size() - 1);
        }
    }

    public boolean hasMoreToUndo(){
        if (entries.isEmpty()){
            return false;
        }

        if (entries.get(entries.size()-1).hasMoreToDo()){
            return true;
        } else {
            entries.remove(entries.size() - 1);
            currentGame.afterUndo();
            return false;
        }
    }

    private class Entry {
        private ArrayList<Integer> moveOrder = new ArrayList<>();
        private ArrayList<Card> currentCards = new ArrayList<>();
        private ArrayList<Stack> currentOrigins = new ArrayList<>();
        private ArrayList<Card> flipCards = new ArrayList<>();

        /**
         * This constructor is used to load saved entries.
         *
         * @param pos The index of the saved entry to load
         */
        Entry(String pos) {
            ArrayList<Integer> cardList = getIntList(RECORD_LIST_ENTRY + pos + CARD);
            ArrayList<Integer> originList = getIntList(RECORD_LIST_ENTRY + pos + ORIGIN);
            ArrayList<Integer> orderList = getIntList(RECORD_LIST_ENTRY + pos + ORDER);

            for (int i = 0; i < cardList.size(); i++) {
                currentCards.add(cards[cardList.get(i)]);
                currentOrigins.add(stacks[originList.get(i)]);

                if (orderList.size()>i){
                    moveOrder.add(orderList.get(i));
                } else {
                    moveOrder.add(0);
                }
            }

            //compability to older way of saving: changed from one possible flip card to multiple
            try {   //new way
                ArrayList<Integer> flipCardList = getIntList(RECORD_LIST_ENTRY + pos + FLIP_CARD);

                for (Integer i : flipCardList) {
                    flipCards.add(cards[i]);
                }
            } catch (Exception e) { //old way
                int flipCardID = getInt(RECORD_LIST_ENTRY + pos + FLIP_CARD, -1);

                if (flipCardID > 0)
                    addFlip(cards[flipCardID]);
            }
        }

        /**
         * Create a new entry with the given cards. Origins will be the current card positions
         *
         * @param cards The cards to add
         */
        Entry(ArrayList<Card> cards) {
            currentCards.addAll(cards);

            for (Card card : cards) {
                currentOrigins.add(card.getStack());
                moveOrder.add(0);
            }
        }

        /**
         * Create a new entry with the given cards. Origin will be applied for all cards
         *
         * @param cards  The cards to add
         * @param origin The origin of the cards
         */
        Entry(ArrayList<Card> cards, Stack origin) {
            currentCards.addAll(cards);

            for (int i = 0; i < currentCards.size(); i++) {
                currentOrigins.add(origin);
                moveOrder.add(0);
            }
        }

        /**
         * Create a new entry with the given cards and origins
         *
         * @param cards   The cards to add
         * @param origins The orgins of the cards
         */
        Entry(ArrayList<Card> cards, ArrayList<Stack> origins) {
            currentCards.addAll(cards);
            currentOrigins.addAll(origins);

            for (int i = 0; i < currentCards.size(); i++) {
                moveOrder.add(0);
            }
        }

        /**
         * Saves the current entry in the shared pref. It needs to save the IDS of the cards and the
         * size of the array lists. Loading happens in one of the constructors
         *
         * @param pos The index of this entry in the array list
         */
        void save(String pos) {
            ArrayList<Integer> listCards = new ArrayList<>();
            ArrayList<Integer> listFlipCards = new ArrayList<>();
            ArrayList<Integer> listOrigins = new ArrayList<>();

            for (int i = 0; i < currentCards.size(); i++) {
                listCards.add(currentCards.get(i).getId());
                listOrigins.add(currentOrigins.get(i).getId());

            }

            putIntList(RECORD_LIST_ENTRY + pos + CARD, listCards);
            putIntList(RECORD_LIST_ENTRY + pos + ORIGIN, listOrigins);
            putIntList(RECORD_LIST_ENTRY + pos + ORDER, moveOrder);

            for (Card card : flipCards) {
                listFlipCards.add(card.getId());
            }

            putIntList(RECORD_LIST_ENTRY + pos + FLIP_CARD, listFlipCards);
        }


        /**
         * Undos the latest entry.
         */
        void undo(GameManager gm) {
            //Check if the movement resulted in a increment of the redeal counter, if so, revert it
            if (currentGame.hasLimitedRecycles()
                    && currentOrigins.get(0) == currentGame.getDiscardStack()
                    && currentCards.get(0).getStack() == currentGame.getDealStack()) {
                currentGame.decrementRecycleCounter(gm);
            }

            for (Card card : flipCards) {
                card.flipWithAnim();
            }

            handlerRecordListUndo.sendEmptyMessageDelayed(0,0);
        }

        /**
         * This contains the actual card movements. It will undo the movements of the cards
         * with the lowest move order and remove them from the list.
         *
         * This method is called from a handler. With each call, the lowest order will be used, until
         * all cards are away. So the movements are tiered.
         */
        void undoMore() {
            ArrayList<Card> cardsWorkCopy = new ArrayList<>(currentCards);
            ArrayList<Stack> originsWorkCopy = new ArrayList<>(currentOrigins);
            ArrayList<Integer> moveOrderWorkCopy = new ArrayList<>(moveOrder);

            int minMoveOrder = min(moveOrderWorkCopy);

            for (int j =0;j<cardsWorkCopy.size();j++){
                if (moveOrderWorkCopy.get(j) == minMoveOrder) {
                    moveToStack(cardsWorkCopy.get(j), originsWorkCopy.get(j), OPTION_UNDO);

                    currentCards.remove(cardsWorkCopy.get(j));
                    currentOrigins.remove(originsWorkCopy.get(j));
                    moveOrder.remove(moveOrderWorkCopy.get(j));
                }
            }
        }

        /**
         * Adds cards in front of this entry. The move order of every current card will be increased
         * by 1 and the new cards get the order 0.
         *
         * @param cards  The cards to add
         * @param stacks The origins of the cards to add
         */
        void addInFront(ArrayList<Card> cards, ArrayList<Stack> stacks) {
            ArrayList<Card> tempCards = currentCards;
            ArrayList<Stack> tempOrigins = currentOrigins;
            ArrayList<Integer> tempMoveOrders = moveOrder;

            currentCards = new ArrayList<>(cards);
            currentOrigins = new ArrayList<>(stacks);
            moveOrder = new ArrayList<>();

            for (int i=0;i<currentCards.size();i++){
                moveOrder.add(0);
            }

            //Check for each card, if it is already in the entry
            for (int i = 0; i < tempCards.size(); i++) {
                currentCards.add(tempCards.get(i));
                currentOrigins.add(tempOrigins.get(i));
                moveOrder.add(tempMoveOrders.get(i)+1);                                         //increment the orders by one
            }
        }


        void addFlip(Card card) {                                                                   //add a card to flip
            flipCards.add(card);
        }

        boolean hasMoreToDo(){
            return currentCards.size()!=0;
        }
    }
}
