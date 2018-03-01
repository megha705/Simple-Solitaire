/* Copyright (C) 2016  Tobias Bielefeld
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

package de.tobiasbielefeld.solitaire.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import de.tobiasbielefeld.solitaire.R;
import de.tobiasbielefeld.solitaire.classes.CardAndStack;
import de.tobiasbielefeld.solitaire.ui.GameManager;
import de.tobiasbielefeld.solitaire.ui.manual.Manual;

import static de.tobiasbielefeld.solitaire.SharedData.GAME;
import static de.tobiasbielefeld.solitaire.SharedData.autoComplete;
import static de.tobiasbielefeld.solitaire.SharedData.autoMove;
import static de.tobiasbielefeld.solitaire.SharedData.currentGame;
import static de.tobiasbielefeld.solitaire.SharedData.gameLogic;
import static de.tobiasbielefeld.solitaire.SharedData.hint;
import static de.tobiasbielefeld.solitaire.SharedData.lg;
import static de.tobiasbielefeld.solitaire.SharedData.movingCards;
import static de.tobiasbielefeld.solitaire.SharedData.prefs;
import static de.tobiasbielefeld.solitaire.SharedData.timer;

/**
 * dialog to handle new games or returning to main menu( in that case, cancel the current activity)
 */

public class DialogInGameHelpMenu extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final GameManager gameManager = (GameManager) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(lg.getGameName())
                .setItems(R.array.help_menu, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // "which" argument contains index of selected item
                        switch (which) {
                            case 0:
                                if (!gameLogic.hasWon()) {
                                    hint.showHint();
                                }
                                break;
                            case 1:
                                if (!gameLogic.hasWon()) {
                                    autoMove.start();
                                }
                                break;
                            case 2:
                                if (!gameLogic.hasWon()) {
                                    if (currentGame.hintTest() == null) {
                                        if (prefs.getShowDialogMixCards()) {
                                            prefs.putShowDialogMixCards(false);
                                            DialogMixCards dialogMixCards = new DialogMixCards();
                                            dialogMixCards.show(getFragmentManager(), "MIX_DIALOG");
                                        } else {
                                            currentGame.mixCards();
                                        }
                                    } else {
                                        DialogMixCardsMovesAvailable dialogMixCardsStillMovesAvailable = new DialogMixCardsMovesAvailable();
                                        dialogMixCardsStillMovesAvailable.show(getFragmentManager(), "MIX_DIALOG_MOVES_AVAILABLE");
                                    }
                                }
                                break;
                            case 3:
                                Intent intent = new Intent(gameManager, Manual.class);
                                intent.putExtra(GAME,lg.getSharedPrefName());
                                startActivity(intent);
                                break;
                        }
                    }
                })
                .setNegativeButton(R.string.game_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //just cancel
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.getListView().setScrollbarFadingEnabled(false);

        return dialog;
    }
}